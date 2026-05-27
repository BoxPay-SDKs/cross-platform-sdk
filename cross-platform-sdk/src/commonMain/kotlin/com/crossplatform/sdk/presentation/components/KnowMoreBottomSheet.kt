package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_card_add
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_card_lock
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowMoreBottomSheet(
    buttonColor: String,
    buttonTextColor : String,
    ctaBorderRadius: Int,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        dragHandle       = null,        // remove default drag handle if not needed
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp)
        ) {

            // ── Heading ────────────────────────────────────────
            Text(
                text       = "RBI Guidelines",
                fontSize   = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color(0xFF2D2B32),
                fontFamily = defaultFontFamily
            )

            // ── Subtitle ───────────────────────────────────────
            Text(
                text     = "As per the new RBI guidelines, we can no longer store your card information with us.",
                fontSize = 14.sp,
                color    = Color(0xFF2D2B32),
                modifier = Modifier.padding(top = 12.dp),
                fontFamily = defaultFontFamily
            )

            // ── Row 1 ──────────────────────────────────────────
            InfoRow(
                icon = Res.drawable.ic_card_lock,
                text = "Your bank/card network will securely save your card information via tokenization if you consent for the same."
            )

            // ── Row 2 ──────────────────────────────────────────
            InfoRow(
                icon = Res.drawable.ic_card_add,
                text = "In case you choose to not tokenize, you'll have to enter card details every time you pay."
            )

            // ── Button ─────────────────────────────────────────
            PayButton(
                text = "Got it!",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp)
                    .clip(RoundedCornerShape(ctaBorderRadius.dp))
                    .background(buttonColor.toComposeColor())
                    .clickable(onClick = onDismiss),
                amount = 0.0,
                currencySymbol = "",
                isValid = true,
                buttonTextColor = buttonTextColor
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: DrawableResource,
    text: String
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.Top
    ) {
        Image(
            painter            = painterResource(icon),
            contentDescription = null,
            modifier           = Modifier.size(28.dp)
        )
        Text(
            text     = text,
            fontSize = 14.sp,
            color    = Color(0xFF2D2B32),
            fontFamily = defaultFontFamily
        )
    }
}