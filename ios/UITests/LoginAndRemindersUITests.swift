import XCTest

/// IOS-002/IOS-003 real verification: taps the actual "Log in" button, drives
/// the Keycloak login page presented by ASWebAuthenticationSession — which
/// runs as a genuinely separate process (com.apple.SafariViewService, found
/// for real via `launchctl list` on the Simulator; analogous to Android's
/// Custom Tabs), so it needs its own XCUIApplication(bundleIdentifier:), not
/// the main app's — then drives the real reminders screen. Verified against
/// the real Keycloak (ios-app client, Phase 1) and real backend.
final class LoginAndRemindersUITests: XCTestCase {
    func testLoginThenCreateAndCompleteReminder() throws {
        let app = XCUIApplication()
        app.launch()

        let reminderTitle = "XCUITest reminder \(UUID().uuidString.prefix(8))"

        app.buttons["Log in"].tap()

        let safari = XCUIApplication(bundleIdentifier: "com.apple.SafariViewService")
        let usernameField = safari.textFields["Username or email"]
        XCTAssertTrue(usernameField.waitForExistence(timeout: 20), "Keycloak login form never appeared in SafariViewService")
        usernameField.tap()
        usernameField.typeText("testuser")

        let passwordField = safari.secureTextFields["Password"]
        XCTAssertTrue(passwordField.waitForExistence(timeout: 5))
        passwordField.tap()
        passwordField.typeText("TestPass123!")

        safari.buttons["Sign In"].tap()

        // Control returns to the app once Keycloak redirects back to com.vidacotidiana.app://callback.
        let titleField = app.textFields["New reminder"]
        XCTAssertTrue(titleField.waitForExistence(timeout: 20), "Reminders screen never appeared after login")
        titleField.tap()
        titleField.typeText(reminderTitle)
        app.buttons["Add"].tap()

        let reminderRow = app.staticTexts["\(reminderTitle) — PENDING"]
        XCTAssertTrue(reminderRow.waitForExistence(timeout: 10), "Created reminder never appeared in the list")

        app.buttons["Complete"].firstMatch.tap()

        let completedRow = app.staticTexts["\(reminderTitle) — COMPLETED"]
        XCTAssertTrue(completedRow.waitForExistence(timeout: 10), "Reminder never showed COMPLETED after tapping Complete")
    }
}
