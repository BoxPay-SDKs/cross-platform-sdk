package com.crossplatform.sdk.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.domain.model.OfferItem
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_offer_tag
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_savings
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_tick_arrow
import org.jetbrains.compose.resources.painterResource

@Composable
fun OfferSection(
    offers: List<OfferItem>,
    selectedCode: String,
    themeColor: Color,
    onApply: (offer: OfferItem) -> Unit,
    onRemove: () -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        offers.size == 1 -> SingleOfferCard(
            offer = offers.first(),
            isApplied = offers.first().code == selectedCode,
            themeColor = themeColor,
            onApply = { onApply(offers.first()) },
            onRemove = onRemove,
            modifier = modifier
        )

        else -> MultiOfferCard(
            offers = offers,
            selectedCode = selectedCode,
            themeColor = themeColor,
            onApply = onApply,
            onRemove = onRemove,
            onViewAll = onViewAll,
            modifier = modifier
        )
    }
}

// ─── Single offer ─────────────────────────────────────────────────────────────

@Composable
fun SingleOfferCard(
    offer: OfferItem,
    isApplied: Boolean,
    themeColor: Color,
    onApply: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appliedGreen = Color(0xFF1D9E75)
    val appliedGreenBg = Color(0xFFE1F5EE)
    val appliedGreenDark = Color(0xFF085041)
    val removeRed = Color(0xFFA32D2D)

    val bgColor = if (isApplied) appliedGreenBg else Color.White
    val borderColor = if (isApplied) appliedGreen else Color(0xFFE0E0E0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (isApplied) 1.5.dp else 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        // ── Main row ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon bubble
            if (isApplied) {
                Image(
                    painter = painterResource(Res.drawable.ic_tick_arrow),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(themeColor),
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Image(
                    painter = painterResource(Res.drawable.ic_offer_tag),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).graphicsLayer(scaleX = -1f),
                    colorFilter = ColorFilter.tint(themeColor)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isApplied) "${offer.code} applied!" else offer.code,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = LocalSDKFonts.current.primary,
                    color = if (isApplied) appliedGreenDark else Color(0xFF1A1A1A)
                )
                Text(
                    text = offer.description,
                    fontSize = 11.sp,
                    fontFamily = LocalSDKFonts.current.primary,
                    color = if (isApplied) Color(0xFF0F6E56) else Color(0xFF888888),
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = if (isApplied) "Remove" else "Apply",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = LocalSDKFonts.current.primary,
                color = if (isApplied) removeRed else themeColor,
                modifier = Modifier.clickable { if (isApplied) onRemove() else onApply() }
            )
        }

        // ── Savings banner (only when applied) ──
        AnimatedVisibility(
            visible = isApplied,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_savings),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "You saved",
                    fontSize = 11.sp,
                    fontFamily = LocalSDKFonts.current.primary,
                    color = appliedGreenDark
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "- ${offer.currencySymbol}${offer.discountAmount}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = LocalSDKFonts.current.primary,
                    color = appliedGreenDark
                )
            }
        }
    }
}

// ─── Multiple offers ──────────────────────────────────────────────────────────

private const val MAX_VISIBLE_CHIPS = 3

@Composable
fun MultiOfferCard(
    offers: List<OfferItem>,
    selectedCode: String,
    themeColor: Color,
    onApply: (offer: OfferItem) -> Unit,
    onRemove: () -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appliedGreen = Color(0xFF1D9E75)
    val appliedGreenBg = Color(0xFFE1F5EE)
    val appliedGreenDark = Color(0xFF085041)
    val appliedOffer = offers.firstOrNull { it.code == selectedCode }

    val visibleOffers = offers.take(MAX_VISIBLE_CHIPS)
    val overflowCount = (offers.size - MAX_VISIBLE_CHIPS).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
    ) {
        // ── Header row ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_offer_tag),
                contentDescription = null,
                modifier = Modifier.size(24.dp).graphicsLayer(scaleX = -1f),
                colorFilter = ColorFilter.tint(themeColor)
            )

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${offers.size} offers available",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = LocalSDKFonts.current.primary,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = if (appliedOffer != null) "1 applied" else "Tap to apply",
                    fontSize = 10.sp,
                    fontFamily = LocalSDKFonts.current.primary,
                    color = if (appliedOffer != null) appliedGreen else Color(0xFF888888)
                )
            }

            Text(
                text = "View all >",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = themeColor,
                fontFamily = LocalSDKFonts.current.primary,
                modifier = Modifier.clickable { onViewAll() }
            )
        }

        HorizontalDivider(color     = Color(0xFFECECED),modifier = Modifier.padding(horizontal = 8.dp))

        // ── Chips row ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            visibleOffers.forEach { offer ->
                val isSelected = offer.code == selectedCode
                OfferChip(
                    code = offer.code,
                    isSelected = isSelected,
                    onClick = {
                        if (isSelected) onRemove() else onApply(offer)
                    }
                )
            }

            if (overflowCount > 0) {
                MoreChip(count = overflowCount, onClick = onViewAll)
            }
        }

        // ── Applied summary bar ──
        AnimatedVisibility(
            visible = appliedOffer != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            appliedOffer?.let { offer ->
                HorizontalDivider(color = Color(0xFFECECED))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(appliedGreenBg)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(appliedGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.ic_tick_arrow),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(appliedGreenDark),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${offer.code} — ${offer.description}",
                        fontSize = 10.sp,
                        color = appliedGreenDark,
                        fontFamily = LocalSDKFonts.current.primary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = buildAnnotatedString {
                            append("- ${offer.currencySymbol}")
                            append("${offer.discountAmount}")
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = LocalSDKFonts.current.primary,
                        color = appliedGreenDark
                    )
                }
            }
        }
    }
}

// ─── Chip components ──────────────────────────────────────────────────────────

@Composable
private fun OfferChip(
    code: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) Color(0xFFE1F5EE) else Color(0xFFFFFFFF)
    val borderColor = if (isSelected) Color(0xFF1D9E75) else Color(0xFFE0E0E0)
    val textColor = if (isSelected) Color(0xFF085041) else Color(0xFF555555)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.dp else 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isSelected) {
            Image(
                painter = painterResource(Res.drawable.ic_tick_arrow),
                contentDescription = null,
                colorFilter = ColorFilter.tint(textColor),
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = code,
            fontSize = 12.sp,
            fontFamily = LocalSDKFonts.current.primary,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
private fun MoreChip(
    count: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF4F4F4))
            .border(0.5.dp, Color(0xFFCCCCCC), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = "+$count more",
            fontSize = 12.sp,
            fontFamily = LocalSDKFonts.current.primary,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF888888)
        )
    }
}