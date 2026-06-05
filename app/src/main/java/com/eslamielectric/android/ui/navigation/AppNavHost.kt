package com.eslamielectric.android.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.eslamielectric.android.core.data.BasketRepository
import com.eslamielectric.android.core.data.SessionStore
import com.eslamielectric.android.feature.auth.AuthRepository
import com.eslamielectric.android.feature.basket.CheckoutException
import com.eslamielectric.android.feature.basket.CheckoutRepository
import com.eslamielectric.android.feature.catalog.CatalogRepository
import com.eslamielectric.android.feature.notifications.NotificationPreferencesRepository
import com.eslamielectric.android.feature.orders.OrdersRepository
import com.eslamielectric.android.ui.screens.AccountHomeScreen
import com.eslamielectric.android.ui.screens.ClaimAccountScreen
import com.eslamielectric.android.ui.screens.GuestTrackScreen
import com.eslamielectric.android.ui.screens.MyOrdersScreen
import com.eslamielectric.android.ui.screens.NotificationsScreen
import com.eslamielectric.android.ui.screens.OrderDetailScreen
import com.eslamielectric.android.ui.screens.BasketScreen
import com.eslamielectric.android.ui.screens.CheckoutResultScreen
import com.eslamielectric.android.ui.screens.CheckoutScreen
import com.eslamielectric.android.ui.screens.ForgotPasswordScreen
import com.eslamielectric.android.ui.screens.ResetPasswordScreen
import com.eslamielectric.android.util.WebLinks
import com.eslamielectric.android.R
import com.eslamielectric.android.ui.screens.HomeScreen
import com.eslamielectric.android.ui.screens.LoginScreen
import com.eslamielectric.android.ui.screens.ProductDetailScreen
import com.eslamielectric.android.ui.screens.ProductsScreen
import com.eslamielectric.android.ui.screens.ProfileScreen
import com.eslamielectric.android.ui.screens.SignUpScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppNavHost(
    basketRepository: BasketRepository,
    catalogRepository: CatalogRepository,
    authRepository: AuthRepository,
    checkoutRepository: CheckoutRepository,
    ordersRepository: OrdersRepository,
    notificationPreferencesRepository: NotificationPreferencesRepository,
    sessionStore: SessionStore,
    locale: String = "en",
    fcmConfigured: Boolean = false,
    deepLinkGuestToken: String? = null,
    deepLinkOrderId: String? = null,
    deepLinkProductId: String? = null,
    openOrdersFromDeepLink: Boolean = false,
    openBasketFromDeepLink: Boolean = false,
    deepLinkEditOrderId: String? = null,
    deepLinkResetToken: String? = null,
    deepLinkClaimToken: String? = null,
    deepLinkCheckoutResultRoute: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
    onLocaleChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val basketItems by basketRepository.itemsFlow.collectAsState(initial = emptyList())
    val basketCount = basketRepository.itemCount(basketItems)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val pendingEditOrderLabel = checkoutRepository.getPendingEditOrder()?.orderLabel

    val editPendingOrder: (String) -> Unit = { orderId ->
        scope.launch {
            try {
                checkoutRepository.loadPendingOrderForEdit(orderId)
                navController.navigate(AppDestinations.Basket.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                }
            } catch (_: CheckoutException.SessionExpired) {
                navController.navigate("${AppDestinations.Account.route}?openLogin=true") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                }
            } catch (_: Exception) {
                // Basket draft errors surface when user retries from order detail.
            }
        }
    }

    LaunchedEffect(deepLinkGuestToken) {
        val token = deepLinkGuestToken?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        navController.navigate(CatalogRoutes.guestOrderByToken(token)) {
            launchSingleTop = true
        }
        onDeepLinkConsumed()
    }

    LaunchedEffect(deepLinkCheckoutResultRoute) {
        val route = deepLinkCheckoutResultRoute?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        navController.navigate(route) {
            launchSingleTop = true
        }
        onDeepLinkConsumed()
    }

    // Notification taps route here. For order:<id> we open the order detail under Account so the
    // Back stack lands the user at Account → My orders, which mirrors web behaviour.
    LaunchedEffect(deepLinkEditOrderId) {
        val orderId = deepLinkEditOrderId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (authRepository.isLoggedIn()) {
            editPendingOrder(orderId)
        } else {
            navController.navigate("${AppDestinations.Account.route}?openLogin=true") {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
            }
        }
        onDeepLinkConsumed()
    }

    LaunchedEffect(deepLinkResetToken) {
        val token = deepLinkResetToken?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        navController.navigate(
            "${AppDestinations.Account.route}?openReset=true&resetToken=${android.net.Uri.encode(token)}"
        ) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
        }
        onDeepLinkConsumed()
    }

    LaunchedEffect(deepLinkClaimToken) {
        val token = deepLinkClaimToken?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        navController.navigate(
            "${AppDestinations.Account.route}?openClaim=true&claimToken=${android.net.Uri.encode(token)}"
        ) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
        }
        onDeepLinkConsumed()
    }

    LaunchedEffect(deepLinkOrderId, deepLinkProductId, openOrdersFromDeepLink, openBasketFromDeepLink) {
        val orderId = deepLinkOrderId?.takeIf { it.isNotBlank() }
        val productId = deepLinkProductId?.takeIf { it.isNotBlank() }
        when {
            orderId != null -> {
                navController.navigate("${AppDestinations.Account.route}?openOrder=$orderId") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                }
                onDeepLinkConsumed()
            }
            openOrdersFromDeepLink -> {
                navController.navigate("${AppDestinations.Account.route}?openOrders=true") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                }
                onDeepLinkConsumed()
            }
            openBasketFromDeepLink -> {
                navController.navigate(AppDestinations.Basket.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                }
                onDeepLinkConsumed()
            }
            productId != null -> {
                navController.navigate(CatalogRoutes.productDetail(productId)) {
                    launchSingleTop = true
                }
                onDeepLinkConsumed()
            }
        }
    }

    val currentRouteBase = currentRoute?.substringBefore('?')?.substringBefore("/{")
    val showBottomBar = currentRouteBase in AppDestinations.bottomNav.map { it.route }

    Scaffold(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        floatingActionButton = {
            if (showBottomBar) {
                FloatingActionButton(
                    onClick = { WebLinks.openWhatsApp(context, locale) },
                    modifier = Modifier.testTag("fab_whatsapp")
                ) {
                    Icon(
                        Icons.Filled.Chat,
                        contentDescription = stringResource(R.string.whatsapp_fab_content_description)
                    )
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    AppDestinations.bottomNav.forEach { dest ->
                        NavigationBarItem(
                            modifier = Modifier.testTag("nav_tab_${dest.route}"),
                            selected = currentRouteBase == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (dest == AppDestinations.Basket && basketCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge { Text(basketCount.toString()) }
                                        }
                                    ) {
                                        Icon(dest.icon, contentDescription = null)
                                    }
                                } else {
                                    Icon(dest.icon, contentDescription = null)
                                }
                            },
                            label = { Text(stringResource(dest.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestinations.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestinations.Home.route) {
                HomeScreen(
                    catalogRepository = catalogRepository,
                    basketRepository = basketRepository,
                    locale = locale,
                    onViewAllProducts = {
                        navController.navigate(AppDestinations.Products.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onProductClick = { product ->
                        navController.navigate(CatalogRoutes.productDetail(product.id))
                    }
                )
            }
            composable(AppDestinations.Products.route) {
                ProductsScreen(
                    catalogRepository = catalogRepository,
                    basketRepository = basketRepository,
                    locale = locale,
                    onProductClick = { product ->
                        navController.navigate(CatalogRoutes.productDetail(product.id))
                    }
                )
            }
            composable(
                route = CatalogRoutes.PRODUCT_DETAIL,
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { entry ->
                val productId = entry.arguments?.getString("productId").orEmpty()
                ProductDetailScreen(
                    productId = productId,
                    catalogRepository = catalogRepository,
                    basketRepository = basketRepository,
                    locale = locale,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = CatalogRoutes.GUEST_ORDER_BY_TOKEN,
                arguments = listOf(navArgument("token") { type = NavType.StringType })
            ) { entry ->
                val token = entry.arguments?.getString("token").orEmpty()
                OrderDetailScreen(
                    ordersRepository = ordersRepository,
                    orderId = "",
                    guestToken = token,
                    isGuest = true,
                    locale = locale,
                    onBack = {
                        navController.navigate(AppDestinations.Account.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onSessionExpired = {
                        navController.navigate("${AppDestinations.Account.route}?openLogin=true") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onProfileIncomplete = {
                        navController.navigate("${AppDestinations.Account.route}?openProfile=true") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppDestinations.Basket.route) {
                BasketScreen(
                    items = basketItems,
                    basketRepository = basketRepository,
                    pendingEditOrderLabel = pendingEditOrderLabel,
                    onProceedToCheckout = {
                        navController.navigate(CheckoutRoutes.CHECKOUT)
                    }
                )
            }
            composable(CheckoutRoutes.CHECKOUT) {
                CheckoutScreen(
                    checkoutRepository = checkoutRepository,
                    authRepository = authRepository,
                    basketRepository = basketRepository,
                    locale = locale,
                    onBack = { navController.popBackStack() },
                    onSessionExpired = {
                        navController.navigate("${AppDestinations.Account.route}?openLogin=true") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToProfile = {
                        navController.navigate("${AppDestinations.Account.route}?openProfile=true") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onPaymentComplete = { order ->
                        order.guestAccessToken?.let { token ->
                            ordersRepository.cacheGuestOrder(order, token)
                        }
                        navController.navigate(
                            CheckoutRoutes.result(
                                success = true,
                                orderNumber = order.orderNumber,
                                orderId = order.id,
                                guestToken = order.guestAccessToken
                            )
                        ) {
                            popUpTo(CheckoutRoutes.CHECKOUT) { inclusive = true }
                        }
                    },
                    onPaymentIncomplete = {
                        navController.navigate(CheckoutRoutes.result(false, null, null, null)) {
                            popUpTo(CheckoutRoutes.CHECKOUT) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = CheckoutRoutes.RESULT,
                arguments = listOf(
                    navArgument("success") { type = NavType.BoolType },
                    navArgument("orderNumber") { type = NavType.StringType },
                    navArgument("orderId") { type = NavType.StringType },
                    navArgument("guestToken") { type = NavType.StringType }
                )
            ) { entry ->
                val success = entry.arguments?.getBoolean("success") ?: false
                val orderNumber = entry.arguments?.getString("orderNumber")?.takeIf { it != "-" }
                val orderId = entry.arguments?.getString("orderId")?.takeIf { it != "-" }
                val guestToken = entry.arguments?.getString("guestToken")?.takeIf { it != "-" }
                val canTrackGuestOrder = success && !orderId.isNullOrBlank() && !guestToken.isNullOrBlank()
                val canClaimAccount = success && canTrackGuestOrder && !authRepository.isLoggedIn()
                CheckoutResultScreen(
                    success = success,
                    order = if (orderNumber != null) {
                        com.eslamielectric.android.core.network.OrderDto(
                            id = orderId.orEmpty(),
                            orderNumber = orderNumber,
                            guestAccessToken = guestToken
                        )
                    } else {
                        null
                    },
                    message = null,
                    onTrackOrder = if (canTrackGuestOrder) {
                        {
                            navController.navigate(CatalogRoutes.guestOrderByToken(guestToken!!)) {
                                popUpTo(CheckoutRoutes.RESULT) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    } else {
                        null
                    },
                    onClaimAccount = if (canClaimAccount) {
                        {
                            navController.navigate("${AppDestinations.Account.route}?openClaim=true") {
                                popUpTo(CheckoutRoutes.RESULT) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    } else {
                        null
                    },
                    onDone = {
                        navController.navigate(AppDestinations.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(
                route = "${AppDestinations.Account.route}?openProfile={openProfile}&openLogin={openLogin}&openOrder={openOrder}&openOrders={openOrders}&openReset={openReset}&resetToken={resetToken}&openClaim={openClaim}&claimToken={claimToken}",
                arguments = listOf(
                    navArgument("openProfile") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("openLogin") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("openOrder") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("openOrders") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("openReset") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("resetToken") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("openClaim") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("claimToken") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val openProfile = entry.arguments?.getBoolean("openProfile") ?: false
                val openLogin = entry.arguments?.getBoolean("openLogin") ?: false
                val openOrder = entry.arguments?.getString("openOrder")?.takeIf { it.isNotBlank() }
                val openOrders = entry.arguments?.getBoolean("openOrders") ?: false
                val openReset = entry.arguments?.getBoolean("openReset") ?: false
                val resetToken = entry.arguments?.getString("resetToken").orEmpty()
                val openClaim = entry.arguments?.getBoolean("openClaim") ?: false
                val claimToken = entry.arguments?.getString("claimToken").orEmpty()
                val accountNav = rememberNavController()
                val accountStart = when {
                    openReset -> AccountRoutes.resetPassword(resetToken)
                    openClaim -> AccountRoutes.claimAccount(claimToken)
                    openLogin -> AccountRoutes.LOGIN
                    openProfile -> AccountRoutes.PROFILE
                    openOrder != null -> OrderRoutes.orderDetail(openOrder)
                    openOrders -> AccountRoutes.ORDERS
                    else -> AccountRoutes.HOME
                }
                NavHost(
                    navController = accountNav,
                    startDestination = accountStart
                ) {
                    composable(AccountRoutes.HOME) {
                        AccountHomeScreen(
                            authRepository = authRepository,
                            locale = locale,
                            onLocaleChange = { newLocale ->
                                scope.launch {
                                    sessionStore.setLocale(newLocale)
                                    onLocaleChanged(newLocale)
                                }
                            },
                            onLogin = { accountNav.navigate(AccountRoutes.LOGIN) },
                            onSignUp = { accountNav.navigate(AccountRoutes.SIGNUP) },
                            onProfile = { accountNav.navigate(AccountRoutes.PROFILE) },
                            onMyOrders = { accountNav.navigate(AccountRoutes.ORDERS) },
                            onGuestTrack = { accountNav.navigate(AccountRoutes.GUEST_TRACK) },
                            onNotifications = { accountNav.navigate(AccountRoutes.NOTIFICATIONS) },
                            onClaimAccount = { accountNav.navigate(AccountRoutes.claimAccount()) }
                        )
                    }
                    composable(AccountRoutes.NOTIFICATIONS) {
                        NotificationsScreen(
                            repository = notificationPreferencesRepository,
                            fcmConfigured = fcmConfigured,
                            onBack = { accountNav.popBackStack() },
                            onSessionExpired = {
                                accountNav.navigate(AccountRoutes.LOGIN) {
                                    popUpTo(AccountRoutes.HOME) { inclusive = false }
                                }
                            }
                        )
                    }
                    composable(AccountRoutes.ORDERS) {
                        MyOrdersScreen(
                            ordersRepository = ordersRepository,
                            locale = locale,
                            onBack = { accountNav.popBackStack() },
                            onOrderClick = { order ->
                                accountNav.navigate(OrderRoutes.orderDetail(order.id))
                            },
                            onEditBeforePayment = { order -> editPendingOrder(order.id) },
                            onSessionExpired = {
                                accountNav.navigate(AccountRoutes.LOGIN) {
                                    popUpTo(AccountRoutes.HOME) { inclusive = false }
                                }
                            }
                        )
                    }
                    composable(
                        route = OrderRoutes.ORDER_DETAIL,
                        arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                    ) { entry ->
                        val orderId = entry.arguments?.getString("orderId").orEmpty()
                        OrderDetailScreen(
                            ordersRepository = ordersRepository,
                            orderId = orderId,
                            guestToken = null,
                            isGuest = false,
                            locale = locale,
                            onBack = { accountNav.popBackStack() },
                            onSessionExpired = {
                                accountNav.navigate(AccountRoutes.LOGIN) {
                                    popUpTo(AccountRoutes.HOME) { inclusive = false }
                                }
                            },
                            onProfileIncomplete = { accountNav.navigate(AccountRoutes.PROFILE) },
                            onEditBeforePayment = { editPendingOrder(orderId) }
                        )
                    }
                    composable(AccountRoutes.GUEST_TRACK) {
                        GuestTrackScreen(
                            ordersRepository = ordersRepository,
                            onBack = { accountNav.popBackStack() },
                            onOrderFound = { order, token ->
                                accountNav.navigate(OrderRoutes.guestOrderDetail(order.id, token))
                            }
                        )
                    }
                    composable(
                        route = OrderRoutes.GUEST_ORDER_DETAIL,
                        arguments = listOf(
                            navArgument("orderId") { type = NavType.StringType },
                            navArgument("guestToken") {
                                type = NavType.StringType
                                defaultValue = "-"
                            }
                        )
                    ) { entry ->
                        val orderId = entry.arguments?.getString("orderId").orEmpty()
                        val token = entry.arguments?.getString("guestToken")?.takeIf { it != "-" }
                        OrderDetailScreen(
                            ordersRepository = ordersRepository,
                            orderId = orderId,
                            guestToken = token,
                            isGuest = true,
                            locale = locale,
                            onBack = { accountNav.popBackStack() },
                            onSessionExpired = {
                                accountNav.navigate(AccountRoutes.LOGIN) {
                                    popUpTo(AccountRoutes.HOME) { inclusive = false }
                                }
                            },
                            onProfileIncomplete = { accountNav.navigate(AccountRoutes.PROFILE) }
                        )
                    }
                    composable(AccountRoutes.LOGIN) {
                        LoginScreen(
                            authRepository = authRepository,
                            onBack = { accountNav.popBackStack() },
                            onSignUp = {
                                accountNav.navigate(AccountRoutes.SIGNUP) {
                                    popUpTo(AccountRoutes.LOGIN) { inclusive = true }
                                }
                            },
                            onForgotPassword = { accountNav.navigate(AccountRoutes.FORGOT_PASSWORD) },
                            onLoggedIn = {
                                accountNav.navigate(AccountRoutes.HOME) {
                                    popUpTo(AccountRoutes.HOME) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable(AccountRoutes.SIGNUP) {
                        SignUpScreen(
                            authRepository = authRepository,
                            onBack = { accountNav.popBackStack() },
                            onSignedUp = {
                                accountNav.navigate(AccountRoutes.HOME) {
                                    popUpTo(AccountRoutes.HOME) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable(AccountRoutes.FORGOT_PASSWORD) {
                        ForgotPasswordScreen(
                            authRepository = authRepository,
                            onBack = { accountNav.popBackStack() }
                        )
                    }
                    composable(
                        route = AccountRoutes.RESET_PASSWORD,
                        arguments = listOf(
                            navArgument("token") {
                                type = NavType.StringType
                                defaultValue = ""
                            }
                        )
                    ) { resetEntry ->
                        val token = resetEntry.arguments?.getString("token").orEmpty()
                        ResetPasswordScreen(
                            authRepository = authRepository,
                            initialToken = token,
                            onBack = { accountNav.popBackStack() },
                            onResetSuccess = {
                                accountNav.navigate(AccountRoutes.LOGIN) {
                                    popUpTo(AccountRoutes.HOME) { inclusive = false }
                                }
                            }
                        )
                    }
                    composable(
                        route = AccountRoutes.CLAIM_ACCOUNT,
                        arguments = listOf(
                            navArgument("token") {
                                type = NavType.StringType
                                defaultValue = ""
                            }
                        )
                    ) { claimEntry ->
                        val token = claimEntry.arguments?.getString("token").orEmpty()
                        ClaimAccountScreen(
                            authRepository = authRepository,
                            initialToken = token,
                            onBack = { accountNav.popBackStack() },
                            onClaimed = {
                                accountNav.navigate(AccountRoutes.ORDERS) {
                                    popUpTo(AccountRoutes.HOME) { inclusive = false }
                                }
                            }
                        )
                    }
                    composable(AccountRoutes.PROFILE) {
                        ProfileScreen(
                            authRepository = authRepository,
                            onBack = { accountNav.popBackStack() },
                            onSessionExpired = {
                                accountNav.navigate(AccountRoutes.LOGIN) {
                                    popUpTo(AccountRoutes.HOME) { inclusive = false }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
