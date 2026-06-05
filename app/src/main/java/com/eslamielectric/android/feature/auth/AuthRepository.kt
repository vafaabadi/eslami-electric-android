package com.eslamielectric.android.feature.auth

import android.content.Intent
import com.eslamielectric.android.core.data.SessionStore
import com.eslamielectric.android.core.network.ApiService
import com.eslamielectric.android.core.network.AuthTokenRequest
import com.eslamielectric.android.core.network.ClaimAccountRequest
import com.eslamielectric.android.core.network.ClaimAccountValidateResponse
import com.eslamielectric.android.core.network.ForgotPasswordRequest
import com.eslamielectric.android.core.network.ResetPasswordRequest
import com.eslamielectric.android.core.network.LoginRequest
import com.eslamielectric.android.core.network.ProfileDto
import com.eslamielectric.android.core.network.ProfilePatchRequest
import com.eslamielectric.android.core.network.SessionExpiredException
import com.eslamielectric.android.core.network.SignupRequest
import com.eslamielectric.android.core.network.mapApiException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import retrofit2.HttpException

sealed class OAuthResult {
    data object Success : OAuthResult()
    data class Error(val message: String) : OAuthResult()
}

class AuthRepository(
    private val api: ApiService,
    private val sessionStore: SessionStore,
    supabaseAuth: SupabaseAuthClient? = null
) {
    private val supabaseAuth: SupabaseAuthClient? =
        supabaseAuth ?: SupabaseAuthClient.fromBuildConfig()
    val isLoggedInFlow: Flow<Boolean> = sessionStore.isLoggedInFlow
    val tokenFlow: Flow<String?> = sessionStore.tokenFlow

    private val oauthScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _oauthResults = MutableSharedFlow<OAuthResult>(replay = 1, extraBufferCapacity = 1)
    val oauthResults: SharedFlow<OAuthResult> = _oauthResults.asSharedFlow()

    fun isGoogleSignInAvailable(): Boolean = supabaseAuth != null

    fun isLoggedIn(): Boolean = sessionStore.isLoggedIn()
    fun getToken(): String? = sessionStore.getToken()

    suspend fun login(email: String, password: String) {
        try {
            val res = api.login(LoginRequest(email.trim().lowercase(), password))
            sessionStore.setToken(res.token)
        } catch (e: HttpException) {
            throw mapApiException(e)
        }
    }

    suspend fun signup(request: SignupRequest) {
        try {
            val res = api.signup(request)
            sessionStore.setToken(res.token)
        } catch (e: HttpException) {
            throw mapApiException(e)
        }
    }

    suspend fun forgotPassword(email: String): String {
        try {
            val res = api.forgotPassword(ForgotPasswordRequest(email.trim().lowercase()))
            return res.message
        } catch (e: HttpException) {
            throw mapApiException(e)
        }
    }

    suspend fun resetPassword(token: String, newPassword: String, confirmPassword: String): String {
        try {
            val res = api.resetPassword(
                ResetPasswordRequest(
                    token = token.trim(),
                    newPassword = newPassword,
                    confirmPassword = confirmPassword
                )
            )
            return res.message
        } catch (e: HttpException) {
            throw mapApiException(e)
        }
    }

    suspend fun validateClaimToken(token: String): ClaimAccountValidateResponse {
        try {
            return api.validateClaimAccount(token.trim())
        } catch (e: HttpException) {
            throw mapApiException(e)
        }
    }

    suspend fun claimAccount(token: String, password: String, confirmPassword: String): String {
        try {
            val res = api.claimAccount(
                ClaimAccountRequest(
                    token = token.trim(),
                    password = password,
                    confirmPassword = confirmPassword
                )
            )
            sessionStore.setToken(res.token)
            return res.message ?: "Account claimed. You can now view your orders."
        } catch (e: HttpException) {
            throw mapApiException(e)
        }
    }

    suspend fun fetchProfile(): ProfileDto = withAuth { api.getMe() }

    suspend fun updateProfile(patch: ProfilePatchRequest): ProfileDto =
        withAuth { api.patchMe(patch) }

    suspend fun logout() {
        sessionStore.setToken(null)
    }

    suspend fun startGoogleSignIn() {
        val client = supabaseAuth
            ?: throw IllegalStateException(
                "Google sign-in is not configured. Rebuild with SUPABASE_URL and SUPABASE_ANON_KEY " +
                    "(local.properties or gradle.properties)."
            )
        try {
            client.signInWithGoogle()
        } catch (e: Exception) {
            throw IllegalStateException(
                e.message ?: "Could not start Google sign-in. Enable Google in Supabase Auth providers.",
                e
            )
        }
    }

    fun handleOAuthDeepLink(intent: Intent?) {
        try {
            handleOAuthDeepLinkInternal(intent)
        } catch (_: Exception) {
            // Never crash the app on malformed or unexpected OAuth callbacks.
        }
    }

    private fun handleOAuthDeepLinkInternal(intent: Intent?) {
        val client = supabaseAuth ?: return
        val uri = intent?.data
        if (uri != null && uri.scheme == OAuthRedirect.SCHEME && uri.host == OAuthRedirect.HOST) {
            val err = uri.getQueryParameter("error") ?: uri.fragment?.let { parseFragmentParam(it, "error") }
            val errDesc = uri.getQueryParameter("error_description")
                ?: uri.fragment?.let { parseFragmentParam(it, "error_description") }
            if (!err.isNullOrBlank() || !errDesc.isNullOrBlank()) {
                _oauthResults.tryEmit(
                    OAuthResult.Error(
                        errDesc?.replace("+", " ")?.trim()?.ifBlank { err }
                            ?: err
                            ?: "Google sign-in was cancelled or failed."
                    )
                )
                return
            }
        }
        val handled = client.handleOAuthDeepLink(intent) { session ->
            val accessToken = session.accessToken
            if (accessToken.isNullOrBlank()) {
                _oauthResults.tryEmit(OAuthResult.Error("No sign-in session was returned. Try again."))
            } else {
                exchangeSupabaseAccessToken(accessToken)
            }
        }
        if (!handled && intent?.data != null) {
            val data = intent.data
            if (data?.scheme == OAuthRedirect.SCHEME && data.host == OAuthRedirect.HOST) {
                _oauthResults.tryEmit(
                    OAuthResult.Error(
                        "Could not complete Google sign-in. Add eslamielectric://auth-callback to " +
                            "Supabase Auth redirect URLs, then try again."
                    )
                )
            }
        }
    }

    private fun exchangeSupabaseAccessToken(accessToken: String) {
        oauthScope.launch(Dispatchers.IO) {
            try {
                val res = api.exchangeAuthToken(AuthTokenRequest(accessToken))
                sessionStore.setToken(res.token)
                _oauthResults.emit(OAuthResult.Success)
            } catch (e: HttpException) {
                val mapped = mapApiException(e)
                _oauthResults.emit(OAuthResult.Error(mapped.message))
            } catch (e: Exception) {
                _oauthResults.emit(
                    OAuthResult.Error(e.message ?: "Could not complete sign-in.")
                )
            }
        }
    }

    private fun parseFragmentParam(fragment: String, key: String): String? {
        val params = fragment.split("&").associate { part ->
            val idx = part.indexOf('=')
            if (idx < 0) part to "" else part.substring(0, idx) to part.substring(idx + 1)
        }
        return params[key]
    }

    private suspend fun <T> withAuth(block: suspend () -> T): T {
        if (!isLoggedIn()) throw SessionExpiredException()
        return try {
            block()
        } catch (e: HttpException) {
            if (e.code() == 401) {
                sessionStore.setToken(null)
                throw SessionExpiredException()
            }
            throw mapApiException(e)
        }
    }
}
