import SwiftUI

struct RootView: View {
    @EnvironmentObject private var authManager: AuthManager

    var body: some View {
        if authManager.isLoggedIn {
            RemindersView()
        } else {
            LoginView()
        }
    }
}
