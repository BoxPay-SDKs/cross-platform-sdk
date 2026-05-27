package com.crossplatform.sdk.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.presentation.components.Footer
import com.crossplatform.sdk.presentation.components.PayButton
import com.crossplatform.sdk.presentation.formatTimer
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import com.crossplatform.sdk.presentation.toComposeColor
import com.crossplatform.sdk.presentation.viewmodel.UpiTimerViewModel
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_info
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_upi
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun UpiTimerScreen(
    onBackPress : () -> Unit,
    shopperVpa : String
) {
    val viewModel : UpiTimerViewModel = koinViewModel()
    val checkoutDetails by CheckoutDetailsHandler.checkoutDetailsFlow.collectAsStateWithLifecycle()
    val timeRemaining by viewModel.timeRemaining.collectAsStateWithLifecycle()
    var showCancelModal by remember { mutableStateOf(false) }
    val isUrgent      = timeRemaining <= 30
    val progressColor = if (isUrgent) Color(0xFFFAA4A4) else checkoutDetails.buttonColor.toComposeColor()
    val textColor     = if (isUrgent) Color(0xFFF53535) else checkoutDetails.buttonColor.toComposeColor()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Complete your payment",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A1A1A),
            fontFamily = defaultFontFamily
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Open your UPI application and confirm the payment before the time expires",
            fontSize = 14.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            fontFamily = defaultFontFamily
        )

        Spacer(Modifier.height(24.dp))

        // UPI ID row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .border(2.dp, Color(0xFFECECED), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            // UPI icon placeholder (replace with painterResource on Android/iOS)
            Image(
                painter = painterResource(Res.drawable.ic_upi),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "UPI Id: $shopperVpa",
                fontSize = 14.sp,
                color = Color(0xFF333333),
                fontFamily = defaultFontFamily
            )
        }


        Text(
            text = "Expires in",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF444444),
            modifier = Modifier.padding(bottom = 16.dp),
            fontFamily = defaultFontFamily
        )

        // Circular countdown
        CircularCountdownTimer(
            size          = 150.dp,
            strokeWidth   = 10.dp,
            progress      = timeRemaining,                      // live value
            totalTime     = UpiTimerViewModel.TIMER_TOTAL,     // 300
            progressColor = progressColor,                      // turns red < 30s
            textColor     = textColor,
            formattedTime = formatTimer(timeRemaining * 1L) // ms → formatTimer
        )

        // Info banner
        Row(
            modifier = Modifier
                .padding(top = 32.dp)
                .fillMaxWidth()
                .background(checkoutDetails.buttonColor.toComposeColor().copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_info),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(checkoutDetails.buttonColor.toComposeColor())
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Kindly avoid using the back button until the transaction process is complete",
                fontSize = 12.sp,
                color = Color(0xFF555555),
                fontFamily = defaultFontFamily,
                lineHeight = 12.sp
            )
        }

        Spacer(Modifier.weight(1f))

        PayButton(
            text = "Cancel Payment",
            modifier = Modifier
                .fillMaxWidth()
                .background(checkoutDetails.buttonColor.toComposeColor(), RoundedCornerShape(12.dp))
                .clickable {
                    showCancelModal = true
                },
            amount = 0.0,
            currencySymbol = "",
            buttonTextColor = checkoutDetails.buttonTextColor,
            isValid = true
        )
        Footer()
    }
    if (showCancelModal) {
        CancelPaymentModal(
            onNoClick  = { showCancelModal = false },
            onYesClick = {
                showCancelModal = false
                viewModel.stopTimer()
                onBackPress()
            }
        )
    }
}

@Composable
fun CancelPaymentModal(onNoClick: () -> Unit, onYesClick: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNoClick,
        title = { Text("Cancel Payment?", fontFamily = defaultFontFamily) },
        text  = { Text("Are you sure you want to cancel this payment?", fontFamily = defaultFontFamily) },
        confirmButton = {
            TextButton(onClick = onYesClick) { Text("Yes", fontFamily = defaultFontFamily) }
        },
        dismissButton = {
            TextButton(onClick = onNoClick)  { Text("No", fontFamily = defaultFontFamily) }
        }
    )
}

@Composable
fun CircularCountdownTimer(
    size: Dp,
    strokeWidth: Dp,
    progress: Int,
    totalTime: Int,
    progressColor: Color,
    textColor: Color,
    formattedTime: String
) {
    val sweepAngle = (progress.toFloat() / totalTime.toFloat()) * 360f
    val trackColor = Color(0xFFEEEEEE)

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val diameter = this.size.minDimension - strokePx
            val topLeft  = Offset(strokePx / 2f, strokePx / 2f)
            val arcSize  = Size(diameter, diameter)

            // Track
            drawArc(
                color      = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress
            drawArc(
                color      = progressColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Text(
            text       = formattedTime,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = textColor,
            fontFamily = defaultFontFamily
        )
    }
}