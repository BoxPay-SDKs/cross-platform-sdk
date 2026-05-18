package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.presentation.ChevronIcon
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun MorePaymentContainer(
    title: String,
    image: DrawableResource,
    surchargeFee: String? = null,
    onClick : () -> Unit
) {
    val checkoutDetails = CheckoutDetailsHandler.checkoutDetails
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(10.dp)
            .clickable{
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Payment Method Icon
        Image(
            painter = painterResource(image),
            contentDescription = title,
            modifier = Modifier
                .size(32.dp)
                .then(
                    if (title == "EMI") Modifier.scale(scaleX = -1f, scaleY = 1f)
                    else Modifier
                )
        )

        // Title + Surcharge
        Column(
            modifier = Modifier.padding(start = 12.dp)
        ) {
            Text(
                text     = title,
                fontSize = 14.sp,
                fontFamily = defaultFontFamily,
                fontWeight = FontWeight.Medium
            )
            if (!surchargeFee.isNullOrEmpty()) {
                Text(
                    text       = "${checkoutDetails.currencySymbol}${surchargeFee} extra applied as surcharge",
                    fontSize   = 14.sp,
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.Medium,
                    color      = Color(0xFF32CD32)
                )
            }
        }

        // Chevron
        Spacer(modifier = Modifier.weight(1f))
        ChevronIcon()
    }
}