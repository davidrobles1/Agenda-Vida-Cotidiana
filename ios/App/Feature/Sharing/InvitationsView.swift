import SwiftUI

@MainActor
final class InvitationsViewModel: ObservableObject {
    @Published var invitations: [Invitation] = []
    @Published var error: String?
    @Published var loading = true

    func refresh() {
        Task {
            loading = true
            do {
                invitations = try await SharingAPI.listMyInvitations()
                error = nil
            } catch {
                self.error = "\(error)"
            }
            loading = false
        }
    }

    func accept(_ invitation: Invitation) {
        Task {
            do {
                try await SharingAPI.acceptInvitation(invitationId: invitation.id)
                refresh()
            } catch {
                self.error = "\(error)"
            }
        }
    }

    func reject(_ invitation: Invitation) {
        Task {
            do {
                try await SharingAPI.rejectInvitation(invitationId: invitation.id)
                refresh()
            } catch {
                self.error = "\(error)"
            }
        }
    }
}

/// IOS-004: GET /me/invitations with accept/reject.
struct InvitationsView: View {
    @StateObject private var viewModel = InvitationsViewModel()

    var body: some View {
        VStack {
            if let error = viewModel.error {
                Text(error)
            }

            if viewModel.loading {
                ProgressView()
            } else if viewModel.invitations.isEmpty {
                Text("No pending invitations")
            } else {
                List(viewModel.invitations) { invitation in
                    HStack {
                        Text(invitation.invitedEmail ?? "")
                        Spacer()
                        Button("Accept") { viewModel.accept(invitation) }
                        Button("Reject") { viewModel.reject(invitation) }
                    }
                }
            }
        }
        .navigationTitle("Invitations")
        .onAppear { viewModel.refresh() }
    }
}
