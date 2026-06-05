package com.eslamielectric.android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.eslamielectric.android.core.network.ApiException
import com.eslamielectric.android.core.network.ProfileDto
import com.eslamielectric.android.core.network.ProfilePatchRequest
import com.eslamielectric.android.core.network.SessionExpiredException
import com.eslamielectric.android.core.network.SignupRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthFormState {
    data object Idle : AuthFormState
    data object Loading : AuthFormState
    data class Error(val message: String) : AuthFormState
    data object Success : AuthFormState
}

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<AuthFormState>(AuthFormState.Idle)
    val state: StateFlow<AuthFormState> = _state.asStateFlow()

    val isGoogleSignInAvailable: Boolean = authRepository.isGoogleSignInAvailable()

    init {
        viewModelScope.launch {
            authRepository.oauthResults.collect { result ->
                when (result) {
                    OAuthResult.Success -> {
                        _state.value = AuthFormState.Success
                    }
                    is OAuthResult.Error -> {
                        _state.value = AuthFormState.Error(result.message)
                    }
                }
            }
        }
        // OAuth completes via deep link after Custom Tab closes; token may arrive before oauthResults.
        viewModelScope.launch {
            authRepository.isLoggedInFlow.collect { loggedIn ->
                if (loggedIn && _state.value !is AuthFormState.Success) {
                    _state.value = AuthFormState.Success
                }
            }
        }
    }

    fun signInWithGoogle() {
        if (!isGoogleSignInAvailable) {
            _state.value = AuthFormState.Error(
                "Google sign-in is not configured. Rebuild with SUPABASE_URL and SUPABASE_ANON_KEY " +
                    "(local.properties or gradle.properties)."
            )
            return
        }
        viewModelScope.launch {
            _state.value = AuthFormState.Loading
            try {
                authRepository.startGoogleSignIn()
                // Browser opens; completion arrives via oauthResults deep link.
                _state.value = AuthFormState.Idle
            } catch (e: IllegalStateException) {
                _state.value = AuthFormState.Error(e.message ?: "Could not start Google sign-in.")
            } catch (e: Exception) {
                _state.value = AuthFormState.Error(
                    e.message ?: "Could not start Google sign-in. Enable Google in Supabase Auth providers."
                )
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthFormState.Error("Email and password are required.")
            return
        }
        viewModelScope.launch {
            _state.value = AuthFormState.Loading
            try {
                authRepository.login(email, password)
                _state.value = AuthFormState.Success
                onSuccess()
            } catch (e: ApiException) {
                _state.value = AuthFormState.Error(loginErrorMessage(e))
            } catch (e: Exception) {
                _state.value = AuthFormState.Error(e.message ?: "Login failed.")
            }
        }
    }

    fun clearError() {
        if (_state.value is AuthFormState.Error) _state.value = AuthFormState.Idle
    }
}

class SignUpViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<AuthFormState>(AuthFormState.Idle)
    val state: StateFlow<AuthFormState> = _state.asStateFlow()

    fun signup(request: SignupRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = AuthFormState.Loading
            try {
                authRepository.signup(request)
                _state.value = AuthFormState.Success
                onSuccess()
            } catch (e: ApiException) {
                _state.value = AuthFormState.Error(e.message)
            } catch (e: Exception) {
                _state.value = AuthFormState.Error(e.message ?: "Sign up failed.")
            }
        }
    }

    fun clearError() {
        if (_state.value is AuthFormState.Error) _state.value = AuthFormState.Idle
    }
}

class ForgotPasswordViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<AuthFormState>(AuthFormState.Idle)
    val state: StateFlow<AuthFormState> = _state.asStateFlow()
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun submit(email: String) {
        if (email.isBlank()) {
            _state.value = AuthFormState.Error("Email is required.")
            return
        }
        viewModelScope.launch {
            _state.value = AuthFormState.Loading
            _successMessage.value = null
            try {
                val msg = authRepository.forgotPassword(email)
                _successMessage.value = msg
                _state.value = AuthFormState.Success
            } catch (e: ApiException) {
                _state.value = AuthFormState.Error(e.message)
            } catch (e: Exception) {
                _state.value = AuthFormState.Error(e.message ?: "Request failed.")
            }
        }
    }

    fun clearError() {
        if (_state.value is AuthFormState.Error) _state.value = AuthFormState.Idle
    }
}

class ResetPasswordViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<AuthFormState>(AuthFormState.Idle)
    val state: StateFlow<AuthFormState> = _state.asStateFlow()
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun submit(token: String, newPassword: String, confirmPassword: String) {
        if (token.isBlank()) {
            _state.value = AuthFormState.Error("Reset link is invalid or missing.")
            return
        }
        if (newPassword.length < 8) {
            _state.value = AuthFormState.Error("Password must be at least 8 characters.")
            return
        }
        if (newPassword != confirmPassword) {
            _state.value = AuthFormState.Error("Passwords do not match.")
            return
        }
        viewModelScope.launch {
            _state.value = AuthFormState.Loading
            _successMessage.value = null
            try {
                val msg = authRepository.resetPassword(token, newPassword, confirmPassword)
                _successMessage.value = msg
                _state.value = AuthFormState.Success
            } catch (e: ApiException) {
                _state.value = AuthFormState.Error(e.message)
            } catch (e: Exception) {
                _state.value = AuthFormState.Error(e.message ?: "Reset failed.")
            }
        }
    }

    fun clearError() {
        if (_state.value is AuthFormState.Error) _state.value = AuthFormState.Idle
    }
}

sealed interface ClaimAccountUiState {
    data object Idle : ClaimAccountUiState
    data object Validating : ClaimAccountUiState
    data class Ready(val maskedEmail: String) : ClaimAccountUiState
    data class Error(val message: String) : ClaimAccountUiState
}

class ClaimAccountViewModel(
    private val authRepository: AuthRepository,
    initialToken: String
) : ViewModel() {
    private val _uiState = MutableStateFlow<ClaimAccountUiState>(ClaimAccountUiState.Idle)
    val uiState: StateFlow<ClaimAccountUiState> = _uiState.asStateFlow()

    private val _submitState = MutableStateFlow<AuthFormState>(AuthFormState.Idle)
    val submitState: StateFlow<AuthFormState> = _submitState.asStateFlow()

    var token: String = initialToken.trim()
        private set

    init {
        if (token.isNotBlank()) validateToken()
    }

    fun updateToken(value: String) {
        token = value.trim()
        if (_uiState.value is ClaimAccountUiState.Error) {
            _uiState.value = ClaimAccountUiState.Idle
        }
    }

    fun validateToken() {
        if (token.isBlank()) {
            _uiState.value = ClaimAccountUiState.Error("Paste the claim link token from your order email.")
            return
        }
        viewModelScope.launch {
            _uiState.value = ClaimAccountUiState.Validating
            try {
                val res = authRepository.validateClaimToken(token)
                if (res.valid) {
                    _uiState.value = ClaimAccountUiState.Ready(res.email.orEmpty())
                } else {
                    _uiState.value = ClaimAccountUiState.Error("Invalid or expired link.")
                }
            } catch (e: ApiException) {
                _uiState.value = ClaimAccountUiState.Error(e.message)
            } catch (e: Exception) {
                _uiState.value = ClaimAccountUiState.Error(e.message ?: "Could not validate link.")
            }
        }
    }

    fun claim(password: String, confirmPassword: String, onSuccess: () -> Unit) {
        if (token.isBlank()) {
            _submitState.value = AuthFormState.Error("Claim link token is required.")
            return
        }
        if (password.length < 8) {
            _submitState.value = AuthFormState.Error("Password must be at least 8 characters.")
            return
        }
        if (password != confirmPassword) {
            _submitState.value = AuthFormState.Error("Passwords do not match.")
            return
        }
        viewModelScope.launch {
            _submitState.value = AuthFormState.Loading
            try {
                authRepository.claimAccount(token, password, confirmPassword)
                _submitState.value = AuthFormState.Success
                onSuccess()
            } catch (e: ApiException) {
                _submitState.value = AuthFormState.Error(e.message)
            } catch (e: Exception) {
                _submitState.value = AuthFormState.Error(e.message ?: "Could not create account.")
            }
        }
    }

    fun clearSubmitError() {
        if (_submitState.value is AuthFormState.Error) _submitState.value = AuthFormState.Idle
    }
}

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Ready(val profile: ProfileDto) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

class AccountViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()

    val isLoggedIn = authRepository.isLoggedInFlow

    fun refresh() {
        if (!authRepository.isLoggedIn()) {
            _profileState.value = ProfileUiState.Loading
            return
        }
        viewModelScope.launch {
            _profileState.value = ProfileUiState.Loading
            try {
                _profileState.value = ProfileUiState.Ready(authRepository.fetchProfile())
            } catch (_: SessionExpiredException) {
                _profileState.value = ProfileUiState.Loading
            } catch (e: ApiException) {
                _profileState.value = ProfileUiState.Error(e.message)
            } catch (e: Exception) {
                _profileState.value = ProfileUiState.Error(e.message ?: "Could not load profile.")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _profileState.value = ProfileUiState.Loading
        }
    }
}

class ProfileViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<AuthFormState>(AuthFormState.Idle)
    val saveState: StateFlow<AuthFormState> = _saveState.asStateFlow()

    var onSessionExpired: (() -> Unit)? = null

    fun load() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                _uiState.value = ProfileUiState.Ready(authRepository.fetchProfile())
            } catch (_: SessionExpiredException) {
                onSessionExpired?.invoke()
            } catch (e: ApiException) {
                _uiState.value = ProfileUiState.Error(e.message)
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Could not load profile.")
            }
        }
    }

    fun save(patch: ProfilePatchRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _saveState.value = AuthFormState.Loading
            try {
                val updated = authRepository.updateProfile(patch)
                _uiState.value = ProfileUiState.Ready(updated)
                _saveState.value = AuthFormState.Success
                onSuccess()
            } catch (_: SessionExpiredException) {
                _saveState.value = AuthFormState.Idle
                onSessionExpired?.invoke()
            } catch (e: ApiException) {
                _saveState.value = AuthFormState.Error(e.message)
            } catch (e: Exception) {
                _saveState.value = AuthFormState.Error(e.message ?: "Save failed.")
            }
        }
    }

    fun clearSaveState() {
        if (_saveState.value is AuthFormState.Error) _saveState.value = AuthFormState.Idle
    }
}

private fun loginErrorMessage(e: ApiException): String = when {
    e.httpCode == 401 -> "Invalid email or password."
    e.httpCode == 423 -> {
        val until = e.lockedUntil?.let { " until $it" }.orEmpty()
        "Too many failed attempts. Try again$until."
    }
    e.httpCode == 429 -> "Too many requests. Please wait and try again."
    else -> e.message
}

fun authViewModelFactory(authRepository: AuthRepository): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(authRepository) as T
            modelClass.isAssignableFrom(SignUpViewModel::class.java) ->
                SignUpViewModel(authRepository) as T
            modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java) ->
                ForgotPasswordViewModel(authRepository) as T
            modelClass.isAssignableFrom(AccountViewModel::class.java) ->
                AccountViewModel(authRepository) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
                ProfileViewModel(authRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }

fun resetPasswordViewModelFactory(authRepository: AuthRepository): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ResetPasswordViewModel(authRepository) as T
    }

fun claimAccountViewModelFactory(
    authRepository: AuthRepository,
    initialToken: String
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ClaimAccountViewModel(authRepository, initialToken) as T
}
