package com.crossplatform.sdk.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.presentation.BackHandler
import com.crossplatform.sdk.presentation.components.CardComponent
import com.crossplatform.sdk.presentation.components.CvvInfoBottomSheet
import com.crossplatform.sdk.presentation.components.KnowMoreBottomSheet
import com.crossplatform.sdk.presentation.components.ShowLoadingComponent
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import com.crossplatform.sdk.presentation.viewmodel.CardScreenViewModel
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalResourceApi::class)
@Composable
fun CardScreen(
    isAutoNavigationEnabled: Boolean = false,
    onBackPress : () -> Unit,
    onExitCheckout : () -> Unit
) {
    BackHandler(onBack = onBackPress)
    val viewModel: CardScreenViewModel = koinViewModel()
    val isBoxPayAnimationVisible by viewModel.isBoxPayAnimationVisible.collectAsStateWithLifecycle()
    val showWebView by viewModel.showWebview.collectAsStateWithLifecycle()
    val isSICheckboxChecked = CheckoutDetailsHandler.isSICheckboxCheckedFlow.collectAsStateWithLifecycle()
    val isSICheckboxEnabled = CheckoutDetailsHandler.isSICheckboxEnabledFlow.collectAsStateWithLifecycle()
    val isSubscriptionCheckout = CheckoutDetailsHandler.isSubscriptionCheckoutFlow.collectAsStateWithLifecycle()
    val currencyFlow = CheckoutDetailsHandler.currencyFlow.collectAsStateWithLifecycle()
    val (_, currencyCode) = currencyFlow.value
    val isTestEnv = CheckoutDetailsHandler.isTestEnvFlow.collectAsStateWithLifecycle()
    val focusedTextInputBorderColor = CheckoutDetailsHandler.focusedBorderColorFlow.collectAsStateWithLifecycle()
    val unfocusedTextInputBorderColor = CheckoutDetailsHandler.unfocusedBorderColorFlow.collectAsStateWithLifecycle()
    val buttonTextColor = CheckoutDetailsHandler.buttonTextColorFlow.collectAsStateWithLifecycle()
    val buttonColor = CheckoutDetailsHandler.buttonColorFlow.collectAsStateWithLifecycle()
    val shopperToken = CheckoutDetailsHandler.shopperTokenFlow.collectAsStateWithLifecycle()
    val subscription = CheckoutDetailsHandler.subscriptionFlow.collectAsStateWithLifecycle()
    val amount = CheckoutDetailsHandler.amountFlow.collectAsStateWithLifecycle()
    val ctaBorderRadius = CheckoutDetailsHandler.ctaBorderRadiusFlow.collectAsStateWithLifecycle()

    var isSiCheckBoxChecked  by remember { mutableStateOf(isSICheckboxChecked.value) }

    LaunchedEffect(isAutoNavigationEnabled) {
        if(isAutoNavigationEnabled) {
            onExitCheckout()
        }
    }

    val isSubscriptionDetailsVisible =
        isSubscriptionCheckout.value && isSiCheckBoxChecked

    CardComponent(
        isSICheckboxChecked = isSiCheckBoxChecked,
        isSICheckboxEnabled = isSICheckboxEnabled.value,
        isSubscriptionCheckout = isSubscriptionCheckout.value,
        isSubscriptionDetailsVisible = isSubscriptionDetailsVisible,
        onClickCheckBoxItem = {
            isSiCheckBoxChecked = it
        },
        onClickShowKnowMoreDialog = {
            viewModel.showKnowMoreDialog.value = true
        },
        onClickCVVInfo = {
            viewModel.showCvvInfo.value = true
        },
        onClickSavedCardCheckBox = {
            viewModel.isSavedCardCheckBoxClicked.value = !viewModel.isSavedCardCheckBoxClicked.value
        },
        shopperToken = shopperToken.value,
        subscription = subscription.value,
        currencySymbol = currencyCode,
        cardNumberText = viewModel.cardNumberText.value,
        cardHolderNameText = viewModel.cardHolderNameText.value,
        cardExpiryText = viewModel.cardExpiryText.value,
        cardCvvText = viewModel.cardCvvText.value,
        cardNickNameText = viewModel.cardNickNameText.value,
        cardNumberError = viewModel.cardNumberError.value,
        cardHolderNameError = viewModel.cardHolderNameError.value,
        cardExpiryError = viewModel.cardExpiryError.value,
        cardCvvError = viewModel.cardCvvError.value,
        maxCardNumberLength = viewModel.maxCardNumberLength.value,
        maxCvvLength = viewModel.maxCvvLength.value,
        handleCardNumberChange = {
            viewModel.handleCardNumberChange(it, isTestEnv = isTestEnv.value)
        },
        handleCardHolderNameChange = {
            viewModel.cardHolderNameText.value = it; if (it.isNotBlank()) viewModel.cardHolderNameError.value = false
            viewModel.checkCardValid(isTestEnv.value)
        },
        handleExpiryChange = {
            viewModel.handleExpiryChange(it, isTestEnv.value)
        },
        handleCvvChange = {
            viewModel.cardCvvText.value = it; if (it.isEmpty()) { viewModel.cardCvvError.value = true; viewModel.cardCvvErrorText.value = "Required" } else viewModel.cardCvvError.value = false
            viewModel.checkCardValid(isTestEnv.value)
        },
        cardSelectedIcon = viewModel.cardSelectedIcon.value,
        setCardNumberError = {
            viewModel.cardNumberError.value = false
        },
        setCardHolderNameError = {
            viewModel.cardHolderNameError.value = false
        },
        setCardExpiryError = {
            viewModel.cardExpiryError.value = false
        },
        setCardCvvError = {
            viewModel.cardCvvError.value = false
        },
        unfocusedTextInputBorderColor = unfocusedTextInputBorderColor.value,
        focusedTextInputBorderColor = focusedTextInputBorderColor.value,
        buttonColor = buttonColor.value,
        onBlurCardNumber = {
            val cleaned = viewModel.cardNumberText.value.filter { it.isDigit() }
            viewModel.cardNumberError.value = cleaned.isEmpty() || viewModel.cardNumberText.value.startsWith("0") ||
                    (!isTestEnv.value && (!viewModel.methodEnabled.value || !viewModel.cardNumberValid.value))
            viewModel.cardNumberErrorText.value = when {
                cleaned.isEmpty()     -> "Required"
                !viewModel.methodEnabled.value        -> "This card is not supported for the payment"
                !viewModel.cardNumberValid.value   -> "Invalid card number"
                viewModel.cardNumberText.value.startsWith("0") -> "Invalid card number"
                else                  -> ""
            }
            viewModel.checkCardValid(isTestEnv.value)
        },
        onBlurCardName = {
            viewModel.cardHolderNameError.value    = viewModel.cardHolderNameText.value.trim().isEmpty()
            viewModel.cardHolderNameErrorText.value = if (viewModel.cardHolderNameError.value) "Required" else ""
        },
        onBlurCardExpiry = {
            viewModel.cardExpiryError.value    = viewModel.cardExpiryText.value.length < 4 || !viewModel.cardExpiryValid.value
            viewModel.cardExpiryErrorText.value = when {
                viewModel.cardExpiryText.value.isEmpty() -> "Required"
                else                     -> "Invalid Expiry"
            }
        },
        onBlurCardCVV = {
            viewModel.cardCvvError.value    = viewModel.cardCvvText.value.length < viewModel.maxCvvLength.value
            viewModel.cardCvvErrorText.value = if (viewModel.cardCvvText.value.isEmpty()) "Required" else "Invalid CVV"
        },
        cardNumberErrorText = viewModel.cardNumberErrorText.value,
        cardHolderNameErrorText = viewModel.cardHolderNameErrorText.value,
        cardExpiryErrorText = viewModel.cardExpiryErrorText.value,
        cardCvvErrorText = viewModel.cardCvvErrorText.value,
        amount = amount.value,
        cardValid = viewModel.cardValid.value,
        postCardRequest = {
            viewModel.postCardRequest(it)
        },
        buttonTextColor = buttonTextColor.value,
        ctaBorderRadius = ctaBorderRadius.value,
        isBoxPayPayButtonVisible = true,
        isSavedCardCheckBoxClicked = viewModel.isSavedCardCheckBoxClicked.value,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    )

    if (viewModel.showCvvInfo.value) {
        CvvInfoBottomSheet(
            onClick = {
                viewModel.showCvvInfo.value = !viewModel.showCvvInfo.value
            },
            buttonColor = buttonColor.value,
            buttonTextColor = buttonTextColor.value,
            borderRadius = ctaBorderRadius.value
        )
    }

    if (viewModel.showKnowMoreDialog.value) {
        KnowMoreBottomSheet(
            buttonTextColor = buttonTextColor.value,
            buttonColor = buttonColor.value,
            ctaBorderRadius = ctaBorderRadius.value,
            onDismiss = {
                viewModel.showKnowMoreDialog.value = false
            }
        )
    }

    if(isBoxPayAnimationVisible) {
        ShowLoadingComponent(Modifier.fillMaxSize())
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
            Text(text = "✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = LocalSDKFonts.current.primary)
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
            fontFamily = LocalSDKFonts.current.primary,
            color = Color(0xFF2D2B32)
        )
        Text(
            text = if(heading.contains("amount", true) || heading.contains("paid", true)) {buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontSize = 12.sp,
                        fontFamily = LocalSDKFonts.current.secondary,
                        color = Color(0xFF4F4D55)
                    )
                ) {
                    append(currencySymbol)
                }

                // ✅ equivalent of amount Text
                withStyle(
                    style = SpanStyle(
                        fontSize = 12.sp,
                        fontFamily = LocalSDKFonts.current.primary
                    )
                ) {
                    append(" $value")
                }
            }} else buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontSize = 12.sp,
                        fontFamily = LocalSDKFonts.current.primary
                    )
                ) {
                    append(value)
                }
            },
            color = Color(0xFF2D2B32)
        )
    }
}