package com.crossplatform.sdk.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.crossplatform.sdk.presentation.components.scanner.CardScanError
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

@Composable
internal actual fun CameraTextPreview(
    modifier: Modifier,
    onTextRecognized: (List<String>) -> Unit,
    onError: (CardScanError) -> Unit
) {
    val context = LocalContext.current
    val currentOnText by rememberUpdatedState(onTextRecognized)
    val currentOnError by rememberUpdatedState(onError)

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) currentOnError(CardScanError.PermissionDenied)
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) return

    val lifecycleOwner = LocalLifecycleOwner.current
    // COMPATIBLE (TextureView) so the Compose overlay draws correctly on top inside a Dialog.
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner) {
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val analyzer = TextAnalyzer { lines -> currentOnText(lines) }
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null

        providerFuture.addListener({
            try {
                val cameraProvider = providerFuture.get()
                provider = cameraProvider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                // Embossed card text is small; the default 640x480 is too coarse for OCR.
                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(1280, 720),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                )
                            )
                            .build()
                    )
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, analyzer) }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (_: Exception) {
                currentOnError(CardScanError.CameraUnavailable)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            provider?.unbindAll()
            analysisExecutor.shutdown()
            analyzer.close()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private class TextAnalyzer(
    private val onText: (List<String>) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastRunMs = 0L

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        val now = SystemClock.elapsedRealtime()
        if (mediaImage == null || now - lastRunMs < 250) { // ~4 fps is plenty and saves battery
            imageProxy.close()
            return
        }
        lastRunMs = now

        // ML Kit only recognizes text that's upright relative to the rotation we hand it — it
        // doesn't search for arbitrary in-plane rotations on its own. Most cards print the
        // number horizontally, but some (many metal/virtual cards) run it vertically along the
        // edge — and that vertical text can read top-to-bottom OR bottom-to-top depending on the
        // card design, i.e. it needs a +90 correction on some cards and a -90 (+270) correction
        // on others. Trying only one direction risks reading the digits back-to-front on cards
        // that needed the other one: the extracted string then fails the Luhn check and gets
        // silently dropped, which looks identical to "nothing found". Run both, plus the base
        // orientation for normal horizontal cards. Each pass is parsed separately (see
        // CardScanDialog) so a card's own top-to-bottom line order isn't scrambled by mixing
        // results from different orientations.
        val baseRotation = imageProxy.imageInfo.rotationDegrees
        val orientations = intArrayOf(
            baseRotation,
            (baseRotation + 90) % 360,
            (baseRotation + 270) % 360
        )
        var pending = orientations.size

        for (rotation in orientations) {
            val image = InputImage.fromMediaImage(mediaImage, rotation)
            recognizer.process(image)
                // ML Kit delivers listeners on the main thread by default.
                .addOnSuccessListener { text ->
                    val lines = text.textBlocks.flatMap { block -> block.lines.map { it.text } }
                    if (lines.isNotEmpty()) onText(lines)
                }
                .addOnCompleteListener {
                    pending--
                    if (pending == 0) imageProxy.close()
                }
        }
    }

    fun close() = recognizer.close()
}