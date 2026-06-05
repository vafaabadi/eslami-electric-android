package com.eslamielectric.android.feature.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.eslamielectric.android.core.network.ApiException
import com.eslamielectric.android.core.network.ClaimAccountValidateResponse
import com.eslamielectric.android.core.network.SignupRequest
import com.eslamielectric.android.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelsTest {

    @get:Rule
    val instantTask = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>(relaxed = true)

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun signUpViewModel_emitsSuccess() = runTest {
        coEvery { authRepository.signup(any()) } returns Unit
        val vm = SignUpViewModel(authRepository)
        var ok = false
        vm.signup(
            SignupRequest(
                type = "individual",
                firstName = "A",
                surname = "B",
                mobile = "07123456789",
                email = "a@b.com",
                address = "1 Test Street",
                password = "secret123"
            )
        ) {
            ok = true
        }
        advanceUntilIdle()
        assertTrue(ok)
        assertEquals(AuthFormState.Success, vm.state.value)
    }

    @Test
    fun forgotPasswordViewModel_requiresEmail() = runTest {
        val vm = ForgotPasswordViewModel(authRepository)
        vm.submit("  ")
        assertEquals("Email is required.", (vm.state.value as AuthFormState.Error).message)
    }

    @Test
    fun forgotPasswordViewModel_emitsSuccessMessage() = runTest {
        coEvery { authRepository.forgotPassword(any()) } returns "Check your inbox"
        val vm = ForgotPasswordViewModel(authRepository)
        vm.submit("a@b.com")
        advanceUntilIdle()
        assertEquals(AuthFormState.Success, vm.state.value)
        assertEquals("Check your inbox", vm.successMessage.value)
    }

    @Test
    fun resetPasswordViewModel_validatesPasswordRules() = runTest {
        val vm = ResetPasswordViewModel(authRepository)
        vm.submit("", "password1", "password1")
        assertTrue(vm.state.value is AuthFormState.Error)
        vm.submit("tok", "short", "short")
        assertEquals("Password must be at least 8 characters.", (vm.state.value as AuthFormState.Error).message)
        vm.submit("tok", "password123", "password456")
        assertEquals("Passwords do not match.", (vm.state.value as AuthFormState.Error).message)
    }

    @Test
    fun claimAccountViewModel_validatesBlankToken() = runTest {
        val vm = ClaimAccountViewModel(authRepository, initialToken = "")
        vm.validateToken()
        assertTrue(vm.uiState.value is ClaimAccountUiState.Error)
    }

    @Test
    fun claimAccountViewModel_mapsValidToken() = runTest {
        coEvery { authRepository.validateClaimToken(any()) } returns ClaimAccountValidateResponse(
            valid = true,
            email = "b***@test.com"
        )
        val vm = ClaimAccountViewModel(authRepository, initialToken = "claim-tok")
        advanceUntilIdle()
        assertTrue(vm.uiState.value is ClaimAccountUiState.Ready)
        assertEquals("b***@test.com", (vm.uiState.value as ClaimAccountUiState.Ready).maskedEmail)
    }

    @Test
    fun claimAccountViewModel_mapsInvalidToken() = runTest {
        coEvery { authRepository.validateClaimToken(any()) } throws ApiException(400, "Invalid token")
        val vm = ClaimAccountViewModel(authRepository, initialToken = "bad")
        advanceUntilIdle()
        assertEquals("Invalid token", (vm.uiState.value as ClaimAccountUiState.Error).message)
    }
}
