package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.presentation.theme.LocalSDKColors
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.amex_cvv_position_desc
import crossplatformsdk.cross_platform_sdk.generated.resources.amex_cvv_position_title
import crossplatformsdk.cross_platform_sdk.generated.resources.cvv_info_image
import crossplatformsdk.cross_platform_sdk.generated.resources.cvv_info_image_amex
import crossplatformsdk.cross_platform_sdk.generated.resources.generic_position_for_cvv_desc
import crossplatformsdk.cross_platform_sdk.generated.resources.generic_position_for_cvv_title
import crossplatformsdk.cross_platform_sdk.generated.resources.got_it_cta
import crossplatformsdk.cross_platform_sdk.generated.resources.where_to_find_cvv_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.crossplatform.sdk.presentation.theme.LocalSDKColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CvvInfoBottomSheet(onClick: () -> Unit, buttonColor : String, borderRadius : Int, buttonTextColor : String) {
    ModalBottomSheet(
        onDismissRequest = onClick,
        dragHandle       = null,
        containerColor   = LocalSDKColors.current.background,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {

            // --- Title ---
            Text(
                text       = stringResource(Res.string.where_to_find_cvv_title),
                fontFamily = LocalSDKFonts.current.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 18.sp,
                modifier   = Modifier.padding(bottom = 16.dp)
            )

            // --- Generic CVV Image ---
            Image(
                painter            = painterResource(Res.drawable.cvv_info_image),
                contentDescription = "CVV position",
                modifier           = Modifier
                    .height(60.dp),
                contentScale       = ContentScale.Fit
            )

            // --- Generic CVV Info ---
            Text(
                text       = stringResource(Res.string.generic_position_for_cvv_title),
                fontFamily = LocalSDKFonts.current.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 14.sp,
                modifier   = Modifier.padding(top = 12.dp)
            )
            Text(
                text       = stringResource(Res.string.generic_position_for_cvv_desc),
                fontFamily = LocalSDKFonts.current.primary,
                fontWeight = FontWeight.Normal,
                fontSize   = 14.sp,
                color      = Color(0xFF888780),
                modifier   = Modifier.padding(top = 4.dp)
            )

            // --- Divider ---
            HorizontalDivider(
                color    = LocalSDKColors.current.divider,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // --- Amex CVV Image ---
            Image(
                painter            = painterResource(Res.drawable.cvv_info_image_amex),
                contentDescription = "Amex CVV position",
                modifier           = Modifier
                    .height(60.dp),
                contentScale       = ContentScale.Fit
            )

            // --- Amex CVV Info ---
            Text(
                text       = stringResource(Res.string.amex_cvv_position_title),
                fontFamily = LocalSDKFonts.current.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 14.sp,
                modifier   = Modifier.padding(top = 12.dp)
            )
            Text(
                text       = stringResource(Res.string.amex_cvv_position_desc),
                fontFamily = LocalSDKFonts.current.primary,
                fontWeight = FontWeight.Normal,
                fontSize   = 14.sp,
                color      = Color(0xFF888780),
                modifier   = Modifier.padding(top = 4.dp)
            )

            // --- Got it Button ---
            PayButton(
                text = stringResource(Res.string.got_it_cta),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .clip(RoundedCornerShape(borderRadius.dp))
                    .background(buttonColor.toComposeColor())
                    .clickable { onClick() },
                amount = 0.0,
                currencySymbol = "",
                isValid = true,
                buttonTextColor = buttonTextColor
            )
        }
    }
}