package com.eslamielectric.android

import android.app.Application
import com.eslamielectric.android.core.data.BasketRepository
import com.eslamielectric.android.core.data.PendingCheckoutStore
import com.eslamielectric.android.core.data.SessionStore
import com.eslamielectric.android.core.network.ApiService
import com.eslamielectric.android.core.network.NetworkModule
import com.eslamielectric.android.feature.auth.AuthRepository
import com.eslamielectric.android.feature.basket.CheckoutRepository
import com.eslamielectric.android.feature.catalog.CatalogRepository
import com.eslamielectric.android.feature.orders.OrdersRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EslamiElectricApp : Application() {

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
        initializeLocaleHint()
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
