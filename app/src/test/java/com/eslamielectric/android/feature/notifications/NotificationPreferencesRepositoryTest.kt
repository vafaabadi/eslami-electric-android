package com.eslamielectric.android.feature.notifications

import com.eslamielectric.android.core.data.SessionStore
import com.eslamielectric.android.core.network.ApiService
import com.eslamielectric.android.core.network.PushChannelPatch
import com.eslamielectric.android.core.network.PushChannelPreferences
import com.eslamielectric.android.core.network.PushPreferencesDto
import com.eslamielectric.android.core.network.PushPreferencesPatchRequest
import com.eslamielectric.android.core.network.SessionExpiredException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class NotificationPreferencesRepositoryTest {

    private val api = mockk<ApiService>()
    private val sessionStore = mockk<SessionStore>(relaxed = true)
    private lateinit var repository: NotificationPreferencesRepository

    @Before
    fun setUp() {
        every { sessionStore.isLoggedIn() } returns true
        repository = NotificationPreferencesRepository(api, sessionStore)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun load_returnsPreferencesFromApi() = runTest {
        val dto = PushPreferencesDto(
            masterEnabled = false,
            channels = PushChannelPreferences(orders = true, promotions = false)
        )
        coEvery { api.getPushPreferences() } returns dto
        val result = repository.load()
        assertFalse(result.masterEnabled)
        assertFalse(result.channels.promotions)
    }

    @Test
    fun setMasterEnabled_patchesApi() = runTest {
        val patchSlot = slot<PushPreferencesPatchRequest>()
        coEvery { api.updatePushPreferences(capture(patchSlot)) } returns PushPreferencesDto()
        repository.setMasterEnabled(true)
        assertEquals(true, patchSlot.captured.masterEnabled)
    }

    @Test
    fun setChannels_patchesChannelFlags() = runTest {
        val patchSlot = slot<PushPreferencesPatchRequest>()
        coEvery { api.updatePushPreferences(capture(patchSlot)) } returns PushPreferencesDto()
        repository.setChannels(PushChannelPatch(orders = false, general = true))
        assertEquals(false, patchSlot.captured.channels?.orders)
        assertEquals(true, patchSlot.captured.channels?.general)
    }

    @Test(expected = SessionExpiredException::class)
    fun load_throwsWhenLoggedOut() = runTest {
        every { sessionStore.isLoggedIn() } returns false
        repository.load()
    }

    @Test(expected = SessionExpiredException::class)
    fun load_clearsTokenOn401() = runTest {
        coEvery { api.getPushPreferences() } throws httpError(401, """{"error":"expired"}""")
        try {
            repository.load()
        } finally {
            verify { sessionStore.setToken(null) }
        }
    }

    @Test
    fun load_mapsApiErrorBody() = runTest {
        coEvery { api.getPushPreferences() } throws httpError(
            503,
            """{"error":"Service unavailable","code":"SERVER_ERROR"}"""
        )
        try {
            repository.load()
            throw AssertionError("Expected ApiException")
        } catch (e: com.eslamielectric.android.core.network.ApiException) {
            assertEquals(503, e.httpCode)
            assertEquals("Service unavailable", e.message)
            assertEquals("SERVER_ERROR", e.code)
        }
    }

    @Test(expected = SessionExpiredException::class)
    fun setMasterEnabled_throwsWhenLoggedOut() = runTest {
        every { sessionStore.isLoggedIn() } returns false
        repository.setMasterEnabled(true)
    }

    @Test(expected = SessionExpiredException::class)
    fun setChannels_clearsTokenOn401() = runTest {
        coEvery { api.updatePushPreferences(any()) } throws httpError(401, """{"error":"expired"}""")
        try {
            repository.setChannels(PushChannelPatch(orders = false))
        } finally {
            verify { sessionStore.setToken(null) }
        }
    }

    private fun httpError(code: Int, body: String): HttpException {
        val response = Response.error<String>(
            code,
            body.toResponseBody("application/json".toMediaType())
        )
        return HttpException(response)
    }
}
