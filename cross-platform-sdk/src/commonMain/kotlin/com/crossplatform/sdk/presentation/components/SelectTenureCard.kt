package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.crossplatform.sdk.domain.model.SelectTenureCardData
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import com.crossplatform.sdk.presentation.theme.defaultInterFontFamily
import com.crossplatform.sdk.presentation.toComposeColor

@Composable
fun SelectTenureCard(
    data: SelectTenureCardData,
    onRadioClick: (duration: Int, amount: String) -> Unit,
    onProceedForward: () -> Unit,
    buttonTextColor: String,
    buttonColor : String,
    currencySymbol: String
) {
    if (data.isSelected) {
        SelectedTenureCard(data, buttonColor.toComposeColor(), currencySymbol,buttonTextColor, onRadioClick, onProceedForward)
    } else {
        UnselectedTenureCard(data, buttonColor.toComposeColor(), currencySymbol,onRadioClick)
    }
}


// ─── Selected State ───────────────────────────────────────────────────────────

@Composable
private fun SelectedTenureCard(
    data: SelectTenureCardData,
    brandColor: Color,
    currencySymbol : String,
    buttonTextColor : String,
    onRadioClick: (Int, String) -> Unit,
    onProceedForward: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEFF3FA))
    ) {

        // ── Row: Radio + Title + Tags ─────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onRadioClick(data.duration, data.monthlyEmiAmount) }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = true,
                onClick = { onRadioClick(data.duration, data.monthlyEmiAmount) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = brandColor,
                    unselectedColor = Color(0x8001010273.toInt()),
                )
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            fontFamily = defaultFontFamily,
                            color = Color(0xFF2D2B32)
                        )
                    ) {
                        append("${data.duration} months x ")
                    }
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF2D2B32), fontFamily = defaultInterFontFamily)) {
                        append(currencySymbol)
                    }
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF2D2B32), fontFamily = defaultFontFamily)) {
                        append(data.monthlyEmiAmount)
                    }
                }
            )

            Spacer(modifier = Modifier.width(4.dp))
            if (data.isLowCostOffer) EmiTag("LOW COST EMI")
            if (data.isNoCostOffer)  EmiTag("NO COST EMI")
        }

        // ── Breakdown Table ───────────────────────────────────────────────
        val hasDiscount = data.discount != "0"
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .background(Color.White, shape = RoundedCornerShape(12.dp))
        ) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F1F1), shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TableHeaderCell("Monthly EMI", Modifier.weight(1f))
                TableHeaderCell("Interest @${data.interest}% p.a.", Modifier.weight(1f))
                if (hasDiscount) TableHeaderCell("Discount", Modifier.weight(1f))
                TableHeaderCell("Total Cost", Modifier.weight(1f))
            }

            // Value row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TableValueCell("$currencySymbol ${data.monthlyEmiAmount}", Modifier.weight(1f))
                TableValueCell("$currencySymbol ${data.interestCharged}", Modifier.weight(1f))
                if (hasDiscount) {
                    TableValueCell(
                        "-$currencySymbol ${data.discount}",
                        Modifier.weight(1f),
                        valueColor = Color(0xFF1CA672),
                    )
                }
                TableValueCell("$currencySymbol ${data.totalAmount}", Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Charge summary text ───────────────────────────────────────────
        Text(
            modifier = Modifier.padding(horizontal = 12.dp),
            text = buildAnnotatedString {
                append("Your card will be charged for an amount of ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontFamily = defaultFontFamily)) {
                    append("$currencySymbol ${data.debitedAmount}")
                }
                append(". You will be charged an interest of ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontFamily = defaultFontFamily)) {
                    append("$currencySymbol ${data.interestCharged}")
                }
                append(" by the bank making the total payable amount as ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontFamily = defaultFontFamily)) {
                    append("$currencySymbol ${data.totalAmount}")
                }
                append(".")
            },
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = Color(0xFF2D2B32),
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── Processing fee text ───────────────────────────────────────────
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontFamily = defaultFontFamily)) {
                    append("$currencySymbol ${data.processingFee}")
                }
                append(" + GST will be charged by HDFC bank as one-time processing fee.")
            },
            fontSize = 12.sp,
            color = Color(0xFF2D2B32),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Proceed Button ────────────────────────────────────────────────
        PayButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    onProceedForward()
                },
            amount = 0.0,
            currencySymbol = "",
            isValid = true,
            buttonTextColor = buttonTextColor,
            text = "Proceed to Enter Card Details"
        )
    }
}


// ─── Unselected State ─────────────────────────────────────────────────────────

@Composable
private fun UnselectedTenureCard(
    data: SelectTenureCardData,
    brandColor: Color,
    currencySymbol: String,
    onRadioClick: (Int, String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = false,
            onClick = { onRadioClick(data.duration, data.monthlyEmiAmount) },
            colors = RadioButtonDefaults.colors(
                selectedColor = brandColor,
                unselectedColor = Color(0x8001010273.toInt()),
            )
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF2D2B32), fontFamily = defaultFontFamily)) {
                        append("${data.duration} months x $currencySymbol ${data.monthlyEmiAmount} | @${data.interest}% p.a.")
                    }
                },
                modifier = Modifier.clickable { onRadioClick(data.duration, data.monthlyEmiAmount) }
            )

            Row(modifier = Modifier.padding(top = 4.dp)) {
                if (data.isLowCostOffer) EmiTag("LOW COST EMI")
                if (data.isNoCostOffer)  EmiTag("NO COST EMI")
            }
        }
    }
}


// ─── Reusable sub-components ──────────────────────────────────────────────────

@Composable
private fun EmiTag(label: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFFFF0F6),
        border = BorderStroke(1.dp, Color(0xFFFFADD2)),
        modifier = Modifier.padding(start = 4.dp),
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFFEB2F96),
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun TableHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 2.dp),
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        color = Color(0xFF2D2B32),
        fontFamily = defaultFontFamily,
        maxLines = 2,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun TableValueCell(
    text: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color(0xFF2D2B32),
) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 2.dp),
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = valueColor,
        fontFamily = defaultFontFamily,
        maxLines = 2,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}