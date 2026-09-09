package com.crossplatform.sdk.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.presentation.getDeviceDetails
import com.crossplatform.sdk.presentation.rememberOtpAutoReader
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_amex_safekey
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_card
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_diners
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_maestro
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_mastercard_securecode
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_rupay
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_verified_by_visa
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.painterResource

// Reserves room at the bottom of the scrollable content so the resend row
// isn't hidden underneath the pinned footer overlay.
private val FOOTER_RESERVED_HEIGHT = 40.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NativeOTPBottomSheet(
    sheetState: SheetState,
    onClickProceed : (otp : String) -> Unit,
    onClickUrl : () -> Unit,
    onClickCancel : () -> Unit,
    onClickResend : () -> Unit,
    minOtpField: Int,
    maxOtpField: Int,
    cardBrand: String,
    isOtpInvalid: Boolean,
    isLoading : Boolean
) {
    val buttonTextColor = CheckoutDetailsHandler.buttonTextColorFlow.collectAsStateWithLifecycle()
    val buttonColor = CheckoutDetailsHandler.buttonColorFlow.collectAsStateWithLifecycle()
    val ctaBorderRadius = CheckoutDetailsHandler.ctaBorderRadiusFlow.collectAsStateWithLifecycle()
    val currencyFlow = CheckoutDetailsHandler.currencyFlow.collectAsStateWithLifecycle()
    val (_, currencyCode) = currencyFlow.value
    val amount = CheckoutDetailsHandler.amountFlow.collectAsStateWithLifecycle()

    val accentColor = buttonColor.value.toComposeColor()
    var contentHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    val cardSelectedIcon = when(cardBrand.lowercase()) {
        "visa" -> Res.drawable.ic_verified_by_visa
        "mastercard" -> Res.drawable.ic_mastercard_securecode
        "americanexpress" -> Res.drawable.ic_amex_safekey
        "maestro" -> Res.drawable.ic_maestro
        "rupay" -> Res.drawable.ic_rupay
        "diners" -> Res.drawable.ic_diners
        else -> Res.drawable.ic_card
    }

    ModalBottomSheet(
        onDismissRequest = {
            // no operation
        },
        sheetState = sheetState
    ) {
        if (isLoading) {
            ShowLoadingComponent(Modifier.fillMaxWidth()
                .then(
                    if (contentHeightPx > 0)
                        Modifier.height(with(density) { contentHeightPx.toDp() })
                    else
                        Modifier.wrapContentSize() // fallback for the very first load, before we've measured anything
                ))
        } else {
            val keyboardController = LocalSoftwareKeyboardController.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 10.dp)
                    .imePadding()
                    .onGloballyPositioned { coordinates ->
                        contentHeightPx = coordinates.size.height
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ---- Header bar: expiry chip on the left, brand mark on the right ----
                // The ticking countdown lives entirely inside PageExpiryTimer so its
                // per-second state changes don't force this outer Column to recompose.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PageExpiryTimer(
                        accentColor = accentColor,
                        onExpire = onClickCancel
                    )
                    Image(
                        painter = painterResource(cardSelectedIcon),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Enter OTP",
                    fontFamily = LocalSDKFonts.current.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color(0xFF0A090B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Enter the one-time password sent to your registered mobile number",
                    textAlign = TextAlign.Center,
                    color = Color(0xFF6B6D76),
                    fontFamily = LocalSDKFonts.current.primary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ---- OTP boxes ----
                val otpDigits = remember { mutableStateListOf(*Array(maxOtpField) { "" }) }
                val focusRequesters = remember { List(maxOtpField) { FocusRequester() } }

                var showOtpError by remember { mutableStateOf(false) }

                LaunchedEffect(isOtpInvalid) {
                    if (isOtpInvalid) showOtpError = true
                }

                // expectedLength = maxOtpField: gates auto-read so a partial,
                // still-in-progress autofill (e.g. 4 of 6 digits typed in so far
                // by iOS) never gets accepted as the complete code. See
                // OtpAutoReader.kt (iOS) for why that specifically matters there.
                val otpAutoReader = rememberOtpAutoReader(expectedLength = maxOtpField) { code ->
                    code.take(maxOtpField).forEachIndexed { i, char ->
                        otpDigits[i] = char.toString()
                    }
                    showOtpError = false
                    if (code.length >= minOtpField) {
                        onClickProceed(code)
                    }
                }

                DisposableEffect(Unit) {
                    otpAutoReader.start()
                    onDispose { otpAutoReader.stop() }
                }

                OtpBoxRow(
                    digits = otpDigits,
                    focusRequesters = focusRequesters,
                    boxCount = maxOtpField,
                    accentColor = accentColor,
                    isError = showOtpError,
                    onDigitEdited = { showOtpError = false },
                    onDone = { keyboardController?.hide() }
                )

                if (showOtpError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Invalid OTP entered. Please try again.",
                        fontFamily = LocalSDKFonts.current.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD92D20)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                val enteredOtp = otpDigits.joinToString("")

                val isProceedEnabled = enteredOtp.length >= minOtpField &&
                        otpDigits.take(enteredOtp.length).none { it.isEmpty() }

                PayButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                        .shadow(
                            elevation = if (isProceedEnabled) 6.dp else 0.dp,
                            shape = RoundedCornerShape(ctaBorderRadius.value.dp),
                            clip = false
                        )
                        .clip(RoundedCornerShape(ctaBorderRadius.value.dp))
                        .background(
                            if (isProceedEnabled) accentColor
                            else Color(0xFFE6E6E6)
                        )
                        .clickable(enabled = isProceedEnabled) {
                            onClickProceed(enteredOtp)
                        },
                    amount = amount.value,
                    currencySymbol = currencyCode,
                    isValid = isProceedEnabled,
                    buttonTextColor = buttonTextColor.value,
                    text = "Pay"
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ---- Resend OTP (max 3 uses, 1 min cooldown each) ----
                var resendCount by remember { mutableStateOf(0) }
                var cooldownSecondsLeft by remember { mutableStateOf(0) }

                LaunchedEffect(cooldownSecondsLeft > 0, resendCount) {
                    while (isActive && cooldownSecondsLeft > 0) {
                        delay(1_000)
                        cooldownSecondsLeft -= 1
                    }
                }

                val resendExhausted = resendCount >= MAX_RESEND_ATTEMPTS
                val resendEnabled = !resendExhausted && cooldownSecondsLeft <= 0

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Didn't receive the code?",
                        fontFamily = LocalSDKFonts.current.primary,
                        fontSize = 14.sp,
                        color = Color(0xFF6B6D76)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    TextButton(
                        onClick = {
                            resendCount += 1
                            cooldownSecondsLeft = RESEND_COOLDOWN_SECONDS
                            if (resendEnabled) onClickResend()
                        },
                        enabled = resendEnabled
                    ) {
                        Text(
                            text = when {
                                resendExhausted -> "No more resends left"
                                cooldownSecondsLeft > 0 -> "Resend in ${
                                    formatTime(
                                        cooldownSecondsLeft
                                    )
                                }"

                                else -> "Resend OTP"
                            },
                            fontFamily = LocalSDKFonts.current.primary,
                            fontSize = 14.sp,
                            fontWeight = if (resendEnabled) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (resendEnabled) accentColor else Color(0xFF9A9CA5)
                        )
                    }
                }

                // Reserves space so the resend row above never sits behind the
                // pinned footer (Cancel / Continue on Bank page) overlay below.
                Spacer(modifier = Modifier.height(FOOTER_RESERVED_HEIGHT))

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onClickCancel) {
                        Text(
                            text = "Cancel",
                            fontFamily = LocalSDKFonts.current.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6B6D76)
                        )
                    }
                    TextButton(onClick = onClickUrl) {
                        Text(
                            text = "Continue on Bank page",
                            fontFamily = LocalSDKFonts.current.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline,
                            color = accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun KeyboardDoneBar(
    accentColor: Color,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imeVisible = getDeviceDetails().browser.equals("ios", true) && WindowInsets.ime.getBottom(LocalDensity.current) > 0

    AnimatedVisibility(
        visible = imeVisible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF2F2F5))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDone) {
                Text(
                    text = "Done",
                    fontFamily = LocalSDKFonts.current.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = accentColor
                )
            }
        }
    }
}
@Composable
private fun PageExpiryTimer(accentColor: Color, onExpire: () -> Unit) {
    var secondsLeft by remember { mutableStateOf(PAGE_TIMEOUT_SECONDS) }

    LaunchedEffect(Unit) {
        while (isActive && secondsLeft > 0) {
            delay(1_000)
            secondsLeft -= 1
        }
        if (secondsLeft <= 0) {
            onExpire()
        }
    }

    TimerChip(
        text = "This page will expire in ${formatTime(secondsLeft)}",
        accentColor = accentColor
    )
}

@Composable
private fun TimerChip(text: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(accentColor.copy(alpha = 0.1f))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontFamily = LocalSDKFonts.current.primary,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = accentColor
        )
    }
}

@Composable
private fun OtpBoxRow(
    digits: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    focusRequesters: List<FocusRequester>,
    boxCount: Int,
    accentColor: Color,
    isError: Boolean,
    onDigitEdited: () -> Unit,
    onDone: () -> Unit
) {
    val errorColor = Color(0xFFD92D20)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        for (index in 0 until boxCount) {
            val hasDigit = digits[index].isNotEmpty()
            val isLastBox = index == boxCount - 1

            OutlinedTextField(
                value = digits[index],
                onValueChange = { newValue ->
                    val digit = newValue.takeLast(1).filter { it.isDigit() }
                    digits[index] = digit
                    onDigitEdited()
                    when {
                        digit.isNotEmpty() && index < boxCount - 1 ->
                            focusRequesters[index + 1].requestFocus()
                        digit.isEmpty() && index > 0 && newValue.isEmpty() ->
                            focusRequesters[index - 1].requestFocus()
                    }
                },
                // weight(1f) instead of a fixed size: divides available row width
                // evenly across however many boxes there are, so it stays correct
                // on narrow phones and for any maxOtpField count. Height stays
                // fixed since that doesn't need to flex with screen width.
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .focusRequester(focusRequesters[index]),
                singleLine = true,
                isError = isError,
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontFamily = LocalSDKFonts.current.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = Color(0xFF0A090B)
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isError) errorColor else accentColor,
                    unfocusedBorderColor = when {
                        isError -> errorColor
                        hasDigit -> accentColor.copy(alpha = 0.5f)
                        else -> Color(0xFFE1E1E6)
                    },
                    errorBorderColor = errorColor,
                    focusedContainerColor = if (isError) errorColor.copy(alpha = 0.06f) else accentColor.copy(alpha = 0.06f),
                    unfocusedContainerColor = when {
                        isError -> errorColor.copy(alpha = 0.06f)
                        hasDigit -> accentColor.copy(alpha = 0.06f)
                        else -> Color(0xFFFAFAFC)
                    },
                    errorContainerColor = errorColor.copy(alpha = 0.06f),
                    cursorColor = if (isError) errorColor else accentColor
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = if (isLastBox) ImeAction.Done else ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusRequesters.getOrNull(index + 1)?.requestFocus() },
                    onDone = { onDone() }
                )
            )
        }
    }
}

private const val PAGE_TIMEOUT_SECONDS = 3 * 60
private const val RESEND_COOLDOWN_SECONDS = 60
private const val MAX_RESEND_ATTEMPTS = 3