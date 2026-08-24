# MicRouter Development Design

## 1. Project Definition

MicRouter is an Android system audio input/output routing assistant. The goal is not to modify third-party applications such as WeChat, but to provide users with visibility and control over Android audio device selection.

Primary scenario:

- Xiaomi 17 Pro Max / HyperOS
- Bose QC45 used as Bluetooth output device
- DJI Mic Mini used as external microphone input
- User expects: QC45 for playback + DJI Mic Mini for microphone

Current observation:

- DJI Mic Mini works in Android system recording applications.
- During WeChat calls, Bluetooth headset microphone may still be selected.

## 2. Technical Hypothesis

Android has multiple audio layers:

```
Application
    |
Audio Framework
    |
Audio Policy Manager
    |
Audio HAL
    |
Hardware Device
```

Different audio use cases select different routing policies.

The key difference:

- Recorder apps usually create AudioRecord with normal input source.
- Communication apps use AudioManager.MODE_IN_COMMUNICATION and Bluetooth SCO routing rules.

Therefore MicRouter should first solve device visibility, routing diagnosis and supported routing control before attempting aggressive switching.

## 3. Research Direction

### Android APIs

Main APIs to investigate:

- AudioManager
- AudioDeviceInfo
- AudioRecord
- AudioRouting
- BluetoothProfile
- CommunicationDevice API (Android 12+)

Important capabilities:

- Enumerate input devices
- Detect current communication device
- Request preferred communication device where supported
- Monitor routing changes

## 4. GitHub Research Targets

The project should study open-source Android audio projects around:

### Audio routing

Focus:

- AudioManager routing examples
- AudioDeviceInfo enumeration
- Communication device switching

### USB audio

Focus:

- USB Audio Class detection
- External microphone identification
- AudioRecord with USB input

### Bluetooth SCO

Focus:

- SCO lifecycle
- Bluetooth headset profile behavior
- Communication mode routing

The project should avoid root-only approaches in MVP.

## 5. MVP Technical Architecture

```
MicRouter APK

├── Device Scanner
│   ├── Input devices
│   ├── Output devices
│   └── Device capabilities
│
├── Routing Monitor
│   ├── Current communication device
│   ├── Audio mode
│   ├── Bluetooth SCO state
│   └── Route changes
│
├── Routing Controller
│   ├── Preferred communication device API
│   └── User manual switching
│
└── Diagnostic Recorder
    ├── Record from selected device
    ├── Volume meter
    └── Waveform display
```

## 6. Important Product Decision

Do not promise:

"Force every app to use DJI Mic Mini"

Promise:

"Understand and control Android audio routing when the system allows it."

## 7. Development Milestones

### Phase 1

- Kotlin Android project
- Device enumeration
- Show all microphones and speakers
- Show active route

### Phase 2

- Routing monitor
- Real-time microphone level meter
- Bluetooth SCO diagnostics

### Phase 3

- Communication device switching
- Xiaomi HyperOS compatibility testing
- DJI Mic Mini profile testing

### Phase 4

- Publish technical findings
- Expand device compatibility matrix
