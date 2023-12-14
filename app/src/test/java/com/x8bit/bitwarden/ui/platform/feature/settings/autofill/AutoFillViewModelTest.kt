package com.x8bit.bitwarden.ui.platform.feature.settings.autofill

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AutoFillViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should be correct when not set`() {
        val viewModel = createViewModel(state = null)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when set`() {
        val state = DEFAULT_STATE.copy(
            isAutoFillServicesEnabled = true,
            uriDetectionMethod = AutoFillState.UriDetectionMethod.REGULAR_EXPRESSION,
        )
        val viewModel = createViewModel(state = state)
        assertEquals(state, viewModel.stateFlow.value)
    }

    @Test
    fun `on AskToAddLoginClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.AskToAddLoginClick(true))
            assertEquals(AutoFillEvent.ShowToast("Not yet implemented.".asText()), awaitItem())
        }
        assertEquals(
            DEFAULT_STATE.copy(isAskToAddLoginEnabled = true),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on AutoFillServicesClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.AutoFillServicesClick(true))
            assertEquals(AutoFillEvent.ShowToast("Not yet implemented.".asText()), awaitItem())
        }
        assertEquals(
            DEFAULT_STATE.copy(isAutoFillServicesEnabled = true),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.BackClick)
            assertEquals(AutoFillEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on CopyTotpAutomaticallyClick should update the isCopyTotpAutomaticallyEnabled state`() =
        runTest {
            val viewModel = createViewModel()
            val isEnabled = true
            viewModel.eventFlow.test {
                viewModel.trySendAction(AutoFillAction.CopyTotpAutomaticallyClick(isEnabled))
                assertEquals(AutoFillEvent.ShowToast("Not yet implemented.".asText()), awaitItem())
            }
            assertEquals(
                DEFAULT_STATE.copy(isCopyTotpAutomaticallyEnabled = isEnabled),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `on UseAccessibilityClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.UseAccessibilityClick(true))
            assertEquals(AutoFillEvent.ShowToast("Not yet implemented.".asText()), awaitItem())
        }
        assertEquals(
            DEFAULT_STATE.copy(isUseAccessibilityEnabled = true),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on UseDrawOverClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.UseDrawOverClick(true))
            assertEquals(AutoFillEvent.ShowToast("Not yet implemented.".asText()), awaitItem())
        }
        assertEquals(
            DEFAULT_STATE.copy(isUseDrawOverEnabled = true),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on UseInlineAutofillClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.UseInlineAutofillClick(true))
            assertEquals(AutoFillEvent.ShowToast("Not yet implemented.".asText()), awaitItem())
        }
        assertEquals(
            DEFAULT_STATE.copy(isUseInlineAutoFillEnabled = true),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on UriDetectionMethodSelect should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        val method = AutoFillState.UriDetectionMethod.EXACT
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.UriDetectionMethodSelect(method))
            assertEquals(AutoFillEvent.ShowToast("Not yet implemented.".asText()), awaitItem())
        }
        assertEquals(
            DEFAULT_STATE.copy(uriDetectionMethod = method),
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel(
        state: AutoFillState? = DEFAULT_STATE,
    ): AutoFillViewModel = AutoFillViewModel(
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
    )
}

private val DEFAULT_STATE: AutoFillState = AutoFillState(
    isAskToAddLoginEnabled = false,
    isAutoFillServicesEnabled = false,
    isCopyTotpAutomaticallyEnabled = false,
    isUseAccessibilityEnabled = false,
    isUseDrawOverEnabled = false,
    isUseInlineAutoFillEnabled = false,
    uriDetectionMethod = AutoFillState.UriDetectionMethod.DEFAULT,
)