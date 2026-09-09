package com.crossplatform.sdk.presentation

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

/**
 * Play Services SMS User Consent API. No RECEIVE_SMS permission needed —
 * the user gets a one-time system dialog the moment a matching SMS arrives,
 * and we only get the message body after they tap Allow.
 *
 * Gradle: implementation("com.google.android.gms:play-services-auth:21.2.0")
 * in the androidMain source set.
 */
@Composable
actual fun rememberOtpAutoReader(
    expectedLength: Int,
    onOtpReceived: (String) -> Unit
): OtpAutoReaderHandle {
    val context = LocalContext.current
    val currentOnOtpReceived by rememberUpdatedState(onOtpReceived)
    val currentExpectedLength by rememberUpdatedState(expectedLength)

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val message = result.data
                ?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                .orEmpty()
            extractOtpDigits(message, currentExpectedLength)?.let(currentOnOtpReceived)
        }
        // RESULT_CANCELED = user tapped Deny, or dismissed the dialog — no-op.
    }

    return remember {
        object : OtpAutoReaderHandle {
            private var receiver: BroadcastReceiver? = null

            override fun start() {
                val client = SmsRetriever.getClient(context)
                // null sender = listen for any sender; adjust if you know the bank's number.
                client.startSmsUserConsent(null)

                val filter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
                val newReceiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context?, intent: Intent?) {
                        if (intent?.action != SmsRetriever.SMS_RETRIEVED_ACTION) return
                        val extras = intent.extras ?: return
                        val status = extras.get(SmsRetriever.EXTRA_STATUS) as? Status ?: return

                        when (status.statusCode) {
                            CommonStatusCodes.SUCCESS -> {
                                val consentIntent =
                                    extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                                consentIntent?.let(consentLauncher::launch)
                            }
                            CommonStatusCodes.TIMEOUT -> {
                                // 5-minute window (SMS User Consent's own limit) elapsed
                                // with nothing matched — let your existing 3-min page
                                // timer be the source of truth for giving up, not this.
                            }
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.registerReceiver(
                        context, newReceiver, filter, ContextCompat.RECEIVER_EXPORTED
                    )
                } else {
                    @Suppress("UnspecifiedRegisterReceiverFlag")
                    context.registerReceiver(newReceiver, filter)
                }
                receiver = newReceiver
            }

            override fun stop() {
                receiver?.let {
                    runCatching { context.unregisterReceiver(it) }
                }
                receiver = null
            }
        }
    }
}