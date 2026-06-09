package com.crossplatform.sdk.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.AnalyticsEvents
import com.crossplatform.sdk.presentation.BackHandler
import com.crossplatform.sdk.presentation.SectionTitle
import com.crossplatform.sdk.presentation.UiState
import com.crossplatform.sdk.presentation.components.OfferCard
import com.crossplatform.sdk.presentation.components.ShimmerView
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import com.crossplatform.sdk.presentation.toComposeColor
import com.crossplatform.sdk.presentation.viewmodel.InstantOfferViewModel
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_search
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun InstantOfferScreen(
    selectedCode : String,
    onBackPress : () -> Unit,
    onClickApply : (String) -> Unit,
    onClickRemove : () -> Unit
) {
    BackHandler(onBack = onBackPress)
    val viewModel : InstantOfferViewModel = koinViewModel()
    val screenState by viewModel.offerState.collectAsStateWithLifecycle()
    val buttonColor = CheckoutDetailsHandler.buttonColorFlow.collectAsStateWithLifecycle()
    val focusedTextInputBorderColor = CheckoutDetailsHandler.focusedBorderColorFlow.collectAsStateWithLifecycle()
    val unfocusedTextInputBorderColor = CheckoutDetailsHandler.unfocusedBorderColorFlow.collectAsStateWithLifecycle()
    val codeTextField = remember {
        mutableStateOf("")
    }

    when(screenState) {
        is UiState.Error -> {
            val message = (screenState as UiState.Error).message
            Text("Welcome to error screen $message")
            viewModel.callUiAnalytics(
                event = AnalyticsEvents.SDK_CRASH.value,
                screenName = "MainScreen",
                message = "Main Screen not loaded $message"
            )
        }
        UiState.Loading -> {
            ShimmerView()
        }
        is UiState.Success -> {
            val data = (screenState as UiState.Success).data
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    OutlinedTextField(
                        value = codeTextField.value,
                        onValueChange = {
                            codeTextField.value = it
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
                            focusedBorderColor   = focusedTextInputBorderColor.value.toComposeColor(),
                            unfocusedBorderColor = unfocusedTextInputBorderColor.value.toComposeColor(),
                        )
                    )
                    SectionTitle("Instant Offers")
                }
                items(data) { item ->
                    OfferCard(
                        selectedColor = buttonColor.value.toComposeColor(),
                        offerCode = item.code,
                        description = item.description,
                        terms = item.terms,
                        discountType = item.discountType,
                        expiryDate = "",
                        applicable = "",
                        selectedCouponCode = selectedCode
                    )
                }
            }
        }
    }
}