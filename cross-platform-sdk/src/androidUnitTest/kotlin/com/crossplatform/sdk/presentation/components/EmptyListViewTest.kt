package com.crossplatform.sdk.presentation.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmptyListViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `displays the given heading and subheading`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                EmptyListView(heading = "Oops!! No results found", subHeading = "Please try another search")
            }
        }

        composeTestRule.onNodeWithText("Oops!! No results found").assertExists()
        composeTestRule.onNodeWithText("Please try another search").assertExists()
    }
}
