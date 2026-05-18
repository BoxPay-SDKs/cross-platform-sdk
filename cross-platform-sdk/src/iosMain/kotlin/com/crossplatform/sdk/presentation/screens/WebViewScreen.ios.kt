package com.crossplatform.sdk.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.crossplatform.sdk.domain.model.WebViewState
import kotlinx.cinterop.ObjCSignatureOverride
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.darwin.NSObject

@Composable
actual fun WebViewScreen(
    url: String?,
    html: String?,
    onBackPress: (redirectionResult: String?) -> Unit
) {
    var state by remember { mutableStateOf(WebViewState(currentUrl = url ?: "")) }
    val hasCalledBack = remember { mutableStateOf(false) }

    val handleUrl: (String) -> Unit = { navUrl ->
        state = state.copy(currentUrl = navUrl)
        if (!hasCalledBack.value) {
            val result = parseRedirectionResult(navUrl)
            if (result != null) {
                hasCalledBack.value = true
                onBackPress(result)
            }
        }
    }

    // WKWebView held in a remember so it survives recompositions
    val wkWebView = remember {
        WKWebView().apply {
            // javaScriptEnabled is true by default in WKWebView
            // domStorage is always enabled in WKWebView — no flag needed
        }
    }

    // Navigation delegate as a stable remembered object
    val navigationDelegate = remember {
        object : NSObject(), WKNavigationDelegateProtocol {

            // onPageStarted / onShouldStartLoadWithRequest equivalent
            override fun webView(
                webView: WKWebView,
                decidePolicyForNavigationAction: WKNavigationAction,
                decisionHandler: (WKNavigationActionPolicy) -> Unit,
            ) {
                val navUrl = decidePolicyForNavigationAction
                    .request.URL?.absoluteString ?: ""
                handleUrl(navUrl)
                decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
            }

            // onLoadStart
            @ObjCSignatureOverride
            override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
                state = state.copy(isLoading = true)
            }

            // onLoadEnd
            @ObjCSignatureOverride
            override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                state = state.copy(isLoading = false)
            }

            override fun webView(
                webView: WKWebView,
                didFailProvisionalNavigation: WKNavigation?,
                withError: NSError,
            ) {
                state = state.copy(isLoading = false)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── URL bar ───────────────────────────────────────────────────────
        WebViewUrlBar(currentUrl = state.currentUrl)

        // ── WKWebView via UIKitView ───────────────────────────────────────
        UIKitView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                wkWebView.navigationDelegate = navigationDelegate

                // Load content — html takes priority, then url, then fallback
                when {
                    html != null -> wkWebView.loadHTMLString(html, baseURL = null)
                    url != null  -> wkWebView.loadRequest(
                        NSURLRequest.requestWithURL(NSURL.URLWithString(url)!!)
                    )
                    else         -> wkWebView.loadHTMLString(
                        "<h1>No content provided</h1>", baseURL = null
                    )
                }
                wkWebView
            },
            update = { /* WKWebView is imperative — no update needed */ }
        )

        // Loader overlay on top while page is loading
        if (state.isLoading) {
            WebViewLoader()
        }
    }
}