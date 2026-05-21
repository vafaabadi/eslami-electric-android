package com.eslamielectric.android.ui.navigation

object CatalogRoutes {
    const val PRODUCT_DETAIL = "product/{productId}"
    const val GUEST_ORDER_BY_TOKEN = "guest-order-token/{token}"

    fun productDetail(productId: String) = "product/$productId"

    fun guestOrderByToken(token: String) = "guest-order-token/$token"
}
