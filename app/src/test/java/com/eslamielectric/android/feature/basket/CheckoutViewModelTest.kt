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
import kotlinx.coroutines.test.runCurrent
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
        every { checkoutRepository.getPendingCryptoPaymentId() } returns null
        coEvery { checkoutRepository.loadCryptoPayCurrencies() } returns
            com.eslamielectric.android.core.network.CryptoPayCurrenciesResponse(
                defaultPayCurrency = "usdc",
                currencies = listOf(
                    com.eslamielectric.android.core.network.CryptoPayCurrencyDto(
                        payCurrency = "usdc",
                        networkLabel = "Ethereum (ERC-20)",
                        label = "Ethereum (ERC-20) (USDC)"
                    )
                )
            )
        viewModel = CheckoutViewModel(checkoutRepository, authRepository, basketRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun pay_rejectsEmptyBasket() = runTest {
        coEvery { basketRepository.getItemsOnce() } returns emptyList()
        viewModel.pay(context, PAYMENT_CARD, "usdc", FULFILLMENT_COLLECTION, "en", "a@b.com", "Ann", "", "", "", "", "")
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
        viewModel.pay(context, PAYMENT_CARD, "usdc", FULFILLMENT_COLLECTION, "en", "bad-email", "Ann Lee", "", "", "", "", "")
        advanceUntilIdle()
        val state = viewModel.uiState.value as CheckoutUiState.Error
        assertEquals("A valid email is required.", state.message)
    }

    @Test
    fun pay_validatesDeliveryAddress() = runTest {
        coEvery { basketRepository.getItemsOnce() } returns listOf(
            BasketItem(id = "p", name = "P", price = 1.0)
        )
        viewModel.pay(context, PAYMENT_CARD, "usdc", FULFILLMENT_DELIVERY, "en", "a@b.com", "Ann Lee", "", "123", "", "", "")
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
        viewModel.pay(context, PAYMENT_CARD, "usdc", FULFILLMENT_COLLECTION, "en", "a@b.com", "Ann Lee", "", "", "", "", "")
        advanceUntilIdle()
        assertEquals(CheckoutUiState.StripeOpened, viewModel.uiState.value)
    }

    @Test
    fun payCrypto_startsCryptoCheckout() = runTest {
        coEvery { basketRepository.getItemsOnce() } returns listOf(
            BasketItem(id = "p", name = "P", price = 1.0)
        )
        coEvery {
            checkoutRepository.startCryptoCheckout(any(), "usdc")
        } returns CryptoCheckoutStartResult.Ready(
            com.eslamielectric.android.core.network.CreateCryptoPaymentResponse(
                paymentId = "pay-1",
                payAddress = "0xabc",
                payAmount = "1.0",
                payCurrency = "usdc",
                pollInMs = 3000
            )
        )
        viewModel.pay(context, PAYMENT_CRYPTO, "usdc", FULFILLMENT_COLLECTION, "en", "a@b.com", "Ann Lee", "", "", "", "", "")
        runCurrent()
        assertTrue(viewModel.uiState.value is CheckoutUiState.CryptoActive)
        viewModel.cancelCryptoCheckout()
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
        coVerify { checkoutRepository.clearPendingCryptoPayment() }
    }

    @Test
    fun pay_mapsProfileIncomplete() = runTest {
        every { checkoutRepository.isLoggedIn() } returns true
        viewModel = CheckoutViewModel(checkoutRepository, authRepository, basketRepository)
        coEvery { basketRepository.getItemsOnce() } returns listOf(
            BasketItem(id = "p", name = "P", price = 1.0)
        )
        coEvery { checkoutRepository.startCheckout(any(), any()) } throws
            CheckoutException.ProfileIncomplete("Complete your profile", listOf("mobile", "address"))
        viewModel.pay(context, PAYMENT_CARD, "usdc", FULFILLMENT_COLLECTION, "en", "", "", "", "", "", "", "")
        advanceUntilIdle()
        val state = viewModel.uiState.value as CheckoutUiState.Error
        assertTrue(state.profileIncomplete)
        assertEquals(listOf("mobile", "address"), state.missing)
    }

    @Test
    fun pay_mapsSessionExpired() = runTest {
        coEvery { basketRepository.getItemsOnce() } returns listOf(
            BasketItem(id = "p", name = "P", price = 1.0)
        )
        coEvery { checkoutRepository.startCheckout(any(), any()) } throws CheckoutException.SessionExpired
        viewModel.pay(context, PAYMENT_CARD, "usdc", FULFILLMENT_COLLECTION, "en", "a@b.com", "Ann Lee", "", "", "", "", "")
        advanceUntilIdle()
        assertEquals(CheckoutUiState.SessionExpired, viewModel.uiState.value)
    }

    @Test
    fun pay_mapsRateLimited() = runTest {
        coEvery { basketRepository.getItemsOnce() } returns listOf(
            BasketItem(id = "p", name = "P", price = 1.0)
        )
        coEvery { checkoutRepository.startCheckout(any(), any()) } throws
            CheckoutException.RateLimited("Too many checkout attempts")
        viewModel.pay(context, PAYMENT_CARD, "usdc", FULFILLMENT_COLLECTION, "en", "a@b.com", "Ann Lee", "", "", "", "", "")
        advanceUntilIdle()
        val state = viewModel.uiState.value as CheckoutUiState.Error
        assertEquals("Too many checkout attempts", state.message)
    }

    @Test
    fun pay_skipsGuestValidationWhenLoggedIn() = runTest {
        every { checkoutRepository.isLoggedIn() } returns true
        viewModel = CheckoutViewModel(checkoutRepository, authRepository, basketRepository)
        coEvery { basketRepository.getItemsOnce() } returns listOf(
            BasketItem(id = "p", name = "P", price = 1.0)
        )
        coEvery { checkoutRepository.startCheckout(any(), any()) } returns
            CheckoutStartResult.Opened("sess", "https://stripe.test")
        viewModel.pay(context, PAYMENT_CARD, "usdc", FULFILLMENT_COLLECTION, "en", "", "", "", "", "", "", "")
        advanceUntilIdle()
        assertEquals(CheckoutUiState.StripeOpened, viewModel.uiState.value)
    }

    @Test
    fun checkReturnFromStripe_mapsNotPaid() = runTest {
        every { checkoutRepository.getPendingSessionId() } returns "sess_2"
        coEvery { checkoutRepository.handleReturnFromStripe("sess_2") } returns CheckoutReturnResult.NotPaid
        viewModel.checkReturnFromStripe()
        advanceUntilIdle()
        assertEquals(CheckoutReturnUiState.NotPaid, viewModel.returnState.value)
    }

    @Test
    fun checkReturnFromStripe_mapsErrorToUiState() = runTest {
        every { checkoutRepository.getPendingSessionId() } returns "sess_3"
        coEvery { checkoutRepository.handleReturnFromStripe("sess_3") } returns
            CheckoutReturnResult.Error("Payment not confirmed")
        viewModel.checkReturnFromStripe()
        advanceUntilIdle()
        assertEquals(CheckoutUiState.Error("Payment not confirmed"), viewModel.uiState.value)
        assertEquals(CheckoutReturnUiState.Idle, viewModel.returnState.value)
    }

    @Test
    fun clearError_resetsErrorState() = runTest {
        coEvery { basketRepository.getItemsOnce() } returns emptyList()
        viewModel.pay(context, PAYMENT_CARD, "usdc", FULFILLMENT_COLLECTION, "en", "a@b.com", "Ann", "", "", "", "", "")
        advanceUntilIdle()
        viewModel.clearError()
        assertEquals(CheckoutUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun resetAfterStripe_returnsToIdle() = runTest {
        coEvery { basketRepository.getItemsOnce() } returns listOf(
            BasketItem(id = "p", name = "P", price = 1.0)
        )
        coEvery { checkoutRepository.startCheckout(any(), any()) } returns
            CheckoutStartResult.Opened("sess", "https://stripe.test")
        viewModel.pay(context, PAYMENT_CARD, "usdc", FULFILLMENT_COLLECTION, "en", "a@b.com", "Ann Lee", "", "", "", "", "")
        advanceUntilIdle()
        viewModel.resetAfterStripe()
        assertEquals(CheckoutUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun init_usesFallbackCurrenciesWhenApiFails() = runTest {
        coEvery { checkoutRepository.loadCryptoPayCurrencies() } throws RuntimeException("network error")
        viewModel = CheckoutViewModel(checkoutRepository, authRepository, basketRepository)
        advanceUntilIdle()
        assertEquals(CheckoutViewModel.FALLBACK_CRYPTO_CURRENCIES, viewModel.cryptoCurrencies.value)
        assertEquals("usdc", viewModel.defaultPayCurrency.value)
    }

    @Test
    fun init_fetchesProfileWhenLoggedIn() = runTest {
        every { checkoutRepository.isLoggedIn() } returns true
        val profile = com.eslamielectric.android.core.network.ProfileDto(id = "u1", type = "individual")
        coEvery { authRepository.fetchProfile() } returns profile
        viewModel = CheckoutViewModel(checkoutRepository, authRepository, basketRepository)
        advanceUntilIdle()
        assertEquals("u1", viewModel.profile.value?.id)
    }
}
