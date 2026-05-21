package com.eslamielectric.android.feature.auth

import com.eslamielectric.android.core.data.SessionStore
import com.eslamielectric.android.core.network.ApiService
import com.eslamielectric.android.core.network.ForgotPasswordRequest
import com.eslamielectric.android.core.network.LoginRequest
import com.eslamielectric.android.core.network.ProfileDto
import com.eslamielectric.android.core.network.ProfilePatchRequest
import com.eslamielectric.android.core.network.SessionExpiredException
import com.eslamielectric.android.core.network.SignupRequest
import com.eslamielectric.android.core.network.mapApiException
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException

class AuthRepository(
    private val api: ApiService,
    private val sessionStore: SessionStore
) {
    val isLoggedInFlow: Flow<Boolean> = sessionStore.isLoggedInFlow
    val tokenFlow: Flow<String?> = sessionStore.tokenFlow

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

    suspend fun fetchProfile(): ProfileDto = withAuth { api.getMe() }

    suspend fun updateProfile(patch: ProfilePatchRequest): ProfileDto =
        withAuth { api.patchMe(patch) }

    suspend fun logout() {
        sessionStore.setToken(null)
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
