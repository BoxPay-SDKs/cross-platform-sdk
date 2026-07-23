package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.domain.handler.ExpressCheckoutPaymentHandler
import com.crossplatform.sdk.domain.handler.GooglePayExpressCheckoutConfig
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_apple_pay
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_google_pay
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_revolut_pay
import org.jetbrains.compose.resources.painterResource

@Composable
fun ExpressCheckout(
    config : GooglePayExpressCheckoutConfig,
    paymentHandler: ExpressCheckoutPaymentHandler,
    onClickRevolut : () -> Unit,
    onClickGooglePay : () -> Unit,
    onClickApplePay : () -> Unit
) {
    val showApplePay = paymentHandler.isApplePayAvailable()
    val showGooglePay by produceState(initialValue = false, key1 = config) {
        value = paymentHandler.isGooglePayAvailable(config)
    }
    val showRevolutPay = paymentHandler.isRevolutPayAvailable()


    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showApplePay) {
            ApplePayButton(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minWidth = 140.dp),
                onClick = {
                    onClickApplePay()
                }
            )
        }
        if (showGooglePay) {
            GooglePayButton(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minWidth = 140.dp),
                onClick = {
                    onClickGooglePay()
                }
            )
        }
        if (showRevolutPay) {
            RevolutPay(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minWidth = 140.dp),
                onClick = {
                    onClickRevolut()
                }
            )
        }
    }
}

@Composable
private fun RevolutPay(
    onClick: () -> Unit,
    modifier: Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
        modifier = modifier.height(50.dp)
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_revolut_pay),
            modifier = Modifier.width(180.dp),
            contentDescription = "revolut pay "
        )
    }
}

@Composable
private fun GooglePayButton(
    onClick: () -> Unit,
    modifier: Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
        modifier = modifier.height(50.dp)
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_google_pay),
            modifier = Modifier.size(32.dp),
            contentDescription = "google pay "
        )
        Text("Pay", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp, fontFamily = LocalSDKFonts.current.primary)
    }
}

@Composable
private fun ApplePayButton(
    onClick: () -> Unit,
    modifier: Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        modifier = modifier.height(50.dp).border(1.dp, Color.Black, RoundedCornerShape(8.dp))
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_apple_pay),
            modifier = Modifier.size(32.dp),
            contentDescription = "apple pay "
        )
        Text("Pay", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, fontFamily = LocalSDKFonts.current.primary)
    }
}



