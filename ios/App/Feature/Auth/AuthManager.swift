import AuthenticationServices
import Foundation
import UIKit

/// IOS-002: Authorization Code + PKCE against the `ios-app` Keycloak client
/// (Phase 1, public client) via `ASWebAuthenticationSession` — native, no
/// AppAuth-iOS dependency added (lighter weight, per the task's own
/// preference), never a plain WKWebView (Apple's own guidance: the system
/// browser session is what makes SSO/Keycloak's login cookie usable at all).
@MainActor
final class AuthManager: NSObject, ObservableObject {
    @Published private(set) var isLoggedIn: Bool

    private var session: ASWebAuthenticationSession?

    override init() {
        isLoggedIn = KeychainTokenStore.load() != nil
        super.init()
    }

    func login() async throws {
        let verifier = PKCE.generateCodeVerifier()
        let challenge = PKCE.codeChallenge(for: verifier)

        var components = URLComponents(string: "\(AppConfig.oidcIssuer)/protocol/openid-connect/auth")!
        components.queryItems = [
            URLQueryItem(name: "client_id", value: AppConfig.oidcClientId),
            URLQueryItem(name: "response_type", value: "code"),
            URLQueryItem(name: "redirect_uri", value: AppConfig.oidcRedirectUri),
            URLQueryItem(name: "scope", value: AppConfig.oidcScope),
            URLQueryItem(name: "code_challenge", value: challenge),
            URLQueryItem(name: "code_challenge_method", value: "S256"),
        ]

        let callbackURL = try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<URL, Error>) in
            let session = ASWebAuthenticationSession(
                url: components.url!,
                callbackURLScheme: "com.vidacotidiana.app"
            ) { url, error in
                if let url {
                    continuation.resume(returning: url)
                } else {
                    continuation.resume(throwing: error ?? URLError(.unknown))
                }
            }
            session.presentationContextProvider = self
            // Found for real: prefersEphemeralWebBrowserSession = false waits on
            // a native "<App> Wants to Use <domain> to Sign In" system consent
            // alert before navigating anywhere — a real one-time tap for a human,
            // but it never resolved at all in the Simulator/automated-test
            // context (SafariViewService sat on a blank page indefinitely, no
            // error, nothing to tap). true trades away Keycloak SSO cookie reuse
            // but is also arguably the better default anyway: no session
            // cookie shared with system Safari at all.
            session.prefersEphemeralWebBrowserSession = true
            self.session = session
            // start() returns false if the session never actually presented —
            // found for real: without checking this, a failed start left the
            // continuation waiting forever (no error, no UI change, just hung),
            // since the completion handler is never invoked in that case.
            if !session.start() {
                continuation.resume(throwing: AuthError.sessionFailedToStart)
            }
        }

        guard let code = URLComponents(url: callbackURL, resolvingAgainstBaseURL: false)?
            .queryItems?.first(where: { $0.name == "code" })?.value
        else {
            throw AuthError.noAuthorizationCode
        }

        let tokenResponse = try await exchangeCode(code, verifier: verifier)
        KeychainTokenStore.save(
            KeychainTokenStore.Session(
                accessToken: tokenResponse.accessToken,
                refreshToken: tokenResponse.refreshToken,
                idToken: tokenResponse.idToken,
                accessTokenExpiresAt: Date().addingTimeInterval(TimeInterval(tokenResponse.expiresIn))
            )
        )
        isLoggedIn = true
    }

    func logout() {
        KeychainTokenStore.clear()
        isLoggedIn = false
    }

    private struct TokenResponse: Decodable {
        let accessToken: String
        let refreshToken: String?
        let idToken: String?
        let expiresIn: Int

        enum CodingKeys: String, CodingKey {
            case accessToken = "access_token"
            case refreshToken = "refresh_token"
            case idToken = "id_token"
            case expiresIn = "expires_in"
        }
    }

    private func exchangeCode(_ code: String, verifier: String) async throws -> TokenResponse {
        var request = URLRequest(url: URL(string: "\(AppConfig.oidcIssuer)/protocol/openid-connect/token")!)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")

        var body = URLComponents()
        body.queryItems = [
            URLQueryItem(name: "grant_type", value: "authorization_code"),
            URLQueryItem(name: "code", value: code),
            URLQueryItem(name: "redirect_uri", value: AppConfig.oidcRedirectUri),
            URLQueryItem(name: "client_id", value: AppConfig.oidcClientId),
            URLQueryItem(name: "code_verifier", value: verifier),
        ]
        request.httpBody = body.percentEncodedQuery?.data(using: .utf8)

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw AuthError.tokenExchangeFailed
        }
        return try JSONDecoder().decode(TokenResponse.self, from: data)
    }

    enum AuthError: Error {
        case noAuthorizationCode
        case tokenExchangeFailed
        case sessionFailedToStart
    }
}

extension AuthManager: ASWebAuthenticationPresentationContextProviding {
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        let scene = UIApplication.shared.connectedScenes.first { $0.activationState == .foregroundActive }
        let windowScene = scene as? UIWindowScene
        return windowScene?.windows.first { $0.isKeyWindow } ?? ASPresentationAnchor()
    }
}
