package com.eslamielectric.android.feature.catalog

import com.eslamielectric.android.core.network.ApiService
import com.eslamielectric.android.core.network.CategoryDto
import com.eslamielectric.android.core.network.ProductDto

/** Catalog API wrapper for product grids. */
class CatalogRepository(private val api: ApiService) {

    private var cachedProducts: List<ProductDto> = emptyList()
    private var cachedCategories: List<CategoryDto> = emptyList()

    suspend fun loadCategories(): List<CategoryDto> {
        cachedCategories = api.getCategories()
        return cachedCategories
    }

    suspend fun loadProducts(): List<ProductDto> {
        cachedProducts = api.getProducts()
        return cachedProducts
    }

    fun getProductById(id: String): ProductDto? = cachedProducts.find { it.id == id }

    fun getCachedProducts(): List<ProductDto> = cachedProducts

    fun getCachedCategories(): List<CategoryDto> = cachedCategories
}