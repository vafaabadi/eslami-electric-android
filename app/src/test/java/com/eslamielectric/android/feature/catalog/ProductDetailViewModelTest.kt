package com.eslamielectric.android.feature.catalog

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.eslamielectric.android.core.network.ApiService
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.SocketTimeoutException

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelTest {

    @get:Rule
    val instantTask = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

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
    fun load_findsProductInCacheWithoutReload() = runTest {
        coEvery { api.getProducts() } returns listOf(ProductDto(id = "p1", name = "Bulb"))
        repository.loadProducts()
        val viewModel = ProductDetailViewModel(repository, "p1")
        advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is ProductDetailUiState.Ready)
        assertEquals("Bulb", (state as ProductDetailUiState.Ready).product.name)
    }

    @Test
    fun load_fetchesCatalogWhenCacheEmpty() = runTest {
        coEvery { api.getProducts() } returns listOf(ProductDto(id = "p2", name = "Cable"))
        val viewModel = ProductDetailViewModel(repository, "p2")
        advanceUntilIdle()
        val state = viewModel.state.value as ProductDetailUiState.Ready
        assertEquals("Cable", state.product.name)
    }

    @Test
    fun load_emitsNotFoundWhenStillMissingAfterReload() = runTest {
        coEvery { api.getProducts() } returns emptyList()
        val viewModel = ProductDetailViewModel(repository, "missing")
        advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is ProductDetailUiState.Error)
        assertEquals("Product not found.", (state as ProductDetailUiState.Error).message)
    }

    @Test
    fun load_mapsTimeoutToNetworkHint() = runTest {
        coEvery { api.getProducts() } throws SocketTimeoutException("timeout")
        val viewModel = ProductDetailViewModel(repository, "p1")
        advanceUntilIdle()
        val state = viewModel.state.value as ProductDetailUiState.Error
        assertTrue(state.message.contains("Could not reach the store"))
    }
}
