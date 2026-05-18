package com.application.androidkmpapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.presentation.theme.defaultFontFamily

@Composable
fun TokenScreen(onChangeToken : (String) -> Unit) {
    val tokenValue = remember{ mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value  = tokenValue.value,
            enabled = true,
            onValueChange   = {new : String ->
                tokenValue.value = new
            },
            label = {
                Text(
                    text = "Enter token",
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize   = 16.sp
                )
            },
            shape           = RoundedCornerShape(8.dp),
            modifier        = Modifier
                .fillMaxWidth()
                .height(62.dp),
            textStyle       = TextStyle(
                fontFamily = defaultFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize   = 16.sp,
                color      = Color(0xFF0A090B)
            ),
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (tokenValue.value.isNotEmpty()) Color(0xFF1CA672) else Color(0xFFE6E6E6))
                .clickable(enabled = tokenValue.value.isNotEmpty()) { onChangeToken(tokenValue.value) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "Proceed",
                fontSize   = 16.sp,
                fontFamily = defaultFontFamily,
                fontWeight = FontWeight.SemiBold,
                color      = if (tokenValue.value.isNotEmpty()) Color.White else Color(0xFFADACAD),
                modifier   = Modifier.padding(vertical = 14.dp)
            )
        }
    }
}

