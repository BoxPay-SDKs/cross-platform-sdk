package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.presentation.formatTransactionTimestamp
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import com.crossplatform.sdk.presentation.theme.defaultInterFontFamily
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PaymentSuccessful(
    dateNTime : String,
    paymentMethod : String,
    onClick: () -> Unit,
) {
    val checkoutDetails = CheckoutDetailsHandler.checkoutDetails
    LaunchedEffect(Unit) {
        if(!checkoutDetails.isSuccessScreenVisible) {
            onClick()
        }
    }
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/PaymentSuccessful.json").decodeToString()
        )
    }

    val dateNTimeExtracted = formatTransactionTimestamp(dateNTime)
    val uiRows = listOf(
        Pair("Transaction ID", checkoutDetails.transactionId),
        Pair("Date", dateNTimeExtracted?.first ?: ""),
        Pair("Time", dateNTimeExtracted?.second ?: ""),
        Pair("Payment Method", paymentMethod)
    )

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
                text       = "Payment Successful",
                fontSize   = 22.sp,
                fontFamily = defaultFontFamily,
                fontWeight = FontWeight.SemiBold,
                color      = Color(0xFF019939),
                modifier   = Modifier.padding(top = 8.dp)
            )

            uiRows.map {item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.first,
                        fontSize = 14.sp,
                        fontFamily = defaultFontFamily,
                        fontWeight = FontWeight.Normal,
                    )
                    Text(
                        text = item.second,
                        fontSize = 14.sp,
                        fontFamily = defaultFontFamily,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 6.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Amount",
                    fontSize = 14.sp,
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text =buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontFamily = defaultInterFontFamily
                            )
                        ) {
                            append(checkoutDetails.currencySymbol)
                        }
                        withStyle(
                            style = SpanStyle(
                                fontFamily = defaultFontFamily
                            )
                        ) {
                            append("${checkoutDetails.amount}")
                        }
                    },
                    fontSize = 16.sp,
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            HorizontalDivider()

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