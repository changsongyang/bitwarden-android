package com.x8bit.bitwarden.data.auth.repository

import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.auth.repository.model.BreachCountResult
import com.x8bit.bitwarden.data.auth.repository.model.DeleteAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.PasswordStrengthResult
import com.x8bit.bitwarden.data.auth.repository.model.RegisterResult
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.platform.datasource.network.authenticator.AuthenticatorProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides an API for observing an modifying authentication state.
 */
interface AuthRepository : AuthenticatorProvider {
    /**
     * Models the current auth state.
     */
    val authStateFlow: StateFlow<AuthState>

    /**
     * Emits updates for changes to the [UserState].
     */
    val userStateFlow: StateFlow<UserState?>

    /**
     * Flow of the current [CaptchaCallbackTokenResult]. Subscribers should listen to the flow
     * in order to receive updates whenever [setCaptchaCallbackTokenResult] is called.
     */
    val captchaTokenResultFlow: Flow<CaptchaCallbackTokenResult>

    /**
     * The currently persisted saved email address (or `null` if not set).
     */
    var rememberedEmailAddress: String?

    /**
     * Any special account circumstances that may be relevant (ex: pending multi-user account
     * additions).
     *
     * This allows a direct view into and modification of [UserState.specialCircumstance].
     * Note that this call has no effect when there is no [UserState] information available.
     */
    var specialCircumstance: UserState.SpecialCircumstance?

    /**
     * Attempt to delete the current account and logout them out upon success.
     */
    suspend fun deleteAccount(password: String): DeleteAccountResult

    /**
     * Attempt to login with the given email and password. Updated access token will be reflected
     * in [authStateFlow].
     */
    suspend fun login(
        email: String,
        password: String,
        captchaToken: String?,
    ): LoginResult

    /**
     * Log out the current user.
     */
    fun logout()

    /**
     * Switches to the account corresponding to the given [userId] if possible.
     */
    fun switchAccount(userId: String): SwitchAccountResult

    /**
     * Attempt to register a new account with the given parameters.
     */
    suspend fun register(
        email: String,
        masterPassword: String,
        masterPasswordHint: String?,
        captchaToken: String?,
        shouldCheckDataBreaches: Boolean,
    ): RegisterResult

    /**
     * Set the value of [captchaTokenResultFlow].
     */
    fun setCaptchaCallbackTokenResult(tokenResult: CaptchaCallbackTokenResult)

    /**
     * Attempts to get the number of times the given [password] has been breached.
     */
    suspend fun getPasswordBreachCount(password: String): BreachCountResult

    /**
     * Get the password strength for the given [email] and [password] combo.
     */
    suspend fun getPasswordStrength(email: String, password: String): PasswordStrengthResult
}