package com.crossplatform.sdk.presentation.screens

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.crossplatform.sdk.domain.model.WebViewState

@Composable
actual fun WebViewScreen(
    url: String?,
    html: String?,
    onBackPress: (redirectionResult: String?) -> Unit
) {
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

    Box(modifier = Modifier.fillMaxSize()) {

        // ── URL bar ───────────────────────────────────────────────────────
        Column {
            WebViewUrlBar(currentUrl = state.currentUrl)

            // ── WebView ───────────────────────────────────────────────────────
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                        }
                        webViewClient = object : WebViewClient() {

                            // onLoadStart equivalent
                            override fun onPageStarted(
                                view: WebView,
                                pageUrl: String,
                                favicon: Bitmap?,
                            ) {
                                super.onPageStarted(view, pageUrl, favicon)
                                state = state.copy(isLoading = true)
                            }

                            // onLoadEnd equivalent
                            override fun onPageFinished(view: WebView, pageUrl: String) {
                                super.onPageFinished(view, pageUrl)
                                state = state.copy(isLoading = false)
                                handleUrl(pageUrl)
                            }

                            // onShouldStartLoadWithRequest equivalent
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean {
                                return false   // mirrors `return true` in RN (let WebView load it)
                            }
                        }

                        // Load content — html takes priority, then url, then fallback
                        when {
                            html != null -> loadDataWithBaseURL(
                                null, html, "text/html", "UTF-8", null
                            )
                            url != null  -> loadUrl(url)
                            else         -> loadData(
                                "<h1>No content provided</h1>", "text/html", "UTF-8"
                            )
                        }
                    }
                }
            )

            // Loader overlay sits on top of WebView while loading
            if (state.isLoading) {
                WebViewLoader()
            }
        }
    }
}