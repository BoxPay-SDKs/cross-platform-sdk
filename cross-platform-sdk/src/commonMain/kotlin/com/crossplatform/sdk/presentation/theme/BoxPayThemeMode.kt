package com.crossplatform.sdk.presentation.theme

/**
 * Controls which colour scheme the BoxPay SDK renders with.
 *
 * - [DEFAULT]  – always light, regardless of what the host app or device is using.
 * - [DARK]   – always dark, regardless of what the host app or device is using.
 * - [SYSTEM] – follows the device system theme (default).  Use this when the
 *              merchant app can already detect the system theme; it guarantees
 *              the SDK stays in sync with the rest of the app.
 *
 * Pass the value through [BoxPayCommonCheckout] (and the platform-specific
 * entry points [BoxPayActivity] / [BoxPayViewController]) so the SDK honours
 * the merchant's choice at launch time.
 */
enum class BoxPayThemeMode {
    DEFAULT,
    DARK,
    SYSTEM;

    companion object {
        /** Safe parsing – unknown strings fall back to [SYSTEM]. */
        fun fromString(value: String?): BoxPayThemeMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
    }
}
