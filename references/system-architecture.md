# Inception Android — System Architecture Document

> **Version:** 2.0.2 · **Last Updated:** 2026-09-21

---

## 1. Executive Summary

Inception is a decentralized, serverless peer-to-peer messaging application for Android (phone and Wear OS) with a **dual-transport architecture**:

1. **Bluetooth LE mesh** — offline, multi-hop communication with zero internet dependency.
2. **Nostr protocol** — internet-based global messaging via public relays.

The system requires no accounts, phone numbers, or central servers. Identity is derived entirely from locally generated cryptographic key material. Messages route automatically through the optimal transport and fall back gracefully when connectivity changes.

---

## 2. High-Level Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                       INCEPTION APP                          │
│  ┌──────────┐  ┌──────────────┐  ┌────────────────────────┐  │
│  │ Compose  │  │  ViewModels  │  │  Service / Background  │  │
│  │   UI     │◄─┤  (MVVM)      │◄─┤  Layer                 │  │
│  └──────────┘  └──────┬───────┘  └───────────┬────────────┘  │
│                       │                      │               │
│            ┌──────────▼──────────────────────▼──────────┐    │
│            │        MeshCore / UnifiedMeshService        │    │
│            │   (Transport selection, routing, relay)     │    │
│            └──────┬────────────┬────────────┬───────────┘    │
│                   │            │            │                │
│         ┌─────────▼──┐  ┌─────▼──────┐  ┌──▼───────────┐   │
│         │ BLE Mesh   │  │ Wi-Fi      │  │ Nostr        │   │
│         │ Service    │  │ Aware      │  │ Transport    │   │
│         │            │  │ Service    │  │              │   │
│         └─────┬──────┘  └─────┬──────┘  └──────┬───────┘   │
│               │               │                │            │
└───────────────┼───────────────┼────────────────┼────────────┘
                │               │                │
          ┌─────▼─────┐  ┌─────▼─────┐  ┌───────▼───────┐
          │ Bluetooth  │  │ Wi-Fi     │  │  Nostr        │
          │ LE Radio   │  │ Aware     │  │  Relays       │
          │ (Hardware) │  │ (Hardware)│  │  (Internet)   │
          └────────────┘  └───────────┘  └───────────────┘
```

---

## 3. Module Architecture

### 3.1 Project Modules

| Module | Purpose |
|--------|---------|
| `app/` | Phone client — Kotlin/Compose, all mesh, protocol, crypto, UI, and Nostr code |
| `wear/` | Wear OS client — shared source via `syncSharedAppSources` Gradle task |
| `tools/` | Release tooling, Mesh Lab scripts, checksum verification |
| `docs/` | Protocol specifications, release guides, testing conventions |

### 3.2 Source of Truth

`app/` is the single source of truth for all shared mesh and protocol code. The `wear/` module's `build.gradle.kts` declares an include list that the `syncSharedAppSources` task uses to generate `wear/build/sharedSrc`. Shared Kotlin must never be copied into `wear/src/` or edited in `build/`.

---

## 4. Package Architecture (app module)

```
com.inception.android
├── core/                   # Core utilities and base classes
├── crypto/                 # EncryptionService — AES-256-GCM, Argon2id
├── favorites/              # Mutual-favorite peer management & persistence
├── features/
│   ├── file/               # Generic file sharing utilities
│   ├── media/              # ImageUtils — downscaling, EXIF handling
│   └── voice/              # VoiceRecorder, Waveform, VoiceBurstPacket, LiveVoiceManager
├── geohash/                # Geohash computation, location channels, privacy gates
├── hotspot/                # Offline APK sharing via Wi-Fi hotspot + NanoHTTPD
├── identity/               # SecureIdentityStateManager — encrypted key/state storage
├── mesh/                   # ★ Core mesh networking (see §5)
├── model/                  # Data models — InceptionFilePacket, InceptionMessage, etc.
├── net/                    # Network utilities
├── noise/                  # Noise Protocol framework (see §7)
├── nostr/                  # Nostr relay client, subscriptions, DM handler, crypto
├── onboarding/             # Permission flows, Bluetooth/location checks
├── protocol/               # BinaryProtocol — packet serialization (v1/v2)
├── service/                # MeshForegroundService, boot receiver, notification receiver
├── services/               # Auxiliary service layer
├── sync/                   # GCS filter gossip sync — GossipSyncManager, PacketIdUtil
├── ui/                     # ★ Jetpack Compose UI layer (see §9)
├── util/                   # General-purpose utilities
├── utils/                  # Additional helpers
└── wifi-aware/             # Wi-Fi Aware transport — WifiAwareMeshService
```

---

## 5. Mesh Networking Layer

### 5.1 Transport Services

| Service | File | Transport | Range |
|---------|------|-----------|-------|
| `BluetoothMeshService` | `mesh/BluetoothMeshService.kt` | BLE 4.2+ | ~100m, multi-hop |
| `WifiAwareMeshService` | `wifi-aware/WifiAwareMeshService.kt` | Wi-Fi Aware (NAN) | ~70m, high bandwidth |
| `UnifiedMeshService` | `mesh/UnifiedMeshService.kt` | Transport orchestrator | Selects optimal path |

### 5.2 BLE Mesh Stack

```
BluetoothMeshService
├── BluetoothGattServerManager      — GATT server setup, characteristic writes
├── BluetoothGattClientManager      — GATT client connections, notifications
├── BluetoothConnectionManager      — Peer lifecycle, connection coordination
├── BluetoothConnectionTracker      — Active link tracking and limits
├── BluetoothPacketBroadcaster      — Fragmentation, progress, cancellation
├── FragmentManager                 — Fragment assembly and reassembly
├── FragmentingPacketSender         — MTU-aware fragment dispatch
├── PacketProcessor                 — Inbound packet routing and dispatch
├── PacketRelayManager              — TTL-based multi-hop relay
├── MessageHandler                  — Application-layer message processing
├── PeerManager                     — Peer discovery, state, last-seen tracking
├── PeerFingerprintManager          — Noise key fingerprint management
├── SecurityManager                 — Packet signing and verification (Ed25519)
├── PowerManager                    — Adaptive duty cycling, battery optimization
├── StoreForwardManager             — Offline message queuing and delivery
├── TransferProgressManager         — File transfer progress events
├── PrivateMediaTransfer            — Encrypted media send pipeline
├── PrivateMediaSecurity            — Security policy for private media
├── AuthenticatedPeerStateCoordinator — Noise 0x21 peer-state exchange
├── RetryingControlPacketSender     — Reliable control packet delivery
├── MeshCore                        — Shared mesh logic for both transports
└── DirectLinkAnnouncementPolicy    — Peer-directed announcement behavior
```

### 5.3 Packet Flow

```
  SENDER                                          RECEIVER
    │                                                │
    │  1. Build InceptionPacket                      │
    │  2. Sign (Ed25519)                             │
    │  3. Compress (DEFLATE)                         │
    │  4. Fragment (if > MTU)                        │
    │  5. BLE write / Wi-Fi send                     │
    │ ──────────────────────────────────────────────► │
    │                                                │
    │                          6. Reassemble fragments│
    │                          7. Decompress          │
    │                          8. Verify signature    │
    │                          9. Deduplicate         │
    │                         10. Route / deliver     │
    │                                                │
```

### 5.4 Multi-Hop Routing

- **Flood routing** (default): broadcast packets relay to all connected neighbors with TTL decrement (max 7 hops).
- **Source routing** (v2): sender specifies intermediate hop IDs; relay nodes unicast to the next hop with broadcast fallback.
- **Topology discovery**: announcements carry a `DIRECT_NEIGHBORS` TLV (0x04); confirmed bidirectional edges build a local mesh graph for route computation.

---

## 6. Binary Protocol

### 6.1 Packet Versions

| Version | Header Size | Payload Length Field | Features |
|---------|-------------|---------------------|----------|
| v1 | 13 bytes | 2 bytes (max 64 KiB) | Legacy, no routing |
| v2 | 15 bytes | 4 bytes (max ~4 GiB) | Source routing, large file transfer |

### 6.2 Message Types

| Type | Hex | Purpose |
|------|-----|---------|
| `ANNOUNCE` | `0x01` | Identity announcement with TLV payload |
| `MESSAGE` | `0x10` | Public/broadcast text message |
| `NOISE_ENCRYPTED` | `0x11` | Private encrypted message (Noise envelope) |
| `REQUEST_SYNC` | `0x21` | GCS filter gossip sync request |
| `FILE_TRANSFER` | `0x22` | Public file/media transfer |
| `VOICE_FRAME` | `0x29` | Live push-to-talk audio burst |

### 6.3 Packet Structure (v2)

```
┌──────────────────── Fixed Header (15 bytes) ────────────────────┐
│ Version(1) │ Type(1) │ TTL(1) │ Timestamp(8) │ Flags(1) │ PayloadLen(4) │
├──────────────────── Variable Sections ──────────────────────────┤
│ SenderID(8) │ RecipientID(8, optional) │ Route(variable, optional) │
│ Payload(PayloadLen) │ Signature(64, optional) │
└─────────────────────────────────────────────────────────────────┘
```

### 6.4 Fragmentation

- Fragments are 512 bytes max per BLE transport threshold (469 bytes data after overhead).
- Private media: max 256 fragments per transfer.
- Transfer ID: `SHA-256(payload)` for progress tracking.
- Inter-fragment delay: 20 ms.

---

## 7. Cryptographic Architecture

### 7.1 Noise Protocol (Private Messaging)

| Aspect | Detail |
|--------|--------|
| **Pattern** | XX (mutual authentication) |
| **DH** | X25519 |
| **Cipher** | ChaCha20-Poly1305 |
| **Hash** | SHA-256 |
| **Library** | Southern Storm Noise (Java, bundled in `noise/southernstorm/`) |

**Session lifecycle:**
1. Peer discovery → identity announcement received.
2. Initiator starts XX handshake (`NoiseSessionManager`).
3. Three-message exchange with mutual static-key authentication.
4. `NoiseSession` established with transport ciphers.
5. Peer-state proof (`0x21`) exchanged for capability verification.
6. Forward-secret channel active for private messages and media.

**Peer ID binding:**
`peerID = hex(SHA-256(noiseStaticPublicKey)[0..<8])` — 16 hex characters. Verified at both announcement and Noise handshake stages.

### 7.2 Packet Signing (Public Mesh)

- **Algorithm**: Ed25519
- **Scope**: Header + SenderID + RecipientID + Route + Payload (TTL normalized to 0)
- **Key distribution**: Signing public key in announcement TLV `0x03`

### 7.3 Channel Encryption

- **KDF**: Argon2id (password → key)
- **Cipher**: AES-256-GCM
- **Purpose**: Password-protected topic-based group channels

### 7.4 Identity Storage

- `SecureIdentityStateManager`: encrypted persistent storage via AndroidX Security Crypto (EncryptedSharedPreferences)
- Noise static keypair, Ed25519 signing keypair, Nostr identity — all persisted encrypted

### 7.5 Emergency Wipe

Triple-tap triggers immediate clearance of all cryptographic material, identity state, messages, and preferences.

---

## 8. Nostr Integration

### 8.1 Components

```
nostr/
├── NostrClient               — WebSocket connection to relays (OkHttp)
├── NostrRelayManager          — Multi-relay connection pool and lifecycle
├── NostrProtocol              — NIP-compliant event construction
├── NostrCrypto                — Schnorr signing, NIP-04/NIP-44 encryption
├── NostrIdentity              — Ephemeral per-geohash key management
├── NostrTransport             — Nostr-as-mesh-transport bridge
├── NostrFilter                — Subscription filter construction
├── NostrSubscriptionManager   — Active subscription lifecycle
├── NostrDirectMessageHandler  — Private DM processing
├── NostrBackgroundRuntime     — Background relay maintenance
├── NostrBackgroundEventProcessor — Async event ingestion
├── NostrEmbeddedInception     — Inception wire protocol over Nostr events
├── RelayDirectory             — Curated relay list management
├── GeohashRepository          — Location channel message storage
├── GeohashMessageHandler      — Geohash event processing
├── LocationNotesManager       — Persistent location-pinned notes
└── NostrProofOfWork           — Optional PoW for relay admission
```

### 8.2 Event Kinds

| Kind | Purpose |
|------|---------|
| `20000` | Geohash chat message (ephemeral) |
| `20001` | Geohash presence heartbeat (ephemeral) |
| `4` | NIP-04 encrypted direct message |
| `1059` | NIP-44 gift-wrapped sealed message |

### 8.3 Transport Bridge

`NostrTransport` / `TransportBridgeService` bridge mesh packets over Nostr for mutual favorites when both peers are online via relays but not in Bluetooth range. Private messages fall back to Nostr DMs.

---

## 9. UI Architecture

### 9.1 Technology Stack

| Layer | Technology |
|-------|-----------|
| **UI Framework** | Jetpack Compose (Material 3) |
| **Architecture** | MVVM with ViewModels |
| **State** | `StateFlow`, `SharedFlow` |
| **Navigation** | Jetpack Navigation Compose |
| **Concurrency** | Kotlin Coroutines + Flow |

### 9.2 Key UI Components

| Component | File | Responsibility |
|-----------|------|---------------|
| `MainActivity` | `MainActivity.kt` | App entry, navigation host, permission orchestration |
| `ChatScreen` | `ui/ChatScreen.kt` | Main chat interface |
| `ChatViewModel` | `ui/ChatViewModel.kt` | Message state, send/receive, media |
| `InputComponents` | `ui/InputComponents.kt` | Text input, mic button, recording overlay |
| `MessageComponents` | `ui/MessageComponents.kt` | Message rendering (text, audio, image, file) |
| `ChatHeader` | `ui/ChatHeader.kt` | Peer count, channel info, navigation |
| `LocationChannelsSheet` | `ui/LocationChannelsSheet.kt` | Geohash channel browser |
| `MeshPeerListSheet` | `ui/MeshPeerListSheet.kt` | Connected peer list, mesh diagnostics |
| `GeohashPickerActivity` | `ui/GeohashPickerActivity.kt` | 3D globe geohash selector |
| `SecurityVerificationSheet` | `ui/SecurityVerificationSheet.kt` | QR-based key verification |
| `AboutSheet` | `ui/AboutSheet.kt` | App info, debug settings |

### 9.3 Geohash Globe

An interactive 3D globe rendered with Compose Canvas for geohash location picking. Users rotate the globe to select geographic areas for location-based channels at varying precision levels.

---

## 10. Background Services Architecture

```
┌──────────────────────────────────────────────────────┐
│            MeshForegroundService                      │
│  (foregroundServiceType: connectedDevice|dataSync|    │
│   location)                                          │
│                                                      │
│  ┌─────────────────┐  ┌──────────────────┐           │
│  │ BluetoothMesh   │  │ WifiAwareMesh    │           │
│  │ Service         │  │ Service          │           │
│  └────────┬────────┘  └────────┬─────────┘           │
│           │                    │                     │
│  ┌────────▼────────────────────▼─────────┐           │
│  │        UnifiedMeshService              │           │
│  │  (Transport selection & orchestration) │           │
│  └───────────────────────────────────────┘           │
│                                                      │
│  ┌──────────────────────┐                            │
│  │ NostrBackgroundRuntime│  (relay connections)       │
│  └──────────────────────┘                            │
│                                                      │
│  ┌──────────────────────┐                            │
│  │ GossipSyncManager    │  (30s periodic sync)       │
│  └──────────────────────┘                            │
│                                                      │
│  ┌──────────────────────┐                            │
│  │ PowerManager         │  (duty cycling)            │
│  └──────────────────────┘                            │
└──────────────────────────────────────────────────────┘
       │
       │ BOOT_COMPLETED
       ▼
┌──────────────────┐
│ BootCompleted    │  Auto-start if enabled
│ Receiver         │
└──────────────────┘
```

---

## 11. Gossip Sync Protocol

Based on Plumtree-inspired gossip using Golomb-Coded Sets (GCS):

1. Every 30 seconds, each node sends `REQUEST_SYNC` (0x21) with a compact GCS of recently seen packet IDs.
2. Receivers check which local packets are missing from the filter and respond with originals.
3. Only ANNOUNCE and broadcast MESSAGE types are synchronized.
4. Sync is strictly local-only (TTL=0), never relayed beyond direct neighbors.
5. Announcement retention: 60-second age-out, latest-per-peer deduplication.

---

## 12. Data Flow Diagrams

### 12.1 Private Message Flow

```
Sender                              Mesh                              Receiver
  │                                                                      │
  │  1. User types message                                               │
  │  2. Noise encrypt (ChaCha20-Poly1305)                                │
  │  3. Wrap as NOISE_ENCRYPTED (0x11)                                   │
  │  4. Sign outer packet (Ed25519)                                      │
  │  5. Compress + fragment                                              │
  │ ──────── BLE/Wi-Fi ────────────► Relay ──────── BLE/Wi-Fi ──────────►│
  │                                (TTL--)                               │
  │                                                 6. Reassemble        │
  │                                                 7. Verify signature  │
  │                                                 8. Noise decrypt     │
  │                                                 9. Deliver to UI     │
```

### 12.2 File Transfer Flow

```
Sender                              Mesh                              Receiver
  │                                                                      │
  │  1. Select/capture media                                             │
  │  2. Encode InceptionFilePacket (TLV)                                 │
  │  3. (Private: Noise encrypt as 0x20)                                 │
  │  4. Sign + fragment (max 256 frags)                                  │
  │  5. Compute transferId = SHA-256(payload)                            │
  │  6. Map transferId → messageId                                       │
  │ ────── Fragment stream (20ms gap) ────►  ────── Forward ────────────►│
  │  7. Progress events per fragment                                     │
  │                                                 8. Reassemble        │
  │                                                 9. Decode TLV        │
  │                                                10. Persist to disk   │
  │                                                11. Create chat msg   │
```

---

## 13. Cross-Platform Compatibility

| Dimension | Detail |
|-----------|--------|
| **iOS/macOS** | Binary protocol compatible with reference client (Swift/iOS) |
| **Wire format** | Identical v1/v2 packet encoding, TLV layouts, Noise XX |
| **Golden vectors** | Shared test vectors for VoiceBurstPacket, GCS, file TLV |
| **Capability bits** | Coordinated announcement bitfield (bits 0–10) |
| **Wear OS** | Shared source from `app/`; BLE-only (no Wi-Fi Aware) |

---

## 14. Deployment Architecture

```
                                    ┌─────────────┐
                                    │ Google Play  │
                                    │   Store      │
                                    └──────┬──────┘
                                           │ AAB
         ┌─────────┐               ┌──────▼──────┐               ┌────────────┐
         │ GitHub   │── APK/AAB ──►│   Android   │◄── sideload ──│ Hotspot    │
         │ Releases │               │   Devices   │               │ APK Share  │
         └─────────┘               └──────┬──────┘               └────────────┘
                                          │
                                   ┌──────▼──────┐
                                   │  BLE Mesh   │◄──── No internet needed
                                   │  (P2P)      │
                                   └──────┬──────┘
                                          │
                                   ┌──────▼──────┐
                                   │ Nostr Relays│◄──── Optional internet
                                   │ (Public)    │      (optionally via Tor)
                                   └─────────────┘
```

### 14.1 Build & Distribution

- **Reproducible builds**: Pinned Linux container for byte-identical APK/AAB rebuilds
- **ABI splits**: arm64-v8a, x86_64, armeabi-v7a, x86, plus universal
- **Channels**: Google Play (AAB), GitHub Releases (APK), offline hotspot sharing
- **Min SDK**: API 26 (Android 8.0)
- **Target SDK**: API 37

---

## 16. Emergency SOS Dissemination Subsystem

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             EMERGENCY SOS PIPELINE                          │
│                                                                             │
│  [User: 3s Hold] ──► [SosManager]                                           │
│                            │                                                │
│                 ┌──────────┴──────────┐                                     │
│                 ▼                     ▼                                     │
│         [Assemble Payload]    [Sign with Ed25519]                           │
│         • Category: Medical   • Output: 64B sig                             │
│         • Battery %, Geohash  • Nonce / Freshness Window                    │
│                 │                                                           │
│                 ▼                                                           │
│        [InceptionPacket: Type MESSAGE (0x02) / SOS (0x30)]                  │
│         • Recipient: BROADCAST (0xFF...FF)                                  │
│         • TTL: 7 (Maximum)                                                  │
│         • Priority: HIGH (Bypass outbox delay & rate limit)                 │
│                 │                                                           │
│                 ▼                                                           │
│         [BLE Mesh Epidemic Flood with Slotted Jitter 10-50ms]               │
│                 │                                                           │
│                 ├──► [Peer A] ──► Re-broadcast (LRU seen-cache)            │
│                 │       │                                                   │
│                 │       └──► [Receiver Alert] (Morse Haptic + Alarm)        │
│                 │                                                           │
│                 └──► [Peer B: Edge Gateway with 4G/Wi-Fi]                   │
│                         │                                                   │
│                         └──► [Bridge to Nostr NIP-01: #sos event]          │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 16.1 Propagation & Storm Mitigation

1. **Single-Packet Atomicity:** The entire SOS payload fits inside $\le 200$ bytes, avoiding L2CAP fragmentation ($P(\text{delivery}) = p$).
2. **Slotted Jitter:** Relays delay rebroadcast by a random interval $\tau \in [10\text{ ms}, 50\text{ ms}]$ to avoid phase-locked packet collisions on the 2.4 GHz radio band.
3. **Seen-Digest Ring Buffer:** Relays maintain an LRU cache of $(SenderID, Timestamp)$ hashes; duplicates are dropped immediately to prevent infinite broadcast loops.
4. **Multi-Modal Escalation:** Receiving nodes trigger `AudioAttributes.USAGE_ALARM` to bypass silent mode, vibrate in Morse pattern `... --- ...`, and display a sticky top banner.
5. **State Lifecycle:**
   - `ARMED` (3s countdown) $\to$ `ACTIVE_BROADCASTING` (repeats every 60s) $\to$ `ACKNOWLEDGED` (peer ACK received) $\to$ `RESOLVED` (signed `SOS_CANCEL` received).

---

## 17. On-Device Offline RAG & Knowledge Retrieval Subsystem

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         OFFLINE EMERGENCY RAG ENGINE                        │
│                                                                             │
│  User Query ("purify flood water")                                          │
│        │                                                                    │
│        ├──► [Query Classifier]                                              │
│        │          │                                                         │
│        │          ├── [Quick / Low-Battery Mode]                            │
│        │          │         ▼                                               │
│        │          │    [BM25 / Local Vector Search]                         │
│        │          │    • sqlite-vec or ObjectBox                            │
│        │          │    • MiniLM-L6-v2 ONNX (~25MB)                          │
│        │          │         ▼                                               │
│        │          │    [Ranked Manual Chunks] ──► Instant UI Cards          │
│        │          │                                                         │
│        │          └── [Generative Mode: Opt-In]                             │
│        │                    ▼                                               │
│        │               [Top-K Grounded Chunks]                              │
│        │                    ▼                                               │
│        │               [On-Device SLM (SmolLM2-360M / Qwen-0.5B)]           │
│        │                    ▼                                               │
│        │               [Synthesized Step-by-Step Response]                  │
│        │                                                                    │
│        └──► [Zero-Hit Fallback: "Ask the Mesh"]                             │
│                   │                                                         │
│                   ▼                                                         │
│             [Broadcast Query Packet over BLE]                               │
│                   │                                                         │
│                   ▼                                                         │
│             [Neighbor with Manual Returns Verified Chunk]                   │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 17.1 Component Roles

* **Knowledge Base:** Packaged SQLite database storing pre-indexed emergency manuals (Red Cross, FEMA, WHO) in markdown format with pre-computed chunk embeddings ($\le 25\text{ MB}$ total).
* **Embedding Service:** Local ONNX Runtime Mobile executing quantized `all-MiniLM-L6-v2` embeddings (384 dimensions) on CPU/NNAPI.
* **Vector Store:** `sqlite-vec` extension or pure SQLite FTS5 for dual hybrid search (dense semantic + sparse keyword).
* **SLM Inference Engine:** Optional MediaPipe GenAI / ExecuTorch runtime hosting a 4-bit quantized model (e.g., SmolLM2-360M, $<300\text{ MB}$ RAM footprint).
* **Battery Conservation Policy:** Generative inference is disabled when device battery is $< 20\%$ or thermal throttling is active, falling back strictly to fast BM25 snippet extraction.

---

## 18. Document & Arbitrary File Transfer Subsystem

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          DOCUMENT TRANSFER PIPELINE                         │
│                                                                             │
│  [User Picks .pdf / .docx]                                                  │
│        │                                                                    │
│        ▼                                                                    │
│  [FileSharingManager]                                                       │
│        │                                                                    │
│        ├── 1. Filename Sanitization: Strip path characters, create UUID     │
│        ├── 2. Transport Admission:                                          │
│        │      • BLE Mesh: Cap at ≤ 500 KB                                   │
│        │      • Wi-Fi Aware / Direct Socket: Up to 25 MB                    │
│        └── 3. Chunking into InceptionFilePacket:                             │
│               • TLV 0x01: FILE_NAME (UTF-8)                                 │
│               • TLV 0x02: FILE_SIZE (UInt32)                                │
│               • TLV 0x03: MIME_TYPE (UTF-8)                                 │
│               • TLV 0x04: CONTENT (Chunked bytes)                           │
│                      │                                                      │
│                      ▼                                                      │
│  [PrivateMediaTransfer: Noise Payload 0x20]                                 │
│  (ChaCha20-Poly1305 E2EE encryption)                                        │
│        │                                                                    │
│        ▼                                                                    │
│  [Mesh Fragmentation & Multi-Hop Relay]                                     │
│        │                                                                    │
│        ▼                                                                    │
│  [Receiver Pipeline]                                                        │
│        │                                                                    │
│        ├── 1. Stream to Disk in cacheDir/documents/ (Prevent JVM OOM)       │
│        ├── 2. Enforce 10 MiB Bounded Expansion Ceiling                      │
│        ├── 3. Display FileMessageItem Card with Progress & MIME Icon        │
│        └── 4. Open via FileProvider (read-only FLAG_GRANT_READ_URI)         │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 18.1 Security & Performance Isolation

1. **Host Sandboxing:** Received documents are never executed internally. Viewing is delegated to third-party Android viewer apps via `FileProvider` content URIs (`content://com.inception.android.fileprovider/...`).
2. **Path Traversal Protection:** Target filenames are sanitized with regex `[^a-zA-Z0-9._-]`, truncating basenames to 64 characters and isolating files inside `context.cacheDir/documents/`.
3. **Decompression Bomb Protection:** Bounded decoding caps expanded payload at 10 MiB, preventing zip-bomb memory exhaustion attacks.
4. **Transport Gating:** Large documents ($> 500\text{ KB}$) prompt the user to establish a Wi-Fi Aware link to prevent saturating slow BLE mesh channels.

---

## 19. Technology Stack Summary

| Category | Technology | Version |
|----------|-----------|---------|
| Language | Kotlin | 2.4.10 |
| Build | Gradle (AGP) | 9.3.1 |
| UI | Jetpack Compose (Material 3) | BOM 2026.06.01 |
| Navigation | Jetpack Navigation Compose | 2.9.8 |
| Lifecycle | AndroidX Lifecycle | 2.11.0 |
| Crypto | Bouncy Castle | 1.85 |
| Crypto | Google Tink | 1.23.0 |
| BLE | Nordic BLE | 2.11.0 |
| HTTP/WS | OkHttp | 5.4.0 |
| JSON | Gson | 2.14.0 |
| Coroutines | kotlinx-coroutines | 1.11.0 |
| HTTP Server | NanoHTTPD | 2.3.1 |
| QR | ZXing | 3.5.4 |
| Camera | CameraX | 1.6.1 |
| ML / OCR | ML Kit Barcode & Text Recognition | 17.3.0 |
| On-Device AI / RAG | ONNX Runtime Mobile / MediaPipe GenAI | 1.18.0 / 0.10.14 |
| Local Vector DB | sqlite-vec / SQLite FTS5 | Embedded |
| Background | WorkManager | 2.10.1 |
| Location | Play Services Location | 21.4.0 |
| Tor | Arti (Rust) | Custom build |
| Testing | JUnit 4 / Robolectric / Mockito | Various |