# MicRouter v1.0 Release Scope

Status: Authoritative release contract for the first public release (v1.0)
Last updated: 2026-08-25
Derived from: MicRouter_Product_Blueprint.md (Section 6.1 MVP) + owner decisions (2026-08-25)

This document records the two positioning decisions that define the v1.0 release, the hero feature specification, and the concrete in/out scope. When this document and the Blueprint disagree on release scope, this document wins for release planning; the Blueprint remains the source of truth for product strategy and permanent non-goals.

## 1. Release Positioning (decisions)

Two decisions locked on 2026-08-25:

| # | Decision | Choice |
|---|---|---|
| D1 | Hero positioning | **Test bench first** — not inspector-first, not control-first |
| D2 | Record/playback depth | **Full A/B record + playback compare** — not single-path only |

### D1 — Hero: the test bench

Store-listing one-liner:

> "Test, hear, and trust your external mic."

The first-run narrative, home screen, and widget all lead with the test bench. The routing truth panel is NOT the hero; it is the credibility layer that makes the test bench trustworthy.

Rationale:

- Complete consumer loop: open → record → hear → trust.
- Lowest platform risk: self-recording via `AudioRecord` + `setPreferredDevice` is feasibility-table F8 = Reliable; no dependency on telecom arbitration.
- The moat survives: the test bench differs from free recorder apps because it shows which device the signal actually came from (`actual → device`), grounding trust in routing truth.

Anti-drift rule (non-negotiable): MicRouter is a test instrument, not a voice recorder. Recording is a diagnostic act, not the product body. Enforced by bounded clip length, no file persistence, no export.

### D2 — Full A/B record/playback

Chosen depth: record A → record B → side-by-side RMS/peak bars + independent playback per clip.

This maps directly to the Blueprint's core pain: hearing, in the user's own voice, why a $200 DJI mic beats a QC45 headset mic.

## 2. Hero Feature Specification

### 2.1 Record & playback

- Select input (recordable devices only) → Record → Stop (or 15s auto-cap) → immediate playback to the chosen listen output.
- Bounded clip: 15s max. PCM 16-bit mono @48kHz ≈ 96KB/s → 15s ≈ 1.4MB, in-memory only.
- No file persistence, no export (privacy-friendly; nothing is saved).

### 2.2 A/B compare

- Record clip A (device 1) → Record clip B (device 2) → side-by-side view: RMS/peak bars + Play A / Play B.
- The comparison is the proof: same phrase, two devices, the user hears the difference directly.

### 2.3 Underlying capability

`MicTester` already reads PCM and computes RMS before discarding. The work is:

- Add a capture mode that accumulates buffers up to the cap.
- Add an independent `AudioTrack` playback path for captured buffers.
- Add a compare screen (two bars + two play buttons).

Change is localized to `MicTester.kt` + `MicTestScreen.kt` / a new compare screen.

## 3. Information Architecture (hero flow)

```
Onboarding (≤3 screens): Test · Hear · Trust
        ↓
Monitor (HERO): test bench
   ├── level meter (existing)
   ├── Record / Playback (new, primary action)
   ├── A/B compare (new, differentiator)
   ├── ear monitor + listen output (existing)
   └── "actual → device" proof strip (existing, promoted)
        ↓
Devices (pro mode): routing truth + use-for-calls
        ↓
Log (evidence): timeline + export
```

## 4. Truth Panel's New Role

Not dropped — demoted to the proof layer:

- Monitor's `actual → device` line promoted from SESSION small text to a persistent proof strip.
- Devices + Log remain as a professional mode; not forced on first launch.
- "Why/explanation cards" in the Log are deferred: the A/B playback answers "why" more directly.

## 5. In / Out Scope

IN (v1.0):

1. Record & playback (hero)
2. A/B compare (differentiator)
3. First-run onboarding (≤3 screens, inline jargon explanation)
4. Promoted `actual → device` proof strip
5. Release hygiene (below)

OUT (deferred):

- Route hold engine (HoldState state machine + foreground service + QS tile) → V1.x
- Compatibility matrix → V2
- Auto re-routing → never (permanent non-goal)
- Explanation cards in Log → deferred (A/B covers the need)

## 6. Release Hygiene Checklist

- [ ] Release signing config (keystore, gradle `signingConfig`)
- [ ] Privacy policy (one page; zero network permission = headline selling point)
- [ ] Store listing copy + screenshots + launcher icon (replace placeholder)
- [ ] `versionName` → 1.0.0, `versionCode` → 1
- [ ] R8/proguard rules verified on a real device (audio engine uses only public APIs)
- [ ] Verify widget + app behavior after process death (battery-manager kill)

## 7. Explicit Non-Goals (unchanged)

Root / Magisk / Shizuku / modifying other apps / AI processing / controlling other apps' streams / capturing other apps' mic content. See Blueprint §7.
