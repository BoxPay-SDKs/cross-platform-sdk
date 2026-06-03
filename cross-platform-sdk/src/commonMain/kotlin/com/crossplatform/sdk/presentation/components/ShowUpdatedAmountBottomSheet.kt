package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
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
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.CheckoutDetails
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import com.crossplatform.sdk.presentation.theme.defaultInterFontFamily
import com.crossplatform.sdk.presentation.toComposeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowUpdateAmountBottomSheet(
    checkoutDetails: CheckoutDetails,
    selectedMethod : String,
    onClickProceed : () -> Unit,
    onClick : () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onClick,
        dragHandle       = null,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            // Title
            Text(
                text = "Order Summary",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color(0xFF010102),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Base Amount Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sub Total",
                    fontWeight = FontWeight.Normal,
                    fontFamily = defaultFontFamily,
                    fontSize = 12.sp,
                    color = Color(0xFF010102)
                )
                Text(
                    text  = buildAnnotatedString {
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
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    fontFamily = defaultFontFamily,
                    color = Color(0xFF010102)
                )
            }

            // Surcharge Rows — mirrors your displaySurcharge logic
            checkoutDetails.surchargeDetails.forEach { item ->

                if (item.applicableOn.lowercase() == selectedMethod) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            fontFamily = defaultFontFamily,
                            color = Color(0xFF010102)
                        )
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        fontFamily = defaultInterFontFamily
                                    )
                                ) {
                                    append("+ ${checkoutDetails.currencySymbol}")
                                }
                                withStyle(
                                    style = SpanStyle(
                                        fontFamily = defaultFontFamily
                                    )
                                ) {
                                    append("${item.amount}")
                                }
                            },
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            fontFamily = defaultFontFamily,
                            color = Color(0xFF010102)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Total Row
            val totalSurcharge = checkoutDetails.surchargeDetails
                .filter { it.applicableOn.equals(selectedMethod, ignoreCase = true) }
                .sumOf { it.amount }

            val baseAmount = checkoutDetails.amount
            val totalAmount = baseAmount + totalSurcharge

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Payable",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF010102),
                    fontFamily = defaultFontFamily
                )
                Text(
                    text = buildAnnotatedString {
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
                            append("$totalAmount")
                        }
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = defaultFontFamily,
                    fontSize = 14.sp,
                    color = Color(0xFF010102)
                )
            }
            PayButton(
                text = "Proceed to Pay",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(checkoutDetails.ctaBorderRadius.dp))
                    .background(checkoutDetails.buttonColor.toComposeColor(), RoundedCornerShape(checkoutDetails.ctaBorderRadius.dp))
                    .clickable{
                        CheckoutDetailsHandler.setAmount(totalAmount)
                        onClickProceed()
                    },
                amount = 0.0,
                currencySymbol= "",
                isValid = true,
                buttonTextColor = checkoutDetails.buttonTextColor,
            )
        }
    }
}