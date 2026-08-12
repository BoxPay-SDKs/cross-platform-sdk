package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.crossplatform.sdk.domain.model.MainScreenModel
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod
import com.crossplatform.sdk.domain.model.SurchargeModel
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_bnpl
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_card
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_emi
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_netbanking
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_wallet

@Composable
internal fun MorePaymentMethods(
    methodFlags: MainScreenModel.MethodFlags,
    onNavigateToCard: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToNetBanking: () -> Unit,
    onNavigateToEmi: () -> Unit,
    onNavigateToBNPL: () -> Unit,
    savedCardsList : List<SelectedPaymentMethod>,
    surchargeList : List<SurchargeModel>,
    currencySymbol : String,
    walletList : List<SelectedPaymentMethod>,
    netBankingList : List<SelectedPaymentMethod>,
    amount : Double,
    buttonColor : String,
    buttonTextColor : String,
    ctaBorderRadius : Int,
    onProceedForward : (instrumentType: String, instrumentValue: String, type: String) -> Unit
) {
    val selectedWalletId = remember {
        mutableStateOf("")
    }
    val selectedBankId = remember {
        mutableStateOf("")
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = Color(0xFFE6E6E6),
                RoundedCornerShape(12.dp)
            )
            .background(Color.White)
    ) {
        if(methodFlags.isCardsVisible && savedCardsList.isEmpty()) {
            MorePaymentContainer(
                title = "Cards",
                image = Res.drawable.ic_card,
                onClick = onNavigateToCard,
                surchargeFee = surchargeList.find { it.applicableOn.lowercase() == "card" }?.amount,
                currencySymbol = currencySymbol
            )
            if(methodFlags.isWalletVisible || methodFlags.isNetBankingVisible || methodFlags.isEMIVisible || methodFlags.isBNPLVisible) {
                HorizontalDivider()
            }
        }
        if(methodFlags.isWalletVisible) {
            ExpandablePaymentSection(
                title = "Wallet",
                image = Res.drawable.ic_wallet,
                onViewMore = onNavigateToWallet,
                surchargeFee = surchargeList.find { it.applicableOn.lowercase() == "wallet" }?.amount,
                currencySymbol = currencySymbol,
                providerList = walletList,
                amount = amount,
                selectedId = selectedWalletId.value,
                buttonTextColor = buttonTextColor,
                buttonColor = buttonColor,
                ctaBorderRadius = ctaBorderRadius,
                onClickRadio = {
                    selectedWalletId.value = it
                },
                onProceedForward = onProceedForward
            )
            if(methodFlags.isNetBankingVisible || methodFlags.isEMIVisible || methodFlags.isBNPLVisible) {
                HorizontalDivider()
            }
        }
        if(methodFlags.isNetBankingVisible) {
            ExpandablePaymentSection(
                title = "Bank Transfers",
                image = Res.drawable.ic_netbanking,
                onViewMore = onNavigateToNetBanking,
                surchargeFee = surchargeList.find { it.applicableOn.lowercase() == "netbanking" }?.amount,
                currencySymbol = currencySymbol,
                providerList = netBankingList,
                amount = amount,
                selectedId = selectedBankId.value,
                buttonTextColor = buttonTextColor,
                buttonColor = buttonColor,
                ctaBorderRadius = ctaBorderRadius,
                onClickRadio = {
                    selectedBankId.value = it
                },
                onProceedForward = onProceedForward
            )
            if(methodFlags.isEMIVisible || methodFlags.isBNPLVisible) {
                HorizontalDivider()
            }
        }
        if (methodFlags.isEMIVisible) {
            MorePaymentContainer(
                title = "EMI",
                image = Res.drawable.ic_emi,
                onClick = onNavigateToEmi,
                surchargeFee = surchargeList.find { it.applicableOn.lowercase() == "emi" }?.amount,
                currencySymbol = currencySymbol
            )
            if(methodFlags.isBNPLVisible) {
                HorizontalDivider()
            }
        }
        if(methodFlags.isBNPLVisible){
            MorePaymentContainer(
                title = "Buy Now Pay Later",
                image = Res.drawable.ic_bnpl,
                onClick = onNavigateToBNPL,
                surchargeFee = surchargeList.find { it.applicableOn.lowercase() == "buynowpaylater" }?.amount,
                currencySymbol = currencySymbol
            )
        }
    }
}