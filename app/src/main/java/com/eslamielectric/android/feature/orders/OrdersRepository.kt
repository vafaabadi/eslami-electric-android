package com.eslamielectric.android.feature.orders

import android.content.Context
import com.eslamielectric.android.core.data.SessionStore
import com.eslamielectric.android.core.network.ApiService
import com.eslamielectric.android.core.network.GuestOrderTokenRequest
import com.eslamielectric.android.core.network.NetworkModule
import com.eslamielectric.android.core.network.OrderDto
import com.eslamielectric.android.core.network.ResumeCheckoutRequest
import com.eslamielectric.android.core.network.SessionExpiredException
import com.eslamielectric.android.core.network.mapApiException
import com.eslamielectric.android.util.StripeCheckoutTabs
import retrofit2.HttpException

/** Authenticated and guest order APIs — see docs/mobile-api.md */
class OrdersRepository(
    private val api: ApiService,
    private val sessionStore: SessionStore
) {
    private var cachedGuestOrder: OrderDto? = null
    private var cachedGuestToken: String? = null

    fun isLoggedIn(): Boolean = sessionStore.isLoggedIn()

    fun cacheGuestOrder(order: OrderDto, guestToken: String? = null) {
        cachedGuestOrder = order
        cachedGuestToken = guestToken
    }

    fun getCachedGuestOrder(orderId: String): OrderDto? =
        cachedGuestOrder?.takeIf { it.id == orderId }

    fun getCachedGuestToken(orderId: String): String? =
        cachedGuestToken?.takeIf { cachedGuestOrder?.id == orderId }

    fun clearGuestCache() {
        cachedGuestOrder = null
        cachedGuestToken = null
    }

    suspend fun loadMyOrders(): List<OrderDto> = withAuth { token ->
        api.getOrders(NetworkModule.bearer(token)!!)
    }

    suspend fun findOrder(orderId: String): OrderDto? {
        val orders = loadMyOrders()
        return orders.find { it.id == orderId }
    }

    suspend fun cancelOrder(orderId: String) = withAuth { token ->
        api.cancelOrder(NetworkModule.bearer(token)!!, orderId)
    }

    suspend fun resumeCheckout(context: Context, orderId: String, locale: String): String =
        withAuth { token ->
            val response = api.resumeCheckout(
                authorization = NetworkModule.bearer(token)!!,
                orderId = orderId,
                body = ResumeCheckoutRequest(locale = locale)
            )
            StripeCheckoutTabs.open(context, response.url)
            response.url
        }

    suspend fun guestLookup(email: String, orderIdOrNumber: String): OrderDto {
        try {
            val order = api.guestOrderLookup(
                email = email.trim().lowercase(),
                orderId = orderIdOrNumber.trim()
            )
            cacheGuestOrder(order, guestToken = null)
            return order
        } catch (e: HttpException) {
            throw mapApiException(e)
        }
    }

    suspend fun loadGuestByToken(token: String): OrderDto {
        try {
            val order = api.getGuestOrder(token.trim())
            cacheGuestOrder(order, guestToken = token.trim())
            return order
        } catch (e: HttpException) {
            throw mapApiException(e)
        }
    }

    suspend fun guestCancel(token: String) {
        try {
            api.guestCancel(GuestOrderTokenRequest(token = token.trim()))
        } catch (e: HttpException) {
            throw mapApiException(e)
        }
    }

    suspend fun guestResumeCheckout(context: Context, token: String, locale: String): String {
        try {
            val response = api.guestResumeCheckout(
                GuestOrderTokenRequest(token = token.trim(), locale = locale)
            )
            StripeCheckoutTabs.open(context, response.url)
            return response.url
        } catch (e: HttpException) {
            throw mapApiException(e)
        }
    }

    suspend fun logoutOnSessionExpired() {
        sessionStore.setToken(null)
    }

    private suspend fun <T> withAuth(block: suspend (token: String) -> T): T {
        val token = sessionStore.getToken()
        if (token.isNullOrBlank()) throw SessionExpiredException()
        return try {
            block(token)
        } catch (e: HttpException) {
            if (e.code() == 401) {
                sessionStore.setToken(null)
                throw SessionExpiredException()
            }
            throw mapApiException(e)
        }
    }
}

sealed class OrdersException(message: String) : Exception(message) {
    data object SessionExpired : OrdersException("Session expired. Please log in again.")
    data class ProfileIncomplete(val errorMessage: String, val missing: List<String>) :
        OrdersException(errorMessage)
    data class Api(val errorMessage: String, val httpCode: Int = 0) :
        OrdersException(errorMessage)
}

fun mapOrdersResumeException(e: Throwable): OrdersException = when (e) {
    is SessionExpiredException -> OrdersException.SessionExpired
    is com.eslamielectric.android.core.network.ApiException -> {
        when {
            e.httpCode == 403 && e.code == "PROFILE_INCOMPLETE" ->
                OrdersException.ProfileIncomplete(e.message, e.missing.orEmpty())
            else -> OrdersException.Api(e.message, e.httpCode)
        }
    }
    is OrdersException -> e
    else -> OrdersException.Api(e.message ?: "Request failed.")
}
