package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun SavedAddressCard(
    modifier: Modifier = Modifier,
    address1: String?,
    address2: String?,
    city: String?,
    state: String?,
    pinCode: String?,
    number: String?,
    isCurrentlySelected: Boolean,
    addressIcon: DrawableResource,
    label: String,
    onClickEditAddress: () -> Unit,
    onClickSelectAddress: () -> Unit,
    selectedCtaColor: String,
    editAddressIcon: DrawableResource,
) {
    val borderColor = if (isCurrentlySelected) selectedCtaColor.toComposeColor() else Color.White
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.White, shape)
            .border(1.dp, borderColor, shape)
            .clickable { onClickSelectAddress() }
            .padding(horizontal = 12.dp)
            .padding(top = 14.dp, bottom = 12.dp)
    ) {
        // ── Row 1: icon · label · "CURRENTLY SELECTED" tag · edit icon ────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Address type icon (home / work etc.)
            Image(
                painter = painterResource(addressIcon),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(2.dp))

            // Label
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = LocalSDKFonts.current.primary
                ),
                color = Color(0xFF2D2B32),
                modifier = Modifier.weight(1f)
            )

            // "CURRENTLY SELECTED" tag — only when selected
            if (isCurrentlySelected) {
                Spacer(Modifier.width(2.dp))
                FilterTag(text = "CURRENTLY SELECTED")
            }

            // Edit icon
            Spacer(Modifier.width(8.dp))
            Image(
                painter = painterResource(editAddressIcon),
                contentDescription = "Edit address",
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onClickEditAddress() }
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Row 2: full address line ─────────────────────────────────────
        val addressText = buildAnnotatedString {
            val addressSpan = SpanStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF7F7D83),
                 fontFamily = LocalSDKFonts.current.primary
            )
            if (!address1.isNullOrEmpty()) append(AnnotatedString("$address1, ", addressSpan))
            if (!address2.isNullOrEmpty()) append(AnnotatedString("$address2, ", addressSpan))
            if (!city.isNullOrEmpty())     append(AnnotatedString("$city, ",     addressSpan))
            if (!state.isNullOrEmpty())    append(AnnotatedString("$state, ",    addressSpan))
            if (!pinCode.isNullOrEmpty())  append(AnnotatedString(pinCode,       addressSpan))
        }
        if (addressText.isNotEmpty()) {
            Text(text = addressText, fontFamily = LocalSDKFonts.current.primary)
        }

        Spacer(Modifier.height(2.dp))

        // ── Row 3: mobile number ─────────────────────────────────────────
        Text(
            text = "Mobile: $number",
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                 fontFamily = LocalSDKFonts.current.primary
            ),
            color = Color(0xFF7F7D83),
            modifier = Modifier.fillMaxWidth()
        )
    }
}