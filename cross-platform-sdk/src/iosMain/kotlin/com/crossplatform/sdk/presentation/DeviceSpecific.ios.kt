package com.crossplatform.sdk.presentation

import androidx.compose.runtime.Composable
import com.crossplatform.sdk.data.model.BrowserData
import com.crossplatform.sdk.data.model.DeviceDetails
import com.crossplatform.sdk.domain.model.AppLifecycleState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.UIKit.UIScreen
import platform.UIKit.UIDevice
import platform.Foundation.NSTimeZone
import platform.Foundation.NSLocale
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.localTimeZone
import platform.Foundation.secondsFromGMT
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.UIKit.UIUserInterfaceIdiomPad
import kotlin.collections.component1
import kotlin.collections.component2
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.crossplatform.sdk.domain.handler.ExpressCheckoutConfig
import com.crossplatform.sdk.domain.handler.ExpressCheckoutPaymentHandler
import com.crossplatform.sdk.domain.handler.ExpressCheckoutPaymentRequest
import com.crossplatform.sdk.domain.handler.ExpressCheckoutPaymentResult
import com.crossplatform.sdk.payments.RevolutPayBridgeRegistry
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.NSDecimalNumber
import platform.Foundation.create
import platform.PassKit.PKMerchantCapability3DS
import platform.PassKit.PKPayment
import platform.PassKit.PKPaymentAuthorizationController
import platform.PassKit.PKPaymentAuthorizationControllerDelegateProtocol
import platform.PassKit.PKPaymentAuthorizationResult
import platform.PassKit.PKPaymentAuthorizationStatus
import platform.PassKit.PKPaymentNetworkAmex
import platform.PassKit.PKPaymentNetworkMasterCard
import platform.PassKit.PKPaymentNetworkVisa
import platform.PassKit.PKPaymentRequest
import platform.PassKit.PKPaymentSummaryItem
import platform.darwin.NSObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual fun getDeviceDetails(): DeviceDetails {
    return DeviceDetails(
        browser = "ios",
        platformVersion = UIDevice.currentDevice.systemVersion,
        deviceType = "Phone",
        deviceName = "iOS Device",
        deviceBrandName = "Apple"
    )
}

@OptIn(ExperimentalForeignApi::class)
actual fun getBrowserData(): BrowserData {
    return BrowserData(
        screenHeight = UIScreen.mainScreen.bounds.useContents {
            size.height.toInt().toString()
        },
        screenWidth = UIScreen.mainScreen.bounds.useContents {
            size.width.toInt().toString()
        },
        acceptHeader = "application/json",
        userAgentHeader = "KMP SDK",
        browserLanguage = NSLocale.currentLocale.languageCode ,
        ipAddress = "null",
        colorDepth = 24,
        javaEnabled = true,
        timeZoneOffSet = NSTimeZone.localTimeZone.secondsFromGMT.toInt() / 60,
        packageId = "com.boxpay.checkout.sdk"
    )
}

actual fun getInstalledUpiApps(context: Any?): List<String> {
    // friendly name -> candidate schemes; any hit means installed
    val knownUpiSchemes: Map<String, List<String>> = mapOf(
        "gpay"       to listOf("tez://", "gpay://"),
        "phonepe"    to listOf("phonepe://"),
        "paytm"      to listOf("paytmmp://", "paytm://"),
        "bhim"       to listOf("bhim://"),
        "amazon_pay" to listOf("amazonpay://"),
        "mobikwik"   to listOf("mobikwik://"),
        "bharatpe"   to listOf("postpe://"),
        "jupiter"    to listOf("jupiter://"),
        "pop"        to listOf("popclubapp://"),
    )

    return knownUpiSchemes.filter { (_, schemes) ->
        schemes.any { scheme ->
            val nsUrl = NSURL.URLWithString(scheme) ?: return@any false
            UIApplication.sharedApplication.canOpenURL(nsUrl)
        }
    }.keys.toList()
}

@Composable
actual fun getPlatformContext(): Any? {
    return null
}

actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}

actual fun currentYear(): Int {
    val components = NSCalendar.currentCalendar.components(
        NSCalendarUnitYear, NSDate()
    )
    return (components.year % 100).toInt()
}

actual fun currentMonth(): Int {
    val components = NSCalendar.currentCalendar.components(
        NSCalendarUnitMonth, NSDate()
    )
    return components.month.toInt()
}

actual fun launchUpiIntent(url: String, onFailure: (Throwable) -> Unit, onSuccess: () -> Unit) {
    val nsUrl = NSURL.URLWithString(url)
        ?: run {
            onFailure(IllegalArgumentException("Could not create NSURL from: $url"))
            return
        }

    val app = UIApplication.sharedApplication

    // canOpenURL requires the scheme declared in LSApplicationQueriesSchemes
    if (!app.canOpenURL(nsUrl)) {
        onFailure(IllegalStateException("No UPI app found to handle URL: $url"))
        return
    }

    // iOS 10+ async API
    app.openURL(
        url       = nsUrl,
        options   = emptyMap<Any?, Any>(),
        completionHandler = { success ->
            if (!success) {
                onFailure(IllegalStateException("openURL returned false for: $url"))
            } else {
                onSuccess()
            }
        }
    )
}

actual class AppLifecycleObserver actual constructor(private val onStateChange: (AppLifecycleState) -> Unit) {
    private val center = NSNotificationCenter.defaultCenter
    private val tokens = mutableListOf<Any>()

    actual fun start() {
        tokens += center.addObserverForName(
            name   = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue  = NSOperationQueue.mainQueue,
            usingBlock = { _ -> onStateChange(AppLifecycleState.Foreground) }
        )
        tokens += center.addObserverForName(
            name   = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue  = NSOperationQueue.mainQueue,
            usingBlock = { _ -> onStateChange(AppLifecycleState.Background) }
        )
        tokens += center.addObserverForName(
            name   = UIApplicationWillResignActiveNotification,
            `object` = null,
            queue  = NSOperationQueue.mainQueue,
            usingBlock = { _ -> onStateChange(AppLifecycleState.Inactive) }
        )
    }

    actual fun stop() {
        tokens.forEach { center.removeObserver(it) }
        tokens.clear()
    }
}

@Composable
actual fun BackHandler(onBack: () -> Unit) {
    // No-op on iOS — back is handled via UI button only
}

actual fun isTabletDevice(): Boolean {
    return UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad
}

@OptIn(ExperimentalEncodingApi::class, ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun base64ToImageBitmap(base64: String): ImageBitmap {
    // Strip data URI prefix if present — same as Android's substringAfter("base64,")
    val cleanBase64 = base64.substringAfter("base64,", base64)

    // Decode base64 → raw bytes — same kotlin stdlib Base64 as Android
    val bytes = Base64.decode(cleanBase64)

    // Feed raw bytes into Skia (the rendering engine behind CMP on iOS)
    // Skia handles PNG / JPEG / WebP automatically — same formats Android's BitmapFactory supports
    val image = bytes.usePinned { pinned ->
        // NSData wraps the raw bytes without copying — efficient
        val nsData = NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        // Skia decodes from the raw encoded bytes directly
        Image.makeFromEncoded(
            bytes = ByteArray(nsData.length.toInt()).also { out ->
                out.usePinned { outPinned ->
                    platform.posix.memcpy(outPinned.addressOf(0), nsData.bytes, nsData.length)
                }
            }
        )
    }

    return image.toComposeImageBitmap()
}

// iosMain
@Composable
actual fun rememberExpressCheckoutPaymentHandler(): ExpressCheckoutPaymentHandler {
    return IosPaymentHandler()
}

class IosPaymentHandler() : ExpressCheckoutPaymentHandler {
    private var activeDelegate: PaymentDelegate? = null

    override fun isApplePayAvailable() =
        PKPaymentAuthorizationController.canMakePayments()

    override fun isGooglePayAvailable() = false

    override fun isRevolutPayAvailable(): Boolean = true

    override fun launchApplePay(
        request: ExpressCheckoutPaymentRequest,
        config: ExpressCheckoutConfig,
        onResult: (ExpressCheckoutPaymentResult) -> Unit
    ) {
        val pkRequest = PKPaymentRequest().apply {
            merchantIdentifier = config.applePayMerchantIdentifier
            supportedNetworks =
                listOf(PKPaymentNetworkVisa, PKPaymentNetworkMasterCard, PKPaymentNetworkAmex)
            merchantCapabilities = PKMerchantCapability3DS
            countryCode = request.countryCode
            currencyCode = request.currencyCode
            paymentSummaryItems = listOf(
                PKPaymentSummaryItem.summaryItemWithLabel(
                    label = request.merchantName,
                    amount = NSDecimalNumber(string = request.amount)
                )
            )
        }

        val controller = PKPaymentAuthorizationController(pkRequest)
        val delegate = PaymentDelegate(
            onResult = { result ->
                activeDelegate = null
                onResult(result)
            }
        )
        activeDelegate = delegate
        controller.delegate = delegate
        controller.presentWithCompletion { success ->
            if (!success) {
                activeDelegate = null
                onResult(ExpressCheckoutPaymentResult.Failure("Could not present Apple Pay sheet"))
            }
        }
    }

    override fun launchGooglePay(
        request: ExpressCheckoutPaymentRequest,
        config: ExpressCheckoutConfig,
        onResult: (ExpressCheckoutPaymentResult) -> Unit
    ) = onResult(ExpressCheckoutPaymentResult.Failure("Google Pay unavailable on iOS"))

    override fun launchRevolutPay(
        request: ExpressCheckoutPaymentRequest,
        config: ExpressCheckoutConfig,
        merchantPublicKey: String,
        isSandbox: Boolean,
        onResult: (ExpressCheckoutPaymentResult) -> Unit
    ) {
        val executor = RevolutPayBridgeRegistry.executor
            ?: return onResult(ExpressCheckoutPaymentResult.Failure("RevolutPayExecutor not registered"))

        executor.launch(
            orderToken = request.orderToken ?: "",
            merchantPublicKey = merchantPublicKey,
            isSandbox = isSandbox
        ) { success, error ->
            onResult(
                if (success) ExpressCheckoutPaymentResult.Success
                else ExpressCheckoutPaymentResult.Failure(error ?: "Unknown error")
            )
        }
    }
}
private class PaymentDelegate(
    private val onResult: (ExpressCheckoutPaymentResult) -> Unit
) : NSObject(), PKPaymentAuthorizationControllerDelegateProtocol {

    override fun paymentAuthorizationController(
        controller: PKPaymentAuthorizationController,
        didAuthorizePayment: PKPayment,
        handler: (PKPaymentAuthorizationResult?) -> Unit
    ) {
        onResult(ExpressCheckoutPaymentResult.Success)
        handler(
            PKPaymentAuthorizationResult(
                status = PKPaymentAuthorizationStatus.PKPaymentAuthorizationStatusSuccess,
                errors = null
            )
        )
    }

    override fun paymentAuthorizationControllerDidFinish(controller: PKPaymentAuthorizationController) {
        controller.dismissWithCompletion { }
    }
}
