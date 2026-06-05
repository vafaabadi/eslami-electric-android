package com.eslamielectric.android.core.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class PendingCheckoutStoreTest {

    private lateinit var store: PendingCheckoutStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        store = PendingCheckoutStore(context)
        store.clearAll()
    }

    @Test
    fun sessionId_roundTripsAndClears() {
        store.setSessionId("sess_123")
        assertEquals("sess_123", store.getSessionId())
        store.setSessionId(null)
        assertNull(store.getSessionId())
        store.setSessionId("  ")
        assertNull(store.getSessionId())
    }

    @Test
    fun pendingEditOrder_persistsShippingAddressFields() {
        val address = buildJsonObject {
            put("line1", "12 Main St")
            put("city", "London")
            put("postal_code", "SW1A 1AA")
            put("additional_info", "Ring bell")
        }
        store.setPendingEditOrder(
            orderId = "order-uuid",
            orderLabel = "ORD-ABC123",
            fulfillmentType = "delivery",
            shippingAddress = address
        )
        val pending = store.getPendingEditOrder()
        requireNotNull(pending)
        assertEquals("order-uuid", pending.orderId)
        assertEquals("ORD-ABC123", pending.orderLabel)
        assertEquals("delivery", pending.fulfillmentType)
        assertEquals("12 Main St", pending.addressLine1)
        assertEquals("London", pending.addressCity)
        assertEquals("SW1A 1AA", pending.addressPostal)
        assertEquals("Ring bell", pending.addressExtra)
    }

    @Test
    fun clearPendingEditOrder_removesAllPendingFields() {
        store.setPendingEditOrder("id", "ORD-1", "pickup", null)
        store.clearPendingEditOrder()
        assertNull(store.getPendingEditOrder())
    }
}
