package com.eslamielectric.android.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eslamielectric.android.R
import com.eslamielectric.android.feature.notifications.NotificationChannels
import com.eslamielectric.android.feature.notifications.NotificationPermission
import com.eslamielectric.android.feature.notifications.NotificationPreferencesRepository
import com.eslamielectric.android.feature.notifications.NotificationsUiState
import com.eslamielectric.android.feature.notifications.NotificationsViewModel
import com.eslamielectric.android.feature.notifications.notificationsViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    repository: NotificationPreferencesRepository,
    fcmConfigured: Boolean,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: NotificationsViewModel =
        viewModel(factory = notificationsViewModelFactory(repository))
    val state by viewModel.uiState.collectAsState()
    val saveError by viewModel.saveError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(NotificationPermission.isPermissionGranted(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted && NotificationPermission.isPermissionGranted(context)
    }

    LaunchedEffect(Unit) {
        viewModel.onSessionExpired = onSessionExpired
        viewModel.load()
    }

    val saveErrorText = stringResource(R.string.notifications_save_error)
    LaunchedEffect(saveError) {
        val msg = saveError ?: return@LaunchedEffect
        scope.launch { snackbarHostState.showSnackbar(msg.ifBlank { saveErrorText }) }
        viewModel.clearSaveError()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_settings_title)) },
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
                .padding(16.dp)
                .testTag("screen_notifications"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!fcmConfigured) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.notifications_fcm_not_configured),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (!permissionGranted) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.notifications_permission_required),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                TextButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                    Text(stringResource(R.string.permission_dialog_allow))
                                }
                            }
                            OutlinedButton(onClick = {
                                NotificationPermission.openSystemSettings(context)
                            }) {
                                Text(stringResource(R.string.notifications_open_system_settings))
                            }
                        }
                    }
                }
            }
            when (val s = state) {
                NotificationsUiState.Loading -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) { CircularProgressIndicator() }

                is NotificationsUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = s.message.ifBlank { stringResource(R.string.notifications_loading_error) },
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = viewModel::load) { Text(stringResource(R.string.retry)) }
                }

                is NotificationsUiState.Ready -> {
                    val prefs = s.prefs
                    PreferenceRow(
                        title = stringResource(R.string.notifications_master),
                        subtitle = stringResource(R.string.notifications_master_hint),
                        checked = prefs.masterEnabled,
                        onChange = viewModel::setMaster,
                        enabled = true,
                        testTag = "toggle_notifications_master"
                    )
                    HorizontalDivider()
                    PreferenceRow(
                        title = stringResource(R.string.notif_channel_orders_name),
                        subtitle = stringResource(R.string.notif_channel_orders_desc),
                        checked = prefs.channels.orders && prefs.masterEnabled,
                        onChange = { viewModel.setChannel(NotificationChannels.ORDERS, it) },
                        enabled = prefs.masterEnabled,
                        testTag = "toggle_notifications_orders"
                    )
                    PreferenceRow(
                        title = stringResource(R.string.notif_channel_promotions_name),
                        subtitle = stringResource(R.string.notif_channel_promotions_desc),
                        checked = prefs.channels.promotions && prefs.masterEnabled,
                        onChange = { viewModel.setChannel(NotificationChannels.PROMOTIONS, it) },
                        enabled = prefs.masterEnabled
                    )
                    PreferenceRow(
                        title = stringResource(R.string.notif_channel_account_name),
                        subtitle = stringResource(R.string.notif_channel_account_desc),
                        checked = prefs.channels.account && prefs.masterEnabled,
                        onChange = { viewModel.setChannel(NotificationChannels.ACCOUNT, it) },
                        enabled = prefs.masterEnabled
                    )
                    PreferenceRow(
                        title = stringResource(R.string.notif_channel_general_name),
                        subtitle = stringResource(R.string.notif_channel_general_desc),
                        checked = prefs.channels.general && prefs.masterEnabled,
                        onChange = { viewModel.setChannel(NotificationChannels.GENERAL, it) },
                        enabled = prefs.masterEnabled
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean,
    testTag: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}
