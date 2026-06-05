package com.eslamielectric.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eslamielectric.android.R
import com.eslamielectric.android.core.data.BasketRepository
import com.eslamielectric.android.core.network.CategoryDto
import com.eslamielectric.android.core.network.ProductDto
import com.eslamielectric.android.feature.catalog.CatalogRepository
import com.eslamielectric.android.feature.catalog.CatalogUiState
import com.eslamielectric.android.feature.catalog.CatalogViewModel
import com.eslamielectric.android.ui.components.CatalogEmpty
import com.eslamielectric.android.ui.components.CatalogError
import com.eslamielectric.android.ui.components.CatalogLoadingGrid
import com.eslamielectric.android.ui.components.CatalogStateContent
import com.eslamielectric.android.ui.components.ProductGrid
import com.eslamielectric.android.util.displayCategory
import com.eslamielectric.android.util.displayName
import kotlinx.coroutines.launch

private const val HOME_FEATURED_COUNT = 6

private enum class ProductSort {
    DEFAULT,
    PRICE_ASC,
    PRICE_DESC,
    NAME_ASC
}

@Composable
fun ProductsScreen(
    catalogRepository: CatalogRepository,
    basketRepository: BasketRepository,
    locale: String,
    onProductClick: (ProductDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: CatalogViewModel = viewModel(factory = CatalogViewModel.factory(catalogRepository))
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val scope = rememberCoroutineScope()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var sortMode by rememberSaveable { mutableStateOf(ProductSort.DEFAULT) }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("field_search_products"),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.products_search_hint)) }
        )
        val categories = (uiState as? CatalogUiState.Success)?.categories.orEmpty()
        if (categories.isNotEmpty()) {
            CategoryChipRow(
                categories = categories,
                locale = locale,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { selectedCategoryId = it },
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        SortChipRow(
            sortMode = sortMode,
            onSortSelected = { sortMode = it },
            modifier = Modifier.padding(bottom = 8.dp)
        )
        CatalogStateContent(
            state = uiState,
            locale = locale,
            onRetry = viewModel::refresh,
            onRefresh = viewModel::refresh,
            isRefreshing = isRefreshing,
            onAddToBasket = { product, quantity ->
                scope.launch { basketRepository.addProduct(product, product.categoryId, quantity) }
            },
            onProductClick = onProductClick,
            productsFilter = { products ->
                sortProducts(
                    filterProducts(products, locale, searchQuery, selectedCategoryId),
                    locale,
                    sortMode
                )
            },
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            showQuantityControls = true
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    catalogRepository: CatalogRepository,
    basketRepository: BasketRepository,
    locale: String,
    onViewAllProducts: () -> Unit,
    onProductClick: (ProductDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: CatalogViewModel = viewModel(factory = CatalogViewModel.factory(catalogRepository))
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val scope = rememberCoroutineScope()
    val onAdd: (ProductDto, Int) -> Unit = { product, quantity ->
        scope.launch { basketRepository.addProduct(product, product.categoryId, quantity) }
    }
    val refreshing = isRefreshing || uiState is CatalogUiState.Loading
    val pullRefreshState = rememberPullRefreshState(refreshing = refreshing, onRefresh = viewModel::refresh)

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        when (val state = uiState) {
            CatalogUiState.Loading -> CatalogLoadingGrid(modifier = Modifier.fillMaxSize())
            CatalogUiState.Empty -> CatalogEmpty(modifier = Modifier.fillMaxSize())
            is CatalogUiState.Error -> CatalogError(state.message, viewModel::refresh, modifier)
            is CatalogUiState.Success -> {
                val featured = state.products.take(HOME_FEATURED_COUNT)
                if (featured.isEmpty()) {
                    CatalogEmpty(modifier)
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.home_featured),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.testTag("home_featured_title")
                            )
                            TextButton(
                                onClick = onViewAllProducts,
                                modifier = Modifier.testTag("btn_view_all_products")
                            ) {
                                Text(stringResource(R.string.view_all_products))
                            }
                        }
                        ProductGrid(
                            products = featured,
                            locale = locale,
                            onAddToBasket = onAdd,
                            onProductClick = onProductClick,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            showQuantityControls = true
                        )
                    }
                }
            }
        }
        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun CategoryChipRow(
    categories: List<CategoryDto>,
    locale: String,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) },
                label = { Text(stringResource(R.string.products_all_categories)) },
                modifier = Modifier.testTag("chip_category_all")
            )
        }
        items(categories, key = { it.id }) { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) },
                label = { Text(category.displayName(locale)) }
            )
        }
    }
}

@Composable
private fun SortChipRow(
    sortMode: ProductSort,
    onSortSelected: (ProductSort) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = sortMode == ProductSort.DEFAULT,
                onClick = { onSortSelected(ProductSort.DEFAULT) },
                label = { Text(stringResource(R.string.products_sort_default)) },
                modifier = Modifier.testTag("chip_sort_default")
            )
        }
        item {
            FilterChip(
                selected = sortMode == ProductSort.PRICE_ASC,
                onClick = { onSortSelected(ProductSort.PRICE_ASC) },
                label = { Text(stringResource(R.string.products_sort_price_asc)) }
            )
        }
        item {
            FilterChip(
                selected = sortMode == ProductSort.PRICE_DESC,
                onClick = { onSortSelected(ProductSort.PRICE_DESC) },
                label = { Text(stringResource(R.string.products_sort_price_desc)) }
            )
        }
        item {
            FilterChip(
                selected = sortMode == ProductSort.NAME_ASC,
                onClick = { onSortSelected(ProductSort.NAME_ASC) },
                label = { Text(stringResource(R.string.products_sort_name_asc)) }
            )
        }
    }
}

private fun filterProducts(
    products: List<ProductDto>,
    locale: String,
    searchQuery: String,
    categoryId: String?
): List<ProductDto> {
    val query = searchQuery.trim().lowercase()
    return products.filter { product ->
        val matchesCategory = categoryId == null || product.categoryId == categoryId
        val matchesSearch = query.isEmpty() ||
            product.displayName(locale).lowercase().contains(query) ||
            product.name.lowercase().contains(query) ||
            product.nameFa.lowercase().contains(query) ||
            product.displayCategory(locale)?.lowercase()?.contains(query) == true
        matchesCategory && matchesSearch
    }
}

private fun sortProducts(
    products: List<ProductDto>,
    locale: String,
    sortMode: ProductSort
): List<ProductDto> = when (sortMode) {
    ProductSort.DEFAULT -> products
    ProductSort.PRICE_ASC -> products.sortedBy { it.price }
    ProductSort.PRICE_DESC -> products.sortedByDescending { it.price }
    ProductSort.NAME_ASC -> products.sortedBy { it.displayName(locale).lowercase() }
}
