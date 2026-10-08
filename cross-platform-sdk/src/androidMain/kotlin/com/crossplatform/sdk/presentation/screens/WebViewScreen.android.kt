package com.crossplatform.sdk.presentation.screens

import android.app.AlertDialog
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.crossplatform.sdk.domain.model.WebViewState
import com.crossplatform.sdk.presentation.BackHandler

@Composable
internal actual fun WebViewScreen(
    url: String?,
    html: String?,
    onBackPress: (redirectionResult: String?) -> Unit
) {
    BackHandler {
        onBackPress("")
    }
    var state by remember { mutableStateOf(WebViewState(currentUrl = url ?: "")) }

    // Guard against calling onBackPress more than once — mirrors hasCalledBack ref
    val hasCalledBack = remember { mutableStateOf(false) }

    val handleUrl: (String) -> Unit = { navUrl ->
        state = state.copy(currentUrl = navUrl)
        if (!hasCalledBack.value) {
            val result = parseRedirectionResult(navUrl)
            if (result != null) {           // null = "not a completion URL"
                hasCalledBack.value = true
                onBackPress(result)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Top bar: back icon + URL bar ─────────────────────────────

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White),        // solid background blocks WebView content behind it
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBackPress("") }) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier           = Modifier.size(24.dp),
                    tint               = Color.Black
                )
            }
        }
        WebViewUrlBar(currentUrl = state.currentUrl)

        // ── WebView + loader overlay ─────────────────────────────────
        // Box (not Column) so the loader is stacked ON TOP of the WebView
        // and centered, instead of being laid out below it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val webView = WebView(context).apply {
                        // CRITICAL: Explicitly set LayoutParams so WebView occupies space
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    webView.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                    }

                    CookieManager.getInstance().apply {
                        setAcceptThirdPartyCookies(webView, true)   // ← the critical one for this flow
                    }
                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageStarted(
                            view: WebView,
                            pageUrl: String,
                            favicon: Bitmap?,
                        ) {
                            super.onPageStarted(view, pageUrl, favicon)
                            state = state.copy(isLoading = true)
                        }

                        override fun onPageFinished(view: WebView, pageUrl: String) {
                            super.onPageFinished(view, pageUrl)
                            state = state.copy(isLoading = false)
                            handleUrl(pageUrl)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            return false
                        }
                    }

                    webView.webChromeClient = object : WebChromeClient() {
                        override fun onJsConfirm(
                            view: WebView,
                            url: String?,
                            message: String?,
                            result: JsResult
                        ): Boolean {
                            AlertDialog.Builder(context)
                                .setMessage(message)
                                .setPositiveButton("OK") { _, _ -> result.confirm() }
                                .setNegativeButton("Cancel") { _, _ -> result.cancel() }
                                .setOnCancelListener { result.cancel() }   // handles back-press on dialog
                                .show()
                            return true   // ← tells WebView "we handled it, don't drop it"
                        }

                        // Optional but good to have — prevents silent drops of alert/prompt too
                        override fun onJsAlert(
                            view: WebView,
                            url: String?,
                            message: String?,
                            result: JsResult
                        ): Boolean {
                            AlertDialog.Builder(context)
                                .setMessage(message)
                                .setPositiveButton("OK") { _, _ -> result.confirm() }
                                .setOnCancelListener { result.confirm() }
                                .show()
                            return true
                        }
                    }
                    when {
                        !html.isNullOrBlank() -> {
                            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                        }
                        !url.isNullOrBlank() -> {
                            webView.loadUrl(url)
                        }
                        else -> {
                            webView.loadData("<h1>No content provided</h1>", "text/html", "UTF-8")
                        }
                    }
                    webView
                }
            )

            // Now centered over the WebView instead of sitting below it.
            if (state.isLoading) {
                WebViewLoader()
            }
        }
    }
}