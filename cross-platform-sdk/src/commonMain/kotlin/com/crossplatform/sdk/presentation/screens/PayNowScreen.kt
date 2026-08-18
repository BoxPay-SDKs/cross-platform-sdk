package com.crossplatform.sdk.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.domain.model.PayNowUiState
import com.crossplatform.sdk.presentation.BackHandler
import com.crossplatform.sdk.presentation.components.Footer
import com.crossplatform.sdk.presentation.components.ShowLoadingComponent
import com.crossplatform.sdk.presentation.formatTimer
import com.crossplatform.sdk.presentation.rememberQrImageSaver
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import com.crossplatform.sdk.presentation.viewmodel.PayNowScreenViewModel
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_download
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_qr
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_timer
import crossplatformsdk.cross_platform_sdk.generated.resources.paynow_reinitiate_qr_icon
import crossplatformsdk.cross_platform_sdk.generated.resources.qr_expired_icon
import crossplatformsdk.cross_platform_sdk.generated.resources.qr_fetching_icon
import crossplatformsdk.cross_platform_sdk.generated.resources.qr_ready_icon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun PayNowScreen(
    onBackPress : () -> Unit,
    instrumentRef : String
) {

    val viewModel : PayNowScreenViewModel = koinViewModel()
    val saver = rememberQrImageSaver()
    val scope = rememberCoroutineScope()
    val boxPayAnimationVisible by viewModel.isQRFetching.collectAsStateWithLifecycle()
    val qrState by viewModel.qrState.collectAsStateWithLifecycle()
    val buttonTextColor = CheckoutDetailsHandler.buttonTextColorFlow.collectAsStateWithLifecycle()
    val buttonColor = CheckoutDetailsHandler.buttonColorFlow.collectAsStateWithLifecycle()
    val ctaBorderRadius = CheckoutDetailsHandler.ctaBorderRadiusFlow.collectAsStateWithLifecycle()
    var showCancelModal by remember { mutableStateOf(false) }

    var isDownloadingQr by remember { mutableStateOf(false) }
    var downloadDialogState by remember { mutableStateOf<DownloadDialogState?>(null) }

    BackHandler(onBack = {
        showCancelModal = true
    })
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F6FB))
                .verticalScroll(rememberScrollState())
        ) {
            when (val state = qrState) {

                PayNowUiState.Idle -> {
                    PayNowIdleState(
                        buttonColor = buttonColor.value,
                        buttonTextColor = buttonTextColor.value,
                        borderRadius = ctaBorderRadius.value,
                        onGetQr = {
                            viewModel.getPayNowQR(instrumentRef)
                        }
                    )
                }

                is PayNowUiState.Ready -> {
                    PayNowReadyState(
                        expirySec = state.totalSeconds.toLong(),
                        buttonColor = buttonColor.value,
                        borderRadius = ctaBorderRadius.value,
                        onDownloadQr = {
                            if (isDownloadingQr) return@PayNowReadyState  // guard double-taps
                            isDownloadingQr = true
                            scope.launch {
                                saver.saveBase64Image(state.qrImage, fileName = "paynow_qr")
                                    .onSuccess {
                                        println("====successfully downloaded")
                                        viewModel.lifecycleObserver.start()
                                        downloadDialogState = DownloadDialogState.Success
                                    }
                                    .onFailure { e ->
                                        println("==error came $e")
                                        downloadDialogState = DownloadDialogState.Error
                                    }
                                isDownloadingQr = false
                            }
                        },
                        onExpired = {
                            viewModel.markExpired()
                        },
                        isDownloadingQR = isDownloadingQr
                    )
                }

                PayNowUiState.Expired -> {
                    PayNowExpiredState(
                        buttonColor = buttonColor.value,
                        buttonTextColor = buttonTextColor.value,
                        borderRadius = ctaBorderRadius.value,
                        onGetNewQr = {
                            viewModel.getPayNowQR(instrumentRef)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Footer()
        }
        if(boxPayAnimationVisible) {
            ShowLoadingComponent(Modifier.fillMaxSize())
        }
        if (showCancelModal) {
            CancelPaymentModal(
                onNoClick  = { showCancelModal = false },
                onYesClick = {
                    showCancelModal = false
                    viewModel.stopFetchStatusPolling()
                    onBackPress()
                }
            )
        }
    }

    downloadDialogState?.let { dialogState ->
        AlertDialog(
            onDismissRequest = { downloadDialogState = null },
            confirmButton = {
                TextButton(onClick = { downloadDialogState = null }) {
                    Text("OK", fontFamily = LocalSDKFonts.current.primary)
                }
            },
            title = {
                Text(
                    text = if (dialogState is DownloadDialogState.Success) "Download complete"
                    else "Download failed",
                    fontFamily = LocalSDKFonts.current.primary
                )
            },
            text = {
                Text(
                    text = if (dialogState is DownloadDialogState.Success)
                        "QR code saved to your Downloads folder."
                    else
                        "We couldn't save the QR code. Please check your storage permissions and try again.",
                    fontFamily = LocalSDKFonts.current.primary
                )
            }
        )
    }
}

@Composable
private fun PayNowIdleState(
    buttonColor: String,
    buttonTextColor: String,
    borderRadius: Int,
    onGetQr: () -> Unit,
) {

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(Res.drawable.qr_fetching_icon),
            contentDescription = "",
            modifier = Modifier
                .size(140.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Get QR to Pay",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF363840),
            fontFamily = LocalSDKFonts.current.primary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Generate a QR code to pay using any PayNow supported app.",
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = Color(0xFF4F4D55),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onGetQr,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                .clip(RoundedCornerShape(borderRadius.dp))
                .background(buttonColor.toComposeColor()),
            shape = RoundedCornerShape(borderRadius.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor.toComposeColor(),
                contentColor = buttonTextColor.toComposeColor()
            )
        ) {

            Image(
                painter = painterResource(Res.drawable.ic_qr),
                contentDescription = "",
                modifier = Modifier
                    .size(32.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Get QR",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = buttonTextColor.toComposeColor()
            )
        }
    }
}

@Composable
private fun PayNowReadyState(
    expirySec: Long,
    buttonColor: String,
    borderRadius: Int,
    onDownloadQr: () -> Unit,
    isDownloadingQR : Boolean,
    onExpired: () -> Unit
) {

    var remainingSeconds by remember {
        mutableLongStateOf(expirySec)
    }

    LaunchedEffect(expirySec) {

        remainingSeconds = expirySec

        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }

        onExpired()
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Image(
            painter = painterResource(Res.drawable.qr_ready_icon),
            contentDescription = "",
            modifier = Modifier
                .size(140.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "QR Ready",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF363840),
            fontFamily = LocalSDKFonts.current.primary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Download the QR and scan using any PayNow supported app.",
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = Color(0xFF4F4D55),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onDownloadQr,
            enabled = !isDownloadingQR,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                .clip(RoundedCornerShape(borderRadius.dp)),
            shape = RoundedCornerShape(borderRadius.dp),
            border = BorderStroke(1.dp, buttonColor.toComposeColor())
        ) {
            if (isDownloadingQR) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = buttonColor.toComposeColor()
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Downloading QR...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = buttonColor.toComposeColor()
                )
            } else {
                Image(
                    painter = painterResource(Res.drawable.ic_download),
                    contentDescription = "",
                    modifier = Modifier.size(32.dp),
                    colorFilter = ColorFilter.tint(buttonColor.toComposeColor())
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Download QR",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = buttonColor.toComposeColor()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        QrTimer(
            remainingSeconds = remainingSeconds,
            buttonColor = buttonColor
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Once the timer expires, this QR will no longer be valid.",
            fontSize = 13.sp,
            color = Color(0xFF4F4D55),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        HowToPay()
    }
}

@Composable
private fun PayNowExpiredState(
    buttonColor: String,
    buttonTextColor: String,
    borderRadius: Int,
    onGetNewQr: () -> Unit
) {

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Image(
            painter = painterResource(Res.drawable.qr_expired_icon),
            contentDescription = "",
            modifier = Modifier
                .size(140.dp)
        )

        Spacer(modifier = Modifier.height(26.dp))

        Text(
            text = "QR Expired",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF363840),
            fontFamily = LocalSDKFonts.current.primary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "This QR code has expired.Please fetch a new QR to continue.",
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = Color(0xFF363840),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onGetNewQr,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(borderRadius.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor.toComposeColor(),
                contentColor = buttonTextColor.toComposeColor()
            )
        ) {

            Image(
                painter = painterResource(Res.drawable.paynow_reinitiate_qr_icon),
                contentDescription = "",
                modifier = Modifier
                    .size(32.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Get New QR",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun QrTimer(
    remainingSeconds: Long,
    buttonColor: String
) {
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF3F4F6))
            .padding(vertical = 18.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Image(
                    painter = painterResource(Res.drawable.ic_timer),
                    contentDescription = "",
                    modifier = Modifier
                        .size(32.dp),
                    colorFilter = ColorFilter.tint(buttonColor.toComposeColor())
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "QR is valid for",
                    fontSize = 14.sp,
                    color = Color(0xFF363840),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatTimer(remainingSeconds),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = buttonColor.toComposeColor()
            )
        }
    }
}

@Composable
private fun HowToPay() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF3F4F6))
            .padding(
                horizontal = 20.dp,
                vertical = 12.dp
            )
    ) {

        Text(
            text = "How to pay",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF363840)
        )

        Spacer(modifier = Modifier.height(14.dp))

        PayInstruction(
            number = "1.",
            text = "Download the QR code and open it on another device. Launch your banking app on your mobile phone and scan the QR code to make payment."
        )

        Spacer(modifier = Modifier.height(12.dp))

        PayInstruction(
            number = "2.",
            text = "Your payment will be made to the recipient shown on the payment screen."
        )

        Spacer(modifier = Modifier.height(12.dp))

        PayInstruction(
            number = "3.",
            text = "Do not close or refresh this window as it may take up to 2 mins for payment to be processed."
        )

        Spacer(modifier = Modifier.height(12.dp))

        PayInstruction(
            number = "4.",
            text = "You will be taken back to the merchant once the payment process is completed."
        )
    }
}

@Composable
private fun PayInstruction(
    number: String,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {

        Text(
            text = number,
            modifier = Modifier.width(24.dp),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF363840)
        )

        Text(
            text = text,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = Color(0xFF363840)
        )
    }
}

private sealed class DownloadDialogState {
    data object Success : DownloadDialogState()
    data object Error : DownloadDialogState()
}