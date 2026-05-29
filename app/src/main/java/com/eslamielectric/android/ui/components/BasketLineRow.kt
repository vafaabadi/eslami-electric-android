package com.eslamielectric.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.eslamielectric.android.core.data.BasketItem
import com.eslamielectric.android.util.formatPriceUsd
import com.eslamielectric.android.util.resolveProductImageUrl

@Composable
fun BasketLineRow(
    item: BasketItem,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onSetQuantity: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditDialog by rememberSaveable { mutableStateOf(false) }

    if (showEditDialog) {
        QuantityEditDialog(
            initialQuantity = item.quantity,
            onConfirm = { value ->
                onSetQuantity(value)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val imageUrl = resolveProductImageUrl(item.imageUrl)
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.size(72.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatPriceUsd(item.price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = formatPriceUsd(item.lineTotal()),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    IconButton(
                        onClick = onDecrease,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = stringResource(R.string.decrease_quantity)
                        )
                    }
                    Text(
                        text = item.quantity.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .clickable { showEditDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    IconButton(
                        onClick = onIncrease,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.increase_quantity)
                        )
                    }
                    TextButton(onClick = onRemove, modifier = Modifier.padding(start = 4.dp)) {
                        Text(stringResource(R.string.basket_remove))
                    }
                }
            }
        }
    }
}
