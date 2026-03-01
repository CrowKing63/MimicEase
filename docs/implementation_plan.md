# MimicEase Implementation Plan

## Goal Description
Build **MimicEase**, an Android accessibility app based on Google Project GameFace. It allows users with physical disabilities to interact with their Android device using facial expressions via the front camera and MediaPipe on-device ML.

## User Review Required
> [!IMPORTANT]
> The directory currently contains only documentation files. To proceed, I will need to initialize a brand new Android Gradle project. 
> 
> 질문: 제가 터미널(command line)을 사용하여 빈 Android 프로젝트(Gradle) 뼈대를 자동으로 생성할까요? 아니면 Android Studio에서 미리 Empty Activity 프로젝트를 만들어 주시겠습니까?

## Proposed Changes

We will approach the project in several phases as outlined in the documentation:

### 1. Project Initialization & Architecture Setup
- Create Android project (`com.mimicease`) with API 29+ (target 35).
- Setup `build.gradle.kts` (Compose, Hilt, Room, CameraX).
- Create Clean Architecture package structure (`data`, `domain`, `presentation`, `service`, `di`).

### 2. GameFace Module & Assets
- Create a separate `gameFace` module.
- Add `FaceLandmarkerHelper.kt` and configure the `face_landmarker.task` MediaPipe model asset.

### 3. Data & Domain Layer
- Implement `Profile`, `Trigger`, and action models.
- Implement Room Database, Entities, and DI modules.

### 4. Background Services & Face Analysis
- Implement `FaceDetectionForegroundService` to keep the front camera running.
- Implement `ExpressionAnalyzer` and `TriggerMatcher`.

### 5. Accessibility Service
- Implement `MimicAccessibilityService` to perform system actions, gestures, and custom taps.

### 6. Compose UI
- Build the onboarding flow, expression testing screen, profile management, and settings screen.

## Verification Plan
### Automated Tests
- Unit tests for `ExpressionAnalyzer` and `TriggerMatcher` logic.
### Manual Verification
- Run the app on an emulator/device.
- Verify camera permission and foreground service lifecycle.
- Verify facial expression recognition using the Test mode UI.
- Verify accessibility gestures by turning on the service and mapping a smile or blink to the Back/Home action.
