package com.eslamielectric.android.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors web localStorage `basket` entries (public/js/products-page.js). */
@Serializable
data class BasketItem(
    val id: String,
    @SerialName("categoryId") val categoryId: String? = null,
    val name: String,
    @SerialName("name_fa") val nameFa: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val price: Double,
    val quantity: Int = 1
) {
    fun lineTotal(): Double = price * quantity
}
