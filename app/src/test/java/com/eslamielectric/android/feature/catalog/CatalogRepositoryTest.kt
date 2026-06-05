package com.eslamielectric.android.feature.catalog

import com.eslamielectric.android.core.network.ApiService
import com.eslamielectric.android.core.network.CategoryDto
import com.eslamielectric.android.core.network.ProductDto
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CatalogRepositoryTest {

    private val api = mockk<ApiService>()
    private lateinit var repository: CatalogRepository

    @Before
    fun setUp() {
        repository = CatalogRepository(api)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun loadProducts_cachesResults() = runTest {
        val products = listOf(ProductDto(id = "p1", name = "Bulb"))
        coEvery { api.getProducts() } returns products
        val loaded = repository.loadProducts()
        assertEquals(products, loaded)
        assertEquals(products, repository.getCachedProducts())
        assertEquals("Bulb", repository.getProductById("p1")?.name)
    }

    @Test
    fun getProductById_returnsNullWhenMissing() = runTest {
        coEvery { api.getProducts() } returns emptyList()
        repository.loadProducts()
        assertNull(repository.getProductById("missing"))
    }

    @Test
    fun loadCategories_cachesCategories() = runTest {
        val categories = listOf(CategoryDto(id = "c1", name = "Lighting"))
        coEvery { api.getCategories() } returns categories
        assertEquals(categories, repository.loadCategories())
        assertEquals(categories, repository.getCachedCategories())
    }
}
