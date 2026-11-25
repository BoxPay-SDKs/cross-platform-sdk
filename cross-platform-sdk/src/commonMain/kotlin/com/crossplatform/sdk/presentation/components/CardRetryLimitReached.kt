package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.crossplatform.sdk.presentation.theme.LocalSDKColors
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.card_retry_limit_reached_desc
import crossplatformsdk.cross_platform_sdk.generated.resources.payment_method_limit_reached_title
import crossplatformsdk.cross_platform_sdk.generated.resources.try_another_card_cta
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource

/**
 * Shown when a card payment hits its retry limit. Unlike [PaymentMaxRetryReached]
 * it does not list other payment methods: the shopper stays on the card screen
 * and is asked to try another card.
 */
@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun CardRetryLimitReached(
    sheetState: SheetState,
    onTryAnotherCard: () -> Unit
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
        onDismissRequest = onTryAnotherCard,
        sheetState = sheetState,
        dragHandle       = null,
        containerColor   = LocalSDKColors.current.background,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = rememberLottiePainter(
                    composition = composition,
                    iterations = 1
                ),
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )

            Text(
                text       = stringResource(Res.string.payment_method_limit_reached_title),
                fontSize   = 22.sp,
                fontFamily = LocalSDKFonts.current.primary,
                fontWeight = FontWeight.SemiBold,
                color      = Color(0xFFE84142),
                textAlign  = TextAlign.Start,
                modifier   = Modifier.padding(top = 8.dp)
            )

            Text(
                text       = stringResource(Res.string.card_retry_limit_reached_desc),
                fontSize   = 14.sp,
                fontFamily = LocalSDKFonts.current.primary,
                fontWeight = FontWeight.Normal,
                color      = LocalSDKColors.current.textSecondary,
                textAlign  = TextAlign.Start,
                lineHeight = 20.sp,
                modifier   = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            PayButton(
                text = stringResource(Res.string.try_another_card_cta),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(ctaBorderRadius.value.dp))
                    .background(buttonColor.value.toComposeColor())
                    .clickable { onTryAnotherCard() },
                amount = 0.0,
                currencySymbol = "",
                buttonTextColor = buttonTextColor.value,
                isValid = true
            )
        }
    }
}
