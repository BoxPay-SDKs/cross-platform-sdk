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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.crossplatform.sdk.data.model.CheckoutDetails
import com.crossplatform.sdk.domain.model.Bank
import com.crossplatform.sdk.domain.model.ChooseEmiModel
import com.crossplatform.sdk.domain.model.EmiCardGroup
import com.crossplatform.sdk.presentation.BackHandler
import com.crossplatform.sdk.presentation.ChevronIcon
import com.crossplatform.sdk.presentation.SectionTitle
import com.crossplatform.sdk.presentation.UiState
import com.crossplatform.sdk.presentation.components.EmptyListView
import com.crossplatform.sdk.presentation.components.ShimmerView
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import com.crossplatform.sdk.presentation.toComposeColor
import com.crossplatform.sdk.presentation.viewmodel.EMIScreenViewModel
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
    onBackPress : () -> Unit
) {
    BackHandler(onBack = onBackPress)
    val viewModel : EMIScreenViewModel = koinViewModel()
    val checkoutDetails by CheckoutDetailsHandler.checkoutDetailsFlow.collectAsStateWithLifecycle()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when(uiState) {
        is UiState.Error -> {
            val message = (uiState as UiState.Error).message
            Text("Welcome to error screen $message")
            viewModel.callUiAnalytics(
                event = AnalyticsEvents.SDK_CRASH.value,
                screenName = "BNPLScreen",
                message = "BNPLScreen not loaded $message"
            )
        }
        UiState.Loading -> {
            ShimmerView()
        }
        is UiState.Success -> {
            Box(modifier = Modifier.fillMaxSize()) {
                EmiContentScreen(
                    viewModel  = viewModel,
                    emiModel = (uiState as UiState.Success).data,
                    checkoutDetails = checkoutDetails
                )
            }
        }
    }
}
// ─── Main content (bank list) ─────────────────────────────────────────────────

@Composable
private fun EmiContentScreen(
    viewModel : EMIScreenViewModel,
    emiModel : ChooseEmiModel,
    checkoutDetails: CheckoutDetails
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
                selected   = viewModel.selectedCard.value,
                brandColor = checkoutDetails.buttonColor.toComposeColor(),
                onSelect   = { viewModel.onClickCard(it) },
            )

            HorizontalDivider(color = Color(0xFFE6E6E6))

            // ── Search bar ────────────────────────────────────────────────────────
            EmiSearchBar(
                value       = viewModel.searchText.value,
                placeholder = if (viewModel.selectedCard.value == "Others") "Search for other EMI options" else "Search for bank",
                onValueChange = { viewModel.onEditSearchText(it) },
                focusedBorderColor = checkoutDetails.focusedTextInputBorderColor.toComposeColor(),
                unFocusedBorderColor = checkoutDetails.unfocusedTextInputBorderColor.toComposeColor()
            )

            // ── Filter chips ──────────────────────────────────────────────────────
            if ((isNoCostExisted ||  isLowCostExisted) && !viewModel.selectedCard.value.equals("others", true)) {
                EmiFilterRow(
                    isNoCostExisted,
                    isLowCostExisted,
                    onToggle   = { label -> viewModel.onToggleFilter(label) },
                    selectedFilter = viewModel.selectedFilter.value
                )
            }
        }

        // ── Bank list ─────────────────────────────────────────────────────────
        val cardData = emiModel.cards.find { it.cardType == viewModel.selectedCard.value }
        SectionTitle(if (viewModel.selectedCard.value.equals("others", true)) "Others" else "All Banks")

        when {
            cardData == null || cardData.banks.isEmpty() -> EmptyListView(
                heading = "Oops!! No results found",
                subHeading = "Please try another search"
            )

            viewModel.selectedCard.value.equals("others", true) -> OthersPaymentList(
                banks          = cardData.banks,
                selectedValue  = viewModel.selectedOthers.value,
                onSelect       = { viewModel.onSelectedOthersOption(it) },
                onProceed      = { viewModel.onProceedWithOther() },
            )

            else -> BankLazyList(
                banks    = cardData.banks,
                onSelect = { viewModel.onClickBank(it) },
            )
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
                    fontFamily = defaultFontFamily,
                    fontSize   = 14.sp,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        placeholder   = { Text(placeholder, color = Color(0xFFADACB0), fontSize = 14.sp, fontFamily = defaultFontFamily) },
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
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2D2B32), fontFamily = defaultFontFamily)
        Image(
            painter   = painterResource(if (isSelected) Res.drawable.ic_tick_arrow else Res.drawable.add_icon),
            contentDescription = "",
            modifier      = Modifier.size(14.dp),
            colorFilter          = ColorFilter.tint(if (isSelected) Color(0xFF2D2B32) else Color(0xFF7F7D83)),
        )
    }
}


// ─── Bank list ────────────────────────────────────────────────────────────────

@Composable
private fun BankLazyList(
    banks: List<Bank>,
    onSelect: (Bank) -> Unit,
) {
    LazyColumn {
        itemsIndexed(banks) { index, bank ->
            BankCard(
                name          = bank.name,
                iconUrl       = bank.iconUrl,
                hasNoCostEmi  = bank.noCostApplied,
                hasLowCostEmi = bank.lowCostApplied,
                onPress       = { onSelect(bank) },
            )
            if (index < banks.lastIndex) {
                HorizontalDivider(color = Color(0xFFE6E6E6))
            }
        }
    }
}


// ─── Others list ─────────────────────────────────────────────────────────────

@Composable
private fun OthersPaymentList(
    banks: List<Bank>,
    selectedValue: String,
    onSelect: (String) -> Unit,
    onProceed: () -> Unit,
) {
    LazyColumn {
        itemsIndexed(banks) { _, bank ->

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(bank.cardLessEmiValue) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selectedValue == bank.cardLessEmiValue,
                    onClick  = { onSelect(bank.cardLessEmiValue) },
                )
                Spacer(Modifier.width(8.dp))
                Text(bank.name, fontSize = 14.sp, color = Color(0xFF2D2B32))
            }
        }
        if (selectedValue.isNotBlank()) {
            item {
                Button(
                    onClick   = { onProceed() },
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape     = RoundedCornerShape(8.dp),
                ) {
                    Text("Proceed", color = Color.White, fontWeight = FontWeight.SemiBold)
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
            fontWeight = FontWeight.Medium,
            color      = Color(0xFFEB2F96),
        )
    }
}