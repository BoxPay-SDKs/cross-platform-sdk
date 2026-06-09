package com.crossplatform.sdk.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.presentation.viewmodel.MainScreenViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BoxPayElementsComponent() {
    val viewModel : MainScreenViewModel = koinViewModel()
    val screenState by viewModel.state.collectAsStateWithLifecycle()
    val boxPayAnimationVisible by viewModel.isBoxPayAnimationLoading.collectAsStateWithLifecycle()
    val showWebView by viewModel.showWebViewScreen.collectAsStateWithLifecycle()


}