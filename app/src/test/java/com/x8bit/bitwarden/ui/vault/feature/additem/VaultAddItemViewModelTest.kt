package com.x8bit.bitwarden.ui.vault.feature.additem

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.additem.util.toViewState
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class VaultAddItemViewModelTest : BaseViewModelTest() {

    private val initialState = createVaultAddLoginItemState()
    private val initialSavedStateHandle = createSavedStateHandleWithState(
        state = initialState,
        vaultAddEditType = VaultAddEditType.AddItem,
    )
    private val mutableVaultItemFlow = MutableStateFlow<DataState<CipherView?>>(DataState.Loading)
    private val vaultRepository: VaultRepository = mockk {
        every { getVaultItemStateFlow(DEFAULT_EDIT_ITEM_ID) } returns mutableVaultItemFlow
    }

    @BeforeEach
    fun setup() {
        mockkStatic(CIPHER_VIEW_EXTENSIONS_PATH)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(CIPHER_VIEW_EXTENSIONS_PATH)
    }

    @Test
    fun `initial state should be correct when state is null`() = runTest {
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = null,
                vaultAddEditType = VaultAddEditType.AddItem,
            ),
        )
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
        }
    }

    @Test
    fun `initial add state should be correct`() = runTest {
        val vaultAddEditType = VaultAddEditType.AddItem
        val initState = createVaultAddLoginItemState(vaultAddEditType = vaultAddEditType)
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initState,
                vaultAddEditType = vaultAddEditType,
            ),
        )
        assertEquals(initState, viewModel.stateFlow.value)
        verify(exactly = 0) {
            vaultRepository.getVaultItemStateFlow(DEFAULT_EDIT_ITEM_ID)
        }
    }

    @Test
    fun `initial edit state should be correct`() = runTest {
        val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
        val initState = createVaultAddLoginItemState(vaultAddEditType = vaultAddEditType)
        val viewModel = createAddVaultItemViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initState,
                vaultAddEditType = vaultAddEditType,
            ),
        )
        assertEquals(
            initState.copy(viewState = VaultAddItemState.ViewState.Loading),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) {
            vaultRepository.getVaultItemStateFlow(DEFAULT_EDIT_ITEM_ID)
        }
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createAddVaultItemViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultAddItemAction.CloseClick)
            assertEquals(VaultAddItemEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `in add mode, SaveClick should show dialog, and remove it once an item is saved`() =
        runTest {
            val stateWithDialog = createVaultAddLoginItemState(
                name = "tester",
                dialogState = VaultAddItemState.DialogState.Loading(
                    R.string.saving.asText(),
                ),
            )

            val stateWithName = createVaultAddLoginItemState(
                name = "tester",
            )

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = VaultAddEditType.AddItem,
                ),
            )

            coEvery {
                vaultRepository.createCipher(any())
            } returns CreateCipherResult.Success

            viewModel.stateFlow.test {
                viewModel.actionChannel.trySend(VaultAddItemAction.SaveClick)
                assertEquals(stateWithName, awaitItem())
                assertEquals(stateWithDialog, awaitItem())
                assertEquals(stateWithName, awaitItem())
            }

            coVerify(exactly = 1) {
                vaultRepository.createCipher(any())
            }
        }

    @Test
    fun `in add mode, SaveClick should update value to loading`() = runTest {
        val stateWithName = createVaultAddLoginItemState(
            name = "tester",
        )

        val viewModel = createAddVaultItemViewModel(
            createSavedStateHandleWithState(
                state = stateWithName,
                vaultAddEditType = VaultAddEditType.AddItem,
            ),
        )

        coEvery {
            vaultRepository.createCipher(any())
        } returns CreateCipherResult.Success
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultAddItemAction.SaveClick)
            assertEquals(VaultAddItemEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `in add mode, SaveClick createCipher error should emit ShowToast`() = runTest {
        val stateWithName = createVaultAddLoginItemState(
            name = "tester",
        )

        val viewModel = createAddVaultItemViewModel(
            createSavedStateHandleWithState(
                state = stateWithName,
                vaultAddEditType = VaultAddEditType.AddItem,
            ),
        )

        coEvery {
            vaultRepository.createCipher(any())
        } returns CreateCipherResult.Error
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultAddItemAction.SaveClick)
            assertEquals(VaultAddItemEvent.ShowToast("Save Item Failure"), awaitItem())
        }
    }

    @Test
    fun `in edit mode, SaveClick should show dialog, and remove it once an item is saved`() =
        runTest {
            val cipherView = mockk<CipherView>()
            val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
            val stateWithDialog = createVaultAddLoginItemState(
                vaultAddEditType = vaultAddEditType,
                name = "tester",
                dialogState = VaultAddItemState.DialogState.Loading(
                    R.string.saving.asText(),
                ),
            )

            val stateWithName = createVaultAddLoginItemState(
                vaultAddEditType = vaultAddEditType,
                name = "tester",
            )
            every { cipherView.toViewState() } returns stateWithName.viewState
            mutableVaultItemFlow.value = DataState.Loaded(cipherView)

            val viewModel = createAddVaultItemViewModel(
                createSavedStateHandleWithState(
                    state = stateWithName,
                    vaultAddEditType = vaultAddEditType,
                ),
            )

            coEvery {
                vaultRepository.updateCipher(DEFAULT_EDIT_ITEM_ID, any())
            } returns UpdateCipherResult.Success

            viewModel.stateFlow.test {
                assertEquals(stateWithName, awaitItem())
                viewModel.actionChannel.trySend(VaultAddItemAction.SaveClick)
                assertEquals(stateWithDialog, awaitItem())
                assertEquals(stateWithName, awaitItem())
            }

            coVerify(exactly = 1) {
                cipherView.toViewState()
                vaultRepository.updateCipher(DEFAULT_EDIT_ITEM_ID, any())
            }
        }

    @Test
    fun `in edit mode, SaveClick createCipher error should emit ShowToast`() = runTest {
        val cipherView = mockk<CipherView>()
        val vaultAddEditType = VaultAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)
        val stateWithName = createVaultAddLoginItemState(
            vaultAddEditType = vaultAddEditType,
            name = "tester",
        )

        every { cipherView.toViewState() } returns stateWithName.viewState
        coEvery {
            vaultRepository.updateCipher(DEFAULT_EDIT_ITEM_ID, any())
        } returns UpdateCipherResult.Error
        mutableVaultItemFlow.value = DataState.Loaded(cipherView)

        val viewModel = createAddVaultItemViewModel(
            createSavedStateHandleWithState(
                state = stateWithName,
                vaultAddEditType = vaultAddEditType,
            ),
        )

        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultAddItemAction.SaveClick)
            assertEquals(VaultAddItemEvent.ShowToast("Save Item Failure"), awaitItem())
        }

        coVerify(exactly = 1) {
            vaultRepository.updateCipher(DEFAULT_EDIT_ITEM_ID, any())
        }
    }

    @Test
    fun `Saving item with an empty name field will cause a dialog to show up`() = runTest {
        val stateWithNoName = createVaultAddSecureNotesItemState(name = "")

        val stateWithNoNameAndDialog = createVaultAddSecureNotesItemState(
            name = "",
            dialogState = VaultAddItemState.DialogState.Error(
                R.string.validation_field_required
                    .asText(R.string.name.asText()),
            ),
        )

        val viewModel = createAddVaultItemViewModel(
            createSavedStateHandleWithState(
                state = stateWithNoName,
                vaultAddEditType = VaultAddEditType.AddItem,
            ),
        )
        coEvery { vaultRepository.createCipher(any()) } returns CreateCipherResult.Success
        viewModel.stateFlow.test {
            viewModel.actionChannel.trySend(VaultAddItemAction.SaveClick)
            assertEquals(stateWithNoName, awaitItem())
            assertEquals(stateWithNoNameAndDialog, awaitItem())
        }
    }

    @Test
    fun `HandleDialogDismiss will remove the current dialog`() = runTest {
        val errorState = createVaultAddLoginItemState(
            dialogState = VaultAddItemState.DialogState.Error(
                R.string.validation_field_required
                    .asText(R.string.name.asText()),
            ),
        )

        val viewModel = createAddVaultItemViewModel(
            createSavedStateHandleWithState(
                state = errorState,
                vaultAddEditType = VaultAddEditType.AddItem,
            ),
        )

        coEvery { vaultRepository.createCipher(any()) } returns CreateCipherResult.Success
        viewModel.stateFlow.test {
            viewModel.actionChannel.trySend(VaultAddItemAction.DismissDialog)
            assertEquals(errorState, awaitItem())
            assertEquals(null, awaitItem().dialog)
        }
    }

    @Test
    fun `TypeOptionSelect LOGIN should switch to LoginItem`() = runTest {
        val viewModel = createAddVaultItemViewModel()
        val action = VaultAddItemAction.TypeOptionSelect(VaultAddItemState.ItemTypeOption.LOGIN)

        viewModel.actionChannel.trySend(action)

        val expectedState = initialState.copy(
            viewState = VaultAddItemState.ViewState.Content.Login(),
        )

        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Nested
    inner class VaultAddLoginTypeItemActions {
        private lateinit var viewModel: VaultAddItemViewModel

        @BeforeEach
        fun setup() {
            viewModel = createAddVaultItemViewModel()
        }

        @Test
        fun `NameTextChange should update name in LoginItem`() = runTest {
            val viewModel = createAddVaultItemViewModel()
            val action = VaultAddItemAction.ItemType.LoginType.NameTextChange("newName")

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (initialState.viewState as VaultAddItemState.ViewState.Content.Login)
                    .copy(name = "newName")

            val expectedState = initialState.copy(viewState = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UsernameTextChange should update username in LoginItem`() = runTest {
            val viewModel = createAddVaultItemViewModel()
            val action = VaultAddItemAction.ItemType.LoginType.UsernameTextChange("newUsername")

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (initialState.viewState as VaultAddItemState.ViewState.Content.Login)
                    .copy(username = "newUsername")

            val expectedState = initialState.copy(viewState = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `PasswordTextChange should update password in LoginItem`() = runTest {
            val viewModel = createAddVaultItemViewModel()
            val action = VaultAddItemAction.ItemType.LoginType.PasswordTextChange("newPassword")

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (initialState.viewState as VaultAddItemState.ViewState.Content.Login)
                    .copy(password = "newPassword")

            val expectedState = initialState.copy(viewState = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `UriTextChange should update uri in LoginItem`() = runTest {
            val viewModel = createAddVaultItemViewModel()
            val action = VaultAddItemAction.ItemType.LoginType.UriTextChange("newUri")

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (initialState.viewState as VaultAddItemState.ViewState.Content.Login)
                    .copy(uri = "newUri")

            val expectedState = initialState.copy(viewState = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `FolderChange should update folder in LoginItem`() = runTest {
            val viewModel = createAddVaultItemViewModel()
            val action = VaultAddItemAction.ItemType.LoginType.FolderChange("newFolder".asText())

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (initialState.viewState as VaultAddItemState.ViewState.Content.Login)
                    .copy(folderName = "newFolder".asText())

            val expectedState = initialState.copy(viewState = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `ToggleFavorite should update favorite in LoginItem`() = runTest {
            val viewModel = createAddVaultItemViewModel()
            val action = VaultAddItemAction.ItemType.LoginType.ToggleFavorite(true)

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (initialState.viewState as VaultAddItemState.ViewState.Content.Login)
                    .copy(favorite = true)

            val expectedState = initialState.copy(viewState = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `ToggleMasterPasswordReprompt should update masterPasswordReprompt in LoginItem`() =
            runTest {
                val viewModel = createAddVaultItemViewModel()
                val action = VaultAddItemAction.ItemType.LoginType.ToggleMasterPasswordReprompt(
                    isMasterPasswordReprompt = true,
                )

                viewModel.actionChannel.trySend(action)

                val expectedLoginItem =
                    (initialState.viewState as VaultAddItemState.ViewState.Content.Login)
                        .copy(masterPasswordReprompt = true)

                val expectedState = initialState.copy(viewState = expectedLoginItem)

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Test
        fun `NotesTextChange should update notes in LoginItem`() = runTest {
            val viewModel = createAddVaultItemViewModel()
            val action = VaultAddItemAction.ItemType.LoginType.NotesTextChange(notes = "newNotes")

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (initialState.viewState as VaultAddItemState.ViewState.Content.Login)
                    .copy(notes = "newNotes")

            val expectedState = initialState.copy(viewState = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `OwnershipChange should update ownership in LoginItem`() = runTest {
            val viewModel = createAddVaultItemViewModel()
            val action =
                VaultAddItemAction.ItemType.LoginType.OwnershipChange(ownership = "newOwner")

            viewModel.actionChannel.trySend(action)

            val expectedLoginItem =
                (initialState.viewState as VaultAddItemState.ViewState.Content.Login)
                    .copy(ownership = "newOwner")

            val expectedState = initialState.copy(viewState = expectedLoginItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `OpenUsernameGeneratorClick should emit ShowToast with 'Open Username Generator' message`() =
            runTest {
                val viewModel = createAddVaultItemViewModel()

                viewModel.eventFlow.test {
                    viewModel.actionChannel.trySend(
                        VaultAddItemAction.ItemType.LoginType.OpenUsernameGeneratorClick,
                    )
                    assertEquals(
                        VaultAddItemEvent.ShowToast("Open Username Generator"),
                        awaitItem(),
                    )
                }
            }

        @Test
        fun `PasswordCheckerClick should emit ShowToast with 'Password Checker' message`() =
            runTest {
                val viewModel = createAddVaultItemViewModel()

                viewModel.eventFlow.test {
                    viewModel
                        .actionChannel
                        .trySend(VaultAddItemAction.ItemType.LoginType.PasswordCheckerClick)

                    assertEquals(VaultAddItemEvent.ShowToast("Password Checker"), awaitItem())
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `OpenPasswordGeneratorClick should emit ShowToast with 'Open Password Generator' message`() =
            runTest {
                val viewModel = createAddVaultItemViewModel()

                viewModel.eventFlow.test {
                    viewModel
                        .actionChannel
                        .trySend(VaultAddItemAction.ItemType.LoginType.OpenPasswordGeneratorClick)

                    assertEquals(
                        VaultAddItemEvent.ShowToast("Open Password Generator"),
                        awaitItem(),
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `SetupTotpClick should emit ShowToast with 'Setup TOTP' message`() = runTest {
            val viewModel = createAddVaultItemViewModel()

            viewModel.eventFlow.test {
                viewModel.actionChannel.trySend(VaultAddItemAction.ItemType.LoginType.SetupTotpClick)
                assertEquals(VaultAddItemEvent.ShowToast("Setup TOTP"), awaitItem())
            }
        }

        @Suppress("MaxLineLength")
        @Test
        fun `UriSettingsClick should emit ShowToast with 'URI Settings' message`() = runTest {
            val viewModel = createAddVaultItemViewModel()

            viewModel.eventFlow.test {
                viewModel.actionChannel.trySend(VaultAddItemAction.ItemType.LoginType.UriSettingsClick)
                assertEquals(VaultAddItemEvent.ShowToast("URI Settings"), awaitItem())
            }
        }

        @Test
        fun `AddNewUriClick should emit ShowToast with 'Add New URI' message`() = runTest {
            val viewModel = createAddVaultItemViewModel()

            viewModel.eventFlow.test {
                viewModel
                    .actionChannel
                    .trySend(
                        VaultAddItemAction.ItemType.LoginType.AddNewUriClick,
                    )

                assertEquals(VaultAddItemEvent.ShowToast("Add New URI"), awaitItem())
            }
        }

        @Test
        fun `TooltipClick should emit ShowToast with 'Tooltip' message`() = runTest {
            val viewModel = createAddVaultItemViewModel()

            viewModel.eventFlow.test {
                viewModel
                    .actionChannel
                    .trySend(
                        VaultAddItemAction.ItemType.LoginType.TooltipClick,
                    )
                assertEquals(VaultAddItemEvent.ShowToast("Tooltip"), awaitItem())
            }
        }
    }

    @Nested
    inner class VaultAddSecureNotesTypeItemActions {
        private lateinit var viewModel: VaultAddItemViewModel
        private lateinit var initialState: VaultAddItemState
        private lateinit var initialSavedStateHandle: SavedStateHandle

        @BeforeEach
        fun setup() {
            initialState = createVaultAddSecureNotesItemState()
            initialSavedStateHandle = createSavedStateHandleWithState(
                state = initialState,
                vaultAddEditType = VaultAddEditType.AddItem,
            )
            viewModel = VaultAddItemViewModel(
                savedStateHandle = initialSavedStateHandle,
                vaultRepository = vaultRepository,
            )
        }

        @Test
        fun `NameTextChange should update name in SecureNotesItem`() = runTest {
            val action = VaultAddItemAction.ItemType.SecureNotesType.NameTextChange("newName")

            viewModel.actionChannel.trySend(action)

            val expectedSecureNotesItem =
                (initialState.viewState as VaultAddItemState.ViewState.Content.SecureNotes)
                    .copy(name = "newName")

            val expectedState = initialState.copy(viewState = expectedSecureNotesItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `FolderChange should update folder in SecureNotesItem`() = runTest {
            val action = VaultAddItemAction.ItemType.SecureNotesType.FolderChange(
                "newFolder".asText(),
            )

            viewModel.actionChannel.trySend(action)

            val expectedSecureNotesItem =
                (initialState.viewState as VaultAddItemState.ViewState.Content.SecureNotes)
                    .copy(folderName = "newFolder".asText())

            val expectedState = initialState.copy(viewState = expectedSecureNotesItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `ToggleFavorite should update favorite in SecureNotesItem`() = runTest {
            val action = VaultAddItemAction.ItemType.SecureNotesType.ToggleFavorite(true)

            viewModel.actionChannel.trySend(action)

            val expectedSecureNotesItem =
                (initialState.viewState as VaultAddItemState.ViewState.Content.SecureNotes)
                    .copy(favorite = true)

            val expectedState = initialState.copy(viewState = expectedSecureNotesItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `ToggleMasterPasswordReprompt should update masterPasswordReprompt in SecureNotesItem`() =
            runTest {
                val action =
                    VaultAddItemAction.ItemType.SecureNotesType.ToggleMasterPasswordReprompt(
                        isMasterPasswordReprompt = true,
                    )

                viewModel.actionChannel.trySend(action)

                val expectedSecureNotesItem =
                    (initialState.viewState as VaultAddItemState.ViewState.Content.SecureNotes)
                        .copy(masterPasswordReprompt = true)

                val expectedState = initialState.copy(viewState = expectedSecureNotesItem)

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Suppress("MaxLineLength")
        @Test
        fun `NotesTextChange should update notes in SecureNotesItem`() = runTest {
            val action =
                VaultAddItemAction.ItemType.SecureNotesType.NotesTextChange(note = "newNotes")

            viewModel.actionChannel.trySend(action)

            val expectedSecureNotesItem =
                (initialState.viewState as VaultAddItemState.ViewState.Content.SecureNotes)
                    .copy(notes = "newNotes")

            val expectedState = initialState.copy(viewState = expectedSecureNotesItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `OwnershipChange should update ownership in SecureNotesItem`() = runTest {
            val action =
                VaultAddItemAction.ItemType.SecureNotesType.OwnershipChange(ownership = "newOwner")

            viewModel.actionChannel.trySend(action)

            val expectedSecureNotesItem =
                (initialState.viewState as VaultAddItemState.ViewState.Content.SecureNotes)
                    .copy(ownership = "newOwner")

            val expectedState = initialState.copy(viewState = expectedSecureNotesItem)

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Test
        fun `TooltipClick should emit ShowToast with 'Tooltip' message`() = runTest {
            viewModel.eventFlow.test {
                viewModel
                    .actionChannel
                    .trySend(
                        VaultAddItemAction.ItemType.SecureNotesType.TooltipClick,
                    )
                assertEquals(VaultAddItemEvent.ShowToast("Not yet implemented"), awaitItem())
            }
        }
    }

    @Suppress("LongParameterList")
    private fun createVaultAddLoginItemState(
        vaultAddEditType: VaultAddEditType = VaultAddEditType.AddItem,
        name: String = "",
        username: String = "",
        password: String = "",
        uri: String = "",
        folder: Text = R.string.folder_none.asText(),
        favorite: Boolean = false,
        masterPasswordReprompt: Boolean = false,
        notes: String = "",
        ownership: String = "placeholder@email.com",
        dialogState: VaultAddItemState.DialogState? = null,
    ): VaultAddItemState =
        VaultAddItemState(
            vaultAddEditType = vaultAddEditType,
            viewState = VaultAddItemState.ViewState.Content.Login(
                name = name,
                username = username,
                password = password,
                uri = uri,
                folderName = folder,
                favorite = favorite,
                masterPasswordReprompt = masterPasswordReprompt,
                notes = notes,
                ownership = ownership,
            ),
            dialog = dialogState,
        )

    @Suppress("LongParameterList")
    private fun createVaultAddSecureNotesItemState(
        name: String = "",
        folder: Text = "No Folder".asText(),
        favorite: Boolean = false,
        masterPasswordReprompt: Boolean = false,
        notes: String = "",
        ownership: String = "placeholder@email.com",
        dialogState: VaultAddItemState.DialogState? = null,
    ): VaultAddItemState =
        VaultAddItemState(
            vaultAddEditType = VaultAddEditType.AddItem,
            viewState = VaultAddItemState.ViewState.Content.SecureNotes(
                name = name,
                folderName = folder,
                favorite = favorite,
                masterPasswordReprompt = masterPasswordReprompt,
                notes = notes,
                ownership = ownership,
            ),
            dialog = dialogState,
        )

    private fun createSavedStateHandleWithState(
        state: VaultAddItemState?,
        vaultAddEditType: VaultAddEditType,
    ) = SavedStateHandle().apply {
        set("state", state)
        set(
            "vault_add_edit_type",
            when (vaultAddEditType) {
                VaultAddEditType.AddItem -> "add"
                is VaultAddEditType.EditItem -> "edit"
            },
        )
        set("vault_edit_id", (vaultAddEditType as? VaultAddEditType.EditItem)?.vaultItemId)
    }

    private fun createAddVaultItemViewModel(
        savedStateHandle: SavedStateHandle = initialSavedStateHandle,
        vaultRepo: VaultRepository = vaultRepository,
    ): VaultAddItemViewModel =
        VaultAddItemViewModel(
            savedStateHandle = savedStateHandle,
            vaultRepository = vaultRepo,
        )
}

private const val CIPHER_VIEW_EXTENSIONS_PATH: String =
    "com.x8bit.bitwarden.ui.vault.feature.additem.util.CipherViewExtensionsKt"

private const val DEFAULT_EDIT_ITEM_ID: String = "edit_item_id"