import Foundation

/// Shared bearer-token HTTP helper for API clients added after ReminderAPI
/// (UserAPI, SharingAPI) — ReminderAPI keeps its own copy rather than being
/// refactored onto this, since it's already covered by a real passing
/// XCUITest and isn't worth the risk of touching for a pure DRY gain.
enum APIClient {
    enum APIError: Error { case requestFailed(Int) }

    static func authorizedRequest(path: String, method: String, body: Data? = nil) async throws -> (Data, URLResponse) {
        var request = URLRequest(url: URL(string: "\(AppConfig.apiBaseUrl)\(path)")!)
        request.httpMethod = method
        if let token = KeychainTokenStore.load()?.accessToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if let body {
            request.httpBody = body
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        return try await URLSession.shared.data(for: request)
    }

    static func assertOk(_ response: URLResponse) throws {
        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
            let code = (response as? HTTPURLResponse)?.statusCode ?? -1
            throw APIError.requestFailed(code)
        }
    }
}
