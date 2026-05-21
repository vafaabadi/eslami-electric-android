package com.eslamielectric.android.feature.orders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.eslamielectric.android.core.network.ApiException
import com.eslamielectric.android.core.network.OrderDto
import com.eslamielectric.android.core.network.SessionExpiredException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OrdersListUiState {
    data object Loading : OrdersListUiState
    data class Ready(val orders: List<OrderDto>) : OrdersListUiState
    data object Empty : OrdersListUiState
    data class Error(val message: String) : OrdersListUiState
}

class OrdersListViewModel(
    private val ordersRepository: OrdersRepository
) : ViewModel() {
    private val _state = MutableStateFlow<OrdersListUiState>(OrdersListUiState.Loading)
    val state: StateFlow<OrdersListUiState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    var onSessionExpired: (() -> Unit)? = null

    fun load() {
        viewModelScope.launch {
            _isRefreshing.value = _state.value is OrdersListUiState.Ready ||
                _state.value is OrdersListUiState.Empty
            if (_state.value !is OrdersListUiState.Ready && _state.value !is OrdersListUiState.Empty) {
                _state.value = OrdersListUiState.Loading
            }
            try {
                val orders = ordersRepository.loadMyOrders()
                _state.value = if (orders.isEmpty()) {
                    OrdersListUiState.Empty
                } else {
                    OrdersListUiState.Ready(orders)
                }
            } catch (_: SessionExpiredException) {
                ordersRepository.logoutOnSessionExpired()
                onSessionExpired?.invoke()
            } catch (e: ApiException) {
                _state.value = OrdersListUiState.Error(e.message)
            } catch (e: Exception) {
                _state.value = OrdersListUiState.Error(e.message ?: "Could not load orders.")
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

sealed interface OrderDetailUiState {
    data object Loading : OrderDetailUiState
    data class Ready(val order: OrderDto) : OrderDetailUiState
    data class Error(val message: String) : OrderDetailUiState
}

sealed interface OrderActionState {
    data object Idle : OrderActionState
    data object Loading : OrderActionState
    data class Error(val message: String) : OrderActionState
}

class OrderDetailViewModel(
    private val ordersRepository: OrdersRepository,
    private val orderId: String,
    private val guestToken: String?,
    private val isGuest: Boolean
) : ViewModel() {
    private val _state = MutableStateFlow<OrderDetailUiState>(OrderDetailUiState.Loading)
    val state: StateFlow<OrderDetailUiState> = _state.asStateFlow()

    private val _actionState = MutableStateFlow<OrderActionState>(OrderActionState.Idle)
    val actionState: StateFlow<OrderActionState> = _actionState.asStateFlow()

    var onSessionExpired: (() -> Unit)? = null
    var onProfileIncomplete: ((String) -> Unit)? = null

    val effectiveGuestToken: String?
        get() = guestToken ?: ordersRepository.getCachedGuestToken(orderId)

    fun load() {
        viewModelScope.launch {
            _state.value = OrderDetailUiState.Loading
            try {
                val order = when {
                    isGuest -> {
                        ordersRepository.getCachedGuestOrder(orderId)
                            ?: guestToken?.let { ordersRepository.loadGuestByToken(it) }
                    }
                    else -> ordersRepository.findOrder(orderId)
                }
                if (order == null) {
                    _state.value = OrderDetailUiState.Error("Order not found.")
                } else {
                    _state.value = OrderDetailUiState.Ready(order)
                }
            } catch (_: SessionExpiredException) {
                ordersRepository.logoutOnSessionExpired()
                onSessionExpired?.invoke()
            } catch (e: ApiException) {
                _state.value = OrderDetailUiState.Error(e.message)
            } catch (e: Exception) {
                _state.value = OrderDetailUiState.Error(e.message ?: "Could not load order.")
            }
        }
    }

    fun cancel(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _actionState.value = OrderActionState.Loading
            try {
                if (isGuest) {
                    val token = effectiveGuestToken
                    if (token.isNullOrBlank()) {
                        _actionState.value = OrderActionState.Error("Tracking token required to cancel.")
                        return@launch
                    }
                    ordersRepository.guestCancel(token)
                    ordersRepository.loadGuestByToken(token)
                } else {
                    ordersRepository.cancelOrder(orderId)
                }
                _actionState.value = OrderActionState.Idle
                load()
                onSuccess()
            } catch (_: SessionExpiredException) {
                _actionState.value = OrderActionState.Idle
                ordersRepository.logoutOnSessionExpired()
                onSessionExpired?.invoke()
            } catch (e: ApiException) {
                _actionState.value = OrderActionState.Error(e.message)
            } catch (e: Exception) {
                _actionState.value = OrderActionState.Error(e.message ?: "Cancel failed.")
            }
        }
    }

    fun resumeCheckout(context: Context, locale: String) {
        viewModelScope.launch {
            _actionState.value = OrderActionState.Loading
            try {
                if (isGuest) {
                    val token = effectiveGuestToken
                    if (token.isNullOrBlank()) {
                        _actionState.value = OrderActionState.Error("Use the tracking link from your email to complete payment.")
                        return@launch
                    }
                    ordersRepository.guestResumeCheckout(context, token, locale)
                } else {
                    ordersRepository.resumeCheckout(context, orderId, locale)
                }
                _actionState.value = OrderActionState.Idle
            } catch (_: SessionExpiredException) {
                _actionState.value = OrderActionState.Idle
                ordersRepository.logoutOnSessionExpired()
                onSessionExpired?.invoke()
            } catch (e: Exception) {
                when (val ex = mapOrdersResumeException(e)) {
                    is OrdersException.ProfileIncomplete -> {
                        _actionState.value = OrderActionState.Idle
                        onProfileIncomplete?.invoke(ex.errorMessage)
                    }
                    is OrdersException.SessionExpired -> {
                        _actionState.value = OrderActionState.Idle
                        ordersRepository.logoutOnSessionExpired()
                        onSessionExpired?.invoke()
                    }
                    else -> _actionState.value = OrderActionState.Error(
                        (ex as? OrdersException.Api)?.errorMessage ?: e.message ?: "Could not open checkout."
                    )
                }
            }
        }
    }

    fun clearActionError() {
        if (_actionState.value is OrderActionState.Error) {
            _actionState.value = OrderActionState.Idle
        }
    }
}

sealed interface GuestTrackUiState {
    data object Idle : GuestTrackUiState
    data object Loading : GuestTrackUiState
    data class Error(val message: String) : GuestTrackUiState
}

class GuestTrackViewModel(
    private val ordersRepository: OrdersRepository
) : ViewModel() {
    private val _state = MutableStateFlow<GuestTrackUiState>(GuestTrackUiState.Idle)
    val state: StateFlow<GuestTrackUiState> = _state.asStateFlow()

    fun lookupByEmail(email: String, orderIdOrNumber: String, onFound: (OrderDto) -> Unit) {
        if (email.isBlank() || orderIdOrNumber.isBlank()) {
            _state.value = GuestTrackUiState.Error("Email and order number are required.")
            return
        }
        viewModelScope.launch {
            _state.value = GuestTrackUiState.Loading
            try {
                val order = ordersRepository.guestLookup(email, orderIdOrNumber)
                _state.value = GuestTrackUiState.Idle
                onFound(order)
            } catch (e: ApiException) {
                _state.value = GuestTrackUiState.Error(e.message)
            } catch (e: Exception) {
                _state.value = GuestTrackUiState.Error(e.message ?: "Order not found.")
            }
        }
    }

    fun lookupByToken(token: String, onFound: (OrderDto) -> Unit) {
        if (token.trim().length < 10) {
            _state.value = GuestTrackUiState.Error("Enter a valid tracking token from your email link.")
            return
        }
        viewModelScope.launch {
            _state.value = GuestTrackUiState.Loading
            try {
                val order = ordersRepository.loadGuestByToken(token)
                _state.value = GuestTrackUiState.Idle
                onFound(order)
            } catch (e: ApiException) {
                _state.value = GuestTrackUiState.Error(e.message)
            } catch (e: Exception) {
                _state.value = GuestTrackUiState.Error(e.message ?: "Order not found.")
            }
        }
    }

    fun clearError() {
        if (_state.value is GuestTrackUiState.Error) {
            _state.value = GuestTrackUiState.Idle
        }
    }
}

fun ordersViewModelFactory(
    ordersRepository: OrdersRepository,
    orderId: String = "",
    guestToken: String? = null,
    isGuest: Boolean = false
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(OrdersListViewModel::class.java) ->
            OrdersListViewModel(ordersRepository) as T
        modelClass.isAssignableFrom(OrderDetailViewModel::class.java) ->
            OrderDetailViewModel(ordersRepository, orderId, guestToken, isGuest) as T
        modelClass.isAssignableFrom(GuestTrackViewModel::class.java) ->
            GuestTrackViewModel(ordersRepository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
