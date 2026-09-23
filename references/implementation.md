# Implementation Plan 1: Rebuilding Core Inception Architecture

> **Document Version:** 1.0.0 · **Target Module:** `app/` and `wear/`  
> **Source Repository Reference:** Existing Reference Android Codebase (Directory provided locally)

---

## 1. Objective & Scope

This implementation plan outlines the phased reconstruction of the complete, production-grade **Inception Android** messaging platform. 

### Source Code Reuse Policy
* The existing reference Android repository files serve as the canonical reference implementation.
* Developers/implementers can copy, adapt, and integrate existing source files from the reference directory at each stage.
* The stages are structured hierarchically by architectural dependency: foundational crypto and wire protocols must be established before mesh routing, background services, and UI layers.

---

## 2. Phased Implementation Roadmap

```
Stage 1: Build & Foundation (Gradle, Kotlin, Multi-Module Setup)
   │
   ▼
Stage 2: Cryptographic Core (Noise Protocol XX, Ed25519, Keystore)
   │
   ▼
Stage 3: Wire Binary Protocol (InceptionPacket, TLVs, DEFLATE, Framing)
   │
   ▼
Stage 4: BLE Mesh Networking (GATT Server/Client, Connections, Relay, Routing)
   │
   ▼
Stage 5: Multi-Transport Extension (Wi-Fi Aware & Nostr Relays)
   │
   ▼
Stage 6: Messaging & Gossip Synchronization (MessageRouter, Channels, GCS Sync)
   │
   ▼
Stage 7: Media & Audio Services (Voice Notes, Opus Live Voice, Image Transfers)
   │
   ▼
Stage 8: UI & Jetpack Compose Experience (Theme, Chat, Input, Globe, Sheets)
   │
   ▼
Stage 9: Quality Assurance & Release Verification (Unit Tests, Lint, Mesh Lab)
```

---

## Stage 1: Build & Project Setup

### Goal
Establish the multi-module Android project, Gradle configuration, build flavors, and external dependencies.

### Key Tasks & Files to Copy/Create:
1. **Root Build Configuration:**
   * `build.gradle.kts`: Pinned Kotlin 2.4.10, AGP 9.3.1.
   * `settings.gradle.kts`: Include `:app` and `:wear`.
   * `gradle/libs.versions.toml`: Centralized version catalog (Bouncy Castle 1.85, Tink 1.23.0, Nordic BLE 2.11.0, OkHttp 5.4.0, Compose BOM).
2. **Module Configurations:**
   * `app/build.gradle.kts`: Min SDK 26, Target SDK 37, Compose compiler, ABI splits (`arm64-v8a`, `x86_64`, `armeabi-v7a`, `x86`).
   * `wear/build.gradle.kts`: `syncSharedAppSources` Gradle task generating `wear/build/sharedSrc` from shared app packages.
3. **Android Manifest & Permissions:**
   * `app/src/main/AndroidManifest.xml`: Declare Bluetooth LE, Location, Notification, Foreground Service types (`connectedDevice`, `dataSync`, `microphone`), and `FileProvider`.

---

## Stage 2: Cryptographic Identity & Noise Protocol

### Goal
Implement decentralized, serverless identity generation and End-to-End Encryption (E2EE).

### Key Tasks & Files to Copy/Create:
1. **Key Generation & Storage:**
   * `crypto/`: Keypair generators for X25519 (Diffie-Hellman) and Ed25519 (signatures) via Bouncy Castle / Google Tink.
   * `identity/IdentityManager.kt`: Store identity in Android EncryptedSharedPreferences.
   * Peer ID computation: `hex(SHA-256(noiseStaticPublicKey)[0..<8])` (16 hex characters).
2. **Noise Protocol Implementation:**
   * `noise/NoiseProtocol.kt`: Noise `XX_25519_ChaChaPoly_BLAKE2s` handshake state machine.
   * `noise/NoiseSessionManager.kt`: Ephemeral and static key negotiation, session rotation, and cipher state.
   * `noise/NoiseEncryptionService.kt`: Symmetric encryption/decryption of direct messages.

---

## Stage 3: Wire Protocol & Binary Serialization

### Goal
Implement the compact binary wire protocol, packet envelopes, TLV parsing, and fragmentation.

### Key Tasks & Files to Copy/Create:
1. **Binary Protocol Engine:**
   * `protocol/BinaryProtocol.kt`:
     * Header serialization for v1 (13 bytes) and v2 (15 bytes, 4-byte payload length).
     * Flag handling (`HAS_SIGNATURE`, `HAS_RECIPIENT`, `IS_COMPRESSED`, `HAS_ROUTE`).
     * Ed25519 packet signing and verification (`TTL = 0` normalization).
2. **Data Models & TLVs:**
   * `model/InceptionPacket.kt`: Canonical packet representation.
   * `model/InceptionFilePacket.kt`: File TLVs (`0x01` FILE_NAME, `0x02` FILE_SIZE, `0x03` MIME_TYPE, `0x04` CONTENT).
3. **Compression & Resource Pooling:**
   * `protocol/CompressionUtil.kt`: DEFLATE compression with bounded expansion safety ceiling (10 MiB max).
   * `protocol/DecompressionResourcePool.kt`: Bounded Inflater reuse to prevent memory leaks.

---

## Stage 4: Bluetooth LE Mesh Networking

### Goal
Build the offline, multi-hop peer-to-peer mesh transport.

### Key Tasks & Files to Copy/Create:
1. **GATT Management:**
   * `mesh/BluetoothGattServerManager.kt`: Peripheral mode, advertising custom Inception service UUID, incoming MTU exchange.
   * `mesh/BluetoothGattClientManager.kt`: Central mode, BLE scanning, discovery, connection lifecycle.
   * `mesh/BluetoothConnectionTracker.kt`: Tracking connected peers, device address rotation handling.
2. **Packet Routing & Relay:**
   * `mesh/PacketRelayManager.kt`: Centralized relay logic, TTL decrement, source routing index calculation (`index + 1`), loop detection, and broadcast flooding fallback.
   * `mesh/BluetoothPacketBroadcaster.kt`: Targeted unicast to first hop vs. broadcast flood.
3. **Core Mesh Orchestration:**
   * `mesh/MeshCore.kt` and `mesh/BluetoothMeshService.kt`: Foreground service management, automatic boot restart, transient connection recovery.

---

## Stage 5: Multi-Transport Extension (Wi-Fi Aware & Nostr)

### Goal
Add high-bandwidth local transport and global internet reach.

### Key Tasks & Files to Copy/Create:
1. **Wi-Fi Aware (NAN):**
   * `wifi-aware/WifiAwareMeshService.kt`: Publish/subscribe discovery, IPv6 link-local socket management for high-speed file transfer.
2. **Nostr Transport:**
   * `nostr/NostrClient.kt`: WebSocket client connecting to public Nostr relays.
   * `nostr/NostrDirectMessageHandler.kt`: Nostr NIP-01 and NIP-44 encrypted direct messaging.
   * `nostr/NostrEmbeddedInception.kt`: Bridging mesh packets across Nostr relays.
3. **Transport Orchestration:**
   * `mesh/UnifiedMeshService.kt`: Best-transport selection (Wi-Fi Aware $\to$ BLE $\to$ Nostr), duplicate packet suppression across transport boundaries.

---

## Stage 6: Messaging, Channels & Gossip Sync

### Goal
Provide public/private chats, geohash channels, and Plumtree gossip synchronization.

### Key Tasks & Files to Copy/Create:
1. **Message Routing & Outbox:**
   * `services/MessageRouter.kt`: Outbox ticking, retry policies, store-and-forward queueing.
   * `ui/ChannelManager.kt`: Password-protected channels (Argon2id + AES-256-GCM), public channels.
2. **Gossip Sync Protocol:**
   * `sync/GcsSyncEngine.kt`: Golomb-Coded Sets (GCS) filter generation and query protocol for eventual consistency of public messages.
3. **Geohash Location Channels:**
   * `location/GeohashManager.kt`: Hierarchical geohash channels (levels 2–7), 3D globe coordinate calculation.

---

## Stage 7: Media & Audio Services

### Goal
Implement audio recording, playback, live voice, and media handling.

### Key Tasks & Files to Copy/Create:
1. **Voice Notes:**
   * `media/VoiceRecorder.kt`: 32 kbps AAC recording with 500ms end padding.
   * `media/AudioPlayer.kt`: Audio playback with 120-bin waveform seeking.
2. **Live Voice (Push-to-Talk):**
   * `mesh/LiveVoiceManager.kt`: Real-time Opus frame chunking, ephemeral transmission (`VOICE_FRAME 0x29`), jitter buffering.
3. **File & Image Transfer:**
   * `ui/MediaSendingManager.kt`: Image downscaling (512px max edge), generic file packaging, fragmented chunk streaming.

---

## Stage 8: UI & Jetpack Compose Experience

### Goal
Construct modern, fluid, dark-mode Material 3 user interfaces.

### Key Tasks & Files to Copy/Create:
1. **Design System & Theme:**
   * `ui/theme/Theme.kt`, `ui/theme/Color.kt`, `ui/theme/Type.kt`: Monospace accents, dark mode palette, peer color hashing.
2. **Chat Screen & Messaging Components:**
   * `ui/ChatScreen.kt`: Main conversation screen, private peer switcher, channel drawer.
   * `ui/MessageComponents.kt`: Text bubbles, audio waveform cards, image previews, delivery status indicators.
   * `ui/InputComponents.kt`: Pill-shaped composer, slide-to-cancel voice recorder, media picker actions.
3. **Auxiliary Sheets & Visualizers:**
   * `ui/MeshPeerListSheet.kt`: Active peer list, signal strength (RSSI), routing hops.
   * `ui/globe/GlobeView.kt`: 3D interactive OpenGL/Canvas globe rendering geohash presence.
   * `ui/debug/DebugSettingsSheet.kt`: Mesh graph visualizer, GCS sync debugger, simulated packet drops.

---

## Stage 9: Quality Assurance & Acceptance Verification

### Goal
Validate protocol adherence, cross-platform compatibility, and performance.

### Verification Steps:
1. **Unit & Contract Testing:**
   * Run `./gradlew testDebugUnitTest` to verify protocol encoders, Noise handshake, and GCS filters.
   * Verify golden binary vectors against iOS reference outputs.
2. **Build Verification:**
   * Run `./gradlew :app:assembleDebug :wear:assembleDebug`.
   * Check lint: `./gradlew lintDebug`.
3. **Physical Mesh Validation:**
   * Verify peer discovery and 2-hop relay forwarding using two physical Android test devices.