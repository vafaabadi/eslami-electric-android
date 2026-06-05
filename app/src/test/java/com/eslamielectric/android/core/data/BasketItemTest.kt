package com.eslamielectric.android.core.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BasketItemTest {

    @Test
    fun lineTotal_multipliesPriceByQuantity() {
        val item = BasketItem(id = "x", name = "X", price = 12.5, quantity = 3)
        assertEquals(37.5, item.lineTotal(), 0.001)
    }
}
