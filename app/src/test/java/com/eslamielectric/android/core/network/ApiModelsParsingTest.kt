package com.eslamielectric.android.core.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiModelsParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parsesLoginResponse() {
        val raw = """
            {
              "ok": true,
              "token": "jwt-abc",
              "user": { "id": "u1", "email": "a@b.com", "firstName": "Ann" }
            }
        """.trimIndent()
        val dto = json.decodeFromString<LoginResponse>(raw)
        assertTrue(dto.ok)
        assertEquals("jwt-abc", dto.token)
        assertEquals("u1", dto.user.id)
        assertEquals("Ann", dto.user.firstName)
    }

    @Test
    fun parsesOrderDtoWithSnakeCaseFields() {
        val raw = """
            {
              "id": "ord-1",
              "order_number": "ORD-100",
              "stripe_session_id": "sess_x",
              "amount_total": 2500,
              "currency": "gbp",
              "status": "pending",
              "fulfillment_type": "delivery",
              "customer_email": "guest@example.com",
              "line_items": [
                { "name": "Bulb", "quantity": 2, "unit_amount": 500, "product_id": "p1" }
              ]
            }
        """.trimIndent()
        val order = json.decodeFromString<OrderDto>(raw)
        assertEquals("ORD-100", order.orderNumber)
        assertEquals("sess_x", order.stripeSessionId)
        assertEquals(2500, order.amountTotal)
        assertEquals("delivery", order.fulfillmentType)
        assertEquals(1, order.lineItems?.size)
        assertEquals("p1", order.lineItems?.single()?.productId)
    }

    @Test
    fun parsesBasketDraftResponse() {
        val raw = """
            {
              "orderId": "uuid-1",
              "orderNumber": "ORD-EDIT",
              "fulfillmentType": "collection",
              "basket": [
                { "id": "p1", "name": "Cable", "name_fa": "کابل", "price": 3.5, "quantity": 2 }
              ]
            }
        """.trimIndent()
        val draft = json.decodeFromString<BasketDraftResponse>(raw)
        assertEquals("uuid-1", draft.orderId)
        assertEquals("ORD-EDIT", draft.orderNumber)
        assertEquals("collection", draft.fulfillmentType)
        assertEquals("کابل", draft.basket.single().nameFa)
    }

    @Test
    fun parsesCheckoutSessionResponse() {
        val raw = """{"url":"https://checkout.stripe.test","sessionId":"sess_99"}"""
        val session = json.decodeFromString<CheckoutSessionResponse>(raw)
        assertEquals("sess_99", session.sessionId)
        assertTrue(session.url.contains("stripe"))
    }

    @Test
    fun parsesApiErrorResponseWithLockedUntil() {
        val raw = """
            {
              "error": "Too many attempts",
              "code": "RATE_LIMITED",
              "lockedUntil": "2026-06-06T12:00:00Z"
            }
        """.trimIndent()
        val err = json.decodeFromString<ApiErrorResponse>(raw)
        assertEquals("RATE_LIMITED", err.code)
        assertEquals("2026-06-06T12:00:00Z", err.lockedUntil)
    }

    @Test
    fun parsesProfileDtoCheckoutFields() {
        val raw = """
            {
              "id": "u1",
              "type": "individual",
              "checkoutProfileComplete": false,
              "checkoutProfileMissing": ["mobile", "address"]
            }
        """.trimIndent()
        val profile = json.decodeFromString<ProfileDto>(raw)
        assertFalse(profile.checkoutProfileComplete!!)
        assertEquals(listOf("mobile", "address"), profile.checkoutProfileMissing)
    }

    @Test
    fun parsesLocaleHintResponse() {
        val raw = """
            {
              "country": "IR",
              "inIran": true,
              "defaultLang": "fa",
              "defaultCurrency": "IRT",
              "usdToToman": 900000
            }
        """.trimIndent()
        val hint = json.decodeFromString<LocaleHintResponse>(raw)
        assertTrue(hint.inIran)
        assertEquals("fa", hint.defaultLang)
        assertEquals(900000, hint.usdToToman)
    }

    @Test
    fun serializesCreateCheckoutSessionRequest() {
        val request = CreateCheckoutSessionRequest(
            lineItems = listOf(CheckoutLineItemRequest(name = "Bulb", price = 9.99, quantity = 1, productId = "p1")),
            guestEmail = "a@b.com",
            locale = "en",
            fulfillmentType = "delivery",
            pendingOrderId = "ord-pending"
        )
        val encoded = json.encodeToString(CreateCheckoutSessionRequest.serializer(), request)
        assertTrue(encoded.contains("\"guestEmail\":\"a@b.com\""))
        assertTrue(encoded.contains("\"pendingOrderId\":\"ord-pending\""))
    }

    @Test
    fun parsesProductDtoSnakeCaseImageUrl() {
        val raw = """
            {
              "id": "p1",
              "name": "Switch",
              "name_fa": "سوئیچ",
              "price": 12.5,
              "image_url": "/img/switch.jpg",
              "categoryId": "cat-9"
            }
        """.trimIndent()
        val product = json.decodeFromString<ProductDto>(raw)
        assertEquals("/img/switch.jpg", product.imageUrl)
        assertEquals("سوئیچ", product.nameFa)
        assertEquals("cat-9", product.categoryId)
    }

    @Test
    fun parsesResumeCheckoutResponse() {
        val raw = """{"url":"https://pay.stripe.test","recreated":true}"""
        val resume = json.decodeFromString<ResumeCheckoutResponse>(raw)
        assertTrue(resume.recreated!!)
        assertNull(resume.url.takeIf { it.isBlank() })
    }
}
