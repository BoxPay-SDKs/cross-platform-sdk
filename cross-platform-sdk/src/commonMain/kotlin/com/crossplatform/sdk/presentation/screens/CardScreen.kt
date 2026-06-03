package com.crossplatform.sdk.presentation.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.presentation.BackHandler
import com.crossplatform.sdk.presentation.ErrorText
import com.crossplatform.sdk.presentation.components.CvvInfoBottomSheet
import com.crossplatform.sdk.presentation.components.Footer
import com.crossplatform.sdk.presentation.components.KnowMoreBottomSheet
import com.crossplatform.sdk.presentation.components.PayButton
import com.crossplatform.sdk.presentation.components.ShowLoadingComponent
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import com.crossplatform.sdk.presentation.theme.defaultInterFontFamily
import com.crossplatform.sdk.presentation.toComposeColor
import com.crossplatform.sdk.presentation.viewmodel.CardScreenViewModel
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_info
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalResourceApi::class)
@Composable
fun CardScreen(
    duration               : String? = null,
    bankName               : String? = null,
    bankUrl                : String? = null,
    offerCode              : String? = null,
    emiAmount              : String? = null,
    percent                : String? = null,
    cardType               : String? = null,
    issuerBrand            : String? = null,
    isAutoNavigationEnabled: Boolean = false,
    onBackPress : () -> Unit
) {
    BackHandler(onBack = onBackPress)
    val checkoutDetails by CheckoutDetailsHandler.checkoutDetailsFlow.collectAsStateWithLifecycle()
    val viewModel: CardScreenViewModel = koinViewModel()
    val isBoxPayAnimationVisible by viewModel.isBoxPayAnimationVisible.collectAsStateWithLifecycle()
    val showWebView by viewModel.showWebview.collectAsStateWithLifecycle()

    var isSiCheckBoxChecked  by remember { mutableStateOf(checkoutDetails.isSICheckboxChecked) }


    val isEmiFlow = !duration.isNullOrEmpty()

    val isSubscriptionDetailsVisible =
        checkoutDetails.isSubscriptionCheckout && isSiCheckBoxChecked

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color.White)) {
        // --- EMI Bank Info ---
        if (!bankName.isNullOrEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE6E6E6), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
//                                AsyncImage(
//                                    model             = bankUrl,
//                                    contentDescription = bankName,
//                                    modifier          = Modifier.size(32.dp),
//                                    error             = painterResource(Res.drawable.ic_netbanking)
//                                )
                    Text(
                        text       = bankName,
                        fontFamily = defaultFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp,
                        modifier   = Modifier.padding(start = 8.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .border(
                            width = 1.5.dp,
                            color = Color(0xFFE6E6E6),
                            shape = RoundedCornerShape(0.dp)
                        )
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text       = "$duration months x ${checkoutDetails.currencySymbol}$emiAmount",
                        fontFamily = defaultFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 12.sp,
                        color      = Color(0xFF2D2B32)
                    )
                    Text(
                        text       = "@$percent% p.a.",
                        fontFamily = defaultFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize   = 12.sp,
                        color      = Color(0xFF2D2B32)
                    )
                }
            }
        }

        // --- Card Number ---
        CardTextField(
            value         = viewModel.cardNumberText.value,
            label         = "Card Number*",
            onValueChange = { viewModel.handleCardNumberChange(it, checkoutDetails.isTestEnv) },
            isError       = viewModel.cardNumberError.value,
            keyboardType  = KeyboardType.Number,
            maxLength     = viewModel.maxCardNumberLength.value,
            modifier      = Modifier.padding(start = 16.dp, end = 16.dp, top = 28.dp),
            trailingIcon  = {
                Image(
                    painter            = painterResource(viewModel.cardSelectedIcon.value),
                    contentDescription = null,
                    modifier           = Modifier.size(width = 32.dp, height = 32.dp)
                )
            },
            onFocus = { viewModel.cardNumberError.value = false },
            visualTransformation = CardNumberVisualTransformation(),
            focusedTextInputBorderColor = checkoutDetails.focusedTextInputBorderColor,
            unfocusedTextInputBorderColor = checkoutDetails.unfocusedTextInputBorderColor,
            onBlur  = {
                val cleaned = viewModel.cardNumberText.value.filter { it.isDigit() }
                viewModel.cardNumberError.value = cleaned.isEmpty() ||
                        (!checkoutDetails.isTestEnv && (!viewModel.methodEnabled.value || !viewModel.cardNumberValid.value || !viewModel.emiIssuerExist.value))
                viewModel.cardNumberErrorText.value = when {
                    cleaned.isEmpty()     -> "Required"
                    !viewModel.methodEnabled.value        -> "This card is not supported for the payment"
                    !viewModel.cardNumberValid.value     -> "Invalid card number"
                    !viewModel.emiIssuerExist.value       -> "We couldn't find any EMI plans for this card"
                    viewModel.emiIssuer.value != issuerBrand && isEmiFlow -> "The card is $viewModel.emiIssuer.value $cardType. Please enter a $issuerBrand $cardType card"
                    else                  -> ""
                }
            }
        )
        if (viewModel.cardNumberError.value) ErrorText(viewModel.cardNumberErrorText.value)

        // --- Cardholder Name ---
        CardTextField(
            value         = viewModel.cardHolderNameText.value,
            label         = "Cardholder Name*",
            onValueChange = {
                viewModel.cardHolderNameText.value = it; if (it.isNotBlank()) viewModel.cardHolderNameError.value = false
                viewModel.checkCardValid(checkoutDetails.isTestEnv, false)
                            },
            isError       = viewModel.cardHolderNameError.value,
            modifier      = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            onFocus       = { viewModel.cardHolderNameError.value = false },
            focusedTextInputBorderColor = checkoutDetails.focusedTextInputBorderColor,
            unfocusedTextInputBorderColor = checkoutDetails.unfocusedTextInputBorderColor,
            onBlur        = {
                viewModel.cardHolderNameError.value    = viewModel.cardHolderNameText.value.trim().isEmpty()
                viewModel.cardHolderNameErrorText.value = if (viewModel.cardHolderNameError.value) "Required" else ""
            }
        )
        if (viewModel.cardHolderNameError.value) ErrorText(viewModel.cardHolderNameErrorText.value)

        // --- Expiry + CVV ---
        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                CardTextField(
                    value         = viewModel.cardExpiryText.value,
                    label         = "Expiry (MM/YY)*",
                    onValueChange = {
                        viewModel.handleExpiryChange(it,checkoutDetails.isTestEnv) },
                    isError       = viewModel.cardExpiryError.value,
                    keyboardType  = KeyboardType.Number,
                    maxLength     = 5,
                    visualTransformation = ExpiryVisualTransformation(),
                    modifier      = Modifier.fillMaxWidth().padding(start = 16.dp),
                    onFocus       = { viewModel.cardExpiryError.value = false },
                    focusedTextInputBorderColor = checkoutDetails.focusedTextInputBorderColor,
                    unfocusedTextInputBorderColor = checkoutDetails.unfocusedTextInputBorderColor,
                    onBlur        = {
                        viewModel.cardExpiryError.value    = viewModel.cardExpiryText.value.length < 4 || !viewModel.cardExpiryValid.value
                        viewModel.cardExpiryErrorText.value = when {
                            viewModel.cardExpiryText.value.isEmpty() -> "Required"
                            else                     -> "Invalid Expiry"
                        }
                    }
                )
                if (viewModel.cardExpiryError.value) ErrorText(viewModel.cardExpiryErrorText.value)
            }
            Column(modifier = Modifier.weight(1f)) {
                CardTextField(
                    value         = viewModel.cardCvvText.value,
                    label         = "CVV*",
                    onValueChange =
                        {
                        viewModel.cardCvvText.value = it; if (it.isEmpty()) { viewModel.cardCvvError.value = true; viewModel.cardCvvErrorText.value = "Required" } else viewModel.cardCvvError.value = false
                        viewModel.checkCardValid(checkoutDetails.isTestEnv, false)
                        },
                    isError       = viewModel.cardCvvError.value,
                    keyboardType  = KeyboardType.NumberPassword,
                    maxLength     = viewModel.maxCvvLength.value,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier      = Modifier.fillMaxWidth().padding(end = 16.dp),
                    trailingIcon  = {
                        Image(
                            painter            = painterResource(Res.drawable.ic_info),
                            contentDescription = null,
                            modifier           = Modifier
                                .size(24.dp)
                                .clickable { viewModel.showCvvInfo.value = true },
                            colorFilter        = ColorFilter.tint(checkoutDetails.buttonColor.toComposeColor())
                        )
                    },
                    onFocus = { viewModel.cardCvvError.value = false },
                    focusedTextInputBorderColor = checkoutDetails.focusedTextInputBorderColor,
                    unfocusedTextInputBorderColor = checkoutDetails.unfocusedTextInputBorderColor,
                    onBlur  = {
                        viewModel.cardCvvError.value    = viewModel.cardCvvText.value.length < viewModel.maxCvvLength.value
                        viewModel.cardCvvErrorText.value = if (viewModel.cardCvvText.value.isEmpty()) "Required" else "Invalid CVV"
                    }
                )
                if (viewModel.cardCvvError.value) ErrorText(viewModel.cardCvvErrorText.value)
            }
        }

        // --- NickName + Save Card (shopper token) ---
        if (!checkoutDetails.shopperToken.isNullOrEmpty()) {
            CardTextField(
                value         = viewModel.cardNickNameText.value,
                label         = "Card NickName (for easy identification)",
                onValueChange = { viewModel.cardNickNameText.value = it },
                modifier      = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                focusedTextInputBorderColor = checkoutDetails.focusedTextInputBorderColor,
                unfocusedTextInputBorderColor = checkoutDetails.unfocusedTextInputBorderColor,
            )

            // CVV not stored info
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE8F6F1))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter            = painterResource(Res.drawable.ic_info),
                    contentDescription = null,
                    modifier           = Modifier.size(20.dp),
                    colorFilter        = ColorFilter.tint(Color(0xFF2D2B32))
                )
                Text(
                    text       = "CVV will not be stored",
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize   = 12.sp,
                    color      = Color(0xFF2D2B32),
                    modifier   = Modifier.padding(start = 8.dp)
                )
            }

            // Save card checkbox
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheckboxItem(
                    isChecked   = viewModel.isSavedCardCheckBoxClicked.value,
                    buttonColor = checkoutDetails.buttonColor,
                    onClick     = { viewModel.isSavedCardCheckBoxClicked.value = !viewModel.isSavedCardCheckBoxClicked.value }
                )
                Text(
                    text       = "Save this card as per RBI guidelines.",
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize   = 14.sp,
                    color      = Color(0xFF2D2B32),
                    modifier   = Modifier.padding(start = 6.dp)
                )
                Text(
                    text       = "Know more",
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 12.sp,
                    color      = checkoutDetails.buttonColor.toComposeColor(),
                    textDecoration = TextDecoration.Underline,
                    modifier   = Modifier
                        .padding(start = 4.dp)
                        .clickable { viewModel.showKnowMoreDialog.value = true }
                )
            }
        }

        // --- SI Checkbox ---
        if ((checkoutDetails.isSICheckboxChecked || checkoutDetails.isSICheckboxEnabled) &&
            checkoutDetails.isSubscriptionCheckout
        ) {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheckboxItem(
                    isChecked   = isSiCheckBoxChecked,
                    buttonColor = checkoutDetails.buttonColor,
                    onClick     = {
                        if(checkoutDetails.isSICheckboxEnabled) {
                            isSiCheckBoxChecked = !isSiCheckBoxChecked
                        }
                    }
                )
                Text(
                    text       = "Set up Standing Instructions (SI) for this payment.",
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize   = 14.sp,
                    color      = Color(0xFF2D2B32),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier   = Modifier.padding(start = 6.dp)
                )
            }
        }

        // --- Subscription Details ---
        if (isSubscriptionDetailsVisible) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFEFF3FA))
                    .padding(vertical = 12.dp)
            ) {
                checkoutDetails.subscription?.forEach { item ->
                    SubscriptionRow(
                        heading        = item.first,
                        value          = item.second,
                        currencySymbol = checkoutDetails.currencySymbol
                    )
                }
            }
        }
        // --- Pay Button ---
        Spacer(Modifier.weight(1f))
        PayButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                .clip(RoundedCornerShape(checkoutDetails.ctaBorderRadius.dp))
                .background(
                    if (viewModel.cardValid.value) checkoutDetails.buttonColor.toComposeColor()
                    else Color(0xFFE6E6E6)
                )
                .clickable(enabled = viewModel.cardValid.value) {
                    viewModel.postCardRequest(isSiCheckBoxChecked)
                },
            amount = checkoutDetails.amount,
            currencySymbol = checkoutDetails.currencySymbol,
            isValid = viewModel.cardValid.value,
            buttonTextColor = checkoutDetails.buttonTextColor,
            text = "Pay"
        )
        Footer()
    }


    if (viewModel.showCvvInfo.value) {
        CvvInfoBottomSheet(
            onClick = {
                viewModel.showCvvInfo.value = !viewModel.showCvvInfo.value
            },
            buttonColor = checkoutDetails.buttonColor,
            buttonTextColor = checkoutDetails.buttonTextColor,
            borderRadius = checkoutDetails.ctaBorderRadius
        )
    }

    if (viewModel.showKnowMoreDialog.value) {
        KnowMoreBottomSheet(
            buttonTextColor = checkoutDetails.buttonTextColor,
            buttonColor = checkoutDetails.buttonColor,
            ctaBorderRadius = checkoutDetails.ctaBorderRadius,
            onDismiss = {
                viewModel.showKnowMoreDialog.value = false
            }
        )
    }

    if(isBoxPayAnimationVisible) {
        ShowLoadingComponent()
    }

    if(showWebView) {
        WebViewScreen(
            url = viewModel.url.value,
            html = viewModel.htmlString.value,
            onBackPress = {result ->
                viewModel.callFetchStatus(result ?: "")
                viewModel.setWebViewScreen(false)
            }
        )
    }
}

// --- Reusable Card TextField ---
@Composable
private fun CardTextField(
    value               : String,
    label               : String,
    onValueChange       : (String) -> Unit,
    modifier            : Modifier = Modifier,
    isError             : Boolean = false,
    keyboardType        : KeyboardType = KeyboardType.Text,
    maxLength           : Int = Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon        : @Composable (() -> Unit)? = null,
    onFocus             : () -> Unit = {},
    onBlur              : () -> Unit = {},
    focusedTextInputBorderColor : String,
    unfocusedTextInputBorderColor : String
) {
    var hasBeenFocused by remember { mutableStateOf(false) }  // ← track first focus
    OutlinedTextField(
        value               = value,
        onValueChange       = { if (it.length <= maxLength) onValueChange(it) },
        label               = {
            Text(
                text       = label,
                fontFamily = defaultFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize   = 16.sp
            )
        },
        isError              = isError,
        trailingIcon         = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions      = KeyboardOptions(keyboardType = keyboardType),
        shape                = RoundedCornerShape(8.dp),
        modifier             = modifier
            .fillMaxWidth()
            .height(62.dp)
            .onFocusChanged { focusState ->
                when {
                    focusState.isFocused -> {
                        hasBeenFocused = true  // ← mark as focused
                        onFocus()
                    }
                    hasBeenFocused -> {        // ← only call onBlur after first focus ✅
                        onBlur()
                    }
                }
            },
        textStyle            = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = Color(0xFF0A090B)
        ),
        colors = OutlinedTextFieldDefaults.colors(
            // Border
            focusedBorderColor   = focusedTextInputBorderColor.toComposeColor(),
            unfocusedBorderColor = unfocusedTextInputBorderColor.toComposeColor(),
        )
    )
}

// --- Reusable Checkbox ---
@Composable
fun CheckboxItem(
    isChecked  : Boolean,
    buttonColor: String,
    onClick    : () -> Unit
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(3.dp))
            .border(2.dp, buttonColor.toComposeColor(), RoundedCornerShape(3.dp))
            .background(if (isChecked) buttonColor.toComposeColor() else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isChecked) {
            Text(
                text       = "✓",
                color      = Color.White,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = defaultFontFamily
            )
        }
    }
}

class CardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text.take(16)

        // Format as groups of 4
        val formatted = trimmed.chunked(4).joinToString(" ")

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                // Add spaces: every 4 digits adds 1 space
                val spaces = minOf(offset / 4, (formatted.length - trimmed.length))
                return (offset + spaces).coerceAtMost(formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                // Remove spaces from offset
                val spaces = formatted.take(offset).count { it == ' ' }
                return (offset - spaces).coerceAtMost(trimmed.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}


class ExpiryVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text.take(4)  // raw digits only "MMYY"

        // Format as MM/YY
        val formatted = when {
            trimmed.length > 2 -> "${trimmed.take(2)}/${trimmed.drop(2)}"
            else               -> trimmed
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return when {
                    offset <= 2 -> offset
                    else        -> (offset + 1).coerceAtMost(formatted.length) // +1 for slash
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset <= 2 -> offset
                    offset == 3 -> 2  // on the slash → map to before slash
                    else        -> (offset - 1).coerceAtMost(trimmed.length) // -1 for slash
                }
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

@Composable
fun SubscriptionRow(
    heading: String,
    value: String,
    currencySymbol : String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp), // paddingTop: 4 approximated as vertical for symmetry; change to top-only if needed
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = heading,
            fontFamily = defaultFontFamily,
            color = Color(0xFF2D2B32)
        )
        Text(
            text = if(heading.contains("amount", true) || heading.contains("paid", true)) {buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontSize = 12.sp,
                        fontFamily = defaultInterFontFamily,
                        color = Color(0xFF4F4D55)
                    )
                ) {
                    append(currencySymbol)
                }

                // ✅ equivalent of amount Text
                withStyle(
                    style = SpanStyle(
                        fontSize = 12.sp,
                        fontFamily = defaultFontFamily
                    )
                ) {
                    append(value)
                }
            }} else buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontSize = 12.sp,
                        fontFamily = defaultFontFamily
                    )
                ) {
                    append(value)
                }
            },
            color = Color(0xFF2D2B32)
        )
    }
}