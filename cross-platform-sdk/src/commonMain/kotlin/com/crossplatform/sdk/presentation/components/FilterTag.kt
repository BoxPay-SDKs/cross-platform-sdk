package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts

@Composable
fun FilterTag(modifier: Modifier = Modifier, text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontFamily = LocalSDKFonts.current.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight(500)
        ),
        color = Color(0xFFEB2F96),
        modifier = modifier
            .border(1.dp, Color(0xFFFFADD2), RoundedCornerShape(4.dp))
            .background(Color(0xFFFFF0F6), RoundedCornerShape(4.dp))
            .padding(vertical = 2.dp, horizontal = 4.dp)
    )
}