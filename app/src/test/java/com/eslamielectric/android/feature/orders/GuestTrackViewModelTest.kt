package com.eslamielectric.android.feature.orders

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.eslamielectric.android.core.network.ApiException
import com.eslamielectric.android.core.network.OrderDto
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GuestTrackViewModelTest {

    @get:Rule
    val instantTask = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val ordersRepository = mockk<OrdersRepository>(relaxed = true)
    private lateinit var viewModel: GuestTrackViewModel

    @Before
    fun setUp() {
        viewModel = GuestTrackViewModel(ordersRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun isOrderNumberNotToken_detectsOrdNumbers() {
        assertTrue(viewModel.isOrderNumberNotToken("ORD-ABC123"))
        assertFalse(viewModel.isOrderNumberNotToken("long-tracking-token-value"))
    }

    @Test
    fun lookupByEmail_requiresFields() = runTest {
        viewModel.lookupByEmail("", "", {})
        assertTrue(viewModel.state.value is GuestTrackUiState.Error)
        assertEquals(
            "Email and order number are required.",
            (viewModel.state.value as GuestTrackUiState.Error).message
        )
    }

    @Test
    fun lookupByEmail_normalizesOrderRefAndCallsRepository() = runTest {
        val order = OrderDto(id = "o1", orderNumber = "ORD-ABC123")
        coEvery { ordersRepository.guestLookup(any(), any()) } returns order
        var found: OrderDto? = null
        viewModel.lookupByEmail("buyer@test.com", "ord-abc123") { found = it }
        advanceUntilIdle()
        assertEquals(order, found)
        assertEquals(GuestTrackUiState.Idle, viewModel.state.value)
    }

    @Test
    fun lookupByToken_rejectsOrderNumberPaste() = runTest {
        viewModel.lookupByToken("ORD-ABC123") {}
        assertTrue(viewModel.state.value is GuestTrackUiState.Error)
    }

    @Test
    fun lookupByToken_rejectsShortToken() = runTest {
        viewModel.lookupByToken("short") {}
        assertTrue(viewModel.state.value is GuestTrackUiState.Error)
    }

    @Test
    fun lookupByToken_mapsApiErrors() = runTest {
        coEvery { ordersRepository.loadGuestByToken(any()) } throws ApiException(404, "Order not found")
        viewModel.lookupByToken("valid-token-abcdefghij") {}
        advanceUntilIdle()
        assertEquals(
            "Order not found",
            (viewModel.state.value as GuestTrackUiState.Error).message
        )
    }
}
