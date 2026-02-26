> **[MimicEase 사양서 — 05/11]** 독립 작업 가능 단위
> **프로젝트**: Google Project GameFace(Android) 기반 표정 인식 안드로이드 접근성 앱
> **스택**: Kotlin + Jetpack Compose, API 29+, MediaPipe 온디바이스 ML
> **전체 목차**: [`docs/00_INDEX.md`](./00_INDEX.md)

---

# 05. 프로필 & 트리거 설정

## 5.1 프로필 (Profile)

여러 개의 프로필을 만들어 상황에 따라 전환할 수 있습니다.
예: '일반 사용', '영상 시청', '게임', '독서'
**한 번에 하나의 프로필만 활성화**됩니다.

### 5.1.1 Profile 속성 명세

| 속성 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | `String` (UUID) | PK, Not Null | 고유 식별자 |
| `name` | `String` | 최대 30자, Not Null | 프로필 이름 |
| `icon` | `String` | Not Null | 이모지 문자 또는 Material Icon 이름 |
| `isActive` | `Boolean` | 전체 중 단 하나만 true | 현재 활성화 여부 |
| `sensitivity` | `Float` | 0.5 ~ 2.0, 기본값 1.0 | 전역 감도 배율. 모든 트리거 임계값에 곱해짐. |
| `globalCooldownMs` | `Int` | 0 ~ 2000ms, 기본값 300 | 어떤 트리거든 발동 후 전체 일시 대기 시간 |
| `createdAt` | `Long` | 자동 생성 | 생성 타임스탬프 (epoch ms) |
| `updatedAt` | `Long` | 자동 갱신 | 수정 타임스탬프 (epoch ms) |
| `triggers` | `List<Trigger>` | Room Relation | 이 프로필에 속한 트리거 목록 |

> **sensitivity 적용 방식**: 실제 발동 임계값 = `trigger.threshold / profile.sensitivity`
> sensitivity=2.0이면 절반의 표정 강도로도 발동. sensitivity=0.5면 두 배 강하게 표정 지어야 발동.

### 5.1.2 프로필 목록 화면 (ProfileListScreen)

**UI 구성 요소**:
- `LazyColumn` — 프로필 카드 목록
- 각 카드: `[아이콘] [이름] [트리거 N개] [활성화 토글] [편집 버튼]`
- 활성화 토글: `Switch` — 탭 시 기존 활성 프로필 비활성화 후 이 프로필 활성화
- 우측 상단 FAB: `+` → 프로필 생성 다이얼로그
- 길게 누르기: 삭제 / 복제 컨텍스트 메뉴

**프로필 활성화 UseCase**:
```kotlin
class ActivateProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(profileId: String) {
        profileRepository.deactivateAll()       // 모든 프로필 isActive = false
        profileRepository.activate(profileId)   // 선택 프로필 isActive = true
    }
}
```

### 5.1.3 프로필 생성/편집 다이얼로그

```
┌──────────────────────────────────┐
│  새 프로필 만들기                 │
│                                  │
│  이름: [________________]        │
│                                  │
│  아이콘 선택:                     │
│  😊 😴 🎮 📺 📖 💼 🏃 ✍️       │
│  (가로 스크롤 이모지 피커)         │
│                                  │
│  [취소]           [만들기]        │
└──────────────────────────────────┘
```

### 5.1.4 프로필 상세/편집 화면 (ProfileEditScreen)

- 이름·아이콘 수정
- 감도(sensitivity) 슬라이더: 0.5x ~ 2.0x (0.1 단위)
- 전역 쿨다운 슬라이더: 0 ~ 2000ms (50ms 단위)
- 이 프로필의 트리거 목록 + '트리거 추가' 버튼

---

## 5.2 트리거 (Trigger)

**트리거 = '표정 조건' + '유지 시간' + '쿨다운' + '액션'**
특정 블렌드쉐이프 값이 임계값을 holdDurationMs 이상 지속하면 액션이 발동됩니다.

### 5.2.1 Trigger 속성 명세

| 속성 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | `String` (UUID) | PK, Not Null | 고유 식별자 |
| `profileId` | `String` (UUID) | FK → Profile.id, CASCADE DELETE | 소속 프로필 |
| `name` | `String` | 최대 50자 | 트리거 이름 (예: '오른쪽 윙크 → 뒤로가기') |
| `blendShape` | `String` | Not Null | 감지할 블렌드쉐이프 ID (예: `"eyeBlinkRight"`) |
| `threshold` | `Float` | 0.0 ~ 1.0, Not Null | 발동 임계값. EMA 적용 후 값 ≥ threshold면 홀드 카운트 시작. |
| `holdDurationMs` | `Int` | 0 ~ 2000, 기본값 200 | 표정을 이 시간(ms) 이상 유지해야 발동 |
| `cooldownMs` | `Int` | 100 ~ 10000, 기본값 1000 | 이 트리거 발동 후 재발동까지 최소 대기 시간 |
| `action` | `Action` | Not Null | 발동 시 수행할 액션 (08_data_model.md 참조) |
| `isEnabled` | `Boolean` | 기본값 true | 이 트리거 개별 활성화 여부 |
| `priority` | `Int` | 기본값 100 | 낮을수록 높은 우선순위. 복수 트리거 동시 발동 시 우선 실행. |

### 5.2.2 쿨다운 3종 세트 이해

```
┌──────────────── 시간 축 ────────────────────────────────────┐
│                                                              │
│  표정 시작    holdDurationMs 충족       표정 종료             │
│     ├──────────────┤─── 액션 발동! ────┤                    │
│                    ↓                                         │
│              개별 쿨다운 시작 (trigger.cooldownMs)           │
│              ├─────────────────────────────────┤            │
│              이 시간 동안 이 트리거 재발동 불가               │
│                                                              │
│              전역 쿨다운 시작 (profile.globalCooldownMs)     │
│              ├──────────┤                                    │
│              이 시간 동안 모든 트리거 발동 불가               │
└──────────────────────────────────────────────────────────────┘
```

| 쿨다운 종류 | 속성 위치 | 기본값 | 역할 |
|------------|----------|--------|------|
| 홀드 시간 | `Trigger.holdDurationMs` | 200ms | 단발성 표정 변화 오작동 방지 |
| 개별 쿨다운 | `Trigger.cooldownMs` | 1000ms | 같은 트리거 연속 발동 방지 |
| 전역 쿨다운 | `Profile.globalCooldownMs` | 300ms | 연속 트리거 발동으로 인한 혼란 방지 |

**권장 시작 설정**: holdDuration=200ms, 개별쿨다운=1000ms, 전역쿨다운=300ms

### 5.2.3 트리거 목록 화면 (TriggerListScreen)

특정 프로필 내 트리거 목록을 보여주는 화면입니다.

**UI 구성 요소**:
```
┌──────────────────────────────────────────────┐
│  😊 기본 프로필  [편집]                        │  ← 프로필 헤더
├──────────────────────────────────────────────┤
│  ● 오른쪽 윙크 → 뒤로가기         [●] [편집] │  ← 트리거 카드
│    eyeBlinkRight  0.6  홀드:300ms             │
├──────────────────────────────────────────────┤
│  ● 왼쪽 윙크 → 홈                [●] [편집] │
│    eyeBlinkLeft   0.6  홀드:300ms             │
├──────────────────────────────────────────────┤
│  ○ 입 벌리기 → 스크롤 위         [○] [편집] │  ← 비활성화된 트리거
│    jawOpen        0.5  홀드:200ms             │
└──────────────────────────────────────────────┘
                  [+ 트리거 추가]
```

### 5.2.4 트리거 편집 화면 (TriggerEditScreen)

새 트리거 생성 또는 기존 트리거 편집 화면입니다.

```
1. 트리거 이름
   [TextField: "오른쪽 윙크 → 뒤로가기"        ]

2. 표정 선택
   [카테고리 탭: 눈 | 입 | 눈썹 | 전체]
   ┌─────────────────┬─────────────────┐
   │ 👁 오른쪽 눈 깜빡  │ 👁 왼쪽 눈 깜빡 │  ← 카드 그리드
   │ (선택됨 ✓)        │                 │
   ├─────────────────┼─────────────────┤
   │ 👁 오른쪽 눈 크게  │ 👁 왼쪽 눈 크게  │
   └─────────────────┴─────────────────┘

3. 실시간 미리보기
   [현재값 게이지: 0.52  ██████░░░░  ← 실시간 업데이트]

4. 임계값
   [Slider 0.0 ──────●─────── 1.0]
                  ↑ 현재값 점선
   임계값: 0.60

5. 유지 시간 (홀드)
   [Slider 0ms ──●────── 2000ms]
   유지 시간: 300ms

6. 쿨다운
   [Slider 100ms ───●──── 5000ms]
   쿨다운: 1000ms

7. 액션 선택
   ┌──────────────────────────────────┐
   │ [시스템] [제스처] [앱/미디어]      │
   │                                  │
   │ ● 뒤로가기                        │  ← 현재 선택
   │ ○ 홈 버튼                         │
   │ ○ 최근 앱                         │
   │ ○ 화면 잠금                       │
   └──────────────────────────────────┘

8. [지금 테스트] 버튼
   → 선택된 임계값 기준으로 5초간 발동 테스트

9. [저장]  [취소]
```

### 5.2.5 TriggerEditViewModel 상태

```kotlin
data class TriggerEditUiState(
    val triggerId: String? = null,       // null이면 신규 생성
    val name: String = "",
    val selectedBlendShape: String? = null,
    val threshold: Float = 0.5f,
    val holdDurationMs: Int = 200,
    val cooldownMs: Int = 1000,
    val selectedAction: Action? = null,
    val currentBlendShapeValue: Float = 0f,  // 실시간 카메라 값
    val isTestMode: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)
```
