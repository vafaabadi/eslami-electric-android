package com.eslamielectric.android.core.network

import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ApiException(
    val httpCode: Int,
    override val message: String,
    val code: String? = null,
    val lockedUntil: String? = null,
    val missing: List<String>? = null
) : Exception(message)

class SessionExpiredException : Exception("Session expired. Please log in again.")

private val errorJson = Json { ignoreUnknownKeys = true }

fun mapApiException(e: Throwable): ApiException = when (e) {
    is ApiException -> e
    is HttpException -> parseHttpException(e)
    is UnknownHostException,
    is SocketTimeoutException,
    is IOException -> ApiException(0, NETWORK_HINT)
    else -> ApiException(0, e.message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again.")
}

private fun parseHttpException(e: HttpException): ApiException {
    val body = e.response()?.errorBody()?.string()
    if (!body.isNullOrBlank()) {
        try {
            val parsed = errorJson.decodeFromString<ApiErrorResponse>(body)
            return ApiException(
                httpCode = e.code(),
                message = parsed.error,
                code = parsed.code,
                lockedUntil = parsed.lockedUntil,
                missing = parsed.missing
            )
        } catch (_: Exception) {
            // fall through
        }
    }
    return ApiException(e.code(), e.message() ?: "Request failed")
}

private const val NETWORK_HINT =
    "Could not reach the store. Start the web API (npm start in cursor-my-web-app), then retry."
