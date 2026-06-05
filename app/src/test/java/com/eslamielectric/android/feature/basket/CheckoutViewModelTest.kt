package com.eslamielectric.android.feature.basket

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.eslamielectric.android.core.data.BasketItem
import com.eslamielectric.android.core.data.BasketRepository
import com.eslamielectric.android.core.network.OrderDto
import com.eslamielectric.android.feature.auth.AuthRepository
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
class CheckoutViewModelTest {

    @get:Rule
    val instantTask = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val checkoutRepository = mockk<CheckoutRepository>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val basketRepository = mockk<BasketRepository>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private lateinit var viewModel: CheckoutViewModel

    @Before
    fun setUp() {
        every { checkoutRepository.isLoggedIn() } returns false
        viewModel = CheckoutViewModel(checkoutRepository, authRepository, basketRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun pay_rejectsEmptyBasket() = runTest {
        coEvery { basketRepository.getItemsOnce() } returns emptyList()
        viewModel.pay(context, FULFILLMENT_COLLECTION, "en", "a@b.com", "Ann", "", "", "", "", "")
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is CheckoutUiState.Error)
        assertEquals("Your basket is empty.", (state as CheckoutUiState.Error).message)
    }

    @Test
    fun pay_validatesGuestEmail() = runTest {
        coEvery { basketRepository.getItemsOnce() } returns listOf(
            BasketItem(id = "p", name = "P", price = 1.0)
        )
        viewModel.pay(context, FULFILLMENT_COLLECTION, "en", "bad-email", "Ann Lee", "", "", "", "", "")
        advanceUntilIdle()
        val state = viewModel.uiState.value as CheckoutUiState.Error
        assertEquals("A valid email is required.", state.message)
    }

    @Test
    fun pay_validatesDeliveryAddress() = runTest {
        coEvery { basketRepository.getItemsOnce() } returns listOf(
            BasketItem(id = "p", name = "P", price = 1.0)
        )
        viewModel.pay(context, FULFILLMENT_DELIVERY, "en", "a@b.com", "Ann Lee", "", "123", "", "", "")
        advanceUntilIdle()
        val state = viewModel.uiState.value as CheckoutUiState.Error
        assertEquals("Delivery address must be at least 5 characters.", state.message)
    }

    @Test
    fun pay_opensStripeOnSuccess() = runTest {
        coEvery { basketRepository.getItemsOnce() } returns listOf(
            BasketItem(id = "p", name = "P", price = 1.0)
        )
        coEvery {
            checkoutRepository.startCheckout(any(), any())
        } returns CheckoutStartResult.Opened("sess", "https://stripe.test")
        viewModel.pay(context, FULFILLMENT_COLLECTION, "en", "a@b.com", "Ann Lee", "", "", "", "", "")
        advanceUntilIdle()
        assertEquals(CheckoutUiState.StripeOpened, viewModel.uiState.value)
    }

    @Test
    fun checkReturnFromStripe_mapsPaidResult() = runTest {
        every { checkoutRepository.getPendingSessionId() } returns "sess_1"
        val order = OrderDto(id = "o1", status = "paid")
        coEvery { checkoutRepository.handleReturnFromStripe("sess_1") } returns CheckoutReturnResult.Paid(order)
        viewModel.checkReturnFromStripe()
        advanceUntilIdle()
        val state = viewModel.returnState.value
        assertTrue(state is CheckoutReturnUiState.Paid)
        assertEquals("o1", (state as CheckoutReturnUiState.Paid).order.id)
    }

    @Test
    fun dismissPendingWithoutPayment_clearsSession() = runTest {
        viewModel.dismissPendingWithoutPayment()
        advanceUntilIdle()
        coVerify { checkoutRepository.clearPendingSession() }
    }
}
