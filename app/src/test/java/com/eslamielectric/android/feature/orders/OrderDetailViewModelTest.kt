package com.eslamielectric.android.feature.orders

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.eslamielectric.android.core.network.OkResponse
import com.eslamielectric.android.core.network.OrderDto
import com.eslamielectric.android.core.network.SessionExpiredException
import com.eslamielectric.android.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderDetailViewModelTest {

    @get:Rule
    val instantTask = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val ordersRepository = mockk<OrdersRepository>(relaxed = true)

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun load_guestUsesCachedOrder() = runTest {
        val order = OrderDto(id = "o1", orderNumber = "ORD-1")
        every { ordersRepository.getCachedGuestOrder("o1") } returns order
        val vm = OrderDetailViewModel(ordersRepository, "o1", guestToken = null, isGuest = true)
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value is OrderDetailUiState.Ready)
        assertEquals("ORD-1", (vm.state.value as OrderDetailUiState.Ready).order.orderNumber)
    }

    @Test
    fun load_loggedInFindsOrder() = runTest {
        val order = OrderDto(id = "o2", orderNumber = "ORD-2")
        coEvery { ordersRepository.findOrder("o2") } returns order
        val vm = OrderDetailViewModel(ordersRepository, "o2", guestToken = null, isGuest = false)
        vm.load()
        advanceUntilIdle()
        assertEquals(order, (vm.state.value as OrderDetailUiState.Ready).order)
    }

    @Test
    fun load_emitsErrorWhenOrderMissing() = runTest {
        coEvery { ordersRepository.findOrder("missing") } returns null
        val vm = OrderDetailViewModel(ordersRepository, "missing", guestToken = null, isGuest = false)
        vm.load()
        advanceUntilIdle()
        assertEquals("Order not found.", (vm.state.value as OrderDetailUiState.Error).message)
    }

    @Test
    fun effectiveGuestToken_fallsBackToRepositoryCache() {
        every { ordersRepository.getCachedGuestToken("o1") } returns "cached-tok"
        val vm = OrderDetailViewModel(ordersRepository, "o1", guestToken = null, isGuest = true)
        assertEquals("cached-tok", vm.effectiveGuestToken)
    }

    @Test
    fun cancel_guestUsesTokenPath() = runTest {
        val vm = OrderDetailViewModel(ordersRepository, "o1", guestToken = "tok", isGuest = true)
        coEvery { ordersRepository.guestCancel("tok") } returns Unit
        var success = false
        vm.cancel { success = true }
        advanceUntilIdle()
        assertTrue(success)
        coVerify { ordersRepository.guestCancel("tok") }
    }

    @Test
    fun cancel_loggedInUsesOrderId() = runTest {
        val vm = OrderDetailViewModel(ordersRepository, "o1", guestToken = null, isGuest = false)
        coEvery { ordersRepository.cancelOrder("o1") } returns OkResponse(ok = true)
        vm.cancel {}
        advanceUntilIdle()
        coVerify { ordersRepository.cancelOrder("o1") }
    }

    @Test
    fun cancel_mapsSessionExpired() = runTest {
        val vm = OrderDetailViewModel(ordersRepository, "o1", guestToken = null, isGuest = false)
        coEvery { ordersRepository.cancelOrder(any()) } throws SessionExpiredException()
        var expired = false
        vm.onSessionExpired = { expired = true }
        vm.cancel {}
        advanceUntilIdle()
        assertTrue(expired)
        coVerify { ordersRepository.logoutOnSessionExpired() }
    }
}
