package com.eslamielectric.android.feature.basket

import android.content.Context
import com.eslamielectric.android.core.data.BasketItem
import com.eslamielectric.android.core.data.BasketRepository
import com.eslamielectric.android.core.data.PendingCheckoutStore
import com.eslamielectric.android.core.data.SessionStore
import com.eslamielectric.android.core.network.ApiService
import com.eslamielectric.android.core.network.BasketDraftItemDto
import com.eslamielectric.android.core.network.BasketDraftResponse
import com.eslamielectric.android.core.network.CheckoutSessionResponse
import com.eslamielectric.android.core.network.ConfirmByCryptoResponse
import com.eslamielectric.android.core.network.CreateCheckoutSessionRequest
import com.eslamielectric.android.core.network.CreateCryptoPaymentResponse
import com.eslamielectric.android.core.network.CryptoPaymentStatusResponse
import com.eslamielectric.android.core.network.OrderDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import com.eslamielectric.android.util.StripeCheckoutTabs

class CheckoutRepositoryTest {

    private val api = mockk<ApiService>()
    private val basketRepository = mockk<BasketRepository>(relaxed = true)
    private val sessionStore = mockk<SessionStore>(relaxed = true)
    private val pendingCheckoutStore = mockk<PendingCheckoutStore>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private lateinit var repository: CheckoutRepository

    @Before
    fun setUp() {
        repository = CheckoutRepository(api, basketRepository, sessionStore, pendingCheckoutStore)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun loadPendingOrderForEdit_loadsDraftAndSetsPendingState() = runTest {
        every { sessionStore.getToken() } returns "jwt-token"
        val address = buildJsonObject { put("line1", "1 Road") }
        coEvery {
            api.getBasketDraft("Bearer jwt-token", "order-1")
        } returns BasketDraftResponse(
            orderId = "order-1",
            orderNumber = "ORD-ABC123",
            basket = listOf(BasketDraftItemDto(id = "p1", name = "Item", price = 1.0)),
            fulfillmentType = "delivery",
            shippingAddress = address
        )
        val label = repository.loadPendingOrderForEdit("order-1")
        assertEquals("ORD-ABC123", label)
        coVerify { basketRepository.loadFromDraft(any()) }
        verify {
            pendingCheckoutStore.setPendingEditOrder(
                orderId = "order-1",
                orderLabel = "ORD-ABC123",
                fulfillmentType = "delivery",
                shippingAddress = address
            )
        }
    }

    @Test(expected = CheckoutException.SessionExpired::class)
    fun loadPendingOrderForEdit_clearsTokenOn401() = runTest {
        every { sessionStore.getToken() } returns "jwt-token"
        coEvery { api.getBasketDraft(any(), any()) } throws httpError(401, """{"error":"expired"}""")
        try {
            repository.loadPendingOrderForEdit("order-1")
        } finally {
            verify { sessionStore.setToken(null) }
        }
    }

    @Test(expected = CheckoutException.EmptyBasket::class)
    fun startCheckout_throwsWhenBasketEmpty() = runTest {
        coEvery { basketRepository.getItemsOnce() } returns emptyList()
        repository.startCheckout(context, CheckoutInput(fulfillmentType = "pickup"))
    }

    @Test
    fun startCheckout_passesPendingOrderIdToApi() = runTest {
        mockkObject(StripeCheckoutTabs)
        every { StripeCheckoutTabs.open(any(), any()) } returns Unit
        coEvery { basketRepository.getItemsOnce() } returns listOf(
            BasketItem(id = "p1", name = "A", price = 5.0, quantity = 1)
        )
        coEvery { basketRepository.toCheckoutLineItems("en") } returns emptyList()
        every { sessionStore.getToken() } returns null
        every { pendingCheckoutStore.getPendingEditOrder() } returns
            PendingCheckoutStore.PendingEditOrder(
                orderId = "pending-99",
                orderLabel = "ORD-X",
                fulfillmentType = "pickup",
                addressLine1 = null,
                addressCity = null,
                addressPostal = null,
                addressExtra = null
            )
        val bodySlot = slot<CreateCheckoutSessionRequest>()
        coEvery {
            api.createCheckoutSession(authorization = null, body = capture(bodySlot))
        } returns CheckoutSessionResponse(url = "https://stripe.test", sessionId = "sess_1")

        val result = repository.startCheckout(
            context,
            CheckoutInput(fulfillmentType = "pickup", locale = "en", guestEmail = " Guest@Test.com ")
        )

        assertTrue(result is CheckoutStartResult.Opened)
        assertEquals("pending-99", bodySlot.captured.pendingOrderId)
        assertEquals("guest@test.com", bodySlot.captured.guestEmail)
        verify { pendingCheckoutStore.setSessionId("sess_1") }
    }

    @Test
    fun handleReturnFromStripe_clearsBasketWhenPaid() = runTest {
        coEvery { api.confirmOrderBySession("sess_paid") } returns mockk(relaxed = true)
        coEvery { api.getOrderBySession("sess_paid") } returns OrderDto(id = "o1", status = "paid")
        val result = repository.handleReturnFromStripe("sess_paid")
        assertTrue(result is CheckoutReturnResult.Paid)
        coVerify { basketRepository.clear() }
        verify { pendingCheckoutStore.clearPendingEditOrder() }
        verify { pendingCheckoutStore.setSessionId(null) }
    }

    @Test
    fun handleReturnFromStripe_returnsNotPaidForUnpaidStatus() = runTest {
        coEvery { api.confirmOrderBySession("sess_open") } returns mockk(relaxed = true)
        coEvery { api.getOrderBySession("sess_open") } returns OrderDto(id = "o2", status = "open")
        val result = repository.handleReturnFromStripe("sess_open")
        assertTrue(result is CheckoutReturnResult.NotPaid)
    }

    @Test
    fun reconcileStalePendingEdit_clearsPaidOrder() = runTest {
        every { sessionStore.getToken() } returns "jwt-token"
        every { pendingCheckoutStore.getPendingEditOrder() } returns
            PendingCheckoutStore.PendingEditOrder(
                orderId = "paid-order",
                orderLabel = "ORD-ZYVUL5",
                fulfillmentType = "delivery",
                addressLine1 = null,
                addressCity = null,
                addressPostal = null,
                addressExtra = null
            )
        coEvery { api.getOrders("Bearer jwt-token") } returns listOf(
            OrderDto(id = "paid-order", status = "paid", orderNumber = "ORD-ZYVUL5")
        )
        repository.reconcileStalePendingEdit()
        verify { pendingCheckoutStore.clearPendingEditOrder() }
    }

    @Test
    fun startCryptoCheckout_persistsPaymentId() = runTest {
        coEvery { basketRepository.getItemsOnce() } returns listOf(
            BasketItem(id = "p1", name = "A", price = 5.0, quantity = 1)
        )
        coEvery { basketRepository.toCheckoutLineItems("en") } returns emptyList()
        every { sessionStore.getToken() } returns null
        every { pendingCheckoutStore.getPendingEditOrder() } returns null
        coEvery {
            api.createCryptoPayment(authorization = null, body = any())
        } returns CreateCryptoPaymentResponse(
            paymentId = "5077125051",
            payAddress = "0xabc",
            payAmount = "1.0",
            payCurrency = "usdc"
        )
        val result = repository.startCryptoCheckout(
            CheckoutInput(fulfillmentType = "collection", locale = "en"),
            payCurrency = "usdc"
        )
        assertTrue(result is CryptoCheckoutStartResult.Ready)
        verify { pendingCheckoutStore.setCryptoPaymentId("5077125051") }
    }

    @Test
    fun handleReturnFromCrypto_clearsBasketWhenPaid() = runTest {
        coEvery { api.getCryptoPaymentStatus("pay-1") } returns CryptoPaymentStatusResponse(
            status = "finished",
            orderStatus = "paid"
        )
        coEvery { api.confirmOrderByCrypto("pay-1") } returns ConfirmByCryptoResponse(updated = true, status = "paid")
        coEvery { api.getOrderByCryptoPayment("pay-1") } returns OrderDto(id = "o-crypto", status = "paid")
        val result = repository.handleReturnFromCrypto("pay-1")
        assertTrue(result is CheckoutReturnResult.Paid)
        coVerify { basketRepository.clear() }
        verify { pendingCheckoutStore.clearPendingEditOrder() }
        verify { pendingCheckoutStore.setCryptoPaymentId(null) }
    }

    private fun httpError(code: Int, body: String): HttpException {
        val response = Response.error<String>(
            code,
            body.toResponseBody("application/json".toMediaType())
        )
        return HttpException(response)
    }
}
