package com.eslamielectric.android.core.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PushPreferencesParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parsesPushPreferencesDto() {
        val raw = """
            {
              "master_enabled": false,
              "channels": {
                "orders": true,
                "promotions": false,
                "account": true,
                "general": false
              },
              "updated_at": "2026-01-01T00:00:00Z"
            }
        """.trimIndent()

        val dto = json.decodeFromString<PushPreferencesDto>(raw)
        assertFalse(dto.masterEnabled)
        assertTrue(dto.channels.orders)
        assertFalse(dto.channels.promotions)
        assertTrue(dto.channels.account)
        assertFalse(dto.channels.general)
        assertEquals("2026-01-01T00:00:00Z", dto.updatedAt)
    }

    @Test
    fun serializesPatchRequest() {
        val request = PushPreferencesPatchRequest(
            masterEnabled = true,
            channels = PushChannelPatch(orders = false, promotions = null)
        )
        val encoded = json.encodeToString(PushPreferencesPatchRequest.serializer(), request)
        assertTrue(encoded.contains("\"master_enabled\":true"))
        assertTrue(encoded.contains("\"orders\":false"))
    }
}
