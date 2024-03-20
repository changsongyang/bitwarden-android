package com.x8bit.bitwarden.data.platform.datasource.disk.di

import android.content.Context
import android.content.SharedPreferences
import com.x8bit.bitwarden.data.platform.datasource.di.EncryptedPreferences
import com.x8bit.bitwarden.data.platform.datasource.di.UnencryptedPreferences
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSourceImpl
import com.x8bit.bitwarden.data.platform.datasource.disk.PushDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.PushDiskSourceImpl
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSourceImpl
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacySecureStorage
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacySecureStorageImpl
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacySecureStorageMigrator
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacySecureStorageMigratorImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Provides persistence-related dependencies in the platform package.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlatformDiskModule {

    @Provides
    @Singleton
    fun provideEnvironmentDiskSource(
        @UnencryptedPreferences sharedPreferences: SharedPreferences,
        json: Json,
    ): EnvironmentDiskSource =
        EnvironmentDiskSourceImpl(
            sharedPreferences = sharedPreferences,
            json = json,
        )

    @Provides
    @Singleton
    fun provideLegacySecureStorage(
        @ApplicationContext context: Context,
    ): LegacySecureStorage =
        LegacySecureStorageImpl(
            context = context,
        )

    @Provides
    @Singleton
    fun provideLegacySecureStorageMigrator(
        legacySecureStorage: LegacySecureStorage,
        @EncryptedPreferences encryptedSharedPreferences: SharedPreferences,
    ): LegacySecureStorageMigrator =
        LegacySecureStorageMigratorImpl(
            legacySecureStorage = legacySecureStorage,
            encryptedSharedPreferences = encryptedSharedPreferences,
        )

    @Provides
    @Singleton
    fun providePushDiskSource(
        @UnencryptedPreferences sharedPreferences: SharedPreferences,
    ): PushDiskSource =
        PushDiskSourceImpl(
            sharedPreferences = sharedPreferences,
        )

    @Provides
    @Singleton
    fun provideSettingsDiskSource(
        @UnencryptedPreferences sharedPreferences: SharedPreferences,
        json: Json,
    ): SettingsDiskSource =
        SettingsDiskSourceImpl(
            sharedPreferences = sharedPreferences,
            json = json,
        )
}