> **[MimicEase 사양서 — 11/11]** 독립 작업 가능 단위
> **프로젝트**: Google Project GameFace(Android) 기반 표정 인식 안드로이드 접근성 앱
> **스택**: Kotlin + Jetpack Compose, API 29+, MediaPipe 온디바이스 ML
> **전체 목차**: [`docs/00_INDEX.md`](./00_INDEX.md)

---

# 11. 개발 로드맵 & 테스트 & 주의사항

## 11.1 개발 로드맵

### Phase 1: 기반 구축 (예상 3~4주)

> 목표: 앱이 실행되고 표정이 실시간으로 화면에 표시되는 상태

- [ ] Gradle 프로젝트 초기 설정 (Hilt, Room, Compose, KSP)
- [ ] `gameFace/` 모듈 통합 (Project GameFace Android 브랜치 클론 → 서브모듈)
- [ ] CameraX + `FaceLandmarkerHelper` 연동 (전면 카메라 프리뷰 + 분석)
- [ ] `ExpressionTestScreen` 기본 구현 (블렌드쉐이프 실시간 표시)
- [ ] Room DB 스키마: `ProfileEntity`, `TriggerEntity`, DAO 구현
- [ ] `ProfileRepository`, `TriggerRepository` 구현체 작성
- [ ] DataStore `AppSettings` 저장/로드 구현
- [ ] Hilt 모듈 설정 (Database, Repository, Service 바인딩)
- [ ] `MimicAccessibilityService` 기본 뼈대 (선언 + 서비스 연결 확인)

**Phase 1 완료 기준**: 앱 실행 → 카메라 프리뷰 → 표정 게이지가 실시간으로 움직임

---

### Phase 2: 핵심 기능 (예상 3~4주)

> 목표: 표정 → 액션 파이프라인 완성, 기본 UI 구현

- [ ] `ExpressionAnalyzer` 구현 (EMA 필터 + 연속 프레임 확정)
- [ ] `TriggerMatcher` 구현 (임계값, 홀드타임, 쿨다운 3종 세트, 우선순위)
- [ ] `ActionExecutor` 구현 (시스템 액션, 탭, 스와이프 우선 구현)
- [ ] `FaceDetectionForegroundService` 완성 (카메라 + 파이프라인 통합)
- [ ] `MimicAccessibilityService` ↔ `FaceDetectionForegroundService` 바인딩
- [ ] 포그라운드 서비스 알림 (기본 버전)
- [ ] `ProfileListScreen`, `ProfileEditScreen` 구현
- [ ] `TriggerListScreen`, `TriggerEditScreen` 구현
- [ ] `ActionPickerBottomSheet` 구현 (시스템/제스처 탭)
- [ ] `HomeScreen` 구현 (서비스 상태 카드 + 활성 프로필 카드)
- [ ] `OnboardingScreen` 5단계 구현
- [ ] 기본 프로필 자동 생성 로직

**Phase 2 완료 기준**: 트리거 설정 후 실제로 표정을 지으면 뒤로가기/홈 버튼이 동작

---

### Phase 3: 완성 및 테스트 (예상 2~3주)

> 목표: 출시 가능한 완성도

- [ ] `CoordinatePickerScreen` (커스텀 좌표 설정 오버레이) 구현
- [ ] `AppPickerScreen` (설치된 앱 목록 선택) 구현
- [ ] `ActionPickerBottomSheet` 앱/미디어 탭 추가 완성
- [ ] 드래그, 핀치 액션 구현
- [ ] 포그라운드 알림 완성 (일시정지/재개 액션 버튼)
- [ ] `SettingsScreen` 완성 (모든 설정 항목)
- [ ] 화면 꺼짐 최적화 (BroadcastReceiver)
- [ ] 카메라 충돌 처리 (다른 앱 카메라 사용 시 자동 일시정지)
- [ ] 부팅 자동 시작 (BootReceiver, 선택사항)
- [ ] 배터리 최적화 제외 요청 플로우
- [ ] 개발자 모드 (FPS, 추론 시간 표시)
- [ ] 실제 기기 테스트 + 성능 튜닝
- [ ] TalkBack 호환성 검증
- [ ] Play Store 배포 준비

---

## 11.2 테스트 전략

### 11.2.1 단위 테스트 (Unit Test)

```kotlin
// ExpressionAnalyzerTest.kt
class ExpressionAnalyzerTest {
    @Test
    fun `EMA 필터 - alpha 0_5로 첫 번째 값은 그대로 반환`() {
        val analyzer = ExpressionAnalyzer(alpha = 0.5f)
        val result = analyzer.processSmoothed(mapOf("eyeBlinkRight" to 0.8f))
        assertThat(result["eyeBlinkRight"]).isEqualTo(0.8f)
    }

    @Test
    fun `EMA 필터 - 두 번째 값은 평균으로 수렴`() {
        val analyzer = ExpressionAnalyzer(alpha = 0.5f)
        analyzer.processSmoothed(mapOf("eyeBlinkRight" to 1.0f))
        val result = analyzer.processSmoothed(mapOf("eyeBlinkRight" to 0.0f))
        assertThat(result["eyeBlinkRight"]).isEqualTo(0.5f)
    }
}

// TriggerMatcherTest.kt
class TriggerMatcherTest {
    @Test
    fun `임계값 미달 시 액션 없음`() { ... }

    @Test
    fun `임계값 초과 후 holdDuration 미충족 시 액션 없음`() { ... }

    @Test
    fun `정상 발동 후 개별 쿨다운 중 재발동 없음`() { ... }

    @Test
    fun `전역 쿨다운 중 다른 트리거도 발동 없음`() { ... }

    @Test
    fun `우선순위 낮은 번호가 먼저 실행`() { ... }
}

// ActionSerializerTest.kt
class ActionSerializerTest {
    @Test
    fun `모든 Action 타입 직렬화 후 역직렬화 동등성 검증`() {
        val actions = listOf(
            Action.GlobalHome, Action.GlobalBack,
            Action.TapCustom(0.3f, 0.7f),
            Action.SwipeUp(300L),
            Action.Drag(0.2f, 0.5f, 0.8f, 0.5f, 500L),
            Action.OpenApp("com.android.chrome")
        )
        actions.forEach { action ->
            val (type, params) = ActionSerializer.serialize(action)
            val deserialized = ActionSerializer.deserialize(type, params)
            assertThat(deserialized).isEqualTo(action)
        }
    }
}
```

### 11.2.2 통합 테스트

- **DB 마이그레이션**: Room `@Database(version = N, exportSchema = true)` + `MigrationTestHelper`
- **Repository Flow**: `turbine` 라이브러리로 Flow 방출 순서 검증
- **ActionExecutor**: `AccessibilityService` Mock으로 올바른 메서드 호출 검증

### 11.2.3 UI 테스트 (Compose Testing)

```kotlin
// 온보딩 E2E 테스트
@Test
fun onboardingFlow_completesSuccessfully() {
    composeTestRule.onNodeWithText("시작하기").performClick()
    // 권한 허용은 UiAutomator로 처리
    composeTestRule.onNodeWithText("접근성 설정으로 이동").assertIsDisplayed()
    // ...
}
```

### 11.2.4 실제 기기 테스트 체크리스트

| 테스트 항목 | 합격 기준 |
|------------|----------|
| 밝은 조명 환경 | 주요 표정 인식률 > 90% |
| 어두운 조명 환경 | 주요 표정 인식률 > 70% |
| 안경 착용 | 눈 관련 표정 인식률 > 80% |
| 마스크 착용 | 눈/눈썹 표정만 동작 (입 표정 비활성화) |
| 다양한 피부 톤 | 편향 없이 일관된 인식률 |
| 1시간 연속 실행 | 크래시 없음, 메모리 증가 < 30MB |
| 배터리 1시간 | < 5% 소모 |
| Android 10 (API 29) | 정상 동작 |
| Android 15 (API 35) | 정상 동작 |
| 저사양 기기 (RAM 3GB) | 앱 종료 없음, 허용 지연 < 150ms |

---

## 11.3 주요 구현 주의사항

### 11.3.1 카메라 충돌 처리

다른 앱(카메라, 영상통화)이 카메라를 점유하면 CameraX가 오류를 발생시킵니다.

```kotlin
// CameraX의 카메라 사용 불가 콜백 처리
cameraProvider.bindToLifecycle(...).also { camera ->
    camera.cameraInfo.cameraState.observe(lifecycleOwner) { state ->
        if (state.error?.code == CameraState.ERROR_CAMERA_IN_USE) {
            pauseAnalysis()
            // 알림 업데이트: "카메라가 다른 앱에서 사용 중"
        }
    }
}
```

### 11.3.2 TalkBack과의 공존

```kotlin
// MimicAccessibilityService.onAccessibilityEvent()
// → 이벤트를 소비(return 후 super 호출 안 함)하지 말 것
// → TalkBack은 이벤트 체인에 있으므로 패스스루 유지

override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    // 화면 정보 업데이트만 하고, 이벤트를 consume하지 않음
    currentWindowPackage = event?.packageName?.toString()
}

// Touch Exploration 모드 감지
fun isScreenReaderActive(): Boolean {
    val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    return am.isTouchExplorationEnabled
}

// Touch Exploration 활성화 시 탭 액션 방식 변경
// → dispatchGesture 대신 AccessibilityNodeInfo.performAction(ACTION_CLICK) 선호
```

### 11.3.3 개인정보 보호 체크리스트

> ⚠️ Play Store 심사 통과를 위한 필수 항목

- [ ] **온디바이스 처리 확인**: 카메라 프레임 또는 표정 데이터를 네트워크로 전송하는 코드 없음
- [ ] **카메라 데이터 저장 없음**: `ImageProxy`는 처리 후 즉시 `close()`. 파일 저장 없음.
- [ ] **사용 로그 수집 없음**: 어떤 액션을 언제 실행했는지 기록·전송하지 않음.
- [ ] **개인정보처리방침 작성**: "카메라는 표정 인식에만 사용되며, 모든 처리는 기기 내에서만 이루어집니다."
- [ ] **Play Console 민감 권한 설명**: CAMERA 권한 사용 목적 명시

### 11.3.4 서비스 안정성

```kotlin
// START_STICKY: 시스템이 서비스를 강제 종료해도 자동 재시작
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // ...
    return START_STICKY
}

// 메모리 부족 시 서비스 재시작 후 상태 복원
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // intent가 null일 수 있음 (재시작 시)
    val profileId = intent?.getStringExtra("profileId") ?: settingsDataStore.activeProfileId
    // ...
    return START_STICKY
}
```

### 11.3.5 API 버전 분기 처리

```kotlin
// FOREGROUND_SERVICE_CAMERA (API 34+)
// AndroidManifest에 선언만 하면 되며, 코드 분기 불필요

// GLOBAL_ACTION_LOCK_SCREEN (API 28+)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
}

// GLOBAL_ACTION_TAKE_SCREENSHOT (API 28+)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
}
```

---

## 11.4 알려진 제약사항 & 향후 계획

| 제약사항 | 원인 | 향후 계획 |
|---------|------|-----------|
| 화면 꺼짐 시 감지 불가 | 카메라 API 제한 | 화면 꺼짐 전 마지막 설정 유지, 켜지면 즉시 재개 |
| 마스크 착용 시 입 표정 오작동 | MediaPipe 모델 한계 | 마스크 착용 모드: 눈/눈썹 표정만 허용 옵션 |
| 동시 다중 얼굴 처리 불가 | `maxNumFaces=1` | 단일 사용자 앱 특성상 현재 설계 유지 |
| 앱 스위처 화면에서 일부 제스처 제한 | Android 보안 정책 | 시스템 레벨 제스처로 우회 |
| Play Store 접근성 앱 심사 강화 | Google 정책 | 영상 데모 + 명확한 목적 설명 자료 준비 |
