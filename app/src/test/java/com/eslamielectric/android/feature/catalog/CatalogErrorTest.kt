package com.eslamielectric.android.feature.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

class CatalogErrorTest {

    @Test
    fun mapCatalogError_wrapsIOException() {
        val message = mapCatalogError(IOException("broken pipe"))
        assertTrue(message.contains("Could not reach the store"))
    }

    @Test
    fun mapCatalogError_wrapsUnknownHost() {
        val message = mapCatalogError(UnknownHostException("no dns"))
        assertTrue(message.contains("10.0.2.2:3000"))
    }

    @Test
    fun mapCatalogError_usesThrowableMessageWhenPresent() {
        assertEquals("Custom failure", mapCatalogError(IllegalStateException("Custom failure")))
    }

    @Test
    fun mapCatalogError_fallsBackWhenMessageBlank() {
        val message = mapCatalogError(RuntimeException(""))
        assertEquals("Something went wrong. Please try again.", message)
    }
}
