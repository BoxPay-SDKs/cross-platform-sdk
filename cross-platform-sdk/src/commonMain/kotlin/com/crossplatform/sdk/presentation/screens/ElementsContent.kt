package com.crossplatform.sdk.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.AnalyticsEvents
import com.crossplatform.sdk.domain.model.MainScreenModel
import com.crossplatform.sdk.domain.model.PaymentMethodTab
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod
import com.crossplatform.sdk.presentation.UiState
import com.crossplatform.sdk.presentation.components.BankComponent
import com.crossplatform.sdk.presentation.components.CardComponent
import com.crossplatform.sdk.presentation.components.Footer
import com.crossplatform.sdk.presentation.components.ShimmerView
import com.crossplatform.sdk.presentation.components.UPIComponent
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.viewmodel.BoxPayElementsViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Data classes — replace the 25-parameter flat signature
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Visual/theming configuration shared by all payment method tabs.
 */
data class ElementsUiConfig(
    val ctaBorderRadius: Int,
    val buttonColor: String,
    val buttonTextColor: String,
    val focusedBorderColor: String,
    val unfocusedBorderColor: String,
    val currencySymbol: String,
    val amount: Double,
)

/**
 * All state related to the UPI QR code flow.
 */
data class QrState(
    val showQROnLoad: Boolean,
    val isQRLoaded: Boolean,
    val qrImage: String,
    val qrTimer: Int,
)

// ─────────────────────────────────────────────────────────────────────────────
// Payment-type string constants — no more magic strings scattered inline
// ─────────────────────────────────────────────────────────────────────────────

private object PaymentTypes {
    const val UPI_COLLECT      = "upi/collect"
    const val UPI_INTENT       = "upi/intent"
    const val UPI_QR           = "upi/qr"
    const val UPI_OTM_COLLECT  = "upiotm/collect"
    const val UPI_OTM_INTENT   = "upiotm/intent"
    const val UPI_OTM_QR       = "upiotm/qr"
    const val NET_BANKING      = "netBanking"
    const val WALLET           = "wallet"
}

// ─────────────────────────────────────────────────────────────────────────────
// Root composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ElementsContent(
    viewModel                : BoxPayElementsViewModel,
    availableMethods         : List<PaymentMethodTab>,
    upiMethodFlags           : MainScreenModel.MethodFlags,
    selectedMethod           : PaymentMethodTab,
    savedUpiList             : List<SelectedPaymentMethod>,
    isBoxPayProceedButtonVisible: Boolean,
    uiConfig                 : ElementsUiConfig,
    qrState                  : QrState,
    shopperToken             : String,
    onMethodSelected         : (PaymentMethodTab) -> Unit,
    stopFetchStatusPolling   : () -> Unit,
    postUpiCollectRequest    : (shopperVpa: String, type: String, instrumentRef: String?, saveInstrument: Boolean?) -> Unit,
    postUpiIntentRequest     : (selectedIntent: String, type: String) -> Unit,
    postUPIQrRequest         : (type: String) -> Unit,
    onProceedCardRequest     : (Boolean) -> Unit,
) {
    // ── Hoist per-tab selection state here so it survives tab switches ────────
    var selectedUpiInstrumentId     by rememberSaveable { mutableStateOf("") }
    var selectedBankInstrumentId    by rememberSaveable { mutableStateOf("") }
    var selectedWalletInstrumentId  by rememberSaveable { mutableStateOf("") }
    var selectedBnplInstrumentId    by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {

        // ── Tab bar — hidden when only one method is available ────────────────
        if (availableMethods.size > 1) {
            MethodTabBar(
                methods        = availableMethods,
                selectedMethod = selectedMethod,
                onSelect       = onMethodSelected,
            )
            HorizontalDivider(color = Color(0xFFD3D1C7), thickness = 0.5.dp)
        }

        // ── Body ──────────────────────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            AnimatedContent(
                targetState = selectedMethod,
                transitionSpec = {
                    slideInHorizontally(tween(220)) { it / 4 } + fadeIn(tween(180)) togetherWith
                            slideOutHorizontally(tween(200)) { -it / 4 } + fadeOut(tween(150))
                },
                label = "methodScreen",
            ) { method ->
                LaunchedEffect(method) {
                    viewModel.setPaySelection(BoxPayElementsViewModel.PaySelection.None)
                }
                when (method) {
                    PaymentMethodTab.UPI ->
                        UpiTab(
                            viewModel              = viewModel,
                            upiMethodFlags         = upiMethodFlags,
                            savedUpiList           = savedUpiList,
                            isBoxPayProceedButtonVisible = isBoxPayProceedButtonVisible,
                            uiConfig               = uiConfig,
                            qrState                = qrState,
                            shopperToken           = shopperToken,
                            selectedId             = selectedUpiInstrumentId,
                            onSelectId             = { selectedUpiInstrumentId = it },
                            stopFetchStatusPolling = stopFetchStatusPolling,
                            postUpiCollectRequest  = postUpiCollectRequest,
                            postUpiIntentRequest   = postUpiIntentRequest,
                            postUPIQrRequest       = postUPIQrRequest,
                        )

                    PaymentMethodTab.CARDS ->
                        CardsTab(
                            viewModel            = viewModel,
                            uiConfig             = uiConfig,
                            shopperToken         = shopperToken,
                            onProceedCardRequest = onProceedCardRequest,
                            isBoxPayPayButtonVisible = isBoxPayProceedButtonVisible
                        )

                    PaymentMethodTab.NETBANKING ->
                        InstrumentListTab(
                            viewModel            = viewModel,
                            screenName           = "NetBanking Element",
                            analyticsScreenName  = "BoxPayElements NetBanking",
                            errorMessage         = "NetBanking elements not loaded",
                            title                = "All Banks",
                            paymentType          = PaymentTypes.NET_BANKING,
                            uiConfig             = uiConfig,
                            selectedInstrumentId = selectedBankInstrumentId,
                            onSelectId           = { selectedBankInstrumentId = it },
                            isBoxPayProceedButtonVisible = isBoxPayProceedButtonVisible
                        )

                    PaymentMethodTab.WALLET ->
                        InstrumentListTab(
                            viewModel            = viewModel,
                            screenName           = "Wallet Elements",
                            analyticsScreenName  = "BoxPayElements Wallet",
                            errorMessage         = "Wallet elements not loaded",
                            title                = "All Wallets",
                            paymentType          = PaymentTypes.WALLET,
                            uiConfig             = uiConfig,
                            selectedInstrumentId = selectedWalletInstrumentId,
                            onSelectId           = { selectedWalletInstrumentId = it },
                            isBoxPayProceedButtonVisible = isBoxPayProceedButtonVisible
                        )

                    PaymentMethodTab.BUYNOWPAYLATER ->
                        InstrumentListTab(
                            viewModel            = viewModel,
                            screenName           = "BNPL Element",
                            analyticsScreenName  = "BoxPayElements BNPL",
                            errorMessage         = "BNPL elements not loaded",
                            title                = "All Banks",
                            paymentType          = PaymentTypes.NET_BANKING,
                            uiConfig             = uiConfig,
                            selectedInstrumentId = selectedBnplInstrumentId,
                            onSelectId           = { selectedBnplInstrumentId = it },
                            isBoxPayProceedButtonVisible = isBoxPayProceedButtonVisible
                        )

                    PaymentMethodTab.EMI ->
                        EmiTab()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UPI Tab
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UpiTab(
    viewModel              : BoxPayElementsViewModel,
    upiMethodFlags         : MainScreenModel.MethodFlags,
    savedUpiList           : List<SelectedPaymentMethod>,
    isBoxPayProceedButtonVisible: Boolean,
    uiConfig               : ElementsUiConfig,
    qrState                : QrState,
    shopperToken           : String,
    selectedId             : String,
    onSelectId             : (String) -> Unit,
    stopFetchStatusPolling : () -> Unit,
    postUpiCollectRequest  : (shopperVpa: String, type: String, instrumentRef: String?, saveInstrument: Boolean?) -> Unit,
    postUpiIntentRequest   : (selectedIntent: String, type: String) -> Unit,
    postUPIQrRequest       : (type: String) -> Unit,
) {
    val isOtm = upiMethodFlags.isUPIOtmCollectVisible

    Column {
        UPIComponent(
            methodFlags               = upiMethodFlags,
            onClickSavedUpiPayButton  = { instrumentRef, shopperVpa ->
                viewModel.setPaySelection(
                    BoxPayElementsViewModel.PaySelection.SavedUpi(instrumentRef, shopperVpa)
                )
                viewModel.callUiAnalytics(
                    AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                    "Boxpay elements upi",
                    "Payment Category selected through saved upi",
                )
                postUpiCollectRequest(shopperVpa, PaymentTypes.UPI_COLLECT, instrumentRef, null)
            },
            onVpaChanged = { vpa ->
                viewModel.setPaySelection(BoxPayElementsViewModel.PaySelection.UpiCollect(vpa))
            },
            onClickUpiCollectPayButton = { shopperVpa, saveInstrument ->
                viewModel.callUiAnalytics(
                    AnalyticsEvents.PAYMENT_CATEGORY_SELECTED.value,
                    "Boxpay elements upi",
                    "Payment Category selected through upi collect",
                )
                viewModel.callUiAnalytics(
                    AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                    "Boxpay elements upi",
                    "Payment Category selected through upi collect",
                )
                postUpiCollectRequest(
                    shopperVpa,
                    if (isOtm) PaymentTypes.UPI_OTM_COLLECT else PaymentTypes.UPI_COLLECT,
                    null,
                    saveInstrument,
                )
            },
            onClickUpiIntentPayButton = { selectedIntent ->
                viewModel.callUiAnalytics(
                    AnalyticsEvents.PAYMENT_CATEGORY_SELECTED.value,
                    "Boxpay elements upi",
                    "Payment Category selected through intent",
                )
                viewModel.callUiAnalytics(
                    AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                    "Boxpay elements upi",
                    "Payment Category selected through intent",
                )
                postUpiIntentRequest(
                    selectedIntent,
                    if (isOtm) PaymentTypes.UPI_OTM_INTENT else PaymentTypes.UPI_INTENT,
                )
            },
            onClickUpiQRPayButton = {
                postUPIQrRequest(if (isOtm) PaymentTypes.UPI_OTM_QR else PaymentTypes.UPI_QR)
            },
            savedUpiList   = savedUpiList,
            onClickRadio   = { id ->
                onSelectId(id)
                viewModel.callUiAnalytics(
                    AnalyticsEvents.PAYMENT_CATEGORY_SELECTED.value,
                    "MainScreen",
                    "Payment Category selected through saved upi",
                )
            },
            onErrorLoadingIntent = { message ->
                viewModel.callUiAnalytics(
                    AnalyticsEvents.UPI_APP_NOT_FOUND.value,
                    "Boxpay elements upi",
                    "UPI App related messages $message",
                )
            },
            buttonTextColor                = uiConfig.buttonTextColor,
            buttonColor                    = uiConfig.buttonColor,
            currencySymbol                 = uiConfig.currencySymbol,
            amount                         = uiConfig.amount,
            ctaBorderRadius                = uiConfig.ctaBorderRadius,
            focusedTextInputBorderColor    = uiConfig.focusedBorderColor,
            unfocusedTextInputBorderColor  = uiConfig.unfocusedBorderColor,
            shopperToken                   = shopperToken,
            selectedId                     = selectedId,
            qrTimer                        = qrState.qrTimer,
            qrImage                        = qrState.qrImage,
            stopFunctionCall               = stopFetchStatusPolling,
            showQROnLoad                   = qrState.showQROnLoad,
            isQRLoaded                     = qrState.isQRLoaded,
            isBoxPayPayButtonVisible       = isBoxPayProceedButtonVisible,
            onClickIntent = {
                viewModel.setPaySelection(BoxPayElementsViewModel.PaySelection.UpiIntent(it))
            }
        )
        Footer()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cards Tab
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CardsTab(
    viewModel            : BoxPayElementsViewModel,
    uiConfig             : ElementsUiConfig,
    shopperToken         : String,
    isBoxPayPayButtonVisible : Boolean,
    onProceedCardRequest : (Boolean) -> Unit,
) {
    // Collect flows once here — not repeatedly inside sub-expressions
    LaunchedEffect(Unit) {
        viewModel.setPaySelection(BoxPayElementsViewModel.PaySelection.Card)
    }
    val isSICheckboxChecked   by CheckoutDetailsHandler.isSICheckboxCheckedFlow.collectAsStateWithLifecycle()
    val isSICheckboxEnabled   by CheckoutDetailsHandler.isSICheckboxEnabledFlow.collectAsStateWithLifecycle()
    val isSubscriptionCheckout by CheckoutDetailsHandler.isSubscriptionCheckoutFlow.collectAsStateWithLifecycle()
    val subscription           by CheckoutDetailsHandler.subscriptionFlow.collectAsStateWithLifecycle()
    val isTestEnv              by CheckoutDetailsHandler.isTestEnvFlow.collectAsStateWithLifecycle()

    // Local copy synced with the external source of truth via LaunchedEffect
    var isSiCheckBoxChecked by remember { mutableStateOf(isSICheckboxChecked) }
    LaunchedEffect(isSICheckboxChecked) { isSiCheckBoxChecked = isSICheckboxChecked }

    val isSubscriptionDetailsVisible = isSubscriptionCheckout && isSiCheckBoxChecked

    CardComponent(
        isSICheckboxChecked          = isSiCheckBoxChecked,
        isSICheckboxEnabled          = isSICheckboxEnabled,
        isSubscriptionCheckout       = isSubscriptionCheckout,
        isSubscriptionDetailsVisible = isSubscriptionDetailsVisible,
        onClickCheckBoxItem          = { isSiCheckBoxChecked = it },
        onClickShowKnowMoreDialog    = { viewModel.showKnowMoreDialog.value = true },
        onClickCVVInfo               = { viewModel.showCvvInfo.value = true },
        onClickSavedCardCheckBox     = {
            viewModel.isSavedCardCheckBoxClicked.value =
                !viewModel.isSavedCardCheckBoxClicked.value
        },
        shopperToken     = shopperToken,
        subscription     = subscription,
        currencySymbol   = uiConfig.currencySymbol,

        // Card field values
        cardNumberText    = viewModel.cardNumberText.value,
        cardHolderNameText = viewModel.cardHolderNameText.value,
        cardExpiryText    = viewModel.cardExpiryText.value,
        cardCvvText       = viewModel.cardCvvText.value,
        cardNickNameText  = viewModel.cardNickNameText.value,

        // Error flags
        cardNumberError    = viewModel.cardNumberError.value,
        cardHolderNameError = viewModel.cardHolderNameError.value,
        cardExpiryError    = viewModel.cardExpiryError.value,
        cardCvvError       = viewModel.cardCvvError.value,

        // Constraints
        maxCardNumberLength = viewModel.maxCardNumberLength.value,
        maxCvvLength        = viewModel.maxCvvLength.value,

        // Change handlers
        handleCardNumberChange = {
            viewModel.handleCardNumberChange(it, isTestEnv = isTestEnv)
        },
        handleCardHolderNameChange = {
            viewModel.cardHolderNameText.value = it
            if (it.isNotBlank()) viewModel.cardHolderNameError.value = false
            viewModel.checkCardValid(isTestEnv)
        },
        handleExpiryChange = {
            viewModel.handleExpiryChange(it, isTestEnv)
        },
        handleCvvChange = { cvv ->
            viewModel.cardCvvText.value = cvv
            // Only clear the error once the CVV meets the required length
            viewModel.cardCvvError.value = cvv.length < viewModel.maxCvvLength.value
            viewModel.cardCvvErrorText.value = when {
                cvv.isEmpty()                           -> "Required"
                cvv.length < viewModel.maxCvvLength.value -> "Invalid CVV"
                else                                    -> ""
            }
            viewModel.checkCardValid(isTestEnv)
        },

        cardSelectedIcon = viewModel.cardSelectedIcon.value,

        // Clear-error callbacks
        setCardNumberError    = { viewModel.cardNumberError.value = false },
        setCardHolderNameError = { viewModel.cardHolderNameError.value = false },
        setCardExpiryError    = { viewModel.cardExpiryError.value = false },
        setCardCvvError       = { viewModel.cardCvvError.value = false },

        // Blur / validation callbacks
        onBlurCardNumber = {
            val cleaned = viewModel.cardNumberText.value.filter { it.isDigit() }
            viewModel.cardNumberError.value = cleaned.isEmpty() ||
                    (!isTestEnv && (!viewModel.methodEnabled.value || !viewModel.cardNumberValid.value))
            viewModel.cardNumberErrorText.value = when {
                cleaned.isEmpty()                -> "Required"
                !viewModel.methodEnabled.value   -> "This card is not supported for the payment"
                !viewModel.cardNumberValid.value -> "Invalid card number"
                else                             -> ""
            }
        },
        onBlurCardName = {
            viewModel.cardHolderNameError.value    = viewModel.cardHolderNameText.value.trim().isEmpty()
            viewModel.cardHolderNameErrorText.value = if (viewModel.cardHolderNameError.value) "Required" else ""
        },
        onBlurCardExpiry = {
            // Require at least 5 chars (MM/YY) AND pass the domain-level validity check
            val expiry = viewModel.cardExpiryText.value
            viewModel.cardExpiryError.value = expiry.length < 5 || !viewModel.cardExpiryValid.value
            viewModel.cardExpiryErrorText.value = when {
                expiry.isEmpty() -> "Required"
                else             -> "Invalid Expiry"
            }
        },
        onBlurCardCVV = {
            viewModel.cardCvvError.value    = viewModel.cardCvvText.value.length < viewModel.maxCvvLength.value
            viewModel.cardCvvErrorText.value = if (viewModel.cardCvvText.value.isEmpty()) "Required" else "Invalid CVV"
        },

        // Error text strings
        cardNumberErrorText    = viewModel.cardNumberErrorText.value,
        cardHolderNameErrorText = viewModel.cardHolderNameErrorText.value,
        cardExpiryErrorText    = viewModel.cardExpiryErrorText.value,
        cardCvvErrorText       = viewModel.cardCvvErrorText.value,

        amount                   = uiConfig.amount,
        cardValid                = viewModel.cardValid.value,
        postCardRequest          = onProceedCardRequest,
        buttonColor              = uiConfig.buttonColor,
        buttonTextColor          = uiConfig.buttonTextColor,
        ctaBorderRadius          = uiConfig.ctaBorderRadius,
        unfocusedTextInputBorderColor = uiConfig.unfocusedBorderColor,
        focusedTextInputBorderColor   = uiConfig.focusedBorderColor,
        isBoxPayPayButtonVisible = isBoxPayPayButtonVisible,
        isSavedCardCheckBoxClicked = viewModel.isSavedCardCheckBoxClicked.value,
        modifier                 = Modifier.fillMaxWidth().wrapContentHeight(),
        normalCheckout = false
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Generic instrument-list tab (NetBanking / Wallet / BNPL)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InstrumentListTab(
    viewModel            : BoxPayElementsViewModel,
    screenName           : String,      // used in analytics
    analyticsScreenName  : String,      // crash-event screen name
    errorMessage         : String,      // crash-event message prefix
    title                : String,      // header shown inside BankComponent
    paymentType          : String,      // passed to postOtherRequest
    uiConfig             : ElementsUiConfig,
    selectedInstrumentId : String,
    onSelectId           : (String) -> Unit,
    isBoxPayProceedButtonVisible: Boolean
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState) {
        is UiState.Error -> {
            val message = (uiState as UiState.Error).message
            Text("Something went wrong. Please try again.")
            LaunchedEffect(message) {
                viewModel.callUiAnalytics(
                    event      = AnalyticsEvents.SDK_CRASH.value,
                    screenName = analyticsScreenName,
                    message    = "$errorMessage $message",
                )
            }
        }

        UiState.Loading ->
            ShimmerView(modifier = Modifier.fillMaxWidth().wrapContentHeight())

        is UiState.Success -> {
            val list = (uiState as UiState.Success).data
            BankComponent(
                modifier                      = Modifier.fillMaxWidth().height(400.dp).verticalScroll(rememberScrollState()),
                list                          = list,
                buttonColor                   = uiConfig.buttonColor,
                buttonTextColor               = uiConfig.buttonTextColor,
                unfocusedTextInputBorderColor = uiConfig.unfocusedBorderColor,
                focusedTextInputBorderColor   = uiConfig.focusedBorderColor,
                selectedInstrumentId          = selectedInstrumentId,
                onClickRadio                  = { id ->
                    onSelectId(id)
                    viewModel.setPaySelection(
                        BoxPayElementsViewModel.PaySelection.Instrument(value = id, type = paymentType)
                    )
                    viewModel.callUiAnalytics(
                        event      = AnalyticsEvents.PAYMENT_CATEGORY_SELECTED.value,
                        screenName = screenName,
                        message    = "payment category selected",
                    )
                },
                searchQuery      = viewModel.netBankingSearchQuery.value,
                onSetSearchQuery = { viewModel.onSearch(it) },
                onProceedForward = {
                    viewModel.setPaySelection(
                        BoxPayElementsViewModel.PaySelection.Instrument(value = it, type = paymentType)
                    )
                    viewModel.postOtherRequest(it, paymentType) },
                amount           = uiConfig.amount,
                currencySymbol   = uiConfig.currencySymbol,
                ctaBorderRadius  = uiConfig.ctaBorderRadius,
                title            = title,
                isBoxPayPayButtonVisible = isBoxPayProceedButtonVisible
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EMI Tab — placeholder until the feature is implemented
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmiTab() {
    EMIScreen(
        modifier =  Modifier.fillMaxWidth().height(400.dp),
        onBackPress = {},
        isAutoNavigationEnabled = false,
        onExitCheckout = {}
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MethodTabBar(
    methods        : List<PaymentMethodTab>,
    selectedMethod : PaymentMethodTab?,
    onSelect       : (PaymentMethodTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .horizontalScroll(rememberScrollState()),
    ) {
        methods.forEach { method ->
            // Compute stable label string once per item, not per recomposition
            val indicatorLabel = remember(method) { "indicator_$method" }
            val isSelected     = method == selectedMethod

            val indicatorW by animateDpAsState(
                targetValue   = if (isSelected) 60.dp else 0.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label         = indicatorLabel,
            )

            Column(
                modifier            = Modifier
                    .widthIn(min = 80.dp)
                    .fillMaxHeight()
                    .clickable { onSelect(method) }
                    .padding(horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    text       = method.name,
                    fontFamily = LocalSDKFonts.current.primary,
                    fontSize   = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (isSelected) Color(0xFF1CA672) else Color(0xFF5F5E5A),
                    modifier   = Modifier.padding(bottom = 8.dp),
                )
                Box(
                    modifier = Modifier
                        .width(indicatorW)
                        .height(2.dp)
                        .background(
                            color = Color(0xFF1CA672),
                            shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp),
                        )
                )
            }
        }
    }
}