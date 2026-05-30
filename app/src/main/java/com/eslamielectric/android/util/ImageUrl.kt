package com.eslamielectric.android.util

import com.eslamielectric.android.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Resolves product image URLs from the API. Relative paths (e.g. `/images/products/...`)
 * are prefixed with [BuildConfig.API_BASE_URL] so Coil can load them on device.
 */
fun resolveProductImageUrl(raw: String?): String? {
    val trimmed = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    ) {
        return trimmed
    }
    val base = BuildConfig.API_BASE_URL.trimEnd('/').toHttpUrlOrNull() ?: return trimmed
    val path = trimmed.trimStart('/')
    if (path.isEmpty()) return null
    val builder = base.newBuilder().encodedPath("/")
    path.split('/').filter { it.isNotEmpty() }.forEach { segment ->
        builder.addPathSegment(segment)
    }
    return builder.build().toString()
}
