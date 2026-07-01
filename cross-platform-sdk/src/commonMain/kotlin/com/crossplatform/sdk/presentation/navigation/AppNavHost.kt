package com.crossplatform.sdk.presentation.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.crossplatform.sdk.data.ServiceRequest
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.handler.CommonSDKDismissHandler
import com.crossplatform.sdk.data.handler.SDKJobHandler
import com.crossplatform.sdk.data.handler.SDKPaymentResponseHandler
import com.crossplatform.sdk.data.handler.UserDataHandler
import com.crossplatform.sdk.data.model.AnalyticsEvents
import com.crossplatform.sdk.data.model.SDKPaymentResponse
import com.crossplatform.sdk.presentation.buildAddressAndUserDetailsString
import com.crossplatform.sdk.presentation.components.PaymentFailed
import com.crossplatform.sdk.presentation.components.PaymentSuccessful
import com.crossplatform.sdk.presentation.components.SessionExpire
import com.crossplatform.sdk.presentation.components.SwipeToPayComponent
import com.crossplatform.sdk.presentation.components.TopBar
import com.crossplatform.sdk.presentation.screens.AddressScreen
import com.crossplatform.sdk.presentation.screens.BNPLScreen
import com.crossplatform.sdk.presentation.screens.CardScreen
import com.crossplatform.sdk.presentation.screens.EMIScreen
import com.crossplatform.sdk.presentation.screens.InstantOfferScreen
import com.crossplatform.sdk.presentation.screens.MainScreen
import com.crossplatform.sdk.presentation.screens.NetBankingScreen
import com.crossplatform.sdk.presentation.screens.SavedAddressScreen
import com.crossplatform.sdk.presentation.screens.ScreenBackInterceptor
import com.crossplatform.sdk.presentation.screens.UpiTimerScreen
import com.crossplatform.sdk.presentation.screens.WalletScreen
import com.crossplatform.sdk.presentation.viewmodel.MainScreenViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val shopperDetails by CheckoutDetailsHandler.shopperFieldsConfigFlow.collectAsStateWithLifecycle()
    val isShippingAddressEnabled = shopperDetails.isShippingAddressEnabled
    val isShippingAddressEditable = shopperDetails.isShippingAddressEditable
    val isFullNameEnabled = shopperDetails.isFullNameEnabled
    val isFullNameEditable = shopperDetails.isFullNameEditable
    val isEmailEnabled = shopperDetails.isEmailEnabled
    val isEmailEditable = shopperDetails.isEmailEditable
    val isPhoneEnabled = shopperDetails.isPhoneEnabled
    val isPhoneEditable = shopperDetails.isPhoneEditable
    val currencyFlow = CheckoutDetailsHandler.currencyFlow.collectAsStateWithLifecycle()
    val (currencySymbol, _) = currencyFlow.value
    val isWebViewVisible = CheckoutDetailsHandler.isWebViewVisibleFlow.collectAsStateWithLifecycle()
    val surchargeDetails = CheckoutDetailsHandler.surchargeDetailsFlow.collectAsStateWithLifecycle()
    val amountBeforeSurcharge = CheckoutDetailsHandler.amountBeforeSurchargeFlow.collectAsStateWithLifecycle()
    val discountAmount = CheckoutDetailsHandler.discountAmountFlow.collectAsStateWithLifecycle()
    val amount = CheckoutDetailsHandler.amountFlow.collectAsStateWithLifecycle()
    val buttonColor = CheckoutDetailsHandler.buttonColorFlow.collectAsStateWithLifecycle()
    val buttonTextColor = CheckoutDetailsHandler.buttonTextColorFlow.collectAsStateWithLifecycle()
    val isPaymentFailed = CheckoutDetailsHandler.isPaymentFailedFlow.collectAsStateWithLifecycle()
    val isPaymentSuccessful = CheckoutDetailsHandler.isPaymentSuccessfulFlow.collectAsStateWithLifecycle()
    val isSessionExpired = CheckoutDetailsHandler.isSessionExpiredFlow.collectAsStateWithLifecycle()
    val successDetails = CheckoutDetailsHandler.successfulTimestampFlow.collectAsStateWithLifecycle()
    val (successTimeStamp , selectedPaymentMethod) = successDetails.value

    val navController = rememberNavController()
    val viewModel: MainScreenViewModel = koinViewModel()
    var showSwipeToPay by remember { mutableStateOf(false) }


    // Track current route to set the correct screen title
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val isNewAddress =
        currentEntry?.arguments?.getBoolean("isNewAddress") ?: false

    val failedSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope            = rememberCoroutineScope()

    fun hideFailedSheet() {
        scope.launch {
            CheckoutDetailsHandler.setSessionFailed()
            failedSheetState.hide()
        }
    }

    val baseRoute = currentRoute?.substringBefore("/{")

    val routeTitle = when (baseRoute) {
        Routes.MainScreen.route    -> "Payment Details"
        Routes.AddressScreen.route -> {
            when {
                isShippingAddressEnabled && isNewAddress   -> "Add Address"
                isShippingAddressEnabled && !isNewAddress  -> "Edit Address"
                !isShippingAddressEnabled && isNewAddress  -> "Add Personal Details"
                else -> "Edit Personal Details"
            }
        }
        Routes.CardScreen.route -> "Pay via Card"
        Routes.NetBankingScreen.route  -> "Select Bank"
        Routes.WalletScreen.route -> "Select Wallet"
        Routes.SavedAddressScreen.route -> "Your Addresses"
        Routes.BNPLScreen.route -> "Select BNPL"
        else -> "Payment Details"
    }

    val screenTitle = ScreenBackInterceptor.currentTitle?.invoke() ?: routeTitle

    val selectedOfferCode = remember {
        mutableStateOf("")
    }

    Column (modifier = Modifier.fillMaxSize()) {
        if(!viewModel.isLoadingSession.value && !isWebViewVisible.value) {
            TopBar(
                showDesc   = true,
                text       = screenTitle,             // changes per screen
                onBackPress = {
                    if (ScreenBackInterceptor.onBack?.invoke() == true) {
                        // no operation to be performed as it is already handled in EMI screen
                    }
                    else if (!navController.popBackStack() || (isNewAddress && baseRoute == Routes.AddressScreen.route)) {
                        callSDKPaymentResponse()
                    } else if (!navController.popBackStack()) {
                        callSDKPaymentResponse()
                    }else {
                        if(!surchargeDetails.value.isEmpty()) {
                            CheckoutDetailsHandler.setAmount(amountBeforeSurcharge.value)
                        }
                    }
                },    // from API via ViewModel
                sessionSeconds = viewModel.sessionSeconds.value,
            )
        }
        NavHost(
            navController = navController,
            startDestination = Routes.MainScreen.route
        ) {
            composable(Routes.MainScreen.route) {
                MainScreen(
                    viewModel = viewModel,
                    onProceedCardScreen = { isAutoNavigationEnabled ->
                        navController.navigate("${Routes.CardScreen.route}/$isAutoNavigationEnabled")
                    },
                    onProceedEMIScreen = { isAutoNavigationEnabled ->
                        navController.navigate("${Routes.EMIScreen.route}/$isAutoNavigationEnabled")
                    },
                    onProceedAddressScreen = {isNewAddress ->
                        navController.navigate("${Routes.AddressScreen.route}/${isNewAddress}")
                    },
                    onProceedSavedAddressScreen = {
                        navController.navigate(Routes.SavedAddressScreen.route)
                    },
                    onProceedNetBankingScreen = { isAutoNavigationEnabled ->
                        navController.navigate("${Routes.NetBankingScreen.route}/$isAutoNavigationEnabled")
                    },
                    onProceedWalletScreen = { isAutoNavigationEnabled ->
                        navController.navigate("${Routes.WalletScreen.route}/$isAutoNavigationEnabled")
                    },
                    onProceedBNPLScreen = { isAutoNavigationEnabled ->
                        navController.navigate("${Routes.BNPLScreen.route}/$isAutoNavigationEnabled")
                    },
                    onProceedUPITimerScreen = { shopperVpa ->
                        navController.navigate("${Routes.UpiTimerScreen.route}/$shopperVpa")
                    },
                    onShowSwipeToPay = {
                        showSwipeToPay = true
                    },
                    selectedOfferCode = selectedOfferCode.value,
                    onSetSelectedOfferCode = {
                        selectedOfferCode.value = it
                    },
                    onProceedInstantOfferScreen = {
                        navController.navigate(Routes.InstantOfferScreen.route)
                    }
                )
            }

            composable(
                route = "${Routes.UpiTimerScreen.route}/{shopperVpa}",
                arguments = listOf(navArgument("shopperVpa") { type = NavType.StringType })
            ) { backStackEntry ->
                val shopperVpa = backStackEntry.arguments?.getString("shopperVpa") ?: ""
                UpiTimerScreen(
                    shopperVpa  = shopperVpa,
                    onBackPress = {
                        navController.popBackStack()
                    },
                    buttonColor = buttonColor.value,
                    buttonTextColor = buttonTextColor.value
                )
            }

            composable(
                route = "${Routes.CardScreen.route}/{isAutoNavigationEnabled}",
                arguments = listOf(navArgument("isAutoNavigationEnabled") { type = NavType.BoolType })
            ) { backStackEntry ->
                val isAutoNavigationEnabled = backStackEntry.arguments?.getBoolean("isAutoNavigationEnabled") ?: false
                CardScreen(
                    onBackPress = {
                        if(!surchargeDetails.value.isEmpty()) {
                            CheckoutDetailsHandler.setAmount(amountBeforeSurcharge.value)
                        }
                        navController.popBackStack()
                    },
                    isAutoNavigationEnabled = isAutoNavigationEnabled,
                    onExitCheckout = {
                        callSDKPaymentResponse()
                    }
                )
            }

            composable(
                route = "${Routes.EMIScreen.route}/{isAutoNavigationEnabled}",
                arguments = listOf(navArgument("isAutoNavigationEnabled") { type = NavType.BoolType })
            ) { backStackEntry ->
                val isAutoNavigationEnabled = backStackEntry.arguments?.getBoolean("isAutoNavigationEnabled") ?: false
                EMIScreen(
                    onBackPress = {
                        if(!surchargeDetails.value.isEmpty()) {
                            CheckoutDetailsHandler.setAmount(amountBeforeSurcharge.value)
                        }
                        navController.popBackStack()
                    },
                    isAutoNavigationEnabled = isAutoNavigationEnabled,
                    onExitCheckout = {
                        callSDKPaymentResponse()
                    }
                )
            }

            composable(
                route = "${Routes.AddressScreen.route}/{isNewAddress}",
                arguments = listOf(navArgument("isNewAddress") { type = NavType.BoolType })
            ) {_ ->
                AddressScreen(
                    onAddressSaved = {
                        viewModel.callUiAnalytics(
                            event = AnalyticsEvents.ADDRESS_UPDATED.value,
                            screenName = "App nav host",
                            message = "Address Updated successfully"
                        )
                        navController.popBackStack()
                    },
                    onBackPress = {
                        navController.popBackStack()
                    },
                    isEmailEnabled = isEmailEnabled,
                    isFullNameEnabled = isFullNameEnabled,
                    isPhoneEnabled = isPhoneEditable,
                    isShippingEnabled = isShippingAddressEnabled
                )
            }

            composable(Routes.SavedAddressScreen.route) {
                SavedAddressScreen(
                    onBackPress = {
                        navController.popBackStack()
                    },
                    buttonColor = buttonColor.value
                )
            }

            composable(
                route = "${Routes.BNPLScreen.route}/{isAutoNavigationEnabled}",
                arguments = listOf(navArgument("isAutoNavigationEnabled") { type = NavType.BoolType })
            ) { backStackEntry ->
                val isAutoNavigationEnabled = backStackEntry.arguments?.getBoolean("isAutoNavigationEnabled") ?: false
                BNPLScreen(
                    onBackPress = {
                        if(!surchargeDetails.value.isEmpty()) {
                            CheckoutDetailsHandler.setAmount(amountBeforeSurcharge.value)
                        }
                        navController.popBackStack()
                    },
                    isAutoNavigationEnabled = isAutoNavigationEnabled,
                    onExitCheckout = {
                        callSDKPaymentResponse()
                    }
                )
            }

            composable(
                route = "${Routes.NetBankingScreen.route}/{isAutoNavigationEnabled}",
                arguments = listOf(navArgument("isAutoNavigationEnabled") { type = NavType.BoolType })
            ) { backStackEntry ->
                val isAutoNavigationEnabled = backStackEntry.arguments?.getBoolean("isAutoNavigationEnabled") ?: false
                NetBankingScreen(
                    onBackPress = {
                        if(!surchargeDetails.value.isEmpty()) {
                            CheckoutDetailsHandler.setAmount(amountBeforeSurcharge.value)
                        }
                        navController.popBackStack()
                    },
                    isAutoNavigationEnabled = isAutoNavigationEnabled,
                    onExitCheckout = {
                        callSDKPaymentResponse()
                    }
                )
            }

            composable(
                route = "${Routes.WalletScreen.route}/{isAutoNavigationEnabled}",
                arguments = listOf(navArgument("isAutoNavigationEnabled") { type = NavType.BoolType })
            ) { backStackEntry ->
                val isAutoNavigationEnabled = backStackEntry.arguments?.getBoolean("isAutoNavigationEnabled") ?: false
                WalletScreen(
                    onBackPress = {
                        if(!surchargeDetails.value.isEmpty()) {
                            CheckoutDetailsHandler.setAmount(amountBeforeSurcharge.value)
                        }
                        navController.popBackStack()
                    },
                    isAutoNavigationEnabled = isAutoNavigationEnabled,
                    onExitCheckout = {
                        callSDKPaymentResponse()
                    }
                )
            }

            composable(
                route = Routes.InstantOfferScreen.route,
            ) {
                InstantOfferScreen(
                    selectedCode = selectedOfferCode.value,
                    onBackPress = {
                        navController.popBackStack()
                    },
                    onClickRemove = {
                        selectedOfferCode.value = ""
                        viewModel.removeOffer(discountAmount.value, amount.value)
                        navController.popBackStack()
                    },
                    onClickApply = {
                        selectedOfferCode.value = it
                        val amount = amount.value + discountAmount.value
                        viewModel.applyOffer(it, amount)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
    if(showSwipeToPay) {
        val firstInstrument = viewModel.recommendedList.value[0]
        ModalBottomSheet(
            onDismissRequest = {
                showSwipeToPay = false
            },
            dragHandle       = null,
            containerColor   = Color.White,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            SwipeToPayComponent(
                buttonColor = buttonColor.value,
                buttonTextColor = buttonTextColor.value,
                amount = amount.value,
                currencySymbol = currencySymbol,
                lastUsedUpi = firstInstrument.displayValue,
                logoUrl = firstInstrument.imageUrl,
                address = buildAddressAndUserDetailsString(),
                toShowAddress = isShippingAddressEnabled ||
                        isFullNameEnabled ||
                        isEmailEnabled ||
                        isPhoneEnabled,
                toShowPersonal = !isShippingAddressEnabled,
                toShowOnChangeAddressClick = isShippingAddressEditable ||
                        isFullNameEditable ||
                        isEmailEditable ||
                        isPhoneEditable,
                onClickMoreOptions = {
                    // Replace swipe screen with full MainScreen in the same sheet
                    showSwipeToPay = false
                },
                onSwipeComplete = {
                    showSwipeToPay = false
                    viewModel.postUpiCollectRequest(
                        instrumentRef = firstInstrument.instrumentType,
                        type = if (firstInstrument.type.equals(
                                "upi",
                                true
                            )
                        ) "upi/collect" else "card/token",
                        shopperVpa = firstInstrument.displayValue
                    )
                },
                onClickChangeAddress = {
                    showSwipeToPay = false
                    navController.navigate(Routes.SavedAddressScreen.route)
                }
            )
        }
    }
    if (isSessionExpired.value) {
        viewModel.stopSessionCountDown()
        viewModel.stopFetchStatusPolling()
        viewModel.qrTimer.value = 0
        SessionExpire (
            onClick = {
                viewModel.callUiAnalytics(
                    event = AnalyticsEvents.PAYMENT_RESULT_SCREEN_DISPLAYED.value,
                    screenName = "App nav host",
                    message = "Session expired button clicked"
                )
                CheckoutDetailsHandler.setSessionExpired()
                callSDKPaymentResponse()
            }
        )
    }
    if (isPaymentSuccessful.value) {
        viewModel.stopSessionCountDown()
        viewModel.stopFetchStatusPolling()
        viewModel.qrTimer.value = 0
        PaymentSuccessful (
            dateNTime = successTimeStamp,
            paymentMethod = selectedPaymentMethod,
            onClick = {
                viewModel.callUiAnalytics(
                    event = AnalyticsEvents.PAYMENT_RESULT_SCREEN_DISPLAYED.value,
                    screenName = "App nav host",
                    message = "Payment successful"
                )
                callSDKPaymentResponse()
            }
        )
    }
    if (isPaymentFailed.value) {
        viewModel.qrTimer.value = 0
        PaymentFailed(
            sheetState = failedSheetState,
            onClick = {
                viewModel.callUiAnalytics(
                    event = AnalyticsEvents.PAYMENT_RESULT_SCREEN_DISPLAYED.value,
                    screenName = "App nav host",
                    message = "Payment failed"
                )
                hideFailedSheet()
            },
            onDismiss = {
                viewModel.callUiAnalytics(
                    event = AnalyticsEvents.PAYMENT_RESULT_SCREEN_DISPLAYED.value,
                    screenName = "App nav host",
                    message = "Exit checkout through payment failed modal"
                )
                callSDKPaymentResponse()
            }
        )
    }
}


fun callSDKPaymentResponse() {
    SDKJobHandler.cancelAll()

    // ✅ Capture data BEFORE resetting
    val (status, transactionId) = CheckoutDetailsHandler.transactionFlow.value
    val inquiryToken = CheckoutDetailsHandler.inquiryTokenFlow.value

    ServiceRequest.close()

    // ✅ Reset first so UI clears immediately
    CheckoutDetailsHandler.resetToDefault()
    UserDataHandler.resetToDefault()

    // ✅ Notify after reset
    SDKPaymentResponseHandler.notifyResult(
        result = SDKPaymentResponse(
            status = status,
            transactionId = transactionId,
            inquiryToken = inquiryToken
        )
    )

    CommonSDKDismissHandler.notifyToCloseSDK()
}