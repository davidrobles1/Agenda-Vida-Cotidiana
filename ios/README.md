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

`xcodes install --latest` itself is now **blocked on an interactive Apple ID login**
(username + password, almost certainly followed by a 2FA prompt) that only a real
terminal session with the account owner typing can provide — not something that can be
relayed through an automated tool call, and not something this assistant should ever
be asked to hold credentials for. To continue:

```
export PATH="$HOME/bin:$PATH"
xcodes install --latest
```

Run that yourself, in your own Terminal, and complete the Apple ID prompts (this is the
same "only login necessary, no App Store window" step from the original plan). Once
Xcode is installed, license-accepted, and `xcodebuild -showsdks` lists an iOS SDK,
IOS-002/IOS-003 (AppAuth or `ASWebAuthenticationSession` login + reminders CRUD) can
proceed the same way AND-002/AND-003 did.

## Structure

```
App/
  VidaCotidianaApp.swift   # entry point (mark @main once inside a real Xcode App target)
  ContentView.swift        # minimal SwiftUI view, renders "Vida Cotidiana"
  Core/{Network,Security,UI}
  Feature/{Auth,Home,Reminders,Sharing,Settings}
  Navigation/
```
