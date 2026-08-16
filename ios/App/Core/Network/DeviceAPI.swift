import Foundation

struct DevicePushToken: Codable, Identifiable {
    let id: String
    let platform: String
    let createdAt: String
    let lastSeenAt: String
}

private struct RegisterDeviceRequest: Encodable {
    let platform: String
    let token: String
}

/// IOS-005 contract — POST/GET /me/devices, matches Documentacion/openapi/openapi.yaml.
enum DeviceAPI {
    static func listDevices() async throws -> [DevicePushToken] {
        let (data, response) = try await APIClient.authorizedRequest(path: "/me/devices", method: "GET")
        try APIClient.assertOk(response)
        return try JSONDecoder().decode([DevicePushToken].self, from: data)
    }

    static func registerDevice(token: String) async throws -> DevicePushToken {
        let body = try JSONEncoder().encode(RegisterDeviceRequest(platform: "IOS", token: token))
        let (data, response) = try await APIClient.authorizedRequest(path: "/me/devices", method: "POST", body: body)
        try APIClient.assertOk(response)
        return try JSONDecoder().decode(DevicePushToken.self, from: data)
    }
}
