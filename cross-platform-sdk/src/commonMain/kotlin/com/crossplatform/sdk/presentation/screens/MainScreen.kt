package com.crossplatform.sdk.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.handler.UserDataHandler
import com.crossplatform.sdk.domain.model.TransactionStatusEnum
import com.crossplatform.sdk.presentation.SectionTitle
import com.crossplatform.sdk.presentation.UiState
import com.crossplatform.sdk.presentation.components.AddressComponent
import com.crossplatform.sdk.presentation.components.Footer
import com.crossplatform.sdk.presentation.components.MorePaymentMethods
import com.crossplatform.sdk.presentation.components.ShimmerView
import com.crossplatform.sdk.presentation.components.ShowLoadingComponent
import com.crossplatform.sdk.presentation.components.UPIComponent
import com.crossplatform.sdk.presentation.launchUpiIntent
import com.crossplatform.sdk.presentation.viewmodel.MainScreenViewModel
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
) {
    val screenState by viewModel.state.collectAsStateWithLifecycle()
    val boxPayAnimationVisible by viewModel.isBoxPayAnimationLoading.collectAsStateWithLifecycle()
    val checkoutDetails by CheckoutDetailsHandler.checkoutDetailsFlow
        .collectAsStateWithLifecycle()
    val userDetails by UserDataHandler.userDataFlow
        .collectAsStateWithLifecycle()

    when (screenState) {
        is UiState.Loading -> ShimmerView()
        is UiState.Error   -> {
            val message = (screenState as UiState.Error).message
            Text("Welcome to error screen $message")
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

            // --- Check if mandatory fields are missing ---
            LaunchedEffect(Unit) {
                val isMandatoryDataMissing =
                    (checkoutDetails.isFullNameEnabled  && checkoutDetails.isFullNameEditable  && userDetails.firstName.isNullOrEmpty()) ||
                            (checkoutDetails.isEmailEnabled     && checkoutDetails.isEmailEditable     && userDetails.email.isNullOrEmpty()) ||
                            (checkoutDetails.isPhoneEnabled     && checkoutDetails.isPhoneEditable     && userDetails.completePhoneNumber.isNullOrEmpty()) ||
                            (checkoutDetails.isShippingAddressEnabled && checkoutDetails.isShippingAddressEditable && userDetails.address1.isNullOrEmpty())

                if (isMandatoryDataMissing) {
                    onProceedAddressScreen(true)
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
                        address = "wz-5 street no 1 ramgarh colony moti nafar new delhi 110015",
                        navigateToAddressScreen = {
                            if (isAddressComponentClickable) onProceedAddressScreen(false)
                        },
                        checkoutDetails = checkoutDetails,
                        userData = userDetails
                    )
                }

                // --- UPI ---
                if (response.methodFlags.isUPIVisible) {
                    SectionTitle("Pay by any UPI")
                    UPIComponent(
                        methodFlags = response.methodFlags,
                        checkoutDetails = checkoutDetails,
                        onClickSavedUpiPayButton = {instrumentRef, shopperVpa ->
                            viewModel.postUpiCollectRequest(instrumentRef = instrumentRef, type = "upi/collect", shopperVpa = shopperVpa)
                        },
                        onClickUpiCollectPayButton = {shopperVpa ->
                            viewModel.postUpiCollectRequest(shopperVpa = shopperVpa, type = "upi/collect")
                        },
                        onClickUpiIntentPayButton = {selectedIntent ->
                            viewModel.postUpiIntentRequest(selectedIntent = selectedIntent, type = "upi/intent")
                        },
                        onClickUpiQRPayButton = {

                        }
                    )
                }

                // --- More Payment Methods ---
                if (otherPaymentMethodEnabled) {
                    SectionTitle(
                        if (response.methodFlags.isUPIVisible) "More Payment Options"
                        else "Payment Options"
                    )
                    MorePaymentMethods(
                        methodFlags      = response.methodFlags,
                        onNavigateToCard        = { onProceedCardScreen() },
                        onNavigateToWallet      = { onProceedWalletScreen() },
                        onNavigateToNetBanking  = { onProceedNetBankingScreen() },
                        onNavigateToEmi         = { onProceedEMIScreen() },
                        onNavigateToBNPL        = { onProceedBNPLScreen() }
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
                onFailure = {_ ->
                    CheckoutDetailsHandler.setSessionFailed()
                    viewModel.upiIntentUrl.value = ""
                },
                onSuccess = {
                    viewModel.isUpiOpening.value = true
                }
            )
        }
    }

    LaunchedEffect(viewModel.proceedToTimer.value) {
        if(viewModel.proceedToTimer.value) {
            viewModel.proceedToTimer.value = false
            onProceedUPITimerScreen(viewModel.upiId.value)
        }
    }
}