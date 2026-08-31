package com.crossplatform.sdk.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    onNavigateToPayNow : (instrumentType : String) -> Unit,
    savedCardsList : List<SelectedPaymentMethod>,
    surchargeList : List<SurchargeModel>,
    currencySymbol : String,
    walletList : List<SelectedPaymentMethod>,
    netBankingList : List<SelectedPaymentMethod>,
    bnplList : List<SelectedPaymentMethod>,
    amount : Double,
    buttonColor : String,
    buttonTextColor : String,
    ctaBorderRadius : Int,
    onProceedForward : (instrumentType: String, instrumentValue: String, type: String) -> Unit,
    setSelectedPaymentMethod: (String) -> Unit,
    setSelectedPaymentNetwork: (String) -> Unit
) {
    val isWalletVisible = remember {
        mutableStateOf(false)
    }
    val isNetBankingVisible = remember {
        mutableStateOf(false)
    }
    val isBNPLVisible = remember {
        mutableStateOf(false)
    }
    val walletRotation by animateFloatAsState(
        targetValue = if (isWalletVisible.value) 180f else 0f,
        label       = "chevron"
    )
    val bankRotation by animateFloatAsState(
        targetValue = if (isNetBankingVisible.value) 180f else 0f,
        label       = "chevron"
    )
    val bnplRotation by animateFloatAsState(
        targetValue = if (isNetBankingVisible.value) 180f else 0f,
        label       = "chevron"
    )
    val selectedWalletId = remember {
        mutableStateOf("")
    }
    val selectedBankId = remember {
        mutableStateOf("")
    }
    val selectedBnplId = remember {
        mutableStateOf("")
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if(methodFlags.isCardsVisible && savedCardsList.isEmpty()) {
            MorePaymentContainer(
                title = "Cards",
                image = Res.drawable.ic_card,
                onClick = onNavigateToCard,
                surchargeFee = surchargeList.find { it.applicableOn.lowercase() == "card" }?.amount,
                currencySymbol = currencySymbol
            )
        }
        if(methodFlags.isWalletVisible) {
            ExpandablePaymentSection(
                title = "Wallet",
                image = Res.drawable.ic_wallet,
                onViewMore = onNavigateToWallet,
                surchargeList = surchargeList,
                currencySymbol = currencySymbol,
                providerList = walletList,
                amount = amount,
                selectedId = selectedWalletId.value,
                buttonTextColor = buttonTextColor,
                buttonColor = buttonColor,
                ctaBorderRadius = ctaBorderRadius,
                onClickRadio = {id , name ->
                    setSelectedPaymentNetwork(name)
                    selectedWalletId.value = id
                },
                onProceedForward = onProceedForward,
                rotate = walletRotation,
                isExpanded = isWalletVisible.value,
                setIsExpanded = {
                    selectedBankId.value = ""
                    selectedBnplId.value = ""
                    isNetBankingVisible.value = false
                    isBNPLVisible.value = false
                    val newExpandedState = !isWalletVisible.value
                    isWalletVisible.value = newExpandedState
                    setSelectedPaymentMethod(if (newExpandedState) "wallet" else "")
                }
            )
        }
        if(methodFlags.isNetBankingVisible) {
            ExpandablePaymentSection(
                title = "Bank Transfers",
                image = Res.drawable.ic_netbanking,
                onViewMore = onNavigateToNetBanking,
                surchargeList = surchargeList,
                currencySymbol = currencySymbol,
                providerList = netBankingList,
                amount = amount,
                selectedId = selectedBankId.value,
                buttonTextColor = buttonTextColor,
                buttonColor = buttonColor,
                ctaBorderRadius = ctaBorderRadius,
                onClickRadio = {id , name ->
                    setSelectedPaymentNetwork(name)
                    selectedBankId.value = id
                },
                onProceedForward = onProceedForward,
                rotate = bankRotation,
                isExpanded = isNetBankingVisible.value,
                setIsExpanded = {
                    selectedWalletId.value = ""
                    selectedBnplId.value = ""
                    isWalletVisible.value = false
                    isBNPLVisible.value = false
                    val newExpandedState = !isNetBankingVisible.value
                    isNetBankingVisible.value = newExpandedState
                    setSelectedPaymentMethod(if (newExpandedState) "netbanking" else "")
                }
            )
        }
        if (methodFlags.isEMIVisible) {
            MorePaymentContainer(
                title = "EMI",
                image = Res.drawable.ic_emi,
                onClick = onNavigateToEmi,
                surchargeFee = surchargeList.find { it.applicableOn.lowercase() == "emi" }?.amount,
                currencySymbol = currencySymbol
            )
        }
        if(methodFlags.isBNPLVisible){
            ExpandablePaymentSection(
                title = "Buy Now Pay Later",
                image = Res.drawable.ic_bnpl,
                onViewMore = onNavigateToBNPL,
                surchargeList = surchargeList,
                currencySymbol = currencySymbol,
                providerList = bnplList,
                amount = amount,
                selectedId = selectedBnplId.value,
                buttonTextColor = buttonTextColor,
                buttonColor = buttonColor,
                ctaBorderRadius = ctaBorderRadius,
                onClickRadio = {id , name ->
                    setSelectedPaymentNetwork(name)
                    selectedBnplId.value = id
                },
                onProceedForward = onProceedForward,
                rotate = bnplRotation,
                isExpanded = isBNPLVisible.value,
                setIsExpanded = {
                    selectedWalletId.value = ""
                    selectedBankId.value = ""
                    isWalletVisible.value = false
                    isNetBankingVisible.value = false
                    val newExpandedState = !isBNPLVisible.value
                    isBNPLVisible.value = newExpandedState
                    setSelectedPaymentMethod(if (newExpandedState) "buynowpaylater" else "")
                }
            )
        }
        methodFlags.additionalPaymentMethods.map { method ->
            MorePaymentContainer(
                title = method.title,
                logoUrl = method.iconUrl,
                onClick = {
                    selectedBankId.value = ""
                    selectedBnplId.value = ""
                    selectedWalletId.value = ""
                    isNetBankingVisible.value = false
                    isBNPLVisible.value = false
                    isWalletVisible.value = false
                    setSelectedPaymentMethod("")
                    onNavigateToPayNow(method.instrumentTypeValue)
                },
                currencySymbol = currencySymbol
            )
        }
    }
}