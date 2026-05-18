package com.crossplatform.sdk.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.handler.SDKJobHandler
import com.crossplatform.sdk.data.handler.SDKPaymentResponseHandler
import com.crossplatform.sdk.data.handler.UserDataHandler
import com.crossplatform.sdk.data.model.SDKPaymentResponse
import com.crossplatform.sdk.presentation.components.PaymentFailed
import com.crossplatform.sdk.presentation.components.PaymentSuccessful
import com.crossplatform.sdk.presentation.components.SessionExpire
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
    val navController = rememberNavController()
    val viewModel: MainScreenViewModel = koinViewModel()

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
        if(!viewModel.isLoadingSession.value) {
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
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.SavedAddressScreen.route) {
                SavedAddressScreen(
                    onBackPress = {
                        navController.popBackStack()
                    }
                )
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
    if (checkoutDetails.isSessionExpired) {
        SessionExpire (
            onClick = {
                callSDKPaymentResponse()
            }
        )
    }
    if (checkoutDetails.isPaymentSuccessful) {
        PaymentSuccessful (
            dateNTime = checkoutDetails.successfulTimeStamp,
            paymentMethod = checkoutDetails.selectedPaymentMethod,
            onClick = {
                callSDKPaymentResponse()
            }
        )
    }
    if (checkoutDetails.isPaymentFailed) {
        PaymentFailed(
            sheetState = failedSheetState,
            onClick = {
                hideFailedSheet()
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
}