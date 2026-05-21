package com.eslamielectric.android.feature.basket

import android.content.Context
import com.eslamielectric.android.core.data.BasketRepository
import com.eslamielectric.android.core.data.PendingCheckoutStore
import com.eslamielectric.android.core.data.SessionStore
import com.eslamielectric.android.core.network.ApiService
import com.eslamielectric.android.core.network.CreateCheckoutSessionRequest
import com.eslamielectric.android.core.network.NetworkModule
import com.eslamielectric.android.core.network.OrderDto
import com.eslamielectric.android.core.network.ShippingAddressRequest
import com.eslamielectric.android.core.network.mapApiException
import com.eslamielectric.android.util.StripeCheckoutTabs
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

sealed class CheckoutReturnResult {
    data class Paid(val order: OrderDto) : CheckoutReturnResult()
    data object NotPaid : CheckoutReturnResult()
    data class Error(val message: String) : CheckoutReturnResult()
}

/** Checkout: create Stripe session, Custom Tab, and post-payment confirmation. */
class CheckoutRepository(
    private val api: ApiService,
    private val basketRepository: BasketRepository,
    private val sessionStore: SessionStore,
    private val pendingCheckoutStore: PendingCheckoutStore
) {
    fun isLoggedIn(): Boolean = sessionStore.isLoggedIn()

    fun getPendingSessionId(): String? = pendingCheckoutStore.getSessionId()

    suspend fun startCheckout(context: Context, input: CheckoutInput): CheckoutStartResult {
        val items = basketRepository.getItemsOnce()
        if (items.isEmpty()) {
            throw CheckoutException.EmptyBasket
        }

        val lineItems = basketRepository.toCheckoutLineItems(input.locale)
        val token = sessionStore.getToken()
        val auth = NetworkModule.bearer(token)

        try {
            val response = api.createCheckoutSession(
                authorization = auth,
                body = CreateCheckoutSessionRequest(
                    lineItems = lineItems,
                    locale = input.locale,
                    fulfillmentType = input.fulfillmentType,
                    guestEmail = input.guestEmail?.trim()?.lowercase(),
                    guestName = input.guestName?.trim(),
                    guestPhone = input.guestPhone?.trim()?.ifBlank { null },
                    shippingAddress = input.shippingAddress
                )
            )
            pendingCheckoutStore.setSessionId(response.sessionId)
            StripeCheckoutTabs.open(context, response.url)
            return CheckoutStartResult.Opened(response.sessionId, response.url)
        } catch (e: HttpException) {
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
    }

    suspend fun handleReturnFromStripe(sessionId: String): CheckoutReturnResult {
        return try {
            runCatching { api.confirmOrderBySession(sessionId) }
            val order = api.getOrderBySession(sessionId)
            if (order.status == "paid") {
                basketRepository.clear()
                pendingCheckoutStore.setSessionId(null)
                CheckoutReturnResult.Paid(order)
            } else {
                pendingCheckoutStore.setSessionId(null)
                CheckoutReturnResult.NotPaid
            }
        } catch (e: HttpException) {
            CheckoutReturnResult.Error(mapApiException(e).message)
        } catch (e: Exception) {
            CheckoutReturnResult.Error(e.message ?: "Could not verify payment.")
        }
    }

    suspend fun clearPendingSession() {
        pendingCheckoutStore.setSessionId(null)
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
