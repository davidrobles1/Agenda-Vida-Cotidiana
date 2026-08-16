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
 * AND-006 real verification (fatal path): taps "Debug: crash", which throws
 * for real and kills the app process — only run against a debug build
 * assembled with -PcrashlyticsDebugEnabled=true. This test's own process
 * dies along with the app's (same process, connectedAndroidTest), which the
 * test harness records as a real, expected failure — the actual proof this
 * test exists to produce is on-device evidence (checked separately via
 * `adb shell run-as` on the .crashlytics report directory, documented in
 * 01-technical-backlog.md/AND-006), not this test's own pass/fail status.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CrashlyticsCrashTest {

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
    fun debugCrashButton_crashesForReal() {
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
            composeRule.onAllNodesWithTag("debug_crash_button").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("debug_crash_button").performClick()
    }
}
