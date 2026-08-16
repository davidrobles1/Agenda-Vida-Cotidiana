# Vida Cotidiana — iOS (IOS-001, bootstrap)

Bootstrap scaffold only — no auth, no real API consumption. That starts at IOS-002.

Stack (08b-ios-architecture.md): SwiftUI + Swift native (DEC-006/ADR-010), `iOS 17`
minimum (DEC-012). ASSUMPTION: Swift Package Manager as dependency manager, per the
doc's own TBD (CocoaPods not ruled out for a future library that requires it).

## Build status (real, 2026-08-15)

Both real build paths were attempted in the environment this scaffold was created in:

- `xcodebuild -version` → `xcode-select: error: tool 'xcodebuild' requires Xcode, but
  active developer directory '/Library/Developer/CommandLineTools' is a command line
  tools instance` — only the Command Line Tools are installed, not the Xcode.app
  needed to build/run an iOS target.
- `swift build` → falls back to the macOS SDK (the only one available) and fails
  type-checking `WindowGroup`/`Scene`/`App`, which this package declares only for
  `.iOS(.v17)` (SwiftUI's iOS 17 API surface isn't fully available under an old
  default macOS deployment target).

This is **BLOCKED_BY_ENVIRONMENT** (macOS host present, but Xcode.app / iOS SDK are
not), not a project defect. To build on a machine with Xcode installed: open
`Package.swift` in Xcode, or create a new Xcode "App" project and drag `App/` in as
its source group (a real `.xcodeproj` — with signing, Info.plist, entitlements —
cannot be reliably hand-authored outside Xcode; this scaffold intentionally ships
source files + `Package.swift` instead of a hand-written `.xcodeproj`).

## Xcode install attempt (real, 2026-08-15)

`brew install xcodesorg/made/xcodes` was retried after `/opt/homebrew` ownership was
fixed, but failed building from source: `error: xcbuild executable at
'/Library/Developer/SharedFrameworks/XCBuild.framework/.../xcbuild' does not exist or
is not executable` — a circular dependency (building the tool that installs Xcode
requires Xcode). Worked around by downloading the prebuilt universal binary directly
from the `xcodes` GitHub release (`xcodes-2.0.3`, arm64+x86_64) to `~/bin/xcodes`,
bypassing Homebrew's build entirely — confirmed real: `xcodes version` → `2.0.3`,
`xcodes list` lists real available Xcode versions from Apple.

## Xcode installed, not yet selected (real, 2026-08-16)

`xcodes install --latest` needed an interactive Apple ID login (username + password,
almost certainly 2FA) — the user ran it themselves, in their own Terminal, and it
succeeded. Confirmed for real here:

```
$ xcodes installed
26.6 (17F113) [Apple Silicon]	/Applications/Xcode-26.6.0.app
```

But `xcode-select -p` still points at `/Library/Developer/CommandLineTools`, and
switching it (`sudo xcode-select -s /Applications/Xcode-26.6.0.app`) needs the same
interactive `sudo` password this environment can't provide — confirmed by trying both
the direct command and `xcodes select` (which also shells out to `sudo` internally and
hit the identical prompt). Three commands are still needed, all requiring the account
owner's password, all safe to run together:

```
sudo xcode-select -s /Applications/Xcode-26.6.0.app
sudo xcodebuild -license accept
sudo xcodebuild -runFirstLaunch
```

Once those run, verify with `xcodebuild -version` and `xcodebuild -showsdks` (should
list an iOS SDK), then IOS-002/IOS-003 (AppAuth or `ASWebAuthenticationSession` login +
reminders CRUD) can proceed the same way AND-002/AND-003 did — including trimming
simulator runtimes to iOS-only (`xcodes runtimes --uninstall watchOS,tvOS,visionOS`, if
those installed by default) and building for the simulator to verify without needing to
touch code-signing at all.

## Structure

```
App/
  VidaCotidianaApp.swift   # entry point (mark @main once inside a real Xcode App target)
  ContentView.swift        # minimal SwiftUI view, renders "Vida Cotidiana"
  Core/{Network,Security,UI}
  Feature/{Auth,Home,Reminders,Sharing,Settings}
  Navigation/
```
