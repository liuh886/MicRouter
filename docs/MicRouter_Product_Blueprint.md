# MicRouter Product Blueprint

Status: Authoritative product document (supersedes MVP_Product_Model.md and MicRouter_MVP_Product_Model.md)
Last updated: 2026-08-24
Scope: Problem argumentation, competitive analysis, technical feasibility verdicts, layered product blueprint, success criteria.

Companion documents:

- MicRouter_Framework_Architecture.md — system layering and information architecture
- Android_Audio_Routing_Research.md — API investigation details
- Phase_Completion_Report.md — engineering progress log
- Product_Design_Handoff.md — original design handoff record (historical)

---

# 1. Product Thesis

MicRouter is an Android audio routing inspector and controller for creators who use professional external microphones.

Product promise:

> Make Android audio routing visible, understandable, and controllable — where the platform permits it.

The strategic framing is deliberately honest: MicRouter is an observability-first tool with conditional control, not a routing override hack. Every capability claim in this document is backed by a public API or explicitly marked as impossible.

Why now:

1. Wireless microphones (DJI Mic Mini, Rode Wireless GO, Hollyland Lark) have become mainstream creator gear, and their phone connection path (USB-C receiver or Bluetooth) collides with the phone's own Bluetooth audio arbitration.
2. VoIP applications are migrating to telecom-managed system calls on modern Android, changing routing arbitration in ways users cannot see.
3. No existing product serves the combination of external professional microphones, communication scenarios (calls, live streaming), and observability-first design. See Section 4.

---

# 2. Problem Argumentation

## 2.1 Primary Failure Scenario

Hardware: Xiaomi 17 Pro Max (HyperOS) + Bose QC45 (Bluetooth headset) + DJI Mic Mini (USB-C receiver).

Expected behavior:

- Output: Bose QC45
- Input: DJI Mic Mini

Observed behavior:

- System recorder apps correctly use DJI Mic Mini as input.
- During WeChat calls, input switches to the QC45 hands-free microphone.
- Remote participants report low volume; the DJI Mic's signal advantage is lost exactly when it matters most.

## 2.2 Community Evidence

This is not an isolated device quirk. Long-running community threads document the same class of failure across brands and years:

- V2EX threads (e.g. topic 535092) show widespread confusion: Bluetooth headsets play media via A2DP but fail to capture input in WeChat voice messages and calls, because WeChat uses different audio interfaces per scenario. Users resort to folklore workarounds such as disabling "Media audio" for the headset.
- The same thread shows behavior varies across OEMs (Xiaomi, Huawei, OnePlus, Samsung), Android versions, and Bluetooth codec generations — evidence that the problem is structural, not a single bug.

Implication: a diagnostic tool that explains "which profile is active and why" has durable demand independent of any single fix.

## 2.3 Vendor Evidence

DJI publishes an official third-party app compatibility matrix for Mic Mini (Bluetooth direct connection mode). It shows per-app inconsistency on Android: some apps capture from the transmitter, others do not (e.g. several live-streaming apps marked unsupported while others work).

Implication: even the accessory vendor cannot guarantee routing behavior, because the decision belongs to each application's audio mode plus the OS routing policy. This validates MicRouter's positioning as the missing visibility layer between vendors' compatibility lists and actual runtime behavior.

## 2.4 Root Cause Chain

The failure follows from two distinct routing policies inside Android:

```
Recording path (works):
AudioRecord (MEDIA / UNPROCESSED source)
    -> AudioPolicyManager normal-input rules
    -> highest-priority active input = USB microphone
    -> DJI Mic Mini ✓

Communication path (fails):
App sets MODE_IN_COMMUNICATION
    -> AudioPolicyManager communication routing
    -> Bluetooth SCO / LE Audio priority over USB
    -> QC45 hands-free mic ✗
```

Key mechanisms confirmed by AOSP sources (`AudioDeviceBroker`, `CallAudioCommunicationDeviceTracker`) and API documentation:

1. Communication routing is arbitration-based: the most recent request wins ("last writer wins").
2. Since Android 14, legacy switches (`setSpeakerphoneOn`, `startBluetoothSco`) are deprecated in favor of `setCommunicationDevice()` / `clearCommunicationDevice()` — the official control surface MicRouter targets.
3. On telecom-managed calls (`MODE_IN_CALL` set by `com.android.server.telecom`), a third-party `setCommunicationDevice()` may return `true` yet be silently outranked by the telecom stack, with no callback. This is verified field behavior documented by the clear-mic-router project on recent Pixel builds.
4. Some OEM telephony stacks reject third-party communication-device selection outright for cellular calls.

Conclusion: full third-party control of other applications' call routing is not achievable without privileged access. MicRouter therefore competes on visibility, diagnosis, best-effort control, and honest reporting of what the platform refused — never on fake guarantees.

---

# 3. Design Principles (Carried Forward)

1. Visibility before control. First answer "what is Android doing?", then offer actions.
2. Professional backend, consumer frontend. The engine understands AudioPolicy concepts; the UI shows status, explanation, and available actions only.
3. Evidence-driven debugging. Every routing change produces timestamped observable evidence the user can export.
4. Honest capability reporting. When the OS ignores a routing request, say so explicitly instead of showing false success.

---

# 4. Competitive Landscape and Borrowed Lessons

| Product | Category | What it proves | Lesson borrowed |
|---|---|---|---|
| SoundAbout (~3.7M downloads) | Global routing switcher | Massive latent demand for manual routing | Global hard toggles break across OS versions; avoid deprecated APIs |
| Lesser AudioSwitch | Input/output force-switch | Same demand, but non-functional on Android 11+ | Cautionary tale: never build on deprecated routing APIs; state platform limits in-product |
| Samsung Sound Assistant | Per-app output switching (Galaxy-only) | OEMs absorb generic switching features | Differentiate on external-mic diagnostics + cross-vendor coverage, which OEM tools ignore |
| Twilio AudioSwitch (library) | In-call device selection SDK | Clean abstraction of device list + active device + selection events | Adopt its data model for the Devices screen and event stream |
| clear-mic-router (open source) | Forces built-in mic during calls | Closest technical sibling; opposite direction (built-in vs external mic) | Borrow the route-hold watchdog concept (assert -> held -> contested -> lost), honest loss reporting, and mode-change detection (`OnModeChangedListener` covering both `MODE_IN_CALL` and `MODE_IN_COMMUNICATION`) |
| Native volume-panel output picker | OS built-in | Baseline user expectation for output choice | Never compete on plain output switching; focus on input/communication diagnosis |

Gap statement: no product combines (a) external professional microphone focus, (b) communication-scenario diagnosis, (c) observability-first UX, (d) cross-vendor compatibility knowledge. That intersection is MicRouter's position.

---

# 5. Technical Feasibility Verdict Table

Verdicts reflect public-API reality on Android 12+ (API 31+). No root, no Magisk, no Shizuku, no accessibility hacks.

| # | Capability | Public API basis | Verdict | Known failure modes |
|---|---|---|---|---|
| F1 | Device enumeration (input/output/communication-capable) | `AudioManager.getDevices()`, `AudioDeviceCallback` | Reliable | HyperOS aggressive battery management can delay callbacks; mitigate with foreground service during sessions |
| F2 | Read current communication device | `getCommunicationDevice()` | Reliable | Returns fallback device when nothing selected; must distinguish "default" vs "user-selected" |
| F3 | List switch candidates | `getAvailableCommunicationDevices()` | Reliable | Whether a USB receiver appears depends on OEM integration of USB headsets; treat as data, surface honestly |
| F4 | Switch communication device | `setCommunicationDevice()` | Conditional | Last-writer-wins; telecom-managed calls may accept then silently ignore; some OEM stacks reject |
| F5 | Hold route against takeover attempts | Re-assert loop polling actual route | Best effort | Works only while our process holds an audio-mode client relationship; system telecom arbitration outranks us on `MODE_IN_CALL`; must implement explicit give-up rules so we never fight the user's own manual choice |
| F6 | Control another app's stream routing | Requires `MODIFY_AUDIO_ROUTING` (system permission) | Impossible | Permanent boundary; communicate clearly in-product |
| F7 | Monitor other apps' microphone content | Privacy capture policy (Android 10+) | Impossible | Only our own test recording is permitted; frame level meter as self-test, not surveillance |
| F8 | Self-test microphone capture per device | `AudioRecord` + `setPreferredDevice()` (own streams) | Reliable | VOICE_COMMUNICATION source routes differently than MIC source; test both and label results per source |
| F9 | Route event timeline | Device callback + mode listener + SCO/BLE state broadcasts | Reliable | Event ordering across sources needs correlation timestamps |

Design responses derived from this table:

1. Route-hold state machine (official API only): `Idle -> Asserted -> Held -> Contested -> Held/Lost`. Transitions driven by route polls and `OnModeChangedListener`. On `Lost`, the UI states plainly: "Android kept the Bluetooth microphone for this call" with the evidence trail.
2. Explanation cards: each timeline event links to a human-readable reason (profile priority, last-writer-wins, telecom arbitration).
3. Compatibility database as the moat: since per-device behavior is irreducible, aggregate observed outcomes into a device x ROM x app-version matrix (V2).

---

# 6. Layered Product Blueprint

The five-layer framework (Device -> Observation -> Routing Decision -> User Control -> Diagnostics) remains the architectural backbone. This section phases it into shippable products.

## 6.1 MVP — Audio Route Inspector (current phase)

Goal: answer "which device is Android using, and why did it change?" with zero configuration.

Features:

1. Device Dashboard
   - Current audio mode (`NORMAL` / `IN_CALL` / `IN_COMMUNICATION`)
   - Current communication device (or "system default")
   - Input devices list with active marker
   - Output devices list with active marker
   - Bluetooth SCO / LE Audio state indicator
2. Route Event Timeline
   - Device added/removed events
   - Mode changes
   - Communication device changes
   - Persisted locally; exportable as text
3. Microphone Self-Test
   - Level meter (RMS) recorded from a chosen input
   - Sample rate / channel info
   - Comparison mode: run same test against two inputs side by side
4. Diagnostic Export
   - One-tap package: device inventory + timeline + test results + build info

MVP acceptance (hardware: Xiaomi 17 Pro Max, Bose QC45, DJI Mic Mini):

1. DJI Mic Mini detected with correct name/type/connection
2. QC45 detected including its hands-free variant
3. During a WeChat call, timeline records the input switch to QC45 hands-free with timestamps
4. Self-test shows measurable signal difference between DJI Mic Mini and QC45 mic
5. Export produces a complete readable report
6. User can articulate why the DJI mic was not used — validated by the explanation card shown at the switch event

Non-goals at MVP: automatic re-routing, background monitoring service, database sync.

## 6.2 V1 — Routing Assistant

Goal: best-effort control with honest feedback, within official API boundaries.

Features:

1. Preferred Device Profiles
   - User ranks preferred communication devices (e.g. 1. DJI Mic Mini, 2. USB mic, 3. Phone mic)
   - Profile applies when the user taps Engage, or automatically when a supported communication session starts
2. Route-Hold Engine
   - State machine from Section 5 (Idle/Asserted/Held/Contested/Lost)
   - Foreground service with persistent notification showing live state
   - Explicit give-up rules: user-initiated manual switches are always respected
   - Quick Settings tile for per-call engagement
3. Scenario Classification
   - Detect `MODE_IN_COMMUNICATION` (VoIP) vs `MODE_IN_CALL` (telecom-managed) and adjust expectations/reporting accordingly
4. Guided Remediation Flows
   - When the platform refuses routing, present evidence-based guidance, e.g. deep-link to the earbuds' Bluetooth settings page and explain that disabling its "Phone calls" (HFP) profile removes it from call-mic contention — a one-time, reboot-proof user action requiring no privileges
5. Success Telemetry (local)
   - Record outcome class per attempt: Applied / Contested-Lost / Platform-Rejected

V1 acceptance:

1. On WeChat voice messages and VoIP calls where arbitration permits, engaging the hold engine results in DJI Mic Mini as input, and the dashboard shows Held
2. On telecom-managed scenarios where the platform silently overrides, the app reports Lost with the evidence trail rather than false success
3. Disengaging restores pre-session routing behavior
4. Battery impact of a 30-minute held session measured and documented

## 6.3 V2 — Compatibility Platform

Goal: convert individual diagnoses into collective knowledge.

Features:

1. Device x ROM Behavior Matrix
   - Community-contributed reports: microphone model, phone model, ROM version, app, scenario, outcome
   - Opt-in submission of exported diagnostic packages (privacy-reviewed, stripped)
2. Presets Library
   - Creator mode (external mic priority), Meeting mode (headset consistency), Podcast QA mode (self-test suite)
3. Camera-App Companion
   - Pre-recording check: verify which input a camera app will likely use based on stored knowledge
4. Publication
   - Technical findings published as documentation; positions MicRouter as the reference on Android communication-audio routing behavior

---

# 7. Permanent Non-Goals

Updated and extended. These do not graduate into later versions.

1. Root or Magisk dependencies
2. Shizuku or any shell-advisor/automation framework dependency
3. Modifying WeChat or any third-party application
4. Injecting virtual audio devices or HAL modifications
5. AI processing, noise cancellation, or voice enhancement
6. Controlling other applications' stream routing (platform-forbidden)
7. Capturing other applications' microphone content (privacy-forbidden)

Rationale for strictness: every excluded item either violates platform policy, breaks silently across releases (see Lesser AudioSwitch), or creates support burden disproportionate to value. The moat is trust through honesty, not workaround depth.

---

# 8. Success Metrics

MVP:

- Detection accuracy: 100% correct identification of the three reference devices across 10 connect/disconnect cycles
- Diagnosis correctness: timeline matches `adb shell dumpsys audio` ground truth on all sampled events
- Comprehension: a first-time user can answer "why is my external mic not used?" unaided after one session

V1:

- Switch success rate tracked per scenario class (WeChat voice message / WeChat VoIP call / cellular call / Zoom-Meet-Teams)
- Zero false-success reports: every Held claim verified by route poll at assertion time
- Session stability: hold engine survives 30-minute calls without process death on HyperOS battery profiles tested

V2:

- Matrix coverage: >= 20 microphone models and >= 10 phone models with at least one verified report each

---

# 9. Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| HyperOS kills background monitor | Timeline gaps, hold engine death | Foreground service with microphone type during sessions; document battery whitelist steps in-app |
| VoIP apps migrate fully to telecom-managed calls | V1 control success rate drops | Scenario classification keeps reporting truthful; pivot emphasis toward diagnosis + guided remediation |
| Platform deprecates current APIs | Rework required | Track Android release notes each cycle; we already target the newest recommended APIs (post-14 deprecation wave) |
| OEM-specific rejection spreads | Fewer controllable devices | Compatibility matrix becomes more valuable, not less; honesty converts limitation into differentiation |
| Users expect "force everything" | Review backlash | Set expectations in store listing and first-run education; never promise what F4/F5 verdicts forbid |

---

# Appendix A: Document Merge Changelog

This document supersedes and absorbs:

- MVP_Product_Model.md — vision, target users, core screens, success criteria, technical architecture tree, roadmap phases, acceptance test (merged into Sections 6.1, 6.2, 8)
- MicRouter_MVP_Product_Model.md — vision, user flow, screens, non-goals, future expansion (merged into Sections 2, 6)

Both files were removed from the repository after merge. Unique engineering artifacts referenced by them (architecture trees, acceptance tests) are preserved above and in MicRouter_Framework_Architecture.md.

# Appendix B: References

1. AudioManager API reference — `setCommunicationDevice()`, `getAvailableCommunicationDevices()`, Android 14 deprecations: developer.android.com/reference/android/media/AudioManager
2. Audio routing API updates for VoIP apps (Android 14): developer.android.com/develop/connectivity/telecom/voip-app/api-updates
3. AOSP `AudioDeviceBroker.java` and `CallAudioCommunicationDeviceTracker.java` — communication-route arbitration internals
4. shivarya/clear-mic-router — field-verified behavior of `setCommunicationDevice()` against telecom-managed calls and TWS route stealing; route-hold watchdog design
5. DJI Mic Mini Third-Party App Compatibility List (sekidorc.com PDF, v1.0 2024-11-26)
6. DJI Mic Mini FAQ — Bluetooth direct connection limitations on Android
7. V2EX topic 535092 — community evidence of Bluetooth microphone failures in WeChat scenarios across OEMs
