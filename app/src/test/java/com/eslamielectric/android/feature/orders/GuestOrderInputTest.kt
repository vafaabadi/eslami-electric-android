package com.eslamielectric.android.feature.orders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuestOrderInputTest {

    @Test
    fun recognizesOrderNumbers() {
        assertTrue(GuestOrderInput.looksLikeOrderNumber("ORD-ABC123"))
        assertTrue(GuestOrderInput.looksLikeOrderNumber("ord-abc123"))
    }

    @Test
    fun recognizesUuidOrderRefs() {
        assertTrue(GuestOrderInput.looksLikeOrderNumber("550e8400-e29b-41d4-a716-446655440000"))
    }

    @Test
    fun rejectsInvalidOrderRefs() {
        assertFalse(GuestOrderInput.looksLikeOrderNumber("ORD-12"))
        assertFalse(GuestOrderInput.looksLikeOrderNumber("hello"))
    }

    @Test
    fun normalizesOrderNumberCase() {
        assertEquals("ORD-ABC123", GuestOrderInput.normalizeOrderRef("ord-abc123"))
        assertEquals("550e8400-e29b-41d4-a716-446655440000", GuestOrderInput.normalizeOrderRef("550e8400-e29b-41d4-a716-446655440000"))
    }

    @Test
    fun extractsTokenFromUrl() {
        val url = "https://www.eslamielectric.com/order.html?token=abc%2B123"
        assertEquals("abc+123", GuestOrderInput.extractTrackingToken(url))
    }

    @Test
    fun returnsRawTokenWhenNotUrl() {
        assertEquals("plain-token", GuestOrderInput.extractTrackingToken("plain-token"))
    }
}
