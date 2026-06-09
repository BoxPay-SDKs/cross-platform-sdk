package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.presentation.ChevronIcon
import com.crossplatform.sdk.presentation.SectionTitle
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_location
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_user
import org.jetbrains.compose.resources.painterResource

@Composable
fun AddressComponent(
    address: String,
    navigateToAddressScreen: () -> Unit,
    isShippingAddressEnabled: Boolean,
    isFullNameEnabled : Boolean,
    isPhoneEnabled : Boolean,
    isEmailEnabled : Boolean,
    isShippingAddressEditable : Boolean,
    isFullNameEditable : Boolean,
    isEmailEditable : Boolean,
    isPhoneEditable : Boolean,
    firstName  :String?,
    lastName : String?,
    email : String?,
    completePhoneNumber : String?,
    labelType : String?,
    labelName : String?
) {

    val showPersonalDetails = (isFullNameEnabled ||
            isPhoneEnabled ||
            isEmailEnabled) &&
            !isShippingAddressEnabled
    Column {

        // --- Case 1: Address exists ---
        if (address.isNotEmpty() && isShippingAddressEnabled) {
            SectionTitle("Address")

            AddressCard(
                onClick = {
                    if (isShippingAddressEditable) navigateToAddressScreen()
                }
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_location),
                    contentDescription = null,
                    modifier          = Modifier.size(32.dp)
                )

                Column(modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("Deliver at ")
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                                append(
                                    if (labelType == "Other") labelName ?: ""
                                    else labelType ?: ""
                                )
                            }
                        },
                        fontFamily = defaultFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize   = 14.sp
                    )
                    Text(
                        text       = address,
                        fontFamily = defaultFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                }

                if (isShippingAddressEditable) {
                    ChevronIcon()
                }
            }
        }
        else if (showPersonalDetails) {
            SectionTitle("Personal Details")

            val isEditable = isFullNameEditable ||
                    isPhoneEditable ||
                    isEmailEditable

            AddressCard(
                onClick = { if (isEditable) navigateToAddressScreen() }
            ) {
                // ← check only enabled fields for hasData
                val hasData =
                    (isFullNameEnabled  && !firstName.isNullOrEmpty()) ||
                            (isPhoneEnabled     && !completePhoneNumber.isNullOrEmpty()) ||
                            (isEmailEnabled     && !email.isNullOrEmpty())

                if (hasData) {
                    Image(
                        painter            = painterResource(Res.drawable.ic_user),
                        contentDescription = null,
                        modifier           = Modifier.size(32.dp)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        // show name + phone only if enabled
                        if (isFullNameEnabled || isPhoneEnabled) {
                            Text(
                                text = buildString {
                                    if (isFullNameEnabled && !firstName.isNullOrEmpty()) {
                                        append("$firstName ${lastName ?: ""}")
                                    }
                                    if (isFullNameEnabled && isPhoneEnabled &&
                                        !firstName.isNullOrEmpty() && !completePhoneNumber.isNullOrEmpty()
                                    ) {
                                        append(" | ")
                                    }
                                    if (isPhoneEnabled && !completePhoneNumber.isNullOrEmpty()) {
                                        append(completePhoneNumber)
                                    }
                                },
                                fontFamily = defaultFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 14.sp
                            )
                        }
                        // show email only if enabled
                        if (isEmailEnabled && !email.isNullOrEmpty()) {
                            Text(
                                text       = email,
                                fontFamily = defaultFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 14.sp,
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (isEditable) {
                    ChevronIcon()
                }
            }
        }
    }
}

@Composable
private fun AddressCard(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = Color(0xFFE6E6E6),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}