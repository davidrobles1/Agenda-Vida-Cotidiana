import SwiftUI

@MainActor
final class RemindersViewModel: ObservableObject {
    @Published var reminders: [Reminder] = []
    @Published var error: String?
    @Published var newTitle: String = ""
    @Published var currentUserId: String?

    func refresh() {
        Task {
            do {
                reminders = try await ReminderAPI.listReminders()
            } catch {
                self.error = "\(error)"
            }
        }
        // Share button visibility only — a failure here just means it won't show.
        Task {
            currentUserId = try? await UserAPI.getCurrentUser().id
        }
    }

    func createReminder() {
        let title = newTitle
        guard !title.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        newTitle = ""
        Task {
            do {
                _ = try await ReminderAPI.createReminder(title: title)
                refresh()
            } catch {
                self.error = "\(error)"
            }
        }
    }

    func complete(_ reminder: Reminder) {
        Task {
            do {
                _ = try await ReminderAPI.completeReminder(id: reminder.id, version: reminder.version)
                refresh()
            } catch {
                self.error = "\(error)"
            }
        }
    }
}

struct RemindersView: View {
    @EnvironmentObject private var authManager: AuthManager
    @StateObject private var viewModel = RemindersViewModel()
    @State private var sharingReminderId: String?

    var body: some View {
        NavigationStack {
            VStack {
                HStack {
                    Text("Vida Cotidiana")
                    Spacer()
                    NavigationLink("Invitations") { InvitationsView() }
                    NavigationLink("Notifications") { NotificationsView() }
                    Button("Log out") { authManager.logout() }
                }

                HStack {
                    TextField("New reminder", text: $viewModel.newTitle)
                    Button("Add") { viewModel.createReminder() }
                }

                if let error = viewModel.error {
                    Text(error)
                }

                List(viewModel.reminders) { reminder in
                    VStack(alignment: .leading) {
                        HStack {
                            Text("\(reminder.title) — \(reminder.status)")
                            Spacer()
                            if reminder.status == "PENDING" {
                                Button("Complete") { viewModel.complete(reminder) }
                            }
                            if reminder.ownerUserId == viewModel.currentUserId {
                                Button("Share") {
                                    sharingReminderId = sharingReminderId == reminder.id ? nil : reminder.id
                                }
                                .accessibilityIdentifier("share_button_\(reminder.title)")
                            }
                        }
                        if sharingReminderId == reminder.id {
                            ShareView(reminderId: reminder.id)
                        }
                    }
                    .accessibilityIdentifier("reminder_row_\(reminder.title)")
                }
            }
            .padding()
            .onAppear { viewModel.refresh() }
        }
    }
}
