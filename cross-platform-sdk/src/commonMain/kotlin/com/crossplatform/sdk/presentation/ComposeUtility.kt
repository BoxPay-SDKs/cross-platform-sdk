package com.crossplatform.sdk.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.chervon_down
import org.jetbrains.compose.resources.painterResource

@Composable
fun ChevronIcon() {
    Image(
        painter = painterResource(Res.drawable.chervon_down),
        contentDescription = null,
        modifier = Modifier
            .padding(bottom = 10.dp)
            .size(width = 30.dp, height = 30.dp)
            .rotate(-90f)
            .padding(end = 10.dp, bottom = 20.dp)
    )
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text       = title,
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 14.sp,
        modifier   = Modifier.padding(bottom = 8.dp, start = 16.dp, end = 16.dp, top = 16.dp)
    )
}

@Composable
fun ErrorText(message: String) {
    Text(
        text       = message,
        fontSize   = 12.sp,
        color      = Color(0xFFE12121),
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Normal,
        modifier   = Modifier.padding(start = 16.dp, top = 2.dp)
    )
}