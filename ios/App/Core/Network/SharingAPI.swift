import Foundation

struct ReminderShare: Codable, Identifiable {
    let id: String
    let reminderId: String
    let collaboratorUserId: String
    let status: String
    let createdAt: String
    let revokedAt: String?
}

struct Invitation: Codable, Identifiable {
    let id: String
    let reminderId: String
    let invitedEmail: String?
    let status: String
    let expiresAt: String
    let createdAt: String
}

struct SharesAndInvitationsResponse: Codable {
    let shares: [ReminderShare]
    let invitations: [Invitation]
}

private struct InvitationsPage: Decodable {
    let items: [Invitation]
}

private struct CreateInvitationRequest: Encodable {
    let email: String?
    let username: String?
}

/// AND-004/IOS-004/WEB-004 contract — mirrors android/.../SharingApi.kt and
/// web/src/features/sharing/api.ts exactly; no new endpoints invented.
enum SharingAPI {
    static func listSharesAndInvitations(reminderId: String) async throws -> SharesAndInvitationsResponse {
        let (data, response) = try await APIClient.authorizedRequest(path: "/reminders/\(reminderId)/shares", method: "GET")
        try APIClient.assertOk(response)
        return try JSONDecoder().decode(SharesAndInvitationsResponse.self, from: data)
    }

    static func createInvitation(reminderId: String, email: String?, username: String?) async throws -> Invitation {
        let body = try JSONEncoder().encode(CreateInvitationRequest(email: email, username: username))
        let (data, response) = try await APIClient.authorizedRequest(path: "/reminders/\(reminderId)/shares", method: "POST", body: body)
        try APIClient.assertOk(response)
        return try JSONDecoder().decode(Invitation.self, from: data)
    }

    static func revokeShare(reminderId: String, shareId: String) async throws {
        let (_, response) = try await APIClient.authorizedRequest(path: "/reminders/\(reminderId)/shares/\(shareId)", method: "DELETE")
        try APIClient.assertOk(response)
    }

    static func cancelInvitation(invitationId: String) async throws {
        let (_, response) = try await APIClient.authorizedRequest(path: "/invitations/\(invitationId)", method: "DELETE")
        try APIClient.assertOk(response)
    }

    static func listMyInvitations() async throws -> [Invitation] {
        let (data, response) = try await APIClient.authorizedRequest(path: "/me/invitations", method: "GET")
        try APIClient.assertOk(response)
        return try JSONDecoder().decode(InvitationsPage.self, from: data).items
    }

    static func acceptInvitation(invitationId: String) async throws {
        let (_, response) = try await APIClient.authorizedRequest(path: "/invitations/\(invitationId)/accept", method: "POST")
        try APIClient.assertOk(response)
    }

    static func rejectInvitation(invitationId: String) async throws {
        let (_, response) = try await APIClient.authorizedRequest(path: "/invitations/\(invitationId)/reject", method: "POST")
        try APIClient.assertOk(response)
    }
}
