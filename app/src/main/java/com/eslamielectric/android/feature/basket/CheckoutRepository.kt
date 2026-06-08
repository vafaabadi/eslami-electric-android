package com.eslamielectric.android.feature.basket

import android.content.Context
import com.eslamielectric.android.core.data.BasketRepository
import com.eslamielectric.android.core.data.PendingCheckoutStore
import com.eslamielectric.android.core.data.SessionStore
import com.eslamielectric.android.core.network.ApiService
import com.eslamielectric.android.core.network.CreateCheckoutSessionRequest
import com.eslamielectric.android.core.network.CreateCryptoPaymentResponse
import com.eslamielectric.android.core.network.CryptoPayCurrenciesResponse
import com.eslamielectric.android.core.network.CryptoPaymentStatusResponse
import com.eslamielectric.android.core.network.NetworkModule
import com.eslamielectric.android.core.network.OrderDto
import com.eslamielectric.android.core.network.ShippingAddressRequest
import com.eslamielectric.android.core.network.mapApiException
import com.eslamielectric.android.util.StripeCheckoutTabs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException

data class CheckoutInput(
    val fulfillmentType: String,
    val locale: String = "en",
    val guestEmail: String? = null,
    val guestName: String? = null,
    val guestPhone: String? = null,
    val shippingAddress: ShippingAddressRequest? = null
)

sealed class CheckoutStartResult {
    data class Opened(val sessionId: String, val url: String) : CheckoutStartResult()
}

sealed class CryptoCheckoutStartResult {
    data class Ready(val payment: CreateCryptoPaymentResponse) : CryptoCheckoutStartResult()
}

sealed class CheckoutReturnResult {
    data class Paid(val order: OrderDto) : CheckoutReturnResult()
    data object NotPaid : CheckoutReturnResult()
    data class Error(val message: String) : CheckoutReturnResult()
}

sealed class CryptoPollResult {
    data class Finished(val order: OrderDto) : CryptoPollResult()
    data object Waiting : CryptoPollResult()
    data class Failed(val message: String) : CryptoPollResult()
    data class Error(val message: String) : CryptoPollResult()
}

/** Checkout: Stripe + NOWPayments crypto, Custom Tab, and post-payment confirmation. */
class CheckoutRepository(
    private val api: ApiService,
    private val basketRepository: BasketRepository,
    private val sessionStore: SessionStore,
    private val pendingCheckoutStore: PendingCheckoutStore
) {
    private val _checkoutStateRevision = MutableStateFlow(0)
    val checkoutStateRevision: StateFlow<Int> = _checkoutStateRevision.asStateFlow()

    fun isLoggedIn(): Boolean = sessionStore.isLoggedIn()

    fun getPendingSessionId(): String? = pendingCheckoutStore.getSessionId()

    fun getPendingCryptoPaymentId(): String? = pendingCheckoutStore.getCryptoPaymentId()

    fun getPendingEditOrder(): PendingCheckoutStore.PendingEditOrder? =
        pendingCheckoutStore.getPendingEditOrder()

    fun getPendingEditOrderLabel(): String? = getPendingEditOrder()?.orderLabel

    private fun markCheckoutStateChanged() {
        _checkoutStateRevision.value += 1
    }

    suspend fun loadPendingOrderForEdit(orderId: String): String {
        val token = sessionStore.getToken()
        if (token.isNullOrBlank()) throw CheckoutException.SessionExpired
        try {
            val draft = api.getBasketDraft(NetworkModule.bearer(token)!!, orderId)
            basketRepository.loadFromDraft(draft.basket)
            pendingCheckoutStore.setPendingEditOrder(
                orderId = draft.orderId,
                orderLabel = draft.orderNumber,
                fulfillmentType = draft.fulfillmentType,
                shippingAddress = draft.shippingAddress
            )
            markCheckoutStateChanged()
            return draft.orderNumber ?: draft.orderId
        } catch (e: HttpException) {
            if (e.code() == 401) {
                sessionStore.setToken(null)
                throw CheckoutException.SessionExpired
            }
            throw CheckoutException.Api(mapApiException(e).message, e.code())
        }
    }

    /**
     * Clears stale pending-order edit state when the order is no longer unpaid.
     * Basket tab should always reflect the live basket, not a completed order.
     */
    suspend fun reconcileStalePendingEdit() {
        val pending = pendingCheckoutStore.getPendingEditOrder() ?: run {
            markCheckoutStateChanged()
            return
        }
        val token = sessionStore.getToken()
        if (token.isNullOrBlank()) {
            markCheckoutStateChanged()
            return
        }
        try {
            val orders = api.getOrders(NetworkModule.bearer(token)!!)
            val order = orders.find { it.id == pending.orderId }
            if (order == null || order.status != "pending") {
                pendingCheckoutStore.clearPendingEditOrder()
            }
        } catch (_: Exception) {
            // Keep pending state on transient network errors.
        }
        markCheckoutStateChanged()
    }

    suspend fun finalizeSuccessfulCheckout(clearBasket: Boolean = true) {
        if (clearBasket) {
            basketRepository.clear()
        }
        pendingCheckoutStore.clearPendingEditOrder()
        pendingCheckoutStore.setSessionId(null)
        pendingCheckoutStore.setCryptoPaymentId(null)
        markCheckoutStateChanged()
    }

    suspend fun loadCryptoPayCurrencies(): CryptoPayCurrenciesResponse {
        return try {
            api.getCryptoPayCurrencies()
        } catch (e: HttpException) {
            throw CheckoutException.Api(mapApiException(e).message, e.code())
        }
    }

    private suspend fun resolvePendingOrderIdForCheckout(): String? {
        if (pendingCheckoutStore.getPendingEditOrder() == null) return null
        reconcileStalePendingEdit()
        return pendingCheckoutStore.getPendingEditOrder()?.orderId
    }

    private fun buildCheckoutRequest(
        input: CheckoutInput,
        lineItems: List<com.eslamielectric.android.core.network.CheckoutLineItemRequest>,
        pendingOrderId: String?,
        payCurrency: String? = null
    ): CreateCheckoutSessionRequest {
        val loggedIn = sessionStore.getToken()?.isNotBlank() == true
        return CreateCheckoutSessionRequest(
            lineItems = lineItems,
            locale = input.locale,
            fulfillmentType = input.fulfillmentType,
            guestEmail = if (!loggedIn) input.guestEmail?.trim()?.lowercase() else null,
            guestName = if (!loggedIn) input.guestName?.trim() else null,
            guestPhone = if (!loggedIn) input.guestPhone?.trim()?.ifBlank { null } else null,
            shippingAddress = input.shippingAddress,
            pendingOrderId = pendingOrderId,
            payCurrency = payCurrency
        )
    }

    private fun mapCheckoutHttpException(e: HttpException): Nothing {
        val apiEx = mapApiException(e)
        when {
            apiEx.httpCode == 401 && apiEx.code == "SESSION_EXPIRED" -> {
                sessionStore.setToken(null)
                throw CheckoutException.SessionExpired
            }
            apiEx.httpCode == 403 && apiEx.code == "PROFILE_INCOMPLETE" ->
                throw CheckoutException.ProfileIncomplete(apiEx.message, apiEx.missing.orEmpty())
            apiEx.httpCode == 429 ->
                throw CheckoutException.RateLimited(apiEx.message)
            else -> throw CheckoutException.Api(apiEx.message, apiEx.httpCode)
        }
    }

    suspend fun startCheckout(context: Context, input: CheckoutInput): CheckoutStartResult {
        val items = basketRepository.getItemsOnce()
        if (items.isEmpty()) {
            throw CheckoutException.EmptyBasket
        }

        val lineItems = basketRepository.toCheckoutLineItems(input.locale)
        val token = sessionStore.getToken()
        val auth = NetworkModule.bearer(token)
        val pendingOrderId = resolvePendingOrderIdForCheckout()

        try {
            val response = api.createCheckoutSession(
                authorization = auth,
                body = buildCheckoutRequest(input, lineItems, pendingOrderId)
            )
            pendingCheckoutStore.setSessionId(response.sessionId)
            markCheckoutStateChanged()
            StripeCheckoutTabs.open(context, response.url)
            return CheckoutStartResult.Opened(response.sessionId, response.url)
        } catch (e: HttpException) {
            mapCheckoutHttpException(e)
        }
    }

    suspend fun startCryptoCheckout(input: CheckoutInput, payCurrency: String): CryptoCheckoutStartResult {
        val items = basketRepository.getItemsOnce()
        if (items.isEmpty()) {
            throw CheckoutException.EmptyBasket
        }
        val ticker = payCurrency.trim()
        if (ticker.isBlank()) {
            throw CheckoutException.Api("Select a payment network.")
        }

        val lineItems = basketRepository.toCheckoutLineItems(input.locale)
        val token = sessionStore.getToken()
        val auth = NetworkModule.bearer(token)
        val pendingOrderId = resolvePendingOrderIdForCheckout()

        try {
            val response = api.createCryptoPayment(
                authorization = auth,
                body = buildCheckoutRequest(input, lineItems, pendingOrderId, payCurrency = ticker)
            )
            pendingCheckoutStore.setCryptoPaymentId(response.paymentId)
            markCheckoutStateChanged()
            return CryptoCheckoutStartResult.Ready(response)
        } catch (e: HttpException) {
            mapCheckoutHttpException(e)
        }
    }

    suspend fun pollCryptoPayment(paymentId: String): CryptoPollResult {
        return try {
            val status = api.getCryptoPaymentStatus(paymentId)
            when {
                status.status == "finished" || status.orderStatus == "paid" -> {
                    runCatching { api.confirmOrderByCrypto(paymentId) }
                    val order = api.getOrderByCryptoPayment(paymentId)
                    if (order.status == "paid") {
                        finalizeSuccessfulCheckout()
                        CryptoPollResult.Finished(order)
                    } else {
                        CryptoPollResult.Waiting
                    }
                }
                status.terminalFailure == true ->
                    CryptoPollResult.Failed("Payment failed or expired. Try again or pay by card.")
                else -> CryptoPollResult.Waiting
            }
        } catch (e: HttpException) {
            CryptoPollResult.Error(mapApiException(e).message)
        } catch (e: Exception) {
            CryptoPollResult.Error(e.message ?: "Could not check payment status.")
        }
    }

    suspend fun handleReturnFromStripe(sessionId: String): CheckoutReturnResult {
        return try {
            runCatching { api.confirmOrderBySession(sessionId) }
            val order = api.getOrderBySession(sessionId)
            if (order.status == "paid") {
                finalizeSuccessfulCheckout()
                CheckoutReturnResult.Paid(order)
            } else {
                pendingCheckoutStore.setSessionId(null)
                markCheckoutStateChanged()
                CheckoutReturnResult.NotPaid
            }
        } catch (e: HttpException) {
            CheckoutReturnResult.Error(mapApiException(e).message)
        } catch (e: Exception) {
            CheckoutReturnResult.Error(e.message ?: "Could not verify payment.")
        }
    }

    suspend fun handleReturnFromCrypto(paymentId: String): CheckoutReturnResult {
        return when (val poll = pollCryptoPayment(paymentId)) {
            is CryptoPollResult.Finished -> CheckoutReturnResult.Paid(poll.order)
            is CryptoPollResult.Failed -> {
                pendingCheckoutStore.setCryptoPaymentId(null)
                markCheckoutStateChanged()
                CheckoutReturnResult.Error(poll.message)
            }
            is CryptoPollResult.Error -> CheckoutReturnResult.Error(poll.message)
            CryptoPollResult.Waiting -> CheckoutReturnResult.NotPaid
        }
    }

    suspend fun clearPendingSession() {
        pendingCheckoutStore.setSessionId(null)
        markCheckoutStateChanged()
    }

    suspend fun clearPendingCryptoPayment() {
        pendingCheckoutStore.setCryptoPaymentId(null)
        markCheckoutStateChanged()
    }
}

sealed class CheckoutException(message: String) : Exception(message) {
    data object EmptyBasket : CheckoutException("Your basket is empty.")
    data object SessionExpired : CheckoutException("Session expired. Please log in again.")
    data class ProfileIncomplete(val errorMessage: String, val missing: List<String>) :
        CheckoutException(errorMessage)
    data class RateLimited(val errorMessage: String) : CheckoutException(errorMessage)
    data class Api(val errorMessage: String, val httpCode: Int = 0) :
        CheckoutException(errorMessage)
}
