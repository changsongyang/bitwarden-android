package com.x8bit.bitwarden.data.auth.datasource.disk

import android.content.SharedPreferences
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.PendingAuthRequestJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.platform.datasource.disk.BaseEncryptedDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacySecureStorageMigrator
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.platform.util.decodeFromStringOrNull
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

// These keys should be encrypted
private const val ACCOUNT_TOKENS_KEY = "accountTokens"
private const val BIOMETRICS_UNLOCK_KEY = "userKeyBiometricUnlock"
private const val USER_AUTO_UNLOCK_KEY_KEY = "userKeyAutoUnlock"
private const val DEVICE_KEY_KEY = "deviceKey"
private const val PENDING_ADMIN_AUTH_REQUEST_KEY = "pendingAdminAuthRequest"

// These keys should not be encrypted
private const val UNIQUE_APP_ID_KEY = "appId"
private const val REMEMBERED_EMAIL_ADDRESS_KEY = "rememberedEmail"
private const val REMEMBERED_ORG_IDENTIFIER_KEY = "rememberedOrgIdentifier"
private const val STATE_KEY = "state"
private const val LAST_ACTIVE_TIME_KEY = "lastActiveTime"
private const val INVALID_UNLOCK_ATTEMPTS_KEY = "invalidUnlockAttempts"
private const val MASTER_KEY_ENCRYPTION_USER_KEY = "masterKeyEncryptedUserKey"
private const val MASTER_KEY_ENCRYPTION_PRIVATE_KEY = "encPrivateKey"
private const val PIN_PROTECTED_USER_KEY_KEY = "pinKeyEncryptedUserKey"
private const val ENCRYPTED_PIN_KEY = "protectedPin"
private const val ORGANIZATIONS_KEY = "organizations"
private const val ORGANIZATION_KEYS_KEY = "encOrgKeys"
private const val TWO_FACTOR_TOKEN_KEY = "twoFactorToken"
private const val MASTER_PASSWORD_HASH_KEY = "keyHash"
private const val POLICIES_KEY = "policies"
private const val SHOULD_TRUST_DEVICE_KEY = "shouldTrustDevice"
private const val EMAIL_VERIFICATION_URLS = "emailVerificationUrls"

/**
 * Primary implementation of [AuthDiskSource].
 */
@Suppress("TooManyFunctions")
class AuthDiskSourceImpl(
    encryptedSharedPreferences: SharedPreferences,
    sharedPreferences: SharedPreferences,
    legacySecureStorageMigrator: LegacySecureStorageMigrator,
    private val json: Json,
) : BaseEncryptedDiskSource(
    encryptedSharedPreferences = encryptedSharedPreferences,
    sharedPreferences = sharedPreferences,
),
    AuthDiskSource {

    private val inMemoryPinProtectedUserKeys = mutableMapOf<String, String?>()
    private val mutableOrganizationsFlowMap =
        mutableMapOf<String, MutableSharedFlow<List<SyncResponseJson.Profile.Organization>?>>()
    private val mutablePoliciesFlowMap =
        mutableMapOf<String, MutableSharedFlow<List<SyncResponseJson.Policy>?>>()
    private val mutableAccountTokensFlowMap =
        mutableMapOf<String, MutableSharedFlow<AccountTokensJson?>>()
    private val mutableUserStateFlow = bufferedMutableSharedFlow<UserStateJson?>(replay = 1)

    override var userState: UserStateJson?
        get() = getString(key = STATE_KEY)?.let { json.decodeFromStringOrNull(it) }
        set(value) {
            putString(
                key = STATE_KEY,
                value = value?.let { json.encodeToString(value) },
            )
            mutableUserStateFlow.tryEmit(value)
        }

    init {
        // We must migrate if necessary before any of the migrated values would be initialized
        // and accessed.
        legacySecureStorageMigrator.migrateIfNecessary()

        // We must migrate the tokens from being stored in the UserState(shared preferences) to
        // being stored separately in encrypted shared preferences.
        migrateAccountTokens()
    }

    override val uniqueAppId: String
        get() = getString(key = UNIQUE_APP_ID_KEY) ?: generateAndStoreUniqueAppId()

    override var rememberedEmailAddress: String?
        get() = getString(key = REMEMBERED_EMAIL_ADDRESS_KEY)
        set(value) {
            putString(
                key = REMEMBERED_EMAIL_ADDRESS_KEY,
                value = value,
            )
        }

    override var rememberedOrgIdentifier: String?
        get() = getString(key = REMEMBERED_ORG_IDENTIFIER_KEY)
        set(value) {
            putString(
                key = REMEMBERED_ORG_IDENTIFIER_KEY,
                value = value,
            )
        }

    override val userStateFlow: Flow<UserStateJson?>
        get() = mutableUserStateFlow
            .onSubscription { emit(userState) }

    override fun clearData(userId: String) {
        storeLastActiveTimeMillis(userId = userId, lastActiveTimeMillis = null)
        storeInvalidUnlockAttempts(userId = userId, invalidUnlockAttempts = null)
        storeUserKey(userId = userId, userKey = null)
        storeUserAutoUnlockKey(userId = userId, userAutoUnlockKey = null)
        storePinProtectedUserKey(userId = userId, pinProtectedUserKey = null)
        storeEncryptedPin(userId = userId, encryptedPin = null)
        storePrivateKey(userId = userId, privateKey = null)
        storeOrganizationKeys(userId = userId, organizationKeys = null)
        storeOrganizations(userId = userId, organizations = null)
        storeUserBiometricUnlockKey(userId = userId, biometricsKey = null)
        storeMasterPasswordHash(userId = userId, passwordHash = null)
        storePolicies(userId = userId, policies = null)
        storeAccountTokens(userId = userId, accountTokens = null)

        // Do not remove the DeviceKey or PendingAuthRequest on logout, these are persisted
        // indefinitely unless the TDE flow explicitly removes them.
    }

    override fun getShouldTrustDevice(userId: String): Boolean =
        requireNotNull(
            getBoolean(key = SHOULD_TRUST_DEVICE_KEY.appendIdentifier(userId), default = false),
        )

    override fun storeShouldTrustDevice(userId: String, shouldTrustDevice: Boolean?) {
        putBoolean(SHOULD_TRUST_DEVICE_KEY.appendIdentifier(userId), shouldTrustDevice)
    }

    override fun getLastActiveTimeMillis(userId: String): Long? =
        getLong(key = LAST_ACTIVE_TIME_KEY.appendIdentifier(userId))

    override fun storeLastActiveTimeMillis(
        userId: String,
        lastActiveTimeMillis: Long?,
    ) {
        putLong(
            key = LAST_ACTIVE_TIME_KEY.appendIdentifier(userId),
            value = lastActiveTimeMillis,
        )
    }

    override fun getInvalidUnlockAttempts(userId: String): Int? =
        getInt(key = INVALID_UNLOCK_ATTEMPTS_KEY.appendIdentifier(userId))

    override fun storeInvalidUnlockAttempts(
        userId: String,
        invalidUnlockAttempts: Int?,
    ) {
        putInt(
            key = INVALID_UNLOCK_ATTEMPTS_KEY.appendIdentifier(userId),
            value = invalidUnlockAttempts,
        )
    }

    override fun getUserKey(userId: String): String? =
        getString(key = MASTER_KEY_ENCRYPTION_USER_KEY.appendIdentifier(userId))

    override fun storeUserKey(userId: String, userKey: String?) {
        putString(
            key = MASTER_KEY_ENCRYPTION_USER_KEY.appendIdentifier(userId),
            value = userKey,
        )
    }

    override fun getPrivateKey(userId: String): String? =
        getString(key = MASTER_KEY_ENCRYPTION_PRIVATE_KEY.appendIdentifier(userId))

    override fun storePrivateKey(userId: String, privateKey: String?) {
        putString(
            key = MASTER_KEY_ENCRYPTION_PRIVATE_KEY.appendIdentifier(userId),
            value = privateKey,
        )
    }

    override fun getUserAutoUnlockKey(userId: String): String? =
        getEncryptedString(
            key = USER_AUTO_UNLOCK_KEY_KEY.appendIdentifier(userId),
            default = null,
        )

    override fun storeUserAutoUnlockKey(
        userId: String,
        userAutoUnlockKey: String?,
    ) {
        putEncryptedString(
            key = USER_AUTO_UNLOCK_KEY_KEY.appendIdentifier(userId),
            value = userAutoUnlockKey,
        )
    }

    override fun getDeviceKey(
        userId: String,
    ): String? = getEncryptedString(key = DEVICE_KEY_KEY.appendIdentifier(userId))

    override fun storeDeviceKey(
        userId: String,
        deviceKey: String?,
    ) {
        putEncryptedString(key = DEVICE_KEY_KEY.appendIdentifier(userId), value = deviceKey)
    }

    override fun getPendingAuthRequest(
        userId: String,
    ): PendingAuthRequestJson? =
        getEncryptedString(key = PENDING_ADMIN_AUTH_REQUEST_KEY.appendIdentifier(userId))
            ?.let { json.decodeFromStringOrNull(it) }

    override fun storePendingAuthRequest(
        userId: String,
        pendingAuthRequest: PendingAuthRequestJson?,
    ) {
        putEncryptedString(
            key = PENDING_ADMIN_AUTH_REQUEST_KEY.appendIdentifier(userId),
            value = pendingAuthRequest?.let { json.encodeToString(it) },
        )
    }

    override fun getUserBiometricUnlockKey(userId: String): String? =
        getEncryptedString(key = BIOMETRICS_UNLOCK_KEY.appendIdentifier(userId))

    override fun storeUserBiometricUnlockKey(
        userId: String,
        biometricsKey: String?,
    ) {
        putEncryptedString(
            key = BIOMETRICS_UNLOCK_KEY.appendIdentifier(userId),
            value = biometricsKey,
        )
    }

    override fun getPinProtectedUserKey(userId: String): String? =
        inMemoryPinProtectedUserKeys[userId]
            ?: getString(key = PIN_PROTECTED_USER_KEY_KEY.appendIdentifier(userId))

    override fun storePinProtectedUserKey(
        userId: String,
        pinProtectedUserKey: String?,
        inMemoryOnly: Boolean,
    ) {
        inMemoryPinProtectedUserKeys[userId] = pinProtectedUserKey
        if (inMemoryOnly) return
        putString(
            key = PIN_PROTECTED_USER_KEY_KEY.appendIdentifier(userId),
            value = pinProtectedUserKey,
        )
    }

    override fun getTwoFactorToken(email: String): String? =
        getString(key = TWO_FACTOR_TOKEN_KEY.appendIdentifier(email))

    override fun storeTwoFactorToken(email: String, twoFactorToken: String?) {
        putString(
            key = TWO_FACTOR_TOKEN_KEY.appendIdentifier(email),
            value = twoFactorToken,
        )
    }

    override fun getEncryptedPin(userId: String): String? =
        getString(key = ENCRYPTED_PIN_KEY.appendIdentifier(userId))

    override fun storeEncryptedPin(
        userId: String,
        encryptedPin: String?,
    ) {
        putString(
            key = ENCRYPTED_PIN_KEY.appendIdentifier(userId),
            value = encryptedPin,
        )
    }

    override fun getOrganizationKeys(userId: String): Map<String, String>? =
        getString(key = ORGANIZATION_KEYS_KEY.appendIdentifier(userId))
            ?.let { json.decodeFromStringOrNull(it) }

    override fun storeOrganizationKeys(
        userId: String,
        organizationKeys: Map<String, String>?,
    ) {
        putString(
            key = ORGANIZATION_KEYS_KEY.appendIdentifier(userId),
            value = organizationKeys?.let { json.encodeToString(it) },
        )
    }

    override fun getOrganizations(
        userId: String,
    ): List<SyncResponseJson.Profile.Organization>? =
        getString(key = ORGANIZATIONS_KEY.appendIdentifier(userId))
            ?.let {
                // The organizations are stored as a map
                val organizationMap: Map<String, SyncResponseJson.Profile.Organization>? =
                    json.decodeFromStringOrNull(it)
                organizationMap?.values?.toList()
            }

    override fun getOrganizationsFlow(
        userId: String,
    ): Flow<List<SyncResponseJson.Profile.Organization>?> =
        getMutableOrganizationsFlow(userId = userId)
            .onSubscription { emit(getOrganizations(userId = userId)) }

    override fun storeOrganizations(
        userId: String,
        organizations: List<SyncResponseJson.Profile.Organization>?,
    ) {
        putString(
            key = ORGANIZATIONS_KEY.appendIdentifier(userId),
            value = organizations?.let { nonNullOrganizations ->
                // The organizations are stored as a map
                val organizationsMap = nonNullOrganizations.associateBy { it.id }
                json.encodeToString(organizationsMap)
            },
        )
        getMutableOrganizationsFlow(userId = userId).tryEmit(organizations)
    }

    override fun getMasterPasswordHash(userId: String): String? =
        getString(key = MASTER_PASSWORD_HASH_KEY.appendIdentifier(userId))

    override fun storeMasterPasswordHash(userId: String, passwordHash: String?) {
        putString(key = MASTER_PASSWORD_HASH_KEY.appendIdentifier(userId), value = passwordHash)
    }

    override fun getPolicies(userId: String): List<SyncResponseJson.Policy>? =
        getString(key = POLICIES_KEY.appendIdentifier(userId))
            ?.let {
                // The policies are stored as a map.
                val policiesMap: Map<String, SyncResponseJson.Policy>? =
                    json.decodeFromStringOrNull(it)
                policiesMap?.values?.toList()
            }

    override fun getPoliciesFlow(
        userId: String,
    ): Flow<List<SyncResponseJson.Policy>?> =
        getMutablePoliciesFlow(userId = userId)
            .onSubscription { emit(getPolicies(userId = userId)) }

    override fun storePolicies(userId: String, policies: List<SyncResponseJson.Policy>?) {
        putString(
            key = POLICIES_KEY.appendIdentifier(userId),
            value = policies?.let { nonNullPolicies ->
                // The policies are stored as a map.
                val policiesMap = nonNullPolicies.associateBy { it.id }
                json.encodeToString(policiesMap)
            },
        )
        getMutablePoliciesFlow(userId = userId).tryEmit(policies)
    }

    override fun getAccountTokens(userId: String): AccountTokensJson? =
        getEncryptedString(key = ACCOUNT_TOKENS_KEY.appendIdentifier(userId))
            ?.let { json.decodeFromStringOrNull(it) }

    override fun getAccountTokensFlow(userId: String): Flow<AccountTokensJson?> =
        getMutableAccountTokensFlow(userId = userId)
            .onSubscription { emit(getAccountTokens(userId = userId)) }

    override fun storeAccountTokens(userId: String, accountTokens: AccountTokensJson?) {
        putEncryptedString(
            key = ACCOUNT_TOKENS_KEY.appendIdentifier(userId),
            value = accountTokens?.let { json.encodeToString(it) },
        )
        getMutableAccountTokensFlow(userId = userId).tryEmit(accountTokens)
    }

    override fun storeEmailVerificationUrls(
        userEmail: String,
        urls: EnvironmentUrlDataJson,
    ) {
        putString(
            key = EMAIL_VERIFICATION_URLS.appendIdentifier(userEmail),
            value = json.encodeToString(urls),
        )
    }

    override fun getEmailVerificationUrls(
        userEmail: String,
    ): EnvironmentUrlDataJson? =
        getString(key = EMAIL_VERIFICATION_URLS.appendIdentifier(userEmail))
            ?.let {
                json.decodeFromStringOrNull(it)
            }

    private fun generateAndStoreUniqueAppId(): String =
        UUID
            .randomUUID()
            .toString()
            .also {
                putString(key = UNIQUE_APP_ID_KEY, value = it)
            }

    private fun getMutableOrganizationsFlow(
        userId: String,
    ): MutableSharedFlow<List<SyncResponseJson.Profile.Organization>?> =
        mutableOrganizationsFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    private fun getMutablePoliciesFlow(
        userId: String,
    ): MutableSharedFlow<List<SyncResponseJson.Policy>?> =
        mutablePoliciesFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    private fun getMutableAccountTokensFlow(
        userId: String,
    ): MutableSharedFlow<AccountTokensJson?> =
        mutableAccountTokensFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    private fun migrateAccountTokens() {
        userState
            ?.accounts
            .orEmpty()
            .values
            .forEach { accountJson ->
                @Suppress("DEPRECATION")
                accountJson.tokens?.let { storeAccountTokens(accountJson.profile.userId, it) }
            }
        userState = userState?.copy(
            accounts = userState
                ?.accounts
                ?.mapValues { (_, accountJson) -> accountJson.copy(tokens = null) }
                .orEmpty(),
        )
    }
}
