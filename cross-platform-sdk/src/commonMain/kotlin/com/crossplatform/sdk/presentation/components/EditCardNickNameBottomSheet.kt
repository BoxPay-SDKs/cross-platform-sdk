package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.presentation.theme.LocalSDKColors
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.cancel_cta
import crossplatformsdk.cross_platform_sdk.generated.resources.card_nick_name_title
import crossplatformsdk.cross_platform_sdk.generated.resources.edit_card_nickname_title
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_card
import crossplatformsdk.cross_platform_sdk.generated.resources.save_cta
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val NICKNAME_MAX_LENGTH = 30

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditCardNicknameBottomSheet(
    imageUrl : String,
    cardDisplayValue: String,
    initialNickName: String,
    buttonColor: String,
    buttonTextColor: String,
    ctaBorderRadius: Int,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nickName by remember { mutableStateOf(initialNickName) }

    val trimmed = nickName.trim()
    val canSave = trimmed.isNotEmpty() && trimmed != initialNickName.trim()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            KamelImage(
                resource              = asyncPainterResource(data = imageUrl),
                contentDescription = "Saved Card",
                modifier           = Modifier.size(32.dp),
                onLoading = {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = LocalSDKColors.current.divider,
                                RoundedCornerShape(12.dp)
                            )
                    )
                },
                onFailure = {
                    Image(
                        painter = painterResource(Res.drawable.ic_card),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = stringResource(Res.string.edit_card_nickname_title),
                    fontFamily = LocalSDKFonts.current.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalSDKColors.current.textSecondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Card ending **** ${cardDisplayValue.takeLast(4)}",
                    fontFamily = LocalSDKFonts.current.primary,
                    fontSize = 12.sp,
                    color = LocalSDKColors.current.textSecondary
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        OutlinedTextField(
            value = nickName,
            onValueChange = { if (it.length <= NICKNAME_MAX_LENGTH) nickName = it },
            label = {
                Text(
                    stringResource(Res.string.card_nick_name_title),
                    fontFamily = LocalSDKFonts.current.primary
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = buttonColor.toComposeColor(),
                focusedLabelColor = buttonColor.toComposeColor(),
                cursorColor = buttonColor.toComposeColor()
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        PayButton(
            text = stringResource(Res.string.save_cta),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ctaBorderRadius.dp))
                .background(
                    if (canSave) buttonColor.toComposeColor()
                    else LocalSDKColors.current.divider
                )
                .clickable(enabled = canSave) {
                    onSave(trimmed)
                },
            amount = 0.0,
            currencySymbol = "",
            buttonTextColor = buttonTextColor,
            isValid = canSave
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { onDismiss() },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(ctaBorderRadius.dp)),
            shape = RoundedCornerShape(ctaBorderRadius.dp),
            border = BorderStroke(
                width = 1.dp,
                color = buttonColor.toComposeColor()
            )
        ) {
            Text(
                stringResource(Res.string.cancel_cta),
                fontFamily = LocalSDKFonts.current.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color      = buttonColor.toComposeColor(),
                modifier   = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}