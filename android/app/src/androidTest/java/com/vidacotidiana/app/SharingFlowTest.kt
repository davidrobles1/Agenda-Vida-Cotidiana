package com.vidacotidiana.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
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
import java.util.UUID

/**
 * AND-004 real-device verification: owner (testuser, the device's logged-in
 * user) creates a reminder and invites "userb" by username through the
 * actual backend. The B side (userb accepting) is verified directly against
 * the real backend/API per the task's own allowance ("Postman para el lado
 * B") rather than duplicating a second full device login in this test —
 * this test's job is to prove the A-side (owner) UI is real, not to
 * re-verify AND-002/003's login flow a second time.
 *
 * Requires: testuser logged in (same dev stack as LoginAndRemindersFlowTest),
 * and "userb" already provisioned in the backend's local USER table (done via
 * one real password-grant login + GET /me against the running backend before
 * this test runs — see CIERRE notes).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SharingFlowTest {

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
    fun ownerInvitesCollaboratorByUsername() {
        val reminderTitle = "Sharing test reminder ${UUID.randomUUID().toString().take(8)}"

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
            composeRule.onAllNodesWithTag("reminder_title_input").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("reminder_title_input").performTextReplacement(reminderTitle)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_reminder_button").performClick()

        waitOrDumpTree(20_000, "02_after_create") {
            composeRule.onAllNodesWithText(reminderTitle, substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        // Share button only renders once currentUserId (fetched via GET /me) resolves and
        // matches the reminder's ownerUserId — wait for it rather than assuming it's already there.
        waitOrDumpTree(15_000, "03_share_button_visible") {
            composeRule.onAllNodesWithTag("share_button_$reminderTitle").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("share_button_$reminderTitle").performClick()

        waitOrDumpTree(10_000, "04_dialog_open") {
            composeRule.onAllNodesWithTag("invite_recipient_input").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("invite_recipient_input").performTextReplacement("userb")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("invite_button").performClick()

        waitOrDumpTree(15_000, "05_after_invite") {
            composeRule.onAllNodesWithText("userb@example.com", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
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
