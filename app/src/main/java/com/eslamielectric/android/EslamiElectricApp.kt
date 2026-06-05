package com.eslamielectric.android

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.eslamielectric.android.core.data.BasketRepository
import com.eslamielectric.android.core.data.PendingCheckoutStore
import com.eslamielectric.android.core.data.SessionStore
import com.eslamielectric.android.core.network.ApiService
import com.eslamielectric.android.core.network.NetworkModule
import com.eslamielectric.android.feature.auth.AuthRepository
import com.eslamielectric.android.feature.basket.CheckoutRepository
import com.eslamielectric.android.feature.catalog.CatalogRepository
import com.eslamielectric.android.feature.notifications.NotificationChannels
import com.eslamielectric.android.feature.notifications.NotificationPreferencesRepository
import com.eslamielectric.android.feature.notifications.PushTokenManager
import com.eslamielectric.android.feature.orders.OrdersRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class EslamiElectricApp : Application(), ImageLoaderFactory {

    lateinit var sessionStore: SessionStore
        private set

    lateinit var basketRepository: BasketRepository
        private set

    lateinit var api: ApiService
        private set

    lateinit var catalogRepository: CatalogRepository
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var checkoutRepository: CheckoutRepository
        private set

    lateinit var ordersRepository: OrdersRepository
        private set

    /** Null when google-services.json is missing in the build — see [PushTokenManager]. */
    var pushTokenManager: PushTokenManager? = null
        private set

    lateinit var notificationPreferencesRepository: NotificationPreferencesRepository
        private set

    override fun newImageLoader(): ImageLoader {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "EslamiElectric-Android/${BuildConfig.VERSION_NAME}")
                        .build()
                )
            }
            .build()
        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
        basketRepository = BasketRepository(this)
        val pendingCheckoutStore = PendingCheckoutStore(this)
        api = NetworkModule.createApiService { sessionStore.getToken() }
        catalogRepository = CatalogRepository(api)
        authRepository = AuthRepository(api, sessionStore)
        checkoutRepository = CheckoutRepository(api, basketRepository, sessionStore, pendingCheckoutStore)
        ordersRepository = OrdersRepository(api, sessionStore)
        notificationPreferencesRepository = NotificationPreferencesRepository(api, sessionStore)
        initializeLocaleHint()
        // Pre-create FCM channels so background notification messages display correctly.
        NotificationChannels.ensureCreated(this)
        // Bind token lifecycle to login/logout/locale changes. Safe even if FCM is not configured.
        pushTokenManager = PushTokenManager(this, api, sessionStore).also { it.start() }
    }

    private fun initializeLocaleHint() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!sessionStore.isLocaleInitialized()) {
                    val hint = api.getLocaleHint()
                    sessionStore.applyLocaleHint(hint.defaultLang)
                }
            } catch (_: Exception) {
                sessionStore.applyLocaleHint("en")
            }
        }
    }
}
