import SwiftUI

struct ContentView: View {
    @EnvironmentObject var viewModel: ChatViewModel
    
    var body: some View {
        Group {
            if viewModel.hasIdentity && viewModel.isUnlocked {
                ChatListView()
            } else {
                SetupView()
            }
        }
        .preferredColorScheme(.dark)
        .alert(isPresented: .constant(viewModel.error != nil)) {
            Alert(
                title: Text("Error"),
                message: Text(viewModel.error ?? "Unknown error"),
                dismissButton: .default(Text("OK")) {
                    viewModel.error = nil
                }
            )
        }
    }
}
