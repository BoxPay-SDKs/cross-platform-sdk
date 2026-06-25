package com.crossplatform.sdk.presentation.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.AnalyticsEvents
import com.crossplatform.sdk.domain.model.Bank
import com.crossplatform.sdk.domain.model.ChooseEmiModel
import com.crossplatform.sdk.domain.model.EmiCardGroup
import com.crossplatform.sdk.presentation.BackHandler
import com.crossplatform.sdk.presentation.ChevronIcon
import com.crossplatform.sdk.presentation.SectionTitle
import com.crossplatform.sdk.presentation.UiState
import com.crossplatform.sdk.presentation.components.CardComponent
import com.crossplatform.sdk.presentation.components.CvvInfoBottomSheet
import com.crossplatform.sdk.presentation.components.EmptyListView
import com.crossplatform.sdk.presentation.components.Footer
import com.crossplatform.sdk.presentation.components.KnowMoreBottomSheet
import com.crossplatform.sdk.presentation.components.PayButton
import com.crossplatform.sdk.presentation.components.ShimmerView
import com.crossplatform.sdk.presentation.components.ShowLoadingComponent
import com.crossplatform.sdk.presentation.formatPercent
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import com.crossplatform.sdk.presentation.viewmodel.EMIScreenViewModel
import com.crossplatform.sdk.presentation.viewmodel.EmiStep
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.add_icon
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_netbanking
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_search
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_tick_arrow
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EMIScreen(
    modifier : Modifier = Modifier.fillMaxSize(),
    onBackPress : () -> Unit,
    isAutoNavigationEnabled : Boolean,
    onExitCheckout : () -> Unit
) {
    val viewModel : EMIScreenViewModel = koinViewModel()
    val handleBack = {
        if (!viewModel.goBackStep()) onBackPress()   // exit EMI only at root
    }
    BackHandler(onBack = handleBack)
    DisposableEffect(Unit) {
        ScreenBackInterceptor.onBack = { viewModel.goBackStep() }
        ScreenBackInterceptor.currentTitle = {
            when (viewModel.currentStep) {
                EmiStep.Card    -> "Pay via Card"
                EmiStep.Tenure  -> "Select Tenure"
                EmiStep.Content -> "Choose EMI Option"
            }
        }
        onDispose {
            ScreenBackInterceptor.onBack = null
            ScreenBackInterceptor.currentTitle = null
        }
    }
    val buttonColor = CheckoutDetailsHandler.buttonColorFlow.collectAsStateWithLifecycle()
    val buttonTextColor = CheckoutDetailsHandler.buttonTextColorFlow.collectAsStateWithLifecycle()
    val focusedTextInputBorderColor = CheckoutDetailsHandler.focusedBorderColorFlow.collectAsStateWithLifecycle()
    val unfocusedTextInputBorderColor = CheckoutDetailsHandler.unfocusedBorderColorFlow.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isBoxPayAnimationVisible by viewModel.isBoxPayAnimationVisible.collectAsStateWithLifecycle()
    val showWebView by viewModel.showWebview.collectAsStateWithLifecycle()
    val isSICheckboxChecked = CheckoutDetailsHandler.isSICheckboxCheckedFlow.collectAsStateWithLifecycle()
    val isSICheckboxEnabled = CheckoutDetailsHandler.isSICheckboxEnabledFlow.collectAsStateWithLifecycle()
    val isSubscriptionCheckout = CheckoutDetailsHandler.isSubscriptionCheckoutFlow.collectAsStateWithLifecycle()
    val currencyFlow = CheckoutDetailsHandler.currencyFlow.collectAsStateWithLifecycle()
    val (currencySymbol, _) = currencyFlow.value
    val isTestEnv = CheckoutDetailsHandler.isTestEnvFlow.collectAsStateWithLifecycle()
    var isSiCheckBoxChecked  by remember { mutableStateOf(isSICheckboxChecked.value) }
    val shopperToken = CheckoutDetailsHandler.shopperTokenFlow.collectAsStateWithLifecycle()
    val subscription = CheckoutDetailsHandler.subscriptionFlow.collectAsStateWithLifecycle()
    val amount = CheckoutDetailsHandler.amountFlow.collectAsStateWithLifecycle()
    val ctaBorderRadius = CheckoutDetailsHandler.ctaBorderRadiusFlow.collectAsStateWithLifecycle()


    LaunchedEffect(isAutoNavigationEnabled) {
        if(isAutoNavigationEnabled) {
            onExitCheckout()
        }
    }

    val isSubscriptionDetailsVisible =
        isSubscriptionCheckout.value && isSiCheckBoxChecked

    when(uiState) {
        is UiState.Error -> {
            val message = (uiState as UiState.Error).message
            Text("Welcome to error screen $message")
            LaunchedEffect(message) {
                viewModel.callUiAnalytics(
                    event      = AnalyticsEvents.SDK_CRASH.value,
                    screenName = "BoxPayElements",
                    message    = "BoxPay elements not loaded $message",
                )
            }
        }
        UiState.Loading -> {
            ShimmerView(modifier = modifier)
        }
        is UiState.Success -> {
            Column(modifier = modifier) {
                when(viewModel.currentStep) {
                    EmiStep.Card -> {
                        CardComponent(
                            duration = viewModel.selectedEmi.value.first.toString(),
                            emiAmount = viewModel.selectedEmi.value.second,
                            bankUrl = viewModel.selectedBank.value?.iconUrl,
                            bankName = viewModel.selectedBank.value?.name,
                            percent = viewModel.selectedPercent.value.toString(),
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
                            currencySymbol = currencySymbol,
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
                                viewModel.cardNumberError.value = cleaned.isEmpty() ||
                                        (!isTestEnv.value && (!viewModel.methodEnabled.value || !viewModel.cardNumberValid.value))
                                viewModel.cardNumberErrorText.value = when {
                                    cleaned.isEmpty()     -> "Required"
                                    !viewModel.methodEnabled.value        -> "This card is not supported for the payment"
                                    !viewModel.cardNumberValid.value     -> "Invalid card number"
                                    else                  -> ""
                                }
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
                                viewModel.postEMIRequest()
                            },
                            buttonTextColor = buttonTextColor.value,
                            ctaBorderRadius = ctaBorderRadius.value,
                            isBoxPayPayButtonVisible = true,
                            isSavedCardCheckBoxClicked = viewModel.isSavedCardCheckBoxClicked.value,
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        )
                    }
                    EmiStep.Tenure -> SelectTenureScreen(
                        selectedBank = viewModel.selectedBank.value,
                        cardType = viewModel.selectedCard.value,
                        selectedEmi = viewModel.selectedEmi.value,
                        buttonColor = buttonColor.value,
                        buttonTextColor = buttonTextColor.value,
                        onClickRadio = {duration, amount, code ->
                            viewModel.onClickRadio(duration = duration, amount = amount, code = code)
                        },
                        onProceed = { percent, low, no, discount, netAmount ->
                            viewModel.onProceedEmi(
                                percent,
                                low,
                                no,
                                discount,
                                netAmount
                            )
                        },
                        currencySymbol = currencySymbol,
                        ctaBorderRadius = ctaBorderRadius.value
                    )
                    else -> EmiContentScreen(
                        selectedCard = viewModel.selectedCard.value,
                        onClickCard = {viewModel.onClickCard(it)},
                        onClickBank = {viewModel.onClickBank(it)},
                        searchText = viewModel.searchText.value,
                        onEditSearchText = {viewModel.onEditSearchText(it)},
                        emiModel = (uiState as UiState.Success).data,
                        buttonColor = buttonColor.value,
                        onToggleFilter = {viewModel.onToggleFilter(it)},
                        selectedOthers = viewModel.selectedOthers.value,
                        selectedFilter = viewModel.selectedFilter.value,
                        onProceedOther = {viewModel.onProceedWithOther()},
                        onSelectOthersOption = {viewModel.onSelectedOthersOption(it)},
                        focusedTextInputBorderColor = focusedTextInputBorderColor.value,
                        unfocusedTextInputBorderColor = unfocusedTextInputBorderColor.value,
                        buttonTextColor = buttonTextColor.value,
                        ctaBorderRadius = ctaBorderRadius.value
                    )
                }
            }

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
    }
}
// ─── Main content (bank list) ─────────────────────────────────────────────────

@Composable
fun EmiContentScreen(
    selectedCard : String,
    onClickCard : (String) -> Unit,
    onClickBank : (Bank) -> Unit,
    emiModel : ChooseEmiModel,
    buttonColor : String,
    focusedTextInputBorderColor : String,
    unfocusedTextInputBorderColor : String,
    searchText : String,
    onEditSearchText : (String) -> Unit,
    selectedOthers : String?,
    onToggleFilter : (String) -> Unit,
    selectedFilter: String,
    onSelectOthersOption : (String) -> Unit,
    onProceedOther : () -> Unit,
    ctaBorderRadius: Int,
    buttonTextColor: String
) {
    val (isNoCostExisted, isLowCostExisted) = emiModel.cards
        .flatMap { it.banks }
        .fold(Pair(false, false)) { (noCost, lowCost), bank ->
            Pair(noCost || bank.noCostApplied, lowCost || bank.lowCostApplied)
        }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F6FB))) {

        // ── Header ─────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth().wrapContentHeight().background(Color.White)
        ) {
            EmiCardTypeTabRow(
                cards      = emiModel.cards,
                selected   = selectedCard,
                brandColor = buttonColor.toComposeColor(),
                onSelect   = { onClickCard(it) },
            )

            HorizontalDivider(color = Color(0xFFE6E6E6))

            // ── Search bar ────────────────────────────────────────────────────────
            EmiSearchBar(
                value       = searchText,
                placeholder = if (selectedCard == "Others") "Search for other EMI options" else "Search for bank",
                onValueChange = { onEditSearchText(it) },
                focusedBorderColor = focusedTextInputBorderColor.toComposeColor(),
                unFocusedBorderColor = unfocusedTextInputBorderColor.toComposeColor()
            )

            // ── Filter chips ──────────────────────────────────────────────────────
            if ((isNoCostExisted ||  isLowCostExisted) && !selectedCard.equals("others", true)) {
                EmiFilterRow(
                    isNoCostExisted,
                    isLowCostExisted,
                    onToggle   = { label -> onToggleFilter(label) },
                    selectedFilter = selectedFilter
                )
            }
        }

        // ── Bank list ─────────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            val cardData = emiModel.cards.find { it.cardType == selectedCard }
            SectionTitle(if (selectedCard.equals("others", true)) "Others" else "All Banks")

            when {
                cardData == null || cardData.banks.isEmpty() -> EmptyListView(
                    heading = "Oops!! No results found",
                    subHeading = "Please try another search"
                )

                selectedCard.equals("others", true) -> OthersPaymentList(
                    banks          = cardData.banks,
                    selectedValue  = selectedOthers,
                    onSelect       = { onSelectOthersOption(it) },
                    onProceed      = { onProceedOther() },
                    ctaBorderRadius = ctaBorderRadius,
                    buttonColor = buttonColor,
                    buttonTextColor = buttonTextColor
                )

                else -> {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp).background(Color.White, RoundedCornerShape(12.dp))
                    ) {
                        cardData.banks.mapIndexed { index, bank ->
                            BankCard(
                                name          = bank.name,
                                iconUrl       = bank.iconUrl,
                                hasNoCostEmi  = bank.noCostApplied,
                                hasLowCostEmi = bank.lowCostApplied,
                                onPress       = { onClickBank(bank) },
                                percent = bank.percent
                            )
                            if (index < cardData.banks.lastIndex) {
                                HorizontalDivider(color = Color(0xFFE6E6E6))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Footer()
        }
    }
}

// ─── Card type tab row ────────────────────────────────────────────────────────

@Composable
private fun EmiCardTypeTabRow(
    cards: List<EmiCardGroup>,
    selected: String,
    brandColor: Color,
    onSelect: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        cards.forEach { group ->
            val isSelected = group.cardType == selected
            Column(
                modifier     = Modifier
                    .clickable { onSelect(group.cardType) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text       = group.cardType,
                    color      = if (isSelected) brandColor else Color(0xFF010102),
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = LocalSDKFonts.current.primary,
                    fontSize   = 14.sp,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(if (isSelected) brandColor else Color.Transparent)
                )
            }
        }
    }
}


// ─── Search bar ───────────────────────────────────────────────────────────────

@Composable
private fun EmiSearchBar(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    focusedBorderColor: Color,
    unFocusedBorderColor  : Color
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        placeholder   = { Text(placeholder, color = Color(0xFFADACB0), fontSize = 14.sp, fontFamily = LocalSDKFonts.current.primary) },
        leadingIcon   = { Image(
            painter            = painterResource(Res.drawable.ic_search),
            contentDescription = null,
            modifier           = Modifier.size(width = 32.dp, height = 32.dp)
        ) },
        shape         = RoundedCornerShape(6.dp),
        singleLine    = true,
        colors = OutlinedTextFieldDefaults.colors(
            // Border
            focusedBorderColor   = focusedBorderColor,
            unfocusedBorderColor = unFocusedBorderColor,
        )
    )
}


// ─── Filter chips ─────────────────────────────────────────────────────────────

@Composable
private fun EmiFilterRow(
    isNoCostApplied : Boolean,
    isLowCostApplied : Boolean,
    onToggle: (String) -> Unit,
    selectedFilter: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if(isNoCostApplied) {
            FilterChipItem(text = "No Cost EMI", onClick = { onToggle("No Cost EMI") }, isSelected = selectedFilter.equals("No Cost EMI", true))
        }
        if (isLowCostApplied) {
            FilterChipItem(text = "Low Cost EMI", onClick = { onToggle("Low Cost EMI") },isSelected = selectedFilter.equals("Low Cost EMI", true))
        }
    }
}

@Composable
private fun FilterChipItem(text : String, onClick: () -> Unit, isSelected : Boolean) {
    Row(
        modifier = Modifier
            .border(1.dp, if (isSelected) Color(0xFF1CA672) else Color(0xFFE6E6E6), RoundedCornerShape(20.dp))
            .background(if (isSelected) Color(0xFFE8F6F1) else Color.White, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2D2B32), fontFamily = LocalSDKFonts.current.primary)
        Image(
            painter   = painterResource(if (isSelected) Res.drawable.ic_tick_arrow else Res.drawable.add_icon),
            contentDescription = "",
            modifier      = Modifier.size(14.dp),
            colorFilter          = ColorFilter.tint(if (isSelected) Color(0xFF2D2B32) else Color(0xFF7F7D83)),
        )
    }
}



// ─── Others list ─────────────────────────────────────────────────────────────

@Composable
private fun OthersPaymentList(
    banks: List<Bank>,
    selectedValue: String?,
    onSelect: (String) -> Unit,
    onProceed: () -> Unit,
    ctaBorderRadius : Int,
    buttonColor: String,
    buttonTextColor : String
) {
    LazyColumn(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp)
            .background(
                color =  Color.White,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFFE6E6E6),
                RoundedCornerShape(12.dp)
            )
    ) {
        itemsIndexed(banks) { index, bank ->

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(bank.cardLessEmiValue) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedValue == bank.cardLessEmiValue,
                        onClick  = { onSelect(bank.cardLessEmiValue) },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(bank.name, fontSize = 14.sp, color = Color(0xFF2D2B32), fontFamily = LocalSDKFonts.current.primary)
                }

                if (selectedValue == bank.cardLessEmiValue) {
                    PayButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                            .clip(RoundedCornerShape(ctaBorderRadius.dp))
                            .background(
                                buttonColor.toComposeColor()
                            )
                            .clickable {
                                onProceed()
                            },
                        amount = 0.0,
                        currencySymbol = "",
                        isValid = true,
                        buttonTextColor = buttonTextColor,
                        text = "Pay"
                    )
                }

                if (index != banks.lastIndex) {
                    HorizontalDivider(color = Color(0xFFE6E6E6))
                }
            }
        }
    }
}

@Composable
fun BankCard(
    name: String,
    iconUrl: String,
    hasNoCostEmi: Boolean,
    hasLowCostEmi: Boolean,
    onPress: () -> Unit,
    percent : Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPress() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        // ── Bank icon ─────────────────────────────────────────────────────────
        KamelImage(
            resource              = asyncPainterResource(data = iconUrl),
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

        // ── Name + Tags ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        ) {
            Text(
                text       = name,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = LocalSDKFonts.current.primary,
                color      = Color(0xFF4F4D55),
            )
            if (hasNoCostEmi || hasLowCostEmi) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hasNoCostEmi)  EmiTag(label = "NO COST EMI")
                    if (hasLowCostEmi) EmiTag(label = "LOW COST EMI")
                }
            }
        }

        Text(
            text       = "${formatPercent(percent)}%p.a",
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = LocalSDKFonts.current.primary,
            color      = Color(0xFF4F4D55),
        )
        // ── Chevron ───────────────────────────────────────────────────────────
        ChevronIcon()
    }
}

// ─── EMI tag chip ─────────────────────────────────────────────────────────────

@Composable
private fun EmiTag(label: String) {
    Surface(
        shape  = RoundedCornerShape(6.dp),
        color  = Color(0xFFFFF0F6),
        border = BorderStroke(1.dp, Color(0xFFFFADD2)),
    ) {
        Text(
            text       = label,
            modifier   = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            fontSize   = 10.sp,
            fontFamily = LocalSDKFonts.current.primary,
            fontWeight = FontWeight.Medium,
            color      = Color(0xFFEB2F96),
        )
    }
}

object ScreenBackInterceptor {
    // set by whichever screen wants to consume back internally; returns true if consumed
    var onBack: (() -> Boolean)? = null
    var currentTitle: (() -> String)? = null
}