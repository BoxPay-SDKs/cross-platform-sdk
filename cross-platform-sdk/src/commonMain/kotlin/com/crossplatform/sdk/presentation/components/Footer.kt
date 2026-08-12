package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.splash_icon
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun Footer() {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text       = "Secured by",
            fontSize   = 12.sp,
            fontFamily = LocalSDKFonts.current.primary,
            fontWeight = FontWeight.Medium,
            color      = Color(0xFF888888)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Image(
            painter            = painterResource(Res.drawable.splash_icon),
            contentDescription = null,
            modifier           = Modifier.size(50.dp)
        )
        Text(
            text       = "· Terms & Conditions",
            fontSize   = 12.sp,
            fontFamily = LocalSDKFonts.current.primary,
            fontWeight = FontWeight.Medium,
            color      = Color(0xFF888888),
            textDecoration = TextDecoration.Underline,
            modifier   = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple()
                ) {
                    uriHandler.openUri("https://www.boxpay.tech/terms-conditions")
                }
        )
    }
}