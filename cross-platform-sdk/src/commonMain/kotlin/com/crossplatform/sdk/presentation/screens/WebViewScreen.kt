package com.crossplatform.sdk.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
expect fun WebViewScreen(
    url: String?,
    html: String?,
    onBackPress: (redirectionResult: String?) -> Unit,
)

@Composable
fun WebViewUrlBar(currentUrl: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F1F1))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = currentUrl.ifEmpty { "Loading…" }, fontSize = 12.sp, color = Color(0xFF333333), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Full-screen semi-transparent loader — mirrors the RN loaderOverlay */
@Composable
fun WebViewLoader() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99FFFFFF)),   // rgba(255,255,255,0.6)
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = Color(0xFF1A1A6E),
            strokeWidth = 3.dp,
            modifier = Modifier.size(48.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Shared URL-interception logic (platform code calls this)
//
// Returns the redirectionResult query param when the completion URL is hit,
// null otherwise. Mirrors the JS `checkUrl` function exactly.
// ---------------------------------------------------------------------------

fun parseRedirectionResult(navUrl: String): String? {
    // Only act when both markers are present — mirrors:
    // navUrl.includes('payment-completion-handler') && navUrl.includes('boxpay')
    if (!navUrl.contains("payment-completion-handler") && !navUrl.contains("boxpay")) {
        return null
    }

    // Pure Kotlin query string parsing — no java.net.URI needed
    // Splits "https://example.com/path?foo=bar&redirectionResult=abc" on "?"
    val queryString = navUrl
        .substringAfter("?", missingDelimiterValue = "")
        .takeIf { it.isNotEmpty() } ?: return null

    // Find the redirectionResult param among all key=value pairs
    return queryString
        .split("&")
        .firstOrNull { it.startsWith("redirectionResult=") }
        ?.removePrefix("redirectionResult=")
        ?.takeIf { it.isNotEmpty() }
}