package com.eslamielectric.android.util

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.eslamielectric.android.core.network.CategoryDto
import com.eslamielectric.android.core.network.ProductDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

private val Context.sessionDataStore by preferencesDataStore(name = "eslami_session_prefs")

object LocaleHelper {

    private val localeKey = stringPreferencesKey("locale")
    private val localeUserSetKey = stringPreferencesKey("locale_user_set")

    fun readLocaleSync(context: Context): String = runBlocking {
        context.sessionDataStore.data.first()[localeKey] ?: "en"
    }

    fun isUserLocaleSet(context: Context): Boolean = runBlocking {
        context.sessionDataStore.data.first()[localeUserSetKey] == "1"
    }

    fun wrap(context: Context, localeTag: String): Context {
        val tag = if (localeTag == "fa") "fa" else "en"
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}

fun ProductDto.displayName(locale: String): String =
    if (locale == "fa" && nameFa.isNotBlank()) nameFa else name

fun ProductDto.displayDescription(locale: String): String =
    if (locale == "fa" && descriptionFa.isNotBlank()) descriptionFa else description

fun ProductDto.displayCategory(locale: String): String? {
    val fa = categoryFa?.takeIf { it.isNotBlank() }
    val en = category?.takeIf { it.isNotBlank() }
    return if (locale == "fa" && fa != null) fa else en
}

fun ProductDto.imageContentDescription(locale: String): String {
    val altFa = imageAltFa.takeIf { it.isNotBlank() }
    val altEn = imageAltEn.takeIf { it.isNotBlank() }
    return when {
        locale == "fa" && altFa != null -> altFa
        altEn != null -> altEn
        else -> displayName(locale)
    }
}

fun CategoryDto.displayName(locale: String): String =
    if (locale == "fa" && nameFa.isNotBlank()) nameFa else name
