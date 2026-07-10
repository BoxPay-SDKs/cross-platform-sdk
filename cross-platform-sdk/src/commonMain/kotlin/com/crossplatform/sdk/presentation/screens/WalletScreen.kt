package com.crossplatform.sdk.presentation.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.AnalyticsEvents
import com.crossplatform.sdk.presentation.BackHandler
import com.crossplatform.sdk.presentation.UiState
import com.crossplatform.sdk.presentation.components.BankComponent
import com.crossplatform.sdk.presentation.components.ShimmerView
import com.crossplatform.sdk.presentation.components.ShowLoadingComponent
import com.crossplatform.sdk.presentation.viewmodel.WalletViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WalletScreen(
    onBackPress : () -> Unit,
    isAutoNavigationEnabled : Boolean,
    onExitCheckout : () -> Unit
) {
    BackHandler(onBack = onBackPress)
    val viewModel : WalletViewModel = koinViewModel()
    val focusedTextInputBorderColor = CheckoutDetailsHandler.focusedBorderColorFlow.collectAsStateWithLifecycle()
    val unfocusedTextInputBorderColor = CheckoutDetailsHandler.unfocusedBorderColorFlow.collectAsStateWithLifecycle()
    val buttonTextColor = CheckoutDetailsHandler.buttonTextColorFlow.collectAsStateWithLifecycle()
    val buttonColor = CheckoutDetailsHandler.buttonColorFlow.collectAsStateWithLifecycle()
    val currencyFlow = CheckoutDetailsHandler.currencyFlow.collectAsStateWithLifecycle()
    val (_, currencyCode) = currencyFlow.value
    val amount = CheckoutDetailsHandler.amountFlow.collectAsStateWithLifecycle()
    val ctaBorderRadius = CheckoutDetailsHandler.ctaBorderRadiusFlow.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isBoxPayAnimationVisible by viewModel.isBoxPayAnimationVisible.collectAsStateWithLifecycle()
    val showWebView by viewModel.showWebview.collectAsStateWithLifecycle()
    val selectedInstrumentId = remember {
        mutableStateOf("")
    }

    LaunchedEffect(isAutoNavigationEnabled) {
        if(isAutoNavigationEnabled) {
            onExitCheckout()
        }
    }

    when(uiState) {
        is UiState.Error -> {
            val message = (uiState as UiState.Error).message
            Text("Welcome to error screen $message")
            LaunchedEffect(message) {
                viewModel.callUiAnalytics(
                    event      = AnalyticsEvents.SDK_CRASH.value,
                    screenName = "Wallet Screen ",
                    message    = "Wallet screen not loaded $message",
                )
            }
        }
        UiState.Loading -> {
            ShimmerView(modifier = Modifier.fillMaxSize())
        }
        is UiState.Success -> {
            val list = (uiState as UiState.Success).data
            BankComponent(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                list = list,
                buttonColor = buttonColor.value,
                buttonTextColor = buttonTextColor.value,
                unfocusedTextInputBorderColor = unfocusedTextInputBorderColor.value,
                focusedTextInputBorderColor = focusedTextInputBorderColor.value,
                selectedInstrumentId = selectedInstrumentId.value,
                onClickRadio = {
                    selectedInstrumentId.value = it
                    viewModel.callUiAnalytics(
                        event = AnalyticsEvents.PAYMENT_CATEGORY_SELECTED.value,
                        screenName = "Wallet Screen",
                        message = "payment category selected"
                    )
                },
                searchQuery = viewModel.searchQuery.value,
                onSetSearchQuery = {
                    viewModel.onSearch(it)
                },
                onProceedForward = {
                    viewModel.postWalletRequest(it)
                },
                amount = amount.value,
                currencySymbol = currencyCode,
                ctaBorderRadius = ctaBorderRadius.value,
                title = "All Wallet"
            )
        }
    }
    if(isBoxPayAnimationVisible) {
        ShowLoadingComponent(Modifier.fillMaxSize())
    }

    if(showWebView) {
        WebViewScreen(
            url = viewModel.url.value,
            html = viewModel.htmlString.value,
            onBackPress = {result ->
                viewModel.callFetchStatus(result ?: "")
                viewModel.setWebViewScreen(false)
            }
        )
    }
}