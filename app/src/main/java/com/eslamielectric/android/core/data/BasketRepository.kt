package com.eslamielectric.android.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.eslamielectric.android.core.network.CheckoutLineItemRequest
import com.eslamielectric.android.core.network.ProductDto
import com.eslamielectric.android.util.resolveProductImageUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.basketDataStore by preferencesDataStore(name = "eslami_basket_prefs")

class BasketRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val basketKey = stringPreferencesKey("basket")

    val itemsFlow: Flow<List<BasketItem>> = context.basketDataStore.data.map { prefs ->
        decode(prefs[basketKey])
    }

    suspend fun setItems(items: List<BasketItem>) {
        context.basketDataStore.edit { prefs ->
            prefs[basketKey] = json.encodeToString(items)
        }
    }

    suspend fun addProduct(product: ProductDto, categoryId: String?, quantity: Int = 1) {
        val items = getItemsOnce().toMutableList()
        val existing = items.find { it.id == product.id }
        if (existing != null) {
            val idx = items.indexOf(existing)
            items[idx] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            items.add(
                BasketItem(
                    id = product.id,
                    categoryId = categoryId ?: product.categoryId,
                    name = product.name,
                    nameFa = product.nameFa.ifBlank { null },
                    imageUrl = resolveProductImageUrl(product.imageUrl),
                    price = product.price,
                    quantity = quantity
                )
            )
        }
        setItems(items)
    }

    suspend fun getItemsOnce(): List<BasketItem> = itemsFlow.first()

    suspend fun clear() = setItems(emptyList())

    suspend fun updateQuantity(productId: String, quantity: Int) {
        if (quantity <= 0) {
            removeItem(productId)
            return
        }
        val items = getItemsOnce().map { item ->
            if (item.id == productId) item.copy(quantity = quantity) else item
        }
        setItems(items)
    }

    suspend fun removeItem(productId: String) {
        setItems(getItemsOnce().filter { it.id != productId })
    }

    suspend fun toCheckoutLineItems(locale: String): List<CheckoutLineItemRequest> {
        return getItemsOnce().map { item ->
            val displayName = if (locale == "fa" && !item.nameFa.isNullOrBlank()) item.nameFa else item.name
            CheckoutLineItemRequest(
                name = displayName.ifBlank { "Item" },
                price = item.price,
                quantity = item.quantity,
                productId = item.id
            )
        }
    }

    fun itemCount(items: List<BasketItem>): Int = items.sumOf { it.quantity }

    fun subtotal(items: List<BasketItem>): Double = items.sumOf { it.lineTotal() }

    private fun decode(raw: String?): List<BasketItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<BasketItem>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
