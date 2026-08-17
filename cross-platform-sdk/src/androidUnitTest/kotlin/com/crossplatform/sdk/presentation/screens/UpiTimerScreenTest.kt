package com.crossplatform.sdk.presentation.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.fakes.testKoinModule
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

/**
 * Note on timing: [com.crossplatform.sdk.presentation.viewmodel.UpiTimerViewModel]
 * starts its countdown in `init {}` via `delay(1_000L)` in a loop.
 * `UnconfinedTestDispatcher` runs coroutines eagerly but does **not** advance
 * virtual time on its own — nothing in this test calls `advanceTimeBy`/
 * `advanceUntilIdle` — so the timer coroutine starts, immediately suspends
 * on the first `delay`, and never resumes within the test. This is exactly
 * what we want: it lets us assert the fixed initial countdown value
 * (300s -> "05:00") without any timing flakiness.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UpiTimerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        startKoin { modules(testKoinModule()) }
    }

    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `shows the shopper VPA and the initial 5 minute countdown`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                UpiTimerScreen(onBackPress = {}, shopperVpa = "jane@upi", buttonColor = "#000000", buttonTextColor = "#FFFFFF")
            }
        }

        composeTestRule.onNodeWithText("UPI Id: jane@upi").assertExists()
        composeTestRule.onNodeWithText("05:00").assertExists()
    }

    @Test
    fun `tapping Cancel Payment opens the confirmation modal`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                UpiTimerScreen(onBackPress = {}, shopperVpa = "jane@upi", buttonColor = "#000000", buttonTextColor = "#FFFFFF")
            }
        }

        composeTestRule.onNodeWithText("Cancel Payment").performClick()

        composeTestRule.onNodeWithText("Cancel Payment?").assertExists()
        composeTestRule.onNodeWithText("Are you sure you want to cancel this payment?").assertExists()
    }

    @Test
    fun `confirming Yes in the modal invokes onBackPress`() {
        var backPressed = false
        composeTestRule.setContent {
            ProvideSDKFonts {
                UpiTimerScreen(onBackPress = { backPressed = true }, shopperVpa = "jane@upi", buttonColor = "#000000", buttonTextColor = "#FFFFFF")
            }
        }
        composeTestRule.onNodeWithText("Cancel Payment").performClick()

        composeTestRule.onNodeWithText("Yes").performClick()

        assertTrue(backPressed)
    }

    @Test
    fun `tapping No dismisses the modal without calling onBackPress`() {
        var backPressed = false
        composeTestRule.setContent {
            ProvideSDKFonts {
                UpiTimerScreen(onBackPress = { backPressed = true }, shopperVpa = "jane@upi", buttonColor = "#000000", buttonTextColor = "#FFFFFF")
            }
        }
        composeTestRule.onNodeWithText("Cancel Payment").performClick()

        composeTestRule.onNodeWithText("No").performClick()

        assertTrue(!backPressed)
        composeTestRule.onNodeWithText("UPI Id: jane@upi").assertExists() // still on the timer screen
    }
}
