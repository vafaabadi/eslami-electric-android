package com.eslamielectric.android.feature.notifications

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.eslamielectric.android.core.network.ApiException
import com.eslamielectric.android.core.network.PushChannelPreferences
import com.eslamielectric.android.core.network.PushPreferencesDto
import com.eslamielectric.android.core.network.SessionExpiredException
import com.eslamielectric.android.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    @get:Rule
    val instantTask = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repo = mockk<NotificationPreferencesRepository>(relaxed = true)

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun load_emitsReadyState() = runTest {
        val prefs = PushPreferencesDto(masterEnabled = true)
        coEvery { repo.load() } returns prefs
        val vm = NotificationsViewModel(repo)
        vm.load()
        advanceUntilIdle()
        assertEquals(prefs, (vm.uiState.value as NotificationsUiState.Ready).prefs)
    }

    @Test
    fun load_mapsApiError() = runTest {
        coEvery { repo.load() } throws ApiException(500, "Server error")
        val vm = NotificationsViewModel(repo)
        vm.load()
        advanceUntilIdle()
        assertEquals("Server error", (vm.uiState.value as NotificationsUiState.Error).message)
    }

    @Test
    fun setMaster_updatesPreferences() = runTest {
        val initial = PushPreferencesDto(masterEnabled = true)
        val updated = PushPreferencesDto(masterEnabled = false)
        coEvery { repo.load() } returns initial
        coEvery { repo.setMasterEnabled(false) } returns updated
        val vm = NotificationsViewModel(repo)
        vm.load()
        advanceUntilIdle()
        vm.setMaster(false)
        advanceUntilIdle()
        assertFalse((vm.uiState.value as NotificationsUiState.Ready).prefs.masterEnabled)
    }

    @Test
    fun setChannel_ignoresUnknownChannel() = runTest {
        val vm = NotificationsViewModel(repo)
        vm.setChannel("unknown", true)
        advanceUntilIdle()
        assertTrue(vm.uiState.value is NotificationsUiState.Loading)
    }

    @Test
    fun load_invokesSessionExpiredCallback() = runTest {
        coEvery { repo.load() } throws SessionExpiredException()
        val vm = NotificationsViewModel(repo)
        var expired = false
        vm.onSessionExpired = { expired = true }
        vm.load()
        advanceUntilIdle()
        assertTrue(expired)
    }
}
