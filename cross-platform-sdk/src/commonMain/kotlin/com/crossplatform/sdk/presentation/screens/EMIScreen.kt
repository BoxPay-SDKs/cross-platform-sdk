package com.crossplatform.sdk.presentation.screens

import androidx.compose.runtime.Composable
import com.crossplatform.sdk.presentation.BackHandler

@Composable
fun EMIScreen(
    onBackPress : () -> Unit
) {
    BackHandler(onBack = onBackPress)
}