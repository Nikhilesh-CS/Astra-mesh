# AstraMesh

<div align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Kotlin-0095D5?&style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Tor-7D4698?style=for-the-badge&logo=tor&logoColor=white" alt="Tor" />
  <img src="https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge" alt="License" />
  <img src="https://img.shields.io/badge/Status-Active_Development-orange?style=for-the-badge" alt="Active Development" />
</div>

<h3 align="center">"Decentralized peer-to-peer messaging powered by Tor hidden services."</h3>

---

AstraMesh is a privacy-first, serverless, decentralized Android messenger. It enables direct onion-to-onion communication, removing the need for any centralized infrastructure or phone numbers. All communication is routed securely over the Tor network.

## 🚀 Features

- **Tor Hidden Services Integration**: Every device hosts its own V3 onion service.
- **Anonymous Communication**: No phone numbers, no emails, no central identity servers.
- **Peer-to-Peer Messaging**: Direct end-to-end messaging with no middlemen.
- **No Central Servers**: 100% decentralized architecture.
- **Bluetooth & Wi-Fi Local Communication**: Offline mesh capabilities via Google Nearby Connections (Beta).
- **Secure Local Storage**: Hardware-encrypted keys and local database storage.
- **Material 3 Interface**: Modern, beautiful, and responsive UI built with Jetpack Compose.
- **Automatic GitHub Updates**: In-app updater bypassing traditional app stores.
- **Privacy-First Architecture**: Designed strictly for minimal metadata collection.
- **Open-Source**: Transparent and verifiable codebase.

## 🛠 Technology Stack

- **Kotlin**: 100% Kotlin codebase.
- **Android SDK**: Native Android application (Min SDK 26).
- **Tor Embedded Binary**: Guardian Project's embedded Tor daemon.
- **Material Design 3**: UI built with Jetpack Compose.
- **Coroutines & Flows**: Modern reactive threading.
- **GitHub Releases API**: Automated app distribution.
- **LazySodium**: Cryptographic primitives.

## 🏗 Architecture

AstraMesh works by converting your smartphone into a secure Tor hidden service node.

```text
  User A Device                          User B Device
+---------------+                      +---------------+
| AstraMesh App |                      | AstraMesh App |
+-------+-------+                      +-------+-------+
        |                                      ^
        v                                      |
+-------+-------+                      +-------+-------+
|  Tor Daemon   |  --- Tor Network --- |  Tor Daemon   |
+-------+-------+                      +---------------+
|  Local Onion  |                      |  Peer Onion   |
+---------------+                      +---------------+
```
*(Direct End-to-End Communication)*

## 📥 Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Nikhilesh-CS/AstraMesh.git
   ```
2. **Open in Android Studio.**
3. **Build and Run:** Sync gradle and run the `app-debug` build on a physical Android device. (Note: Tor daemon startup might fail on an emulator depending on the ABI).

### Requirements
- **Android 8.0 (API 26)** or higher.
- Background execution and notification permissions are required for the daemon to stay alive.

## 📱 Usage Guide

1. **Start Tor**: Launch the app and wait for the Tor daemon to reach **Bootstrap 100%**.
2. **Get your Address**: Once connected, AstraMesh will display your unique `.onion` address.
3. **Add a Contact**: Obtain your peer's onion address (via out-of-band communication like a secure QR scan or signal message) and add it to your contacts.
4. **Ping**: Check the connectivity status of your peer via the built-in Ping feature.
5. **Chat**: Once the peer is online, begin secure chatting.

## 🛣 Roadmap

**Phase 1:** Core Tor connectivity and basic messaging capabilities. *(Current)*
**Phase 2:** Message persistence, reliable background notifications, and UI enhancements.
**Phase 3:** Forward-secure E2E encryption layering, friend management, and local mesh grouping.
**Phase 4:** Full decentralized ecosystem integration and cross-platform native support.

## 🤝 Contributing

We welcome contributions!
1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests.

## 🛡 Security

If you discover a security vulnerability within AstraMesh, please send an e-mail to the maintainers rather than creating a public issue. See [SECURITY.md](SECURITY.md) for more details on responsible disclosure.

## 📜 License

Distributed under the MIT License. See [LICENSE](LICENSE) for more information.

## 🙏 Acknowledgements

- [The Tor Project](https://www.torproject.org/)
- [Android Open Source Project](https://source.android.com/)
- [Material Design Team](https://m3.material.io/)
- [Guardian Project](https://guardianproject.info/)
