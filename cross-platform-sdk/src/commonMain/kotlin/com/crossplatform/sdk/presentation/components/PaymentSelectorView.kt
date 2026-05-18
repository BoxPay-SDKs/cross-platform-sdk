package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.data.model.CheckoutDetails
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import com.crossplatform.sdk.presentation.toComposeColor
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun PaymentSelectorView(
    providerList     : List<SelectedPaymentMethod>,
    onProceedForward : (instrumentType: String, instrumentValue: String, type: String) -> Unit,
    isLastUsed       : Boolean = false,
    onClickRadio     : (selectedInstrumentValue: String) -> Unit,
    checkoutDetails : CheckoutDetails,
    drawableResource: DrawableResource
) {
    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
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
        providerList.forEachIndexed { index, provider ->
            PaymentSelector(
                id                  = provider.id,
                title               = provider.displayName,
                imageUrl            = provider.imageUrl,
                isSelected          = provider.isSelected == true,
                instrumentTypeValue = provider.instrumentType,
                isLastUsed          = isLastUsed && provider.isLastUsed == true,
                onPress             = { onClickRadio(it) },
                onProceedForward    = { displayValue, instrumentValue ->
                    onProceedForward(displayValue, instrumentValue, provider.type)
                },
                brandColor          = checkoutDetails.buttonColor,
                buttonTextColor     = checkoutDetails.buttonTextColor,
                currencySymbol      = checkoutDetails.currencySymbol,
                amount              = checkoutDetails.amount,
                ctaBorderRadius     = checkoutDetails.ctaBorderRadius,
                drawableResource = drawableResource
            )
            if (index != providerList.lastIndex) {
                HorizontalDivider(
                    color     = Color(0xFFECECED),
                    thickness = 1.dp
                )
            }
        }
    }
}

@Composable
private fun PaymentSelector(
    id                 : String,
    title              : String,
    imageUrl           : String,
    isSelected         : Boolean,
    instrumentTypeValue: String,
    isLastUsed         : Boolean,
    onPress            : (String) -> Unit,
    onProceedForward   : (String, String) -> Unit,
    brandColor         : String,
    buttonTextColor    : String,
    currencySymbol     : String,
    amount             : Double,
    ctaBorderRadius    : Int,
    drawableResource: DrawableResource
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().clickable{onPress(id)},
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Icon ---
            KamelImage(
                resource              = asyncPainterResource(data = imageUrl),
                contentDescription = title,
                modifier           = Modifier.size(32.dp),
                onLoading = {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = Color(0xFFE6E6E6),
                                RoundedCornerShape(12.dp)
                            )
                    )
                },
                onFailure = {
                    Image(
                        painter = painterResource(drawableResource),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            )

            // --- Title + Last Used Tag ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text       = title,
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    color      = Color(0xFF4F4D55),
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.clickable { onPress(id) }
                )
                if (isLastUsed) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(0.5.dp, Color(0xFF1CA672), RoundedCornerShape(6.dp))
                            .background(Color(0x141CA672))
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text       = "Last Used",
                            fontFamily = defaultFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize   = 10.sp,
                            color      = Color(0xFF1CA672)
                        )
                    }
                }
            }

            // --- Radio Button ---
            RadioButton(
                selected = isSelected,
                onClick  = { onPress(id) },
                colors   = RadioButtonDefaults.colors(
                    selectedColor   = brandColor.toComposeColor(),
                    unselectedColor = Color(0x7301010A)
                )
            )
        }

        // --- Pay Button (when selected) ---
        if (isSelected) {
            Spacer(modifier = Modifier.height(10.dp))
            PayButton(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(ctaBorderRadius.dp))
                    .background(brandColor.toComposeColor())
                    .clickable { onProceedForward(title, instrumentTypeValue) },
                amount = amount,
                currencySymbol = currencySymbol,
                text = "Pay",
                buttonTextColor = buttonTextColor,
                isValid = true
            )
        }
    }
}