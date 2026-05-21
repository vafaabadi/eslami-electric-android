package com.eslamielectric.android.util

/** Display-only USD formatting (matches web `formatPriceUSD` when currency is usd). */
fun formatPriceUsd(usd: Double): String = "$${"%.2f".format(usd)}"
