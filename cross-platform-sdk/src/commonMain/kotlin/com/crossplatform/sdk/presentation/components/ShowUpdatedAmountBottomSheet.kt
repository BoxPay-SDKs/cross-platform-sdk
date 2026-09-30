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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.crossplatform.sdk.domain.model.SurchargeModel
import com.crossplatform.sdk.presentation.formatAmount
import com.crossplatform.sdk.presentation.theme.LocalSDKColors
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.order_summary_info
import crossplatformsdk.cross_platform_sdk.generated.resources.proceed_to_pay_cta
import crossplatformsdk.cross_platform_sdk.generated.resources.sub_total_info
import crossplatformsdk.cross_platform_sdk.generated.resources.total_info
import org.jetbrains.compose.resources.stringResource
import com.crossplatform.sdk.presentation.theme.LocalSDKColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShowUpdateAmountBottomSheet(
    selectedMethod : String,
    onClickProceed : (amountAfterSurcharge : Double) -> Unit,
    onClick : () -> Unit,
    currencySymbol : String,
    amount : Double,
    surchargeDetails : List<SurchargeModel>,
    ctaBorderRadius :Int,
    buttonColor : String,
    buttonTextColor : String
) {
    val amountAfterSurcharge = remember {
        mutableStateOf(amount)
    }

    val filteredSurcharges = remember {
        mutableStateOf<List<SurchargeModel>>(emptyList())
    }

    LaunchedEffect(selectedMethod) {
        filteredSurcharges.value = surchargeDetails.filter { item ->
            val applicable = item.applicableOn.lowercase().trim()
            applicable == selectedMethod.lowercase().trim() && item.network.isBlank()
        }


        amountAfterSurcharge.value = filteredSurcharges.value.sumOf { it.amount } + amount
    }

    ModalBottomSheet(
        onDismissRequest = onClick,
        dragHandle       = null,
        containerColor   = LocalSDKColors.current.background,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 12.dp)
        ) {

            // Title
            Text(
                text = stringResource(Res.string.order_summary_info),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = LocalSDKColors.current.textPrimary,
                modifier = Modifier.padding(bottom = 12.dp, start = 12.dp, end = 16.dp)
            )

            // Base Amount Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 12.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(Res.string.sub_total_info),
                    fontWeight = FontWeight.Normal,
                    fontFamily = LocalSDKFonts.current.primary,
                    fontSize = 14.sp,
                    color = LocalSDKColors.current.textPrimary
                )
                Text(
                    text  = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontFamily = LocalSDKFonts.current.secondary
                            )
                        ) {
                            append(currencySymbol)
                        }
                        withStyle(
                            style = SpanStyle(
                                fontFamily = LocalSDKFonts.current.primary
                            )
                        ) {
                            append(" ${formatAmount(amount)}")
                        }
                    },
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    fontFamily = LocalSDKFonts.current.primary,
                    color = LocalSDKColors.current.textPrimary
                )
            }

            // Surcharge Rows — mirrors your displaySurcharge logic
            filteredSurcharges.value.forEach { item ->
                SummaryRow(
                    label = item.title,
                    amount = item.amount,
                    currencySymbol = "+ $currencySymbol",
                    buttonColor = buttonColor,
                    description = item.description
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = LocalSDKColors.current.divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp,start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(Res.string.total_info),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = LocalSDKColors.current.textPrimary,
                    fontFamily = LocalSDKFonts.current.primary
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontFamily = LocalSDKFonts.current.secondary
                            )
                        ) {
                            append(currencySymbol)
                        }
                        withStyle(
                            style = SpanStyle(
                                fontFamily = LocalSDKFonts.current.primary
                            )
                        ) {
                            append(" ${formatAmount(amountAfterSurcharge.value)}")
                        }
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = LocalSDKFonts.current.primary,
                    fontSize = 16.sp,
                    color = LocalSDKColors.current.textPrimary
                )
            }
            PayButton(
                text = stringResource(Res.string.proceed_to_pay_cta),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(ctaBorderRadius.dp))
                    .background(buttonColor.toComposeColor(), RoundedCornerShape(ctaBorderRadius.dp))
                    .clickable{
                        CheckoutDetailsHandler.setAmount(amountAfterSurcharge.value)
                        onClickProceed(amountAfterSurcharge.value)
                    },
                amount = 0.0,
                currencySymbol= "",
                isValid = true,
                buttonTextColor = buttonTextColor,
            )
        }
    }
}