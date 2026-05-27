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
import com.eslamielectric.android.ui.theme.EslamiElectricTheme
import com.eslamielectric.android.util.LocaleHelper

class MainActivity : ComponentActivity() {

    private val app get() = application as EslamiElectricApp

    private var deepLinkGuestToken by mutableStateOf<String?>(null)

    override fun attachBaseContext(newBase: Context) {
        val locale = LocaleHelper.readLocaleSync(newBase)
        super.attachBaseContext(LocaleHelper.wrap(newBase, locale))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkGuestToken = parseGuestOrderToken(intent)
        app.authRepository.handleOAuthDeepLink(intent)
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
                    sessionStore = app.sessionStore,
                    locale = locale,
                    deepLinkGuestToken = deepLinkGuestToken,
                    onDeepLinkConsumed = { deepLinkGuestToken = null },
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
        app.authRepository.handleOAuthDeepLink(intent)
        parseGuestOrderToken(intent)?.let { deepLinkGuestToken = it }
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
