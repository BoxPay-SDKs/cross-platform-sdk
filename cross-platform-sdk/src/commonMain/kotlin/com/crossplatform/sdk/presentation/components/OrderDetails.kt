package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.domain.model.MainScreenModel.OrderItemUiModel
import com.crossplatform.sdk.domain.model.SurchargeModel
import com.crossplatform.sdk.presentation.ChevronIcon
import com.crossplatform.sdk.presentation.formatAmount
import com.crossplatform.sdk.presentation.isTabletDevice
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_broken_order_image
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_cvv_info
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun OrderDetails(
    totalAmount: Double,
    itemsArray: List<OrderItemUiModel>,
    subTotalAmount: Double,
    shippingAmount: Double,
    taxAmount: Double,
    surchargeDetails: List<SurchargeModel>,
    selectedPaymentMethod: String,
    selectedNetwork : String,
    currencySymbol: String,
    buttonColor: String
) {
    var isExpanded by remember { mutableStateOf(false) }

    val itemHeight = 70.dp
    val maxVisibleItems = 3
    val scrollHeight = itemHeight * minOf(itemsArray.size, maxVisibleItems)

    val cardShape = RoundedCornerShape(12.dp)

    val amountAfterSurcharge = remember {
        mutableStateOf(totalAmount)
    }

    val filteredSurcharges = remember {
        mutableStateOf<List<SurchargeModel>>(emptyList())
    }

    val isTablet = isTabletDevice()

    LaunchedEffect(selectedPaymentMethod, selectedNetwork) {
        filteredSurcharges.value = surchargeDetails.filter { item ->
            val applicable = item.applicableOn.lowercase().trim()
            val matches = applicable.isEmpty() ||
                    (applicable == selectedPaymentMethod.lowercase().trim() && item.network.replace(" ", "").equals(selectedNetwork.replace(" ", ""), true)) ||
                    (applicable == selectedPaymentMethod.lowercase().trim() && item.network.isBlank())

            if (item.network.equals("UpiQr", true) || item.network.equals("UpiQrOtm", true)) {
                matches && isTablet
            } else {
                matches
            }
        }

        println("=====filteredsurcharge ${filteredSurcharges.value}")

        amountAfterSurcharge.value = filteredSurcharges.value.sumOf { it.amount } + totalAmount

        CheckoutDetailsHandler.setAmount(amountAfterSurcharge.value)
    }

    if (isExpanded) {
        // ── Expanded card ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(Color.White, cardShape)
                .border(1.dp, Color(0xFFF1F1F1), cardShape)
                .clip(cardShape)
                .padding(vertical = 16.dp)
        ) {
            // Header row — tap to collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = false }
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Price Details",
                    fontSize = 14.sp,
                    color = Color(0xFF363840),
                    fontFamily = LocalSDKFonts.current.primary,
                    fontWeight = FontWeight.SemiBold
                )
                ChevronIcon()
            }

            Spacer(Modifier.height(12.dp))

            // Items list — fixed height, scrollable
            Column(
                modifier = Modifier
                    .height(scrollHeight)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                itemsArray.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Product image
                        if (item.imageUrl?.isNotEmpty() == true) {
                            KamelImage(
                                resource              = asyncPainterResource(data = item.imageUrl),
                                contentDescription = "Saved Card",
                                modifier           = Modifier.size(40.dp),
                                onLoading = {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = Color(0xFFE6E6E6),
                                                RoundedCornerShape(12.dp)
                                            )
                                    )
                                },
                                onFailure = {
                                    Image(
                                        painter = painterResource(Res.drawable.ic_broken_order_image),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            )
                        }

                        // Title + Qty
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = item.imageTitle ?: "",
                                fontSize = 12.sp,
                                color = Color(0xFF2D2B32),
                                fontFamily = LocalSDKFonts.current.primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Qty: ${item.imageQty}",
                                fontSize = 12.sp,
                                color = Color(0xFF2D2B32),
                                fontFamily = LocalSDKFonts.current.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Amount
                        Text(
                            text = buildAnnotatedString {
                                append(" $currencySymbol")
                                append(" ${item.amount}")
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF2D2B32),
                            fontFamily = LocalSDKFonts.current.primary
                        )
                    }
                }
            }

            // Dashed divider (shown when any summary line is present)
            if (subTotalAmount != 0.0 || taxAmount != 0.0 || shippingAmount != 0.0) {
                DashedDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                )
            }

            // Subtotal
            if (subTotalAmount != 0.0) {
                SummaryRow(
                    label = "Subtotal",
                    amount = subTotalAmount,
                    currencySymbol = currencySymbol,
                    buttonColor = buttonColor
                )
            }

            // Tax
            if (taxAmount != 0.0) {
                SummaryRow(
                    label = "Taxes and Fees",
                    amount = taxAmount,
                    currencySymbol = currencySymbol,
                    buttonColor = buttonColor
                )
            }

            // Shipping
            if (shippingAmount != 0.0) {
                SummaryRow(
                    label = "Shipping Amount",
                    amount = shippingAmount,
                    currencySymbol = currencySymbol,
                    buttonColor = buttonColor
                )
            }

            // Surcharges — filtered by selectedPaymentMethod
            filteredSurcharges.value.forEach { item ->
                SummaryRow(
                    label = item.title,
                    amount = item.amount,
                    currencySymbol = "+ $currencySymbol",
                    buttonColor = buttonColor,
                    description = item.description
                )
            }

            // Total row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(Color(0xFFF1F1F1), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total",
                    fontSize = 16.sp,
                    color = Color(0xFF1D1C20),
                    fontFamily = LocalSDKFonts.current.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontFamily = LocalSDKFonts.current.secondary)) {
                            append(" $currencySymbol")
                        }
                        append(" ${formatAmount(amountAfterSurcharge.value)}")
                    },
                    fontSize = 16.sp,
                    color = Color(0xFF1D1C20),
                    fontFamily = LocalSDKFonts.current.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    } else {
        // ── Collapsed card ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(Color.White, cardShape)
                .border(1.dp, Color(0xFFF1F1F1), cardShape)
                .clip(cardShape)
                .clickable { isExpanded = true }
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Price Details",
                fontSize = 14.sp,
                color = Color(0xFF363840),
                fontFamily = LocalSDKFonts.current.primary,
                fontWeight = FontWeight.SemiBold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontFamily = LocalSDKFonts.current.secondary)) {
                            append(currencySymbol)
                        }
                        append(" ${formatAmount(amountAfterSurcharge.value)}")
                    },
                    fontSize = 14.sp,
                    color = Color(0xFF363840),
                    fontFamily = LocalSDKFonts.current.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(6.dp))
                ChevronIcon()
            }
        }
    }
}

// ── Reusable sub-composables ─────────────────────────────────────────────────

@Composable
internal fun SummaryRow(
    label: String,
    amount: Double,
    currencySymbol: String,
    buttonColor: String,
    description: String? = null
) {
    val isDescriptionVisible = remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label + icon grouped — this box always claims the full leftover space
        Row(
            modifier = Modifier.weight(1f), // fill = true (default)
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color(0xFF2D2B32),
                fontFamily = LocalSDKFonts.current.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false) // shrink-to-fit, keeps icon adjacent
            )

            if (!description.isNullOrBlank()) {
                Box(modifier = Modifier.padding(start = 4.dp)) {
                    Image(
                        painter = painterResource(Res.drawable.ic_cvv_info),
                        contentDescription = "info icon",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { isDescriptionVisible.value = true },
                        colorFilter = ColorFilter.tint(buttonColor.toComposeColor())
                    )

                    if (isDescriptionVisible.value) {
                        InfoTooltip(
                            text = description,
                            onDismiss = { isDescriptionVisible.value = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontFamily = LocalSDKFonts.current.secondary)) {
                    append(currencySymbol)
                }
                append(" ${formatAmount(amount)}")
            },
            fontSize = 14.sp,
            color = Color(0xFF2D2B32),
            fontFamily = LocalSDKFonts.current.primary,
            fontWeight = FontWeight.SemiBold
            // no weight, no textAlign needed — it now sits flush right naturally
        )
    }
}

@Composable
private fun InfoTooltip(
    text: String,
    onDismiss: () -> Unit
) {
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(x = -40, y = -170), // tweak to position above icon with caret pointing to it
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xFF2D2B32),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .widthIn(max = 220.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = text,
                    fontSize = 13.sp,
                    fontFamily = LocalSDKFonts.current.primary,
                    color = Color.White,
                    lineHeight = 17.sp
                )
            }
        }
    }
}


// Dashed divider drawn with Canvas (DrawScope supports PathEffect natively).
@Composable
private fun DashedDivider(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.height(1.5.dp)) {
        drawLine(
            color = Color(0xFFE6E6E6),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                floatArrayOf(10f, 8f), 0f
            )
        )
    }
}