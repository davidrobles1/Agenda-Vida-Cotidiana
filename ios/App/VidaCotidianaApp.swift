import SwiftUI

// IOS-001/002/003: entry point for the real Xcode App target (project.yml/xcodegen).
@main
struct VidaCotidianaApp: App {
    @StateObject private var authManager = AuthManager()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(authManager)
        }
    }
}
