import SwiftUI

// IOS-001: bootstrap-only entry point. Real navigation/auth/API consumption
// starts at IOS-002+ (see Navigation/ and Feature/Auth).
// NOTE: this is an SPM library target (see ../Package.swift), so it cannot
// declare @main itself. Once dragged into a real Xcode App target, mark this
// `@main struct VidaCotidianaApp: App`.
public struct VidaCotidianaApp: App {
    public init() {}

    public var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
