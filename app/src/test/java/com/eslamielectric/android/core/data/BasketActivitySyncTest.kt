package com.eslamielectric.android.core.data

import androidx.test.core.app.ApplicationProvider
import com.eslamielectric.android.core.network.ApiService
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class BasketActivitySyncTest {

    private lateinit var sync: BasketActivitySync

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val api = mockk<ApiService>(relaxed = true)
        val sessionStore = SessionStore(context)
        val basketRepository = BasketRepository(context)
        sync = BasketActivitySync(context, api, sessionStore, basketRepository)
    }

    @Test
    fun getOrCreateSessionId_returnsStableUuid() = runTest {
        val first = sync.getOrCreateSessionId()
        val second = sync.getOrCreateSessionId()
        assertTrue(first.matches(Regex("[0-9a-f-]{36}", RegexOption.IGNORE_CASE)))
        assertTrue(first == second)
    }
}
