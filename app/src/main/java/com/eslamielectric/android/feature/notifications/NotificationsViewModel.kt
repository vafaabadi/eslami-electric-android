package com.eslamielectric.android.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.eslamielectric.android.core.network.ApiException
import com.eslamielectric.android.core.network.PushChannelPatch
import com.eslamielectric.android.core.network.PushPreferencesDto
import com.eslamielectric.android.core.network.SessionExpiredException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data class Ready(val prefs: PushPreferencesDto) : NotificationsUiState
    data class Error(val message: String) : NotificationsUiState
}

class NotificationsViewModel(
    private val repo: NotificationPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    var onSessionExpired: (() -> Unit)? = null

    fun load() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState.Loading
            try {
                _uiState.value = NotificationsUiState.Ready(repo.load())
            } catch (_: SessionExpiredException) {
                onSessionExpired?.invoke()
            } catch (e: ApiException) {
                _uiState.value = NotificationsUiState.Error(e.message)
            } catch (e: Exception) {
                _uiState.value = NotificationsUiState.Error(e.message ?: "Could not load notifications.")
            }
        }
    }

    fun setMaster(enabled: Boolean) {
        viewModelScope.launch {
            optimisticallyUpdate { it.copy(masterEnabled = enabled) }
            try {
                val updated = repo.setMasterEnabled(enabled)
                _uiState.value = NotificationsUiState.Ready(updated)
            } catch (_: SessionExpiredException) {
                onSessionExpired?.invoke()
            } catch (e: ApiException) {
                rollbackAndReportError(e.message)
            } catch (e: Exception) {
                rollbackAndReportError(e.message)
            }
        }
    }

    fun setChannel(channel: String, enabled: Boolean) {
        val patch = when (channel) {
            NotificationChannels.ORDERS -> PushChannelPatch(orders = enabled)
            NotificationChannels.PROMOTIONS -> PushChannelPatch(promotions = enabled)
            NotificationChannels.ACCOUNT -> PushChannelPatch(account = enabled)
            NotificationChannels.GENERAL -> PushChannelPatch(general = enabled)
            else -> return
        }
        viewModelScope.launch {
            optimisticallyUpdate { current ->
                val ch = current.channels
                current.copy(
                    channels = when (channel) {
                        NotificationChannels.ORDERS -> ch.copy(orders = enabled)
                        NotificationChannels.PROMOTIONS -> ch.copy(promotions = enabled)
                        NotificationChannels.ACCOUNT -> ch.copy(account = enabled)
                        NotificationChannels.GENERAL -> ch.copy(general = enabled)
                        else -> ch
                    }
                )
            }
            try {
                val updated = repo.setChannels(patch)
                _uiState.value = NotificationsUiState.Ready(updated)
            } catch (_: SessionExpiredException) {
                onSessionExpired?.invoke()
            } catch (e: ApiException) {
                rollbackAndReportError(e.message)
            } catch (e: Exception) {
                rollbackAndReportError(e.message)
            }
        }
    }

    fun clearSaveError() {
        _saveError.value = null
    }

    private fun optimisticallyUpdate(transform: (PushPreferencesDto) -> PushPreferencesDto) {
        val current = (_uiState.value as? NotificationsUiState.Ready)?.prefs ?: return
        _uiState.value = NotificationsUiState.Ready(transform(current))
    }

    private fun rollbackAndReportError(message: String?) {
        _saveError.value = message ?: "Save failed."
        load()
    }
}

fun notificationsViewModelFactory(repo: NotificationPreferencesRepository): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NotificationsViewModel::class.java))
            return NotificationsViewModel(repo) as T
        }
    }
