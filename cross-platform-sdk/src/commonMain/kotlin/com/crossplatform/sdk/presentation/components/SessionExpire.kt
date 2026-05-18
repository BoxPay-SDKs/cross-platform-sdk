package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SessionExpire(
    onClick: () -> Unit
) {
    val checkoutDetails = CheckoutDetailsHandler.checkoutDetails
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/PaymentPending.json").decodeToString()
        )
    }

    ModalBottomSheet(
        onDismissRequest = onClick,
        dragHandle       = null,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lottie Animation
            Image(
                painter = rememberLottiePainter(
                    composition = composition,
                    iterations = 1
                ),
                contentDescription = "Lottie animation",
                modifier = Modifier.size(96.dp)
            )

            // Title
            Text(
                text       = "Payment session has expired.",
                fontSize   = 22.sp,
                fontFamily = defaultFontFamily,
                fontWeight = FontWeight.SemiBold,
                color      = Color(0xFFDB7C1D),
                modifier   = Modifier.padding(top = 8.dp)
            )

            // Description
            Text(
                text       = "For your security, your session has expired due to inactivity. Please restart the payment process.",
                fontSize   = 14.sp,
                fontFamily = defaultFontFamily,
                fontWeight = FontWeight.Normal,
                color      = Color.Black,
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp,
                modifier   = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            // Button
            PayButton(
                text = "Go Back to Home",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(checkoutDetails.ctaBorderRadius.dp))
                    .background(checkoutDetails.buttonColor.toComposeColor())
                    .clickable { onClick() },
                amount = 0.0,
                currencySymbol = "",
                buttonTextColor = checkoutDetails.buttonTextColor,
                isValid = true
            )
        }
    }
}