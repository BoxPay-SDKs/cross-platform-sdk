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
import com.crossplatform.sdk.presentation.screens.UpiTimerScreen
import com.crossplatform.sdk.presentation.screens.WalletScreen
import com.crossplatform.sdk.presentation.viewmodel.MainScreenViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val checkoutDetails by CheckoutDetailsHandler.checkoutDetailsFlow
        .collectAsStateWithLifecycle()
    val userDetails by UserDataHandler.userDataFlow.collectAsStateWithLifecycle()
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

    val screenTitle = when (currentRoute) {
        Routes.MainScreen.route    -> "Payment Details"
        Routes.AddressScreen.route -> {
            when {
                checkoutDetails.isShippingAddressEnabled && isNewAddress   -> "Add Address"
                checkoutDetails.isShippingAddressEnabled && !isNewAddress  -> "Edit Address"
                !checkoutDetails.isShippingAddressEnabled && isNewAddress  -> "Add Personal Details"
                else                                -> "Edit Personal Details"
            }
        }
        Routes.CardScreen.route -> "Pay via Card"
        Routes.NetBankingScreen.route  -> "Select Bank"
        Routes.WalletScreen.route -> "Select Wallet"
        Routes.BNPLScreen.route -> "Select BNPL"
        Routes.EMIScreen.route -> "Choose EMI Option"
        else                           -> "Payment Details"
    }

    Column (modifier = Modifier.fillMaxSize()) {
        if(!viewModel.isLoadingSession.value && !checkoutDetails.isWebViewVisible) {
            TopBar(
                showDesc   = true,
                showSecure = true,
                text       = screenTitle,             // changes per screen
                onBackPress = {
                    if (!navController.popBackStack()) {
                        callSDKPaymentResponse()
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
                    onProceedCardScreen = {
                        navController.navigate(Routes.CardScreen.route)
                    },
                    onProceedEMIScreen = {
                        navController.navigate(Routes.EMIScreen.route)
                    },
                    onProceedAddressScreen = {isNewAddress ->
                        navController.navigate("${Routes.AddressScreen.route}/${isNewAddress}")
                    },
                    onProceedSavedAddressScreen = {
                        navController.navigate(Routes.SavedAddressScreen.route)
                    },
                    onProceedNetBankingScreen = {
                        navController.navigate(Routes.NetBankingScreen.route)
                    },
                    onProceedWalletScreen = {
                        navController.navigate(Routes.WalletScreen.route)
                    },
                    onProceedBNPLScreen = {
                        navController.navigate(Routes.BNPLScreen.route)
                    },
                    onProceedUPITimerScreen = {shopperVpa ->
                        navController.navigate("${Routes.UpiTimerScreen.route}/$shopperVpa")
                    },
                    onShowSwipeToPay = {
                        showSwipeToPay = true
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
                    }
                )
            }

            composable(Routes.CardScreen.route) {
                CardScreen()
            }

            composable(Routes.EMIScreen.route) {
                EMIScreen()
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
                    }
                )
            }

            composable(Routes.SavedAddressScreen.route) {
                SavedAddressScreen()
            }

            composable(Routes.BNPLScreen.route) {
                BNPLScreen()
            }

            composable(Routes.NetBankingScreen.route) {
                NetBankingScreen()
            }

            composable(Routes.WalletScreen.route) {
                WalletScreen()
            }

            composable(Routes.InstantOfferScreen.route) {
                InstantOfferScreen(
                    onBackPress = {
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
                buttonColor = checkoutDetails.buttonColor,
                buttonTextColor = checkoutDetails.buttonTextColor,
                amount = checkoutDetails.amount,
                currencySymbol = checkoutDetails.currencySymbol,
                lastUsedUpi = firstInstrument.displayValue,
                logoUrl = firstInstrument.imageUrl,
                address = buildAddressAndUserDetailsString(checkoutDetails, userDetails),
                toShowAddress = checkoutDetails.isShippingAddressEnabled ||
                        checkoutDetails.isFullNameEnabled ||
                        checkoutDetails.isEmailEnabled ||
                        checkoutDetails.isPhoneEnabled,
                toShowPersonal = !checkoutDetails.isShippingAddressEnabled,
                toShowOnChangeAddressClick = checkoutDetails.isShippingAddressEditable ||
                        checkoutDetails.isFullNameEditable ||
                        checkoutDetails.isEmailEditable ||
                        checkoutDetails.isPhoneEditable,
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
    if (checkoutDetails.isSessionExpired) {
        viewModel.stopSessionCountDown()
        viewModel.stopFetchStatusPolling()
        SessionExpire (
            onClick = {
                viewModel.callUiAnalytics(
                    event = AnalyticsEvents.PAYMENT_RESULT_SCREEN_DISPLAYED.value,
                    screenName = "App nav host",
                    message = "Session expired button clicked"
                )
                callSDKPaymentResponse()
            }
        )
    }
    if (checkoutDetails.isPaymentSuccessful) {
        viewModel.stopSessionCountDown()
        viewModel.stopFetchStatusPolling()
        PaymentSuccessful (
            dateNTime = checkoutDetails.successfulTimeStamp,
            paymentMethod = checkoutDetails.selectedPaymentMethod,
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
    if (checkoutDetails.isPaymentFailed) {
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
    val status = CheckoutDetailsHandler.checkoutDetails.status
    val transactionId = CheckoutDetailsHandler.checkoutDetails.transactionId
    val inquiryToken = CheckoutDetailsHandler.checkoutDetails.inquiryToken

    ServiceRequest.close()

    // ✅ Notify after reset
    SDKPaymentResponseHandler.notifyResult(
        result = SDKPaymentResponse(
            status = status,
            transactionId = transactionId,
            inquiryToken = inquiryToken
        )
    )

    // ✅ Reset first so UI clears immediately
    CheckoutDetailsHandler.resetToDefault()
    UserDataHandler.resetToDefault()

    CommonSDKDismissHandler.notifyToCloseSDK()
}