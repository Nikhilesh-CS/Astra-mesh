import SwiftUI

struct ChatView: View {
    @EnvironmentObject var viewModel: ChatViewModel
    let contact: Contact
    
    @State private var messageText: String = ""
    
    var body: some View {
        ZStack {
            Color(hue: 0.61, saturation: 0.25, brightness: 0.08)
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Messages List
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(viewModel.messages.filter { $0.contactKey == contact.signingPublicKey }) { message in
                                MessageBubble(message: message)
                                    .id(message.id)
                            }
                        }
                        .padding()
                    }
                    .onChange(of: viewModel.messages) { _ in
                        scrollToBottom(proxy: proxy)
                    }
                    .onAppear {
                        viewModel.selectContact(contact)
                        scrollToBottom(proxy: proxy)
                    }
                }
                
                // Input Area
                HStack(spacing: 12) {
                    TextField("Message...", text: $messageText)
                        .padding(12)
                        .background(Color(white: 0.15))
                        .cornerRadius(20)
                        .foregroundColor(.white)
                    
                    Button(action: sendMessage) {
                        Image(systemName: "arrow.up.circle.fill")
                            .font(.system(size: 32))
                            .foregroundColor(messageText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? .gray : Color(hue: 0.6, saturation: 0.8, brightness: 0.8))
                    }
                    .disabled(messageText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
                .padding()
                .background(Color(hue: 0.61, saturation: 0.2, brightness: 0.1))
            }
        }
        .navigationTitle(contact.name)
        .navigationBarTitleDisplayMode(.inline)
    }
    
    private func sendMessage() {
        let text = messageText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        
        viewModel.sendMessage(to: contact, text: text)
        messageText = ""
    }
    
    private func scrollToBottom(proxy: ScrollViewProxy) {
        let chatMessages = viewModel.messages.filter { $0.contactKey == contact.signingPublicKey }
        if let lastId = chatMessages.last?.id {
            withAnimation {
                proxy.scrollTo(lastId, anchor: .bottom)
            }
        }
    }
}

struct MessageBubble: View {
    let message: Message
    
    var body: some View {
        HStack {
            if message.direction == .sent { Spacer() }
            
            Text(message.text)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(
                    message.direction == .sent
                    ? Color(hue: 0.6, saturation: 0.8, brightness: 0.8)
                    : Color(white: 0.2)
                )
                .foregroundColor(.white)
                .clipShape(ChatBubbleShape(isSent: message.direction == .sent))
            
            if message.direction == .received { Spacer() }
        }
    }
}

struct ChatBubbleShape: Shape {
    let isSent: Bool
    
    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: [
                .topLeft,
                .topRight,
                isSent ? .bottomLeft : .bottomRight
            ],
            cornerRadii: CGSize(width: 16, height: 16)
        )
        return Path(path.cgPath)
    }
}
