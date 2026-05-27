package com.crossplatform.sdk.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.AnalyticsEvents
import com.crossplatform.sdk.presentation.SectionTitle
import com.crossplatform.sdk.presentation.UiState
import com.crossplatform.sdk.presentation.components.EmptyListView
import com.crossplatform.sdk.presentation.components.Footer
import com.crossplatform.sdk.presentation.components.PaymentSelectorView
import com.crossplatform.sdk.presentation.components.ShimmerView
import com.crossplatform.sdk.presentation.components.ShowLoadingComponent
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import com.crossplatform.sdk.presentation.toComposeColor
import com.crossplatform.sdk.presentation.viewmodel.BNPLViewModel
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_netbanking
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_search
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BNPLScreen() {
    val viewModel : BNPLViewModel  = koinViewModel()
    val checkoutDetails by CheckoutDetailsHandler.checkoutDetailsFlow
        .collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isBoxPayAnimationVisible by viewModel.isBoxPayAnimationVisible.collectAsStateWithLifecycle()
    val showWebView by viewModel.showWebview.collectAsStateWithLifecycle()

    when(uiState) {
        is UiState.Error -> {
            val message = (uiState as UiState.Error).message
            Text("Welcome to error screen $message")
            viewModel.callUiAnalytics(
                event = AnalyticsEvents.SDK_CRASH.value,
                screenName = "BNPLScreen",
                message = "BNPLScreen not loaded $message"
            )
        }
        UiState.Loading -> {
            ShimmerView()
        }
        is UiState.Success -> {
            val list = (uiState as UiState.Success).data
            Column (
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = viewModel.searchQuery.value,
                    onValueChange = {
                        viewModel.onSearch(it)
                    },
                    modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
                    label = { Text("Search", fontFamily = defaultFontFamily) },
                    singleLine = true,
                    leadingIcon = {
                        Image(
                            painter            = painterResource(Res.drawable.ic_search),
                            contentDescription = null,
                            modifier           = Modifier.size(width = 32.dp, height = 32.dp)
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        // Border
                        focusedBorderColor   = checkoutDetails.focusedTextInputBorderColor.toComposeColor(),
                        unfocusedBorderColor = checkoutDetails.unfocusedTextInputBorderColor.toComposeColor(),
                    )
                )

                SectionTitle("Select BNPL")
                if(list.isEmpty()) {
                    EmptyListView(
                        heading = "Oops!! No results found",
                        subHeading = "Please try another search"
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Footer()
                } else {
                    PaymentSelectorView(
                        providerList = list,
                        onProceedForward = { _, instrumentValue, _ ->
                            viewModel.postBNPLRequest(instrumentValue)
                        },
                        checkoutDetails = checkoutDetails,
                        drawableResource = Res.drawable.ic_netbanking,
                        onClickRadio = {
                            viewModel.callUiAnalytics(
                                event = AnalyticsEvents.PAYMENT_CATEGORY_SELECTED.value,
                                screenName = "BNPLViewModel",
                                message = "Payment method selected"
                            )
                        }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Footer()
                }
            }
        }
    }
    if(isBoxPayAnimationVisible) {
        ShowLoadingComponent()
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