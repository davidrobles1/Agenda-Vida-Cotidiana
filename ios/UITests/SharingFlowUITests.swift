import XCTest

/// IOS-004 real verification: owner (testuser) creates a reminder and
/// invites "userb" by username through the actual backend. Run in
/// Simulator (allowed per task: "Simulador está bien para esto"). The B
/// side (userb accepting) is verified directly against the real backend
/// per the task's own allowance ("Postman para el lado B") rather than
/// duplicating a second full device login here.
///
/// Requires: "userb" already provisioned in the backend's local USER table
/// (one real password-grant login + GET /me against the running backend
/// before this test runs — see CIERRE notes).
final class SharingFlowUITests: XCTestCase {
    func testOwnerInvitesCollaboratorByUsername() throws {
        let app = XCUIApplication()
        app.launch()

        let reminderTitle = "iOS sharing test \(UUID().uuidString.prefix(8))"

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

        let titleField = app.textFields["New reminder"]
        XCTAssertTrue(titleField.waitForExistence(timeout: 20), "Reminders screen never appeared after login")
        titleField.tap()
        titleField.typeText(reminderTitle)
        app.buttons["Add"].tap()

        // Share button only renders once GET /me resolves and matches ownerUserId — wait for it.
        let shareButton = app.buttons["share_button_\(reminderTitle)"]
        XCTAssertTrue(shareButton.waitForExistence(timeout: 15), "Share button never appeared for the owned reminder")
        shareButton.tap()

        let recipientField = app.textFields["invite_recipient_input"]
        XCTAssertTrue(recipientField.waitForExistence(timeout: 10), "Share panel never opened")
        recipientField.tap()
        recipientField.typeText("userb")
        app.buttons["invite_button"].tap()

        let invitedRow = app.staticTexts["userb@example.com — PENDING"]
        XCTAssertTrue(invitedRow.waitForExistence(timeout: 15), "Invitation to userb never appeared in the share panel")
    }
}
