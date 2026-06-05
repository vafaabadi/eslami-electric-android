package com.eslamielectric.android.feature.orders

import com.eslamielectric.android.core.data.SessionStore
import com.eslamielectric.android.core.network.ApiService
import com.eslamielectric.android.core.network.OrderDto
import com.eslamielectric.android.core.network.SessionExpiredException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class OrdersRepositoryTest {

    private val api = mockk<ApiService>()
    private val sessionStore = mockk<SessionStore>(relaxed = true)
    private lateinit var repository: OrdersRepository

    @Before
    fun setUp() {
        repository = OrdersRepository(api, sessionStore)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun guestLookup_trimsEmailAndCachesOrder() = runTest {
        val order = OrderDto(id = "order-1", orderNumber = "ORD-ABC123")
        coEvery {
            api.guestOrderLookup(email = "buyer@test.com", orderId = "ORD-ABC123")
        } returns order
        val result = repository.guestLookup(" buyer@test.com ", " ORD-ABC123 ")
        assertSame(order, result)
        assertEquals(order, repository.getCachedGuestOrder("order-1"))
    }

    @Test
    fun loadGuestByToken_trimsTokenAndCachesWithToken() = runTest {
        val order = OrderDto(id = "order-2", guestAccessToken = "tok-xyz")
        coEvery { api.getGuestOrder("tok-xyz") } returns order
        val result = repository.loadGuestByToken("  tok-xyz  ")
        assertSame(order, result)
        assertEquals("tok-xyz", repository.getCachedGuestToken("order-2"))
    }

    @Test
    fun getCachedGuestOrder_returnsNullForMismatchedId() = runTest {
        repository.cacheGuestOrder(OrderDto(id = "a"), guestToken = "t")
        assertNull(repository.getCachedGuestOrder("b"))
    }

    @Test
    fun clearGuestCache_removesCachedData() = runTest {
        repository.cacheGuestOrder(OrderDto(id = "a"), guestToken = "t")
        repository.clearGuestCache()
        assertNull(repository.getCachedGuestOrder("a"))
        assertNull(repository.getCachedGuestToken("a"))
    }

    @Test(expected = SessionExpiredException::class)
    fun loadMyOrders_throwsWhenNotLoggedIn() = runTest {
        every { sessionStore.getToken() } returns null
        repository.loadMyOrders()
    }

    @Test
    fun loadMyOrders_usesBearerToken() = runTest {
        every { sessionStore.getToken() } returns "jwt"
        coEvery { api.getOrders("Bearer jwt") } returns listOf(OrderDto(id = "o1"))
        val orders = repository.loadMyOrders()
        assertEquals(1, orders.size)
    }

    @Test(expected = SessionExpiredException::class)
    fun loadMyOrders_clearsTokenOn401() = runTest {
        every { sessionStore.getToken() } returns "jwt"
        coEvery { api.getOrders(any()) } throws httpError(401, """{"error":"unauthorized"}""")
        try {
            repository.loadMyOrders()
        } finally {
            verify { sessionStore.setToken(null) }
        }
    }

    @Test
    fun logoutOnSessionExpired_clearsToken() = runTest {
        repository.logoutOnSessionExpired()
        verify { sessionStore.setToken(null) }
    }

    @Test
    fun mapOrdersResumeException_mapsSessionExpired() {
        val mapped = mapOrdersResumeException(SessionExpiredException())
        assertEquals(OrdersException.SessionExpired, mapped)
    }

    private fun httpError(code: Int, body: String): HttpException {
        val response = Response.error<String>(
            code,
            body.toResponseBody("application/json".toMediaType())
        )
        return HttpException(response)
    }
}
