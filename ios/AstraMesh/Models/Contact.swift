import Foundation
import Sodium

/// A known contact with their public keys.
/// Keys are stored as lowercase hex strings for easy DB storage and protocol compatibility.
struct Contact: Identifiable, Equatable {
    /// Ed25519 signing public key (64-char hex = 32 bytes) — unique identifier / address
    let signingPublicKey: String
    /// X25519 encryption public key (64-char hex = 32 bytes)
    let encryptionPublicKey: String
    /// Display name
    let name: String
    /// Optional Tor hidden-service hostname used for distant messaging
    let onionAddress: String = ""
    
    /// Identifiable conformance — uses the signing key as the unique ID.
    var id: String { signingPublicKey }
    
    /// Encryption public key decoded to raw bytes.
    var encryptionPublicKeyBytes: Bytes {
        return decodeHex(encryptionPublicKey)
    }
    
    /// Signing public key decoded to raw bytes.
    var signingPublicKeyBytes: Bytes {
        return decodeHex(signingPublicKey)
    }
}
