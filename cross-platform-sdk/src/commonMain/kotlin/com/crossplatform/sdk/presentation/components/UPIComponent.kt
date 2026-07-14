package com.crossplatform.sdk.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.domain.model.MainScreenModel
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod
import com.crossplatform.sdk.presentation.base64ToImageBitmap
import com.crossplatform.sdk.presentation.getDeviceDetails
import com.crossplatform.sdk.presentation.getInstalledUpiApps
import com.crossplatform.sdk.presentation.getPlatformContext
import com.crossplatform.sdk.presentation.isTabletDevice
import com.crossplatform.sdk.presentation.screens.CheckboxItem
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.add_icon
import crossplatformsdk.cross_platform_sdk.generated.resources.chervon_down
import crossplatformsdk.cross_platform_sdk.generated.resources.gpay_icon
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_bharat_pe
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_bhim_upi
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_jupiter_pay
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_mobikwik_pay
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_qr
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_upi_error
import crossplatformsdk.cross_platform_sdk.generated.resources.other_intent_icon
import crossplatformsdk.cross_platform_sdk.generated.resources.paytm_icon
import crossplatformsdk.cross_platform_sdk.generated.resources.phonepe_icon
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.io.encoding.ExperimentalEncodingApi

val upiRegex = Regex("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{3,64}$")

@OptIn(ExperimentalEncodingApi::class)
@Composable
fun UPIComponent(
    methodFlags: MainScreenModel.MethodFlags,
    savedUpiList : List<SelectedPaymentMethod>,
    onClickUpiCollectPayButton : (String, Boolean) -> Unit,
    onClickUpiIntentPayButton : (String) -> Unit,
    onClickUpiQRPayButton : () -> Unit,
    onClickSavedUpiPayButton : (String, String) -> Unit,
    onClickRadio : (String) -> Unit,
    onErrorLoadingIntent : (String) -> Unit,
    buttonColor : String,
    buttonTextColor : String,
    currencySymbol : String,
    amount : Double,
    ctaBorderRadius : Int,
    focusedTextInputBorderColor : String,
    unfocusedTextInputBorderColor : String,
    shopperToken : String?,
    selectedId : String,
    qrImage : String,
    qrTimer : Int,
    stopFunctionCall : () -> Unit,
    showQROnLoad : Boolean,
    isQRLoaded : Boolean,
    isBoxPayPayButtonVisible : Boolean = true,
    onVpaChanged : (String) -> Unit,
    onClickIntent : (String) -> Unit
) {
    val isSaveInstrumentCheckBoxClicked = remember {
        mutableStateOf(false)
    }
    val context = getPlatformContext()

    val isTablet = isTabletDevice()
    var upiCollectTextInput  by remember { mutableStateOf("") }
    var upiCollectError      by remember { mutableStateOf(false) }
    var upiCollectValid      by remember { mutableStateOf(false) }
    var upiCollectVisible    by remember { mutableStateOf(false) }
    var upiQRVisible         by remember { mutableStateOf(false) }
    var selectedIntent       by remember { mutableStateOf("") }
    var isGpayInstalled      by remember { mutableStateOf(false) }
    var isPhonePeInstalled   by remember { mutableStateOf(false) }
    var isPaytmInstalled     by remember { mutableStateOf(false) }
    var isBhimUpiInstalled   by remember { mutableStateOf(false) }
    var isAmazonInstalled    by remember { mutableStateOf(false) }
    var isMobikwikInstalled  by remember { mutableStateOf(false) }
    var isJupiterInstalled   by remember { mutableStateOf(false) }
    var isPopUpiInstalled    by remember { mutableStateOf(false) }
    var isBharatPeInstalled  by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val installed    = getInstalledUpiApps(context)
        onErrorLoadingIntent(installed.toString())
        isGpayInstalled    = installed.contains("gpay")
        isPhonePeInstalled = installed.contains("phonepe")
        isPaytmInstalled   = installed.contains("paytm")
        isBhimUpiInstalled = installed.contains("bhim")
        isAmazonInstalled = installed.contains("amazon_pay")
        isMobikwikInstalled = installed.contains("mobikwik")
        isJupiterInstalled = installed.contains("jupiter")
        isPopUpiInstalled = installed.contains("pop")
        isBharatPeInstalled = installed.contains("bharatpe")
    }

    LaunchedEffect(Unit) {
        if(showQROnLoad && !isQRLoaded) {
            upiQRVisible = !upiQRVisible
            onClickUpiQRPayButton()
        }
    }

    LaunchedEffect(upiQRVisible) {
        if(!upiQRVisible) {
            stopFunctionCall()
        }
    }

    var remainingTime by remember(qrTimer) {
        mutableIntStateOf(qrTimer)
    }

    val qrIsExpired = remember {
        mutableStateOf(false)
    }

    LaunchedEffect(qrTimer) {
        qrIsExpired.value = false

        while (remainingTime > 0) {
            delay(1000)
            remainingTime--
        }

        qrIsExpired.value = true
        if(qrIsExpired.value) stopFunctionCall()
    }

    LaunchedEffect(upiQRVisible) {
        if(!upiQRVisible) {
            stopFunctionCall()
            remainingTime = 0
        }
    }

    fun handleTextChange(text: String) {
        onVpaChanged(text)
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
            .padding(bottom = 12.dp)
    ) {

        // --- Saved UPI Array ---
            if (savedUpiList.isNotEmpty()) {
                savedUpiList.forEachIndexed { _, provider ->
                    PaymentSelector(
                        id                  = provider.id,
                        title               = provider.displayName,
                        imageUrl            = provider.imageUrl,
                        isSelected          = provider.id == selectedId,
                        instrumentTypeValue = provider.instrumentType,
                        isLastUsed          = false,
                        onPress             = {
                            onClickRadio(it)
                            upiQRVisible = false
                            selectedIntent = ""
                            upiCollectVisible = false
                            upiCollectError   = false
                                              },
                        onProceedForward    = { displayValue, instrumentValue ->
                            onClickSavedUpiPayButton(instrumentValue, displayValue )
                        },
                        brandColor          = buttonColor,
                        buttonTextColor     = buttonTextColor,
                        currencySymbol      = currencySymbol,
                        amount              = amount,
                        ctaBorderRadius     = ctaBorderRadius,
                        drawableResource    = Res.drawable.ic_upi_error,
                        isBoxPayPayButtonVisible = isBoxPayPayButtonVisible
                    )
                    HorizontalDivider(
                        color     = Color(0xFFECECED),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

        // --- UPI Intent ---
        if (methodFlags.isUPIIntentVisible || methodFlags.isUPIOtmIntentVisible) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(start = 16.dp,end = 16.dp, top = 8.dp).horizontalScroll(
                    rememberScrollState()
                ),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                if (isGpayInstalled) {
                    UpiIntentItem(
                        label       = "GPay",
                        icon        = Res.drawable.gpay_icon,
                        isSelected  = selectedIntent == "GPay",
                        buttonColor = buttonColor,
                        onClick     = {
                            upiCollectVisible = false
                            upiCollectError   = false
                            selectedIntent    = "GPay"
                            onClickRadio("")
                            upiQRVisible = false
                            onClickIntent(selectedIntent)
                        }
                    )
                }
                if (isPhonePeInstalled) {
                    UpiIntentItem(
                        label       = "PhonePe",
                        icon        = Res.drawable.phonepe_icon,
                        isSelected  = selectedIntent == "PhonePe",
                        buttonColor = buttonColor,
                        onClick     = {
                            upiCollectVisible = false
                            upiCollectError   = false
                            selectedIntent    = "PhonePe"
                            onClickRadio("")
                            upiQRVisible = false
                            onClickIntent(selectedIntent)
                        }
                    )
                }
                if (isPaytmInstalled) {
                    UpiIntentItem(
                        label       = "PayTm",
                        icon        = Res.drawable.paytm_icon,
                        isSelected  = selectedIntent == "PayTm",
                        buttonColor = buttonColor,
                        onClick     = {
                            upiCollectVisible = false
                            upiCollectError   = false
                            selectedIntent    = "PayTm"
                            onClickRadio("")
                            upiQRVisible = false
                            onClickIntent(selectedIntent)
                        }
                    )
                }
                if(isBhimUpiInstalled) {
                    UpiIntentItem(
                        label       = "Bhim",
                        icon        = Res.drawable.ic_bhim_upi,
                        isSelected  = selectedIntent == "BHIM",
                        buttonColor = buttonColor,
                        onClick     = {
                            upiCollectVisible = false
                            upiCollectError   = false
                            selectedIntent    = "BHIM"
                            onClickRadio("")
                            upiQRVisible = false
                            onClickIntent(selectedIntent)
                        }
                    )
                }
//                if (isAmazonInstalled) {
//                    UpiIntentItem(
//                        label       = "AmazonPay",
//                        icon        = Res.drawable.ic_amazon_pay,
//                        isSelected  = selectedIntent == "AmazonPay",
//                        buttonColor = buttonColor,
//                        onClick     = {
//                            upiCollectVisible = false
//                            upiCollectError   = false
//                            selectedIntent    = "AmazonPay"
//                            onClickRadio("")
//                            upiQRVisible = false
//                            onClickIntent(selectedIntent)
//                        }
//                    )
//                }
                if (isMobikwikInstalled) {
                    UpiIntentItem(
                        label       = "Mobikwik",
                        icon        = Res.drawable.ic_mobikwik_pay,
                        isSelected  = selectedIntent == "Mobikwik",
                        buttonColor = buttonColor,
                        onClick     = {
                            upiCollectVisible = false
                            upiCollectError   = false
                            selectedIntent    = "Mobikwik"
                            onClickRadio("")
                            upiQRVisible = false
                            onClickIntent(selectedIntent)
                        }
                    )
                }
                if (isBharatPeInstalled) {
                    UpiIntentItem(
                        label       = "BharatPe",
                        icon        = Res.drawable.ic_bharat_pe,
                        isSelected  = selectedIntent == "BharatPe",
                        buttonColor = buttonColor,
                        onClick     = {
                            upiCollectVisible = false
                            upiCollectError   = false
                            selectedIntent    = "BharatPe"
                            onClickRadio("")
                            upiQRVisible = false
                            onClickIntent(selectedIntent)
                        }
                    )
                }
                if (isJupiterInstalled) {
                    UpiIntentItem(
                        label       = "Jupiter",
                        icon        = Res.drawable.ic_jupiter_pay,
                        isSelected  = selectedIntent == "Jupiter",
                        buttonColor = buttonColor,
                        onClick     = {
                            upiCollectVisible = false
                            upiCollectError   = false
                            selectedIntent    = "Jupiter"
                            onClickRadio("")
                            upiQRVisible = false
                            onClickIntent(selectedIntent)
                        }
                    )
                }
//                if (isPopUpiInstalled) {
//                    UpiIntentItem(
//                        label       = "Pop UPI",
//                        icon        = Res.drawable.ic_amazon_pay,
//                        isSelected  = selectedIntent == "pop",
//                        buttonColor = buttonColor,
//                        onClick     = {
//                            upiCollectVisible = false
//                            upiCollectError   = false
//                            selectedIntent    = "Pop"
//                            onClickRadio("")
//                            upiQRVisible = false
//                            onClickIntent(selectedIntent)
//                        }
//                    )
//                }
                if(!getDeviceDetails().browser.equals("ios", true)) {
                    UpiIntentItem(
                        label       = "Others",
                        icon        = Res.drawable.other_intent_icon,
                        isSelected  = false,
                        buttonColor = buttonColor,
                        onClick     = {
                            upiCollectVisible = false
                            upiCollectError   = false
                            selectedIntent    = ""
                            onClickUpiIntentPayButton(selectedIntent)
                            onClickRadio("")
                            upiQRVisible = false
                        }
                    )
                }
            }

            // --- Pay via Intent Button ---
            if (selectedIntent.isNotEmpty() && isBoxPayPayButtonVisible) {
                Spacer(modifier = Modifier.height(12.dp))
                PayButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(ctaBorderRadius.dp))
                        .background(buttonColor.toComposeColor())
                        .clickable {
                            onClickUpiIntentPayButton(selectedIntent)
                        },
                    text   = "Pay",
                    amount = amount,
                    currencySymbol = currencySymbol,
                    buttonTextColor = buttonTextColor,
                    isValid = true
                )
            }
        }

        // --- UPI Collect ---
        if (methodFlags.isUPICollectVisible || methodFlags.isUPIOtmCollectVisible) {
            if (methodFlags.isUPIIntentVisible || methodFlags.isUPIOtmIntentVisible) {
                HorizontalDivider(
                    color    = Color(0xFFE6E6E6),
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                )
            }

            UpiExpandableHeader(
                icon        = Res.drawable.add_icon,
                label       = "Add new UPI Id",
                isExpanded  = upiCollectVisible,
                buttonColor = buttonColor,
                onClick     = {
                    selectedIntent    = ""
                    upiCollectVisible = !upiCollectVisible
                    upiQRVisible      = false
                    onClickRadio("")
                    onClickIntent(selectedIntent)
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
                            fontFamily = LocalSDKFonts.current.primary,
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    textStyle = TextStyle(
                        fontFamily = LocalSDKFonts.current.primary,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        // Border
                        focusedBorderColor   = focusedTextInputBorderColor.toComposeColor(),
                        unfocusedBorderColor = unfocusedTextInputBorderColor.toComposeColor(),
                    )
                )

                if (upiCollectError) {
                    Text(
                        text       = "Please enter a valid UPI Id",
                        color      = Color.Red,
                        fontSize   = 12.sp,
                        fontFamily = LocalSDKFonts.current.primary,
                        modifier   = Modifier.padding(top = 4.dp, start = 12.dp, end = 12.dp)
                    )
                }
                if(!shopperToken.isNullOrBlank()) {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, start = 12.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheckboxItem(
                            isChecked   = isSaveInstrumentCheckBoxClicked.value,
                            buttonColor = buttonColor,
                            onClick     = { isSaveInstrumentCheckBoxClicked.value = !isSaveInstrumentCheckBoxClicked.value }
                        )
                        Text(
                            text       = "Save UPI ID for future usage",
                            fontFamily = LocalSDKFonts.current.primary,
                            fontWeight = FontWeight.Normal,
                            fontSize   = 14.sp,
                            color      = Color(0xFF2D2B32),
                            modifier   = Modifier.padding(start = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if(isBoxPayPayButtonVisible) {
                    PayButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .clip(RoundedCornerShape(ctaBorderRadius.dp))
                            .background(if (upiCollectValid) buttonColor.toComposeColor()
                            else Color(0xFFE6E6E6))
                            .clickable(enabled = upiCollectValid) {
                                onClickUpiCollectPayButton(upiCollectTextInput, isSaveInstrumentCheckBoxClicked.value)
                            },
                        text   = "Verify & Pay",
                        amount = amount,
                        currencySymbol = currencySymbol,
                        buttonTextColor = buttonTextColor,
                        isValid = upiCollectValid
                    )
                }
            }
        }

        // --- UPI QR (Tablet only) ---
        if ((methodFlags.isUPIQRVisible || methodFlags.isUPIOtmQRVisible) && isTablet) {
            if (methodFlags.isUPIIntentVisible || methodFlags.isUPIOtmIntentVisible ||
                methodFlags.isUPICollectVisible || methodFlags.isUPIOtmCollectVisible
            ) {
                HorizontalDivider(
                    color    = Color(0xFFE6E6E6),
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                )
            }

            UpiExpandableHeader(
                icon        = Res.drawable.ic_qr,
                label       = "Pay Using QR",
                isExpanded  = upiQRVisible,
                buttonColor = buttonColor,
                onClick     = {
                    upiQRVisible = !upiQRVisible
                    onClickUpiQRPayButton()
                }
            )
        }

        // --- QR Image ---
            if (upiQRVisible && qrImage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.Center) {

                        Image(
                            bitmap = base64ToImageBitmap(qrImage),
                            contentDescription = "Payment QR",
                            modifier = Modifier
                                .size(250.dp)
                                .blur(if (qrIsExpired.value) 12.dp else 0.dp)
                        )
                        if (qrIsExpired.value) {
                            Text(
                                text     = "↻ Retry",
                                color    = buttonColor.toComposeColor(),
                                fontFamily = LocalSDKFonts.current.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { onClickUpiQRPayButton() }.background(Color.White, RoundedCornerShape(12.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text       = "Scan & Pay with UPI Application",
                            fontFamily = LocalSDKFonts.current.primary,
                            fontWeight = FontWeight.Normal,
                            fontSize   = 14.sp
                        )
                        Text(
                            text       = "QR code will expire in",
                            fontFamily = LocalSDKFonts.current.primary,
                            fontWeight = FontWeight.Normal,
                            fontSize   = 14.sp
                        )
                        Text(
                            text       = formatTime(remainingTime),
                            color      = buttonColor.toComposeColor(),
                            fontFamily = LocalSDKFonts.current.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 18.sp
                        )
                    }
                }
            }
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
                    color = if (isSelected) buttonColor.toComposeColor() else Color(0xFFFFFFFF),
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
            fontFamily = LocalSDKFonts.current.primary,
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
            .padding(horizontal = 16.dp),
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
            fontFamily = LocalSDKFonts.current.primary,
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

fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return minutes.toString().padStart(2, '0') +
            ":" +
            seconds.toString().padStart(2, '0')
}