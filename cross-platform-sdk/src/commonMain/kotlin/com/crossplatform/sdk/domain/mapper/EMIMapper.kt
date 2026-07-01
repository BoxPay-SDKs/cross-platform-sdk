package com.crossplatform.sdk.domain.mapper

import com.crossplatform.sdk.data.model.PaymentMethod
import com.crossplatform.sdk.domain.model.Bank
import com.crossplatform.sdk.domain.model.ChooseEmiModel
import com.crossplatform.sdk.domain.model.Emi
import com.crossplatform.sdk.domain.model.EmiCardGroup

fun List<PaymentMethod>.toUiModel(): ChooseEmiModel {
    val cardOrder = mapOf("Credit Card" to 0, "Debit Card" to 1, "Others" to 2)
    val groupedCards = mutableMapOf<String, MutableList<Bank>>()

    forEach { paymentMethod ->
        if (paymentMethod.type != "Emi") return@forEach
        val emiMethod = paymentMethod.emiMethod ?: return@forEach

        // ── Resolve card type ─────────────────────────────────────────────
        val cardType = when {
            paymentMethod.title.contains("Credit", ignoreCase = true) -> "Credit Card"
            paymentMethod.title.contains("Debit", ignoreCase = true)  -> "Debit Card"
            else                                                        -> "Others"
        }

        // ── Resolve logo URL ──────────────────────────────────────────────
        val logoUrl = paymentMethod.logoUrl.let {
            if (it.startsWith("/assets")) "https://checkout.boxpay.in$it" else it
        }

        // ── Resolve offer flags ───────────────────────────────────────────
        val discountType   = paymentMethod.applicableOffer?.firstOrNull()?.discount?.type
        val isNoCostOffer  = discountType == "NoCostEmi"
        val isLowCostOffer = discountType == "LowCostEmi"

        // ── Resolve interest percent ──────────────────────────────────────
        val effectiveRate = emiMethod.effectiveInterestRate ?: 0.0
        val bankRate      = if (cardType == "Others") 0.0 else effectiveRate
        val percent       = if (isNoCostOffer) (emiMethod.merchantBorneInterestRate ?: 0.0) else bankRate

        // ── Resolve bank name ─────────────────────────────────────────────
        val bankName = if (cardType == "Others") {
            emiMethod.cardlessEmiProviderTitle ?: ""
        } else {
            emiMethod.issuerTitle ?: ""
        }

        // ── Resolve interest charged ──────────────────────────────────────
        val interestCharged = if (isLowCostOffer) {
            emiMethod.interestChargedAmountLocaleFull ?: ""
        } else {
            emiMethod.bankChargedInterestAmountLocaleFull ?: ""
        }

        // ── Build EMI ─────────────────────────────────────────────────────
        val emi = Emi(
            duration = emiMethod.duration ?: 0,
            percent = percent,
            amount = emiMethod.emiAmountLocaleFull ?: "",
            totalAmount = emiMethod.totalAmountLocaleFull ?: "",
            discount = emiMethod.merchantBorneInterestAmountLocaleFull ?: "",
            interestCharged = interestCharged,
            noCostApplied = isNoCostOffer,
            lowCostApplied = isLowCostOffer,
            processingFee = emiMethod.processingFee?.amountLocaleFull ?: "0",
            code = paymentMethod.applicableOffer?.firstOrNull()?.code ?: "",
            netAmount = emiMethod.netAmountLocaleFull ?: "",
        )

        // ── Build Bank ────────────────────────────────────────────────────
        val bank = Bank(
            iconUrl          = logoUrl,
            name             = bankName,
            percent          = percent,
            noCostApplied    = isNoCostOffer,
            lowCostApplied   = isLowCostOffer,
            emiList          = emptyList(),
            cardLessEmiValue = emiMethod.cardlessEmiProviderValue ?: "",
            issuerBrand      = if (cardType == "Others") "" else (emiMethod.issuer ?: ""),
        )

        // ── Aggregate into grouped map ────────────────────────────────────
        val banksForCard = groupedCards.getOrPut(cardType) { mutableListOf() }
        val existingBankIndex = banksForCard.indexOfFirst {
            it.name == bank.name && it.iconUrl == bank.iconUrl
        }

        if (existingBankIndex >= 0) {
            val existing = banksForCard[existingBankIndex]
            val emiAlreadyExists = existing.emiList.any {
                it.duration == emi.duration && it.amount == emi.amount
            }
            if (!emiAlreadyExists) {
                banksForCard[existingBankIndex] = existing.copy(
                    emiList        = existing.emiList + emi,
                    noCostApplied  = existing.noCostApplied || isNoCostOffer,
                    lowCostApplied = existing.lowCostApplied || isLowCostOffer,
                    percent        = minOf(existing.percent, percent),
                )
            }
        } else {
            banksForCard += bank.copy(emiList = listOf(emi))
        }
    }

    // ── Sort and build final model ────────────────────────────────────────────
    val sortedCards = groupedCards
        .map { (cardType, banks) ->
            EmiCardGroup(
                cardType = cardType,
                banks = banks
                    .sortedWith(
                        compareByDescending<Bank> { it.noCostApplied }
                            .thenByDescending { it.lowCostApplied }
                            .thenBy { it.percent }
                    )
                    .map { bank ->
                        bank.copy(
                            emiList = bank.emiList.sortedWith(
                                compareByDescending<Emi> { it.noCostApplied }
                                    .thenByDescending { it.lowCostApplied }
                                    .thenBy { it.duration }
                            )
                        )
                    }
            )
        }
        .sortedBy { cardOrder[it.cardType] ?: 3 }

    return ChooseEmiModel(cards = sortedCards)
}