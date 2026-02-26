> **[MimicEase 사양서 — 02/11]** 독립 작업 가능 단위
> **프로젝트**: Google Project GameFace(Android) 기반 표정 인식 안드로이드 접근성 앱
> **스택**: Kotlin + Jetpack Compose, API 29+, MediaPipe 온디바이스 ML
> **전체 목차**: [`docs/00_INDEX.md`](./00_INDEX.md)

---

# 02. 기술 스택

## 2.1 개발 환경

| 분류 | 기술/도구 | 버전/비고 |
|------|-----------|-----------|
| 개발 언어 | Kotlin | 1.9 이상 |
| UI 프레임워크 | Jetpack Compose | Material 3 |
| 최소 SDK | Android API 29 | Android 10 |
| 타겟 SDK | Android API 35 | Android 15 |
| 빌드 도구 | Gradle (Kotlin DSL) | 8.x |
| IDE | Android Studio | Ladybug 이상 권장 |
| 아키텍처 패턴 | MVVM + Clean Architecture | 단방향 데이터 흐름 (UDF) |

## 2.2 핵심 라이브러리 의존성

| 라이브러리 | 용도 | 비고 |
|-----------|------|------|
| **Google Project GameFace (Android)** | 표정 인식 핵심 엔진 | 소스 모듈로 직접 포함 (`gameFace/`) |
| **MediaPipe Face Landmarker** | 얼굴 랜드마크 감지 | GameFace 내부 의존성, 별도 추가 불필요 |
| **Jetpack CameraX** | 전면 카메라 프리뷰/분석 | `camera-camera2`, `camera-lifecycle`, `camera-view` |
| **Room Database** | 프로필·트리거 로컬 저장 | KSP 어노테이션 프로세서 사용 |
| **Hilt (Dagger)** | 의존성 주입(DI) | `hilt-android`, `hilt-compiler` |
| **Kotlin Coroutines + Flow** | 비동기 처리 및 상태 관리 | `kotlinx-coroutines-android` |
| **DataStore (Preferences)** | 앱 설정값 저장 | `datastore-preferences` |
| **Accompanist Permissions** | 런타임 권한 처리 | `accompanist-permissions` |
| **Gson / Moshi** | Action 파라미터 JSON 직렬화 | `TriggerEntity.actionParams` 필드에 사용 |
| **Timber** | 로깅 | `timber` |

### build.gradle.kts (주요 의존성 예시)

```kotlin
dependencies {
    // GameFace 모듈
    implementation(project(":gameFace"))

    // CameraX
    val cameraxVersion = "1.3.0"
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Accompanist
    implementation("com.google.accompanist:accompanist-permissions:0.33.2-alpha")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Timber
    implementation("com.jakewharton.timber:timber:5.0.1")
}
```

## 2.3 프로젝트 디렉터리 구조

Clean Architecture 3개 레이어: **Data / Domain / Presentation**

```
MimicEase/
├── app/
│   └── src/main/
│       ├── java/com/mimicease/
│       │   │
│       │   ├── core/                        # 공통 유틸, 확장함수, 상수
│       │   │   ├── extensions/
│       │   │   │   ├── ContextExtensions.kt
│       │   │   │   └── FlowExtensions.kt
│       │   │   └── utils/
│       │   │       ├── BlendShapeUtils.kt
│       │   │       └── CoordinateUtils.kt
│       │   │
│       │   ├── data/                        # ── Data Layer ──
│       │   │   ├── local/
│       │   │   │   ├── database/
│       │   │   │   │   └── MimicDatabase.kt  # RoomDatabase 싱글턴
│       │   │   │   ├── entity/
│       │   │   │   │   ├── ProfileEntity.kt
│       │   │   │   │   └── TriggerEntity.kt
│       │   │   │   └── dao/
│       │   │   │       ├── ProfileDao.kt
│       │   │   │       └── TriggerDao.kt
│       │   │   ├── repository/
│       │   │   │   ├── ProfileRepositoryImpl.kt
│       │   │   │   └── TriggerRepositoryImpl.kt
│       │   │   └── model/
│       │   │       └── ActionParamsDto.kt    # JSON 직렬화용 DTO
│       │   │
│       │   ├── domain/                      # ── Domain Layer ──
│       │   │   ├── model/
│       │   │   │   ├── Profile.kt
│       │   │   │   ├── Trigger.kt
│       │   │   │   └── Action.kt            # sealed class
│       │   │   ├── repository/
│       │   │   │   ├── ProfileRepository.kt  # 인터페이스
│       │   │   │   └── TriggerRepository.kt
│       │   │   └── usecase/
│       │   │       ├── GetActiveProfileUseCase.kt
│       │   │       ├── SaveProfileUseCase.kt
│       │   │       ├── SaveTriggerUseCase.kt
│       │   │       └── DeleteTriggerUseCase.kt
│       │   │
│       │   ├── presentation/                # ── Presentation Layer ──
│       │   │   ├── ui/
│       │   │   │   ├── onboarding/
│       │   │   │   │   ├── OnboardingScreen.kt
│       │   │   │   │   └── OnboardingViewModel.kt
│       │   │   │   ├── home/
│       │   │   │   │   ├── HomeScreen.kt
│       │   │   │   │   └── HomeViewModel.kt
│       │   │   │   ├── test/                # 표정 테스트 화면
│       │   │   │   │   ├── ExpressionTestScreen.kt
│       │   │   │   │   └── ExpressionTestViewModel.kt
│       │   │   │   ├── profile/
│       │   │   │   │   ├── ProfileListScreen.kt
│       │   │   │   │   ├── ProfileEditScreen.kt
│       │   │   │   │   └── ProfileViewModel.kt
│       │   │   │   ├── trigger/
│       │   │   │   │   ├── TriggerListScreen.kt
│       │   │   │   │   ├── TriggerEditScreen.kt
│       │   │   │   │   ├── TriggerViewModel.kt
│       │   │   │   │   └── CoordinatePickerScreen.kt  # 커스텀 좌표 선택
│       │   │   │   └── settings/
│       │   │   │       ├── SettingsScreen.kt
│       │   │   │       └── SettingsViewModel.kt
│       │   │   └── MainActivity.kt
│       │   │
│       │   ├── service/                     # ── Services ──
│       │   │   ├── MimicAccessibilityService.kt   # AccessibilityService 확장
│       │   │   ├── FaceDetectionForegroundService.kt  # 카메라 + GameFace
│       │   │   ├── ExpressionAnalyzer.kt          # EMA + 연속 프레임 필터
│       │   │   ├── TriggerMatcher.kt              # 임계값 비교 + 쿨다운
│       │   │   └── ActionExecutor.kt              # 실제 액션 실행
│       │   │
│       │   └── di/                          # ── Hilt DI 모듈 ──
│       │       ├── DatabaseModule.kt
│       │       ├── RepositoryModule.kt
│       │       └── ServiceModule.kt
│       │
│       ├── assets/
│       │   └── face_landmarker.task          # MediaPipe 모델 바이너리
│       │
│       └── res/
│           ├── xml/
│           │   └── accessibility_service_config.xml
│           └── values/
│               └── strings.xml
│
└── gameFace/                                # GameFace 라이브러리 모듈
    └── src/main/java/
        └── FaceLandmarkerHelper.kt          # 핵심 진입점
```

## 2.4 아키텍처 원칙

**레이어 의존 방향**: `Presentation → Domain ← Data`

- `Domain` 레이어는 Android 프레임워크에 **의존하지 않음** (순수 Kotlin)
- `Data` 레이어는 `Domain`의 Repository 인터페이스를 구현
- `Presentation` 레이어는 `Domain`의 UseCase만 직접 호출
- `service/` 패키지는 예외적으로 `Domain` 모델을 직접 참조 (실시간 처리 최적화)

**상태 관리**: `StateFlow` (ViewModel) + `SharedFlow` (이벤트) + Compose `collectAsState()`
