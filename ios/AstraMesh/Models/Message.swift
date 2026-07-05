import Foundation

/// Direction of a chat message.
enum MessageDirection: String, Codable {
    case sent
    case received
}

/// A chat message stored locally in the database.
struct Message: Identifiable, Equatable {
    /// Auto-incremented database primary key.
    let id: Int64?
    /// The contact's signing public key (hex) — foreign key into contacts table.
    let contactKey: String
    /// Plaintext message body.
    let text: String
    /// Unix timestamp (seconds since epoch).
    let timestamp: Double
    /// Whether this message was sent or received.
    let direction: MessageDirection
    
    init(id: Int64? = nil, contactKey: String, text: String, timestamp: Double, direction: MessageDirection) {
        self.id = id
        self.contactKey = contactKey
        self.text = text
        self.timestamp = timestamp
        self.direction = direction
    }
}
