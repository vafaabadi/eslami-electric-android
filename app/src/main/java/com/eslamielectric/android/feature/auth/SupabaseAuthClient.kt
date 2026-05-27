package com.eslamielectric.android.feature.auth

import android.content.Intent
import com.eslamielectric.android.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.ExternalAuthAction
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.handleDeeplinks
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.user.UserSession

/** Deep link redirect: eslamielectric://auth-callback (must match Supabase Auth redirect URLs). */
object OAuthRedirect {
    const val SCHEME = "eslamielectric"
    const val HOST = "auth-callback"
    const val URI = "$SCHEME://$HOST"
}

class SupabaseAuthClient private constructor(
    private val client: io.github.jan.supabase.SupabaseClient
) {
    val isConfigured: Boolean = true

    suspend fun signInWithGoogle() {
        client.auth.signInWith(Google) {
            queryParams["prompt"] = "select_account"
        }
    }

    fun handleOAuthDeepLink(intent: Intent?, onSession: (UserSession) -> Unit): Boolean {
        val data = intent?.data ?: return false
        if (data.scheme != OAuthRedirect.SCHEME || data.host != OAuthRedirect.HOST) return false
        client.handleDeeplinks(intent) { session -> onSession(session) }
        return true
    }

    companion object {
        fun fromBuildConfig(): SupabaseAuthClient? {
            val url = BuildConfig.SUPABASE_URL.trim()
            val key = BuildConfig.SUPABASE_ANON_KEY.trim()
            if (url.isBlank() || key.isBlank()) return null
            val client = createSupabaseClient(url, key) {
                install(Auth) {
                    scheme = OAuthRedirect.SCHEME
                    host = OAuthRedirect.HOST
                    defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
                }
            }
            return SupabaseAuthClient(client)
        }
    }
}
