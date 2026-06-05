package com.eslamielectric.android.core.data

import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class SessionStoreTest {

    private lateinit var store: SessionStore

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.sessionDataStore.edit { it.clear() }
        store = SessionStore(context)
        store.setToken(null)
    }

    @Test
    fun setToken_persistsAndUpdatesFlow() {
        store.setToken("jwt-1")
        assertEquals("jwt-1", store.getToken())
        assertTrue(store.isLoggedIn())
        store.setToken(null)
        assertNull(store.getToken())
        assertFalse(store.isLoggedIn())
    }

    @Test
    fun setLocale_persistsUserChoice() = runTest {
        store.setLocale("fa")
        assertEquals("fa", store.localeFlow.first())
        assertTrue(store.isLocaleUserSet())
    }

    @Test
    fun applyLocaleHint_setsFaForIranDefaultOnce() = runTest {
        store.applyLocaleHint("fa")
        assertEquals("fa", store.localeFlow.first())
        assertTrue(store.isLocaleInitialized())
    }

    @Test
    fun applyLocaleHint_setsEnForNonFaDefault() = runTest {
        store.applyLocaleHint("en")
        assertEquals("en", store.localeFlow.first())
        assertTrue(store.isLocaleInitialized())
    }

    @Test
    fun applyLocaleHint_doesNotOverrideUserLocale() = runTest {
        store.setLocale("en")
        store.applyLocaleHint("fa")
        assertEquals("en", store.localeFlow.first())
    }

    @Test
    fun localeFlow_defaultsToEn() = runTest {
        assertEquals("en", store.localeFlow.first())
        assertFalse(store.isLocaleUserSet())
        assertFalse(store.isLocaleInitialized())
    }

    @Test
    fun tokenFlow_emitsLoginStateChanges() = runTest {
        assertFalse(store.isLoggedInFlow.first())
        store.setToken("jwt-live")
        assertEquals("jwt-live", store.tokenFlow.value)
        assertTrue(store.isLoggedInFlow.first())
        store.setToken("  ")
        assertNull(store.getToken())
        assertFalse(store.isLoggedInFlow.first())
    }

    @Test
    fun setLocale_withoutUserFlagLeavesUserSetFalse() = runTest {
        store.setLocale("fa", userSet = false)
        assertEquals("fa", store.localeFlow.first())
        assertFalse(store.isLocaleUserSet())
    }
}
