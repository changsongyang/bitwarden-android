package com.x8bit.bitwarden.data.vault.datasource.sdk

import com.bitwarden.core.Cipher
import com.bitwarden.core.CipherListView
import com.bitwarden.core.CipherView
import com.bitwarden.core.Folder
import com.bitwarden.core.FolderView

/**
 * Source of vault information and functionality from the Bitwarden SDK.
 */
interface VaultSdkSource {
    /**
     * Decrypts a [Cipher] returning a [CipherView] wrapped in a [Result].
     */
    suspend fun decryptCipher(cipher: Cipher): Result<CipherView>

    /**
     * Decrypts a list of [Cipher]s returning a list of [CipherListView] wrapped in a [Result].
     */
    suspend fun decryptCipherList(cipherList: List<Cipher>): Result<List<CipherListView>>

    /**
     * Decrypts a [Folder] returning a [FolderView] wrapped in a [Result].
     */
    suspend fun decryptFolder(folder: Folder): Result<FolderView>

    /**
     * Decrypts a list of [Folder]s returning a list of [FolderView] wrapped in a [Result].
     */
    suspend fun decryptFolderList(folderList: List<Folder>): Result<List<FolderView>>
}
