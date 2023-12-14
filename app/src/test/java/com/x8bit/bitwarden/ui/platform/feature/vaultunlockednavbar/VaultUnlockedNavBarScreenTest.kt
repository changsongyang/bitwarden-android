package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.navOptions
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.FakeNavHostController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Test

class VaultUnlockedNavBarScreenTest : BaseComposeTest() {
    private val fakeNavHostController = FakeNavHostController()

    private val expectedNavOptions = navOptions {
        // When changing root navigation state, pop everything else off the back stack:
        popUpTo(fakeNavHostController.graphId) {
            inclusive = false
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }

    @Test
    fun `vault tab click should send VaultTabClick action`() {
        val viewModel = mockk<VaultUnlockedNavBarViewModel>(relaxed = true)
        composeTestRule.apply {
            setContent {
                VaultUnlockedNavBarScreen(
                    viewModel = viewModel,
                    navController = fakeNavHostController,
                    onNavigateToVaultAddItem = {},
                    onNavigateToVaultItem = {},
                    onNavigateToVaultEditItem = {},
                    onNavigateToNewSend = {},
                    onNavigateToDeleteAccount = {},
                    onNavigateToPasswordHistory = {},
                )
            }
            onNodeWithText("My vault").performClick()
        }
        verify { viewModel.trySendAction(VaultUnlockedNavBarAction.VaultTabClick) }
    }

    @Test
    fun `NavigateToVaultScreen should navigate to VaultScreen`() {
        val vaultUnlockedNavBarEventFlow = MutableSharedFlow<VaultUnlockedNavBarEvent>(
            extraBufferCapacity = Int.MAX_VALUE,
        )
        val viewModel = mockk<VaultUnlockedNavBarViewModel>(relaxed = true) {
            every { eventFlow } returns vaultUnlockedNavBarEventFlow
        }
        composeTestRule.apply {
            setContent {
                VaultUnlockedNavBarScreen(
                    viewModel = viewModel,
                    navController = fakeNavHostController,
                    onNavigateToVaultAddItem = {},
                    onNavigateToVaultItem = {},
                    onNavigateToVaultEditItem = {},
                    onNavigateToNewSend = {},
                    onNavigateToDeleteAccount = {},
                    onNavigateToPasswordHistory = {},
                )
            }
            runOnIdle { fakeNavHostController.assertCurrentRoute("vault_graph") }
            vaultUnlockedNavBarEventFlow.tryEmit(VaultUnlockedNavBarEvent.NavigateToVaultScreen)
            runOnIdle {
                fakeNavHostController.assertLastNavigation(
                    route = "vault_graph",
                    navOptions = expectedNavOptions,
                )
            }
        }
    }

    @Test
    fun `send tab click should send SendTabClick action`() {
        val viewModel = mockk<VaultUnlockedNavBarViewModel>(relaxed = true)
        composeTestRule.apply {
            setContent {
                VaultUnlockedNavBarScreen(
                    viewModel = viewModel,
                    navController = fakeNavHostController,
                    onNavigateToVaultAddItem = {},
                    onNavigateToVaultItem = {},
                    onNavigateToVaultEditItem = {},
                    onNavigateToNewSend = {},
                    onNavigateToDeleteAccount = {},
                    onNavigateToPasswordHistory = {},
                )
            }
            onNodeWithText("Send").performClick()
        }
        verify { viewModel.trySendAction(VaultUnlockedNavBarAction.SendTabClick) }
    }

    @Test
    fun `NavigateToSendScreen should navigate to SendScreen`() {
        val vaultUnlockedNavBarEventFlow = MutableSharedFlow<VaultUnlockedNavBarEvent>(
            extraBufferCapacity = Int.MAX_VALUE,
        )
        val viewModel = mockk<VaultUnlockedNavBarViewModel>(relaxed = true) {
            every { eventFlow } returns vaultUnlockedNavBarEventFlow
        }
        composeTestRule.apply {
            setContent {
                VaultUnlockedNavBarScreen(
                    viewModel = viewModel,
                    navController = fakeNavHostController,
                    onNavigateToVaultAddItem = {},
                    onNavigateToVaultItem = {},
                    onNavigateToVaultEditItem = {},
                    onNavigateToNewSend = {},
                    onNavigateToDeleteAccount = {},
                    onNavigateToPasswordHistory = {},
                )
            }
            runOnIdle { fakeNavHostController.assertCurrentRoute("vault_graph") }
            vaultUnlockedNavBarEventFlow.tryEmit(VaultUnlockedNavBarEvent.NavigateToSendScreen)
            runOnIdle {
                fakeNavHostController.assertLastNavigation(
                    route = "send",
                    navOptions = expectedNavOptions,
                )
            }
        }
    }

    @Test
    fun `generator tab click should send GeneratorTabClick action`() {
        val viewModel = mockk<VaultUnlockedNavBarViewModel>(relaxed = true)
        composeTestRule.apply {
            setContent {
                VaultUnlockedNavBarScreen(
                    viewModel = viewModel,
                    navController = fakeNavHostController,
                    onNavigateToVaultAddItem = {},
                    onNavigateToVaultItem = {},
                    onNavigateToVaultEditItem = {},
                    onNavigateToNewSend = {},
                    onNavigateToDeleteAccount = {},
                    onNavigateToPasswordHistory = {},
                )
            }
            onNodeWithText("Generator").performClick()
        }
        verify { viewModel.trySendAction(VaultUnlockedNavBarAction.GeneratorTabClick) }
    }

    @Test
    fun `NavigateToGeneratorScreen should navigate to GeneratorScreen`() {
        val vaultUnlockedNavBarEventFlow = MutableSharedFlow<VaultUnlockedNavBarEvent>(
            extraBufferCapacity = Int.MAX_VALUE,
        )
        val viewModel = mockk<VaultUnlockedNavBarViewModel>(relaxed = true) {
            every { eventFlow } returns vaultUnlockedNavBarEventFlow
        }
        composeTestRule.apply {
            setContent {
                VaultUnlockedNavBarScreen(
                    viewModel = viewModel,
                    navController = fakeNavHostController,
                    onNavigateToVaultAddItem = {},
                    onNavigateToVaultItem = {},
                    onNavigateToVaultEditItem = {},
                    onNavigateToNewSend = {},
                    onNavigateToDeleteAccount = {},
                    onNavigateToPasswordHistory = {},
                )
            }
            runOnIdle { fakeNavHostController.assertCurrentRoute("vault_graph") }
            vaultUnlockedNavBarEventFlow.tryEmit(VaultUnlockedNavBarEvent.NavigateToGeneratorScreen)
            runOnIdle {
                fakeNavHostController.assertLastNavigation(
                    route = "generator",
                    navOptions = expectedNavOptions,
                )
            }
        }
    }

    @Test
    fun `settings tab click should send SendTabClick action`() {
        val viewModel = mockk<VaultUnlockedNavBarViewModel>(relaxed = true)
        composeTestRule.apply {
            setContent {
                VaultUnlockedNavBarScreen(
                    viewModel = viewModel,
                    navController = fakeNavHostController,
                    onNavigateToVaultAddItem = {},
                    onNavigateToVaultItem = {},
                    onNavigateToVaultEditItem = {},
                    onNavigateToNewSend = {},
                    onNavigateToDeleteAccount = {},
                    onNavigateToPasswordHistory = {},
                )
            }
            onNodeWithText("Settings").performClick()
        }
        verify { viewModel.trySendAction(VaultUnlockedNavBarAction.SettingsTabClick) }
    }

    @Test
    fun `NavigateToSettingsScreen should navigate to SettingsScreen`() {
        val vaultUnlockedNavBarEventFlow = MutableSharedFlow<VaultUnlockedNavBarEvent>(
            extraBufferCapacity = Int.MAX_VALUE,
        )
        val viewModel = mockk<VaultUnlockedNavBarViewModel>(relaxed = true) {
            every { eventFlow } returns vaultUnlockedNavBarEventFlow
        }
        composeTestRule.apply {
            setContent {
                VaultUnlockedNavBarScreen(
                    viewModel = viewModel,
                    navController = fakeNavHostController,
                    onNavigateToVaultAddItem = {},
                    onNavigateToVaultItem = {},
                    onNavigateToVaultEditItem = {},
                    onNavigateToNewSend = {},
                    onNavigateToDeleteAccount = {},
                    onNavigateToPasswordHistory = {},
                )
            }
            runOnIdle { fakeNavHostController.assertCurrentRoute("vault_graph") }
            vaultUnlockedNavBarEventFlow.tryEmit(VaultUnlockedNavBarEvent.NavigateToSettingsScreen)
            runOnIdle {
                fakeNavHostController.assertLastNavigation(
                    route = "settings_graph",
                    navOptions = expectedNavOptions,
                )
            }
        }
    }
}