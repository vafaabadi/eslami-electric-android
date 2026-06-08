package com.eslamielectric.android.feature.basket

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.eslamielectric.android.core.data.BasketRepository
import com.eslamielectric.android.core.network.CreateCryptoPaymentResponse
import com.eslamielectric.android.core.network.CryptoPayCurrencyDto
import com.eslamielectric.android.core.network.OrderDto
import com.eslamielectric.android.core.network.ProfileDto
import com.eslamielectric.android.core.network.ShippingAddressRequest
import com.eslamielectric.android.feature.auth.AuthRepository
import com.eslamielectric.android.util.StripeCheckoutTabs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

const val FULFILLMENT_DELIVERY = "delivery"
const val FULFILLMENT_COLLECTION = "collection"

const val PAYMENT_CARD = "card"
const val PAYMENT_CRYPTO = "crypto"

sealed interface CheckoutUiState {
    data object Idle : CheckoutUiState
    data object Loading : CheckoutUiState
    data class Error(val message: String, val profileIncomplete: Boolean = false, val missing: List<String> = emptyList()) : CheckoutUiState
    data object SessionExpired : CheckoutUiState
    data object StripeOpened : CheckoutUiState
    data class CryptoActive(val payment: CreateCryptoPaymentResponse) : CheckoutUiState
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

    private val _cryptoCurrencies = MutableStateFlow<List<CryptoPayCurrencyDto>>(emptyList())
    val cryptoCurrencies: StateFlow<List<CryptoPayCurrencyDto>> = _cryptoCurrencies.asStateFlow()

    private val _defaultPayCurrency = MutableStateFlow<String?>(null)
    val defaultPayCurrency: StateFlow<String?> = _defaultPayCurrency.asStateFlow()

    private val _cryptoStatusMessage = MutableStateFlow<String?>(null)
    val cryptoStatusMessage: StateFlow<String?> = _cryptoStatusMessage.asStateFlow()

    private var cryptoPollJob: Job? = null

    val isLoggedIn get() = checkoutRepository.isLoggedIn()

    init {
        if (isLoggedIn) {
            viewModelScope.launch {
                runCatching { authRepository.fetchProfile() }
                    .onSuccess { _profile.value = it }
            }
        }
        viewModelScope.launch {
            runCatching { checkoutRepository.loadCryptoPayCurrencies() }
                .onSuccess { response ->
                    _cryptoCurrencies.value = response.currencies
                    _defaultPayCurrency.value = response.defaultPayCurrency
                        ?: response.currencies.firstOrNull()?.payCurrency
                }
        }
        checkoutRepository.getPendingCryptoPaymentId()?.let { paymentId ->
            viewModelScope.launch {
                when (val result = checkoutRepository.handleReturnFromCrypto(paymentId)) {
                    is CheckoutReturnResult.Paid -> {
                        _returnState.value = CheckoutReturnUiState.Paid(result.order)
                    }
                    is CheckoutReturnResult.Error -> {
                        _uiState.value = CheckoutUiState.Error(result.message)
                    }
                    else -> Unit
                }
            }
        }
    }

    fun pay(
        context: Context,
        paymentMethod: String,
        payCurrency: String,
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
        if (paymentMethod == PAYMENT_CRYPTO) {
            payCrypto(payCurrency, fulfillmentType, locale, guestEmail, guestName, guestPhone, addressLine1, addressCity, addressPostal, addressExtra)
            return
        }
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

            val shipping = shippingFor(fulfillmentType, addressLine1, addressCity, addressPostal, addressExtra)
            _uiState.value = CheckoutUiState.Loading
            try {
                checkoutRepository.startCheckout(
                    context = context,
                    input = checkoutInput(fulfillmentType, locale, guestEmail, guestName, guestPhone, shipping)
                )
                _uiState.value = CheckoutUiState.StripeOpened
            } catch (e: CheckoutException) {
                mapCheckoutException(e)
            } catch (e: Exception) {
                _uiState.value = CheckoutUiState.Error(e.message ?: "Checkout failed.")
            }
        }
    }

    fun payCrypto(
        payCurrency: String,
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
            val validationError = validate(fulfillmentType, guestEmail, guestName, addressLine1)
            if (validationError != null) {
                _uiState.value = CheckoutUiState.Error(validationError)
                return@launch
            }

            val shipping = shippingFor(fulfillmentType, addressLine1, addressCity, addressPostal, addressExtra)
            _uiState.value = CheckoutUiState.Loading
            _cryptoStatusMessage.value = null
            try {
                when (
                    val result = checkoutRepository.startCryptoCheckout(
                        input = checkoutInput(fulfillmentType, locale, guestEmail, guestName, guestPhone, shipping),
                        payCurrency = payCurrency
                    )
                ) {
                    is CryptoCheckoutStartResult.Ready -> {
                        _uiState.value = CheckoutUiState.CryptoActive(result.payment)
                        _cryptoStatusMessage.value = "Send crypto to the address below."
                        startCryptoPolling(result.payment.paymentId, result.payment.pollInMs ?: 3000)
                    }
                }
            } catch (e: CheckoutException) {
                mapCheckoutException(e)
            } catch (e: Exception) {
                _uiState.value = CheckoutUiState.Error(e.message ?: "Crypto checkout failed.")
            }
        }
    }

    fun openCryptoInvoice(context: Context, url: String?) {
        val invoiceUrl = url?.takeIf { it.isNotBlank() } ?: return
        StripeCheckoutTabs.open(context, invoiceUrl)
    }

    fun cancelCryptoCheckout() {
        cryptoPollJob?.cancel()
        cryptoPollJob = null
        viewModelScope.launch {
            checkoutRepository.clearPendingCryptoPayment()
        }
        _cryptoStatusMessage.value = null
        _uiState.value = CheckoutUiState.Idle
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

    fun checkReturnFromCrypto() {
        val paymentId = checkoutRepository.getPendingCryptoPaymentId() ?: return
        if (_returnState.value is CheckoutReturnUiState.Checking) return
        if (_uiState.value !is CheckoutUiState.CryptoActive && _uiState.value !is CheckoutUiState.StripeOpened) {
            // Resume polling after process death or returning from invoice Custom Tab.
        }

        viewModelScope.launch {
            _returnState.value = CheckoutReturnUiState.Checking
            when (val result = checkoutRepository.handleReturnFromCrypto(paymentId)) {
                is CheckoutReturnResult.Paid -> {
                    cryptoPollJob?.cancel()
                    _returnState.value = CheckoutReturnUiState.Paid(result.order)
                }
                is CheckoutReturnResult.NotPaid -> _returnState.value = CheckoutReturnUiState.NotPaid
                is CheckoutReturnResult.Error -> {
                    cryptoPollJob?.cancel()
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
            checkoutRepository.clearPendingCryptoPayment()
        }
    }

    private fun startCryptoPolling(paymentId: String, initialDelayMs: Int) {
        cryptoPollJob?.cancel()
        cryptoPollJob = viewModelScope.launch {
            var delayMs = initialDelayMs.coerceAtLeast(1000)
            while (true) {
                delay(delayMs.toLong())
                when (val poll = checkoutRepository.pollCryptoPayment(paymentId)) {
                    is CryptoPollResult.Finished -> {
                        _returnState.value = CheckoutReturnUiState.Paid(poll.order)
                        return@launch
                    }
                    CryptoPollResult.Waiting -> {
                        _cryptoStatusMessage.value = "Waiting for payment…"
                    }
                    is CryptoPollResult.Failed -> {
                        _uiState.value = CheckoutUiState.Error(poll.message)
                        checkoutRepository.clearPendingCryptoPayment()
                        return@launch
                    }
                    is CryptoPollResult.Error -> {
                        _cryptoStatusMessage.value = poll.message
                    }
                }
                delayMs = 3000
            }
        }
    }

    private fun checkoutInput(
        fulfillmentType: String,
        locale: String,
        guestEmail: String,
        guestName: String,
        guestPhone: String,
        shipping: ShippingAddressRequest?
    ) = CheckoutInput(
        fulfillmentType = fulfillmentType,
        locale = locale,
        guestEmail = if (!isLoggedIn) guestEmail else null,
        guestName = if (!isLoggedIn) guestName else null,
        guestPhone = if (!isLoggedIn) guestPhone.ifBlank { null } else null,
        shippingAddress = shipping
    )

    private fun shippingFor(
        fulfillmentType: String,
        addressLine1: String,
        addressCity: String,
        addressPostal: String,
        addressExtra: String
    ) = if (fulfillmentType == FULFILLMENT_DELIVERY) {
        ShippingAddressRequest(
            line1 = addressLine1.trim(),
            city = addressCity.trim().ifBlank { null },
            postalCode = addressPostal.trim().ifBlank { null },
            additionalInfo = addressExtra.trim().ifBlank { null }
        )
    } else {
        null
    }

    private fun mapCheckoutException(e: CheckoutException) {
        _uiState.value = when (e) {
            CheckoutException.EmptyBasket -> CheckoutUiState.Error(e.message ?: "Your basket is empty.")
            CheckoutException.SessionExpired -> CheckoutUiState.SessionExpired
            is CheckoutException.ProfileIncomplete -> CheckoutUiState.Error(
                message = e.errorMessage,
                profileIncomplete = true,
                missing = e.missing
            )
            is CheckoutException.RateLimited -> CheckoutUiState.Error(e.errorMessage)
            is CheckoutException.Api -> CheckoutUiState.Error(e.errorMessage)
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

    override fun onCleared() {
        cryptoPollJob?.cancel()
        super.onCleared()
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
