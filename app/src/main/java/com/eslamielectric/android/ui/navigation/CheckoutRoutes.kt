package com.eslamielectric.android.ui.navigation

object CheckoutRoutes {
    const val CHECKOUT = "checkout"
    const val RESULT = "checkout_result/{success}/{orderNumber}"

    fun result(success: Boolean, orderNumber: String?): String {
        val num = orderNumber?.takeIf { it.isNotBlank() } ?: "-"
        return "checkout_result/$success/$num"
    }
}
