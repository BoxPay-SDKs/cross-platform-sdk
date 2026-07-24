package com.crossplatform.sdk.presentation

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import com.crossplatform.sdk.data.model.BrowserData
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.crossplatform.sdk.data.model.DeviceDetails
import java.util.TimeZone
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.crossplatform.sdk.data.model.GooglePayExpressCheckoutResponse
import com.crossplatform.sdk.domain.handler.ApplePayExpressCheckoutConfig
import com.crossplatform.sdk.domain.handler.ExpressCheckoutPaymentHandler
import com.crossplatform.sdk.domain.handler.ExpressCheckoutPaymentRequest
import com.crossplatform.sdk.domain.handler.ExpressCheckoutPaymentResult
import com.crossplatform.sdk.domain.handler.GooglePayExpressCheckoutConfig
import com.crossplatform.sdk.domain.handler.RevolutPayExpressCheckoutConfig
import com.crossplatform.sdk.domain.model.AppLifecycleState
import com.crossplatform.sdk.payments.RevolutPaySDK
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.contract.TaskResultContracts
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


lateinit var appContext: Context
actual fun getDeviceDetails(): DeviceDetails {
    return DeviceDetails(
        browser = "android",
        platformVersion = Build.VERSION.SDK_INT.toString(),
        deviceType = "Phone",
        deviceName = "Android Device",
        deviceBrandName = Build.BRAND
    )
}

actual fun getBrowserData(): BrowserData {
    return BrowserData(
        screenHeight = Resources.getSystem().displayMetrics.heightPixels.toString(),
        screenWidth = Resources.getSystem().displayMetrics.widthPixels.toString(),
        acceptHeader = "application/json",
        userAgentHeader = "KMP SDK",
        browserLanguage = "en_US",
        ipAddress = "null",
        colorDepth = 24,
        javaEnabled = true,
        timeZoneOffSet = TimeZone.getDefault().rawOffset / 60000,
        packageId = "com.boxpay.checkout.sdk"
    )
}

actual fun getInstalledUpiApps(context: Any?): List<String> {
    try {
        val knownUpiPackages: Map<String, String> = mapOf(
            "gpay"       to "com.google.android.apps.nbu.paisa.user",
            "paytm"      to "net.one97.paytm",
            "phonepe"    to "com.phonepe.app",
            "bhim"       to "in.org.npci.upiapp",
            "amazon_pay" to "in.amazon.mShop.android.shopping",
            "mobikwik"   to "com.mobikwik_new",
            "bharatpe"   to "com.postpe.app",     // consumer app
            "jupiter"    to "money.jupiter",
            "pop"        to "com.popclub.android",
        )
        val pm = (context as Context).packageManager

        // Step 1: Intent discovery — use upi://pay so host-specific filters are matched too
        val upiIntent = Intent(Intent.ACTION_VIEW, "upi://pay".toUri())
        val intentDiscovered: Set<String> = pm
            .queryIntentActivities(upiIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }
            .toSet()

        // Step 2: Explicit check — needs each package declared in <queries> or it throws
        val explicitlyFound: Set<String> = knownUpiPackages.values
            .filter { packageName ->
                try {
                    pm.getPackageInfo(packageName, 0)   // 0 is enough; GET_ACTIVITIES not needed
                    true
                } catch (_: PackageManager.NameNotFoundException) {
                    false
                }
            }
            .toSet()

        // Step 3: Merge and map back to friendly names
        val allFound = intentDiscovered + explicitlyFound
        val knownPackageToKey = knownUpiPackages.entries.associate { it.value to it.key }
        return allFound.map { knownPackageToKey[it] ?: it }
    } catch (_: Exception) {
        return emptyList()
    }
}

@Composable
actual fun getPlatformContext(): Any? {
    appContext = LocalContext.current
    return  LocalContext.current
}

actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}

actual fun currentYear(): Int {
    return Calendar.getInstance().get(Calendar.YEAR) % 100
}

actual fun currentMonth(): Int {
    return Calendar.getInstance().get(Calendar.MONTH) + 1
}

actual fun launchUpiIntent(url: String, onFailure: (Throwable) -> Unit, onSuccess: () -> Unit) {
    try {
        val uri    = url.toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // resolveActivity is the safe way to check before startActivity
        val canHandle = appContext.packageManager
            .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null

        if (canHandle) {
            appContext.startActivity(intent)
            onSuccess()
        } else {
            onFailure(IllegalStateException("No UPI app found to handle intent: $url"))
        }
    } catch (e: Exception) {
        onFailure(e)
    }
}

actual class AppLifecycleObserver actual constructor(onStateChange: (AppLifecycleState) -> Unit) {
    private val observer = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            onStateChange(AppLifecycleState.Foreground)
        }
        override fun onStop(owner: LifecycleOwner) {
            onStateChange(AppLifecycleState.Background)
        }
    }

    actual fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
    }

    actual fun stop() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
    }
}

@Composable
actual fun BackHandler(onBack: () -> Unit) {
    BackHandler {
        onBack()
    }
}

actual fun isTabletDevice(): Boolean {
    val smallestWidthDp = Resources.getSystem().configuration.smallestScreenWidthDp
    return smallestWidthDp >= 600
}

@OptIn(ExperimentalEncodingApi::class)
actual fun base64ToImageBitmap(base64: String): ImageBitmap {
    val cleanBase64 = base64.substringAfter("base64,", base64)

    val bytes = Base64.decode(cleanBase64)

    val bitmap = BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size
    ) ?: error("Unable to decode image")

    return bitmap.asImageBitmap()
}

// androidMain
@Composable
actual fun rememberExpressCheckoutPaymentHandler(): ExpressCheckoutPaymentHandler {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val client =  Wallet.getPaymentsClient(
        context,
        Wallet.WalletOptions.Builder()
            .setEnvironment(3)
            .build()
    )

    var pendingCallback by remember { mutableStateOf<((ExpressCheckoutPaymentResult) -> Unit)?>(null) }

    // safe here — rememberLauncherForActivityResult registers during initial
    // composition, before the activity can reach STARTED, unlike calling
    // activity.registerForActivityResult() manually from a class constructor
    val launcher = rememberLauncherForActivityResult(
        TaskResultContracts.GetPaymentDataResult()
    ) { taskResult ->
        val callback = pendingCallback
        when (taskResult.status.statusCode) {
            CommonStatusCodes.SUCCESS -> {
                if(taskResult.result != null) {
                    val json = taskResult.result!!.toJson()
                    val paymentData = Json.decodeFromString<GooglePayExpressCheckoutResponse>(json)
                    val token = paymentData.paymentMethodData.tokenizationData.token
                    callback?.invoke(ExpressCheckoutPaymentResult.Success(token))
                } else {
                    callback?.invoke(ExpressCheckoutPaymentResult.Success())
                }
            }
            CommonStatusCodes.CANCELED -> {
                callback?.invoke(ExpressCheckoutPaymentResult.Cancelled)
            }
            CommonStatusCodes.DEVELOPER_ERROR -> {
                callback?.invoke(ExpressCheckoutPaymentResult.Failure(taskResult?.result?.toString() ?: ""))
            }
            CommonStatusCodes.INTERNAL_ERROR -> {
                callback?.invoke(ExpressCheckoutPaymentResult.Failure(taskResult?.result?.toString() ?: ""))
            }
            else -> {
                callback?.invoke(ExpressCheckoutPaymentResult.Failure(taskResult?.result?.toString() ?: ""))
            }
        }
    }

    return remember(client, launcher, activity) {
        AndroidPaymentHandler(
            activity = activity,
            client = client,
            launcher = launcher,
            onLaunch = {
                pendingCallback = it
            }
        )
    }
}

private fun Context.findActivity(): ComponentActivity {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    throw IllegalStateException("ExpressCheckout requires a ComponentActivity host")
}

// androidMain — Google Pay via Wallet's PaymentsClient, Revolut Pay via
// RevolutPaySdk (registered explicitly by the merchant in their own
// Activity.onCreate -- see RevolutPaySdk.kt -- NOT created here)
class AndroidPaymentHandler(
    private val activity: ComponentActivity,
    private val client: PaymentsClient,
    private val launcher: ActivityResultLauncher<Task<PaymentData>>,
    private val onLaunch: ((ExpressCheckoutPaymentResult) -> Unit) -> Unit
) : ExpressCheckoutPaymentHandler {

    override suspend fun isGooglePayAvailable(config: GooglePayExpressCheckoutConfig,): Boolean {
        val request = IsReadyToPayRequest.fromJson(buildGooglePayConfigJson(config))
        return client.isReadyToPay(request).await()
    }

    override fun isApplePayAvailable(): Boolean = false

    // false if the merchant forgot to call RevolutPaySdk.register(this) in
    // their Activity.onCreate -- hides the button rather than showing one
    // that fails when tapped.
    override fun isRevolutPayAvailable(): Boolean = RevolutPaySDK.isAvailable(activity)

    override fun launchGooglePay(
        request: ExpressCheckoutPaymentRequest,
        config: GooglePayExpressCheckoutConfig,
        onResult: (ExpressCheckoutPaymentResult) -> Unit
    ) {
        onLaunch(onResult)

        val paymentRequest = PaymentDataRequest.fromJson(
            buildGooglePayRequestJson(request, config)
        )

        client.loadPaymentData(paymentRequest)
            .addOnCompleteListener(launcher::launch)
    }

    override fun launchApplePay(
        request: ExpressCheckoutPaymentRequest,
        config: ApplePayExpressCheckoutConfig,
        onResult: (ExpressCheckoutPaymentResult) -> Unit
    ) {
        onResult(
            ExpressCheckoutPaymentResult.Failure(
                "Apple Pay is not supported on Android"
            )
        )
    }

    override fun launchRevolutPay(
        request: ExpressCheckoutPaymentRequest,
        config: RevolutPayExpressCheckoutConfig,
        isSandbox : Boolean,
        onResult: (ExpressCheckoutPaymentResult) -> Unit
    ) {
        val orderToken = config.orderToken

        RevolutPaySDK.configure(
            merchantPublicKey = config.merchantPublicKey
        )

        RevolutPaySDK.pay(
            activity = activity,
            orderToken = orderToken,
            returnUrl = config.revolutReturnUrl
        ) { result ->
            onResult(result)
        }
    }
}

private fun buildGooglePayConfigJson(config: GooglePayExpressCheckoutConfig): String {
    val cardPaymentMethod = config.allowedPaymentMethods
        .firstOrNull { it.type.equals("CARD", ignoreCase = true) }
        ?.let { method ->
            JSONObject().apply {
                put("type", "CARD") // normalize to what Google expects, regardless of backend casing
                put("parameters", JSONObject().apply {
                    put("allowedCardNetworks", JSONArray(method.parameters?.allowedCardNetworks))
                    put("allowedAuthMethods", JSONArray(method.parameters?.allowedAuthMethods))
                })
            }
        } ?: throw IllegalStateException(
        "No CARD entry found in allowedPaymentMethods: ${config.allowedPaymentMethods}"
    )

    return JSONObject().apply {
        put("apiVersion", 2)
        put("apiVersionMinor", 0)
        put("allowedPaymentMethods", JSONArray(listOf(cardPaymentMethod)))
    }.toString()
}

private fun buildGooglePayRequestJson(request: ExpressCheckoutPaymentRequest, config: GooglePayExpressCheckoutConfig): String {
    val tokenizationSpecification = JSONObject()
        .put("type", "PAYMENT_GATEWAY")
        .put("parameters", JSONObject()
            .put("gateway", config.gateway)
            .put("gatewayMerchantId", config.merchantId)
        )
    val cardPaymentMethod = config.allowedPaymentMethods
        .firstOrNull { it.type.equals("CARD", ignoreCase = true) }
        ?.let { method ->
            JSONObject().apply {
                put("type", "CARD") // normalize to what Google expects, regardless of backend casing
                put("parameters", JSONObject().apply {
                    put("allowedCardNetworks", JSONArray(method.parameters?.allowedCardNetworks))
                    put("allowedAuthMethods", JSONArray(method.parameters?.allowedAuthMethods))
                })
                put("tokenizationSpecification", tokenizationSpecification)
            }
        } ?: throw IllegalStateException(
        "No CARD entry found in allowedPaymentMethods: ${config.allowedPaymentMethods}"
    )

    val transactionInfo = JSONObject()
        .put("totalPrice", request.amount)
        .put("totalPriceStatus", "FINAL")
        .put("currencyCode", request.currencyCode)
        .put("countryCode", request.countryCode)

    val merchantInfo = JSONObject()
        .put("merchantName", config.merchantName)

    return JSONObject().apply {
        put("apiVersion", 2)
        put("apiVersionMinor", 0)
        put("allowedPaymentMethods", JSONArray(listOf(cardPaymentMethod)))
        put("transactionInfo", transactionInfo)
        put("merchantInfo", merchantInfo)
    }.toString()
}

