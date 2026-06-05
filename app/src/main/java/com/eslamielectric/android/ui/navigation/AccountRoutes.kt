package com.eslamielectric.android.ui.navigation

import android.net.Uri

object AccountRoutes {
    const val HOME = "account/home"
    const val LOGIN = "account/login"
    const val SIGNUP = "account/signup"
    const val PROFILE = "account/profile"
    const val FORGOT_PASSWORD = "account/forgot-password"
    const val RESET_PASSWORD = "account/reset-password?token={token}"
    const val CLAIM_ACCOUNT = "account/claim-account?token={token}"
    const val NOTIFICATIONS = "account/notifications"
    const val ORDERS = OrderRoutes.ORDERS_LIST
    const val GUEST_TRACK = OrderRoutes.GUEST_TRACK

    fun resetPassword(token: String = "") =
        "account/reset-password?token=${Uri.encode(token)}"

    fun claimAccount(token: String = "") =
        "account/claim-account?token=${Uri.encode(token)}"
}
