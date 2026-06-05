package com.eslamielectric.android.feature.catalog

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.eslamielectric.android.core.network.ApiService
import com.eslamielectric.android.core.network.CategoryDto
import com.eslamielectric.android.core.network.ProductDto
import com.eslamielectric.android.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    @get:Rule
    val instantTask = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val api = mockk<ApiService>()
    private lateinit var repository: CatalogRepository
    private lateinit var viewModel: CatalogViewModel

    @Before
    fun setUp() {
        repository = CatalogRepository(api)
        viewModel = CatalogViewModel(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun refresh_emitsSuccessWithProductsAndCategories() = runTest {
        val products = listOf(
            ProductDto(id = "p1", name = "Bulb", categoryId = "c1", category = "Lighting")
        )
        val categories = listOf(CategoryDto(id = "c1", name = "Lighting"))
        coEvery { api.getProducts() } returns products
        coEvery { api.getCategories() } returns categories
        viewModel.refresh()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is CatalogUiState.Success)
        val success = state as CatalogUiState.Success
        assertEquals(1, success.products.size)
        assertEquals("Lighting", success.categories.single().name)
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun refresh_emitsEmptyWhenNoProducts() = runTest {
        coEvery { api.getProducts() } returns emptyList()
        coEvery { api.getCategories() } returns emptyList()
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(CatalogUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun refresh_derivesCategoriesWhenApiFails() = runTest {
        val products = listOf(
            ProductDto(id = "p1", name = "A", categoryId = "c1", category = "Cat A", categoryFa = "فا"),
            ProductDto(id = "p2", name = "B", categoryId = "c1", category = "Cat A"),
            ProductDto(id = "p3", name = "C", categoryId = null)
        )
        coEvery { api.getProducts() } returns products
        coEvery { api.getCategories() } throws IOException("offline")
        viewModel.refresh()
        advanceUntilIdle()
        val state = viewModel.uiState.value as CatalogUiState.Success
        assertEquals(1, state.categories.size)
        assertEquals("Cat A", state.categories.single().name)
        assertEquals("فا", state.categories.single().nameFa)
    }

    @Test
    fun refresh_mapsNetworkError() = runTest {
        coEvery { api.getProducts() } throws IOException("no host")
        viewModel.refresh()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is CatalogUiState.Error)
        assertTrue((state as CatalogUiState.Error).message.contains("Could not reach the store"))
    }
}
