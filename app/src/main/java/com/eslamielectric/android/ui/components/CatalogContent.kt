package com.eslamielectric.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.eslamielectric.android.R
import com.eslamielectric.android.core.network.ProductDto
import com.eslamielectric.android.feature.catalog.CatalogUiState
import com.eslamielectric.android.util.displayCategory
import com.eslamielectric.android.util.displayName
import com.eslamielectric.android.util.formatPriceUsd
import com.eslamielectric.android.util.imageContentDescription

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CatalogStateContent(
    state: CatalogUiState,
    locale: String,
    onRetry: () -> Unit,
    onAddToBasket: (ProductDto, Int) -> Unit,
    onProductClick: (ProductDto) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    productsFilter: (List<ProductDto>) -> List<ProductDto> = { it },
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    showQuantityControls: Boolean = false
) {
    val refreshing = isRefreshing || state is CatalogUiState.Loading
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { onRefresh?.invoke() ?: onRetry() }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (onRefresh != null || state !is CatalogUiState.Loading) Modifier.pullRefresh(pullRefreshState) else Modifier)
    ) {
        when (state) {
            CatalogUiState.Loading -> CatalogLoadingGrid(modifier = Modifier.fillMaxSize())
            CatalogUiState.Empty -> CatalogEmpty(modifier = Modifier.fillMaxSize())
            is CatalogUiState.Error -> CatalogError(state.message, onRetry, Modifier.fillMaxSize())
            is CatalogUiState.Success -> {
                val products = productsFilter(state.products)
                if (products.isEmpty()) {
                    CatalogEmpty(Modifier.fillMaxSize())
                } else {
                    ProductGrid(
                        products = products,
                        locale = locale,
                        onAddToBasket = onAddToBasket,
                        onProductClick = onProductClick,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = contentPadding,
                        showQuantityControls = showQuantityControls
                    )
                }
            }
        }
        if (onRefresh != null || state !is CatalogUiState.Loading) {
            PullRefreshIndicator(
                refreshing = refreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun CatalogLoadingGrid(
    modifier: Modifier = Modifier,
    itemCount: Int = 6
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 168.dp),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(itemCount) {
            ProductCardSkeleton()
        }
    }
}

@Composable
fun ProductCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {}
            Column(modifier = Modifier.padding(12.dp)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {}
                Surface(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(0.5f)
                        .height(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {}
                Surface(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(0.4f)
                        .height(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {}
            }
        }
    }
}

@Composable
fun CatalogEmpty(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.catalog_empty),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(24.dp)
        )
    }
}

@Composable
fun CatalogError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.catalog_error_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp)
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
fun ProductGrid(
    products: List<ProductDto>,
    locale: String,
    onAddToBasket: (ProductDto, Int) -> Unit,
    onProductClick: (ProductDto) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    showQuantityControls: Boolean = false
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 168.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(products, key = { it.id }) { product ->
            ProductCard(
                product = product,
                locale = locale,
                onAddToBasket = { quantity -> onAddToBasket(product, quantity) },
                onClick = { onProductClick(product) },
                modifier = Modifier.fillMaxWidth(),
                showQuantityControls = showQuantityControls
            )
        }
    }
}

@Composable
fun ProductCard(
    product: ProductDto,
    locale: String,
    onAddToBasket: (Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showQuantityControls: Boolean = false
) {
    var quantity by rememberSaveable(product.id) { mutableIntStateOf(1) }

    Card(
        modifier = modifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            val imageUrl = product.imageUrl.takeIf { it.isNotBlank() }
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = product.imageContentDescription(locale),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = product.displayName(locale).take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(24.dp)
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.displayName(locale),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                product.displayCategory(locale)?.let { category ->
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatPriceUsd(product.price),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (showQuantityControls) {
                    CompactQuantityStepper(
                        quantity = quantity,
                        onQuantityChange = { quantity = it },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Button(
                    onClick = { onAddToBasket(if (showQuantityControls) quantity else 1) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.add_to_basket))
                }
            }
        }
    }
}
