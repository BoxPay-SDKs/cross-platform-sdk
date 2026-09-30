package com.crossplatform.sdk.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.data.model.FetchSavedAddress
import com.crossplatform.sdk.presentation.BackHandler
import com.crossplatform.sdk.presentation.SectionTitle
import com.crossplatform.sdk.presentation.UiState
import com.crossplatform.sdk.presentation.components.Footer
import com.crossplatform.sdk.presentation.components.SavedAddressCard
import com.crossplatform.sdk.presentation.components.ShimmerView
import com.crossplatform.sdk.presentation.theme.LocalSDKColors
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import com.crossplatform.sdk.presentation.viewmodel.AddressScreenViewModel
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.add_icon
import crossplatformsdk.cross_platform_sdk.generated.resources.add_new_address_cta
import crossplatformsdk.cross_platform_sdk.generated.resources.chervon_down
import crossplatformsdk.cross_platform_sdk.generated.resources.home_info
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_home
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_more
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_other_house
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_work
import crossplatformsdk.cross_platform_sdk.generated.resources.other_info
import crossplatformsdk.cross_platform_sdk.generated.resources.saved_addresses_title
import crossplatformsdk.cross_platform_sdk.generated.resources.work_info
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import com.crossplatform.sdk.presentation.theme.LocalSDKColors

@Composable
internal fun SavedAddressScreen(
    onBackPress : () -> Unit,
    buttonColor : String,
    onProceedAddNewAddress : () -> Unit,
    onClickSelectAddress : (address : FetchSavedAddress) -> Unit
) {
    BackHandler(onBack = onBackPress)
    val viewModel : AddressScreenViewModel = koinViewModel()
    val uiState by viewModel.savedList.collectAsStateWithLifecycle()
    val selectedAddress by viewModel.selectedSavedAddress.collectAsStateWithLifecycle()

    when(uiState) {
        is UiState.Error -> {
            val message = (uiState as UiState.Error).message
            Text("Welcome to error screen $message")
        }
        UiState.Loading -> {
            ShimmerView(modifier = Modifier.fillMaxSize())
        }
        is UiState.Success ->{
            val response = (uiState as UiState.Success).data

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(
                                width = 1.dp,
                                color = LocalSDKColors.current.divider,
                                RoundedCornerShape(12.dp)
                            )
                            .background(LocalSDKColors.current.background, RoundedCornerShape(12.dp))
                            .clickable { onProceedAddNewAddress() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter            = painterResource(Res.drawable.add_icon),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(buttonColor.toComposeColor()),
                            modifier           = Modifier
                                .size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text       = stringResource(Res.string.add_new_address_cta),
                            color      = buttonColor.toComposeColor(),
                            fontFamily = LocalSDKFonts.current.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp,
                            modifier   = Modifier.weight(1f)
                        )
                        Image(
                            painter            = painterResource(Res.drawable.chervon_down),
                            contentDescription = null,
                    colorFilter        = ColorFilter.tint(LocalSDKColors.current.textSecondary),
                            modifier           = Modifier
                                .size(width = 20.dp, height = 30.dp)
                                .rotate(-90f)
                        )
                    }
                    SectionTitle(stringResource(Res.string.saved_addresses_title))
                }
                items(response) {item ->
                    val addressIcon = if (item.labelType == "Home") Res.drawable.ic_home else if (item.labelType == "Work") Res.drawable.ic_work else Res.drawable.ic_other_house
                    val label = if (item.labelType == "Home") stringResource(Res.string.home_info) else if (item.labelType == "Work") stringResource(Res.string.work_info) else stringResource(Res.string.other_info)
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
                        },
                        onClickSelectAddress = {
                            onClickSelectAddress(item)
                        },
                        selectedCtaColor = buttonColor,
                        editAddressIcon = Res.drawable.ic_more
                    )
                }
                item {
                    Footer()
                }
                item {
                    Footer()
                }
            }
        }
    }
}