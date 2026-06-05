package com.eslamielectric.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eslamielectric.android.ui.navigation.AppNavHost
import com.eslamielectric.android.ui.navigation.CheckoutRoutes
import com.eslamielectric.android.ui.theme.EslamiElectricTheme
import com.eslamielectric.android.util.LocaleHelper

class MainActivity : ComponentActivity() {

    private val app get() = application as EslamiElectricApp

    private var deepLinkGuestToken by mutableStateOf<String?>(null)
    private var deepLinkOrderId by mutableStateOf<String?>(null)
    private var deepLinkProductId by mutableStateOf<String?>(null)
    private var openOrdersFromDeepLink by mutableStateOf(false)
    private var openBasketFromDeepLink by mutableStateOf(false)
    private var deepLinkCheckoutResultRoute by mutableStateOf<String?>(null)

    override fun attachBaseContext(newBase: Context) {
        val locale = LocaleHelper.readLocaleSync(newBase)
        super.attachBaseContext(LocaleHelper.wrap(newBase, locale))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkGuestToken = parseGuestOrderToken(intent)
        applyPushDeepLink(intent)
        applyCheckoutResultDeepLink(intent)
        runCatching { app.authRepository.handleOAuthDeepLink(intent) }
        enableEdgeToEdge()
        setContent {
            val locale by app.sessionStore.localeFlow.collectAsState(initial = LocaleHelper.readLocaleSync(this))
            EslamiElectricTheme(locale = locale) {
                AppNavHost(
                    basketRepository = app.basketRepository,
                    catalogRepository = app.catalogRepository,
                    authRepository = app.authRepository,
                    checkoutRepository = app.checkoutRepository,
                    ordersRepository = app.ordersRepository,
                    notificationPreferencesRepository = app.notificationPreferencesRepository,
                    sessionStore = app.sessionStore,
                    locale = locale,
                    fcmConfigured = app.pushTokenManager?.isFcmReady() == true,
                    deepLinkGuestToken = deepLinkGuestToken,
                    deepLinkOrderId = deepLinkOrderId,
                    deepLinkProductId = deepLinkProductId,
                    openOrdersFromDeepLink = openOrdersFromDeepLink,
                    openBasketFromDeepLink = openBasketFromDeepLink,
                    deepLinkCheckoutResultRoute = deepLinkCheckoutResultRoute,
                    onDeepLinkConsumed = {
                        deepLinkGuestToken = null
                        deepLinkOrderId = null
                        deepLinkProductId = null
                        openOrdersFromDeepLink = false
                        openBasketFromDeepLink = false
                        deepLinkCheckoutResultRoute = null
                    },
                    onLocaleChanged = { _ ->
                        recreate()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        runCatching { app.authRepository.handleOAuthDeepLink(intent) }
        parseGuestOrderToken(intent)?.let { deepLinkGuestToken = it }
        applyPushDeepLink(intent)
        applyCheckoutResultDeepLink(intent)
    }

    private fun applyCheckoutResultDeepLink(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        if (data.scheme != "eslamielectric" || data.host != "checkout-result") return
        val success = data.getQueryParameter("success")?.toBooleanStrictOrNull() ?: false
        val orderNumber = data.getQueryParameter("orderNumber")
        val orderId = data.getQueryParameter("orderId")
        val guestToken = data.getQueryParameter("guestToken")
        deepLinkCheckoutResultRoute = CheckoutRoutes.result(success, orderNumber, orderId, guestToken)
    }

    private fun applyPushDeepLink(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        if (data.scheme != "eslamielectric" || data.host != "push") return
        val segs = data.pathSegments
        if (segs.isEmpty()) return
        when (segs[0]) {
            "order" -> {
                if (segs.size >= 2) deepLinkOrderId = segs[1]
                else openOrdersFromDeepLink = true
            }
            "orders" -> openOrdersFromDeepLink = true
            "basket" -> openBasketFromDeepLink = true
            "product" -> if (segs.size >= 2) deepLinkProductId = segs[1]
        }
    }

    companion object {
        fun parseGuestOrderToken(intent: Intent?): String? {
            val data: Uri = intent?.data ?: return null
            val tokenFromQuery = data.getQueryParameter("token")?.trim()?.takeIf { it.isNotEmpty() }
            if (tokenFromQuery != null) return tokenFromQuery
            if (data.scheme == "eslamielectric" && data.host == "order") {
                return data.getQueryParameter("token")?.trim()?.takeIf { it.isNotEmpty() }
            }
            return null
        }
    }
}
