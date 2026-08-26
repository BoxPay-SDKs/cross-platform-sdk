package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod
import com.crossplatform.sdk.domain.model.SurchargeModel
import com.crossplatform.sdk.presentation.SectionTitle
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_netbanking
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_search
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun BankComponent(
    modifier : Modifier,
    searchQuery : String,
    onSetSearchQuery : (String) -> Unit,
    focusedTextInputBorderColor : String,
    unfocusedTextInputBorderColor : String,
    list : List<SelectedPaymentMethod>,
    onProceedForward : (displayValue : String, instrumentValue : String, type : String) -> Unit,
    onClickRadio : (id : String, name : String) -> Unit,
    selectedInstrumentId : String,
    buttonTextColor : String,
    buttonColor : String,
    amount : Double,
    currencySymbol : String,
    ctaBorderRadius : Int,
    title : String,
    surchargeList : List<SurchargeModel>,
    isBoxPayPayButtonVisible : Boolean = true
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Column (
        modifier = modifier
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
               onSetSearchQuery(it)
            },
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
            label = { Text("Search", fontFamily = LocalSDKFonts.current.primary) },
            singleLine = true,
            leadingIcon = {
                Image(
                    painter            = painterResource(Res.drawable.ic_search),
                    contentDescription = null,
                    modifier           = Modifier.size(width = 32.dp, height = 32.dp)
                )
            },
            keyboardOptions      = KeyboardOptions(
                imeAction = ImeAction.Done          // ← shows "Done" on iOS keyboard
            ),
            keyboardActions      = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()          // ← dismisses keyboard on iOS
                    keyboardController?.hide()         // ← belt-and-suspenders for Android/iOS parity
                }
            ),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                // Border
                focusedBorderColor   = focusedTextInputBorderColor.toComposeColor(),
                unfocusedBorderColor = unfocusedTextInputBorderColor.toComposeColor(),
            )
        )

        SectionTitle(title)
        if(list.isEmpty()) {
            EmptyListView(
                heading = "Oops!! No results found",
                subHeading = "Please try another search"
            )
            Spacer(Modifier.then(
                if(isBoxPayPayButtonVisible) Modifier.height(10.dp)
                else Modifier.weight(1f)
            ))
            Footer()
        } else {
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .background(
                        color =  Color.White,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFFE6E6E6),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                list.mapIndexed { index, provider ->
                    PaymentSelector(
                        id                  = provider.id,
                        title               = provider.displayName,
                        imageUrl            = provider.imageUrl,
                        isSelected          = provider.id == selectedInstrumentId,
                        instrumentTypeValue = provider.instrumentType,
                        isLastUsed          = false,
                        onPress             = {
                            onClickRadio(it, provider.displayName)
                        },
                        onProceedForward    = { displayValue, instrumentValue ->
                            onProceedForward(displayValue, instrumentValue, provider.type)
                        },
                        brandColor          = buttonColor,
                        buttonTextColor     = buttonTextColor,
                        currencySymbol      = currencySymbol,
                        amount              = amount,
                        ctaBorderRadius     = ctaBorderRadius,
                        drawableResource    = Res.drawable.ic_netbanking,
                        isBoxPayPayButtonVisible = isBoxPayPayButtonVisible,
                        surchargeFee = surchargeList.find {
                            it.network.replace(" ", "").equals(provider.displayName.replace(" ", ""), ignoreCase = true)
                        }?.amount
                    )
                    if(index != list.lastIndex) {
                        HorizontalDivider(
                            color     = Color(0xFFECECED),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.then(
                if(isBoxPayPayButtonVisible) Modifier.height(10.dp)
                else Modifier.weight(1f)
            ))
            Footer()
        }
    }
}