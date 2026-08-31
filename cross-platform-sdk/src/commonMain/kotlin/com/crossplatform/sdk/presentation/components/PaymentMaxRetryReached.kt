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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.presentation.ChevronIcon
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_bnpl
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_card
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_emi
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_netbanking
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_upi
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_wallet
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_wallet_semi_bold
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun PaymentMaxRetryReached(
    availableMethods : List<String>,
    sheetState: SheetState,
    onSelectMethod: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val buttonTextColor = CheckoutDetailsHandler.buttonTextColorFlow.collectAsStateWithLifecycle()
    val buttonColor = CheckoutDetailsHandler.buttonColorFlow.collectAsStateWithLifecycle()
    val ctaBorderRadius = CheckoutDetailsHandler.ctaBorderRadiusFlow.collectAsStateWithLifecycle()

    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/PaymentFailed.json").decodeToString()
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                text       = "Payment method limit reached",
                fontSize   = 22.sp,
                fontFamily = LocalSDKFonts.current.primary,
                fontWeight = FontWeight.SemiBold,
                color      = Color(0xFFE84142),
                modifier   = Modifier.padding(top = 8.dp)
            )

            // Description
            Text(
                text       = "You’ve reached the maximum number of payment attempts for this payment method. Please select a different payment method to continue",
                fontSize   = 14.sp,
                fontFamily = LocalSDKFonts.current.primary,
                fontWeight = FontWeight.Normal,
                color      = Color.Black,
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp,
                modifier   = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            // "Try another method" label
            Text(
                text       = "Try another payment method",
                fontSize   = 13.sp,
                fontFamily = LocalSDKFonts.current.primary,
                fontWeight = FontWeight.Medium,
                color      = Color(0xFF6B6B6B),
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Alternate payment methods list
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFFF5F6FB), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
            ) {
                itemsIndexed(availableMethods) { index, method ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectMethod(method) }
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Image(
                            painter = painterResource(getIcon(method)),
                            contentDescription = method,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text       = method,
                            fontSize   = 15.sp,
                            fontFamily = LocalSDKFonts.current.primary,
                            fontWeight = FontWeight.Normal,
                            color      = Color.Black,
                            modifier   = Modifier.padding(start = 12.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        ChevronIcon()
                    }
                    if(index != availableMethods.lastIndex) {
                        HorizontalDivider(color = Color(0xFFE6E6E6))
                    }
                }
            }

            // Dismiss / Cancel button
            PayButton(
                text = "Cancel",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(ctaBorderRadius.value.dp))
                    .background(buttonColor.value.toComposeColor())
                    .clickable { onDismiss() },
                amount = 0.0,
                currencySymbol = "",
                buttonTextColor = buttonTextColor.value,
                isValid = true
            )
        }
    }
}

internal fun getIcon(name : String) : DrawableResource {
    return when(name.lowercase()) {
        "upi","upionetimemandate" -> Res.drawable.ic_upi
        "card" -> Res.drawable.ic_card
        "wallet" -> Res.drawable.ic_wallet
        "netbanking" -> Res.drawable.ic_netbanking
        "buynowpaylater" -> Res.drawable.ic_bnpl
        "emi" -> Res.drawable.ic_emi
         else -> Res.drawable.ic_wallet_semi_bold
    }
}