# MicRouter Phase Completion Report

Status reflects the actual codebase on branch main (see android-app/). Scope and
acceptance criteria are defined in MicRouter_Product_Blueprint.md section 6.

## Phase 1 - Device Discovery

Status: implemented

- AudioDeviceScanner over getDevices() and getAvailableCommunicationDevices()
- Unified AudioDeviceItem model with type labels and communication-candidate flag
- Dashboard screen: audio mode, active communication device, input/output lists

## Phase 2 - Route Monitoring

Status: implemented

- RouteMonitor: AudioDeviceCallback + OnModeChangedListener + OnCommunicationDeviceChangedListener
- Replay-buffered event flow; RouteEventLogger ring buffer (500 events)
- Inspector screen: timestamped timeline with copy/share export

## Phase 3 - Communication Controller

Status: implemented

- Read / select / clear communication device via API 31 methods only
- Per-device "Use for calls" action limited to communication candidates
- Honest labeling of telecom-managed vs VoIP modes in state labels

## Phase 4 - Diagnostic Export

Status: implemented

- DiagnosticReportBuilder: device info + session snapshot + device inventory + route log
- One-tap copy or share from Inspector

## Phase 5 - Microphone Validation

Status: implemented

- MicTester: AudioRecord self-test per selected input (VOICE_COMMUNICATION source)
- setPreferredDevice applied to own stream; RMS level meter with sample-rate report
- Runtime RECORD_AUDIO + BLUETOOTH_CONNECT permission flow in MainActivity

## Phase 6 - Hardware Validation

Status: pending physical test

Hardware matrix (blueprint section 6.1 acceptance):

- Xiaomi 17 Pro Max
- Bose QC45
- DJI Mic Mini
- WeChat communication mode

Test procedure:

1. Connect QC45, connect DJI Mic Mini receiver
2. Start WeChat call; verify timeline records the input switch to QC45 hands-free
3. Run mic self-test for both inputs; compare levels
4. Use "Use for calls" on DJI Mic Mini during call; observe Held/Lost outcome honestly
5. Export diagnostic report and archive it under docs/reports/

Constraints unchanged: no root. No Magisk. No Shizuku. No application modification.
