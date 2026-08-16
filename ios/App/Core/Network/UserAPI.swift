import Foundation

struct CurrentUser: Codable {
    let id: String
    let email: String
    let username: String
}

enum UserAPI {
    static func getCurrentUser() async throws -> CurrentUser {
        let (data, response) = try await APIClient.authorizedRequest(path: "/me", method: "GET")
        try APIClient.assertOk(response)
        return try JSONDecoder().decode(CurrentUser.self, from: data)
    }
}
