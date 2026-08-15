// swift-tools-version: 5.10
// IOS-001: bootstrap only — no auth, no real API consumption (IOS-002+).
// ASSUMPTION: Swift Package Manager as dependency manager, per the doc's own
// TBD (08b-ios-architecture.md §TBD) — CocoaPods not ruled out if a specific
// future library requires it. This Package.swift lets `swift build`/editors
// resolve and type-check the source tree; a real .xcodeproj (App target,
// signing, Info.plist, entitlements) must still be created in Xcode when
// available — that step needs the full Xcode app, not just SPM.
import PackageDescription

let package = Package(
    name: "VidaCotidiana",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "VidaCotidiana", targets: ["VidaCotidiana"])
    ],
    targets: [
        .target(name: "VidaCotidiana", path: "App")
    ]
)
