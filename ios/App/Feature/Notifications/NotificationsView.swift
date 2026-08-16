import SwiftUI

@MainActor
final class NotificationsViewModel: ObservableObject {
    @Published var devices: [DevicePushToken] = []
    @Published var error: String?
    @Published var loading = true

    /// IOS-005 gated on Firebase config (CIERRE): no GoogleService-Info.plist
    /// exists in this checkout, so FirebaseMessaging isn't added via SPM and
    /// no real FCM token can be obtained. registerDevice() below is real and
    /// callable the moment a real token is available.
    let blockedOnFirebaseConfig = true

    func refresh() {
        Task {
            loading = true
            do {
                devices = try await DeviceAPI.listDevices()
                error = nil
            } catch {
                self.error = "\(error)"
            }
            loading = false
        }
    }

    func registerDevice(token: String) {
        Task {
            do {
                _ = try await DeviceAPI.registerDevice(token: token)
                refresh()
            } catch {
                self.error = "\(error)"
            }
        }
    }
}

/// IOS-005: "Notifications" screen with an enable button and POST /me/devices call.
struct NotificationsView: View {
    @StateObject private var viewModel = NotificationsViewModel()

    var body: some View {
        VStack(alignment: .leading) {
            if let error = viewModel.error {
                Text(error)
            }

            if viewModel.blockedOnFirebaseConfig {
                Text("Push notifications require Firebase configuration, which isn't set up in this build yet.")
                Button("Enable notifications") {}.disabled(true)
            } else {
                Button("Enable notifications") { /* real token wired once available */ }
            }

            if viewModel.loading {
                ProgressView()
            } else {
                ForEach(viewModel.devices) { device in
                    Text("\(device.platform) — registered \(device.createdAt)")
                }
            }
        }
        .padding()
        .navigationTitle("Notifications")
        .onAppear { viewModel.refresh() }
    }
}
