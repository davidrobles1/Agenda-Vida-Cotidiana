import SwiftUI

@MainActor
final class ShareViewModel: ObservableObject {
    let reminderId: String
    @Published var shares: [ReminderShare] = []
    @Published var invitations: [Invitation] = []
    @Published var recipient: String = ""
    @Published var error: String?

    init(reminderId: String) {
        self.reminderId = reminderId
    }

    func refresh() {
        Task {
            do {
                let result = try await SharingAPI.listSharesAndInvitations(reminderId: reminderId)
                shares = result.shares
                invitations = result.invitations
                error = nil
            } catch {
                self.error = "\(error)"
            }
        }
    }

    func invite() {
        let value = recipient.trimmingCharacters(in: .whitespaces)
        guard !value.isEmpty else { return }
        Task {
            do {
                if value.contains("@") {
                    _ = try await SharingAPI.createInvitation(reminderId: reminderId, email: value, username: nil)
                } else {
                    _ = try await SharingAPI.createInvitation(reminderId: reminderId, email: nil, username: value)
                }
                recipient = ""
                refresh()
            } catch {
                self.error = "\(error)"
            }
        }
    }

    func revoke(_ share: ReminderShare) {
        Task {
            do {
                try await SharingAPI.revokeShare(reminderId: reminderId, shareId: share.id)
                refresh()
            } catch {
                self.error = "\(error)"
            }
        }
    }

    func cancel(_ invitation: Invitation) {
        Task {
            do {
                try await SharingAPI.cancelInvitation(invitationId: invitation.id)
                refresh()
            } catch {
                self.error = "\(error)"
            }
        }
    }
}

/// IOS-004: inline share panel shown from a reminder's own row (owner only).
struct ShareView: View {
    @StateObject private var viewModel: ShareViewModel

    init(reminderId: String) {
        _viewModel = StateObject(wrappedValue: ShareViewModel(reminderId: reminderId))
    }

    var body: some View {
        VStack(alignment: .leading) {
            HStack {
                TextField("Email or username", text: $viewModel.recipient)
                    .accessibilityIdentifier("invite_recipient_input")
                Button("Invite") { viewModel.invite() }
                    .accessibilityIdentifier("invite_button")
            }

            if let error = viewModel.error {
                Text(error)
            }

            ForEach(viewModel.shares) { share in
                HStack {
                    Text("\(share.collaboratorUserId) — \(share.status)")
                    Spacer()
                    if share.status == "ACTIVE" {
                        Button("Revoke") { viewModel.revoke(share) }
                    }
                }
            }

            ForEach(viewModel.invitations.filter { $0.status == "PENDING" }) { invitation in
                HStack {
                    Text("\(invitation.invitedEmail ?? "") — \(invitation.status)")
                    Spacer()
                    Button("Cancel") { viewModel.cancel(invitation) }
                }
            }
        }
        .padding(.vertical, 4)
        .accessibilityIdentifier("share_dialog_\(viewModel.reminderId)")
        .onAppear { viewModel.refresh() }
    }
}
