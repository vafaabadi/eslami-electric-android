package com.eslamielectric.android.feature.basket

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.eslamielectric.android.core.data.BasketRepository
import com.eslamielectric.android.core.network.OrderDto
import com.eslamielectric.android.core.network.ProfileDto
import com.eslamielectric.android.core.network.ShippingAddressRequest
import com.eslamielectric.android.feature.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

const val FULFILLMENT_DELIVERY = "delivery"
const val FULFILLMENT_COLLECTION = "collection"

sealed interface CheckoutUiState {
    data object Idle : CheckoutUiState
    data object Loading : CheckoutUiState
    data class Error(val message: String, val profileIncomplete: Boolean = false, val missing: List<String> = emptyList()) : CheckoutUiState
    data object SessionExpired : CheckoutUiState
    data object StripeOpened : CheckoutUiState
}

sealed interface CheckoutReturnUiState {
    data object Idle : CheckoutReturnUiState
    data object Checking : CheckoutReturnUiState
    data class Paid(val order: OrderDto) : CheckoutReturnUiState
    data object NotPaid : CheckoutReturnUiState
    data class Error(val message: String) : CheckoutReturnUiState
}

class CheckoutViewModel(
    private val checkoutRepository: CheckoutRepository,
    private val authRepository: AuthRepository,
    private val basketRepository: BasketRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Idle)
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private val _returnState = MutableStateFlow<CheckoutReturnUiState>(CheckoutReturnUiState.Idle)
    val returnState: StateFlow<CheckoutReturnUiState> = _returnState.asStateFlow()

    private val _profile = MutableStateFlow<ProfileDto?>(null)
    val profile: StateFlow<ProfileDto?> = _profile.asStateFlow()

    val isLoggedIn get() = checkoutRepository.isLoggedIn()

    init {
        if (isLoggedIn) {
            viewModelScope.launch {
                runCatching { authRepository.fetchProfile() }
                    .onSuccess { _profile.value = it }
            }
        }
    }

    fun pay(
        context: Context,
        fulfillmentType: String,
        locale: String,
        guestEmail: String,
        guestName: String,
        guestPhone: String,
        addressLine1: String,
        addressCity: String,
        addressPostal: String,
        addressExtra: String
    ) {
        viewModelScope.launch {
            if (basketRepository.getItemsOnce().isEmpty()) {
                _uiState.value = CheckoutUiState.Error("Your basket is empty.")
                return@launch
            }

            val validationError = validate(
                fulfillmentType = fulfillmentType,
                guestEmail = guestEmail,
                guestName = guestName,
                addressLine1 = addressLine1
            )
            if (validationError != null) {
                _uiState.value = CheckoutUiState.Error(validationError)
                return@launch
            }

            val shipping = if (fulfillmentType == FULFILLMENT_DELIVERY) {
                ShippingAddressRequest(
                    line1 = addressLine1.trim(),
                    city = addressCity.trim().ifBlank { null },
                    postalCode = addressPostal.trim().ifBlank { null },
                    additionalInfo = addressExtra.trim().ifBlank { null }
                )
            } else {
                null
            }

            _uiState.value = CheckoutUiState.Loading
            try {
                checkoutRepository.startCheckout(
                    context = context,
                    input = CheckoutInput(
                        fulfillmentType = fulfillmentType,
                        locale = locale,
                        guestEmail = if (!isLoggedIn) guestEmail else null,
                        guestName = if (!isLoggedIn) guestName else null,
                        guestPhone = if (!isLoggedIn) guestPhone.ifBlank { null } else null,
                        shippingAddress = shipping
                    )
                )
                _uiState.value = CheckoutUiState.StripeOpened
            } catch (e: CheckoutException.EmptyBasket) {
                _uiState.value = CheckoutUiState.Error(e.message ?: "Your basket is empty.")
            } catch (e: CheckoutException.SessionExpired) {
                _uiState.value = CheckoutUiState.SessionExpired
            } catch (e: CheckoutException.ProfileIncomplete) {
                _uiState.value = CheckoutUiState.Error(
                    message = e.errorMessage,
                    profileIncomplete = true,
                    missing = e.missing
                )
            } catch (e: CheckoutException.RateLimited) {
                _uiState.value = CheckoutUiState.Error(e.errorMessage)
            } catch (e: CheckoutException.Api) {
                _uiState.value = CheckoutUiState.Error(e.errorMessage)
            } catch (e: Exception) {
                _uiState.value = CheckoutUiState.Error(e.message ?: "Checkout failed.")
            }
        }
    }

    fun checkReturnFromStripe() {
        val sessionId = checkoutRepository.getPendingSessionId() ?: return
        if (_returnState.value is CheckoutReturnUiState.Checking) return

        viewModelScope.launch {
            _returnState.value = CheckoutReturnUiState.Checking
            when (val result = checkoutRepository.handleReturnFromStripe(sessionId)) {
                is CheckoutReturnResult.Paid -> _returnState.value = CheckoutReturnUiState.Paid(result.order)
                is CheckoutReturnResult.NotPaid -> _returnState.value = CheckoutReturnUiState.NotPaid
                is CheckoutReturnResult.Error -> {
                    _returnState.value = CheckoutReturnUiState.Idle
                    _uiState.value = CheckoutUiState.Error(result.message)
                }
            }
        }
    }

    fun dismissReturnState() {
        _returnState.value = CheckoutReturnUiState.Idle
    }

    fun clearError() {
        if (_uiState.value is CheckoutUiState.Error) {
            _uiState.value = CheckoutUiState.Idle
        }
    }

    fun resetAfterStripe() {
        if (_uiState.value is CheckoutUiState.StripeOpened) {
            _uiState.value = CheckoutUiState.Idle
        }
    }

    fun dismissPendingWithoutPayment() {
        viewModelScope.launch {
            checkoutRepository.clearPendingSession()
        }
    }

    private fun validate(
        fulfillmentType: String,
        guestEmail: String,
        guestName: String,
        addressLine1: String
    ): String? {
        if (!isLoggedIn) {
            if (guestName.trim().length < 2) return "Name is required (at least 2 characters)."
            val email = guestEmail.trim().lowercase()
            if (!EMAIL_RE.matches(email)) return "A valid email is required."
        }
        if (fulfillmentType == FULFILLMENT_DELIVERY) {
            val line1 = addressLine1.trim()
            if (line1.isBlank()) return "Delivery address is required."
            if (line1.length < 5) return "Delivery address must be at least 5 characters."
        }
        return null
    }

    companion object {
        private val EMAIL_RE = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}

fun checkoutViewModelFactory(
    checkoutRepository: CheckoutRepository,
    authRepository: AuthRepository,
    basketRepository: BasketRepository
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CheckoutViewModel(checkoutRepository, authRepository, basketRepository) as T
    }
}
