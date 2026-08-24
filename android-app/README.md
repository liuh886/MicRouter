# MicRouter Android App

MVP phase: Audio Route Inspector (see `docs/MicRouter_Product_Blueprint.md` section 6.1).

- Kotlin 2.0 + Jetpack Compose (Material 3)
- minSdk 31 / targetSdk 34 (`setCommunicationDevice` is API 31+, so no legacy paths)
- MVVM with StateFlow unidirectional data flow, manual DI container
- Single `:app` module; package layering prepares for V1 route-hold engine split

## Build

Open this folder in Android Studio (Ladybug or newer). If Gradle wrapper files are
missing on first open, let the IDE regenerate them (or run `gradle wrapper` once).
No local SDK required for editing; building requires an Android SDK with API 34.

## Architecture

```
app/src/main/java/com/liuh886/microuter/
├── MicRouterApp.kt              Application + manual DI container
├── MainActivity.kt              Permission flow + Compose host
├── core/model/                  Unified models (framework layer 1 output)
│   ├── AudioDeviceItem.kt       Device model + AudioDeviceInfo mapping/labels
│   ├── RouteEvent.kt            Timeline event model
│   ├── AudioSessionState.kt     Session snapshot (mode, comm device, lists)
│   └── HoldState.kt             V1 route-hold state machine states
├── audio/                       Engine: thin wrappers over public APIs only
│   ├── AudioDeviceScanner.kt    Enumeration (getDevices, candidates)
│   ├── RouteMonitor.kt          Device callback + mode + comm-device listeners
│   ├── CommunicationController.kt  Read/select/clear communication device
│   └── MicTester.kt             AudioRecord self-test with RMS level meter
├── data/
│   ├── RouteEventLogger.kt      Ring buffer + export text
│   ├── DiagnosticReportBuilder.kt  Full report: device + snapshot + inventory + log
│   └── AudioRepository.kt       Facade: state/events flows + control actions
└── ui/
    ├── RootScaffold.kt          Bottom navigation (Dashboard/Inspector/MicTest)
    ├── theme/Theme.kt           Material 3 + dynamic color
    ├── dashboard/               Status cards + per-device "Use for calls"
    ├── inspector/               Route timeline + copy/share export
    └── mictest/                 Input picker + live level meter
```

## Permissions

- `RECORD_AUDIO` — microphone self-test
- `BLUETOOTH_CONNECT` — Bluetooth device metadata on Android 12+

## Scope guardrails

Official public APIs only. No root, no Magisk, no Shizuku, no modification of
other applications. Where the platform ignores a routing request, the app must
report that honestly rather than claim success.
