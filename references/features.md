# Inception Android — Features Document

> **Version:** 2.0.2 · **Last Updated:** 2026-09-21

---

## 1. Feature Overview

Inception for Android is a decentralized peer-to-peer messaging app with two communication transports: **Bluetooth LE mesh** for offline/proximity messaging and the **Nostr protocol** for internet-based global reach. The app requires no accounts, phone numbers, or central servers.

### Feature Categories

| Category | Features |
|----------|---------|
| 🔗 Mesh Networking | BLE mesh, Wi-Fi Aware, multi-hop relay, source routing |
| 💬 Messaging | Public chat, private E2EE, channels, IRC commands |
| 🎤 Media | Voice notes, live voice, images, generic files |
| 🌍 Location | Geohash channels, 3D globe, presence, location notes |
| 🔐 Security | Noise Protocol E2EE, emergency wipe, QR verification |
| 🌐 Internet | Nostr relays, Tor support, transport bridging |
| 📱 Platform | Wear OS, cross-platform, offline APK sharing |

---

## 2. Mesh Networking Features

### 2.1 Bluetooth LE Mesh Network

**Description:** Fully decentralized mesh network over Bluetooth Low Energy. Devices automatically discover peers, form connections, and relay messages without any internet infrastructure.

**Capabilities:**
- Automatic peer discovery via BLE scanning and advertising
- Simultaneous GATT server and client roles
- Multi-hop relay routing (max 7 hops)
- Configurable connection limits with oldest-link eviction
- BLE address rotation handling with stable peer identity
- Recovery from Bluetooth toggle, airplane mode, and transient failures
- Persistent operation via Android foreground service
- Auto-start after device boot (user-configurable)

**Technical Details:**
- Transport: BLE 4.2+ (required hardware feature)
- Fragment size: 512 bytes per BLE write (469 bytes data after overhead)
- Inter-fragment delay: 20ms
- Packet signing: Ed25519 for origin authentication
- Packet compression: DEFLATE

---

### 2.2 Wi-Fi Aware Transport

**Description:** Higher-bandwidth local mesh transport using Wi-Fi Aware (NAN) on supported devices. Operates alongside BLE as part of the unified mesh.

**Capabilities:**
- Hardware capability detection (optional feature)
- Publish/subscribe session management
- Socket-based data exchange (higher throughput than BLE)
- Provisional socket authentication with canonical peer promotion
- Recovery from Wi-Fi, location, and airplane mode toggles
- Full resource cleanup on service stop

**Technical Details:**
- Transport: Wi-Fi Aware (Android 8.0+, hardware-dependent)
- IPv6 link-local communication
- Requires `NEARBY_WIFI_DEVICES` permission on Android 13+
- Managed alongside BLE by `UnifiedMeshService`

---

### 2.3 Source-Based Routing (v2)

**Description:** Efficient unicast routing where senders specify an explicit path of intermediate relay hops, avoiding broadcast flooding for directed messages.

**Capabilities:**
- Sender specifies explicit intermediate hop IDs
- Route embedded in v2 packet format
- Topology discovery via neighbor list gossip (TLV 0x04)
- Two-way edge verification (both peers must announce each other)
- Automatic fallback to broadcast/flood if next hop is unreachable
- Route covered by Ed25519 signature for integrity
- All fragments inherit the parent packet's route

---

### 2.4 Unified Transport Orchestration

**Description:** Intelligent transport selection that automatically chooses the best available transport (BLE, Wi-Fi Aware, or Nostr) for each message.

**Capabilities:**
- Automatic best-transport selection
- Seamless failover between transports
- Single peer identity across multiple transports
- Duplicate suppression across transports
- TTL-consistent relay across transport boundaries

---

### 2.5 Gossip Sync Protocol

**Description:** Plumtree-inspired gossip synchronization using Golomb-Coded Sets (GCS) for eventual consistency of public messages across the mesh.

**Capabilities:**
- Periodic sync every 30 seconds (broadcast to neighbors)
- Per-peer initial sync 5 seconds after first announcement
- Compact GCS filters (configurable 128–1024 bytes)
- Configurable false-positive rate (0.1%–5%, default 1%)
- Synchronizes ANNOUNCE and broadcast MESSAGE types
- Local-only (TTL=0, never relayed beyond direct neighbors)
- 60-second announcement age-out with 15-second pruning

---

## 3. Messaging Features

### 3.1 Public Broadcast Messaging

**Description:** Send text messages visible to all connected mesh peers. Messages relay through multi-hop routing up to 7 hops.

**Capabilities:**
- Broadcast text to all mesh peers
- Multi-hop relay with TTL-based routing
- Message deduplication across hops
- Ed25519 packet signing for origin verification
- Gossip sync for eventual consistency

---

### 3.2 Private End-to-End Encrypted Messaging

**Description:** Confidential one-to-one messaging using the Noise Protocol with mutual authentication and forward secrecy.

**Capabilities:**
- Noise Protocol XX handshake (X25519 + ChaCha20-Poly1305)
- Mutual authentication via static key exchange
- Forward secrecy through ephemeral keys
- Automatic Noise session establishment on first private message
- Private message routing via directed packets or Nostr fallback
- Store-and-forward for temporarily offline peers

**Security Model:**
- Peer ID cryptographically bound to Noise static key
- Announcement verification at both discovery and handshake stages
- Rehandshake support without disrupting active sessions
- HSTS-style capability pinning (no silent downgrades)

---

### 3.3 Channel-Based Group Messaging

**Description:** Topic-based group channels with optional password protection for organized group conversations.

**Capabilities:**
- Create and join named channels (IRC-style `/join #channel`)
- Optional password protection using Argon2id KDF + AES-256-GCM
- Channel messages encrypted with derived key
- Per-channel message history
- Channel member listing (`/who`)

---

### 3.4 IRC-Style Slash Commands

**Description:** Familiar command interface inspired by IRC for power-user interactions.

**Supported Commands:**

| Command | Description |
|---------|-------------|
| `/join #channel` | Join or create a channel |
| `/join #channel password` | Join a password-protected channel |
| `/msg <peer> <text>` | Send a private message |
| `/who` | List users in current channel/mesh |
| `/nick <name>` | Change display nickname |
| `/leave` | Leave current channel |
| `/help` | Show available commands |

---

### 3.5 Mutual Favorites & Nostr DM Fallback

**Description:** Favorite peers for persistent private communication. When mesh is unavailable, private messages automatically fall back to Nostr relay-based delivery.

**Capabilities:**
- Mark peers as favorites for persistent communication
- Encrypted favorite state persistence
- Automatic Nostr DM fallback for favorites when mesh is unavailable
- NIP-04 and NIP-44 encrypted direct message support
- Peer availability notifications

---

## 4. Media & File Transfer Features

### 4.1 Voice Notes

**Description:** Hold-to-record voice messages transmitted over the mesh with waveform visualization.

**Recording:**
- Hold mic button to start recording
- Real-time scrolling waveform overlay during recording (~240 columns, ~20 FPS)
- 500ms end padding prevents audio clipping
- Keyboard stays visible; text cursor hidden during recording
- AAC codec in MP4 container (`audio/mp4`)
- 44.1 kHz sample rate, mono, ~32 kbps

**Playback:**
- 120-bin static waveform (identical on sender and receiver)
- Play/pause toggle with duration display
- Interactive waveform seeking (tap to jump to position)
- Progress fill during transfer (blue) and playback (green)

**Transfer:**
- Fragment progress tracking with cancel support
- Deterministic transfer ID via SHA-256
- Private voice notes encrypted as Noise payload 0x20

---

### 4.2 Live Push-to-Talk Voice

**Description:** Real-time streaming voice over the mesh while the user holds the mic button. The finalized voice note is sent afterward as a reliable fallback.

**Capabilities:**
- Real-time AAC-LC streaming (16 kHz, mono, 64ms per frame)
- Unique 8-byte burst ID per gesture
- START → DATA (1–8 frames per packet) → END sequence
- CANCELED flag for aborted recordings
- Max 210 bytes per packet (below fragmentation threshold)
- Public mesh: signed `VOICE_FRAME` (0x29) broadcast
- Private: encrypted Noise inner payload (0x08)
- Receiver: bounded assembly with gap/loss handling

---

### 4.3 Image Sharing

**Description:** Send images over the mesh with automatic downscaling and a distinctive block-reveal progress animation.

**Sending:**
- System image picker (SAF) — no storage permission required
- Automatic downscaling to 512px longest edge
- JPEG compression at 85% quality
- EXIF orientation handling

**Progress Visualization:**
- Block-reveal animation (24×16 grid, dense, no gaps)
- Fragments map to sequential grid blocks
- Cancel button overlay during sending

**Receiving:**
- Full image render with rounded corners
- Tap to open fullscreen viewer
- Save to device Downloads via MediaStore

---

### 4.4 Generic File Transfer

**Description:** Send arbitrary files over the mesh with the same TLV encoding, progress tracking, and cancellation support.

**Capabilities:**
- Any file type via InceptionFilePacket TLV
- File pill rendering with icon and filename
- Open/save via system handlers
- Filename collision handling with `(n)` suffix

---

## 5. Location-Based Features

### 5.1 Geohash Location Channels

**Description:** Geographic chat rooms using geohash coordinates over Nostr relays, enabling location-aware group communication.

**Capabilities:**
- Browse channels at multiple precision levels (Region → Building)
- Send and receive messages in location channels
- Per-geohash ephemeral identity (privacy-preserving)
- Online participant counting

**Precision Levels:**

| Precision | Level | Area |
|-----------|-------|------|
| 2 | Region | ~2,500 km |
| 4 | Province | ~40 km |
| 5 | City | ~5 km |
| 6 | Neighborhood | ~1.2 km |
| 7 | Block | ~150 m |
| 8+ | Building | ~40 m |

---

### 5.2 Interactive 3D Globe Picker

**Description:** An interactive 3D Earth globe rendered with Compose Canvas for intuitive geohash location selection.

**Capabilities:**
- Rotate the globe to browse geographic regions
- Zoom to select precision level
- Visual geohash grid overlay
- Direct channel joining from globe selection

---

### 5.3 Geohash Presence

**Description:** Privacy-preserving online presence tracking for geohash channels using ephemeral Nostr events.

**Capabilities:**
- Heartbeat broadcasting (Kind 20001) at randomized intervals (40–80s)
- Privacy restriction: only broadcast at precision ≤ 5 (City level)
- Temporal decorrelation with 2–5 second inter-broadcast delays
- Online threshold: 5 minutes since last heartbeat
- Uncertainty display (`? people`) for high-precision zero-count channels
- Counts aggregate both heartbeats and active chat messages

---

### 5.4 Geohash Bookmarks

**Description:** Save favorite geohash locations for quick access to frequently visited channels.

**Capabilities:**
- Bookmark any geohash location
- Quick-switch between bookmarked channels
- Persistent bookmark storage

---

### 5.5 Location Notes

**Description:** Create persistent location-pinned notes visible to other users in the same geographic area.

**Capabilities:**
- Create notes at specific geohash coordinates
- Notes persist via Nostr events
- Browse nearby notes from other users

---

## 6. Security & Privacy Features

### 6.1 Noise Protocol Encryption

**Description:** State-of-the-art end-to-end encryption using the Noise Protocol Framework for all private communications.

**Details:**
- XX handshake pattern: mutual static-key authentication
- X25519 Diffie-Hellman key agreement
- ChaCha20-Poly1305 authenticated encryption
- SHA-256 for handshake hashing
- Forward secrecy via ephemeral key exchange
- Session rehandshake without disrupting existing sessions
- Inbound rehandshake uses candidate isolation

---

### 6.2 Ed25519 Packet Authentication

**Description:** Every mesh packet carries a cryptographic signature proving its origin, preventing spoofing and tampering.

**Details:**
- Ed25519 signing of full packet content
- TTL normalization (set to 0) for relay compatibility
- Signature covers header, IDs, route, and payload
- Public key distributed via announcement TLV 0x03

---

### 6.3 Emergency Data Wipe

**Description:** Triple-tap gesture instantly clears all cryptographic material, messages, identity state, and preferences.

**Capabilities:**
- Immediate destruction of all key material
- Clear all message history
- Reset all preferences and state
- In-flight operations cancelled
- No recovery possible after wipe

---

### 6.4 QR-Based Key Verification

**Description:** Out-of-band key verification via QR code scanning to establish trust beyond TOFU.

**Capabilities:**
- Generate QR code with peer's key fingerprint
- Camera-based QR scanning (CameraX + ML Kit)
- Visual confirmation of key match
- Deep link support (`inception://verify`)

---

### 6.5 Private Media Security

**Description:** Enhanced security for media transfers with capability-based encryption, HSTS-style pinning, and mixed-client migration.

**Capabilities:**
- Authenticated capability exchange (0x21 peer state)
- HSTS-style pinning: once a peer proves private-media capability, it cannot silently downgrade
- Legacy client detection with explicit user consent for unencrypted fallback
- Five-second watchdog for old-client detection
- Panic wipe clears all capability state

---

### 6.6 Channel Encryption

**Description:** Password-protected channels with strong key derivation and authenticated encryption.

**Details:**
- KDF: Argon2id (password → encryption key)
- Cipher: AES-256-GCM (authenticated encryption)
- Per-channel key derivation
- Password not transmitted over the wire

---

## 7. Internet & Connectivity Features

### 7.1 Nostr Protocol Integration

**Description:** Internet-based messaging via the Nostr protocol for global reach beyond Bluetooth range.

**Capabilities:**
- Multi-relay WebSocket connections
- NIP-compliant event construction and signing
- Subscription filter management
- Relay reconnection with backoff policies
- Event deduplication
- Background relay maintenance

---

### 7.2 Tor Support (Arti)

**Description:** Built-in Tor client using the Arti (Rust) library for private internet connectivity.

**Capabilities:**
- Tor circuit establishment
- Relay connections through Tor
- Onion service client support
- JNI bridge to native Rust Arti library

---

### 7.3 Nostr-Inception Transport Bridge

**Description:** Bridge Inception mesh protocol over Nostr events for seamless communication across transport boundaries.

**Capabilities:**
- Embed Inception wire protocol in Nostr events
- Nostr DM fallback for mutual favorites
- Transparent to the messaging layer

---

## 8. Platform Features

### 8.1 Wear OS Support

**Description:** Companion Wear OS app sharing core mesh and protocol code from the phone client.

**Capabilities:**
- BLE mesh networking on the watch
- Live voice send and receive
- Private messaging
- Shared source from `app/` via `syncSharedAppSources`
- Compact watch-optimized UI

---

### 8.2 Cross-Platform Compatibility

**Description:** Full binary protocol compatibility with the iOS/macOS client.

**Capabilities:**
- Identical v1/v2 packet encoding
- Shared Noise XX handshake parameters
- Compatible TLV layouts for announcements and files
- Shared golden test vectors
- Coordinated capability bitfield

---

### 8.3 Offline APK Sharing

**Description:** Share the Inception APK with nearby devices via Wi-Fi hotspot, enabling peer-to-peer app distribution without internet.

**Capabilities:**
- Create Wi-Fi hotspot for APK sharing
- NanoHTTPD server for file hosting
- QR code for connection details
- Wake lock to keep hotspot alive

---

### 8.4 In-App APK Updates

**Description:** Download and install app updates from GitHub Releases directly within the app.

**Capabilities:**
- Check GitHub Releases for new versions
- Background download via WorkManager
- Foreground service notification during download
- Version comparison and update prompts

---

## 9. User Interface Features

### 9.1 Material 3 Design

**Description:** Modern Android UI built with Jetpack Compose and Material 3 design language.

**Key Screens:**
- Chat screen with message timeline
- Peer list with mesh diagnostics
- Geohash channel browser
- 3D globe geohash picker
- Security verification sheet
- About/settings sheet with debug options
- Onboarding permission flow

---

### 9.2 Notifications

**Description:** Rich notification system for incoming messages, peer availability, and conversation management.

**Capabilities:**
- Per-conversation notification grouping
- Message reply from notification
- Peer online/offline notifications
- Foreground service notification for mesh status
- Notification channels for different message types

---

### 9.3 Conversation Management

**Description:** Organized conversation view with unread tracking and conversation summaries.

**Capabilities:**
- Public mesh chat
- Private conversations (per-peer)
- Channel conversations (per-channel)
- Geohash location channels
- Unread message counts and summaries
- Conversation security state indicators

---

### 9.4 Cashu Token Rendering

**Description:** Inline detection and rendering of Cashu ecash tokens in chat messages.

---

### 9.5 Link Preview Pills

**Description:** Inline link previews with visual pill rendering for URLs in chat messages.

---

## 10. Developer & Debug Features

### 10.1 Mesh Diagnostics

**Description:** Debug sheet with detailed mesh network state, transport metrics, and configuration.

**Capabilities:**
- Connected peer list with transport details
- BLE scan/advertise state
- Wi-Fi Aware session state
- Sync settings (GCS parameters)
- Duty cycling configuration
- Mesh topology visualization

---

### 10.2 Mesh Lab Physical Testing

**Description:** Comprehensive physical-device test framework for validating mesh behavior on real hardware.

**Capabilities:**
- Multi-device test orchestration
- Transport validation matrix
- BLE discovery and recovery testing
- GATT setup/teardown verification
- Fragmentation boundary testing
- Cross-transport failover testing
- Evidence collection and reporting

---

### 10.3 Reproducible Builds

**Description:** Deterministic build system for byte-identical APK/AAB reproduction.

**Capabilities:**
- Pinned Linux container environment
- SHA-256 checksum verification
- GitHub release certificate validation
- Build tools version pinning