package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_exit_door
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ExitCheckoutConfirmation(
    onConfirmExit: () -> Unit,
    onStay: () -> Unit,
    buttonColor: String,
    buttonTextColor : String,
    ctaBorderRadius : Int,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val scope = rememberCoroutineScope()

    fun dismiss(after: () -> Unit) {
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) after()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onStay,
        sheetState = sheetState,
        dragHandle       = null,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_exit_door),
                contentDescription = null,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Text(
                text = "Exit checkout?",
                fontFamily = LocalSDKFonts.current.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4F4D55),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your payment hasn't been completed yet. If you exit now, your progress will be lost.",
                fontFamily = LocalSDKFonts.current.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { dismiss(onConfirmExit) },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(ctaBorderRadius.dp)),
                shape = RoundedCornerShape(ctaBorderRadius.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = buttonColor.toComposeColor()
                )
            ) {
                Text(
                    "Exit checkout",
                    fontFamily = LocalSDKFonts.current.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color      = buttonColor.toComposeColor(),
                    modifier   = Modifier.padding(vertical = 6.dp)
                )
            }

            PayButton(
                text = "Back to Checkout",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(ctaBorderRadius.dp))
                    .background(buttonColor.toComposeColor())
                    .clickable { onStay() },
                amount = 0.0,
                currencySymbol = "",
                buttonTextColor = buttonTextColor,
                isValid = true
            )
        }
    }
}