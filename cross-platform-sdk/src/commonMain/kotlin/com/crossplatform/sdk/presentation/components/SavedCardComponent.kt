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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod
import com.crossplatform.sdk.presentation.ChevronIcon
import com.crossplatform.sdk.presentation.screens.CheckboxItem
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.add_icon
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_card
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_trash
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun SavedCardComponent(
    savedCards : List<SelectedPaymentMethod>,
    onProceedForward : (instrumentRef : String, isSICheckboxChecked : Boolean) -> Unit,
    onClickAddCard : () -> Unit,
    buttonTextColor : String,
    buttonColor : String,
    currencySymbol: String,
    amount: Double,
    ctaBorderRadius : Int,
    isSICheckboxChecked: Boolean,
    isSICheckboxEnabled : Boolean,
    onClickDeleteCard: (String, String) -> Unit,
    onClickRadio : () -> Unit
) {
    val selectedId = remember {
        mutableStateOf("")
    }
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp)
        .background(
            color        = Color.White,
            shape        = RoundedCornerShape(12.dp)
        )
        .border(
            width = 1.dp,
            color = Color(0xFFE6E6E6),
            RoundedCornerShape(12.dp)
        )
    ) {
        savedCards.forEach { card ->
            SavedCardRow(
                id                  = card.id,
                nickName            = card.displayName,
                cardNumber          = card.displayValue,
                imageUrl            = card.imageUrl,
                isSelected          = card.id == selectedId.value,
                instrumentTypeValue = card.instrumentType,
                onPress             = {
                    selectedId.value = it
                    onClickRadio()
                },
                onProceedForward    = onProceedForward,
                brandColor          = buttonColor,
                currencySymbol      = currencySymbol,
                amount              = amount,
                onClickDeleteCard = onClickDeleteCard,
                buttonColor = buttonColor,
                buttonTextColor = buttonTextColor,
                ctaBorderRadius = ctaBorderRadius,
                isSICheckboxChecked = isSICheckboxChecked,
                isSICheckboxEnabled  = isSICheckboxEnabled,
            )
            HorizontalDivider(color = Color(0xFFECECED), thickness = 1.dp, modifier = Modifier.padding(horizontal = 12.dp))
        }

        // Add new card row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClickAddCard() }
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter            = painterResource(Res.drawable.add_icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(buttonColor.toComposeColor()),
                    modifier           = Modifier
                        .size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text       = "Add new Card",
                    fontSize   = 14.sp,
                    fontFamily = LocalSDKFonts.current.primary,
                    fontWeight = FontWeight.SemiBold,
                    color      = buttonColor.toComposeColor()
                )
                Spacer(modifier = Modifier.weight(1f))
                ChevronIcon()
            }
        }
    }
}


@Composable
private fun SavedCardRow(
    id                  : String,
    nickName            : String,
    cardNumber          : String,
    imageUrl            : String,
    isSelected          : Boolean,
    instrumentTypeValue : String,
    onPress             : (String) -> Unit,
    onProceedForward    : (String, Boolean) -> Unit,
    brandColor          : String,
    currencySymbol      : String,
    amount              : Double,
    buttonTextColor : String,
    buttonColor : String,
    ctaBorderRadius : Int,
    isSICheckboxChecked: Boolean,
    isSICheckboxEnabled : Boolean,
    onClickDeleteCard : (String, String) -> Unit
) {
    var isSICheckBoxChecked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color        = if (isSelected) Color(0xFFEDF8F4) else Color.White,
                shape        = RoundedCornerShape(if (isSelected) 0.dp else 12.dp)
            )
            .padding(vertical = 16.dp, horizontal = 12.dp)
    ) {
        // Card info row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth()
        ) {
            // Card network logo
            KamelImage(
                resource              = asyncPainterResource(data = imageUrl),
                contentDescription = "Saved Card",
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
                        painter = painterResource(Res.drawable.ic_card),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            )

            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                if (nickName.isNotEmpty()) {
                    Text(
                        text       = nickName,
                        fontFamily = LocalSDKFonts.current.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 12.sp,
                        color      = Color(0xFF4F4D55),
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.clickable { onPress(id) }
                    )
                }
                Text(
                    text       = cardNumber,
                    fontFamily = LocalSDKFonts.current.primary,
                    fontWeight = FontWeight.Normal,
                    fontSize   = 12.sp,
                    color      = Color(0xFF4F4D55),
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.clickable { onPress(id) }
                )
            }

            RadioButton(
                selected = isSelected,
                onClick  = {
                    isSICheckBoxChecked = false
                    onPress(id)
                },
                colors   = RadioButtonDefaults.colors(
                    selectedColor   = brandColor.toComposeColor(),
                    unselectedColor = Color(0x7301010A)
                )
            )
            Image(
                painter            = painterResource(Res.drawable.ic_trash),
                contentDescription = null,
                colorFilter = ColorFilter.tint(buttonColor.toComposeColor()),
                modifier           = Modifier
                    .size(32.dp)
                    .clickable{
                        onClickDeleteCard(id, nickName)
                    }
            )
        }

        // SI Checkbox
        val showSI = (isSICheckboxChecked || isSICheckboxEnabled) && isSelected
        if (showSI) {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheckboxItem(
                    isChecked   = isSICheckBoxChecked,
                    buttonColor = buttonColor,
                    onClick     = { isSICheckBoxChecked = !isSICheckBoxChecked }
                )
                Text(
                    text       = "Set up Standing Instructions (SI) for this payment.",
                    fontFamily = LocalSDKFonts.current.primary,
                    fontWeight = FontWeight.Normal,
                    fontSize   = 14.sp,
                    color      = Color(0xFF2D2B32),
                    modifier   = Modifier.padding(start = 6.dp)
                )
            }
        }

        // Pay button
        if (isSelected) {
            PayButton(
                text = "Pay",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clickable{
                        onProceedForward(instrumentTypeValue, isSICheckBoxChecked)
                    }
                    .background(brandColor.toComposeColor(), RoundedCornerShape(ctaBorderRadius.dp)),
                amount = amount,
                currencySymbol = currencySymbol,
                isValid = true,
                buttonTextColor = buttonTextColor
            )
        }
    }
}