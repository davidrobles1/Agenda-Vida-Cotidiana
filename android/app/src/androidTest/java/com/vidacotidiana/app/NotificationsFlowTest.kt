package com.vidacotidiana.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
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
 * AND-005 real-device verification: a real google-services.json (Firebase
 * project vida-cotidiana-6da30) is present, so this obtains a real FCM
 * token from Firebase and registers it against the real backend
 * (POST /me/devices) — not a mocked SDK, not a hardcoded token. The device
 * row appearing in the UI proves the round trip through Firebase and the
 * backend both worked; independently re-confirmed via GET /me/devices in
 * the CIERRE notes. Actual push delivery end-to-end is separate (needs a
 * Firebase service account wired into the backend's FcmPushNotificationSender)
 * and out of scope here.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NotificationsFlowTest {

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
    fun enableNotificationsRegistersRealFcmToken() {
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
        waitOrDumpTree(20_000, "01_after_login") {
            composeRule.onAllNodesWithTag("notifications_button").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("notifications_button").performClick()

        waitOrDumpTree(10_000, "02_notifications_screen") {
            composeRule.onAllNodesWithTag("enable_notifications_button").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("enable_notifications_button").performClick()

        // Real network round trip: Firebase (token) then the backend (registration) — allow time for both.
        waitOrDumpTree(20_000, "03_device_registered") {
            composeRule.onAllNodesWithText("ANDROID — registered", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        // A stale/unrelated device row satisfying the wait above isn't enough on its own
        // (found for real: a leftover row from manual API testing made this pass once
        // while the real registration was still failing with 400) — also assert no error
        // surfaced, which only clears on a genuinely successful registerDevice() call.
        composeRule.onAllNodesWithText("HTTP", substring = true).assertCountEquals(0)
    }

    private fun waitOrDumpTree(timeoutMillis: Long, tag: String, condition: () -> Boolean) {
        try {
            composeRule.waitUntil(timeoutMillis) { runCatching(condition).getOrDefault(false) }
        } catch (e: Throwable) {
            runCatching { composeRule.onRoot(useUnmergedTree = true).printToLog("VC_TEST_$tag") }
            throw e
        }
    }
}
