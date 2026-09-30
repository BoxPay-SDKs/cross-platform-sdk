package com.crossplatform.sdk.presentation.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.domain.model.SurchargeModel
import com.crossplatform.sdk.presentation.ErrorText
import com.crossplatform.sdk.presentation.screens.CardNumberVisualTransformation
import com.crossplatform.sdk.presentation.screens.CheckboxItem
import com.crossplatform.sdk.presentation.screens.ExpiryVisualTransformation
import com.crossplatform.sdk.presentation.screens.SubscriptionRow
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.card_holder_name_title
import crossplatformsdk.cross_platform_sdk.generated.resources.card_nick_name_title
import crossplatformsdk.cross_platform_sdk.generated.resources.card_number_title
import crossplatformsdk.cross_platform_sdk.generated.resources.cvv_not_be_stored_info
import crossplatformsdk.cross_platform_sdk.generated.resources.cvv_title
import crossplatformsdk.cross_platform_sdk.generated.resources.duration_months_x
import crossplatformsdk.cross_platform_sdk.generated.resources.expiry_title
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_cvv_info
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_info
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_netbanking
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_scan_card
import crossplatformsdk.cross_platform_sdk.generated.resources.know_more_cta
import crossplatformsdk.cross_platform_sdk.generated.resources.pay_cta
import crossplatformsdk.cross_platform_sdk.generated.resources.percent_title
import crossplatformsdk.cross_platform_sdk.generated.resources.save_this_card_info
import crossplatformsdk.cross_platform_sdk.generated.resources.scan_card_cta
import crossplatformsdk.cross_platform_sdk.generated.resources.set_up_si_instructions
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CardComponent(
    duration               : String? = null,
    bankName               : String? = null,
    bankUrl                : String? = null,
    emiAmount              : String? = null,
    percent                : String? = null,
    isBoxPayPayButtonVisible : Boolean,
    currencySymbol : String,
    cardNumberText : String,
    cardHolderNameText : String,
    cardExpiryText : String,
    cardCvvText : String,
    cardNickNameText : String,
    cardExpiryError : Boolean,
    cardCvvError : Boolean,
    maxCvvLength : Int,
    handleCardNumberChange : (String) -> Unit,
    handleCardHolderNameChange : (String) -> Unit,
    handleExpiryChange : (String) -> Unit,
    handleCvvChange : (String) -> Unit,
    cardNumberError : Boolean,
    cardHolderNameError : Boolean,
    maxCardNumberLength : Int,
    cardSelectedIcon : DrawableResource,
    setCardNumberError : () -> Unit,
    setCardHolderNameError : () -> Unit,
    setCardExpiryError : () -> Unit,
    setCardCvvError : () -> Unit,
    unfocusedTextInputBorderColor : String,
    focusedTextInputBorderColor : String,
    buttonColor : String,
    onBlurCardNumber : () -> Unit,
    onBlurCardName : () -> Unit,
    onBlurCardExpiry : () -> Unit,
    onBlurCardCVV : () -> Unit,
    cardNumberErrorText  : String,
    cardHolderNameErrorText : String,
    cardExpiryErrorText : String,
    cardCvvErrorText : String,
    amount : Double,
    cardValid : Boolean,
    isSICheckboxChecked : Boolean,
    isSICheckboxEnabled : Boolean,
    isSubscriptionCheckout : Boolean,
    postCardRequest : (Boolean) -> Unit,
    subscription : List<Pair<String, String>>?,
    buttonTextColor : String,
    ctaBorderRadius : Int,
    isSubscriptionDetailsVisible : Boolean,
    onClickCheckBoxItem : (Boolean) -> Unit,
    onClickShowKnowMoreDialog : () -> Unit,
    onClickCVVInfo :() -> Unit,
    isSavedCardCheckBoxClicked : Boolean,
    onClickSavedCardCheckBox : () -> Unit,
    shopperToken : String?,
    modifier: Modifier,
    appliedSurcharge : List<SurchargeModel>,
    normalCheckout : Boolean = true
) {
//    val acceptedCardList = CheckoutDetailsHandler.acceptedCardsListFlow.collectAsStateWithLifecycle()
    var showCardScanner by remember { mutableStateOf(false) }

    if (showCardScanner) {
        CardScanDialog(
            accentColor = buttonColor.toComposeColor(),
            onDismiss   = { showCardScanner = false },
            onScanned   = { card ->
                showCardScanner = false
                // Fed through the same handlers as typing, so brand detection, max length,
                // validation and cardValid all behave exactly as if the user had typed it.
                card.cardNumber?.let { handleCardNumberChange(it.take(maxCardNumberLength)) }
                // Digits only ("MMYY"), the format the user would type into ExpiryVisualTransformation.
                // If handleExpiryChange expects "MM/YY", change this to "${month}/${year}".
                val month = card.expiryMonth
                val year  = card.expiryYear
                if (month != null && year != null) handleExpiryChange(month + year)
                card.holderName?.let { handleCardHolderNameChange(it) }
            }
        )
    }

    Column(modifier = modifier.fillMaxWidth().background(Color.White)) {
        // --- EMI Bank Info ---
        if (!bankName.isNullOrEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, LocalSDKColors.current.divider, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KamelImage(
                        resource              = asyncPainterResource(data = bankUrl ?: ""),
                        contentDescription = "Bank Logo",
                        modifier           = Modifier.size(32.dp),
                        onLoading = {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFE0E0E0), CircleShape)
                            )
                        },
                        onFailure = {
                            Image(
                                painter = painterResource(Res.drawable.ic_netbanking),
                                contentDescription = "Back",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    )
                    Text(
                        text       = bankName,
                        fontFamily = LocalSDKFonts.current.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp,
                        modifier   = Modifier.padding(start = 8.dp),
                        color      = LocalSDKColors.current.textPrimary,
                    )
                }
                Row {
                    Text(
                        text = buildAnnotatedString {
                            append(
                                AnnotatedString(
                                    text = stringResource(Res.string.duration_months_x, duration ?: ""),
                                    spanStyle = SpanStyle(
                                        fontFamily = LocalSDKFonts.current.primary
                                    )
                                )
                            )
                            append(
                                AnnotatedString(
                                    text = currencySymbol
                                )
                            )
                            append(
                                AnnotatedString(
                                    text = " $emiAmount",
                                    spanStyle = SpanStyle(
                                        fontFamily = LocalSDKFonts.current.primary
                                    )
                                )
                            )
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 12.sp,
                        color      = LocalSDKColors.current.textPrimary
                    )
                    Text(
                        text       = stringResource(Res.string.percent_title, percent ?: ""),
                        fontFamily = LocalSDKFonts.current.primary,
                        fontWeight = FontWeight.Normal,
                        fontSize   = 12.sp,
                        color      = LocalSDKColors.current.textPrimary
                    )
                }
            }
        }

        // --- Scan card ---
        if (normalCheckout) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showCardScanner = true }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter            = painterResource(Res.drawable.ic_scan_card),
                        contentDescription = null,
                        modifier           = Modifier.size(18.dp),
                        colorFilter        = ColorFilter.tint(buttonColor.toComposeColor())
                    )
                    Text(
                        text       = stringResource(Res.string.scan_card_cta),
                        fontFamily = LocalSDKFonts.current.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 13.sp,
                        color      = buttonColor.toComposeColor(),
                        modifier   = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }

        // --- Card Number ---
        CardTextField(
            value         = cardNumberText,
            label         = stringResource(Res.string.card_number_title),
            onValueChange = { handleCardNumberChange(it) },
            isError       = cardNumberError,
            keyboardType  = KeyboardType.Number,
            maxLength     = maxCardNumberLength,
            modifier      = Modifier.padding(
                start = 16.dp,
                end   = 16.dp,
                top   = if (normalCheckout) 8.dp else 28.dp
            ),
            trailingIcon  = {
                Image(
                    painter            = painterResource(cardSelectedIcon),
                    contentDescription = null,
                    modifier           = Modifier.size(width = 32.dp, height = 32.dp)
                )
            },
            onFocus = { setCardNumberError() },
            visualTransformation = CardNumberVisualTransformation(),
            focusedTextInputBorderColor = focusedTextInputBorderColor,
            unfocusedTextInputBorderColor = unfocusedTextInputBorderColor,
            onBlur  = {
                onBlurCardNumber()
            }
        )
        if (cardNumberError) ErrorText(cardNumberErrorText)

        // --- Expiry + CVV ---
        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                CardTextField(
                    value         = cardExpiryText,
                    label         = stringResource(Res.string.expiry_title),
                    onValueChange = {
                        handleExpiryChange(it)
                    },
                    isError       = cardExpiryError,
                    keyboardType  = KeyboardType.Number,
                    maxLength     = 5,
                    visualTransformation = ExpiryVisualTransformation(),
                    modifier      = Modifier.fillMaxWidth().padding(start = 16.dp),
                    onFocus       = { setCardExpiryError() },
                    focusedTextInputBorderColor = focusedTextInputBorderColor,
                    unfocusedTextInputBorderColor = unfocusedTextInputBorderColor,
                    onBlur        = {
                        onBlurCardExpiry()
                    }
                )
                if (cardExpiryError) ErrorText(cardExpiryErrorText)
            }
            Column(modifier = Modifier.weight(1f)) {
                CardTextField(
                    value         = cardCvvText,
                    label         = stringResource(Res.string.cvv_title),
                    onValueChange =
                        {
                            handleCvvChange(it)
                        },
                    isError       = cardCvvError,
                    keyboardType  = KeyboardType.NumberPassword,
                    maxLength     = maxCvvLength,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier      = Modifier.fillMaxWidth().padding(end = 16.dp),
                    trailingIcon  = {
                        Image(
                            painter            = painterResource(Res.drawable.ic_cvv_info),
                            contentDescription = null,
                            modifier           = Modifier
                                .size(24.dp)
                                .clickable { onClickCVVInfo() },
                            colorFilter        = ColorFilter.tint(buttonColor.toComposeColor())
                        )
                    },
                    onFocus = { setCardCvvError() },
                    focusedTextInputBorderColor = focusedTextInputBorderColor,
                    unfocusedTextInputBorderColor = unfocusedTextInputBorderColor,
                    onBlur  = {
                        onBlurCardCVV()
                    }
                )
                if (cardCvvError) ErrorText(cardCvvErrorText)
            }
        }

        // --- Cardholder Name ---
        CardTextField(
            value         = cardHolderNameText,
            label         = stringResource(Res.string.card_holder_name_title),
            onValueChange = {
                handleCardHolderNameChange(it)
            },
            isError       = cardHolderNameError,
            modifier      = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            onFocus       = { setCardHolderNameError() },
            focusedTextInputBorderColor = focusedTextInputBorderColor,
            unfocusedTextInputBorderColor = unfocusedTextInputBorderColor,
            onBlur        = {
                onBlurCardName()
            }
        )
        if (cardHolderNameError) ErrorText(cardHolderNameErrorText)

        // --- NickName + Save Card (shopper token) ---
        if (!shopperToken.isNullOrEmpty()) {
            CardTextField(
                value         = cardNickNameText,
                label         = stringResource(Res.string.card_nick_name_title),
                onValueChange = {  },
                modifier      = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                focusedTextInputBorderColor = focusedTextInputBorderColor,
                unfocusedTextInputBorderColor = unfocusedTextInputBorderColor,
            )

            // CVV not stored info
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LocalSDKColors.current.surfaceVariant)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter            = painterResource(Res.drawable.ic_info),
                    contentDescription = null,
                    modifier           = Modifier.size(20.dp),
                    colorFilter        = ColorFilter.tint(LocalSDKColors.current.textPrimary)
                )
                Text(
                    text       = stringResource(Res.string.cvv_not_be_stored_info),
                    fontFamily = LocalSDKFonts.current.primary,
                    fontWeight = FontWeight.Normal,
                    fontSize   = 12.sp,
                    color      = LocalSDKColors.current.textPrimary,
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
                    isChecked   = isSavedCardCheckBoxClicked,
                    buttonColor = buttonColor,
                    onClick     = { onClickSavedCardCheckBox() }
                )
                Text(
                    text       = stringResource(Res.string.save_this_card_info),
                    fontFamily = LocalSDKFonts.current.primary,
                    fontWeight = FontWeight.Normal,
                    fontSize   = 14.sp,
                    color      = LocalSDKColors.current.textPrimary,
                    modifier   = Modifier.padding(start = 6.dp)
                )
                Text(
                    text       = stringResource(Res.string.know_more_cta),
                    fontFamily = LocalSDKFonts.current.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 12.sp,
                    color      = buttonColor.toComposeColor(),
                    textDecoration = TextDecoration.Underline,
                    modifier   = Modifier
                        .padding(start = 4.dp)
                        .clickable { onClickShowKnowMoreDialog() }
                )
            }
        }

        // --- SI Checkbox ---
        if ((isSICheckboxChecked || isSICheckboxEnabled) &&
            isSubscriptionCheckout
        ) {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheckboxItem(
                    isChecked   = isSICheckboxChecked,
                    buttonColor = buttonColor,
                    onClick     = {
                        if(isSICheckboxEnabled) {
                            onClickCheckBoxItem(!isSICheckboxChecked)
                        }
                    }
                )
                Text(
                    text       = stringResource(Res.string.set_up_si_instructions),
                    fontFamily = LocalSDKFonts.current.primary,
                    fontWeight = FontWeight.Normal,
                    fontSize   = 14.sp,
                    color      = LocalSDKColors.current.textPrimary,
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
                subscription?.forEach { item ->
                    SubscriptionRow(
                        heading        = item.first,
                        value          = item.second,
                        currencySymbol = currencySymbol
                    )
                }
            }
        }
        Spacer(Modifier.then(
            if(!normalCheckout) Modifier.height(10.dp)
            else Modifier.weight(1f)
        ))
        if(appliedSurcharge.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFEFF3FA))
                    .padding(vertical = 12.dp)
            ) {
                appliedSurcharge.forEach { item ->
                    SummaryRow(
                        label = item.title,
                        amount = item.amount,
                        currencySymbol = "+ $currencySymbol",
                        buttonColor = buttonColor,
                        description = item.description
                    )
                }
            }
        }
        if(isBoxPayPayButtonVisible) {
            PayButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .clip(RoundedCornerShape(ctaBorderRadius.dp))
                    .background(
                        if (cardValid) buttonColor.toComposeColor()
                        else LocalSDKColors.current.divider
                    )
                    .clickable(enabled = cardValid) {
                        postCardRequest(isSICheckboxChecked)
                    },
                amount = amount,
                currencySymbol = currencySymbol,
                isValid = cardValid,
                buttonTextColor = buttonTextColor,
                text = stringResource(Res.string.pay_cta)
            )
        }
        Footer()
    }
}

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
    val keyboardController = LocalSoftwareKeyboardController.current   // ← add this

    OutlinedTextField(
        value               = value,
        onValueChange       = { new ->
            if (new.length <= maxLength) {
                onValueChange(new)
                if (keyboardType == KeyboardType.Number || keyboardType == KeyboardType.NumberPassword) {
                    if (new.length == maxLength) {
                        keyboardController?.hide()
                        onBlur()
                    }
                }
            }
        },
        label               = {
            Text(
                text       = label,
                fontFamily = LocalSDKFonts.current.primary,
                fontWeight = FontWeight.Normal,
                fontSize   = 14.sp,
                color      = LocalSDKColors.current.textPrimary,
            )
        },
        isError              = isError,
        trailingIcon         = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions      = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        shape                = RoundedCornerShape(8.dp),
        keyboardActions = KeyboardActions(
            onDone = {
                keyboardController?.hide()
                onBlur()
            }
        ),
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
            fontFamily = LocalSDKFonts.current.primary,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = LocalSDKColors.current.textPrimary
        ),
        colors = OutlinedTextFieldDefaults.colors(
            // Border
            focusedBorderColor   = focusedTextInputBorderColor.toComposeColor(),
            unfocusedBorderColor = unfocusedTextInputBorderColor.toComposeColor(),
        )
    )
}