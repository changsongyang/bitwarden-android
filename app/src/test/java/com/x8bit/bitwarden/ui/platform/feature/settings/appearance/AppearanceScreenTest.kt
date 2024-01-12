package com.x8bit.bitwarden.ui.platform.feature.settings.appearance

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppearanceScreenTest : BaseComposeTest() {

    private var haveCalledNavigateBack = false
    private val mutableEventFlow = bufferedMutableSharedFlow<AppearanceEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<AppearanceViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            AppearanceScreen(
                onNavigateBack = { haveCalledNavigateBack = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `on back click should send BackClick`() {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(AppearanceAction.BackClick) }
    }

    @Test
    fun `on language row click should display language selection dialog`() {
        composeTestRule.onNodeWithText("Language").performClick()
        composeTestRule
            .onAllNodesWithText("Language")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on language selection dialog item click should send LanguageChange and show dialog`() {
        // Clicking the Language row shows the language selection dialog
        composeTestRule.onNodeWithText("Language").performClick()
        // Selecting a language dismisses this dialog and displays the confirmation
        composeTestRule
            .onAllNodesWithText("Afrikaans")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule
            .onAllNodesWithText("Afrikaans")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsNotDisplayed()

        // Should show confirmation dialog
        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        // Clicking "Ok" should dismiss confirmation dialog
        composeTestRule.onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(
                AppearanceAction.LanguageChange(
                    language = AppLanguage.AFRIKAANS,
                ),
            )
        }
    }

    @Test
    fun `on language selection dialog cancel click should dismiss dialog`() {
        composeTestRule.onNodeWithText("Language").performClick()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on theme row click should display theme selection dialog`() {
        composeTestRule.onNodeWithText("Theme").performClick()
        composeTestRule
            .onAllNodesWithText("Theme")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on theme selection dialog item click should send ThemeChange`() {
        composeTestRule.onNodeWithText("Theme").performClick()
        composeTestRule
            .onAllNodesWithText("Dark")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(
                AppearanceAction.ThemeChange(
                    theme = AppearanceState.Theme.DARK,
                ),
            )
        }
    }

    @Test
    fun `on theme selection dialog cancel click should dismiss dialog`() {
        composeTestRule.onNodeWithText("Theme").performClick()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on show website icons row click should send ShowWebsiteIconsToggled`() {
        composeTestRule.onNodeWithText("Show website icons").performClick()
        verify { viewModel.trySendAction(AppearanceAction.ShowWebsiteIconsToggle(true)) }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(AppearanceEvent.NavigateBack)
        assertTrue(haveCalledNavigateBack)
    }
}

private val DEFAULT_STATE = AppearanceState(
    language = AppLanguage.DEFAULT,
    showWebsiteIcons = false,
    theme = AppearanceState.Theme.DEFAULT,
)
