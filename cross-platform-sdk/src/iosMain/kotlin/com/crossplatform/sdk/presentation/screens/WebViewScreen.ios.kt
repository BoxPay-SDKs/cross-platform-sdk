package com.crossplatform.sdk.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

@Composable
internal actual fun WebViewScreen(
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

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Top bar: back icon + URL bar ─────────────────────────────
        IconButton(onClick = {
            onBackPress("")
        }) {
            BackArrowIcon()
        }
        WebViewUrlBar(currentUrl = state.currentUrl)

        // ── WKWebView + loader overlay ────────────────────────────────
        // Box (not Column) so the loader is stacked ON TOP of the WebView
        // and centered, instead of being laid out below it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            UIKitView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    wkWebView.navigationDelegate = navigationDelegate

                    // Load content — html takes priority, then url, then fallback
                    when {
                        !html.isNullOrBlank() -> wkWebView.loadHTMLString(html, baseURL = null)
                        !url.isNullOrBlank()  -> wkWebView.loadRequest(
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

            // Now centered over the WebView instead of the whole screen.
            if (state.isLoading) {
                WebViewLoader()
            }
        }
    }
}

@Composable
fun BackArrowIcon(modifier: Modifier = Modifier, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(24.dp)) {
        val strokeWidth = 4f
        drawLine(
            color = color,
            start = Offset(size.width * 0.75f, size.height * 0.1f),
            end = Offset(size.width * 0.25f, size.height * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.25f, size.height * 0.5f),
            end = Offset(size.width * 0.75f, size.height * 0.9f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}