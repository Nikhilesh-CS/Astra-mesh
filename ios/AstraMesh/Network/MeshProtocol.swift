import Foundation

/// Shared peer wire format used across Nearby and Tor transports.
enum MeshProtocol {
    static let helloType = "hello"
    static let messageType = "msg"
    static let relayType = "relay"
    static let defaultTTL = 5
    
    struct EncryptedPayload: Equatable {
        let fromSigningKey: String
        let toSigningKey: String
        let ciphertextHex: String
        let nonceHex: String
        let signatureHex: String
    }
    
    enum Frame: Equatable {
        case hello(contactString: String)
        case message(EncryptedPayload)
        case relay(EncryptedPayload, ttl: Int)
    }
    
    static func encodeHello(contactString: String) -> String? {
        return encode(frame: .hello(contactString: contactString))
    }
    
    static func encodeDirectMessage(payload: EncryptedPayload) -> String? {
        return encode(frame: .message(payload))
    }
    
    static func encodeRelayMessage(payload: EncryptedPayload, ttl: Int = defaultTTL) -> String? {
        return encode(frame: .relay(payload, ttl: ttl))
    }
    
    static func encode(frame: Frame) -> String? {
        let object: [String: Any]
        
        switch frame {
        case .hello(let contactString):
            object = [
                "type": helloType,
                "contact": contactString
            ]
            
        case .message(let payload):
            object = [
                "type": messageType,
                "from": payload.fromSigningKey,
                "to": payload.toSigningKey,
                "ciphertext": payload.ciphertextHex,
                "nonce": payload.nonceHex,
                "signature": payload.signatureHex
            ]
            
        case .relay(let payload, let ttl):
            object = [
                "type": relayType,
                "dest": payload.toSigningKey,
                "from": payload.fromSigningKey,
                "ttl": ttl,
                "ciphertext": payload.ciphertextHex,
                "nonce": payload.nonceHex,
                "signature": payload.signatureHex
            ]
        }
        
        guard let data = try? JSONSerialization.data(withJSONObject: object) else {
            return nil
        }
        
        return String(data: data, encoding: .utf8)
    }
    
    static func decode(_ raw: String) -> Frame? {
        guard let data = raw.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let type = json["type"] as? String else {
            return nil
        }
        
        switch type {
        case helloType:
            guard let contactString = json["contact"] as? String else { return nil }
            return .hello(contactString: contactString)
            
        case messageType:
            guard let payload = parseEncryptedPayload(json: json, destinationKey: "to") else { return nil }
            return .message(payload)
            
        case relayType:
            guard let payload = parseEncryptedPayload(json: json, destinationKey: "dest") else { return nil }
            let ttl = (json["ttl"] as? Int) ?? defaultTTL
            return .relay(payload, ttl: ttl)
            
        default:
            return nil
        }
    }
    
    private static func parseEncryptedPayload(json: [String: Any], destinationKey: String) -> EncryptedPayload? {
        guard let from = json["from"] as? String,
              let to = json[destinationKey] as? String,
              let ciphertext = json["ciphertext"] as? String,
              let nonce = json["nonce"] as? String,
              let signature = json["signature"] as? String else {
            return nil
        }
        
        return EncryptedPayload(
            fromSigningKey: from,
            toSigningKey: to,
            ciphertextHex: ciphertext,
            nonceHex: nonce,
            signatureHex: signature
        )
    }
}
