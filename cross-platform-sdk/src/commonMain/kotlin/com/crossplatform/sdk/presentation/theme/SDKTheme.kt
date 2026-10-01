package com.crossplatform.sdk.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ─── Color tokens ────────────────────────────────────────────────────────────

/**
 * Semantic colour tokens consumed by every composable in the SDK.
 * Components read colours via [LocalSDKColors] rather than hardcoding values,
 * so switching between light and dark mode requires only swapping this object.
 */
data class SDKThemeColors(
    /** Page / screen background */
    val background: Color,
    /** Card / sheet / bottom-bar surface */
    val surface: Color,
    /** Secondary surface (e.g. shimmer base, light chips) */
    val surfaceVariant: Color,
    /** Primary text on [background] */
    val textPrimary: Color,
    /** Secondary / subdued text */
    val textSecondary: Color,
    /** Thin divider lines */
    val divider: Color,
    /** Shimmer highlight colour */
    val shimmerHighlight: Color,
    /** Arrow / icon tint on top-bar */
    val iconTint: Color,
    /** True when the active scheme is dark */
    val isDark: Boolean,
)

// ─── Light palette ────────────────────────────────────────────────────────────

val SDKLightColors = SDKThemeColors(
    background      = Color(0xFFFFFFFF),
    surface         = Color(0xFFF7F7F8),
    surfaceVariant  = Color(0xFFEEEEEF),
    textPrimary     = Color(0xFF363840),
    textSecondary   = Color(0xFF4F4D55),
    divider         = Color(0xFFE0E0E0),
    shimmerHighlight = Color(0xFFE8E8E8),
    iconTint        = Color(0xFF363840),
    isDark          = false,
)

// ─── Dark palette ─────────────────────────────────────────────────────────────

val SDKDarkColors = SDKThemeColors(
    background      = Color(0xFF121212),
    surface         = Color(0xFF1E1E1E),
    surfaceVariant  = Color(0xFF2A2A2A),
    textPrimary     = Color(0xFFE3E3E3),
    textSecondary   = Color(0xFF9E9E9E),
    divider         = Color(0xFF3A3A3A),
    shimmerHighlight = Color(0xFF2E2E2E),
    iconTint        = Color(0xFFE3E3E3),
    isDark          = true,
)

// ─── CompositionLocal ─────────────────────────────────────────────────────────

/**
 * Provides [SDKThemeColors] to the entire SDK composable tree.
 * Always use [LocalSDKColors].current instead of hardcoded colours.
 */
val LocalSDKColors = staticCompositionLocalOf<SDKThemeColors> { SDKLightColors }

// ─── Material3 colour schemes (thin bridge to Material internals) ─────────────

private val sdkLightColorScheme = lightColorScheme(
    background      = SDKLightColors.background,
    surface         = SDKLightColors.surface,
    onBackground    = SDKLightColors.textPrimary,
    onSurface       = SDKLightColors.textPrimary,
)

private val sdkDarkColorScheme = darkColorScheme(
    background      = SDKDarkColors.background,
    surface         = SDKDarkColors.surface,
    onBackground    = SDKDarkColors.textPrimary,
    onSurface       = SDKDarkColors.textPrimary,
)

// ─── SDKThemeProvider ─────────────────────────────────────────────────────────

/**
 * Wraps [content] with the resolved [SDKThemeColors] and a matching
 * Material3 [MaterialTheme] so that all SDK components (and any downstream
 * Material3 components) use the correct colour scheme.
 *
 * Resolution order:
 * 1. [BoxPayThemeMode.DEFAULT] → always [SDKLightColors]
 * 2. [BoxPayThemeMode.DARK]  → always [SDKDarkColors]
 * 3. [BoxPayThemeMode.SYSTEM] → follows [isSystemInDarkTheme]
 */
@Composable
internal fun SDKThemeProvider(
    themeMode: BoxPayThemeMode,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        BoxPayThemeMode.DEFAULT  -> false
        BoxPayThemeMode.DARK   -> true
        BoxPayThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colors       = if (useDark) SDKDarkColors       else SDKLightColors
    val colorScheme  = if (useDark) sdkDarkColorScheme  else sdkLightColorScheme

    CompositionLocalProvider(LocalSDKColors provides colors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
