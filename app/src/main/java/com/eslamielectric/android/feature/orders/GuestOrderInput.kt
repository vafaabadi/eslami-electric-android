package com.eslamielectric.android.feature.orders

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** Parses and validates guest order lookup input (order number vs tracking token). */
object GuestOrderInput {
    private val ORDER_NUMBER = Regex("^ORD-[A-Z0-9]{6}$", RegexOption.IGNORE_CASE)
    private val UUID = Regex(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
        RegexOption.IGNORE_CASE
    )
    private val TOKEN_FROM_URL = Regex("(?:[?&#]|^)token=([^&#\\s]+)", RegexOption.IGNORE_CASE)

    fun looksLikeOrderNumber(value: String): Boolean {
        val trimmed = value.trim()
        return ORDER_NUMBER.matches(trimmed) || UUID.matches(trimmed)
    }

    fun normalizeOrderRef(value: String): String {
        val trimmed = value.trim()
        return if (ORDER_NUMBER.matches(trimmed)) trimmed.uppercase() else trimmed
    }

    /** Accept a raw token or a pasted tracking link (`order.html?token=…`). */
    fun extractTrackingToken(raw: String): String {
        val trimmed = raw.trim()
        TOKEN_FROM_URL.find(trimmed)?.groupValues?.getOrNull(1)?.let { encoded ->
            return URLDecoder.decode(encoded, StandardCharsets.UTF_8.name()).trim()
        }
        return trimmed
    }
}
