package com.eslamielectric.android.core.analytics

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AnalyticsLoggerTest {

    @Test
    fun logScreen_doesNotCrashWhenFirebaseUnconfigured() {
        val logger = AnalyticsLogger(ApplicationProvider.getApplicationContext())
        logger.logScreen(AnalyticsEvents.SCREEN_HOME)
        logger.logEvent(AnalyticsEvents.CHECKOUT_STARTED)
    }

    @Test
    fun isEnabled_falseWithoutGoogleServicesInCi() {
        val logger = AnalyticsLogger(ApplicationProvider.getApplicationContext())
        assertFalse(logger.isEnabled())
    }
}
