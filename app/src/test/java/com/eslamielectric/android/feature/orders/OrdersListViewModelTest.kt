package com.eslamielectric.android.feature.orders

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.eslamielectric.android.core.network.ApiException
import com.eslamielectric.android.core.network.OrderDto
import com.eslamielectric.android.core.network.SessionExpiredException
import com.eslamielectric.android.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
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
class OrdersListViewModelTest {

    @get:Rule
    val instantTask = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val ordersRepository = mockk<OrdersRepository>(relaxed = true)
    private lateinit var viewModel: OrdersListViewModel

    @Before
    fun setUp() {
        viewModel = OrdersListViewModel(ordersRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun load_emitsReadyWhenOrdersExist() = runTest {
        coEvery { ordersRepository.loadMyOrders() } returns listOf(
            OrderDto(id = "o1", orderNumber = "ORD-1")
        )
        viewModel.load()
        advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is OrdersListUiState.Ready)
        assertEquals(1, (state as OrdersListUiState.Ready).orders.size)
    }

    @Test
    fun load_emitsEmptyWhenNoOrders() = runTest {
        coEvery { ordersRepository.loadMyOrders() } returns emptyList()
        viewModel.load()
        advanceUntilIdle()
        assertEquals(OrdersListUiState.Empty, viewModel.state.value)
    }

    @Test
    fun load_invokesSessionExpiredCallback() = runTest {
        coEvery { ordersRepository.loadMyOrders() } throws SessionExpiredException()
        var expired = false
        viewModel.onSessionExpired = { expired = true }
        viewModel.load()
        advanceUntilIdle()
        assertTrue(expired)
        coVerify { ordersRepository.logoutOnSessionExpired() }
    }

    @Test
    fun load_mapsApiExceptionToError() = runTest {
        coEvery { ordersRepository.loadMyOrders() } throws ApiException(500, "Server error")
        viewModel.load()
        advanceUntilIdle()
        assertEquals("Server error", (viewModel.state.value as OrdersListUiState.Error).message)
    }
}
