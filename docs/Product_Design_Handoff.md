# MicRouter Product Design Handoff

## 1. Product Vision

MicRouter is an Android professional audio routing inspector and controller.

Core problem:

Users connect high-quality external microphones (DJI Mic Mini, Rode Wireless, USB microphones) but Android communication scenarios often select the wrong input device. The user cannot understand or control why a Bluetooth headset microphone is used instead of the external microphone.

Product promise:

> Make Android audio routing visible, understandable, and controllable.

Non-goals:

- No root
- No Magisk
- No modification of WeChat or other apps
- No AI processing

---

## 2. Primary User Scenario

### Scenario A: WeChat call with QC45 + DJI Mic Mini

Hardware:

- Xiaomi 17 Pro Max
- Bose QC45
- DJI Mic Mini

Expected:

Input: DJI Mic Mini

Output: QC45

Actual problem:

Input: QC45 microphone

Result:

Remote participant hears low volume.

---

## 3. Product Positioning

Category:

Android Audio Routing Utility

Comparable concepts:

- Network diagnostic tools
- Bluetooth device managers
- Professional camera monitoring tools

MicRouter is not a recorder. It is an observability and control layer.

---

## 4. Competitive Reference

### Twilio AudioSwitch

Reference value:

- Device discovery
- Communication audio management
- Device selection model

MicRouter should learn its abstraction of available devices and active device state.

### Android AOSP Audio Routing

Reference value:

- AudioDeviceInfo
- AudioDeviceCallback
- Communication device APIs

Android provides official routing mechanisms but users lack visibility.

### Existing audio switch apps

Reference value:

- Fast switching interaction
- Simple controls

Gap:

Most products focus on output switching, not professional microphone diagnostics.

---

## 5. Core User Journey

## First Launch

User opens MicRouter.

The app immediately shows:

- Connected devices
- Current communication device
- Input/output status

No complicated setup.

---

## Dashboard

Main screen:

```
Current Communication Device

Bose QC45

Input Devices

✓ DJI Mic Mini
✓ QC45 Hands Free
✓ Phone Microphone

Output Devices

✓ QC45 Stereo
✓ Speaker
```

---

## Route Inspector

Purpose:

Help users answer:

"Which microphone is actually being used?"

Display:

- Current route
- Device type
- Connection status
- Timestamped events

Example:

```
10:20 DJI Mic connected
10:21 WeChat call started
10:21 Communication device changed
10:21 QC45 selected
```

---

## Microphone Monitor

Purpose:

Validate microphone availability.

Features:

- Input level meter
- Device comparison
- Recording test

Example:

```
DJI Mic Mini
████████ 80%

QC45 Mic
██ 20%
```

---

## Manual Routing

Advanced feature:

Allow users to select preferred communication device.

Example:

```
Set communication device:

○ QC45
● DJI Mic Mini
○ Phone Mic
```

---

## 6. Information Architecture

```
MicRouter
│
├── Dashboard
│
├── Devices
│   ├── Input devices
│   └── Output devices
│
├── Inspector
│   ├── Current route
│   └── Event logs
│
├── Test
│   └── Microphone level
│
└── Settings
    └── Export diagnostics
```

---

## 7. Design Principles

### Principle 1: Visibility before control

First solve:

"What happened?"

Then:

"How can I change it?"

### Principle 2: Professional but simple

Target users understand hardware but do not understand Android AudioPolicy internals.

### Principle 3: Evidence-driven debugging

Every routing change should produce observable evidence.

---

## 8. MVP Definition

MVP success:

User can:

1. See DJI Mic Mini detected
2. See QC45 detected
3. Identify active communication device
4. Observe route changes during WeChat calls
5. Attempt manual communication device switching
6. Export diagnostic logs

---

## 9. Future Direction

Potential expansion:

- Wireless microphone compatibility database
- Creator mode
- Podcast mode
- Camera recording assistant
- Android audio compatibility testing platform

Long-term vision:

Become the "Bluetooth and external microphone control center" for Android creators.
