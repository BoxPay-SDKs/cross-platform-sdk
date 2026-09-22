@file:OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)

package com.crossplatform.sdk.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.crossplatform.sdk.presentation.components.scanner.CardScanError
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPreset1920x1080
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMSampleBufferRef
import platform.QuartzCore.CACurrentMediaTime
import platform.UIKit.UIView
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import platform.posix.QOS_CLASS_USER_INITIATED

@Composable
internal actual fun CameraTextPreview(
    modifier: Modifier,
    onTextRecognized: (List<String>) -> Unit,
    onError: (CardScanError) -> Unit
) {
    val currentOnText by rememberUpdatedState(onTextRecognized)
    val currentOnError by rememberUpdatedState(onError)
    var authorized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> authorized = true
            AVAuthorizationStatusNotDetermined ->
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    dispatch_async(dispatch_get_main_queue()) {
                        if (granted) authorized = true
                        else currentOnError(CardScanError.PermissionDenied)
                    }
                }
            else -> currentOnError(CardScanError.PermissionDenied)
        }
    }

    if (!authorized) return

    val scanner = remember {
        IosTextScanner(
            onText = { currentOnText(it) },
            onError = { currentOnError(it) }
        )
    }
    DisposableEffect(scanner) {
        scanner.start()
        onDispose { scanner.stop() }
    }

    UIKitView(
        factory = { PreviewContainer(scanner.session) },
        modifier = modifier
    )
}

/** UIView hosting the camera preview layer and keeping it sized to the view. */
private class PreviewContainer(session: AVCaptureSession) : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private val previewLayer = AVCaptureVideoPreviewLayer(session = session)

    init {
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        previewLayer.connection?.videoOrientation = AVCaptureVideoOrientationPortrait
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer.frame = bounds
    }
}

/**
 * AVCaptureSession -> video frames -> Vision text recognition (all on-device).
 * Frames are throttled to ~4/s.
 */
private class IosTextScanner(
    private val onText: (List<String>) -> Unit,
    private val onError: (CardScanError) -> Unit
) : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {

    val session = AVCaptureSession()
    private val videoQueue = dispatch_queue_create("com.crossplatform.sdk.cardscan.video", null)
    private var lastRunSeconds = 0.0

    private val request = VNRecognizeTextRequest(completionHandler = { req, _ ->
        val lines = (req as? VNRecognizeTextRequest)?.results.orEmpty().mapNotNull { obs ->
            val top = (obs as? VNRecognizedTextObservation)?.topCandidates(1u)?.firstOrNull()
            (top as? VNRecognizedText)?.string
        }
        if (lines.isNotEmpty()) {
            dispatch_async(dispatch_get_main_queue()) { onText(lines) }
        }
    }).apply {
        recognitionLevel = VNRequestTextRecognitionLevelAccurate
        usesLanguageCorrection = false // language correction "fixes" digit strings into words
    }

    // Orientations to run each frame through. The capture connection already delivers upright
    // (portrait) buffers, so `.up` catches a normally-printed horizontal card number. Vision
    // doesn't search other in-plane rotations on its own, though — a number printed running
    // vertically down the card (common on many metal/virtual cards) looks like a column of
    // sideways characters to a `.up` pass and gets missed entirely. That vertical text can read
    // top-to-bottom on one card design and bottom-to-top on another, so a single extra rotation
    // only catches one direction — the other reads the digits back-to-front, fails the Luhn
    // check, and silently drops, which looks identical to "nothing found". `.right` and `.left`
    // cover both directions.
    private val orientations = listOf(
        platform.ImageIO.kCGImagePropertyOrientationUp,
        platform.ImageIO.kCGImagePropertyOrientationRight,
        platform.ImageIO.kCGImagePropertyOrientationLeft
    )

    fun start() {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        val input = device?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, null) }
        if (input == null || !session.canAddInput(input)) {
            onError(CardScanError.CameraUnavailable)
            return
        }

        session.beginConfiguration()
        if (session.canSetSessionPreset(AVCaptureSessionPreset1920x1080)) {
            session.sessionPreset = AVCaptureSessionPreset1920x1080
        }
        session.addInput(input)

        val output = AVCaptureVideoDataOutput().apply {
            alwaysDiscardsLateVideoFrames = true
            setSampleBufferDelegate(this@IosTextScanner, videoQueue)
        }
        if (session.canAddOutput(output)) {
            session.addOutput(output)
            // Deliver upright buffers so Vision doesn't need an orientation hint.
            output.connectionWithMediaType(AVMediaTypeVideo)?.videoOrientation =
                AVCaptureVideoOrientationPortrait
        }
        session.commitConfiguration()

        // startRunning blocks, so keep it off the main thread.
        dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED.toLong(), 0UL)) {
            session.startRunning()
        }
    }

    fun stop() {
        dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED.toLong(), 0UL)) {
            session.stopRunning()
        }
    }

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection
    ) {
        val now = CACurrentMediaTime()
        if (didOutputSampleBuffer == null || now - lastRunSeconds < 0.25) return
        lastRunSeconds = now

        // Each pass's completion fires onText separately (see the request's completionHandler
        // above), so results from the two orientations reach CardTextParser as two distinct
        // line lists rather than one merged, order-scrambled blob.
        for (orientation in orientations) {
            val handler = VNImageRequestHandler(
                cMSampleBuffer = didOutputSampleBuffer,
                orientation = orientation,
                options = emptyMap<Any?, Any>()
            )
            handler.performRequests(listOf(request), null)
        }
    }
}