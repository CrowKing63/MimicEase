> **[MimicEase 사양서 — 04/11]** 독립 작업 가능 단위
> **프로젝트**: Google Project GameFace(Android) 기반 표정 인식 안드로이드 접근성 앱
> **스택**: Kotlin + Jetpack Compose, API 29+, MediaPipe 온디바이스 ML
> **전체 목차**: [`docs/00_INDEX.md`](./00_INDEX.md)

---

# 04. 온보딩 & 표정 테스트 모드

## 4.1 온보딩 플로우

신규 사용자가 앱 최초 실행 시 경험하는 5단계 시퀀스입니다.
`AppSettings.onboardingCompleted == false`일 때 자동 진입합니다.

### 단계별 명세

| 단계 | 화면 이름 | 주요 내용 | 완료 조건 |
|------|-----------|-----------|-----------|
| Step 1 | 환영 (Welcome) | 앱 소개, 핵심 가치 3가지 카드, '시작하기' 버튼 | 버튼 탭 |
| Step 2 | 카메라 권한 | 카메라 권한이 필요한 이유 설명 → 권한 요청 다이얼로그 | 권한 허용 |
| Step 3 | 접근성 서비스 | "설정으로 이동" 버튼 → 시스템 설정으로 딥링크 → 돌아왔을 때 활성화 감지 | 접근성 서비스 활성화 확인 |
| Step 4 | 표정 테스트 소개 | 테스트 모드 사용법 설명 (짧은 애니메이션/일러스트), '해보기' 버튼 | 버튼 탭 또는 스킵 |
| Step 5 | 기본 프로필 생성 | "기본 프로필 자동 생성" 버튼 (추천 트리거 4개 포함) 또는 "직접 설정" | 프로필 생성 완료 |

### 온보딩 완료 처리

```kotlin
// OnboardingViewModel.kt
fun completeOnboarding() {
    viewModelScope.launch {
        settingsRepository.updateSettings { it.copy(onboardingCompleted = true) }
        _navigationEvent.emit(NavigationEvent.GoToHome)
    }
}
```

### 접근성 서비스 활성화 상태 감지

```kotlin
// 접근성 서비스가 활성화되어 있는지 확인
fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(
        AccessibilityServiceInfo.FEEDBACK_ALL_MASK
    )
    return enabledServices.any {
        it.resolveInfo.serviceInfo.packageName == context.packageName
    }
}

// 접근성 설정 화면으로 이동
fun navigateToAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    context.startActivity(intent)
}
```

### 기본 프로필 자동 생성 (추천 트리거 구성)

Step 5에서 '기본 프로필 자동 생성'을 선택하면 아래 4개 트리거가 포함된 프로필이 생성됩니다:

| 트리거 이름 | 표정 | 임계값 | 홀드 | 액션 |
|------------|------|--------|------|------|
| 오른쪽 윙크 → 뒤로가기 | `eyeBlinkRight` | 0.6 | 300ms | `Action.GlobalBack` |
| 왼쪽 윙크 → 홈 | `eyeBlinkLeft` | 0.6 | 300ms | `Action.GlobalHome` |
| 입 벌리기 → 위로 스크롤 | `jawOpen` | 0.5 | 200ms | `Action.ScrollUp` |
| 눈썹 올리기 → 최근 앱 | `browInnerUp` | 0.5 | 400ms | `Action.GlobalRecents` |

---

## 4.2 표정 테스트 모드 (Expression Test Mode)

사용자가 자신이 어떤 표정을 얼마나 명확하게 지을 수 있는지 **실시간으로** 확인하는 화면입니다.
Bottom Navigation의 '테스트(Test)' 탭에서 진입합니다.

### 4.2.1 화면 레이아웃

```
┌─────────────────────────────────────────┐
│   [ 카메라 프리뷰 (원형 마스크, 좌우 반전) ]  │  ← 상단 40%
│              (얼굴 위치 가이드 원)          │
├─────────────────────────────────────────┤
│  [전체] [눈] [입] [눈썹] [기타]            │  ← 카테고리 탭
├─────────────────────────────────────────┤
│  👁 눈 깜빡임 (오른쪽)  0.73  ████████░░  │
│  👁 눈 깜빡임 (왼쪽)    0.12  ██░░░░░░░░  │
│  ↑  눈썹 올리기 (안쪽)  0.05  █░░░░░░░░░  │
│  😊 미소 (오른쪽)       0.45  █████░░░░░  │
│  😊 미소 (왼쪽)         0.41  ████░░░░░░  │
│  👄 입 벌리기           0.31  ███░░░░░░░  │
│     ...                                 │  ← LazyColumn 스크롤
├─────────────────────────────────────────┤
│     [+ 이 표정으로 트리거 만들기]           │  ← FAB (Top 1 표정 자동 선택)
└─────────────────────────────────────────┘
```

### 4.2.2 카테고리별 블렌드쉐이프 분류

| 카테고리 탭 | 포함 블렌드쉐이프 |
|------------|-----------------|
| 눈 | `eyeBlink*`, `eyeWide*`, `eyeSquint*`, `eyeLook*` |
| 눈썹 | `browInnerUp`, `browOuterUp*`, `browDown*` |
| 입 | `mouth*`, `jaw*`, `tongueOut` |
| 볼/코 | `cheek*`, `noseSneer*` |
| 전체 | 모든 52개 표정 |

### 4.2.3 ExpressionTestViewModel 상태

```kotlin
data class ExpressionTestUiState(
    val blendShapeValues: Map<String, Float> = emptyMap(),  // 실시간 값
    val selectedCategory: BlendShapeCategory = BlendShapeCategory.ALL,
    val topExpressions: List<Pair<String, Float>> = emptyList(),  // Top 3
    val isRecording: Boolean = false,
    val recordingResult: RecordingResult? = null,  // 녹화 완료 시
    val isCameraReady: Boolean = false
)

data class RecordingResult(
    val blendShapeId: String,
    val maxValue: Float,
    val avgValue: Float,
    val minValue: Float
)
```

### 4.2.4 기능 상세

**실시간 게이지 업데이트**
- `FaceLandmarkerHelper.onResults` → `StateFlow<Map<String, Float>>` → Compose recomposition
- 업데이트 주기: 카메라 프레임 기준 (최대 30fps). UI는 Compose가 자동 처리.
- 각 항목: `LinearProgressIndicator(progress = value)` + 숫자 표시 (`"%.2f".format(value)`)

**임계값 미리보기 슬라이더**
- 각 표정 항목에 슬라이더 확장 가능 (항목 탭 시 펼쳐짐)
- 슬라이더 위치 = 현재 임계값 후보. 현재 실시간 값이 이 선을 넘으면 게이지가 색상 변경.
- `Slider(value = previewThreshold, onValueChange = { previewThreshold = it }, valueRange = 0f..1f)`

**녹화 모드 (10초)**
- '● 녹화' 버튼 탭 → 10초 카운트다운
- 녹화 중 각 블렌드쉐이프의 최대/평균/최소 값 수집
- 완료 후 결과 다이얼로그: "이 표정의 최대값은 0.82였습니다. 임계값 0.65로 트리거를 만드시겠어요?"
- → 확인 탭 시 `TriggerEditScreen`으로 해당 블렌드쉐이프와 추천 임계값 미리 채워서 이동

**'이 표정으로 트리거 만들기' FAB**
- Top 1 (가장 높은 값) 블렌드쉐이프를 자동 선택
- `TriggerEditScreen(preselectedBlendShape = topBlendShape, preselectedThreshold = currentValue * 0.85f)`로 이동
- 임계값은 현재 최대값의 85%로 사전 설정 (안전 마진)

### 4.2.5 카메라 연동 코드 패턴

```kotlin
// ExpressionTestViewModel.kt
@HiltViewModel
class ExpressionTestViewModel @Inject constructor(
    private val cameraManager: CameraManager  // FaceLandmarkerHelper 래퍼
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpressionTestUiState())
    val uiState: StateFlow<ExpressionTestUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cameraManager.blendShapeFlow
                .flowOn(Dispatchers.Default)
                .collect { values ->
                    val top3 = values.entries
                        .sortedByDescending { it.value }
                        .take(3)
                        .map { it.key to it.value }
                    _uiState.update { it.copy(blendShapeValues = values, topExpressions = top3) }
                }
        }
    }
}
```
