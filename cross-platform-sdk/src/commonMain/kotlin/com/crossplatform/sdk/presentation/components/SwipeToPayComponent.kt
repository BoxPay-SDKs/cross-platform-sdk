package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.presentation.ChevronIcon
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_keyboard_double_arrow
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_tick_arrow
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_upi
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

@Composable
fun SwipeToPayComponent(
    buttonColor: String,
    buttonTextColor: String,
    amount: Double,
    currencySymbol: String,
    lastUsedUpi: String,
    onClickMoreOptions: () -> Unit,
    onSwipeComplete: () -> Unit,
    address: String,
    onClickChangeAddress: () -> Unit,
    toShowOnChangeAddressClick: Boolean,
    toShowAddress: Boolean,
    toShowPersonal: Boolean,
    logoUrl: String
) {
    Card(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp)
        ) {
            // ── Address / Personal Details section ──────────────────────
            if (toShowAddress) {
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (toShowPersonal) "Personal Details" else "Shipping Address",
                        color = Color(0xFF2D2B32),
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = LocalSDKFonts.current.primary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (toShowOnChangeAddressClick) {
                        Text(
                            text = "Change",
                            color = buttonColor.toComposeColor(),
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = LocalSDKFonts.current.primary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(start = 4.dp, end = 12.dp)
                                .clickable { onClickChangeAddress() }
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = address,
                    color = Color(0xFF7F7D83),
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = LocalSDKFonts.current.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
            }

            // ── Payment title row ───────────────────────────────────────
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Pay ₹<amount>"
                Text(
                    text = buildAnnotatedString {
                        append(
                            AnnotatedString(
                                text = "Pay ",
                                spanStyle = SpanStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = LocalSDKFonts.current.primary
                                )
                            )
                        )
                        append(
                            AnnotatedString(
                                text = currencySymbol,
                                spanStyle = SpanStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = LocalSDKFonts.current.secondary
                                )
                            )
                        )
                        append(
                            AnnotatedString(
                                text = " $amount",
                                spanStyle = SpanStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = LocalSDKFonts.current.primary
                                )
                            )
                        )
                    },
                    color = Color(0xFF2D2B32),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.weight(1f))
                Text(
                    text = "More Options",
                    color = buttonColor.toComposeColor(),
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = LocalSDKFonts.current.primary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable {
                        onClickMoreOptions()
                    }
                )
                ChevronIcon()
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Last Used Payment Option",
                color = Color(0xFF7F7D83),
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = LocalSDKFonts.current.primary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // ── Selected instrument row ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0xFFEDF8F4), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFEFEFEF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo box
                KamelImage(
                    resource              = asyncPainterResource(data = logoUrl),
                    contentDescription = "icon",
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
                            painter = painterResource(Res.drawable.ic_upi),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                )

                Spacer(Modifier.width(12.dp))

                Text(
                    text = lastUsedUpi,
                    color = Color(0xFF4F4D55),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = LocalSDKFonts.current.primary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                RadioButton(
                    selected = true,
                    onClick = {},
                    colors = RadioButtonDefaults.colors(
                        selectedColor = buttonColor.toComposeColor()
                    )
                )
            }

            Spacer(Modifier.height(18.dp))

            // ── Swipe-to-pay CTA ────────────────────────────────────────
            SwipeToPayButtonComponent(
                onSwipeComplete = { onSwipeComplete() },
                buttonColor = buttonColor,
                buttonTextColor = buttonTextColor,
                modifier = Modifier.fillMaxWidth(),
                amount = amount,
                currencySymbol = currencySymbol
            )

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
fun SwipeToPayButtonComponent(
    onSwipeComplete: () -> Unit,
    modifier: Modifier = Modifier,
    buttonColor: String,
    buttonTextColor: String,
    height: Dp = 48.dp,
    amount: Double,
    currencySymbol: String
) {
    val swipePosition = remember { mutableStateOf(0f) }
    val buttonWidth = remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val heightPx = with(density) { height.toPx() }

    val seventyPercentThreshold = buttonWidth.value * 0.7f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(buttonColor.toComposeColor(), shape = RoundedCornerShape(12.dp))
            .onSizeChanged { size ->
                buttonWidth.value = size.width
            }
    ) {
        val textAlpha = 1f - (swipePosition.value / buttonWidth.value.toFloat())
            .coerceIn(0f, 1f)

        // Center label
        Text(
            text = buildAnnotatedString {
                append(
                    AnnotatedString(
                        text = "Swipe to Pay ",
                        spanStyle = SpanStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                             fontFamily = LocalSDKFonts.current.primary
                        )
                    )
                )
                append(
                    AnnotatedString(
                        text = currencySymbol,
                        spanStyle = SpanStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                             fontFamily = LocalSDKFonts.current.secondary
                        )
                    )
                )
                append(
                    AnnotatedString(
                        text = "$amount",
                        spanStyle = SpanStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                             fontFamily = LocalSDKFonts.current.primary
                        )
                    )
                )
            },
            color = buttonTextColor.toComposeColor().copy(alpha = textAlpha),
            modifier = Modifier.align(Alignment.Center)
        )

        // Draggable thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(swipePosition.value.roundToInt(), 0) }
                .size(height)
                .padding(vertical = 3.dp, horizontal = 4.dp)
                .background(Color.White, shape = RoundedCornerShape(10.dp))
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newPosition = (swipePosition.value + delta)
                            .coerceIn(0f, buttonWidth.value - heightPx)
                        swipePosition.value = newPosition
                    },
                    onDragStopped = {
                        if (swipePosition.value >= (buttonWidth.value - heightPx) ||
                            swipePosition.value >= seventyPercentThreshold
                        ) {
                            onSwipeComplete()
                        }
                        swipePosition.value = 0f
                    }
                )
        ) {
            // Use compose-resources painter in CMP
            val iconRes = if (swipePosition.value >= seventyPercentThreshold) {
                Res.drawable.ic_tick_arrow
            } else {
                Res.drawable.ic_keyboard_double_arrow
            }

            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(buttonColor.toComposeColor()),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
            )
        }
    }
}