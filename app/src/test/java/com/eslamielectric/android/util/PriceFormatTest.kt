package com.eslamielectric.android.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PriceFormatTest {

    @Test
    fun formatsUsdWithTwoDecimals() {
        assertEquals("$12.50", formatPriceUsd(12.5))
        assertEquals("$0.00", formatPriceUsd(0.0))
        assertEquals("$999.99", formatPriceUsd(999.99))
    }

    @Test
    fun formatsLargeAmountsWithoutThousandsSeparator() {
        assertEquals("$1234.56", formatPriceUsd(1234.56))
    }

    @Test
    fun padsSingleDigitCents() {
        assertEquals("$10.05", formatPriceUsd(10.05))
        assertEquals("$10.50", formatPriceUsd(10.5))
    }
}
