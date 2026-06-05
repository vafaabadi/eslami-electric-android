package com.eslamielectric.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eslamielectric.android.R
import com.eslamielectric.android.core.network.OrderDto
import com.eslamielectric.android.core.network.OrderLineItemDto
import com.eslamielectric.android.feature.orders.GuestOrderInput
import com.eslamielectric.android.feature.orders.GuestTrackUiState
import com.eslamielectric.android.feature.orders.GuestTrackViewModel
import com.eslamielectric.android.feature.orders.OrderActionState
import com.eslamielectric.android.feature.orders.OrderDetailUiState
import com.eslamielectric.android.feature.orders.OrderDetailViewModel
import com.eslamielectric.android.feature.orders.OrdersListUiState
import com.eslamielectric.android.feature.orders.OrdersListViewModel
import com.eslamielectric.android.feature.orders.OrdersRepository
import com.eslamielectric.android.feature.orders.ordersViewModelFactory
import com.eslamielectric.android.util.formatOrderCents
import com.eslamielectric.android.util.formatOrderDate
import com.eslamielectric.android.util.formatShippingAddress
import com.eslamielectric.android.util.fulfillmentLabel
import com.eslamielectric.android.util.isPendingOrder
import com.eslamielectric.android.util.lineItemTotalCents
import com.eslamielectric.android.util.orderStatusLabel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun MyOrdersScreen(
    ordersRepository: OrdersRepository,
    locale: String,
    onBack: () -> Unit,
    onOrderClick: (OrderDto) -> Unit,
    onSessionExpired: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: OrdersListViewModel = viewModel(factory = ordersViewModelFactory(ordersRepository))
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val refreshing = isRefreshing || state is OrdersListUiState.Loading
    val pullRefreshState = rememberPullRefreshState(refreshing = refreshing, onRefresh = viewModel::load)

    LaunchedEffect(Unit) {
        viewModel.onSessionExpired = onSessionExpired
        viewModel.load()
    }

    Scaffold(
        modifier = modifier.testTag("screen_my_orders"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.orders_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            when (val s = state) {
            OrdersListUiState.Loading -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }

            is OrdersListUiState.Error -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(s.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = viewModel::load) { Text(stringResource(R.string.retry)) }
            }

            OrdersListUiState.Empty -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.orders_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("orders_empty")
                )
            }

            is OrdersListUiState.Ready -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(s.orders, key = { it.id }) { order ->
                    OrderListCard(
                        order = order,
                        locale = locale,
                        onClick = { onOrderClick(order) }
                    )
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
}

@Composable
private fun OrderListCard(
    order: OrderDto,
    locale: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.orderNumber ?: order.id,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = orderStatusLabel(order.status),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = stringResource(R.string.order_date_label, formatOrderDate(order.createdAt, locale)),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.order_total_label,
                    formatOrderCents(order.amountTotal, order.currency)
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.order_fulfillment_label,
                    fulfillmentLabel(order.fulfillmentType)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    ordersRepository: OrdersRepository,
    orderId: String,
    guestToken: String?,
    isGuest: Boolean,
    locale: String,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    onProfileIncomplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: OrderDetailViewModel = viewModel(
        factory = ordersViewModelFactory(ordersRepository, orderId, guestToken, isGuest)
    )
    val state by viewModel.state.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val context = LocalContext.current
    var showCancelDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onSessionExpired = onSessionExpired
        viewModel.onProfileIncomplete = { onProfileIncomplete() }
        viewModel.load()
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(stringResource(R.string.order_cancel_confirm_title)) },
            text = { Text(stringResource(R.string.order_cancel_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancel { }
                    }
                ) { Text(stringResource(R.string.order_cancel_confirm_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text(stringResource(R.string.order_cancel_confirm_no))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.order_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            OrderDetailUiState.Loading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }

            is OrderDetailUiState.Error -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(s.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = viewModel::load) { Text(stringResource(R.string.retry)) }
            }

            is OrderDetailUiState.Ready -> {
                val order = s.order
                val pending = isPendingOrder(order)
                val canGuestActions = !isGuest || !viewModel.effectiveGuestToken.isNullOrBlank()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = order.orderNumber ?: order.id,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OrderDetailRow(
                        stringResource(R.string.order_status_label),
                        orderStatusLabel(order.status)
                    )
                    OrderDetailRow(
                        stringResource(R.string.order_date_label_plain),
                        formatOrderDate(order.createdAt, locale)
                    )
                    OrderDetailRow(
                        stringResource(R.string.order_payment_label),
                        orderStatusLabel(order.status)
                    )
                    OrderDetailRow(
                        stringResource(R.string.order_fulfillment_label_plain),
                        fulfillmentLabel(order.fulfillmentType)
                    )
                    order.trackingNumber?.takeIf { it.isNotBlank() }?.let { tracking ->
                        OrderDetailRow(stringResource(R.string.order_tracking_label), tracking)
                    }
                    formatShippingAddress(order.shippingAddress)?.let { addr ->
                        OrderDetailRow(stringResource(R.string.checkout_shipping_address), addr)
                    }
                    if (pending) {
                        Text(
                            text = stringResource(R.string.order_pending_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        if (isGuest && !canGuestActions) {
                            Text(
                                text = stringResource(R.string.order_guest_token_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.order_items_heading),
                        style = MaterialTheme.typography.titleMedium
                    )
                    order.lineItems.orEmpty().forEach { item ->
                        OrderLineItemRow(item, order.currency)
                    }
                    HorizontalDivider()
                    Text(
                        text = stringResource(
                            R.string.order_total_label,
                            formatOrderCents(order.amountTotal, order.currency)
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (pending && canGuestActions) {
                        Button(
                            onClick = { viewModel.resumeCheckout(context, locale) },
                            enabled = actionState !is OrderActionState.Loading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.order_resume_payment))
                        }
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            enabled = actionState !is OrderActionState.Loading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.order_cancel))
                        }
                    }
                    if (actionState is OrderActionState.Loading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) { CircularProgressIndicator() }
                    }
                    if (actionState is OrderActionState.Error) {
                        Text(
                            text = (actionState as OrderActionState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = viewModel::clearActionError) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderDetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun OrderLineItemRow(item: OrderLineItemDto, currency: String?) {
    val qty = item.quantity ?: 1
    val lineTotal = lineItemTotalCents(item)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${item.name ?: "—"} × $qty",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatOrderCents(lineTotal, currency),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestTrackScreen(
    ordersRepository: OrdersRepository,
    onBack: () -> Unit,
    onOrderFound: (OrderDto, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: GuestTrackViewModel = viewModel(factory = ordersViewModelFactory(ordersRepository))
    val state by viewModel.state.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var orderRef by rememberSaveable { mutableStateOf("") }
    var trackingToken by rememberSaveable { mutableStateOf("") }
    var useToken by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.guest_track_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.guest_track_intro),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { useToken = false; viewModel.clearError() },
                    modifier = Modifier.testTag("guest_track_mode_email")
                ) {
                    Text(
                        stringResource(R.string.guest_track_mode_email),
                        fontWeight = if (!useToken) FontWeight.Bold else FontWeight.Normal
                    )
                }
                TextButton(
                    onClick = { useToken = true; viewModel.clearError() },
                    modifier = Modifier.testTag("guest_track_mode_token")
                ) {
                    Text(
                        stringResource(R.string.guest_track_mode_token),
                        fontWeight = if (useToken) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            if (!useToken) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; viewModel.clearError() },
                    label = { Text(stringResource(R.string.label_email)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_guest_email")
                )
                OutlinedTextField(
                    value = orderRef,
                    onValueChange = { orderRef = it; viewModel.clearError() },
                    label = { Text(stringResource(R.string.guest_track_order_ref)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_guest_order_ref"),
                    placeholder = { Text(stringResource(R.string.guest_track_order_ref_hint)) }
                )
            } else {
                OutlinedTextField(
                    value = trackingToken,
                    onValueChange = { trackingToken = it; viewModel.clearError() },
                    label = { Text(stringResource(R.string.guest_track_token_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_guest_token"),
                    placeholder = { Text(stringResource(R.string.guest_track_token_hint)) },
                    supportingText = {
                        Text(stringResource(R.string.guest_track_token_supporting))
                    }
                )
            }
            if (state is GuestTrackUiState.Error) {
                Text(
                    (state as GuestTrackUiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (state is GuestTrackUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            Button(
                onClick = {
                    if (useToken) {
                        if (viewModel.isOrderNumberNotToken(trackingToken)) {
                            orderRef = GuestOrderInput.normalizeOrderRef(trackingToken)
                            trackingToken = ""
                            useToken = false
                            viewModel.clearError()
                            return@Button
                        }
                        viewModel.lookupByToken(trackingToken) { order ->
                            onOrderFound(order, GuestOrderInput.extractTrackingToken(trackingToken))
                        }
                    } else {
                        viewModel.lookupByEmail(email, orderRef) { order ->
                            onOrderFound(order, null)
                        }
                    }
                },
                enabled = state !is GuestTrackUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_guest_track_submit")
            ) {
                Text(stringResource(R.string.guest_track_submit))
            }
        }
    }
}
