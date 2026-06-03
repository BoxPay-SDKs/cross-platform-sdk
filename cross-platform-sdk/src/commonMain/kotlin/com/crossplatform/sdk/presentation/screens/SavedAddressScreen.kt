package com.crossplatform.sdk.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.presentation.BackHandler
import com.crossplatform.sdk.presentation.UiState
import com.crossplatform.sdk.presentation.components.SavedAddressCard
import com.crossplatform.sdk.presentation.components.ShimmerView
import com.crossplatform.sdk.presentation.viewmodel.AddressScreenViewModel
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_home
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_more
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_other_house
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_work
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SavedAddressScreen(
    onBackPress : () -> Unit
) {
    BackHandler(onBack = onBackPress)
    val viewModel : AddressScreenViewModel = koinViewModel()
    val uiState by viewModel.savedList.collectAsStateWithLifecycle()
    val selectedAddress by viewModel.selectedSavedAddress.collectAsStateWithLifecycle()
    val checkoutDetails by CheckoutDetailsHandler.checkoutDetailsFlow.collectAsStateWithLifecycle()

    when(uiState) {
        is UiState.Error -> {
            val message = (uiState as UiState.Error).message
            Text("Welcome to error screen $message")
        }
        UiState.Loading -> {
            ShimmerView()
        }
        is UiState.Success ->{
            val response = (uiState as UiState.Success).data

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                response.map {item ->
                    val addressIcon = if (item.labelType == "Home") Res.drawable.ic_home else if (item.labelType == "Work") Res.drawable.ic_work else Res.drawable.ic_other_house
                    val label = if (item.labelType == "Home") "Home" else if (item.labelType == "Work") "Work" else "Other"
                    SavedAddressCard(
                        modifier = Modifier.fillMaxSize(),
                        address2 = item.address2,
                        address1 = item.address1,
                        city = item.city,
                        pinCode = item.postalCode,
                        state = item.state,
                        addressIcon = addressIcon,
                        label = label,
                        number = item.phoneNumber,
                        isCurrentlySelected = selectedAddress == item.addressRef,
                        onClickEditAddress = {
                            println("======address $item")
                        },
                        onClickSelectAddress = {
                            println("========$item")
                        },
                        selectedCtaColor = checkoutDetails.buttonColor,
                        editAddressIcon = Res.drawable.ic_more
                    )
                }
            }
        }
    }
}