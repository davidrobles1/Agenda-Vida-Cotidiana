package com.vidacotidiana.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * AND-006 real verification (non-fatal path): taps "Debug: record error",
 * which calls FirebaseCrashlytics.recordException() for real — only run
 * against a debug build assembled with -PcrashlyticsDebugEnabled=true (see
 * app/build.gradle.kts/VidaCotidianaApplication.kt); a plain debug build has
 * collection disabled and this event would be dropped by the SDK, not sent.
 *
 * This test only confirms the app-side call happens without throwing —
 * Crashlytics has no public real-time API to query events the way
 * GlitchTip does (01-technical-backlog.md, AND-006), so the actual arrival
 * in the Firebase console needs a human glance, documented separately.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CrashlyticsRecordExceptionTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        hiltRule.inject()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun recordExceptionButton_doesNotCrashTheApp() {
        composeRule.onNodeWithTag("login_button").performClick()

        device.wait(Until.hasObject(By.pkg("com.android.chrome").depth(0)), 15_000)
        device.waitForIdle()

        val editTexts = device.wait(Until.findObjects(By.clazz("android.widget.EditText")), 8_000)
        if (editTexts != null && editTexts.size >= 2) {
            editTexts[0].text = "testuser"
            editTexts[1].text = "TestPass123!"
            device.pressEnter()
        }

        device.waitForIdle()
        composeRule.waitUntil(20_000) {
            composeRule.onAllNodesWithTag("debug_record_exception_button").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("debug_record_exception_button").performClick()
        composeRule.waitForIdle()

        // Still alive and responsive after the call — proves recordException()
        // didn't throw or crash the process (it's meant to be non-fatal).
        composeRule.onNodeWithTag("debug_record_exception_button").performClick()
    }
}
