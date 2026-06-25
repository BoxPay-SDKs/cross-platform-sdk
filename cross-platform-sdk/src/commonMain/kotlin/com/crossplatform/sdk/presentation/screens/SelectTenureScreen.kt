package com.crossplatform.sdk.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.domain.model.Bank
import com.crossplatform.sdk.presentation.components.FilterTag
import com.crossplatform.sdk.presentation.components.Footer
import com.crossplatform.sdk.presentation.components.PayButton
import com.crossplatform.sdk.presentation.formatPercent
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_netbanking
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun SelectTenureScreen(
    selectedBank: Bank?,
    cardType: String,
    selectedEmi: Pair<Int?, String?>,
    buttonColor: String,
    buttonTextColor: String,
    onClickRadio: (duration: Int, amount: String, code: String?) -> Unit,
    onProceed: (Double, Boolean, Boolean, discount: String, netAmount: String) -> Unit,
    currencySymbol: String,
    ctaBorderRadius : Int
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 30.dp)
                .fillMaxWidth()
                .wrapContentHeight()
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE6E6E6), RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
        ) {
            // ── Bank header row ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KamelImage(
                    resource              = asyncPainterResource(data = selectedBank?.iconUrl ?: ""),
                    contentDescription = "Bank Logo",
                    modifier           = Modifier.size(32.dp),
                    onLoading = {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFE0E0E0), CircleShape)
                        )
                    },
                    onFailure = {
                        Image(
                            painter = painterResource(Res.drawable.ic_netbanking),
                            contentDescription = "Back",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                )
                Text(
                    text = "${selectedBank?.name} | $cardType EMI",
                    style = TextStyle(fontFamily = LocalSDKFonts.current.primary, fontSize = 16.sp, fontWeight = FontWeight(600)),
                    color = Color(0xFF2D2B32),
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            // ── EMI rows ─────────────────────────────────────────────────
            selectedBank?.emiList?.forEachIndexed { index, emi ->
                EmiAmountDetails(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clickable { onClickRadio(emi.duration, emi.amount, emi.code) },
                    isSelected = selectedEmi.first == emi.duration && selectedEmi.second == emi.amount,
                    onClickRadio = { onClickRadio(emi.duration, emi.amount, emi.code) },
                    selectedColor = buttonColor,
                    currencySymbol = currencySymbol,
                    month = emi.duration,
                    amount = emi.amount,
                    percent = emi.percent,
                    discount = emi.discount,
                    interest = emi.interestCharged,
                    total = emi.totalAmount,
                    processingFee = emi.processingFee,
                    bankName = selectedBank.name,
                    onProceed = {
                        onProceed(
                            emi.percent,
                            emi.lowCostApplied,
                            emi.noCostApplied,
                            emi.discount,
                            emi.netAmount
                        )
                    },
                    isNoCostApplied = emi.noCostApplied,
                    isLowCostApplied = emi.lowCostApplied,
                    selectedTextColor = buttonTextColor,
                    netAmount = emi.netAmount,
                    ctaBorderRadius = ctaBorderRadius
                )
                if (index < selectedBank.emiList.lastIndex) {
                    HorizontalDivider(modifier = Modifier.fillMaxWidth())
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Footer()
    }
}


@Composable
fun EmiAmountDetails(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    onClickRadio: () -> Unit,
    selectedColor: String,
    month: Int,
    amount: String,
    percent: Double,
    total: String,
    interest: String,
    discount: String,
    processingFee: String,
    bankName: String,
    onProceed: () -> Unit,
    isNoCostApplied: Boolean,
    isLowCostApplied: Boolean,
    currencySymbol: String,
    selectedTextColor: String,
    netAmount: String,
    ctaBorderRadius : Int
) {
    Column(
        modifier = modifier
            .background(if (isSelected) Color(0xFFEFF3FA) else Color.White)
            .padding(bottom = 8.dp)
    ) {
        // ── Radio + heading + tags row ────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClickRadio,
                colors = RadioButtonDefaults.colors(selectedColor = selectedColor.toComposeColor())
            )

            Text(
                text = buildAnnotatedString {
                    append(
                        AnnotatedString(
                            text = "$month months x ",
                            spanStyle = SpanStyle(
                                fontFamily = LocalSDKFonts.current.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight(600)
                            )
                        )
                    )
                    append(
                        AnnotatedString(
                            text = currencySymbol,
                            spanStyle = SpanStyle(
                                fontFamily = LocalSDKFonts.current.secondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight(600)
                            )
                        )
                    )
                    append(
                        AnnotatedString(
                            text = amount,
                            spanStyle = SpanStyle(
                                fontFamily = LocalSDKFonts.current.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight(600)
                            )
                        )
                    )
                    if (!isSelected) {
                        append(
                            AnnotatedString(
                                text = " | @${formatPercent(percent)}% p.a.",
                                spanStyle = SpanStyle(
                                    fontFamily = LocalSDKFonts.current.primary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight(600)
                                )
                            )
                        )
                    }
                },
                color = Color(0xFF4F4D55),
                modifier = Modifier.padding(start = 8.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Tags sit right after the text
            if (isNoCostApplied) {
                FilterTag(text = "NO COST EMI")
            } else if (isLowCostApplied) {
                FilterTag(text = "LOW COST EMI")
            }
        }

        // ── Expanded content ──────────────────────────────────────────────
        if (isSelected) {
            TableDetails(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                amount = amount,
                total = total,
                interest = interest,
                discount = discount,
                interestRate = formatPercent(percent),
                isNoCostApplied = isNoCostApplied,
                currencySymbol = currencySymbol
            )

            // ── Charge note ───────────────────────────────────────────────
            Text(
                text = buildAnnotatedString {
                    AppendStyled("Your card will be charged for an amount of", 400)
                    AppendCurrency(currencySymbol)
                    AppendStyled("$netAmount.", 600)
                    AppendStyled(" You will be charged an interest of ", 400)
                    AppendCurrency(currencySymbol)
                    AppendStyled(interest, 600)
                    AppendStyled(" by the bank making the total payable amount as", 400)
                    AppendCurrency(currencySymbol)
                    AppendStyled(total, 600)
                },
                color = Color(0xFF2D2B32),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp)
            )

            // ── Processing fee ────────────────────────────────────────────
            Text(
                text = buildAnnotatedString {
                    if (processingFee == "0") {
                        AppendStyled("No Processing Fee will be charged by $bankName", 600)
                    } else {
                        AppendCurrency(currencySymbol)
                        AppendStyled("$processingFee+GST", 600)
                        AppendStyled(" will be charged by $bankName as one-time processing fee.", 400)
                    }
                },
                color = Color(0xFF2D2B32),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp)
            )

            // ── CTA button ────────────────────────────────────────────────
            PayButton(
                text = "Proceed to Enter Card Details",
                modifier =  Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .clip(RoundedCornerShape(ctaBorderRadius.dp))
                    .background(
                         selectedColor.toComposeColor()
                    )
                    .clickable {
                        onProceed()
                    },
                amount = 0.0,
                currencySymbol = "",
                isValid = true,
                buttonTextColor = selectedTextColor
            )
        }
    }
}

@Composable
fun TableDetails(
    modifier: Modifier = Modifier,
    amount: String,
    interest: String,
    discount: String?,
    total: String,
    interestRate: String,
    isNoCostApplied: Boolean,
    currencySymbol: String
) {
    Column(modifier.border(1.dp, Color(0xFFE6E6E6), RoundedCornerShape(12.dp))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color(0xFFF1F1F1),
                    RoundedCornerShape(topEnd = 12.dp, topStart = 12.dp)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Monthly EMI",
                style = TextStyle(
                    fontFamily = LocalSDKFonts.current.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight(600)
                ),
                color = Color(0xFF2D2B32),
                modifier = Modifier
                    .weight(0.3f)
                    .padding(8.dp)
            )
            Text(
                text = "Interest @$interestRate% p.a.",
                style = TextStyle(
                    fontFamily = LocalSDKFonts.current.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight(600)
                ),
                color = Color(0xFF2D2B32),
                modifier = Modifier
                    .weight(0.3f)
                    .padding(8.dp)
            )
            if (isNoCostApplied) {
                Text(
                    text = "Discount",
                    style = TextStyle(
                        fontFamily = LocalSDKFonts.current.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight(600)
                    ),
                    color = Color(0xFF2D2B32),
                    modifier = Modifier
                        .weight(0.3f)
                        .padding(8.dp)
                )
            }
            Text(
                text = "Total Cost",
                style = TextStyle(
                    fontFamily = LocalSDKFonts.current.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight(600)
                ),
                color = Color(0xFF2D2B32),
                modifier = Modifier
                    .weight(0.3f)
                    .padding(8.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color.White,
                    RoundedCornerShape(bottomEnd = 12.dp, bottomStart = 12.dp)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = buildAnnotatedString {
                    append(
                        AnnotatedString(
                            text = currencySymbol,
                            spanStyle = SpanStyle(
                                fontFamily = LocalSDKFonts.current.secondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight(200)
                            )
                        )
                    )
                    append(
                        AnnotatedString(
                            text = amount,
                            spanStyle = SpanStyle(
                                fontFamily = LocalSDKFonts.current.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight(400)
                            )
                        )
                    )
                },
                color = Color(0xFF2D2B32),
                modifier = Modifier
                    .weight(0.3f)
                    .padding(10.dp)
            )
            Text(
                text = buildAnnotatedString {
                    append(
                        AnnotatedString(
                            text = currencySymbol,
                            spanStyle = SpanStyle(
                                fontFamily = LocalSDKFonts.current.secondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight(200)
                            )
                        )
                    )
                    append(
                        AnnotatedString(
                            text = interest,
                            spanStyle = SpanStyle(
                                fontFamily = LocalSDKFonts.current.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight(400)
                            )
                        )
                    )
                },
                color = Color(0xFF2D2B32),
                modifier = Modifier
                    .weight(0.3f)
                    .padding(10.dp)
            )
            if (isNoCostApplied && !discount.isNullOrBlank()) {
                Text(
                    text = buildAnnotatedString {
                        append(
                            AnnotatedString(
                                text = "-$currencySymbol",
                                spanStyle = SpanStyle(
                                    fontFamily = LocalSDKFonts.current.secondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight(200)
                                )
                            )
                        )
                        append(
                            AnnotatedString(
                                text = discount,
                                spanStyle = SpanStyle(
                                    fontFamily = LocalSDKFonts.current.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight(200)
                                )
                            )
                        )
                    },
                    color = Color(0xFF1CA672),
                    modifier = Modifier
                        .weight(0.3f)
                        .padding(10.dp)
                )
            }
            Text(
                text = buildAnnotatedString {
                    append(
                        AnnotatedString(
                            text = currencySymbol,
                            spanStyle = SpanStyle(
                                fontFamily = LocalSDKFonts.current.secondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight(200)
                            )
                        )
                    )
                    append(
                        AnnotatedString(
                            text = total,
                            spanStyle = SpanStyle(
                                fontFamily = LocalSDKFonts.current.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight(600)
                            )
                        )
                    )
                },
                color = Color(0xFF2D2B32),
                modifier = Modifier
                    .weight(0.3f)
                    .padding(10.dp)
            )
        }
    }
}

@Composable
private fun AnnotatedString.Builder.AppendStyled(text: String, weight: Int) {
    append(
        AnnotatedString(
            text = text,
            spanStyle = SpanStyle(
                fontFamily = LocalSDKFonts.current.primary,
                fontWeight = FontWeight(weight),
                fontSize = 12.sp
            )
        )
    )
}

@Composable
private fun AnnotatedString.Builder.AppendCurrency(symbol: String) {
    append(
        AnnotatedString(
            text = " $symbol",
            spanStyle = SpanStyle(
                fontFamily = LocalSDKFonts.current.secondary,
                fontWeight = FontWeight(200),
                fontSize = 12.sp
            )
        )
    )
}