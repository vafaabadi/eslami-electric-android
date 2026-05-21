package com.eslamielectric.android.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.eslamielectric.android.core.network.ProductDto
import com.eslamielectric.android.core.network.CategoryDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed interface CatalogUiState {
    data object Loading : CatalogUiState
    data object Empty : CatalogUiState
    data class Success(
        val products: List<ProductDto>,
        val categories: List<CategoryDto> = emptyList()
    ) : CatalogUiState
    data class Error(val message: String) : CatalogUiState
}

sealed interface ProductDetailUiState {
    data object Loading : ProductDetailUiState
    data class Ready(val product: ProductDto) : ProductDetailUiState
    data class Error(val message: String) : ProductDetailUiState
}

class ProductDetailViewModel(
    private val catalogRepository: CatalogRepository,
    private val productId: String
) : ViewModel() {

    private val _state = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val state: StateFlow<ProductDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = ProductDetailUiState.Loading
            try {
                var product = catalogRepository.getProductById(productId)
                if (product == null) {
                    catalogRepository.loadProducts()
                    product = catalogRepository.getProductById(productId)
                }
                _state.value = if (product != null) {
                    ProductDetailUiState.Ready(product)
                } else {
                    ProductDetailUiState.Error("Product not found.")
                }
            } catch (e: Exception) {
                _state.value = ProductDetailUiState.Error(mapCatalogError(e))
            }
        }
    }

    companion object {
        fun factory(catalogRepository: CatalogRepository, productId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ProductDetailViewModel::class.java)) {
                        return ProductDetailViewModel(catalogRepository, productId) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
                }
            }
    }
}

class CatalogViewModel(
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = _uiState.value is CatalogUiState.Success
            if (_uiState.value !is CatalogUiState.Success) {
                _uiState.value = CatalogUiState.Loading
            }
            try {
                val products = catalogRepository.loadProducts()
                val categories = try {
                    catalogRepository.loadCategories()
                } catch (_: Exception) {
                    deriveCategories(products)
                }
                _uiState.value = if (products.isEmpty()) {
                    CatalogUiState.Empty
                } else {
                    CatalogUiState.Success(products = products, categories = categories)
                }
            } catch (e: Exception) {
                _uiState.value = CatalogUiState.Error(mapCatalogError(e))
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun deriveCategories(products: List<ProductDto>): List<CategoryDto> {
        return products
            .mapNotNull { p ->
                val id = p.categoryId ?: return@mapNotNull null
                CategoryDto(
                    id = id,
                    name = p.category.orEmpty(),
                    nameFa = p.categoryFa.orEmpty()
                )
            }
            .distinctBy { it.id }
    }

    companion object {
        fun factory(catalogRepository: CatalogRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
                        return CatalogViewModel(catalogRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
                }
            }
    }
}

fun mapCatalogError(e: Throwable): String = when (e) {
    is UnknownHostException,
    is SocketTimeoutException,
    is IOException -> NETWORK_HINT
    else -> e.message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
}

private const val NETWORK_HINT =
    "Could not reach the store. Start the web API on your computer (npm start in cursor-my-web-app), " +
        "then retry. Emulator uses http://10.0.2.2:3000."
