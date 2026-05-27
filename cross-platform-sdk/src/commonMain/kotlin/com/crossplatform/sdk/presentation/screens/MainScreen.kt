package com.crossplatform.sdk.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.crossplatform.sdk.presentation.components.OrderDetails
import com.crossplatform.sdk.presentation.components.PaymentSelectorView
import com.crossplatform.sdk.presentation.components.SavedCardComponent
import com.crossplatform.sdk.presentation.components.ShimmerView
import com.crossplatform.sdk.presentation.components.ShowLoadingComponent
import com.crossplatform.sdk.presentation.components.UPIComponent
import com.crossplatform.sdk.presentation.launchUpiIntent
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import com.crossplatform.sdk.presentation.viewmodel.MainScreenViewModel
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_upi
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun MainScreen(
    viewModel : MainScreenViewModel,
    onProceedCardScreen: () -> Unit,
    onProceedEMIScreen: () -> Unit,
    onProceedAddressScreen: (isNewAddress : Boolean) -> Unit,
    onProceedSavedAddressScreen: () -> Unit,
    onProceedNetBankingScreen: () -> Unit,
    onProceedWalletScreen: () -> Unit,
    onProceedBNPLScreen: () -> Unit,
    onProceedUPITimerScreen: (shopperVpa: String) -> Unit,
    onShowSwipeToPay : () -> Unit
) {
    val screenState by viewModel.state.collectAsStateWithLifecycle()
    val boxPayAnimationVisible by viewModel.isBoxPayAnimationLoading.collectAsStateWithLifecycle()
    val checkoutDetails by CheckoutDetailsHandler.checkoutDetailsFlow
        .collectAsStateWithLifecycle()
    val userDetails by UserDataHandler.userDataFlow
        .collectAsStateWithLifecycle()
    val showWebView by viewModel.showWebViewScreen.collectAsStateWithLifecycle()
    val isSwipeToPayVisible = remember {
        mutableStateOf(false)
    }
    val selectedDeleteCardId = remember {
        mutableStateOf("")
    }
    val isShowSavedCardDeleteConfirmation = remember {
        mutableStateOf(false)
    }
    val selectedDeleteCardName = remember {
        mutableStateOf("")
    }
    val isToastVisible by viewModel.isToastVisible.collectAsStateWithLifecycle()


    when (screenState) {
        is UiState.Loading -> ShimmerView()
        is UiState.Error   -> {
            val message = (screenState as UiState.Error).message
            Text("Welcome to error screen $message")
            viewModel.callUiAnalytics(
                event = AnalyticsEvents.SDK_CRASH.value,
                screenName = "MainScreen",
                message = "Main Screen not loaded $message"
            )
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

            val isMandatoryDataMissing =
                (checkoutDetails.isFullNameEnabled  && checkoutDetails.isFullNameEditable  && userDetails.firstName.isNullOrEmpty()) ||
                        (checkoutDetails.isEmailEnabled     && checkoutDetails.isEmailEditable     && userDetails.email.isNullOrEmpty()) ||
                        (checkoutDetails.isPhoneEnabled     && checkoutDetails.isPhoneEditable     && userDetails.completePhoneNumber.isNullOrEmpty()) ||
                        (checkoutDetails.isShippingAddressEnabled && checkoutDetails.isShippingAddressEditable && userDetails.address1.isNullOrEmpty())

            if (isMandatoryDataMissing) {
                onProceedAddressScreen(true)
            }
            val showSwipeToPay = !isMandatoryDataMissing &&
                    viewModel.recommendedList.value.isNotEmpty()

            LaunchedEffect(Unit) {
                if(showSwipeToPay && !isSwipeToPayVisible.value) {
                    isSwipeToPayVisible.value = true
                    onShowSwipeToPay()
                }
            }
            val showAddressComponent =
                checkoutDetails.isShippingAddressEnabled ||
                        checkoutDetails.isFullNameEnabled ||
                        checkoutDetails.isEmailEnabled ||
                        checkoutDetails.isPhoneEnabled

            val isAddressComponentClickable =
                checkoutDetails.isShippingAddressEditable ||
                        checkoutDetails.isFullNameEditable ||
                        checkoutDetails.isEmailEditable ||
                        checkoutDetails.isPhoneEditable

            val otherPaymentMethodEnabled =
                response.methodFlags.isCardsVisible ||
                        response.methodFlags.isWalletVisible ||
                        response.methodFlags.isNetBankingVisible ||
                        response.methodFlags.isEMIVisible ||
                        response.methodFlags.isBNPLVisible

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F6FB))
                    .verticalScroll(rememberScrollState())
            ) {

                // --- Address ---
                if (showAddressComponent) {
                    AddressComponent(
                        address = buildAddressString(checkoutDetails, userDetails),
                        navigateToAddressScreen = {
                            if (isAddressComponentClickable && checkoutDetails.shopperToken.isNullOrBlank()) onProceedAddressScreen(false)
                            else onProceedSavedAddressScreen()
                        },
                        checkoutDetails = checkoutDetails,
                        userData = userDetails
                    )
                }

                if(!viewModel.recommendedList.value.isEmpty()) {
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
                        checkoutDetails = checkoutDetails,
                        drawableResource = Res.drawable.ic_upi,
                        onClickRadio = {
                            viewModel.callUiAnalytics(
                                event = AnalyticsEvents.PAYMENT_CATEGORY_SELECTED.value,
                                screenName = "MainScreen",
                                message = "Payment Category selected through recommended method"
                            )
                        }
                    )
                }

                // --- UPI ---
                if (response.methodFlags.isUPIVisible || response.methodFlags.isUPIOtmVisible) {
                    SectionTitle("Pay by any UPI")
                    UPIComponent(
                        methodFlags = response.methodFlags,
                        checkoutDetails = checkoutDetails,
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

                        },
                        savedUpiList = viewModel.upiRecommendedList.value,
                        onClickRadio = {
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
                                viewModel.callUiAnalytics(
                                    event = AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                                    screenName = "MainScreen",
                                    message = "Navigated to Add card screen from saved card component"
                                )
                                onProceedCardScreen()
                            },
                            checkoutDetails = checkoutDetails,
                            onClickDeleteCard = {id , nickname ->
                                selectedDeleteCardName.value = nickname
                                selectedDeleteCardId.value = id
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
                                viewModel.callUiAnalytics(
                                    event = AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                                    screenName = "MainScreen",
                                    message = "Navigated to Add card screen from normal flow"
                                )
                                onProceedCardScreen()
                            },
                        onNavigateToWallet      =
                            {
                                viewModel.callUiAnalytics(
                                    event = AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                                    screenName = "MainScreen",
                                    message = "Navigated to Wallet screen"
                                )
                                onProceedWalletScreen()
                            },
                        onNavigateToNetBanking  =
                            {
                                viewModel.callUiAnalytics(
                                    event = AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                                    screenName = "MainScreen",
                                    message = "Navigated to NetBanking screen"
                                )
                                onProceedNetBankingScreen()
                            },
                        onNavigateToEmi         =
                            {
                                viewModel.callUiAnalytics(
                                    event = AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                                    screenName = "MainScreen",
                                    message = "Navigated to EMI screen"
                                )
                                onProceedEMIScreen()
                            },
                        onNavigateToBNPL        =
                            {
                                viewModel.callUiAnalytics(
                                    event = AnalyticsEvents.PAYMENT_METHOD_SELECTED.value,
                                    screenName = "MainScreen",
                                    message = "Navigated to BNPL screen"
                                )
                                onProceedBNPLScreen()
                            },
                        savedCardsList = viewModel.cardsRecommendedList.value,
                        surchargeList = checkoutDetails.surchargeDetails
                    )
                }

                if (checkoutDetails.isOrderItemDetailsVisible) {
                    SectionTitle("Order Details")
                    OrderDetails(
                        totalAmount = checkoutDetails.amount,
                        itemsArray = response.orderDetails?.items ?: emptyList(),
                        subTotalAmount = response.orderDetails?.subTotalAmount ?: 0.0,
                        shippingAmount = response.orderDetails?.shippingAmount ?: 0.0,
                        taxAmount = response.orderDetails?.taxAmount ?: 0.0,
                        surchargeDetails = checkoutDetails.surchargeDetails,
                        selectedPaymentMethod = "Upi",
                        currencySymbol = checkoutDetails.currencySymbol
                    )
                }
                Spacer(Modifier.weight(1f))
                Footer()
            }
        }
    }

    if(boxPayAnimationVisible) {
        ShowLoadingComponent()
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
                    fontFamily = defaultFontFamily
                )
            },
            text = {
                Text(
                    "We couldn't remove your saved card due to an issue. Your card is still saved.",
                    fontFamily = defaultFontFamily
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.isToastVisible.value = false
                }) {
                    Text("Cancel", fontFamily = defaultFontFamily)
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
                    fontFamily = defaultFontFamily
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    isShowSavedCardDeleteConfirmation.value = false
                    viewModel.onClickDeleteSavedCard(selectedDeleteCardId.value)
                }) {
                    Text("Yes", fontFamily = defaultFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    isShowSavedCardDeleteConfirmation.value = false
                }) {
                    Text("No", fontFamily = defaultFontFamily)
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