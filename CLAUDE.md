# CLAUDE.md

## 프로젝트 개요

**MimicEase**는 ALS, 뇌성마비, 척수 손상 등 심각한 신체 장애를 가진 사용자가 얼굴 표정으로 스마트폰을 제어할 수 있도록 하는 Android 접근성 앱입니다. Google Project GameFace(MediaPipe Face Landmarker)를 활용해 52개의 얼굴 BlendShape를 실시간으로 분석하며, 모든 처리는 온디바이스(서버 통신 없음)로 이루어집니다.

기술 스택: Kotlin 2.0.21 · Jetpack Compose + Material 3 · MVVM + Clean Architecture · MediaPipe 0.10.8 · CameraX 1.4.0 · Room 2.6.1 · Hilt 2.51.1 · DataStore 1.1.1 · minSdk 29 / targetSdk 35 → 상세: `docs/02_tech_stack.md`

## 저장소 구조

```
app/src/main/java/com/mimicease/
├── data/         # Room DB, DataStore, Repository 구현체
├── domain/       # 순수 Kotlin 도메인 레이어 (Android 비의존)
├── presentation/ # Compose UI + ViewModel
├── service/      # 백그라운드 서비스 및 핵심 로직
├── di/           # Hilt DI 모듈
└── navigation/   # MimicNavGraph
gameFace/         # FaceLandmarkerHelper.java (MediaPipe, Java)
scripts/          # Windows ADB 인스톨러 (install.bat, install.ps1)
docs/             # 상세 문서 12개 → docs/00_INDEX.md 참조
```

### 핵심 서비스 & 컴포넌트

| 파일 | 역할 |
|------|------|
| `MimicAccessibilityService.kt` | Android 접근성 서비스 (제스처/액션 실행, FaceService 바인딩) |
| `FaceDetectionForegroundService.kt` | 포그라운드 서비스 (카메라 + 얼굴 감지, Phase 3 통합) |
| `ExpressionAnalyzer.kt` | EMA 필터 적용 (alpha 설정 가능, 연속 프레임 카운터 포함) |
| `TriggerMatcher.kt` | 임계값 + holdDuration + 쿨다운 기반 트리거 매칭 |
| `ActionExecutor.kt` | 접근성 액션 실행 (GestureDescription, Intent, AudioManager, 커서 액션) |
| `GlobalToggleController.kt` | 다중 채널 글로벌 토글 (표정/브로드캐스트) + TTS/진동 피드백 |
| `HeadTracker.kt` | 머리 yaw/pitch → 화면 커서 좌표 변환 (데드존 + 가속) |
| `CursorOverlayView.kt` | HEAD_MOUSE 모드 오버레이 커서 UI |
| `DwellClickController.kt` | 드웰 클릭 (일정 시간 정지 → 자동 탭) |
| `SwitchAccessBridge.kt` | Switch Access 키 이벤트 주입 |
| `CursorTracker.kt` | 접근성 이벤트로 현재 포커스 위치 추적 |
| `ToggleBroadcastReceiver.kt` | 외부 앱/AI 어시스턴트 토글 브로드캐스트 수신 |
| `ModeManager.kt` | 모드별 Action 허용/차단 필터링 로직 |

### 데이터 흐름

```
Camera (ImageProxy)
  ↓ FaceLandmarkerHelper (GameFace)  [52 BlendShapes + transformMatrix]
  ↓ ExpressionAnalyzer  [EMA 필터]
  ↓ GlobalToggleController.checkExpressionToggle()  [토글 먼저 확인]
  ↓ TriggerMatcher  [임계값 + holdDuration + 쿨다운]
  ↓ ActionExecutor  [GestureDescription, Intent, AudioManager]
  ↓ MimicAccessibilityService → 시스템 액션

HEAD_MOUSE 추가 경로:
  transformMatrix → HeadTracker [데드존+가속] → DwellClickController → CursorOverlayView
```

### 상호작용 모드 (`InteractionMode`)

| 모드 | 설명 | 차단되는 Action |
|------|------|----------------|
| `EXPRESSION_ONLY` | 기본 모드. 표정 → 고정좌표 제스처/시스템 액션 | TapAtCursor 계열 |
| `CURSOR_CLICK` | BT 마우스로 커서 이동, 표정으로 클릭 | 없음 (전체 허용) |
| `HEAD_MOUSE` | 머리 움직임으로 커서 제어 + 드웰 클릭 | TapCenter, TapCustom, DoubleTap, LongPress |

## 빌드 및 테스트

```bash
./gradlew assembleDebug          # 디버그 APK
./gradlew assembleRelease        # 릴리즈 APK
./gradlew :app:compileDebugKotlin # Kotlin 컴파일만 빠르게 검증
./gradlew :app:test              # 유닛 테스트 (3개, 전체 통과)
./gradlew installDebug           # 기기 설치
```

> Windows에서 `Unable to establish loopback connection` 오류 시: `./gradlew --stop` 후 재시도.

## 개발 규칙

### 아키텍처 원칙
- **Clean Architecture 엄수**: `domain` 레이어는 Android에 의존하지 않아야 합니다.
- **단방향 데이터 흐름**: ViewModel → UI (StateFlow / UiState 패턴 사용)
- **Repository 패턴**: UI는 직접 데이터 소스에 접근하지 않습니다.

### 코드 스타일
- Kotlin 관용구 사용 · 새 UI는 Jetpack Compose · DI는 Hilt · 비동기는 Coroutines + Flow

### 주요 도메인 모델

- `Profile` — 표정 프로필 (이름, 활성화 상태, globalCooldownMs, 트리거 목록)
- `Trigger` — 표정-액션 매핑 (BlendShape, threshold, holdDurationMs, cooldownMs, priority, action)
- `Action` (sealed class) — 35+ 액션 타입 (시스템, 제스처, 커서, 앱 실행, 미디어 제어, 스위치)
- `InteractionMode` — EXPRESSION_ONLY / CURSOR_CLICK / HEAD_MOUSE
- `AppSettings` — DataStore 설정 (EMA, 모드, 글로벌 토글, 헤드마우스, 드웰 등)
- **BlendShape 정규 출처**: `presentation/ui/common/BlendShapeUtils.kt`의 `BLENDSHAPE_DISPLAY_NAMES` (52개)

### 주요 함정 (Gotchas)

- **SystemClock 금지**: `service/` 레이어에서 `android.os.SystemClock` 사용 금지 — JVM 유닛 테스트 "not mocked" 예외. `System.currentTimeMillis()` 사용.
- **ViewModel 위치**: 각 화면 `.kt` 파일 내에 함께 정의됨 (예: `ExpressionTestScreen.kt` 내 `ExpressionTestViewModel`)
- **Java 모듈**: `gameFace/` 모듈의 `FaceLandmarkerHelper`는 Java로 작성됨 (Kotlin 아님)
- **TalkBack 공존**: `onAccessibilityEvent()`에서 이벤트를 consume하지 말 것 — TalkBack과 체인 유지
- **카메라 충돌**: 다른 앱 카메라 사용 시 `CameraState.ERROR_CAMERA_IN_USE` 감지 후 `pauseAnalysis()` 호출
- **서비스 재시작**: `onStartCommand()`에서 `intent`가 null일 수 있음 (`START_STICKY` 재시작 시)
- **FaceDetectionForegroundService 초기화 순서**: `startForeground()`는 `onCreate()` 초반에 호출해야 5초 타임아웃 방지. MediaPipe 모델 로딩은 `Handler(faceLandmarkerHelper.looper).post { init() }`로 HandlerThread에서 비동기 실행
- **Navigation triggerId**: `triggerEdit/{profileId}/{triggerId}` 라우트에서 `backStackEntry.arguments?.getString("triggerId") ?: return@composable`로 null 방지.
- **PS5.1 UTF-8 BOM**: `install.ps1`은 반드시 UTF-8 BOM(`EF BB BF`)으로 저장 — BOM 없는 UTF-8은 PS5.1에서 한글 파싱 오류로 즉시 종료.
- **ADB + $ErrorActionPreference**: `"Stop"` + ADB는 치명적 조합 — ADB는 성공 시에도 stderr에 쓰므로 `NativeCommandError` 발생. 반드시 `"Continue"` 사용, 에러 판정은 `$LASTEXITCODE`로.
- **PowerShell 파이프라인 단일 항목**: `@(& adb devices | Where-Object { ... })`로 강제 배열화 필수 — 결과 1개이면 문자열 반환.
- **자동 업데이트 제거 완료 (v1.4.0)**: `CheckForUpdateUseCase` 등 전부 삭제됨. 복원 금지.
- **볼륨 키 조합 토글 제거 완료 (v1.4.1)**: `handleKeyEvent()` 등 전부 삭제됨. 복원 금지 — 빅스비 TTS 초기화 크래시 원인. `GlobalToggleController`는 표정/브로드캐스트 채널만 지원.
- **versionName 필수 동기화**: `app/build.gradle.kts`의 `versionName`이 `BuildConfig.VERSION_NAME` 기준값 — 릴리즈 태그 배포 시 함께 업데이트.
- **라이브러리 manifest 권한 병합**: 불필요한 `INTERNET` 권한은 `<uses-permission android:name="android.permission.INTERNET" tools:node="remove" />`로 명시적 차단.
- **DataStore 싱글턴 — applicationContext 필수**: Service context에서 `context.appSettingsDataStore` 직접 호출 금지 → `IllegalStateException: multiple DataStores active`. 반드시 `context.applicationContext.appSettingsDataStore` 사용.
- **카메라 권한 없이 접근성 서비스 활성화**: 온보딩에서 카메라 권한 거부 시 `shouldShowRationale`로 감지해 앱 설정 화면으로 안내.
- **ModalBottomSheet 높이 제약**: `fillMaxHeight(fraction)` 내부 `weight()` 크래시 — `heightIn(max = LocalConfiguration.current.screenHeightDp.dp * fraction)` 절대값 사용. `fillMaxSize()` 대신 `fillMaxWidth()` 권장.
- **카메라 프리뷰 공유**: `FaceDetectionForegroundService.previewUseCase`의 SurfaceProvider를 `attachPreviewSurfaceProvider/detachPreviewSurfaceProvider`로 외부 등록/해제 — 카메라 재바인딩 없음.
