# Record & Playback + A/B Compare — Implementation Plan

Status: Implementation plan for the v1.0 hero feature
Last updated: 2026-08-25
Derived from: Release_Scope_v1.md (D1 test-bench hero, D2 full A/B compare)
Authoritative for: record/playback engine design + A/B compare implementation

## 1. Benchmark — what excellent projects do, and what we adopt

Live-fetched via `gh` (GitHub API) on 2026-08-25. Now in Android / Oboe referenced from training knowledge (WebFetch was blocked by network policy).

### 1.1 twilio/audioswitch (fetched: README, AudioSwitch.kt, AudioDeviceManager.kt, AudioDevice.kt)

| Dimension | twilio's approach | MicRouter adoption |
|---|---|---|
| Threading | "Strongly recommended: create and access from a single application thread." | **Keep** MicTester's single worker thread. Do NOT add a second read loop for capture. |
| Audio state | `cacheAudioState()` / `restoreAudioState()` snapshot mode + mic-mute + speakerphone, restore on deactivate. | **Adopt**: snapshot `mode` + `communicationDevice` at session start; restore both on end (replaces hardcoded `MODE_NORMAL`). Matches Blueprint V1 "disengaging restores pre-session routing". |
| Device activate | `enableCommunicationForAudioDeviceType` calls `setCommunicationDevice` only when different, else `clearCommunicationDevice`. | **Adopt**: guard `CommunicationController.select` against redundant re-set. |
| BT failure | On activation error: revert selected device, re-enumerate, fire `onBluetoothHeadsetActivationError`. | Already mirrored by `⚠ SCO link unconfirmed` honest reporting. No change. |
| Device model | `sealed class AudioDevice` with typed subclasses. | Keep flat `AudioDeviceItem` (deals with ~15 device types + computed `recordable`/`isCommunicationCandidate` flags). No rewrite. |

### 1.2 Android AudioTrack — short-clip playback

`MODE_STATIC` is the correct tool for short, fixed-length, repeatedly-played clips: write the whole PCM buffer once, then `play()`; replay via `stop()`+`play()` or `setPlaybackHeadPosition(0)`. No underrun risk, lowest latency. `MODE_STREAM` is for continuous streaming and would need a feed loop — wrong tool for A/B clips.

### 1.3 Now in Android (unidirectional data flow)

Single `UiState` data class per screen, exposed as `StateFlow`, updated only via intent functions in the ViewModel. MicRouter already follows MVVM+StateFlow; the A/B state machine is expressed this way.

## 2. Design Decisions

1. **Extend the existing MicTester loop, do not build a parallel recorder.** The whole point of the test bench is that the captured clip reflects *the same routing truth* the level meter shows (SCO activation, `setPreferredDevice`, MIC-source fallback). A parallel `AudioRecord` would diverge from that truth.
2. **`AudioTrack.MODE_STATIC` for playback.** Short clips, written once, played repeatedly.
3. **Pre-allocated capture buffer.** 15s @48kHz mono = 720,000 samples ≈ 1.44MB. Pre-allocate `ShortArray(cap)` + fill-count; no reallocation during capture.
4. **Save/restore audio state** (adopted from twilio): snapshot `mode` + `communicationDevice` on session start, restore on end.
5. **Keep single audio worker thread.** Capture appends inside the existing read loop; playback runs its blocking `write()` on a short-lived background thread to avoid main-thread jank, then `play()` (async on the mixer thread).
6. **Surface the sample-rate gap as a diagnostic signal.** A/B clips record their `sampleRate`; the UI shows it (e.g. QC45 @16kHz narrowband vs DJI @48kHz). This turns a technical difference into the *explanation* of why one mic sounds worse — the product's honest-explanation ethos, applied to the test bench.

## 3. Data Model

New `core/model/RecordedClip.kt`:

```
data class RecordedClip(
    val deviceId: Int,
    val deviceName: String,
    val samples: ShortArray,      // mono PCM 16-bit
    val sampleRate: Int,
    val durationMs: Long,
    val peak: Int,                // 0..32767
    val rms: Float               // 0..1
)
```

## 4. Implementation Steps (phased, each compiles + verifiable)

### Phase A — Capture in MicTester

- Add capture state to `MicTester`: `captureBuffer: ShortArray?`, `captureFill: Int`, `captureDeviceId`, `captureStartMs`.
- Add `startCapture(deviceId, maxMs)` and expose `onClipReady: (RecordedClip) -> Unit` (threaded to a callback).
- In the existing read loop, after RMS: if capturing, append `buffer[0 until read]`, track peak; finalize on `stopCapture()` or when `fill >= cap`.
- No UI yet; verify via a temporary hook (or unit-test the finalize math).

### Phase B — Playback (`ClipPlayer`)

- New `audio/ClipPlayer.kt`: `play(clip: RecordedClip, output: AudioDeviceInfo?, onDone)` using `AudioTrack.MODE_STATIC`:
  - build AudioTrack (mono, clip.sampleRate, bufferSize = samples.size * 2),
  - `setPreferredDevice(output)`, blocking `write()` on a background thread, `play()`,
  - expose `stop()`/`release()`; guard lifecycle with a job + `try/finally`.
- Replay semantics: `stop()` then `play()` (or `setPlaybackHeadPosition(0)` + `play()`).

### Phase C — ViewModel + A/B UI

- `MicTestViewModel`: add `clipA: RecordedClip?`, `clipB: RecordedClip?`, `captureState: Idle/Recording(device)/Ready`, `playbackState`. Single `UiState` (NiA style) or extend existing — keep it minimal.
- `MicTestScreen`: add two A/B slots. Each slot: device picker (recordable only), Record button (→ `startCapture`), live level bar while recording, then a stored RMS/peak bar + Play button once ready. Side-by-side layout with sample-rate labels.
- Reuse existing `LevelTrack` for the stored-level bars; add a small `CompareBar` in `Components.kt` if needed.

### Phase D — Save/restore audio state

- In `AudioRepository` (or `CommunicationController`): snapshot `mode` + `communicationDevice` at `beginLink`/session start; on `endLink`/stop, restore both instead of `setMode(MODE_NORMAL)` + `clear()`.
- Add redundant-set guard to `CommunicationController.select`.

### Phase E — Verify + commit

- Compile → `adb install` → on-device screenshot verification → conventional commit (per handover working agreement).

## 5. File Change Map

| File | Change |
|---|---|
| `core/model/RecordedClip.kt` | NEW — clip value type |
| `audio/ClipPlayer.kt` | NEW — MODE_STATIC playback |
| `audio/MicTester.kt` | capture state + append in loop + finalize callback |
| `audio/CommunicationController.kt` | redundant-set guard + state snapshot helpers |
| `data/AudioRepository.kt` | save/restore audio state on session start/end |
| `ui/mictest/MicTestViewModel.kt` | A/B state machine (clipA/clipB/capture/playback) |
| `ui/mictest/MicTestScreen.kt` | Record/Play buttons + A/B slots + sample-rate labels |
| `ui/components/Components.kt` | `CompareBar` component (if needed) |

## 6. Verification Plan

- Unit: clip finalize math (peak/rms/duration) on synthetic buffers.
- On-device (Xiaomi HyperOS):
  1. DJI Mic Mini → record → playback → hear your own voice.
  2. A/B DJI vs QC45 → side-by-side bars show a real level gap; sample-rate labels show 48k vs 16k.
  3. End session → confirm prior audio mode/route restored (not forced to NORMAL).
  4. BT clip (QC45 SCO) → confirm `⚠ SCO link unconfirmed` still surfaces honestly if HyperOS rejects.
- Regression: level meter, ear monitor, hot-switch, widget all unchanged.

## 7. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Main-thread jank on 1.4MB playback `write()` | Write on background thread; `play()` is async. |
| Capture diverges from routed device | Capture inside the existing loop (decision #1), never a second `AudioRecord`. |
| A/B sample-rate mismatch (BT 16k vs USB 48k) | Show sample rate as a feature, not a bug (decision #6); level bars are normalized RMS. |
| Memory (2 clips × 1.44MB) | Bounded 15s cap, in-memory only, released when a new clip replaces an old one. |
| Restore leaves wrong state on abnormal stop | `try/finally` around session; always run restore in `endLink`. |
