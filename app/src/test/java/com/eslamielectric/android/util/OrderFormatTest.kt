package com.eslamielectric.android.util

import com.eslamielectric.android.core.network.OrderDto
import com.eslamielectric.android.core.network.OrderLineItemDto
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderFormatTest {

    @Test
    fun formatOrderCentsUsd() {
        assertEquals("$10.50", formatOrderCents(1050, "usd"))
        assertEquals("—", formatOrderCents(null, "usd"))
    }

    @Test
    fun formatOrderCentsNonUsd() {
        assertEquals("10.50 EUR", formatOrderCents(1050, "eur"))
    }

    @Test
    fun normalizeOrderStatus() {
        assertEquals("pending", normalizeOrderStatus("  PENDING "))
        assertEquals("", normalizeOrderStatus(null))
    }

    @Test
    fun isPendingOrder() {
        assertTrue(isPendingOrder(OrderDto(id = "1", orderNumber = "ORD-1", status = "pending")))
        assertFalse(isPendingOrder(OrderDto(id = "1", orderNumber = "ORD-1", status = "paid")))
    }

    @Test
    fun formatShippingAddressJoinsFields() {
        val address = buildJsonObject {
            put("line1", "1 Main St")
            put("city", "London")
            put("country", "GB")
        }
        assertEquals("1 Main St, London, GB", formatShippingAddress(address))
    }

    @Test
    fun lineItemTotalPrefersAmountTotal() {
        val item = OrderLineItemDto(amountTotal = 500, unitAmount = 100, quantity = 2)
        assertEquals(500, lineItemTotalCents(item))
    }

    @Test
    fun lineItemTotalComputesFromUnitAndQty() {
        val item = OrderLineItemDto(amountTotal = null, unitAmount = 250, quantity = 3)
        assertEquals(750, lineItemTotalCents(item))
    }

    @Test
    fun lineItemTotalNullWhenMissingData() {
        assertNull(lineItemTotalCents(OrderLineItemDto()))
    }
}
