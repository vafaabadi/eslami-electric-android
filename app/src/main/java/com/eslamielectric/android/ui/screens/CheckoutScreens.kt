package com.eslamielectric.android.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eslamielectric.android.R
import com.eslamielectric.android.core.analytics.AnalyticsEvents
import com.eslamielectric.android.core.analytics.AnalyticsLogger
import com.eslamielectric.android.core.data.BasketRepository
import com.eslamielectric.android.core.network.OrderDto
import com.eslamielectric.android.feature.auth.AuthRepository
import com.eslamielectric.android.feature.basket.CheckoutRepository
import coil.compose.AsyncImage
import com.eslamielectric.android.feature.basket.CheckoutReturnUiState
import com.eslamielectric.android.feature.basket.CheckoutUiState
import com.eslamielectric.android.feature.basket.CheckoutViewModel
import com.eslamielectric.android.feature.basket.FULFILLMENT_COLLECTION
import com.eslamielectric.android.feature.basket.FULFILLMENT_DELIVERY
import com.eslamielectric.android.feature.basket.PAYMENT_CARD
import com.eslamielectric.android.feature.basket.PAYMENT_CRYPTO
import com.eslamielectric.android.feature.basket.checkoutViewModelFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CheckoutScreen(
    checkoutRepository: CheckoutRepository,
    authRepository: AuthRepository,
    basketRepository: BasketRepository,
    analyticsLogger: AnalyticsLogger,
    locale: String,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onPaymentComplete: (OrderDto) -> Unit,
    onPaymentIncomplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: CheckoutViewModel = viewModel(
        factory = checkoutViewModelFactory(checkoutRepository, authRepository, basketRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val returnState by viewModel.returnState.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val cryptoCurrencies by viewModel.cryptoCurrencies.collectAsState()
    val defaultPayCurrency by viewModel.defaultPayCurrency.collectAsState()
    val cryptoStatusMessage by viewModel.cryptoStatusMessage.collectAsState()
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val clipboard = LocalClipboardManager.current

    var fulfillment by rememberSaveable { mutableStateOf(FULFILLMENT_DELIVERY) }
    var paymentMethod by rememberSaveable { mutableStateOf(PAYMENT_CARD) }
    var selectedPayCurrency by rememberSaveable { mutableStateOf<String?>(null) }
    var guestName by rememberSaveable { mutableStateOf("") }
    var guestEmail by rememberSaveable { mutableStateOf("") }
    var guestPhone by rememberSaveable { mutableStateOf("") }
    var addressLine1 by rememberSaveable { mutableStateOf("") }
    var addressCity by rememberSaveable { mutableStateOf("") }
    var addressPostal by rememberSaveable { mutableStateOf("") }
    var addressExtra by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(profile) {
        profile?.address?.takeIf { it.isNotBlank() && addressLine1.isBlank() }?.let {
            addressLine1 = it
        }
    }

    LaunchedEffect(defaultPayCurrency, cryptoCurrencies) {
        if (selectedPayCurrency.isNullOrBlank()) {
            selectedPayCurrency = defaultPayCurrency ?: cryptoCurrencies.firstOrNull()?.payCurrency
        }
    }

    LaunchedEffect(Unit) {
        checkoutRepository.getPendingEditOrder()?.let { pending ->
            pending.fulfillmentType?.takeIf { it.isNotBlank() }?.let { fulfillment = it }
            pending.addressLine1?.takeIf { it.isNotBlank() && addressLine1.isBlank() }?.let { addressLine1 = it }
            pending.addressCity?.takeIf { it.isNotBlank() && addressCity.isBlank() }?.let { addressCity = it }
            pending.addressPostal?.takeIf { it.isNotBlank() && addressPostal.isBlank() }?.let { addressPostal = it }
            pending.addressExtra?.takeIf { it.isNotBlank() && addressExtra.isBlank() }?.let { addressExtra = it }
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is CheckoutUiState.SessionExpired -> onSessionExpired()
            else -> Unit
        }
    }

    LaunchedEffect(returnState) {
        when (val state = returnState) {
            is CheckoutReturnUiState.Paid -> {
                onPaymentComplete(state.order)
                viewModel.dismissReturnState()
            }
            is CheckoutReturnUiState.NotPaid -> {
                onPaymentIncomplete()
                viewModel.dismissReturnState()
            }
            else -> Unit
        }
    }

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkReturnFromStripe()
                viewModel.checkReturnFromCrypto()
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier.testTag("screen_checkout"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.checkout_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.checkout_fulfillment), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = fulfillment == FULFILLMENT_DELIVERY,
                    onClick = { fulfillment = FULFILLMENT_DELIVERY },
                    label = { Text(stringResource(R.string.checkout_delivery)) }
                )
                FilterChip(
                    selected = fulfillment == FULFILLMENT_COLLECTION,
                    onClick = { fulfillment = FULFILLMENT_COLLECTION },
                    label = { Text(stringResource(R.string.checkout_collection)) }
                )
            }

            if (!viewModel.isLoggedIn) {
                Text(stringResource(R.string.checkout_guest_details), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = guestName,
                    onValueChange = { guestName = it },
                    label = { Text(stringResource(R.string.label_guest_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = guestEmail,
                    onValueChange = { guestEmail = it },
                    label = { Text(stringResource(R.string.label_email)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                OutlinedTextField(
                    value = guestPhone,
                    onValueChange = { guestPhone = it },
                    label = { Text(stringResource(R.string.label_guest_phone_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }

            if (fulfillment == FULFILLMENT_DELIVERY) {
                Text(stringResource(R.string.checkout_shipping_address), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = addressLine1,
                    onValueChange = { addressLine1 = it },
                    label = { Text(stringResource(R.string.label_street_address)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                OutlinedTextField(
                    value = addressCity,
                    onValueChange = { addressCity = it },
                    label = { Text(stringResource(R.string.label_city)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = addressPostal,
                    onValueChange = { addressPostal = it },
                    label = { Text(stringResource(R.string.label_postal_code)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = addressExtra,
                    onValueChange = { addressExtra = it },
                    label = { Text(stringResource(R.string.label_address_extra_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(stringResource(R.string.checkout_payment_method), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = paymentMethod == PAYMENT_CARD,
                    onClick = { paymentMethod = PAYMENT_CARD },
                    label = { Text(stringResource(R.string.checkout_pay_card)) },
                    modifier = Modifier.testTag("chip_payment_card")
                )
                FilterChip(
                    selected = paymentMethod == PAYMENT_CRYPTO,
                    onClick = { paymentMethod = PAYMENT_CRYPTO },
                    label = { Text(stringResource(R.string.checkout_pay_crypto)) },
                    modifier = Modifier.testTag("chip_payment_crypto"),
                    enabled = cryptoCurrencies.isNotEmpty()
                )
            }

            if (paymentMethod == PAYMENT_CRYPTO && cryptoCurrencies.size > 1) {
                Text(stringResource(R.string.checkout_crypto_network_title), style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("crypto_network_chips")
                ) {
                    cryptoCurrencies.forEach { currency ->
                        FilterChip(
                            selected = selectedPayCurrency == currency.payCurrency,
                            onClick = { selectedPayCurrency = currency.payCurrency },
                            label = { Text(currency.label ?: currency.networkLabel) }
                        )
                    }
                }
            }

            if (uiState is CheckoutUiState.CryptoActive) {
                val payment = (uiState as CheckoutUiState.CryptoActive).payment
                CryptoPaymentPanel(
                    payment = payment,
                    statusMessage = cryptoStatusMessage,
                    onCopyAddress = { address ->
                        clipboard.setText(AnnotatedString(address))
                        Toast.makeText(context, context.getString(R.string.checkout_crypto_address_copied), Toast.LENGTH_SHORT).show()
                    },
                    onOpenInvoice = {
                        viewModel.openCryptoInvoice(context, payment.invoiceUrl ?: payment.gatewayUrl)
                    },
                    onCancel = { viewModel.cancelCryptoCheckout() }
                )
            }

            if (uiState is CheckoutUiState.Error) {
                val err = uiState as CheckoutUiState.Error
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(err.message, color = MaterialTheme.colorScheme.error)
                    if (err.missing.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatMissingFields(err.missing),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (err.profileIncomplete) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onNavigateToProfile) {
                            Text(stringResource(R.string.checkout_complete_profile))
                        }
                    }
                }
            }

            if (returnState is CheckoutReturnUiState.Checking) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
                    Text(stringResource(R.string.checkout_verifying_payment))
                }
            }

            if (uiState !is CheckoutUiState.CryptoActive) {
                Button(
                    onClick = {
                        viewModel.clearError()
                        analyticsLogger.logEvent(AnalyticsEvents.CHECKOUT_STARTED)
                        viewModel.pay(
                            context = context,
                            paymentMethod = paymentMethod,
                            payCurrency = selectedPayCurrency.orEmpty(),
                            fulfillmentType = fulfillment,
                            locale = locale,
                            guestEmail = guestEmail,
                            guestName = guestName,
                            guestPhone = guestPhone,
                            addressLine1 = addressLine1,
                            addressCity = addressCity,
                            addressPostal = addressPostal,
                            addressExtra = addressExtra
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(
                            if (paymentMethod == PAYMENT_CRYPTO) "btn_checkout_pay_crypto"
                            else "btn_checkout_pay_stripe"
                        ),
                    enabled = uiState !is CheckoutUiState.Loading &&
                        returnState !is CheckoutReturnUiState.Checking &&
                        (paymentMethod != PAYMENT_CRYPTO || !selectedPayCurrency.isNullOrBlank())
                ) {
                    if (uiState is CheckoutUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            stringResource(
                                if (paymentMethod == PAYMENT_CRYPTO) R.string.checkout_pay_now
                                else R.string.checkout_pay_stripe
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CryptoPaymentPanel(
    payment: com.eslamielectric.android.core.network.CreateCryptoPaymentResponse,
    statusMessage: String?,
    onCopyAddress: (String) -> Unit,
    onOpenInvoice: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val networkLabel = payment.networkLabel.orEmpty()
    val qrContent = payment.payAddress ?: payment.invoiceUrl ?: payment.gatewayUrl
    val qrUrl = qrContent?.let {
        "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" +
            URLEncoder.encode(it, StandardCharsets.UTF_8.name())
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("panel_crypto_payment"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (networkLabel.isNotBlank()) {
                Text(
                    text = stringResource(R.string.checkout_crypto_network_warning, networkLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (!payment.payAmount.isNullOrBlank() && !payment.payCurrency.isNullOrBlank()) {
                Text(
                    text = stringResource(
                        R.string.checkout_crypto_send_amount,
                        payment.payAmount,
                        payment.payCurrency.uppercase()
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            payment.payAddress?.let { address ->
                Text(stringResource(R.string.checkout_crypto_address_label), style = MaterialTheme.typography.labelLarge)
                Text(text = address, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(
                    onClick = { onCopyAddress(address) },
                    modifier = Modifier.testTag("btn_crypto_copy_address")
                ) {
                    Text(stringResource(R.string.checkout_crypto_copy_address))
                }
            }
            qrUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Payment QR code",
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.CenterHorizontally)
                        .testTag("img_crypto_qr"),
                    contentScale = ContentScale.Fit
                )
            }
            if (!payment.invoiceUrl.isNullOrBlank() || !payment.gatewayUrl.isNullOrBlank()) {
                Button(
                    onClick = onOpenInvoice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_crypto_open_invoice")
                ) {
                    Text(stringResource(R.string.checkout_crypto_open_invoice))
                }
            }
            statusMessage?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_crypto_cancel")
            ) {
                Text(stringResource(R.string.checkout_crypto_cancel))
            }
        }
    }
}

@Composable
fun CheckoutResultScreen(
    success: Boolean,
    order: OrderDto?,
    message: String?,
    onDone: () -> Unit,
    onTrackOrder: (() -> Unit)? = null,
    onClaimAccount: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("screen_checkout_result"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(
                if (success) R.string.checkout_success_title else R.string.checkout_incomplete_title
            ),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when {
                success -> stringResource(R.string.checkout_success_thanks)
                !message.isNullOrBlank() -> message
                else -> stringResource(R.string.checkout_incomplete_hint)
            },
            style = MaterialTheme.typography.bodyLarge
        )
        if (success && order?.orderNumber != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.checkout_order_number, order.orderNumber),
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (success && onTrackOrder != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.checkout_guest_track_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (success && onClaimAccount != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.checkout_claim_account_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (success && onTrackOrder != null) {
            Button(onClick = onTrackOrder, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.checkout_track_order))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (success && onClaimAccount != null) {
            OutlinedButton(
                onClick = onClaimAccount,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_checkout_claim_account")
            ) {
                Text(stringResource(R.string.checkout_claim_account))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.checkout_done))
        }
    }
}

@Composable
private fun formatMissingFields(missing: List<String>): String {
    val labels = missing.mapNotNull { field ->
        when (field) {
            "firstName" -> stringResource(R.string.label_first_name)
            "surname" -> stringResource(R.string.label_surname)
            "mobile" -> stringResource(R.string.label_mobile)
            "email" -> stringResource(R.string.label_email)
            "contactEmail" -> stringResource(R.string.label_contact_email_optional)
            "companyName" -> stringResource(R.string.label_company_name)
            "companyContactNumber" -> stringResource(R.string.label_company_contact_optional)
            else -> field
        }
    }
    return if (labels.isEmpty()) "" else stringResource(R.string.checkout_missing_fields, labels.joinToString(", "))
}
