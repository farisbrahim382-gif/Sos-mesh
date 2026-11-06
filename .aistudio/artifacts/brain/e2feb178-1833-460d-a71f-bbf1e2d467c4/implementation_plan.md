# SOS MESH — Off-Grid Emergency Communication System

An offline-first, decentralized tactical emergency communication and mesh networking Android application built with Jetpack Compose, Room database, and a multi-transport engine (BLE LoRa ESP32/Meshtastic, Wi-Fi Direct, and an integrated 3-node virtual testbench). SOS MESH enables reliable peer-to-peer messaging, emergency broadcast alerts, store-and-forward routing, and offline location sharing without cellular coverage or internet.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The architectural direction and foundational choices below were confirmed based on the initial technical interview:

- **Confirmed Decision 1 (Hardware Protocol)**: Dual protocol support in the Hardware Abstraction Layer (HAL) for both **Meshtastic BLE protocol** and **custom ESP32 LoRa framed binary protocol** (SX1262/SX1276/SX1268 transceivers).
- **Confirmed Decision 2 (Verification Environment)**: Integrated **3-Node Virtual Mesh Testbench** (Node A: Originator, Node B: Intermediate Relay, Node C: Recipient) running inside the app alongside physical BLE/Wi-Fi Direct engines, providing immediate verification of multi-hop routing, packet deduplication, and SOS prioritization even in emulator environments.
- **Confirmed Decision 3 (Frequency Plan)**: Initial default regional frequency configured to **US915 (902–928 MHz)**, with seamless runtime switching to **EU868 (863–870 MHz)**, **AS923 (920–925 MHz)**, and **433 MHz ISM**.
- **Confirmed Decision 4 (Bilingual Support)**: Tactical high-contrast interface with native **English and Arabic (العربية)** localization and full RTL/LTR layout mirroring.

---

## 1. Overview & Core Concept

### What It Does
SOS MESH turns any Android device into an off-grid resilient communication terminal. When terrestrial cell towers fail or natural disasters strike, SOS MESH forms ad-hoc wireless mesh links using:
1. Direct device-to-device Bluetooth Low Energy (BLE) and Wi-Fi Direct.
2. Long-range external LoRa transceivers (ESP32 with SX1262/SX1276/SX1268 chips or standard Meshtastic hardware) linked via Bluetooth.
3. An automatic multi-hop store-and-forward engine that relays packets across intermediate nodes until they reach their destination or broadcast radius.

### Target Audience & Persona
- **First Responders & Search and Rescue (SAR)**: Teams requiring real-time situational awareness, emergency beacons, and team coordination without relying on civilian cellular towers.
- **Disaster Survivors & Civilians**: Individuals trapped in flood, earthquake, or power blackout scenarios needing to broadcast distress signals (medical, fire, trapped) with battery and coordinate telemetry.
- **Remote Expeditions & Off-Grid Travelers**: Hikers, field scientists, and remote crews outside telecom coverage areas.

### Key Value Delivered
- **Zero-Infrastructure Reliability**: Messages hop between nodes autonomously without internet, cell towers, or centralized servers.
- **True SOS Prioritization**: Emergency packets bypass normal message queues, preempt local channel traffic, and carry verified battery, severity, and sensor/manual coordinate telemetry.
- **Extensible Hardware Abstraction**: Plug-and-play pairing with affordable ESP32 LoRa boards or Meshtastic hardware.

---

## 2. User Experience & Visual Design

### Key User Flows

```
┌────────────────────────────────────────────────────────────────────────┐
│                        CORE USER INTERACTION FLOWS                     │
├──────────────────┬──────────────────┬─────────────────┬────────────────┤
│ 1. SOS Broadcast │ 2. Mesh Chat     │ 3. Node Radar   │ 4. Hardware    │
│    Flow          │    & Groups      │    & Topology   │    Manager     │
├──────────────────┼──────────────────┼─────────────────┼────────────────┤
│ • Tap SOS Button │ • Select channel │ • View active   │ • Scan BLE     │
│ • Select Hazard  │   (Public/Group) │   mesh topology │ • Pair ESP32   │
│   (Medical/Fire) │ • Compose with   │ • Hop-distance  │ • Set Region   │
│ • 3-sec cancel   │   priority tag   │   visualization │   (US915/EU868)│
│   countdown      │ • Store-&-fwd    │ • Signal RSSI/  │ • Telemetry &  │
│ • Continuous     │   queue status   │   SNR metrics   │   Battery sync │
│   beacon pulse   │ • E2E encrypted  │ • Manual/GPS pos│ • Testbench sim│
└──────────────────┴──────────────────┴─────────────────┴────────────────┘
```

1. **Emergency SOS Flow**:
   - High-visibility red pulsating SOS trigger card on the Dashboard and persistent emergency FAB.
   - Hold-to-activate (or 3-second safety countdown) preventing accidental triggers.
   - Category selector: Medical, Fire, Lost/Trapped, Accident, Natural Disaster, Security, General Aid.
   - Automatic snapshot of device telemetry: Battery %, coordinate state (Exact GPS / Estimated / Manual / Unknown), timestamp.
   - Instant mesh-wide broadcast with `Priority = CRITICAL`, TTL = 7 hops, store-and-forward queuing, and visual acknowledgment tracking when neighbor nodes relay the alert.

2. **Tactical Mesh Dashboard**:
   - Top Status Bar: Active Node ID, Mesh Status (Online/Offline/Relay Mode), Connected Radio (ESP32 SX1262 / Meshtastic / BLE Local), Battery level, Active Node Count.
   - Live Emergency Alert Banner: Displays incoming SOS alerts with one-tap focus, tone/vibrate alerting, and sender distance.
   - Quick Action Tiles: "Broadcast SOS", "Direct Comms", "Node Radar", "Radio Hardware".
   - Channel Activity Stream: Live packet logs and recent channel messages.

3. **Secure Messaging & Channel Coordination**:
   - Channels: `#emergency-broadcast` (public all-mesh), `#medical-response`, `#team-alpha`, plus 1-on-1 private encrypted direct chats.
   - Message bubble metadata: Hop count (`Hops: 2`), Transport tag (`LoRa 915MHz` / `BLE` / `Wi-Fi Direct`), delivery status (`Queued` -> `Transmitted` -> `Relayed` -> `ACK Received`).
   - Offline message composer with auto-retry and queue management.

4. **Mesh Radar & Node Map**:
   - Visual radar/topology graph showing discovered nodes, signal quality (RSSI/SNR), hop distances, and last-seen timestamps.
   - Coordinate viewer supporting manual pin placement, offline coordinate entry, or hardware GPS fixes without relying on online map tile servers.

5. **Radio Hardware & Virtual Testbench**:
   - BLE device scanner for ESP32 and Meshtastic nodes.
   - Radio parameter configuration: Frequency Band (US915 / EU868 / AS923 / 433MHz), Bandwidth (125 / 250 / 500 kHz), Spreading Factor (SF7 - SF12), Coding Rate (4/5 - 4/8), Tx Power (up to 22 dBm).
   - In-app 3-Node Virtual Testbench controller: Toggle Node A (Origin), Node B (Relay), Node C (Target), inject simulated packet loss, and observe multi-hop routing in real time.

### Visual Identity & Theme

- **Aesthetic Direction**: **Tactical Military / High-Contrast Emergency HUD**. Pure OLED black surfaces with razor-sharp neon indicators, clean geometric typography, and zero extraneous fluff.
- **Color Palette**:
  - `Background`: `#0B0F14` (Deep Tactical Charcoal)
  - `Surface`: `#131A22` (Matte Armor Plate)
  - `SurfaceVariant`: `#1C2633` (Secondary Elevation)
  - `Primary / Emergency Crimson`: `#FF3B30` (Vivid Emergency Alert)
  - `Secondary / Mesh Cyan`: `#00E5FF` (Active Mesh Links & Packets)
  - `Warning / Signal Amber`: `#FF9F0A` (Radio Diagnostics & Retries)
  - `Success / Secure Green`: `#30D158` (Connected Nodes & ACK confirmations)
  - `TextPrimary`: `#F2F5F8` (Crisp High-Contrast White)
  - `TextSecondary`: `#8A99A8` (Tactical Muted Gray)
- **Typography**: Monospace and condensed sans-serif pairing for metrics, telemetry, and node IDs (`Roboto Mono` / `Chakra Petch` style telemetry readouts).
- **Localization**: Full English and Arabic string tables (`res/values/strings.xml` and `res/values-ar/strings.xml`) with proper RTL layout mirroring.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Multi-Transport Architecture with Unified Routing Engine
- **Chosen Approach**: A unified `MeshTransport` interface implemented by `BleDirectTransport`, `WiFiDirectTransport`, `Esp32LoraBleTransport`, `MeshtasticBleTransport`, and `VirtualSimulationTransport`.
- **Why**: Allows the core routing, packet deduplication, store-and-forward, and cryptographic security layers to remain transport-agnostic while exploiting whatever physical link is active.
- **Alternatives Considered**: Direct tight coupling to Meshtastic only (rejected because it prevents custom low-cost ESP32 SX1262 modules and on-device Bluetooth/Wi-Fi direct operation).

### Decision 2: Offline-First Persistence via Room
- **Chosen Approach**: Room Database storing Nodes, Messages, Emergency Alerts, Pending Packets, and Radio Settings.
- **Why**: Guarantees zero data loss across device reboots or battery exhaustions. Outgoing packets remain in the database queue and resume forwarding automatically when radio links reconnect.

### Decision 3: Integrated 3-Node Virtual Testbench
- **Chosen Approach**: Built-in interactive testbench that instantiates virtual node instances inside the app to simulate 3-hop message routing, packet drop rates, and SOS preemption.
- **Why**: Crucial for developers and users to verify and demonstrate multi-hop mesh forwarding on any Android device or emulator without needing multiple physical ESP32 transceivers.

### Decision 4: Deterministic Packet Deduplication & Anti-Replay
- **Chosen Approach**: 64-bit Packet Hash / UUID cache with sliding Bloom-style window and monotonically increasing sequence counters per node.
- **Why**: Prevents broadcast storms and infinite routing loops in multi-hop flood routing.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER (JETPACK COMPOSE)            │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐   │
│  │ Dashboard    │ │ SOS Alert    │ │ Messages /   │ │ Node Radar   │   │
│  │ Screen       │ │ Screen       │ │ Channels     │ │ & Topology   │   │
│  └───────┬──────┘ └───────┬──────┘ └───────┬──────┘ └───────┬──────┘   │
│          │                │                │                │          │
│  ┌───────┴────────────────┴────────────────┴────────────────┴──────┐   │
│  │                    HARDWARE & DIAGNOSTICS VIEW                  │   │
│  │   ESP32 BLE Config • Regional Band Picker • 3-Node Testbench    │   │
│  └─────────────────────────────────┬───────────────────────────────┘   │
└────────────────────────────────────┼───────────────────────────────────┘
                                     │ StateFlow & UI Events
┌────────────────────────────────────▼───────────────────────────────────┐
│                       VIEWMODEL & APPLICATION STATE                    │
│      MainViewModel • SosViewModel • MessageViewModel • RadioViewModel  │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ Domain Operations
┌────────────────────────────────────▼───────────────────────────────────┐
│                     DECENTRALIZED MESH ENGINE LAYER                    │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ Packet Dispatcher & Deduplication Engine (TTL, Seq, PacketHash) │   │
│  ├─────────────────────────────────────────────────────────────────┤   │
│  │ Priority Router (CRITICAL SOS Preemption > Chat > Heartbeats)   │   │
│  ├─────────────────────────────────────────────────────────────────┤   │
│  │ Store-and-Forward Queue & ACK Manager with Exponential Backoff   │   │
│  ├─────────────────────────────────────────────────────────────────┤   │
│  │ Cryptographic Engine (AES-256-GCM Payload Encryption & HMAC)   │   │
│  └─────────────────────────────────┬───────────────────────────────┘   │
└────────────────────────────────────┼───────────────────────────────────┘
                                     │ Reactive Repositories
┌────────────────────────────────────▼───────────────────────────────────┐
│              DATA ACCESS & HARDWARE ABSTRACTION LAYER (HAL)            │
│  ┌───────────────────────────────┐   ┌──────────────────────────────┐  │
│  │      ROOM LOCAL DATABASE      │   │   UNIFIED TRANSPORT ENGINE   │  │
│  │ • MessagesDao                 │   │ • Esp32BleTransport (SX1262) │  │
│  │ • MeshNodesDao                │   │ • MeshtasticBleTransport     │  │
│  │ • EmergencyAlertsDao          │   │ • BleDirectTransport         │  │
│  │ • OutgoingQueueDao            │   │ • WiFiDirectTransport        │  │
│  │ • RadioSettingsDao            │   │ • VirtualTestbenchTransport  │  │
│  └───────────────────────────────┘   └──────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

### Core Data Entities

1. **`MeshNodeEntity`**:
   - `nodeId: String` (e.g. `"NODE-7F8A"`)
   - `alias: String`, `hwModel: String`, `batteryLevel: Int`
   - `lastSeenTimestamp: Long`, `hopCount: Int`, `snr: Float`, `rssi: Int`
   - `latitude: Double?`, `longitude: Double?`, `locationSource: String`
   - `isDirectNeighbor: Boolean`, `publicKey: String?`

2. **`MeshMessageEntity`**:
   - `messageId: String` (UUID)
   - `senderNodeId: String`, `recipientNodeId: String` (`"BROADCAST"` or target ID)
   - `channelName: String`, `content: String`, `priority: Int` (0=Normal, 1=High, 2=SOS)
   - `hopLimit: Int`, `hopsTraveled: Int`, `timestamp: Long`
   - `deliveryStatus: String` (`QUEUED`, `TRANSMITTED`, `RELAYED`, `DELIVERED`, `FAILED`)
   - `transportType: String` (`LORA`, `BLE_DIRECT`, `WIFI_DIRECT`, `VIRTUAL`)

3. **`EmergencyAlertEntity`**:
   - `alertId: String`
   - `originNodeId: String`, `emergencyType: String` (Medical, Fire, Lost, Accident, etc.)
   - `severity: String` (`CRITICAL`, `URGENT`, `ADVISORY`)
   - `batteryPercent: Int`, `timestamp: Long`
   - `locationCoordinates: String?`, `locationType: String` (Exact GPS, Manual, Estimated, Unknown)
   - `acknowledgedByNodeIds: String` (comma-separated list)
   - `isActive: Boolean`

4. **`RadioConfigEntity`**:
   - `activeTransport: String` (ESP32_LORA, MESHTASTIC, BLE_DIRECT, VIRTUAL)
   - `regionFrequency: String` (`US915`, `EU868`, `AS923`, `ISM433`)
   - `frequencyHz: Long`, `bandwidthKhz: Int`, `spreadingFactor: Int`, `txPowerDbm: Int`
   - `bleDeviceAddress: String?`, `bleDeviceName: String?`

---

## 5. Development Roadmap & Execution Phases

- **Phase 1: Project Foundation & Architecture**
  - Configure Room database, entities, DAOs, and repository layer.
  - Set up navigation with deep-linked screens, M3 tactical color tokens, and English/Arabic string resources.
  - Setup core models and testbed infrastructure.
- **Phase 2: Decentralized Mesh Engine & Packet Framing**
  - Implement binary packet framing (Header: Magic, PacketId, Sender, Recipient, Type, Priority, TTL, HopCount, PayloadLength, CRC/HMAC).
  - Implement packet deduplication cache, flood routing, store-and-forward queue, and priority sorting.
- **Phase 3: Hardware Abstraction Layer (HAL)**
  - Implement `Esp32LoraBleTransport` supporting custom ESP32 + SX1262 binary frame protocol.
  - Implement `MeshtasticBleTransport` skeleton for Meshtastic device interop.
  - Implement `VirtualSimulationTransport` for multi-node simulation.
- **Phase 4: Emergency SOS Engine**
  - Build SOS emergency controller with safety confirmation, telemetry capture, and high-priority packet broadcast.
  - Implement real-time audio/haptic pulse alert notification system.
- **Phase 5: Tactical Compose UI & Visualization**
  - Build Dashboard HUD with quick actions, active alerts banner, and telemetry meters.
  - Build SOS Emergency Screen with hazard categorizer and countdown.
  - Build Channels & Messaging screen with live delivery trackers.
  - Build Node Radar / Topology screen with distance & link quality meters.
  - Build Radio Hardware & Virtual 3-Node Testbench screen for live multi-hop demonstrations.
- **Phase 6: Verification & Compilation**
  - Verify Gradle build and compile cleanly.
  - Validate Robolectric unit tests for packet routing, deduplication, and SOS preemption.

---

## User Review Required

Please review this implementation plan. Click **Proceed** to approve this plan and begin Phase 1 execution.
