package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod
import com.crossplatform.sdk.presentation.ChevronIcon
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_upi_error
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun ExpandablePaymentSection(
    title: String,
    image: DrawableResource,
    providerList: List<SelectedPaymentMethod>,
    surchargeFee: Double?,
    currencySymbol: String,
    amount: Double,
    selectedId: String,
    buttonTextColor: String,
    buttonColor: String,
    ctaBorderRadius: Int,
    onClickRadio: (String) -> Unit,
    onProceedForward: (instrumentType: String, instrumentValue: String, type: String) -> Unit,
    onViewMore: () -> Unit,
    isBoxPayPayButtonVisible: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val hasMore = providerList.size > 4

    Column(modifier = Modifier.fillMaxWidth()) {

        // Header row — same as MorePaymentContainer, but toggles expansion
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(start = 16.dp, bottom = 10.dp, top = 10.dp, end = 8.dp)
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(image),
                contentDescription = title,
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontFamily = LocalSDKFonts.current.primary,
                    fontWeight = FontWeight.Medium
                )
                if (surchargeFee != null && surchargeFee != 0.0) {
                    Text(
                        text = "$currencySymbol $surchargeFee extra applied as surcharge",
                        fontSize = 14.sp,
                        fontFamily = LocalSDKFonts.current.primary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF32CD32)
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            ChevronIcon()
        }

        // Expanded content — inline selector
        if (expanded && providerList.isNotEmpty()) {
            providerList.forEachIndexed { _, provider ->
                PaymentSelector(
                    id                  = provider.id,
                    title               = provider.displayName,
                    imageUrl            = provider.imageUrl,
                    isSelected          = provider.id == selectedId,
                    instrumentTypeValue = provider.instrumentType,
                    isLastUsed          = false,
                    onPress             = {
                        onClickRadio(it)
                    },
                    onProceedForward    = { displayValue, instrumentValue ->
                        onProceedForward(displayValue, instrumentValue, provider.type)
                    },
                    brandColor          = buttonColor,
                    buttonTextColor     = buttonTextColor,
                    currencySymbol      = currencySymbol,
                    amount              = amount,
                    ctaBorderRadius     = ctaBorderRadius,
                    drawableResource    = Res.drawable.ic_upi_error,
                    isBoxPayPayButtonVisible = isBoxPayPayButtonVisible
                )
                HorizontalDivider(
                    color     = Color(0xFFECECED),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            if (hasMore) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewMore() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View More",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = LocalSDKFonts.current.primary,
                        color = Color(0xFF1A73E8),
                        modifier = Modifier.weight(1f)
                    )
                    ChevronIcon()
                }
            }
        }
    }
}