# MimicEase

**Face-expression smartphone control for people with severe physical disabilities**

MimicEase is a free, open-source Android accessibility app that lets users with ALS, cerebral palsy, spinal cord injuries, and other motor disabilities control their smartphones entirely through facial expressions. No touch, no voice commands — just your face.

> All processing happens **on-device**. No data ever leaves your phone.

---

## Why MimicEase?

People with severe motor impairments often cannot use touchscreens or styluses. Existing solutions are expensive, require specialized hardware, or depend on cloud services that raise privacy concerns. MimicEase turns any modern Android phone's front camera into a fully capable, privacy-respecting input device.

---

## Key Features

- **52 facial expression triggers** — powered by Google MediaPipe Face Landmarker (cheek puffs, eyebrow raises, mouth gestures, jaw open, tongue out, and more)
- **3 interaction modes**
  - `Expression Only` — fixed-coordinate taps and system actions triggered by expressions
  - `Cursor Click` — Bluetooth mouse moves the cursor; expressions click
  - `Head Mouse` — head movement controls an on-screen cursor with dwell-click support
- **35+ mappable actions** — back, home, scroll, swipe, app launch, volume, media control, screen brightness, and more
- **Multi-profile support** — create different profiles for different postures or apps
- **Global toggle** — enable/disable via volume key combo, a facial expression, Quick Settings tile, or broadcast (Bixby Routines / Gemini)
- **EMA smoothing** — exponential moving average filter reduces jitter and false triggers
- **Hold duration + cooldown** — prevents accidental activations
- **Fully offline** — zero network access; all computation runs on-device via MediaPipe

---

## How It Works

```
Front Camera (ImageProxy)
      ↓
MediaPipe Face Landmarker  [52 BlendShapes + head pose matrix]
      ↓
Expression Analyzer  [EMA filter, consecutive-frame gate]
      ↓
Global Toggle Check
      ↓
Trigger Matcher  [threshold + holdDuration + cooldown]
      ↓
Action Executor  [GestureDescription, Intent, AudioManager]
      ↓
Android System  [tap, swipe, back, home, app launch, …]
```

In **Head Mouse** mode, the head-pose transformation matrix is additionally routed through `HeadTracker` → `DwellClickController` → an overlay cursor rendered on screen.

---

## Screenshots

> *(Coming soon — contributions welcome)*

---

## Requirements

| Requirement | Minimum |
|---|---|
| Android | 10 (API 29) |
| Target SDK | 35 |
| Front camera | Required |
| Accessibility service | Must be enabled manually in Settings |
| Internet | Not required |

---

## Installation

### From Releases (recommended)

1. Download the latest `.apk` from the [Releases](../../releases) page.
2. Enable **Install from unknown sources** in your Android settings.
3. Install the APK.
4. Open MimicEase and follow the onboarding flow.
5. Go to **Settings → Accessibility → MimicEase** and enable the accessibility service.

### Build from Source

```bash
git clone https://github.com/<your-org>/MimicEase.git
cd MimicEase

# Debug APK
./gradlew assembleDebug

# Install directly to a connected device
./gradlew installDebug
```

**Prerequisites:** Android Studio Hedgehog or newer, JDK 17+.

---

## Permissions

| Permission | Purpose |
|---|---|
| `CAMERA` | Real-time face detection via front camera |
| `FOREGROUND_SERVICE` | Keep the face-detection service running while the screen is on |
| `FOREGROUND_SERVICE_CAMERA` | Required on Android 12+ for foreground camera access |
| `VIBRATE` | Haptic feedback on toggle events |
| `RECEIVE_BOOT_COMPLETED` | Optional auto-start on device boot |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevent the OS from suspending the foreground service |
| Accessibility Service | Execute gestures and system actions on behalf of the user |

---

## Architecture

MimicEase follows **Clean Architecture** with three layers:

```
app/
├── data/         # Room DB · DataStore · Repository implementations
├── domain/       # Pure Kotlin models & repository interfaces (no Android deps)
├── presentation/ # Jetpack Compose screens + ViewModels
├── service/      # Background services & core logic
├── di/           # Hilt dependency injection modules
└── navigation/   # Compose Navigation graph
gameFace/
└── FaceLandmarkerHelper.java  # MediaPipe face-detection engine (Java)
```

**Tech stack:** Kotlin 2.0 · Jetpack Compose · Material 3 · Hilt · Room · DataStore · CameraX · MediaPipe · Coroutines/Flow · Gson · Timber

---

## Supported Languages

| Language | Region |
|---|---|
| English | Default |
| 한국어 | Korea |
| 中文（简体） | Mainland China |
| 中文（繁體） | Taiwan / Hong Kong |
| 日本語 | Japan |
| Español | Spanish-speaking regions |
| Français | French-speaking regions |
| Deutsch | Germany / Austria / Switzerland |
| Português | Brazil / Portugal |

---

## Expression Trigger Examples

| Expression | Example Action |
|---|---|
| Raise left eyebrow | Scroll up |
| Puff left cheek | Go back |
| Open mouth wide | Go home |
| Smile | Volume up |
| Tongue out | Take screenshot |
| Blink right eye | Launch favorite app |

Every trigger is fully customizable — threshold, hold duration, cooldown, and action are all configurable per profile.

---

## Contributing

Contributions of all kinds are welcome — code, translations, testing on new devices, and accessibility feedback from users with disabilities.

1. Fork the repository.
2. Create a feature branch: `git checkout -b feat/my-feature`
3. Follow the [developer guidelines](docs/00_INDEX.md).
4. Open a pull request with a clear description.

Please keep the `domain/` layer free of Android dependencies and use `System.currentTimeMillis()` instead of `SystemClock` in service-layer code (required for unit-test compatibility).

---

## Running Tests

```bash
# Unit tests (no device needed)
./gradlew :app:test

# Quick Kotlin compilation check
./gradlew :app:compileDebugKotlin
```

Tests cover: EMA filter logic (`ExpressionAnalyzerTest`), trigger matching (`TriggerMatcherTest`), and Gson serialization (`ActionSerializerTest`).

---

## License

```
Copyright 2025 MimicEase Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## Acknowledgements

- [Google Project GameFace](https://github.com/google/project-gameface) — face-control reference implementation
- [MediaPipe](https://developers.google.com/mediapipe) — on-device ML face landmarking
- [CameraX](https://developer.android.com/training/camerax) — Android camera abstraction

---

*MimicEase is built with care for people who need it most. If this project has helped you or someone you know, please consider starring the repository or sharing it with accessibility communities.*
