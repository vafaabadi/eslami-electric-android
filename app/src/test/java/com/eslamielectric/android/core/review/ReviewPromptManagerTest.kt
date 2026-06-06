package com.eslamielectric.android.core.review

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.play.core.review.ReviewInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class ReviewPromptManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val launcher = mockk<PlayReviewLauncher>(relaxed = true)
    private val reviewInfo = mockk<ReviewInfo>()
    private val activity = mockk<Activity>(relaxed = true)
    private var now = 1_000_000L

    @Before
    fun setUp() {
        context.getSharedPreferences("eslami_review_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        coEvery { launcher.requestReviewFlow() } returns reviewInfo
        coEvery { launcher.launchReviewFlow(any(), any()) } returns true
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun shouldPrompt_falseBeforeSecondOrder() {
        val manager = ReviewPromptManager(context, launcher = launcher, clock = { now })
        assertFalse(manager.shouldPrompt(1))
    }

    @Test
    fun shouldPrompt_trueOnSecondOrderWhenNeverPrompted() {
        val manager = ReviewPromptManager(context, launcher = launcher, clock = { now })
        assertTrue(manager.shouldPrompt(2))
    }

    @Test
    fun shouldPrompt_falseWithinCooldownAfterPrompt() {
        val prefs = context.getSharedPreferences("eslami_review_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("successful_orders", 3)
            .putLong("last_prompt_at", now - ReviewPromptManager.COOLDOWN_MS + 1)
            .apply()
        val manager = ReviewPromptManager(context, launcher = launcher, clock = { now }, prefs = prefs)
        assertFalse(manager.shouldPrompt())
    }

    @Test
    fun shouldPrompt_trueAfterCooldownExpires() {
        val prefs = context.getSharedPreferences("eslami_review_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("successful_orders", 3)
            .putLong("last_prompt_at", now - ReviewPromptManager.COOLDOWN_MS)
            .apply()
        val manager = ReviewPromptManager(context, launcher = launcher, clock = { now }, prefs = prefs)
        assertTrue(manager.shouldPrompt())
    }

    @Test
    fun onCheckoutSuccess_skipsReviewOnFirstOrder() = runTest {
        val manager = ReviewPromptManager(context, launcher = launcher, clock = { now })
        manager.onCheckoutSuccess(activity)
        coVerify(exactly = 0) { launcher.requestReviewFlow() }
    }

    @Test
    fun onCheckoutSuccess_launchesReviewOnSecondOrder() = runTest {
        val manager = ReviewPromptManager(context, launcher = launcher, clock = { now })
        manager.onCheckoutSuccess(activity)
        manager.onCheckoutSuccess(activity)
        coVerify(exactly = 1) { launcher.requestReviewFlow() }
        coVerify(exactly = 1) { launcher.launchReviewFlow(activity, reviewInfo) }
    }

    @Test
    fun onCheckoutSuccess_doesNotRelaunchWithinCooldown() = runTest {
        val manager = ReviewPromptManager(context, launcher = launcher, clock = { now })
        manager.onCheckoutSuccess(activity)
        manager.onCheckoutSuccess(activity)
        manager.onCheckoutSuccess(activity)
        coVerify(exactly = 1) { launcher.requestReviewFlow() }
    }
}
