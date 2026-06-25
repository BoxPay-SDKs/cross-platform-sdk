package com.crossplatform.sdk.presentation.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.data.handler.BoxPayElementsHandler
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.AnalyticsEvents
import com.crossplatform.sdk.domain.model.PaymentMethodTab
import com.crossplatform.sdk.presentation.UiState
import com.crossplatform.sdk.presentation.components.CvvInfoBottomSheet
import com.crossplatform.sdk.presentation.components.KnowMoreBottomSheet
import com.crossplatform.sdk.presentation.components.ShimmerView
import com.crossplatform.sdk.presentation.components.ShowLoadingComponent
import com.crossplatform.sdk.presentation.viewmodel.BoxPayElementsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxPayElementsComponent(
    paymentMethodList : List<PaymentMethodTab>,
    isBoxPayProceedButtonVisible : Boolean,
    shopperToken : String,
    handler: BoxPayElementsHandler? = null
) {
    val viewModel : BoxPayElementsViewModel = koinViewModel()
    val screenState by viewModel.state.collectAsStateWithLifecycle()
    val boxPayAnimationVisible by viewModel.isBoxPayAnimationLoading.collectAsStateWithLifecycle()
    val showWebView by viewModel.showWebViewScreen.collectAsStateWithLifecycle()


    // ── CheckoutDetailsHandler fields ──────────────────────────────────────
    val ctaBorderRadius  by CheckoutDetailsHandler.ctaBorderRadiusFlow.collectAsStateWithLifecycle()
    val buttonColor      by CheckoutDetailsHandler.buttonColorFlow.collectAsStateWithLifecycle()
    val buttonTextColor  by CheckoutDetailsHandler.buttonTextColorFlow.collectAsStateWithLifecycle()
    val focusedBorder    by CheckoutDetailsHandler.focusedBorderColorFlow.collectAsStateWithLifecycle()
    val unfocusedBorder  by CheckoutDetailsHandler.unfocusedBorderColorFlow.collectAsStateWithLifecycle()
    val currencyFlow   by CheckoutDetailsHandler.currencyFlow.collectAsStateWithLifecycle()
    val (currencySymbol, _) = currencyFlow
    val amount           by CheckoutDetailsHandler.amountFlow.collectAsStateWithLifecycle()
    val showQROnLoad = CheckoutDetailsHandler.showQROnLoadFlow.collectAsStateWithLifecycle()

    val selectedMethod = remember {
        mutableStateOf(paymentMethodList.firstOrNull() ?: PaymentMethodTab.UPI)
    }

    val canPay by viewModel.isPayable.collectAsState()

    LaunchedEffect(canPay) { handler?.setPayable(canPay) }

    // merchant → SDK: route their button tap to the real payment call
    DisposableEffect(handler) {
        handler?.onSubmit = { viewModel.submitSelectedInstrument() }
        onDispose { handler?.onSubmit = null }
    }


    // ── Bottom sheet states ────────────────────────────────────────────────
    val webViewSheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val upiTimerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    when (screenState) {
        is UiState.Error -> {
            val message = (screenState as UiState.Error).message
            Text("Welcome to error screen $message")
            LaunchedEffect(message) {
                viewModel.callUiAnalytics(
                    event      = AnalyticsEvents.SDK_CRASH.value,
                    screenName = "BoxPayElements",
                    message    = "BoxPay elements not loaded $message",
                )
            }
        }
        UiState.Loading ->  ShimmerView(modifier = Modifier.wrapContentHeight().fillMaxWidth().padding(horizontal = 16.dp))
        is UiState.Success -> {
            val response = (screenState as UiState.Success).data
            if (boxPayAnimationVisible) {
                ShowLoadingComponent(Modifier.fillMaxWidth().wrapContentHeight())
                return
            }

            ElementsContent(
                viewModel        = viewModel,
                availableMethods = paymentMethodList,
                upiMethodFlags   = response.methodFlags,
                selectedMethod   = selectedMethod.value,
                savedUpiList     = viewModel.upiRecommendedList.value,
                isBoxPayProceedButtonVisible = isBoxPayProceedButtonVisible,

                uiConfig = ElementsUiConfig(
                    ctaBorderRadius      = ctaBorderRadius,
                    buttonColor          = buttonColor,
                    buttonTextColor      = buttonTextColor,
                    focusedBorderColor   = focusedBorder,
                    unfocusedBorderColor = unfocusedBorder,
                    currencySymbol       = currencySymbol,
                    amount               = amount,
                ),

                qrState = QrState(
                    showQROnLoad = showQROnLoad.value,
                    isQRLoaded   = viewModel.isQRLoaded.value,
                    qrImage      = viewModel.qrImage.value,
                    qrTimer      = viewModel.qrTimer.value,
                ),

                shopperToken = shopperToken,

                onMethodSelected = { method ->
                    viewModel.removeQRFromView()
                    if (method in setOf(
                            PaymentMethodTab.BUYNOWPAYLATER,
                            PaymentMethodTab.WALLET,
                            PaymentMethodTab.NETBANKING
                    )) {
                        viewModel.loadBanksList(method.name)
                    }
                    selectedMethod.value = method
                },

                stopFetchStatusPolling = { viewModel.stopFetchStatusPolling() },
                postUPIQrRequest       = { type -> viewModel.postUPIQrRequest(type) },
                postUpiCollectRequest  = { shopperVpa, type, instrumentRef, saveInstrument ->
                    viewModel.postUpiCollectRequest(
                        shopperVpa     = shopperVpa,
                        type           = type,
                        instrumentRef  = instrumentRef,
                        saveInstrument = saveInstrument,
                    )
                },
                postUpiIntentRequest   = { selectedIntent, type ->
                    viewModel.postUpiIntentRequest(selectedIntent = selectedIntent, type = type)
                },
                onProceedCardRequest   = { viewModel.postCardRequest(it) },
            )
        }
    }

    // ── WebView bottom sheet ───────────────────────────────────────
    // showWebView drives visibility — handler sets/clears it
    if (showWebView) {
        ModalBottomSheet(
            onDismissRequest   = {
                viewModel.onWebViewDismissed()
                CheckoutDetailsHandler.setIsWebViewVisible(false)
            },
            sheetState         = webViewSheetState,
            dragHandle         = null,       // full height, no handle needed
            containerColor     = Color.White,
            modifier           = Modifier.fillMaxSize(),
        ) {
            WebViewScreen(
                url = viewModel.setWebViewUrl.value,
                html = viewModel.setWebViewHtml.value,
                onBackPress = {result ->
                    viewModel.callFetchStatus(result ?: "")
                    viewModel.setWebViewScreen(false)
                }
            )
        }
    }

    if (viewModel.showCvvInfo.value) {
        ModalBottomSheet(
            onDismissRequest   = {
                viewModel.onWebViewDismissed()
                CheckoutDetailsHandler.setIsWebViewVisible(false)
            },
            sheetState         = webViewSheetState,
            dragHandle         = null,       // full height, no handle needed
            containerColor     = Color.White,
            modifier           = Modifier.fillMaxSize(),
        ) {
            CvvInfoBottomSheet(
                onClick = {
                    viewModel.showCvvInfo.value = !viewModel.showCvvInfo.value
                },
                buttonColor = buttonColor,
                buttonTextColor = buttonTextColor,
                borderRadius = ctaBorderRadius
            )
        }
    }

    if (viewModel.showKnowMoreDialog.value) {
        ModalBottomSheet(
            onDismissRequest   = {
                viewModel.onWebViewDismissed()
                CheckoutDetailsHandler.setIsWebViewVisible(false)
            },
            sheetState         = webViewSheetState,
            dragHandle         = null,       // full height, no handle needed
            containerColor     = Color.White,
            modifier           = Modifier.fillMaxSize(),
        ) {
            KnowMoreBottomSheet(
                buttonTextColor = buttonTextColor,
                buttonColor = buttonColor,
                ctaBorderRadius = ctaBorderRadius,
                onDismiss = {
                    viewModel.showKnowMoreDialog.value = false
                }
            )
        }
    }

    // ── UpiTimer bottom sheet ──────────────────────────────────────
    if (viewModel.proceedToTimer.value) {
        ModalBottomSheet(
            onDismissRequest   = {
                viewModel.onUPITimerBottomSheetDismissed()
            },
            sheetState         = upiTimerSheetState,
            dragHandle         = null,
            containerColor     = Color.White,
            modifier           = Modifier.fillMaxSize(),
        ) {
            UpiTimerScreen(
                shopperVpa  = viewModel.upiId.value,
                onBackPress = {
                    viewModel.onUPITimerBottomSheetDismissed()
                },
                buttonColor = buttonColor,
                buttonTextColor = buttonTextColor
            )
        }
    }
}