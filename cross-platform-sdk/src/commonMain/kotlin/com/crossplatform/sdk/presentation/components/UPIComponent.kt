package com.crossplatform.sdk.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.data.model.CheckoutDetails
import com.crossplatform.sdk.domain.model.MainScreenModel
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod
import com.crossplatform.sdk.presentation.ChevronIcon
import com.crossplatform.sdk.presentation.getInstalledUpiApps
import com.crossplatform.sdk.presentation.getPlatformContext
import com.crossplatform.sdk.presentation.screens.CheckboxItem
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.add_icon
import crossplatformsdk.cross_platform_sdk.generated.resources.chervon_down
import crossplatformsdk.cross_platform_sdk.generated.resources.gpay_icon
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_qr
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_upi
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_upi_error
import crossplatformsdk.cross_platform_sdk.generated.resources.other_intent_icon
import crossplatformsdk.cross_platform_sdk.generated.resources.paytm_icon
import crossplatformsdk.cross_platform_sdk.generated.resources.phonepe_icon
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
@Composable
fun UPIComponent(
    methodFlags: MainScreenModel.MethodFlags,
    savedUpiList : List<SelectedPaymentMethod>,
    checkoutDetails: CheckoutDetails,
    onClickUpiCollectPayButton : (String, Boolean) -> Unit,
    onClickUpiIntentPayButton : (String) -> Unit,
    onClickUpiQRPayButton : () -> Unit,
    onClickSavedUpiPayButton : (String, String) -> Unit,
    onClickRadio : () -> Unit,
    onErrorLoadingIntent : (String) -> Unit
) {
    val isSaveInstrumentCheckBoxClicked = remember {
        mutableStateOf(false)
    }

    val selectedId = remember {
        mutableStateOf("")
    }
    val context = getPlatformContext()

    val isTablet = false
    var upiCollectTextInput  by remember { mutableStateOf("") }
    var upiCollectError      by remember { mutableStateOf(false) }
    var upiCollectValid      by remember { mutableStateOf(false) }
    var upiCollectVisible    by remember { mutableStateOf(false) }
    var upiQRVisible         by remember { mutableStateOf(false) }
    var selectedIntent       by remember { mutableStateOf("") }
    var isGpayInstalled      by remember { mutableStateOf(false) }
    var isPhonePeInstalled   by remember { mutableStateOf(false) }
    var isPaytmInstalled     by remember { mutableStateOf(false) }

    val upiRegex = remember { Regex("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{3,64}$") }


    LaunchedEffect(Unit) {
        val installed    = getInstalledUpiApps(context)
        onErrorLoadingIntent(installed.toString())
        isGpayInstalled    = installed.contains("gpay")
        isPhonePeInstalled = installed.contains("phonepe")
        isPaytmInstalled   = installed.contains("paytm")
    }


    fun handleTextChange(text: String) {
        upiCollectTextInput = text
        upiCollectError     = false
        if (text.trim().isNotEmpty() && upiRegex.matches(text)) {
            upiCollectValid = true
            upiCollectError = false
        } else {
            if (text.contains("@") && (text.split("@").getOrNull(1)?.length ?: 0) >= 2) {
                upiCollectError = true
                upiCollectValid = false
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE6E6E6), RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(12.dp)
    ) {

        // --- Saved UPI Array ---
            if (savedUpiList.isNotEmpty()) {
                savedUpiList.forEachIndexed { _, provider ->
                    PaymentSelector(
                        id                  = provider.id,
                        title               = provider.displayName,
                        imageUrl            = provider.imageUrl,
                        isSelected          = provider.id == selectedId.value,
                        instrumentTypeValue = provider.instrumentType,
                        isLastUsed          = false,
                        onPress             = {
                            onClickRadio()
                            selectedId.value = it
                                              },
                        onProceedForward    = { displayValue, instrumentValue ->
                            onClickSavedUpiPayButton(instrumentValue, displayValue )
                        },
                        brandColor          = checkoutDetails.buttonColor,
                        buttonTextColor     = checkoutDetails.buttonTextColor,
                        currencySymbol      = checkoutDetails.currencySymbol,
                        amount              = checkoutDetails.amount,
                        ctaBorderRadius     = checkoutDetails.ctaBorderRadius,
                        drawableResource    = Res.drawable.ic_upi_error
                    )
                    HorizontalDivider(
                        color     = Color(0xFFECECED),
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

        // --- UPI Intent ---
        if (methodFlags.isUPIIntentVisible || methodFlags.isUPIOtmIntentVisible) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isGpayInstalled) {
                    UpiIntentItem(
                        label       = "GPay",
                        icon        = Res.drawable.gpay_icon,
                        isSelected  = selectedIntent == "GPay",
                        buttonColor = checkoutDetails.buttonColor,
                        onClick     = {
                            upiCollectVisible = false
                            upiCollectError   = false
                            selectedIntent    = "GPay"
//                                resetSavedUpi()
                        }
                    )
                }
                if (isPhonePeInstalled) {
                    UpiIntentItem(
                        label       = "PhonePe",
                        icon        = Res.drawable.phonepe_icon,
                        isSelected  = selectedIntent == "PhonePe",
                        buttonColor = checkoutDetails.buttonColor,
                        onClick     = {
                            upiCollectVisible = false
                            upiCollectError   = false
                            selectedIntent    = "PhonePe"
//                                resetSavedUpi()
                        }
                    )
                }
                if (isPaytmInstalled) {
                    UpiIntentItem(
                        label       = "PayTm",
                        icon        = Res.drawable.paytm_icon,
                        isSelected  = selectedIntent == "PayTm",
                        buttonColor = checkoutDetails.buttonColor,
                        onClick     = {
                            upiCollectVisible = false
                            upiCollectError   = false
                            selectedIntent    = "PayTm"
//                                resetSavedUpi()
                        }
                    )
                }
                UpiIntentItem(
                    label       = "Others",
                    icon        = Res.drawable.other_intent_icon,
                    isSelected  = false,
                    buttonColor = checkoutDetails.buttonColor,
                    onClick     = {
                        upiCollectVisible = false
                        upiCollectError   = false
                        selectedIntent    = "Other"
//                            resetSavedUpi()
//                            onHandleUpiPayment("")
                    }
                )
            }

            // --- Pay via Intent Button ---
            if (selectedIntent.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                PayButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(checkoutDetails.ctaBorderRadius.dp))
                        .background(checkoutDetails.buttonColor.toComposeColor())
                        .clickable {
                            onClickUpiIntentPayButton(selectedIntent)
                        },
                    text   = "Pay",
                    amount = checkoutDetails.amount,
                    currencySymbol = checkoutDetails.currencySymbol,
                    buttonTextColor = checkoutDetails.buttonTextColor,
                    isValid = true
                )
            }
        }

        // --- UPI Collect ---
        if (methodFlags.isUPICollectVisible || methodFlags.isUPIOtmCollectVisible) {
            if (methodFlags.isUPIIntentVisible || methodFlags.isUPIOtmIntentVisible) {
                HorizontalDivider(
                    color    = Color(0xFFE6E6E6),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            UpiExpandableHeader(
                icon        = Res.drawable.add_icon,
                label       = "Add new UPI Id",
                isExpanded  = upiCollectVisible,
                buttonColor = checkoutDetails.buttonColor,
                onClick     = {
                    selectedIntent    = ""
                    upiCollectVisible = !upiCollectVisible
                    upiQRVisible      = false
//                        resetSavedUpi()
                }
            )

            // --- Collect Input ---
            if (upiCollectVisible) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value         = upiCollectTextInput,
                    onValueChange = { handleTextChange(it) },
                    label         = {
                        Text(
                            text       = "Enter UPI Id",
                            fontFamily = defaultFontFamily,
                            fontWeight = FontWeight.Normal
                        )
                    },
                    isError      = upiCollectError,
                    trailingIcon = {
                        if (upiCollectError) {
                            Image(
                                painter            = painterResource(Res.drawable.ic_upi_error),
                                contentDescription = null,
                                modifier           = Modifier.size(20.dp)
                            )
                        }
                    },
                    shape    = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontFamily = defaultFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        // Border
                        focusedBorderColor   = checkoutDetails.focusedTextInputBorderColor.toComposeColor(),
                        unfocusedBorderColor = checkoutDetails.unfocusedTextInputBorderColor.toComposeColor(),
                    )
                )

                if (upiCollectError) {
                    Text(
                        text       = "Please enter a valid UPI Id",
                        color      = Color.Red,
                        fontSize   = 12.sp,
                        fontFamily = defaultFontFamily,
                        modifier   = Modifier.padding(top = 4.dp)
                    )
                }
                if(!checkoutDetails.shopperToken.isNullOrBlank()) {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheckboxItem(
                            isChecked   = isSaveInstrumentCheckBoxClicked.value,
                            buttonColor = checkoutDetails.buttonColor,
                            onClick     = { isSaveInstrumentCheckBoxClicked.value = !isSaveInstrumentCheckBoxClicked.value }
                        )
                        Text(
                            text       = "Save UPI ID for future usage",
                            fontFamily = defaultFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize   = 14.sp,
                            color      = Color(0xFF2D2B32),
                            modifier   = Modifier.padding(start = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                PayButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(checkoutDetails.ctaBorderRadius.dp))
                        .background(if (upiCollectValid) checkoutDetails.buttonColor.toComposeColor()
                        else Color(0xFFE6E6E6))
                        .clickable(enabled = upiCollectValid) {
                            onClickUpiCollectPayButton(upiCollectTextInput, isSaveInstrumentCheckBoxClicked.value)
                        },
                    text   = "Verify & Pay",
                    amount = checkoutDetails.amount,
                    currencySymbol = checkoutDetails.currencySymbol,
                    buttonTextColor = checkoutDetails.buttonTextColor,
                    isValid = upiCollectValid
                )
            }
        }

        // --- UPI QR (Tablet only) ---
        if ((methodFlags.isUPIQRVisible || methodFlags.isUPIOtmQRVisible) && isTablet) {
            if (methodFlags.isUPIIntentVisible || methodFlags.isUPIOtmIntentVisible ||
                methodFlags.isUPICollectVisible || methodFlags.isUPIOtmCollectVisible
            ) {
                HorizontalDivider(
                    color    = Color(0xFFE6E6E6),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            UpiExpandableHeader(
                icon        = Res.drawable.ic_qr,
                label       = "Pay Using QR",
                isExpanded  = upiQRVisible,
                buttonColor = checkoutDetails.buttonColor,
                onClick     = {
//                        if (upiQRVisible) upiQRVisible = false
//                        else onQRChevronClick()
                }
            )
        }

        // --- QR Image ---
//            if (upiQRVisible && qrImage.isNotEmpty()) {
//                Spacer(modifier = Modifier.height(12.dp))
//                Row(
//                    modifier          = Modifier.fillMaxWidth(),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Box(contentAlignment = Alignment.Center) {
//                        val imageBitmap = remember(key1=qrImage) {
//                            val bytes = Base64.decode(qrImage, Base64.DEFAULT)
//                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
//                        }
//                        Image(
//                            bitmap             = imageBitmap,
//                            contentDescription = "QR Code",
//                            modifier           = Modifier
//                                .size(150.dp)
//                                .alpha(if (qrIsExpired) 0.2f else 1f)
//                        )
//                        if (qrIsExpired) {
//                            Text(
//                                text     = "↻ Retry",
//                                color    = checkoutDetails.buttonColor.toComposeColor(),
//                                fontFamily = defaultFontFamily,
//                                fontWeight = FontWeight.SemiBold,
//                                modifier = Modifier.clickable { onRetryQR() }
//                            )
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.width(16.dp))
//
//                    Column {
//                        Text(
//                            text       = "Scan & Pay with UPI Application",
//                            fontFamily = defaultFontFamily,
//                            fontWeight = FontWeight.Normal,
//                            fontSize   = 14.sp
//                        )
//                        Text(
//                            text       = "QR code will expire in",
//                            fontFamily = defaultFontFamily,
//                            fontWeight = FontWeight.Normal,
//                            fontSize   = 14.sp
//                        )
//                        Text(
//                            text       = formatTime(timeRemaining),
//                            color      = checkoutDetails.buttonColor.toComposeColor(),
//                            fontFamily = defaultFontFamily,
//                            fontWeight = FontWeight.SemiBold,
//                            fontSize   = 18.sp
//                        )
//                    }
//                }
//            }
    }
}

@Composable
private fun UpiIntentItem(
    label      : String,
    icon       : DrawableResource,
    isSelected : Boolean,
    buttonColor: String,
    onClick    : () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) buttonColor.toComposeColor() else Color(0xFFE6E6E6),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter            = painterResource(icon),
                contentDescription = label,
                modifier           = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text       = label,
            fontSize   = 12.sp,
            fontFamily = defaultFontFamily,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color      = if (isSelected) buttonColor.toComposeColor() else Color.Black
        )
    }
}

// --- Reusable: Expandable Header ---
@Composable
private fun UpiExpandableHeader(
    icon       : DrawableResource,
    label      : String,
    isExpanded : Boolean,
    buttonColor: String,
    onClick    : () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label       = "chevron"
    )

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter            = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(buttonColor.toComposeColor()),
            modifier           = Modifier
                .size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text       = label,
            color      = buttonColor.toComposeColor(),
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 14.sp,
            modifier   = Modifier.weight(1f)
        )
        Image(
            painter            = painterResource(Res.drawable.chervon_down),
            contentDescription = null,
            modifier           = Modifier
                .size(width = 20.dp, height = 30.dp)
                .rotate(rotation)
        )
    }
}
