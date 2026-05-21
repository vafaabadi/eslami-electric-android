package com.eslamielectric.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eslamielectric.android.R
import com.eslamielectric.android.core.data.BasketItem
import com.eslamielectric.android.core.data.BasketRepository
import com.eslamielectric.android.ui.components.BasketLineRow
import com.eslamielectric.android.util.formatPriceUsd
import kotlinx.coroutines.launch

@Composable
fun BasketScreen(
    items: List<BasketItem>,
    basketRepository: BasketRepository,
    onProceedToCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    if (items.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.placeholder_basket), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { item ->
                BasketLineRow(
                    item = item,
                    onDecrease = {
                        scope.launch {
                            basketRepository.updateQuantity(item.id, item.quantity - 1)
                        }
                    },
                    onIncrease = {
                        scope.launch {
                            basketRepository.updateQuantity(item.id, item.quantity + 1)
                        }
                    },
                    onRemove = {
                        scope.launch { basketRepository.removeItem(item.id) }
                    }
                )
            }
        }
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.basket_total),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = formatPriceUsd(basketRepository.subtotal(items)),
                style = MaterialTheme.typography.titleLarge
            )
        }
        Button(
            onClick = onProceedToCheckout,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(stringResource(R.string.checkout_proceed))
        }
    }
}

@Composable
private fun PlaceholderCenter(textRes: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(textRes), style = MaterialTheme.typography.bodyLarge)
    }
}
