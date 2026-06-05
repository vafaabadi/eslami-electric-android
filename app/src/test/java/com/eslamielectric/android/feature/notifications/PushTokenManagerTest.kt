package com.eslamielectric.android.feature.notifications

import androidx.test.core.app.ApplicationProvider
import com.eslamielectric.android.core.data.SessionStore
import com.eslamielectric.android.core.network.ApiService
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class PushTokenManagerTest {

    private val api = mockk<ApiService>(relaxed = true)
    private val sessionStore = mockk<SessionStore>(relaxed = true)
    private lateinit var manager: PushTokenManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        manager = PushTokenManager(context, api, sessionStore)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun onTokenRefreshed_ignoresBlankToken() {
        manager.onTokenRefreshed("")
        manager.onTokenRefreshed("   ")
        coVerify(exactly = 0) { api.registerPushToken(any()) }
    }

    @Test
    fun start_doesNotCrashWhenFcmMayBeUnconfigured() {
        manager.start()
    }

    @Test
    fun isFcmReady_matchesRepeatedCalls() {
        assertEquals(manager.isFcmReady(), manager.isFcmReady())
    }
}
