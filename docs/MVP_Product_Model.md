# MicRouter MVP Product Model

## Product Vision

MicRouter is an Android professional audio routing inspector and controller.

It solves the problem that Android users cannot understand or control which microphone is selected when multiple audio devices are connected.

Target users:

- Wireless microphone users
- Mobile creators
- Online meeting users
- Podcast users
- Professional Android users

## MVP Version: Audio Route Inspector

The first version focuses on visibility and diagnostics before control.

## Core Features

### 1. Device Dashboard

Show:

- Current audio mode
- Current communication device
- Available input devices
- Available output devices

Example:

```
Communication Device:
Bose QC45 Hands Free

Available Inputs:
✓ DJI Mic Mini Receiver
✓ Built-in Microphone
✓ Bluetooth SCO Mic
```

### 2. Route Event Monitor

Real-time timeline:

```
22:10:01 DJI Mic connected
22:10:05 QC45 SCO activated
22:10:08 Communication device changed
```

Purpose:

Allow field debugging during calls.

### 3. Microphone Level Monitor

Measure input signal level for each accessible source.

Purpose:

Verify whether DJI Mic is actually receiving audio.

### 4. Communication Device Controller

Experimental feature:

Provide manual switching:

- DJI Mic Mini
- Bluetooth headset
- Phone microphone

Only use official Android APIs.

## Technical Architecture

```
MicRouter APK

├── DeviceScanner
│   ├── AudioManager
│   ├── AudioDeviceCallback
│   └── DeviceRepository
│
├── RouteMonitor
│   ├── Communication State
│   └── Event Logger
│
├── Audio Inspector
│   └── Input Level Analyzer
│
└── UI
    ├── Dashboard
    ├── Device Detail
    └── Debug Log
```

## Development Roadmap

### Phase 1

Device discovery.

Deliverable:

List all microphones and audio devices.

### Phase 2

Route monitoring.

Deliverable:

Understand Xiaomi + DJI + QC45 behavior.

### Phase 3

Communication device switching.

Deliverable:

Test setCommunicationDevice compatibility.

### Phase 4

Device compatibility database.

Deliverable:

Record Android vendor behavior.

## MVP Acceptance Test

Hardware:

- Xiaomi 17 Pro Max
- Bose QC45
- DJI Mic Mini

Tests:

1. Connect QC45
2. Connect DJI Mic Mini
3. Start WeChat call
4. Record route events
5. Check active communication device
6. Try manual routing

Expected result:

Clear understanding of why DJI Mic is or is not selected.
