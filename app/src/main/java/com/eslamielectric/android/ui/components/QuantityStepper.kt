package com.eslamielectric.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eslamielectric.android.R

private const val MIN_QUANTITY = 1
private const val MAX_QUANTITY = 9999

@Composable
fun CompactQuantityStepper(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minQuantity: Int = MIN_QUANTITY,
    maxQuantity: Int = MAX_QUANTITY
) {
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    val clamped = quantity.coerceIn(minQuantity, maxQuantity)

    if (showEditDialog) {
        QuantityEditDialog(
            initialQuantity = clamped,
            minQuantity = minQuantity,
            maxQuantity = maxQuantity,
            onConfirm = { value ->
                onQuantityChange(value)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { if (clamped > minQuantity) onQuantityChange(clamped - 1) },
            enabled = clamped > minQuantity,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = stringResource(R.string.decrease_quantity)
            )
        }
        Text(
            text = clamped.toString(),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .clickable { showEditDialog = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        IconButton(
            onClick = { if (clamped < maxQuantity) onQuantityChange(clamped + 1) },
            enabled = clamped < maxQuantity,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.increase_quantity)
            )
        }
    }
}

@Composable
private fun QuantityEditDialog(
    initialQuantity: Int,
    minQuantity: Int,
    maxQuantity: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var input by rememberSaveable { mutableStateOf(initialQuantity.toString()) }
    val parsed = input.trim().toIntOrNull()
    val isValid = parsed != null && parsed in minQuantity..maxQuantity

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quantity_edit_title)) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { newValue ->
                    input = newValue.filter { it.isDigit() }.take(4)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text(stringResource(R.string.quantity_edit_label)) },
                isError = input.isNotBlank() && !isValid
            )
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = isValid
            ) {
                Text(stringResource(R.string.quantity_edit_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.quantity_edit_cancel))
            }
        }
    )
}
