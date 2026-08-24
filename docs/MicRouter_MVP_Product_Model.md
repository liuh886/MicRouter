# MicRouter MVP Product Model

## Product Name

MicRouter

## Product Vision

Make Android professional audio devices behave like a desktop audio workstation.

Users should be able to understand:

- Which microphone is active
- Which speaker is active
- Why Android selected this route
- How to switch when possible

## Target Users

Primary:

- DJI Mic Mini users
- Rode Wireless users
- Android video creators
- Mobile meeting users
- Podcast users

Initial problem:

"My wireless microphone works in recording apps but disappears during calls."

## MVP User Flow

### Step 1: Device Discovery

User opens MicRouter.

Display:

Input:

- Built-in microphone
- Bluetooth headset microphone
- DJI Mic Mini receiver
- USB audio device

Output:

- Speaker
- Bluetooth headset

### Step 2: Diagnosis

Show:

```
Current mode:
Communication

Input:
Bose QC45 microphone

Output:
Bose QC45

Available external microphone:
DJI Mic Mini
```

### Step 3: Test

User presses microphone test.

Features:

- Live audio level meter
- RMS volume
- Device name
- Sample rate
- Channel information

Purpose:

Allow field debugging without another person testing calls.

### Step 4: Switch

Where Android allows:

User selects preferred microphone.

Example:

```
Input priority:

1. DJI Mic Mini
2. USB microphone
3. Phone microphone
4. Bluetooth microphone
```

## Core Screens

## Home

```
Audio Status

Input
DJI Mic Mini ✓

Output
QC45 ✓

Mode
Communication

[ Diagnose ]
[ Test Mic ]
```

## Diagnostic Screen

Display:

- Audio mode
- Active communication device
- Available devices
- Bluetooth SCO state
- Permission status

## MVP Success Criteria

Technical:

- Correctly identify DJI Mic Mini
- Correctly identify QC45 microphone
- Provide real-time monitoring
- Log routing changes

User:

- User can immediately know why microphone is not used
- User can manually switch when Android API permits

## Non Goals

MVP does not include:

- Root
- Audio HAL modification
- Virtual microphone driver
- AI processing
- Noise cancellation

## Future Expansion

- Device compatibility database
- Xiaomi / Samsung / Pixel audio behavior matrix
- Creator mode presets
- Camera application integration
