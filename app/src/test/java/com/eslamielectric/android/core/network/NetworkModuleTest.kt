package com.eslamielectric.android.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkModuleTest {

    @Test
    fun bearer_returnsNullOnlyForNullToken() {
        assertNull(NetworkModule.bearer(null))
        assertEquals("Bearer ", NetworkModule.bearer(""))
    }

    @Test
    fun bearer_prefixesToken() {
        assertEquals("Bearer jwt-xyz", NetworkModule.bearer("jwt-xyz"))
    }
}
