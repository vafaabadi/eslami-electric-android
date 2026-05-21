package com.eslamielectric.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.ui.graphics.vector.ImageVector
import com.eslamielectric.android.R

enum class AppDestinations(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
) {
    Home("home", R.string.nav_home, Icons.Default.Home),
    Products("products", R.string.nav_products, Icons.Default.Store),
    Basket("basket", R.string.nav_basket, Icons.Default.ShoppingCart),
    Account("account", R.string.nav_account, Icons.Default.Person);

    companion object {
        val bottomNav = listOf(Home, Products, Basket, Account)
    }
}
