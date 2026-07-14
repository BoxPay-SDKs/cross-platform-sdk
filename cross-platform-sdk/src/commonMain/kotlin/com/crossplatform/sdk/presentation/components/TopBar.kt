package com.crossplatform.sdk.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.presentation.formatTimer
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.arrow_left
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_timer
import crossplatformsdk.cross_platform_sdk.generated.resources.splash_icon
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun TopBar(
    showDesc: Boolean,
    text: String,
    onBackPress: () -> Unit,
    sessionSeconds: Long? = null
) {

    val isMerchantLogoVisible = CheckoutDetailsHandler.isMerchantLogoVisibleFlow.collectAsStateWithLifecycle()
    val merchantLogo = CheckoutDetailsHandler.merchantLogoFlow.collectAsStateWithLifecycle()
    val merchantName = CheckoutDetailsHandler.merchantNameFlow.collectAsStateWithLifecycle()
    val itemsLength = CheckoutDetailsHandler.itemsLengthFlow.collectAsStateWithLifecycle()
    val currency = CheckoutDetailsHandler.currencyFlow.collectAsStateWithLifecycle()
    val (_, currencyCode) = currency.value
    val amount = CheckoutDetailsHandler.amountFlow.collectAsStateWithLifecycle()
    val isSessionExpiryVisible = CheckoutDetailsHandler.isSessionExpiryVisibleFlow.collectAsStateWithLifecycle()
    // Timer urgency threshold — turns red under 2 minutes
    val isUrgent = (sessionSeconds ?: Long.MAX_VALUE) <= 120L

    val timerBg by animateColorAsState(
        targetValue = if (isUrgent) Color(0xFFFDECEA) else Color(0xFFFFF3E0),
        animationSpec = tween(600),
        label = "timerBg"
    )
    val timerTextColor by animateColorAsState(
        targetValue = if (isUrgent) Color(0xFFC0392B) else Color(0xFF7A5800),
        animationSpec = tween(600),
        label = "timerTextColor"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // ✅ equivalent of Pressable + arrow-left image
            if(isMerchantLogoVisible.value) {
                IconButton(
                    onClick = onBackPress,
                    modifier = Modifier.size(24.dp)
                ) {
                    Image(
                        painter = painterResource(Res.drawable.arrow_left),
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
            }

            if(merchantLogo.value.isNotEmpty()) {
                KamelImage(
                    resource              = asyncPainterResource(data = merchantLogo.value),
                    contentDescription = merchantName.value,
                    modifier           = Modifier.size(32.dp),
                    onLoading = {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFE0E0E0), CircleShape)
                        )
                    },
                    onFailure = {
                        Image(
                            painter = painterResource(Res.drawable.splash_icon),
                            contentDescription = "Back",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))
            }

            // ✅ equivalent of <View style={styles.headerColumn}>
            Column(modifier = Modifier.weight(1f)) {

                // ✅ equivalent of headerTitle Text
                Text(
                    text = text,
                    fontFamily = LocalSDKFonts.current.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFF363840)
                )

                // ✅ equivalent of showDesc &&
                if (showDesc) {
                    Text(
                        text = buildAnnotatedString {

                            // ✅ equivalent of itemsLength > 0 && ...
                            withStyle(
                                style = SpanStyle(
                                    fontSize = 12.sp,
                                    fontFamily = LocalSDKFonts.current.primary,
                                    color = Color(0xFF4F4D55)
                                )
                            ) {
                                if (itemsLength.value > 0) {
                                    append("${itemsLength.value} ")
                                    append(
                                        if (itemsLength.value == 1) "item" else "items"
                                    )
                                    append(" . ")
                                }
                            }

                            withStyle(
                                style = SpanStyle(
                                    fontSize = 12.sp,
                                    fontFamily = LocalSDKFonts.current.primary,
                                    color = Color(0xFF4F4D55)
                                )
                            ) {
                                append("Total: ")
                            }

                            // ✅ equivalent of currencySymbol Text
                            withStyle(
                                style = SpanStyle(
                                    fontSize = 12.sp,
                                    fontFamily = LocalSDKFonts.current.secondary,
                                    color = Color(0xFF4F4D55)
                                )
                            ) {
                                append(" $currencyCode")
                            }

                            // ✅ equivalent of amount Text
                            withStyle(
                                style = SpanStyle(
                                    fontSize = 12.sp,
                                    fontFamily = LocalSDKFonts.current.primary,
                                    color = Color(0xFF4F4D55)
                                )
                            ) {
                                append(" ${amount.value}")
                            }
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF4F4D55),
                        modifier = Modifier.paddingFromBaseline(top = 2.dp)
                    )
                }
            }
            if (sessionSeconds != null && isSessionExpiryVisible.value) {
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(color = timerBg, shape = RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_timer),
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(timerTextColor)
                    )
                    Text(
                        text = formatTimer(sessionSeconds),
                        fontSize = 12.sp,
                        fontFamily = LocalSDKFonts.current.primary,
                        fontWeight = FontWeight.Medium,
                        color = timerTextColor
                    )
                }
            }
        }
        HorizontalDivider()
    }
}