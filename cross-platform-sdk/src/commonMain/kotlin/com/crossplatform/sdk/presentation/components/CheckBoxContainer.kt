package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import com.crossplatform.sdk.presentation.toComposeColor

@Composable
fun CheckBoxContainer(
    text: String,
    isCheckBoxSelected: Boolean,
    onCheckBoxClicked: () -> Unit,
    buttonColor: String,
    isSICheckboxEnabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCheckBoxSelected) {
            Text(
                text = "✓",
                color = Color.White,
                fontFamily = defaultFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 16.sp,
                modifier = Modifier
                    .size(20.dp)
                    .then(
                        if (isCheckBoxSelected)
                            Modifier.background(buttonColor.toComposeColor(), RoundedCornerShape(3.dp))
                        else
                            Modifier.background(Color.Transparent, RoundedCornerShape(3.dp))
                    )
                    .border(2.dp, buttonColor.toComposeColor(), RoundedCornerShape(3.dp))
                    .then(
                        if (isSICheckboxEnabled)
                            Modifier.clickable { onCheckBoxClicked() }
                        else
                            Modifier
                    )
                    .wrapContentSize(Alignment.Center)
            )
        }

        // Label
        Text(
            text = text,
            color = Color(0xFF2D2B32),
            fontSize = 14.sp,
            fontFamily = defaultFontFamily,
            modifier = Modifier
                .padding(start = 6.dp)
        )
    }
}