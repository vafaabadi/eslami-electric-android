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
}
