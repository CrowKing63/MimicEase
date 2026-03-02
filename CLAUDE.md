# CLAUDE.md

## 프로젝트 개요

**MimicEase**는 ALS, 뇌성마비, 척수 손상 등 심각한 신체 장애를 가진 사용자가 얼굴 표정으로 스마트폰을 제어할 수 있도록 하는 Android 접근성 앱입니다. Google Project GameFace(MediaPipe Face Landmarker)를 활용해 52개의 얼굴 BlendShape를 실시간으로 분석하며, 모든 처리는 온디바이스(서버 통신 없음)로 이루어집니다.

## 저장소 구조

```
MimicEase/
├── app/                              # 메인 앱 모듈
│   └── src/main/java/com/mimicease/
│       ├── data/                     # 데이터 레이어 (Room DB, DataStore, Repository 구현체)
│       │   ├── local/                # Room Entities, Daos, AppSettingsDataStore
│       │   ├── model/                # ActionSerializer (Gson 직렬화)
│       │   └── repository/           # ProfileRepositoryImpl, TriggerRepositoryImpl, SettingsRepositoryImpl
│       ├── domain/                   # 도메인 레이어 (순수 Kotlin, Android 비의존)
│       │   ├── model/                # Profile, Trigger, Action, InteractionMode, ModeManager
│       │   └── repository/           # Repository 인터페이스
│       ├── presentation/             # 프레젠테이션 레이어 (Compose UI + ViewModel)
│       │   └── ui/                   # home, onboarding, profile, settings, test
│       ├── service/                  # 백그라운드 서비스 및 핵심 로직
│       ├── di/                       # Hilt DI 모듈
│       ├── navigation/               # Compose 네비게이션 그래프 (MimicNavGraph)
│       └── ui/theme/                 # Material3 테마 (Color, Theme, Type)
├── gameFace/                         # GameFace 라이브러리 모듈 (얼굴 인식 엔진)
│   └── src/main/java/com/mimicease/gameface/
│       └── FaceLandmarkerHelper.java # MediaPipe 핵심 얼굴 감지 (Java)
├── docs/                             # 상세 문서 (12개 마크다운 파일)
├── gradle/
│   └── libs.versions.toml            # Gradle 버전 카탈로그
└── MimicEase_사양서.md               # 한국어 사양서
```

### 핵심 서비스 & 컴포넌트

| 파일 | 역할 |
|------|------|
| `MimicAccessibilityService.kt` | Android 접근성 서비스 (제스처/액션 실행, FaceService 바인딩) |
| `FaceDetectionForegroundService.kt` | 포그라운드 서비스 (카메라 + 얼굴 감지, Phase 3 통합) |
| `ExpressionAnalyzer.kt` | EMA 필터 적용 (alpha 설정 가능, 연속 프레임 카운터 포함) |
| `TriggerMatcher.kt` | 임계값 + holdDuration + 쿨다운 기반 트리거 매칭 |
| `ActionExecutor.kt` | 접근성 액션 실행 (GestureDescription, Intent, AudioManager, 커서 액션) |
| `GlobalToggleController.kt` | 다중 채널 글로벌 토글 (키조합/표정/브로드캐스트) + TTS/진동 피드백 |
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
  ↓
FaceLandmarkerHelper (GameFace)  [52 BlendShapes + transformMatrix]
  ↓
ExpressionAnalyzer  [EMA 필터]
  ↓
GlobalToggleController.checkExpressionToggle()  [토글 먼저 확인]
  ↓
TriggerMatcher  [임계값 + holdDuration + 쿨다운]
  ↓ (EXPRESSION_ONLY 모드)
ActionExecutor  [GestureDescription, Intent, AudioManager]
  ↓
MimicAccessibilityService
  ↓
시스템 액션 (뒤로가기, 홈, 제스처, 앱 실행, 미디어 제어)

HEAD_MOUSE 모드 추가 경로:
  ↓ (transformMatrix에서 yaw/pitch 추출)
HeadTracker  [데드존 + 가속 + 화면 좌표 변환]
  ↓
DwellClickController  [드웰 진행도 계산]
  ↓
CursorOverlayView  [오버레이 커서 표시]
```

### 상호작용 모드 (`InteractionMode`)

| 모드 | 설명 | 차단되는 Action |
|------|------|----------------|
| `EXPRESSION_ONLY` | 기본 모드. 표정 → 고정좌표 제스처/시스템 액션 | TapAtCursor 계열 |
| `CURSOR_CLICK` | BT 마우스로 커서 이동, 표정으로 클릭 | 없음 (전체 허용) |
| `HEAD_MOUSE` | 머리 움직임으로 커서 제어 + 드웰 클릭 | TapCenter, TapCustom, DoubleTap, LongPress |

## 기술 스택

- **언어**: Kotlin 2.0.21
- **UI**: Jetpack Compose + Material 3
- **아키텍처**: MVVM + Clean Architecture (Data / Domain / Presentation 3계층)
- **얼굴 인식**: Google Project GameFace + MediaPipe Face Landmarker 0.10.8
- **카메라**: CameraX 1.4.0
- **데이터베이스**: Room 2.6.1
- **의존성 주입**: Hilt 2.51.1
- **비동기**: Kotlin Coroutines + Flow
- **설정 저장**: DataStore Preferences 1.1.1
- **직렬화**: Gson 2.11.0
- **로깅**: Timber 5.0.1
- **최소 SDK**: Android API 29 / 타겟 SDK API 35

## 빌드 및 테스트 명령어

```bash
# 클린 빌드
./gradlew clean build

# 디버그 APK 빌드
./gradlew assembleDebug

# 릴리즈 APK 빌드
./gradlew assembleRelease

# Kotlin 컴파일만 빠르게 검증 (APK 빌드 없이)
./gradlew :app:compileDebugKotlin

# 유닛 테스트 실행
./gradlew test

# 앱 모듈 유닛 테스트
./gradlew :app:test

# 기기/에뮬레이터에 설치
./gradlew installDebug
```

> **참고**: 기기 연결 없이는 계측 테스트(`connectedAndroidTest`)를 실행할 수 없습니다.

## 개발 규칙

### 아키텍처 원칙
- **Clean Architecture 엄수**: `domain` 레이어는 Android에 의존하지 않아야 합니다.
- **단방향 데이터 흐름**: ViewModel → UI (StateFlow / UiState 패턴 사용)
- **Repository 패턴**: UI는 직접 데이터 소스에 접근하지 않습니다.

### 코드 스타일
- Kotlin 관용구(코틀린스러운 코드) 사용
- 새로운 UI 컴포넌트는 Jetpack Compose로 작성
- DI는 반드시 Hilt 사용
- 비동기 처리는 Coroutines + Flow 사용

### 주요 함정 (Gotchas)

- **SystemClock 금지**: `service/` 레이어에서 `android.os.SystemClock` 사용 금지 — JVM 유닛 테스트에서 "not mocked" 예외 발생. `System.currentTimeMillis()` 사용. `TriggerMatcher`, `ExpressionAnalyzer`, `DwellClickController`, `GlobalToggleController` 모두 수정 완료.
- **ViewModel 위치**: ViewModel 클래스가 각 화면 파일과 동일한 `.kt` 파일에 정의됨 (예: `ExpressionTestScreen.kt` 내 `ExpressionTestViewModel`, `SettingsScreen.kt` 내 `SettingsViewModel`)
- **Java 모듈**: `gameFace/` 모듈의 `FaceLandmarkerHelper`는 Java로 작성됨 (Kotlin 아님)
- **TalkBack 공존**: `onAccessibilityEvent()`에서 이벤트를 consume하지 말 것 — TalkBack과 체인 유지
- **카메라 충돌**: 다른 앱 카메라 사용 시 `CameraState.ERROR_CAMERA_IN_USE` 감지 후 `pauseAnalysis()` 호출
- **서비스 재시작**: `onStartCommand()`에서 `intent`가 null일 수 있음 (`START_STICKY` 재시작 시)
- **FaceDetectionForegroundService 초기화 순서**: `startForeground()`는 `onCreate()` 초반에 호출해야 5초 타임아웃 방지. MediaPipe 모델 로딩은 `Handler(faceLandmarkerHelper.looper).post { init() }`로 HandlerThread에서 비동기 실행
- **Navigation triggerId**: `triggerEdit/{profileId}/{triggerId}` 라우트에서 `triggerId`는 `TriggerEditViewModel`의 `SavedStateHandle`에 Navigation이 자동 주입함. 명시적 추출 코드(`backStackEntry.arguments?.getString("triggerId")`)를 추가하여 null 라우팅 방지 완료.

### 주요 도메인 모델

- `Profile` — 표정 프로필 (이름, 활성화 상태, globalCooldownMs, 트리거 목록)
- `Trigger` — 표정-액션 매핑 (BlendShape, threshold, holdDurationMs, cooldownMs, priority, action)
- `Action` (sealed class) — 35+ 액션 타입 (시스템, 제스처, 커서, 앱 실행, 미디어 제어, 스위치)
- `InteractionMode` — EXPRESSION_ONLY / CURSOR_CLICK / HEAD_MOUSE
- `AppSettings` — DataStore 설정 (EMA, 모드, 글로벌 토글, 헤드마우스, 드웰 등)
- **BlendShape 전체 목록**: `ExpressionTestScreen.kt`의 `BLENDSHAPE_DISPLAY_NAMES`가 52개 정규 출처 — 다른 파일에서 BlendShape 목록 유지 시 이 맵 기준으로 동기화

### SettingsScreen 구성 (전체 섹션)

1. **상호작용 모드** — RadioButton으로 EXPRESSION_ONLY / CURSOR_CLICK / HEAD_MOUSE 전환
2. **헤드 마우스** — 감도(0.5~3.0x), 데드존(0.0~0.1 rad) 슬라이더
3. **드웰 클릭** — 활성화 스위치, 대기시간(500~3000ms), 반경(10~100px) 슬라이더 (비활성 시 슬라이더 dim)
4. **글로벌 토글** — 볼륨키 조합 / 표정(오발동 경고) / 브로드캐스트 스위치
5. **감지 설정** — EMA α, 연속 프레임
6. **알림 설정** — 포그라운드 알림 스위치
7. **시스템** — 접근성 서비스 상태, 배터리 최적화 제외
8. **기타** — 개발자 모드, 버전

### 권한

앱은 다음 권한이 필요합니다:
- `CAMERA` — 얼굴 감지
- `FOREGROUND_SERVICE` — 백그라운드 실행
- `FOREGROUND_SERVICE_CAMERA` — 포그라운드 카메라 사용
- `VIBRATE` — 토글 시 진동 피드백
- 접근성 서비스 활성화 — 사용자가 직접 설정에서 활성화 필요

## 유닛 테스트 현황

`app/src/test/java/com/mimicease/`에 3개의 테스트 존재:
- `ExpressionAnalyzerTest` — EMA 필터 로직
- `TriggerMatcherTest` — 임계값/쿨다운/holdDuration 매칭
- `ActionSerializerTest` — Gson 직렬화/역직렬화

`./gradlew :app:test` 전체 통과 확인됨.

## 문서 참조

| 문서 | 내용 |
|------|------|
| `docs/00_INDEX.md` | 문서 목차 및 빠른 참조 |
| `docs/01_project_overview.md` | 프로젝트 목표, 대상 사용자, BlendShape 목록 |
| `docs/02_tech_stack.md` | 기술 스택, 라이브러리, 아키텍처 구조 |
| `docs/03_architecture.md` | 데이터 흐름, 핵심 컴포넌트 4개, 서비스 2개 |
| `docs/04_expression_test.md` | 표정 테스트 화면 상세 |
| `docs/05_profile_trigger.md` | 프로필/트리거 설정 |
| `docs/06_actions.md` | 전체 액션 목록 및 파라미터 사양 |
| `docs/07_ui_screens.md` | 전체 화면 설계 및 네비게이션 구조 |
| `docs/08_data_model.md` | Room 엔티티, 도메인 모델, Action sealed class |
| `docs/09_accessibility_service.md` | 서비스 생명주기, 제스처 실행, EMA 필터 |
| `docs/10_permissions_performance.md` | 권한 처리 및 성능 최적화 |
| `docs/11_roadmap_testing_notes.md` | 개발 로드맵, 테스트 전략, 구현 주의사항 |
| `MimicEase_사양서.md` | 한국어 전체 사양서 |
