# MicRouter Framework Architecture

## 1. Core Understanding

MicRouter is not a single-function microphone switcher. It is a framework for understanding and managing Android audio routing.

The original problem (QC45 + DJI Mic Mini + WeChat) is only one entry scenario.

The underlying problem:

Android has multiple audio devices, but users lack visibility into:

- Which device is active
- Why Android selected that device
- Whether the external microphone is actually used
- How to influence routing decisions

Therefore the framework is:

```
Device Layer
      |
Audio Observation Layer
      |
Routing Decision Layer
      |
User Control Layer
      |
Diagnostic Layer
```

---

# 2. Product Framework

## Layer 1: Device Discovery

Goal:

Understand the available hardware environment.

Input:

- Bluetooth devices
- USB audio devices
- Built-in microphones
- Speakers
- Headsets

Output:

A unified device model.

Example:

```
Device
├── name
├── type
├── connection
├── input capability
├── output capability
└── communication capability
```

---

# Layer 2: Audio State Observation

Goal:

Make invisible Android audio behavior visible.

Functions:

- Current communication device
- Active input device
- Active output device
- Route changes
- Connection events

Equivalent concept:

A network monitoring dashboard, but for audio.

---

# Layer 3: Routing Control

Goal:

Provide user-level control within Android official API boundaries.

Capabilities:

- Select preferred communication device
- Test routing changes
- Restore preferred profile

Constraints:

- No root
- No Magisk
- No modifying third-party applications

---

# Layer 4: Audio Diagnostics

Goal:

Help users debug problems.

Functions:

## Microphone Test

Measure:

- Input availability
- Signal level
- Response quality

## Route Timeline

Example:

```
10:01 DJI Mic connected
10:02 QC45 connected
10:05 WeChat call started
10:05 Communication device changed to QC45
```

## Export Report

Generate a diagnostic package for troubleshooting.

---

# 3. User Personas

## Creator

Uses:

- DJI Mic
- Rode Wireless
- Android camera apps

Need:

Reliable external microphone selection.

---

## Business User

Uses:

- Bluetooth headset
- Conference apps
- External microphones

Need:

Understand call audio problems quickly.

---

## Audio Enthusiast

Need:

Professional visibility into Android audio stack.

---

# 4. Information Architecture

```
MicRouter

Dashboard
 |
 |-- Devices
 |     |-- Input devices
 |     |-- Output devices
 |
 |-- Inspector
 |     |-- Current route
 |     |-- Timeline
 |
 |-- Test Lab
 |     |-- Mic meter
 |     |-- Speaker test
 |
 |-- Control
 |     |-- Communication device selection
 |
 |-- Reports
       |-- Export diagnostics
```

---

# 5. Design Philosophy

## Visibility Before Automation

First answer:

"What is Android doing?"

Then:

"How can we improve it?"

---

## Professional Tool, Consumer UX

The backend can understand AudioPolicy and HAL concepts.

The user only needs:

- Current status
- Problem explanation
- Available actions

---

# 6. Product Evolution Roadmap

## MVP

Audio Route Inspector.

Goal:

Understand Android routing.

---

## V1

Audio Router.

Goal:

Control supported routing scenarios.

---

## V2

Android Audio Compatibility Platform.

Goal:

Build a database of:

- Devices
- Phones
- Android versions
- Routing behavior

---

# 7. Strategic Position

MicRouter should be viewed as:

"A control plane for Android audio devices."

Not merely:

"A microphone switching app."
