package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import com.crossplatform.sdk.presentation.theme.defaultInterFontFamily
import com.crossplatform.sdk.presentation.toComposeColor

@Composable
fun PayButton(
    text : String,
    modifier : Modifier,
    amount : Double,
    currencySymbol : String,
    isValid : Boolean,
    buttonTextColor : String
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontFamily = defaultFontFamily
                    )
                ) {
                    append("$text ")
                }
                if(currencySymbol.isNotEmpty()) {
                    withStyle(
                        style = SpanStyle(
                            fontFamily = defaultInterFontFamily
                        )
                    ) {
                        append(currencySymbol)
                    }
                }
                if(amount != 0.0) {
                    withStyle(
                        style = SpanStyle(
                            fontFamily = defaultFontFamily
                        )
                    ) {
                        append("$amount")
                    }
                }
            },
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            color      = if (isValid) buttonTextColor.toComposeColor() else Color(0xFFADACAD),
            fontSize   = 16.sp,
            modifier   = Modifier.padding(vertical = 14.dp)
        )
    }
}