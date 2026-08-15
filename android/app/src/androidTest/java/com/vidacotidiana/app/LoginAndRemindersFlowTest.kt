package com.vidacotidiana.app

import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
import java.io.File
import java.util.UUID

/**
 * AND-002/AND-003 real-device verification: taps through the actual AppAuth
 * login (Custom Tabs -> real Keycloak, Phase 1's `android-app` client) and
 * the actual reminders CRUD (real backend). UI Automator drives the Custom
 * Tabs login page — a separate process/app, which Espresso alone cannot
 * reach; Compose Testing drives the app's own screens.
 *
 * UNVERIFIED: never run against a real device in this environment (no
 * device/emulator was available — see docs/development/01-technical-backlog.md
 * AND-002/AND-003). The two `By.clazz(EditText)`-by-index selectors below for
 * the Keycloak login form are the standard technique for driving Custom Tabs
 * content with UI Automator (Chrome doesn't map HTML element ids to Android
 * accessibility resource-ids, so `By.res(...)` would not work here) but have
 * not been confirmed against Keycloak's actual rendered login page.
 *
 * Requires the dev stack from the earlier session reachable from the device
 * (Keycloak :8081, backend :8080 — emulator alias 10.0.2.2 or the Mac's LAN
 * IP for a physical device, see README) and the `testuser`/`TestPass123!`
 * account created in Phase 1.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoginAndRemindersFlowTest {

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
    fun loginThenCreateAndCompleteReminder() {
        val reminderTitle = "Instrumented test reminder ${UUID.randomUUID().toString().take(8)}"

        composeRule.onNodeWithTag("login_button").performClick()

        // Custom Tabs opens in a separate process — wait for it, then drive it
        // with UI Automator instead of Espresso/Compose Testing.
        device.wait(Until.hasObject(By.pkg("com.android.chrome").depth(0)), 15_000)
        val editTexts = device.wait(
            Until.findObjects(By.clazz("android.widget.EditText")),
            15_000,
        ) ?: error("Keycloak login form (EditText fields) never appeared in the Custom Tab")
        check(editTexts.size >= 2) { "Expected username+password fields, found ${editTexts.size}" }
        editTexts[0].text = "testuser"
        editTexts[1].text = "TestPass123!"
        device.pressEnter()

        // Control returns to the app once Keycloak redirects back to com.vidacotidiana.app://callback.
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithTag("reminder_title_input").fetchSemanticsNodes().isNotEmpty()
        }
        takeScreenshot("01_logged_in_reminders_screen")

        composeRule.onNodeWithTag("reminder_title_input").performTextInput(reminderTitle)
        composeRule.onNodeWithTag("add_reminder_button").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(reminderTitle, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        takeScreenshot("02_reminder_created")

        composeRule.onNode(
            hasText("Complete") and hasAnySibling(hasText(reminderTitle, substring = true)),
        ).performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("$reminderTitle — COMPLETED", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        takeScreenshot("03_reminder_completed")
    }

    private fun takeScreenshot(name: String) {
        val dir = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "screenshots",
        )
        dir.mkdirs()
        device.takeScreenshot(File(dir, "$name.png"))
    }
}
