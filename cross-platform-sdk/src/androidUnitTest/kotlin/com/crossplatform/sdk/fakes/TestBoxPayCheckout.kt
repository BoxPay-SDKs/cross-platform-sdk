package com.crossplatform.sdk.fakes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.presentation.navigation.AppNavHost
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.koin.compose.KoinApplication

/**
 * A test-only mirror of the real `BoxPayCommonCheckout` (see
 * `BoxPayCheckout.kt`) with exactly one difference: it wires
 * [testKoinModule] instead of the real `di/appModule`, so `AppNavHost` and
 * every screen underneath it resolve fakes instead of hitting the network.
 *
 * This exists because `BoxPayCommonCheckout` hardcodes
 * `KoinApplication(application = { modules(appModule) })` — there is no way
 * to override that from outside, and `KoinApplication` creates its own
 * composition-scoped Koin instance, so a global `startKoin { ... }` in a
 * test's `@Before` would *not* be picked up by the real composable anyway.
 *
 * Every other line is a straight copy of `BoxPayCommonCheckout`'s body:
 * the same `ProvideSDKFonts`, the same `CheckoutDetailsHandler.setCheckoutToken(...)`
 * call the real SDK entry point makes, the same `MaterialTheme { AppNavHost() }`.
 */
@Composable
internal fun TestBoxPayCheckout(
    token: String,
    isTestEnv: Boolean = true,
    shopperToken: String? = null,
    isSuccessScreenVisible: Boolean = true,
    isFailedScreenVisible: Boolean = true,
    showQROnLoad: Boolean = false,
    ctaBorderRadius: Int = 8,
    isSICheckBoxChecked: Boolean = false,
    isSICheckBoxEnabled: Boolean = false,
    focusedTextInputBorderColor: String = "#CCCCCC",
    unfocusedTextInputBorderColor: String = "#DDDDDD",
    fontFamily: String? = null,
    cardScreenRepo: FakeCardScreenRepo = FakeCardScreenRepo(),
    fetchStatusRepo: FakeFetchStatusRepo = FakeFetchStatusRepo(),
    analyticsRepo: FakeCallUIAnalyticsRepo = FakeCallUIAnalyticsRepo(),
    otherPaymentMethodRepo: FakeOtherPaymentMethodRepo = FakeOtherPaymentMethodRepo(),
    mainScreenRepo: FakeMainScreenRepo = FakeMainScreenRepo(),
    addressScreenRepo: FakeAddressScreenRepo = FakeAddressScreenRepo(),
    instantOfferRepo: FakeInstantOfferRepo = FakeInstantOfferRepo(),
) {
    KoinApplication(
        application = {
            modules(
                testKoinModule(
                    cardScreenRepo = cardScreenRepo,
                    fetchStatusRepo = fetchStatusRepo,
                    analyticsRepo = analyticsRepo,
                    otherPaymentMethodRepo = otherPaymentMethodRepo,
                    mainScreenRepo = mainScreenRepo,
                    addressScreenRepo = addressScreenRepo,
                    instantOfferRepo = instantOfferRepo,
                )
            )
        }
    ) {
        val backendFont by CheckoutDetailsHandler.fontFamilyFlow.collectAsStateWithLifecycle()

        ProvideSDKFonts(
            merchantFont = fontFamily,
            backendFont = backendFont,
            onUnknownFontRequested = {},
        ) {
            CheckoutDetailsHandler.setCheckoutToken(
                token = token,
                shopperToken = shopperToken,
                isTestEnv = isTestEnv,
                isSICheckboxEnabled = isSICheckBoxEnabled,
                isSICheckboxChecked = isSICheckBoxChecked,
                ctaBorderRadius = ctaBorderRadius,
                isSuccessScreenVisible = isSuccessScreenVisible,
                isFailedScreenVisible = isFailedScreenVisible,
                focusedTextInputBorderColor = focusedTextInputBorderColor,
                unfocusedTextInputBorderColor = unfocusedTextInputBorderColor,
                showQROnLoad = showQROnLoad,
            )

            MaterialTheme {
                AppNavHost()
            }
        }
    }
}
