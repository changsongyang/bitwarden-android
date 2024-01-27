package com.x8bit.bitwarden.data.vault.manager.di

import android.content.Context
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.platform.manager.AppForegroundManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.FileManager
import com.x8bit.bitwarden.data.vault.manager.FileManagerImpl
import com.x8bit.bitwarden.data.vault.manager.TotpCodeManager
import com.x8bit.bitwarden.data.vault.manager.TotpCodeManagerImpl
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.manager.VaultLockManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides managers in the vault package.
 */
@Module
@InstallIn(SingletonComponent::class)
object VaultManagerModule {

    @Provides
    @Singleton
    fun provideFileManager(
        @ApplicationContext context: Context,
    ): FileManager = FileManagerImpl(context)

    @Provides
    @Singleton
    fun provideVaultLockManager(
        authDiskSource: AuthDiskSource,
        authSdkSource: AuthSdkSource,
        vaultSdkSource: VaultSdkSource,
        settingsRepository: SettingsRepository,
        appForegroundManager: AppForegroundManager,
        userLogoutManager: UserLogoutManager,
        dispatcherManager: DispatcherManager,
    ): VaultLockManager =
        VaultLockManagerImpl(
            authDiskSource = authDiskSource,
            authSdkSource = authSdkSource,
            vaultSdkSource = vaultSdkSource,
            settingsRepository = settingsRepository,
            appForegroundManager = appForegroundManager,
            userLogoutManager = userLogoutManager,
            dispatcherManager = dispatcherManager,
        )

    @Provides
    @Singleton
    fun provideTotpManager(
        vaultSdkSource: VaultSdkSource,
        dispatcherManager: DispatcherManager,
        clock: Clock,
    ): TotpCodeManager =
        TotpCodeManagerImpl(
            vaultSdkSource = vaultSdkSource,
            dispatcherManager = dispatcherManager,
            clock = clock,
        )
}
