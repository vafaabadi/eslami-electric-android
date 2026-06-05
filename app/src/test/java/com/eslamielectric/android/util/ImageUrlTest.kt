package com.eslamielectric.android.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageUrlTest {

    @Test
    fun returnsNullForBlankInput() {
        assertNull(resolveProductImageUrl(null, "https://example.com"))
        assertNull(resolveProductImageUrl("", "https://example.com"))
        assertNull(resolveProductImageUrl("   ", "https://example.com"))
    }

    @Test
    fun passesThroughAbsoluteHttpUrls() {
        assertEquals(
            "https://cdn.example.com/img.png",
            resolveProductImageUrl("https://cdn.example.com/img.png", "https://example.com")
        )
        assertEquals(
            "http://cdn.example.com/img.png",
            resolveProductImageUrl("http://cdn.example.com/img.png", "https://example.com")
        )
    }

    @Test
    fun resolvesProtocolRelativeUrls() {
        assertEquals(
            "https://cdn.example.com/img.png",
            resolveProductImageUrl("//cdn.example.com/img.png", "https://example.com")
        )
    }

    @Test
    fun resolvesRelativePathsAgainstBase() {
        val base = "https://www.eslamielectric.com"
        assertEquals(
            "https://www.eslamielectric.com/images/products/a.jpg",
            resolveProductImageUrl("/images/products/a.jpg", base)
        )
        assertEquals(
            "https://www.eslamielectric.com/images/products/b.jpg",
            resolveProductImageUrl("images/products/b.jpg", base)
        )
    }

    @Test
    fun returnsNullWhenBaseIsInvalid() {
        assertNull(resolveProductImageUrl("/images/x.jpg", "not-a-url"))
    }
}
