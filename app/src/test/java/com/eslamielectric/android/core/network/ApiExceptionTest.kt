package com.eslamielectric.android.core.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ApiExceptionTest {

    @Test
    fun mapApiException_parsesJsonErrorBody() {
        val response = Response.error<String>(
            403,
            """{"error":"Complete your profile","code":"PROFILE_INCOMPLETE","missing":["phone"]}"""
                .toResponseBody("application/json".toMediaType())
        )
        val mapped = mapApiException(HttpException(response))
        assertEquals(403, mapped.httpCode)
        assertEquals("Complete your profile", mapped.message)
        assertEquals("PROFILE_INCOMPLETE", mapped.code)
        assertEquals(listOf("phone"), mapped.missing)
    }

    @Test
    fun mapApiException_wrapsIOException() {
        val mapped = mapApiException(IOException("network down"))
        assertEquals(0, mapped.httpCode)
        assertTrue(mapped.message.isNotBlank())
    }

    @Test
    fun mapApiException_passesThroughExistingApiException() {
        val original = ApiException(429, "Too many requests", code = "RATE_LIMITED")
        assertEquals(original, mapApiException(original))
    }

    @Test
    fun mapApiException_parsesLockedUntilField() {
        val response = Response.error<String>(
            429,
            """{"error":"Locked","code":"RATE_LIMITED","lockedUntil":"2026-06-06T12:00:00Z"}"""
                .toResponseBody("application/json".toMediaType())
        )
        val mapped = mapApiException(HttpException(response))
        assertEquals("RATE_LIMITED", mapped.code)
        assertEquals("2026-06-06T12:00:00Z", mapped.lockedUntil)
    }

    @Test
    fun mapApiException_fallsBackWhenErrorBodyMalformed() {
        val response = Response.error<String>(
            500,
            "not-json".toResponseBody("text/plain".toMediaType())
        )
        val mapped = mapApiException(HttpException(response))
        assertEquals(500, mapped.httpCode)
        assertTrue(mapped.message.isNotBlank())
    }

    @Test
    fun mapApiException_fallsBackWhenErrorBodyEmpty() {
        val response = Response.error<String>(
            404,
            "".toResponseBody("application/json".toMediaType())
        )
        val mapped = mapApiException(HttpException(response))
        assertEquals(404, mapped.httpCode)
    }

    @Test
    fun mapApiException_wrapsUnknownHost() {
        val mapped = mapApiException(java.net.UnknownHostException("offline"))
        assertEquals(0, mapped.httpCode)
        assertTrue(mapped.message.isNotBlank())
    }

    @Test
    fun mapApiException_usesGenericMessageForBlankThrowable() {
        val mapped = mapApiException(RuntimeException(""))
        assertEquals("Something went wrong. Please try again.", mapped.message)
    }
}
