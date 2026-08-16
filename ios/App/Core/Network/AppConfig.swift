import Foundation

/// IOS-002/003. Simulator: `localhost` works directly (the Simulator shares
/// the host Mac's network stack, unlike the Android emulator). Physical
/// device: replace with the Mac's current LAN IP (see ios/README.md —
/// verified real during Android on-device testing that this changes on DHCP
/// renewal, so it's not hardcoded to one value here).
enum AppConfig {
    static let host = "192.168.0.18" // Mac's current LAN IP — physical device testing
    static let oidcIssuer = "http://\(host):8081/realms/vida-cotidiana"
    static let oidcClientId = "ios-app"
    static let oidcRedirectUri = "com.vidacotidiana.app://callback"
    static let oidcScope = "openid profile email"
    static let apiBaseUrl = "http://\(host):8080/api/v1"
}
