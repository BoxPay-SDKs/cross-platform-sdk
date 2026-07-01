package com.crossplatform.sdk.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.handler.UserDataHandler
import com.crossplatform.sdk.data.model.AnalyticsEvents
import com.crossplatform.sdk.domain.model.TransactionStatusEnum
import com.crossplatform.sdk.presentation.SectionTitle
import com.crossplatform.sdk.presentation.UiState
import com.crossplatform.sdk.presentation.buildAddressString
import com.crossplatform.sdk.presentation.components.AddressComponent
import com.crossplatform.sdk.presentation.components.Footer
import com.crossplatform.sdk.presentation.components.MorePaymentMethods
import com.crossplatform.sdk.presentation.components.OfferSection
import com.crossplatform.sdk.presentation.components.OrderDetails
import com.crossplatform.sdk.presentation.components.PaymentSelectorView
import com.crossplatform.sdk.presentation.components.SavedCardComponent
import com.crossplatform.sdk.presentation.components.ShimmerView
import com.crossplatform.sdk.presentation.components.ShowLoadingComponent
import com.crossplatform.sdk.presentation.components.ShowUpdateAmountBottomSheet
import com.crossplatform.sdk.presentation.components.UPIComponent
import com.crossplatform.sdk.presentation.isPresentInSurchargeModel
import com.crossplatform.sdk.presentation.launchUpiIntent
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import com.crossplatform.sdk.presentation.viewmodel.MainScreenViewModel
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_upi
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun MainScreen(
    viewModel : MainScreenViewModel,
    onProceedCardScreen: (isAutoNavigationEnabled: Boolean) -> Unit,
    onProceedEMIScreen: (isAutoNavigationEnabled: Boolean) -> Unit,
    onProceedAddressScreen: (isNewAddress : Boolean) -> Unit,
    onProceedSavedAddressScreen: () -> Unit,
    onProceedNetBankingScreen: (isAutoNavigationEnabled: Boolean) -> Unit,
    onProceedWalletScreen: (isAutoNavigationEnabled: Boolean) -> Unit,
    onProceedBNPLScreen: (isAutoNavigationEnabled: Boolean) -> Unit,
    onProceedUPITimerScreen: (shopperVpa: String) -> Unit,
    onShowSwipeToPay : () -> Unit,
    onProceedInstantOfferScreen : () -> Unit,
    selectedOfferCode: String,
    onSetSelectedOfferCode : (String) -> Unit
) {
    val screenState by viewModel.state.collectAsStateWithLifecycle()
    val boxPayAnimationVisible by viewModel.isBoxPayAnimationLoading.collectAsStateWithLifecycle()
    val showWebView by viewModel.showWebViewScreen.collectAsStateWithLifecycle()
    val shopperFlow = CheckoutDetailsHandler.shopperFieldsConfigFlow.collectAsStateWithLifecycle()
    val firstName = UserDataHandler.firstNameFlow.collectAsStateWithLifecycle()
    val lastName = UserDataHandler.lastNameFlow.collectAsStateWithLifecycle()
    val email = UserDataHandler.emailFlow.collectAsStateWithLifecycle()
    val completePhoneNumber = UserDataHandler.completePhoneNumberFlow.collectAsStateWithLifecycle()
    val addressFlow = UserDataHandler.addressFlow.collectAsStateWithLifecycle()
    val labelFlow = UserDataHandler.labelFlow.collectAsStateWithLifecycle()
    val customFieldsFlow = UserDataHandler.customFieldsFlow.collectAsStateWithLifecycle()

    val focusedTextInputBorderColor = CheckoutDetailsHandler.focusedBorderColorFlow.collectAsStateWithLifecycle()
    val unfocusedTextInputBorderColor = CheckoutDetailsHandler.unfocusedBorderColorFlow.collectAsStateWithLifecycle()
    val buttonTextColor = CheckoutDetailsHandler.buttonTextColorFlow.collectAsStateWithLifecycle()
    val buttonColor = CheckoutDetailsHandler.buttonColorFlow.collectAsStateWithLifecycle()
    val currencyFlow = CheckoutDetailsHandler.currencyFlow.collectAsStateWithLifecycle()
    val (currencySymbol, _) = currencyFlow.value
    val amount = CheckoutDetailsHandler.amountFlow.collectAsStateWithLifecycle()
    val ctaBorderRadius = CheckoutDetailsHandler.ctaBorderRadiusFlow.collectAsStateWithLifecycle()
    val shopperToken = CheckoutDetailsHandler.shopperTokenFlow.collectAsStateWithLifecycle()
    val discountAmount = CheckoutDetailsHandler.discountAmountFlow.collectAsStateWithLifecycle()
    val surchargeDetails = CheckoutDetailsHandler.surchargeDetailsFlow.collectAsStateWithLifecycle()
    val isSICheckboxChecked = CheckoutDetailsHandler.isSICheckboxCheckedFlow.collectAsStateWithLifecycle()
    val isSICheckboxEnabled = CheckoutDetailsHandler.isSICheckboxEnabledFlow.collectAsStateWithLifecycle()
    val isOrderItemDetailsVisible = CheckoutDetailsHandler.isOrderItemDetailsVisibleFlow.collectAsStateWithLifecycle()
    val showQROnLoad = CheckoutDetailsHandler.showQROnLoadFlow.collectAsStateWithLifecycle()

    val selectedDeleteCardId = remember {
        mutableStateOf("")
    }
    val isShowSavedCardDeleteConfirmation = remember {
        mutableStateOf(false)
    }
    val selectedDeleteCardName = remember {
        mutableStateOf("")
    }
    val showUpdatedAmountBottomSheet = remember {
        mutableStateOf(false)
    }

    val selectedMethod = remember {
        mutableStateOf("")
    }
    val isToastVisible by viewModel.isToastVisible.collectAsStateWithLifecycle()

    val selectedRecommendedInstrumentId = remember {
        mutableStateOf("")
    }

    val selectedUpiInstrumentId = remember {
        mutableStateOf("")
    }

    when (screenState) {
        is UiState.Loading ->  ShimmerView(modifier = Modifier.fillMaxSize())
        is UiState.Error   -> {
            val message = (screenState as UiState.Error).message
            Text("Welcome to error screen $message")
            LaunchedEffect(message) {
                viewModel.callUiAnalytics(
                    event      = AnalyticsEvents.SDK_CRASH.value,
                    screenName = "Main screen",
                    message    = "mainscreen not loaded $message",
                )
            }
        }
        is UiState.Success -> {
            val response = (screenState as UiState.Success).data
            viewModel.isLoadingSession.value = false
            if(response.status == TransactionStatusEnum.EXPIRED) {
                viewModel.callCheckoutSessionExpireModal(
                    transactionId = response.transactionId
                )
            }

            if(response.status == TransactionStatusEnum.SUCCESS) {
                viewModel.callCheckoutSessionSuccessModal(
                    transactionId = response.transactionId
                )
            }

            LaunchedEffect(Unit) {
                viewModel.startSessionCountdown(response.sessionExpiryTimer)
            }

            val isMandatoryCustomFieldMissing = customFieldsFlow.value.any {
                it.mandatory && it.fieldValue.isNullOrBlank()
            }

            val isMandatoryDataMissing =
                (shopperFlow.value.isFullNameEnabled  && shopperFlow.value.isFullNameEditable  && firstName.value.isNullOrEmpty()) ||
                        (shopperFlow.value.isEmailEnabled     && shopperFlow.value.isEmailEditable     && email.value.isNullOrEmpty()) ||
                        (shopperFlow.value.isPhoneEnabled     && shopperFlow.value.isPhoneEditable     && completePhoneNumber.value.isNullOrEmpty()) ||
                        (shopperFlow.value.isShippingAddressEnabled && shopperFlow.value.isShippingAddressEditable && addressFlow.value.address1.isNullOrEmpty()) ||
                        isMandatoryCustomFieldMissing

            if (isMandatoryDataMissing) {
                onProceedAddressScreen(true)
            }
            val showSwipeToPay = !isMandatoryDataMissing &&
                    viewModel.recommendedList.value.isNotEmpty()

            LaunchedEffect(Unit) {
                if(showSwipeToPay && !viewModel.isSwipeToPayVisible.value && !showQROnLoad.value) {
                    viewModel.isSwipeToPayVisible.value = true
                    onShowSwipeToPay()
                }
            }
            val showAddressComponent =
                shopperFlow.value.isShippingAddressEnabled ||
                        shopperFlow.value.isFullNameEnabled ||
                        shopperFlow.value.isEmailEnabled ||
                        shopperFlow.value.isPhoneEnabled

            val isAddressComponentClickable =
                shopperFlow.value.isShippingAddressEditable ||
                        shopperFlow.value.isFullNameEditable ||
                        shopperFlow.value.isEmailEditable ||
                        shopperFlow.value.isPhoneEditable

            val otherPaymentMethodEnabled =
                response.methodFlags.isCardsVisible ||
                        response.methodFlags.isWalletVisible ||
                        response.methodFlags.isNetBankingVisible ||
                        response.methodFlags.isEMIVisible ||
                        response.methodFlags.isBNPLVisible

            val enabledMethods = buildList {
                if (response.methodFlags.isCardsVisible) add("card")
                if (response.methodFlags.isWalletVisible) add("wallet")
                if (response.methodFlags.isNetBankingVisible) add("netbanking")
                if (response.methodFlags.isEMIVisible) add("emi")
                if (response.methodFlags.isBNPLVisible) add("bnpl")
                if (response.methodFlags.isUPIVisible) add("upi")
            }.distinct()

            val shouldAutoNavigate =
                enabledMethods.size == 1 &&
                        !isMandatoryDataMissing &&
                        !showAddressComponent &&
                        !isOrderItemDetailsVisible.value

            LaunchedEffect(shouldAutoNavigate) {
                if (!shouldAutoNavigate) return@LaunchedEffect

                when (enabledMethods.first()) {
                    "card" -> onProceedCardScreen(true)
                    "wallet" -> onProceedWalletScreen(true)
                    "netbanking" -> onProceedNetBankingScreen(true)
                    "emi" -> onProceedEMIScreen(true)
                    "bnpl" -> onProceedBNPLScreen(true)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F6FB))
                    .verticalScroll(rememberScrollState())
            ) {

                // --- Address ---
                if (showAddressComponent) {
                    AddressComponent(
                        address = buildAddressString(),
                        navigateToAddressScreen = {
                            viewModel.removeQRFromView()
                            if (isAddressComponentClickable && shopperToken.value.isNullOrBlank()) onProceedAddressScreen(false)
                            else onProceedSavedAddressScreen()
                        },
                        isEmailEditable = shopperFlow.value.isEmailEditable,
                        isPhoneEditable = shopperFlow.value.isPhoneEditable,
                        isFullNameEditable = shopperFlow.value.isFullNameEditable,
                        isShippingAddressEditable = shopperFlow.value.isShippingAddressEditable,
                        isEmailEnabled = shopperFlow.value.isEmailEnabled,
                        isPhoneEnabled = shopperFlow.value.isPhoneEnabled,
                        isFullNameEnabled = shopperFlow.value.isFullNameEnabled,
                        isShippingAddressEnabled = shopperFlow.value.isShippingAddressEnabled,
                        firstName = firstName.value,
                        lastName = lastName.value,
                        email = email.value,
                        completePhoneNumber = completePhoneNumber.value,
                        labelType = labelFlow.value.first,
                        labelName = labelFlow.value.second
                    )
                }

                if(viewModel.appliedOffers.value.isNotEmpty()) {
                    SectionTitle("Offers & discounts")
                    OfferSection(
                        offers = viewModel.appliedOffers.value,
                        selectedCode = selectedOfferCode,
                        themeColor = buttonColor.value.toComposeColor(),
                        onApply = { offer ->
                            viewModel.removeQRFromView()
                            val amount = amount.value + discountAmount.value
                            viewModel.applyOffer(offer.code, amount)
                            onSetSelectedOfferCode(offer.code)
                        },
                        onRemove = {
                            viewModel.removeQRFromView()
                            viewModel.removeOffer(discountAmount.value, amount.value)
                            onSetSelectedOfferCode("")
                        },
                        onViewAll = {
                            viewModel.removeQRFromView()
                            onProceedInstantOfferScreen()
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if(viewModel.recommendedList.value.isNotEmpty() && response.methodFlags.isUPIVisible) {
                    SectionTitle("Recommended")
                    PaymentSelectorView(
                        providerList      = viewModel.recommendedList.value,
                        onProceedForward  = { display, instrument, _ ->
                            viewModel.callUiAnalytics(
                                event = AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                                screenName = "MainScreen",
                                message = "Payment Category selected through recommended method"
                            )
                            viewModel.postUpiCollectRequest(instrumentRef = instrument, type = "upi/collect", shopperVpa = display)
                        },
                        drawableResource = Res.drawable.ic_upi,
                        onClickRadio = {
                            viewModel.removeQRFromView()
                            selectedRecommendedInstrumentId.value = it
                            selectedUpiInstrumentId.value = ""
                            viewModel.callUiAnalytics(
                                event = AnalyticsEvents.PAYMENT_CATEGORY_SELECTED.value,
                                screenName = "MainScreen",
                                message = "Payment Category selected through recommended method"
                            )
                        },
                        buttonTextColor = buttonTextColor.value,
                        buttonColor = buttonColor.value,
                        currencySymbol = currencySymbol,
                        amount = amount.value,
                        ctaBorderRadius = ctaBorderRadius.value,
                        selectedId = selectedRecommendedInstrumentId.value
                    )
                }

                // --- UPI ---
                if (response.methodFlags.isUPIVisible || response.methodFlags.isUPIOtmVisible) {
                    SectionTitle(if(response.methodFlags.isUPIOtmVisible) "UPI One Time Mandate" else "Pay by any UPI")
                    UPIComponent(
                        methodFlags = response.methodFlags,
                        onClickSavedUpiPayButton = {instrumentRef, shopperVpa ->
                            viewModel.callUiAnalytics(
                                event = AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                                screenName = "MainScreen",
                                message = "Payment Category selected through saved upi"
                            )
                            viewModel.postUpiCollectRequest(instrumentRef = instrumentRef, type = "upi/collect", shopperVpa = shopperVpa)
                        },
                        onClickUpiCollectPayButton = {shopperVpa, saveInstrument->
                            viewModel.callUiAnalytics(
                                event = AnalyticsEvents.PAYMENT_CATEGORY_SELECTED.value,
                                screenName = "MainScreen",
                                message = "Payment Category selected through upi collect"
                            )
                            viewModel.callUiAnalytics(
                                event = AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                                screenName = "MainScreen",
                                message = "Payment Category selected through upi collect"
                            )
                            viewModel.postUpiCollectRequest(shopperVpa = shopperVpa, type = if(response. methodFlags.isUPIOtmCollectVisible ) "upiotm/collect" else "upi/collect", saveInstrument = saveInstrument)
                        },
                        onClickUpiIntentPayButton = {selectedIntent ->
                            viewModel.callUiAnalytics(
                                event = AnalyticsEvents.PAYMENT_CATEGORY_SELECTED.value,
                                screenName = "MainScreen",
                                message = "Payment Category selected through intent"
                            )
                            viewModel.callUiAnalytics(
                                event = AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                                screenName = "MainScreen",
                                message = "Payment Category selected through intent"
                            )
                            viewModel.postUpiIntentRequest(selectedIntent = selectedIntent, type =if(response. methodFlags.isUPIOtmCollectVisible ) "upiotm/intent" else "upi/intent")
                        },
                        onClickUpiQRPayButton = {
                            viewModel.isQRLoaded.value = true
                            viewModel.postUPIQrRequest(if(response. methodFlags.isUPIOtmCollectVisible ) "upiotm/qr" else "upi/qr")
                        },
                        savedUpiList = viewModel.upiRecommendedList.value,
                        onClickRadio = {
                            selectedUpiInstrumentId.value = it
                            selectedRecommendedInstrumentId.value = ""
                            viewModel.callUiAnalytics(
                                event = AnalyticsEvents.PAYMENT_CATEGORY_SELECTED.value,
                                screenName = "MainScreen",
                                message = "Payment Category selected through saved upi"
                            )
                        },
                        onErrorLoadingIntent = {message ->
                            viewModel.callUiAnalytics(
                                event = AnalyticsEvents.UPI_APP_NOT_FOUND.value,
                                screenName = "MainScreen",
                                message = "UPI App related messages $message"
                            )
                        },
                        buttonTextColor = buttonTextColor.value,
                        buttonColor = buttonColor.value,
                        currencySymbol = currencySymbol,
                        amount = amount.value,
                        ctaBorderRadius = ctaBorderRadius.value,
                        focusedTextInputBorderColor = focusedTextInputBorderColor.value,
                        unfocusedTextInputBorderColor = unfocusedTextInputBorderColor.value,
                        shopperToken = shopperToken.value,
                        selectedId = selectedUpiInstrumentId.value,
                        qrTimer = viewModel.qrTimer.value,
                        qrImage = viewModel.qrImage.value,
                        stopFunctionCall = {
                            viewModel.stopFetchStatusPolling()
                        },
                        showQROnLoad = showQROnLoad.value,
                        isQRLoaded = viewModel.isQRLoaded.value,
                        onVpaChanged = {
                            // no operation
                        },
                        onClickIntent = {
                            // no operation
                        }
                    )
                }

                // --- More Payment Methods ---
                if (otherPaymentMethodEnabled) {
                    if(!viewModel.cardsRecommendedList.value.isEmpty()) {
                        SectionTitle("Credit & Debit Cards")
                        SavedCardComponent(
                            savedCards = viewModel.cardsRecommendedList.value,
                            onProceedForward = { instrumentRef, isSICheckboxChecked ->
                                viewModel.callUiAnalytics(
                                    event = AnalyticsEvents.PAYMENT_CATEGORY_SELECTED.value,
                                    screenName = "MainScreen",
                                    message = "Payment Category selected through saved card"
                                )
                                viewModel.callUiAnalytics(
                                    event = AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                                    screenName = "MainScreen",
                                    message = "Payment Category selected through saved card"
                                )
                                viewModel.postSavedCardRequest(instrumentRef = instrumentRef, isSICheckboxChecked = isSICheckboxChecked)
                            },
                            onClickAddCard = {
                                viewModel.removeQRFromView()
                                selectedRecommendedInstrumentId.value = ""
                                selectedUpiInstrumentId.value = ""
                                viewModel.callUiAnalytics(
                                    event = AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                                    screenName = "MainScreen",
                                    message = "Navigated to Add card screen from saved card component"
                                )
                                onProceedCardScreen(false)
                            },
                            onClickDeleteCard = {id , nickname ->
                                viewModel.removeQRFromView()
                                selectedDeleteCardName.value = nickname
                                selectedDeleteCardId.value = id
                                isShowSavedCardDeleteConfirmation.value = true
                            },
                            buttonColor = buttonColor.value,
                            buttonTextColor = buttonTextColor.value,
                            currencySymbol = currencySymbol,
                            amount = amount.value,
                            ctaBorderRadius = ctaBorderRadius.value,
                            isSICheckboxChecked = isSICheckboxChecked.value,
                            isSICheckboxEnabled = isSICheckboxEnabled.value,
                            onClickRadio =  {
                                viewModel.removeQRFromView()
                            }
                        )
                    }
                    SectionTitle(
                        if (response.methodFlags.isUPIVisible) "More Payment Options"
                        else "Payment Options"
                    )
                    MorePaymentMethods(
                        methodFlags      = response.methodFlags,
                        onNavigateToCard =
                            {
                                viewModel.removeQRFromView()
                                viewModel.callUiAnalytics(
                                    event = AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                                    screenName = "MainScreen",
                                    message = "Navigated to Add card screen from normal flow"
                                )
                                if(isPresentInSurchargeModel(surchargeDetails.value, "card")) {
                                    selectedMethod.value = "card"
                                    showUpdatedAmountBottomSheet.value = true
                                } else{
                                    onProceedCardScreen(false)
                                }
                            },
                        onNavigateToWallet      =
                            {
                                viewModel.removeQRFromView()
                                viewModel.callUiAnalytics(
                                    event = AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                                    screenName = "MainScreen",
                                    message = "Navigated to Wallet screen"
                                )
                                if(isPresentInSurchargeModel(surchargeDetails.value, "wallet")) {
                                    selectedMethod.value = "wallet"
                                    showUpdatedAmountBottomSheet.value = true
                                } else{
                                    onProceedWalletScreen(false)
                                }
                            },
                        onNavigateToNetBanking  =
                            {
                                viewModel.removeQRFromView()
                                viewModel.callUiAnalytics(
                                    event = AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                                    screenName = "MainScreen",
                                    message = "Navigated to NetBanking screen"
                                )
                                if(isPresentInSurchargeModel(surchargeDetails.value, "netbanking")) {
                                    selectedMethod.value = "netbanking"
                                    showUpdatedAmountBottomSheet.value = true
                                } else{
                                    onProceedNetBankingScreen(false)
                                }
                            },
                        onNavigateToEmi         =
                            {
                                viewModel.removeQRFromView()
                                viewModel.callUiAnalytics(
                                    event = AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                                    screenName = "MainScreen",
                                    message = "Navigated to EMI screen"
                                )
                                if(isPresentInSurchargeModel(surchargeDetails.value, "emi")) {
                                    selectedMethod.value = "emi"
                                    showUpdatedAmountBottomSheet.value = true
                                } else{
                                    onProceedEMIScreen(false)
                                }
                            },
                        onNavigateToBNPL        =
                            {
                                viewModel.removeQRFromView()
                                viewModel.callUiAnalytics(
                                    event = AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                                    screenName = "MainScreen",
                                    message = "Navigated to BNPL screen"
                                )
                                if(isPresentInSurchargeModel(surchargeDetails.value, "buynowpaylater")) {
                                    selectedMethod.value = "buynowpaylater"
                                    showUpdatedAmountBottomSheet.value = true
                                } else{
                                    onProceedBNPLScreen(false)
                                }
                            },
                        savedCardsList = viewModel.cardsRecommendedList.value,
                        surchargeList = surchargeDetails.value,
                        currencySymbol = currencySymbol
                    )
                }

                if (isOrderItemDetailsVisible.value) {
                    SectionTitle("Order Details")
                    OrderDetails(
                        totalAmount = amount.value,
                        itemsArray = response.orderDetails?.items ?: emptyList(),
                        subTotalAmount = response.orderDetails?.subTotalAmount ?: 0.0,
                        shippingAmount = response.orderDetails?.shippingAmount ?: 0.0,
                        taxAmount = response.orderDetails?.taxAmount ?: 0.0,
                        surchargeDetails = surchargeDetails.value,
                        selectedPaymentMethod = "Upi",
                        currencySymbol = currencySymbol
                    )
                }
                Spacer(Modifier.weight(1f))
                Footer()
            }
        }
    }

    if(boxPayAnimationVisible) {
        ShowLoadingComponent(Modifier.fillMaxSize())
    }

    LaunchedEffect(viewModel.upiIntentUrl.value) {
        if(viewModel.upiIntentUrl.value.isNotEmpty()) {
            launchUpiIntent(
                url = viewModel.upiIntentUrl.value,
                onFailure = {error ->
                    viewModel.callUiAnalytics(
                        event = AnalyticsEvents.FAILED_TO_LAUNCH_UPI_INTENT.value,
                        screenName = "MainScreen",
                        message = "UPI intent not launched $error"
                    )
                    viewModel.isBoxPayAnimationLoading.value = false
                    CheckoutDetailsHandler.setSessionFailed()
                    viewModel.upiIntentUrl.value = ""
                },
                onSuccess = {
                    viewModel.isUpiOpening.value = true
                }
            )
        }
    }

    if(isToastVisible) {
        AlertDialog(
            onDismissRequest = {
                viewModel.isToastVisible.value = false
            },
            title = {
                Text(
                    "Unable to Remove Card",
                    fontFamily = LocalSDKFonts.current.primary
                )
            },
            text = {
                Text(
                    "We couldn't remove your saved card due to an issue. Your card is still saved.",
                    fontFamily = LocalSDKFonts.current.primary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.isToastVisible.value = false
                }) {
                    Text("Cancel", fontFamily = LocalSDKFonts.current.primary)
                }
            }
        )
    }

    if(isShowSavedCardDeleteConfirmation.value) {
        AlertDialog(
            onDismissRequest = {
                isShowSavedCardDeleteConfirmation.value = false
            },
            title = {
                Text(
                    "Do you actually want to delete the saved Card ${selectedDeleteCardName.value}?",
                    fontFamily = LocalSDKFonts.current.primary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    isShowSavedCardDeleteConfirmation.value = false
                    viewModel.onClickDeleteSavedCard(selectedDeleteCardId.value)
                }) {
                    Text("Yes", fontFamily = LocalSDKFonts.current.primary, fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    isShowSavedCardDeleteConfirmation.value = false
                }) {
                    Text("No", fontFamily = LocalSDKFonts.current.primary, fontSize = 14.sp)
                }
            }
        )
    }

    LaunchedEffect(viewModel.proceedToTimer.value) {
        if(viewModel.proceedToTimer.value) {
            viewModel.proceedToTimer.value = false
            viewModel.startFetchStatusPolling("")
            onProceedUPITimerScreen(viewModel.upiId.value)
        }
    }

    if (showUpdatedAmountBottomSheet.value) {
        ShowUpdateAmountBottomSheet(
            selectedMethod = selectedMethod.value,
            onClickProceed = {
                showUpdatedAmountBottomSheet.value = false
                when (selectedMethod.value) {
                    "card"       -> {
                        onProceedCardScreen(false)
                        selectedMethod.value = ""
                    }
                    "wallet"     -> {
                        onProceedWalletScreen(false)
                        selectedMethod.value = ""
                    }
                    "netbanking" -> {
                        onProceedNetBankingScreen(false)
                        selectedMethod.value = ""
                    }
                    "emi"        -> {
                        onProceedEMIScreen(false)
                        selectedMethod.value = ""
                    }
                    "buynowpaylater"       -> {
                        onProceedBNPLScreen(false)
                        selectedMethod.value = ""
                    }
                }
            },
            onClick = {
                showUpdatedAmountBottomSheet.value = false
                selectedMethod.value = ""
            },
            surchargeDetails = surchargeDetails.value,
            currencySymbol = currencySymbol,
            amount = amount.value,
            ctaBorderRadius = ctaBorderRadius.value,
            buttonColor = buttonColor.value,
            buttonTextColor = buttonTextColor.value
        )
    }

    if(showWebView) {
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