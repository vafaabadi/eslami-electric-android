package com.eslamielectric.android.util

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
/**
 * Opens Stripe Checkout URL in Chrome Custom Tabs (see docs/mobile-api.md).
 */
object StripeCheckoutTabs {

    fun open(context: Context, checkoutUrl: String) {
        val uri = Uri.parse(checkoutUrl)
        val toolbarColor = ContextCompat.getColor(context, android.R.color.black)
        val params = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(toolbarColor)
            .build()
        val intent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(params)
            .setShowTitle(true)
            .build()
        intent.launchUrl(context, uri)
    }
}
