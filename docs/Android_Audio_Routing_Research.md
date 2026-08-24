# MicRouter Android Audio Routing Research

## Goal

Build a non-root Android audio routing inspector and controller focused on external microphones such as DJI Mic Mini.

The target is not to modify WeChat or inject into other applications. The target is to understand and control Android system communication audio routing through official APIs.

## Problem Statement

Observed scenario:

- Xiaomi 17 Pro Max
- Bose QC45 connected as Bluetooth headset
- DJI Mic Mini connected as external microphone
- System recorder can use DJI Mic Mini
- WeChat calls still appear to use QC45 microphone

Hypothesis:

Android has different routing policies for recording and communication modes.

Recording path:

```
AudioRecord
   -> selected input device
   -> DJI Mic Mini
```

Communication path:

```
MODE_IN_COMMUNICATION
   -> Audio Policy Manager
   -> Bluetooth SCO priority
   -> QC45 microphone
```

## Technical Investigation Areas

### 1. Audio Device Discovery

Use:

- AudioManager.getDevices()
- AudioDeviceCallback
- AudioDeviceInfo

Collect:

- device type
- product name
- address
- input/output capability

### 2. Communication Device API

Android 12+

Investigate:

- AudioManager.getAvailableCommunicationDevices()
- AudioManager.communicationDevice
- AudioManager.setCommunicationDevice()

Question:

Can third-party apps switch Xiaomi HyperOS communication input without root?

### 3. Real-time Route Monitoring

Implement Audio Route Inspector:

Display:

- current communication device
- available input devices
- route changes
- microphone level
- event timeline

### 4. AOSP Audio Policy Research

Study:

frameworks/av/services/audiopolicy

Key components:

- AudioPolicyManager
- Engine
- Device selection rules

Purpose:

Understand why Bluetooth SCO wins over USB microphone.

## Non-goals

- No root solution
- No Magisk module
- No modifying WeChat
- No AI features

## MVP Success Criteria

1. Detect DJI Mic Mini correctly
2. Show actual communication route
3. Monitor route changes during WeChat calls
4. Attempt system communication device switching
5. Export diagnostic logs
