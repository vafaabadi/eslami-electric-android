package com.eslamielectric.android.util

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** External web URLs — mirrors public site routes (EN/FA). */
object WebLinks {

    private const val SITE_BASE = "https://www.eslamielectric.com"
    private const val WHATSAPP_NUMBER = "989155417904"

    private const val WHATSAPP_MESSAGE_EN =
        "Hello, I found your shop online and would like to enquire about your products."
    private const val WHATSAPP_MESSAGE_FA =
        "سلام، از طریق سایت شما آشنا شدم و می‌خواهم درباره محصولات شما سوال کنم."

    fun privacyPolicyUrl(locale: String): String =
        "$SITE_BASE/${if (locale == "fa") "fa" else "en"}/privacy"

    fun whatsAppUrl(locale: String): String {
        val message = if (locale == "fa") WHATSAPP_MESSAGE_FA else WHATSAPP_MESSAGE_EN
        val encoded = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
        return "https://wa.me/$WHATSAPP_NUMBER?text=$encoded"
    }

    fun openInCustomTab(context: Context, url: String) {
        val uri = Uri.parse(url)
        val toolbarColor = ContextCompat.getColor(context, android.R.color.black)
        val params = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(toolbarColor)
            .build()
        CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(params)
            .setShowTitle(true)
            .build()
            .launchUrl(context, uri)
    }

    fun openWhatsApp(context: Context, locale: String) {
        openInCustomTab(context, whatsAppUrl(locale))
    }

    fun openPrivacyPolicy(context: Context, locale: String) {
        openInCustomTab(context, privacyPolicyUrl(locale))
    }
}
