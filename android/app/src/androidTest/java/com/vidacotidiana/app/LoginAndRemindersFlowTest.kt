package com.vidacotidiana.app

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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
 * AND-002/AND-003 real-device verification: taps through the actual AppAuth
 * login (Custom Tabs -> real Keycloak, Phase 1's `android-app` client) and
 * the actual reminders CRUD (real backend). UI Automator drives the Custom
 * Tabs login page — a separate process/app, which Espresso alone cannot
 * reach; Compose Testing drives the app's own screens.
 *
 * Run for real on a physical Samsung device (Android 16) over Wi-Fi against
 * the Mac's LAN IP. Three real bugs were found and fixed getting this test to
 * pass: (1) AppAuth's RedirectUriReceiverActivity requires a Theme.AppCompat
 * theme — the app's plain platform theme crashed it on the redirect back from
 * Keycloak (fixed: res/values/themes.xml). (2) AppAuth's DefaultConnectionBuilder
 * refuses non-HTTPS connections outright, which blocked the token exchange
 * against the plain-HTTP local dev Keycloak (fixed: debug-only
 * DebugConnectionBuilder/AppAuthConfigProvider, HTTPS-only preserved for
 * release). (3) The reminders list keeps every reminder ever created against
 * the real backend (no reset between runs), so an untagged "Complete" button
 * was ambiguous across runs (fixed: per-title testTag on each row).
 *
 * Requires the dev stack reachable from the device over Wi-Fi (Keycloak
 * :8081, backend :8080 — the Mac's current LAN IP, hardcoded into
 * BuildConfig.OIDC_ISSUER/API_BASE_URL in app/build.gradle.kts; update it if
 * the Mac's DHCP lease changes) and the `testuser`/`TestPass123!` account
 * created in Phase 1.
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
        device.waitForIdle()

        // Either Keycloak shows a fresh login form, or (if this device's Chrome
        // already has a live Keycloak SSO session from an earlier run) it
        // silently re-authenticates and redirects straight back — both are
        // valid outcomes, so only fill the form if one actually appears.
        val editTexts = device.wait(Until.findObjects(By.clazz("android.widget.EditText")), 8_000)
        if (editTexts != null && editTexts.size >= 2) {
            editTexts[0].text = "testuser"
            editTexts[1].text = "TestPass123!"
            device.pressEnter()
        }

        // Control returns to the app once Keycloak redirects back to com.vidacotidiana.app://callback
        // — AppAuth's RedirectUriReceiverActivity briefly owns the foreground before handing back to
        // MainActivity, during which the compose hierarchy the rule is attached to is momentarily gone
        // (not just empty) — fetchSemanticsNodes() throws IllegalStateException in that window, so treat
        // that as "not yet" and keep polling rather than letting it fail the wait immediately.
        device.waitForIdle()
        waitOrDumpTree(20_000, "01_after_login") {
            composeRule.onAllNodesWithTag("reminder_title_input").fetchSemanticsNodes().isNotEmpty()
        }

        // performTextInput goes through the real IME — on this device (Spanish
        // locale, Samsung predictive keyboard) that mangled the typed text into
        // "nuevareminder" via autocomplete. performTextReplacement sets the
        // value directly via the SetText semantics action, bypassing the
        // keyboard entirely.
        composeRule.onNodeWithTag("reminder_title_input").performTextReplacement(reminderTitle)
        composeRule.waitForIdle()
        // Pinpointing a real failure seen here: no new reminder ever appears
        // after this click, with nothing reaching the backend at all (confirmed
        // via server logs) — so confirm definitively whether the state actually
        // took hold before blaming the click/network path.
        composeRule.onNodeWithTag("reminder_title_input").assert(hasText(reminderTitle))
        composeRule.onNodeWithTag("add_reminder_button").performClick()

        waitOrDumpTree(20_000, "02_after_create") {
            composeRule.onAllNodesWithText(reminderTitle, substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNode(
            hasText("Complete") and hasAnyAncestor(hasTestTag("reminder_row_$reminderTitle")),
        ).performClick()

        waitOrDumpTree(20_000, "03_after_complete") {
            composeRule.onAllNodesWithText("$reminderTitle — COMPLETED", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Screenshots turned out unreliable here: UiDevice.takeScreenshot writes
     * in-process (this app's own storage permissions apply), so it has to
     * land in the app's own external-files-dir — which AGP wipes when it
     * uninstalls the app right after connectedAndroidTest finishes, before
     * there's any chance to `adb pull` it. Dumping the semantics tree to
     * Logcat via printToLog has no such file-lifecycle problem and needs no
     * storage permissions at all, so on any failure here it's the more
     * reliable way to see real device state: `adb logcat -d | grep VC_TEST`.
     */
    private fun waitOrDumpTree(timeoutMillis: Long, tag: String, condition: () -> Boolean) {
        try {
            composeRule.waitUntil(timeoutMillis) { runCatching(condition).getOrDefault(false) }
        } catch (e: Throwable) {
            runCatching { composeRule.onRoot(useUnmergedTree = true).printToLog("VC_TEST_$tag") }
            throw e
        }
    }
}
