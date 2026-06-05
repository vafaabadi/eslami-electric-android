package com.eslamielectric.android.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.eslamielectric.android.R
import com.eslamielectric.android.core.data.BasketRepository
import com.eslamielectric.android.core.network.ProductDto
import com.eslamielectric.android.feature.catalog.CatalogRepository
import com.eslamielectric.android.feature.catalog.ProductDetailUiState
import com.eslamielectric.android.feature.catalog.ProductDetailViewModel
import com.eslamielectric.android.util.displayCategory
import com.eslamielectric.android.util.displayDescription
import com.eslamielectric.android.util.displayName
import com.eslamielectric.android.util.formatPriceUsd
import com.eslamielectric.android.ui.components.CompactQuantityStepper
import com.eslamielectric.android.util.imageContentDescription
import com.eslamielectric.android.util.resolveProductImageUrl
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    catalogRepository: CatalogRepository,
    basketRepository: BasketRepository,
    locale: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ProductDetailViewModel = viewModel(
        factory = ProductDetailViewModel.factory(catalogRepository, productId)
    )
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.testTag("screen_product_detail"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.product_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        when (val ui = state) {
            ProductDetailUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(48.dp))
                    CircularProgressIndicator()
                }
            }
            is ProductDetailUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(ui.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is ProductDetailUiState.Ready -> {
                ProductDetailContent(
                    product = ui.product,
                    locale = locale,
                    onAddToBasket = { quantity ->
                        scope.launch {
                            basketRepository.addProduct(ui.product, ui.product.categoryId, quantity)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }
}

@Composable
private fun ProductDetailContent(
    product: ProductDto,
    locale: String,
    onAddToBasket: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var quantity by rememberSaveable { mutableIntStateOf(1) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        val imageUrl = resolveProductImageUrl(product.imageUrl)
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = product.imageContentDescription(locale),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = product.displayName(locale),
                style = MaterialTheme.typography.headlineSmall
            )
            product.displayCategory(locale)?.let { category ->
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Text(
                text = formatPriceUsd(product.price),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp)
            )
            val description = product.displayDescription(locale)
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            CompactQuantityStepper(
                quantity = quantity,
                onQuantityChange = { quantity = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            )
            Button(
                onClick = { onAddToBasket(quantity) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .testTag("btn_add_to_basket")
            ) {
                Text(stringResource(R.string.add_to_basket))
            }
        }
    }
}
