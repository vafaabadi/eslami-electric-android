package com.eslamielectric.android.feature.auth

import com.eslamielectric.android.core.data.SessionStore
import com.eslamielectric.android.core.network.ApiService
import com.eslamielectric.android.core.network.AuthUserDto
import com.eslamielectric.android.core.network.LoginRequest
import com.eslamielectric.android.core.network.LoginResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private val api = mockk<ApiService>()
    private val sessionStore = mockk<SessionStore>(relaxed = true)
    private lateinit var repository: AuthRepository

    @Before
    fun setUp() {
        repository = AuthRepository(api, sessionStore, supabaseAuth = null)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun login_storesTokenFromApi() = runTest {
        val requestSlot = slot<LoginRequest>()
        coEvery { api.login(capture(requestSlot)) } returns LoginResponse(
            ok = true,
            token = "jwt-abc",
            user = AuthUserDto(id = "u1", email = "user@test.com")
        )
        repository.login(" User@Test.com ", "secret")
        assertEquals("user@test.com", requestSlot.captured.email)
        verify { sessionStore.setToken("jwt-abc") }
    }

    @Test
    fun logout_clearsToken() = runTest {
        repository.logout()
        verify { sessionStore.setToken(null) }
    }

    @Test
    fun isLoggedIn_delegatesToSessionStore() {
        every { sessionStore.isLoggedIn() } returns true
        assertTrue(repository.isLoggedIn())
        every { sessionStore.isLoggedIn() } returns false
        assertFalse(repository.isLoggedIn())
    }

    @Test
    fun isGoogleSignInAvailable_falseWithoutSupabaseClient() {
        assertFalse(repository.isGoogleSignInAvailable())
    }

    @Test
    fun getToken_delegatesToSessionStore() {
        every { sessionStore.getToken() } returns "stored-jwt"
        assertEquals("stored-jwt", repository.getToken())
    }
}
