package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod
import com.crossplatform.sdk.presentation.SectionTitle
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_netbanking
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_search
import org.jetbrains.compose.resources.painterResource

@Composable
fun BankComponent(
    modifier : Modifier,
    searchQuery : String,
    onSetSearchQuery : (String) -> Unit,
    focusedTextInputBorderColor : String,
    unfocusedTextInputBorderColor : String,
    list : List<SelectedPaymentMethod>,
    onProceedForward : (String) -> Unit,
    onClickRadio : (String) -> Unit,
    selectedInstrumentId : String,
    buttonTextColor : String,
    buttonColor : String,
    amount : Double,
    currencySymbol : String,
    ctaBorderRadius : Int,
    title : String,
    isBoxPayPayButtonVisible : Boolean = true
) {
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
            PaymentSelectorView(
                providerList = list,
                onProceedForward = { _, instrumentValue, _ ->
                    onProceedForward(instrumentValue)
                },
                drawableResource = Res.drawable.ic_netbanking,
                onClickRadio = {
                    onClickRadio(it)
                },
                buttonTextColor = buttonTextColor,
                buttonColor = buttonColor,
                currencySymbol = currencySymbol,
                amount = amount,
                ctaBorderRadius = ctaBorderRadius,
                selectedId = selectedInstrumentId,
                isBoxPayPayButtonVisible = isBoxPayPayButtonVisible
            )
            Spacer(Modifier.then(
                if(isBoxPayPayButtonVisible) Modifier.height(10.dp)
                else Modifier.weight(1f)
            ))
            Footer()
        }
    }
}