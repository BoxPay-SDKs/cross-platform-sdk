package com.crossplatform.sdk.presentation.screens

import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
        IconButton(onClick = {
            onBackPress("")
        }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(24.dp),
                tint = Color.Black
            )
        }
        WebViewUrlBar(currentUrl = state.currentUrl)

        // ── WebView + loader overlay ─────────────────────────────────
        // Box (not Column) so the loader is stacked ON TOP of the WebView
        // and centered, instead of being laid out below it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val webView = WebView(context)
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
                },
                update = { webView ->
                    webView.requestLayout()   // ensures WebView re-measures against final container size
                }
            )

            // Now centered over the WebView instead of sitting below it.
            if (state.isLoading) {
                WebViewLoader()
            }
        }
    }
}