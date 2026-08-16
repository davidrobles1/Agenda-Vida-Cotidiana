import SwiftUI

struct LoginView: View {
    @EnvironmentObject private var authManager: AuthManager
    @State private var error: String?

    var body: some View {
        VStack(spacing: 16) {
            Text("Vida Cotidiana")
            Button("Log in") {
                Task {
                    do {
                        try await authManager.login()
                    } catch {
                        self.error = "\(error)"
                    }
                }
            }
            if let error {
                Text(error)
            }
        }
    }
}
