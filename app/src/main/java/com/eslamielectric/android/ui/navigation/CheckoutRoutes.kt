package com.eslamielectric.android.ui.navigation

object CheckoutRoutes {
    const val CHECKOUT = "checkout"
    const val RESULT = "checkout_result/{success}/{orderNumber}/{orderId}/{guestToken}"

    fun result(
        success: Boolean,
        orderNumber: String?,
        orderId: String? = null,
        guestToken: String? = null
    ): String {
        val num = orderNumber?.takeIf { it.isNotBlank() } ?: "-"
        val id = orderId?.takeIf { it.isNotBlank() } ?: "-"
        val token = guestToken?.takeIf { it.isNotBlank() } ?: "-"
        return "checkout_result/$success/$num/$id/$token"
    }
}
