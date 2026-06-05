package com.eslamielectric.android.util

import com.eslamielectric.android.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Resolves product image URLs from the API. Relative paths (e.g. `/images/products/...`)
 * are prefixed with [BuildConfig.API_BASE_URL] so Coil can load them on device.
 */
fun resolveProductImageUrl(raw: String?): String? =
    resolveProductImageUrl(raw, BuildConfig.API_BASE_URL)

/** Resolves product image URLs; [apiBaseUrl] is injectable for unit tests. */
internal fun resolveProductImageUrl(raw: String?, apiBaseUrl: String): String? {
    val trimmed = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    ) {
        return trimmed
    }
    if (trimmed.startsWith("//")) {
        return "https:$trimmed"
    }
    val base = apiBaseUrl.trimEnd('/').toHttpUrlOrNull() ?: return null
    val link = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
    return base.resolve(link)?.toString()
}
