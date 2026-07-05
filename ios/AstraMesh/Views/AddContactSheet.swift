import SwiftUI

struct AddContactSheet: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var viewModel: ChatViewModel
    
    @State private var contactString: String = ""
    @State private var errorText: String? = nil
    
    var body: some View {
        NavigationStack {
            ZStack {
                Color(hue: 0.61, saturation: 0.25, brightness: 0.08)
                    .ignoresSafeArea()
                
                VStack(spacing: 24) {
                    Text("Add Contact")
                        .font(.title2)
                        .bold()
                        .padding(.top)
                    
                    Text("Paste a contact key (starts with astra:...) to add a peer. Tor-enabled contacts may include a .onion address.")
                        .multilineTextAlignment(.center)
                        .foregroundColor(.gray)
                    
                    VStack(alignment: .leading, spacing: 8) {
                        TextField("astra:...", text: $contactString)
                            .padding()
                            .background(Color(white: 0.15))
                            .cornerRadius(10)
                            .foregroundColor(.white)
                            .textInputAutocapitalization(.never)
                            .disableAutocorrection(true)
                        
                        if let error = errorText {
                            Text(error)
                                .foregroundColor(.red)
                                .font(.caption)
                                .padding(.leading, 4)
                        }
                    }
                    .padding(.horizontal)
                    
                    Button(action: addContact) {
                        Text("Add Contact")
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(
                                contactString.isEmpty ? Color.gray : Color(hue: 0.6, saturation: 0.8, brightness: 0.8)
                            )
                            .cornerRadius(12)
                    }
                    .disabled(contactString.isEmpty)
                    .padding(.horizontal)
                    
                    Spacer()
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
    }
    
    private func addContact() {
        viewModel.addContact(from: contactString.trimmingCharacters(in: .whitespacesAndNewlines))
        if viewModel.error == nil {
            dismiss()
        } else {
            errorText = viewModel.error ?? "Invalid contact string."
        }
    }
}
