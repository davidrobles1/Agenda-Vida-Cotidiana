import Foundation

struct Reminder: Codable, Identifiable {
    let id: String
    let title: String
    let description: String?
    let dueAt: String?
    let status: String
    let version: Int
    let ownerUserId: String
}

private struct RemindersPage: Decodable {
    let items: [Reminder]
}

private struct CreateReminderRequest: Encodable {
    let title: String
}

private struct CompleteReminderRequest: Encodable {
    let version: Int?
}

enum ReminderAPI {
    enum APIError: Error { case requestFailed(Int) }

    static func listReminders() async throws -> [Reminder] {
        let (data, response) = try await authorizedRequest(path: "/reminders", method: "GET")
        try assertOk(response)
        return try JSONDecoder().decode(RemindersPage.self, from: data).items
    }

    static func createReminder(title: String) async throws -> Reminder {
        let body = try JSONEncoder().encode(CreateReminderRequest(title: title))
        let (data, response) = try await authorizedRequest(path: "/reminders", method: "POST", body: body)
        try assertOk(response)
        return try JSONDecoder().decode(Reminder.self, from: data)
    }

    static func completeReminder(id: String, version: Int) async throws -> Reminder {
        let body = try JSONEncoder().encode(CompleteReminderRequest(version: version))
        let (data, response) = try await authorizedRequest(path: "/reminders/\(id)/complete", method: "POST", body: body)
        try assertOk(response)
        return try JSONDecoder().decode(Reminder.self, from: data)
    }

    private static func authorizedRequest(path: String, method: String, body: Data? = nil) async throws -> (Data, URLResponse) {
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

    private static func assertOk(_ response: URLResponse) throws {
        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
            let code = (response as? HTTPURLResponse)?.statusCode ?? -1
            throw APIError.requestFailed(code)
        }
    }
}
