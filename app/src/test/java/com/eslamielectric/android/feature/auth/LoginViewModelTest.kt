package com.eslamielectric.android.feature.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.eslamielectric.android.core.network.ApiException
import com.eslamielectric.android.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val instantTask = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        every { authRepository.isGoogleSignInAvailable() } returns false
        every { authRepository.oauthResults } returns MutableSharedFlow(replay = 0)
        every { authRepository.isLoggedInFlow } returns flowOf(false)
        viewModel = LoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun login_rejectsBlankCredentials() = runTest {
        viewModel.login("", "pass") {}
        assertTrue(viewModel.state.value is AuthFormState.Error)
        viewModel.login("a@b.com", "") {}
        assertEquals("Email and password are required.", (viewModel.state.value as AuthFormState.Error).message)
    }

    @Test
    fun login_emitsSuccessOnRepositorySuccess() = runTest {
        coEvery { authRepository.login(any(), any()) } returns Unit
        var success = false
        viewModel.login("a@b.com", "secret") { success = true }
        advanceUntilIdle()
        assertTrue(success)
        assertEquals(AuthFormState.Success, viewModel.state.value)
    }

    @Test
    fun login_mapsApiException() = runTest {
        coEvery { authRepository.login(any(), any()) } throws ApiException(401, "Invalid credentials")
        viewModel.login("a@b.com", "wrong") {}
        advanceUntilIdle()
        assertTrue(viewModel.state.value is AuthFormState.Error)
    }

    @Test
    fun signInWithGoogle_errorsWhenUnavailable() = runTest {
        viewModel.signInWithGoogle()
        assertTrue(viewModel.state.value is AuthFormState.Error)
        assertFalse(viewModel.isGoogleSignInAvailable)
    }
}
