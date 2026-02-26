# MimicEase
## 표정 기반 안드로이드 접근성 앱 — 상세 기술 사양서

| 항목 | 내용 |
|------|------|
| 문서 버전 | v1.0 (초안) |
| 작성일 | 2026년 2월 25일 |
| 기반 라이브러리 | Google Project GameFace (Android) |
| 타겟 플랫폼 | Android 10 이상 (API 29+) |
| 개발 언어 | Kotlin / Jetpack Compose |

> 이 문서는 바이브코딩(Vibe Coding)을 위한 상세 사양서입니다.

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술 스택](#2-기술-스택)
3. [시스템 아키텍처](#3-시스템-아키텍처)
4. [핵심 기능 명세](#4-핵심-기능-명세)
5. [화면 설계](#5-화면-설계-uiux-specification)
6. [데이터 모델](#6-데이터-모델-data-model)
7. [접근성 서비스 구현](#7-접근성-서비스-구현)
8. [권한 요구사항](#8-권한-요구사항)
9. [성능 및 배터리 최적화](#9-성능-및-배터리-최적화)
10. [개발 로드맵](#10-개발-로드맵)
11. [테스트 전략](#11-테스트-전략)
12. [주요 구현 주의사항](#12-주요-구현-주의사항-implementation-notes)

---

## 1. 프로젝트 개요

### 1.1 앱 소개

MimicEase는 신체적 장애로 인해 스마트폰 터치스크린 조작이 어려운 사용자를 위한 안드로이드 접근성 애플리케이션입니다. Google의 오픈소스 프로젝트인 Project GameFace 라이브러리의 안드로이드 버전을 핵심 엔진으로 채택하여, 전면 카메라를 통해 실시간으로 사용자의 표정을 인식하고, 사전에 설정된 트리거와 액션 매핑에 따라 다양한 스마트폰 조작 기능을 수행합니다.

앱의 이름 'MimicEase'는 표정을 모방(Mimic)한다는 의미와 조작의 용이성(Ease)을 결합한 것으로, '표정으로 편안하게'라는 핵심 가치를 담고 있습니다.

**핵심 가치 제안:**

- **개인화**: 모든 사용자는 서로 다른 신체적 특성을 지닙니다. MimicEase는 사용자가 자신이 편안하게 지을 수 있는 표정을 탐색하고, 그 표정에 원하는 기능을 자유롭게 매핑할 수 있도록 합니다.
- **접근성**: 안드로이드 AccessibilityService 위에 구축되어 시스템 전반에 걸쳐 동작하며, 특정 앱에 종속되지 않습니다.
- **간결함**: 복잡한 설정 없이도 직관적인 온보딩 과정을 통해 빠르게 시작할 수 있습니다.
- **신뢰성**: 백그라운드 서비스로 안정적으로 운영되며, 배터리 효율과 성능 사이의 균형을 최우선으로 합니다.

### 1.2 대상 사용자

- 근위축증, 뇌성마비, 척수 손상 등으로 인해 손가락·손목의 정밀한 조작이 어려운 사용자
- 손을 전혀 사용할 수 없어 대체 입력 수단이 필요한 사용자
- 피로·떨림·통증 등으로 인해 장시간 스마트폰 조작이 힘든 사용자
- 기존 스위치 컨트롤, 음성 인식 등의 대체 입력 수단이 충분히 효과적이지 않은 사용자

### 1.3 기반 기술: Google Project GameFace

Google Project GameFace는 얼굴 랜드마크(Face Landmark) 및 블렌드쉐이프(BlendShape)를 활용하여 표정을 인식하는 오픈소스 접근성 프로젝트입니다. 원래 게임 조작을 위해 개발되었으나, MimicEase는 이를 일반 스마트폰 접근성 도구로 확장합니다.

| 항목 | 상세 내용 |
|------|-----------|
| 오픈소스 레포지토리 | github.com/google/project-gameface (Android 브랜치) |
| 핵심 기술 | MediaPipe Face Landmarker (52개 블렌드쉐이프) |
| 인식 대상 | 눈썹, 눈, 볼, 입술, 코 등 얼굴 근육 움직임 |
| 처리 방식 | 온디바이스 ML (서버 전송 없음, 개인정보 보호) |
| 지원 표정 예시 | 눈 깜빡임, 눈썹 올리기, 입 벌리기, 볼 부풀리기, 미소 짓기 등 |
| 라이선스 | Apache License 2.0 |

### 1.4 지원 블렌드쉐이프 목록

각 값은 `0.0 ~ 1.0` 범위의 float 값으로 표현됩니다.

| 블렌드쉐이프 ID | 설명 | 권장 트리거 강도 |
|----------------|------|----------------|
| `eyeBlinkLeft` / `eyeBlinkRight` | 왼쪽/오른쪽 눈 깜빡임 | 0.5 이상 |
| `eyeWideLeft` / `eyeWideRight` | 왼쪽/오른쪽 눈 크게 뜨기 | 0.6 이상 |
| `browInnerUp` | 눈썹 안쪽 올리기 | 0.4 이상 |
| `browOuterUpLeft` / `browOuterUpRight` | 눈썹 바깥쪽 올리기 | 0.5 이상 |
| `browDownLeft` / `browDownRight` | 눈썹 찌푸리기 | 0.5 이상 |
| `mouthSmileLeft` / `mouthSmileRight` | 미소 짓기 (좌/우) | 0.5 이상 |
| `mouthOpen` | 입 벌리기 | 0.5 이상 |
| `mouthPucker` | 입 모으기 (뽀뽀 표정) | 0.5 이상 |
| `mouthLeft` / `mouthRight` | 입 좌/우로 당기기 | 0.4 이상 |
| `cheekPuff` | 볼 부풀리기 | 0.5 이상 |
| `noseSneerLeft` / `noseSneerRight` | 코 찡그리기 | 0.4 이상 |
| `jawOpen` | 턱 벌리기 | 0.4 이상 |
| `tongueOut` | 혀 내밀기 | 0.6 이상 |

---

## 2. 기술 스택

### 2.1 개발 환경

| 분류 | 기술/도구 | 버전/비고 |
|------|-----------|-----------|
| 개발 언어 | Kotlin | 1.9 이상 |
| UI 프레임워크 | Jetpack Compose | Material 3 |
| 최소 SDK | Android API 29 | Android 10 |
| 타겟 SDK | Android API 35 | Android 15 |
| 빌드 도구 | Gradle (Kotlin DSL) | 8.x |
| IDE | Android Studio | Ladybug 이상 권장 |
| 아키텍처 패턴 | MVVM + Clean Architecture | 단방향 데이터 흐름 |

### 2.2 핵심 라이브러리 의존성

| 라이브러리 | 용도 | 추가 방법 |
|-----------|------|-----------|
| Google Project GameFace (Android) | 표정 인식 핵심 엔진 | 소스 모듈로 포함 |
| MediaPipe Face Landmarker | 얼굴 랜드마크 감지 | GameFace 내부 의존성 |
| Jetpack CameraX | 전면 카메라 프리뷰/캡처 | `implementation 'camera'` |
| Room Database | 프로필·트리거 로컬 저장 | `implementation 'room'` |
| Hilt (Dagger) | 의존성 주입(DI) | `implementation 'hilt'` |
| Kotlin Coroutines + Flow | 비동기 처리 및 상태 관리 | `implementation 'coroutines'` |
| DataStore (Preferences) | 앱 설정값 저장 | `implementation 'datastore'` |
| Accompanist Permissions | 런타임 권한 처리 | `implementation 'permissions'` |
| Timber | 로깅 | `implementation 'timber'` |

### 2.3 프로젝트 구조

Clean Architecture 원칙에 따라 3개의 레이어로 분리합니다:

```
app/
  ├── src/main/
  │   ├── java/com/mimicease/
  │   │   ├── core/                    # 공통 유틸, 확장함수, 상수
  │   │   │   ├── extensions/
  │   │   │   └── utils/
  │   │   ├── data/                    # Data Layer
  │   │   │   ├── local/               # Room DB, DataStore
  │   │   │   │   ├── database/
  │   │   │   │   ├── entity/
  │   │   │   │   └── dao/
  │   │   │   ├── repository/          # Repository 구현체
  │   │   │   └── model/               # Data Model (DTO)
  │   │   ├── domain/                  # Domain Layer
  │   │   │   ├── model/               # Domain Entity
  │   │   │   ├── repository/          # Repository 인터페이스
  │   │   │   └── usecase/             # Use Case
  │   │   ├── presentation/            # Presentation Layer
  │   │   │   ├── ui/                  # Composable 화면
  │   │   │   │   ├── onboarding/
  │   │   │   │   ├── home/
  │   │   │   │   ├── test/            # 표정 테스트 화면
  │   │   │   │   ├── profile/         # 프로필 관리
  │   │   │   │   ├── trigger/         # 트리거 설정
  │   │   │   │   └── settings/
  │   │   │   └── viewmodel/
  │   │   ├── service/                 # 접근성 서비스
  │   │   │   ├── MimicAccessibilityService.kt
  │   │   │   ├── FaceDetectionService.kt
  │   │   │   └── ActionExecutor.kt
  │   │   └── di/                      # Hilt 모듈
  │   ├── assets/
  │   │   └── face_landmarker.task      # MediaPipe 모델 파일
  │   └── res/
gameFace/                               # GameFace 라이브러리 모듈
```

---

## 3. 시스템 아키텍처

### 3.1 전체 시스템 흐름

```
전면 카메라 (CameraX)
     ↓  ImageProxy 프레임
GameFace FaceLandmarkerHelper
     ↓  FaceLandmarkerResult (52개 블렌드쉐이프 + 값)
ExpressionAnalyzer
     ↓  Map<BlendShape, Float> (현재 표정 상태)
TriggerMatcher
     ↓  매칭된 Trigger 목록 (쿨다운, 중복 제거 포함)
ActionExecutor
     ↓  AccessibilityService 노드 조작 / 시스템 API 호출
실제 액션 수행 (탭, 스와이프, 홈, 뒤로가기, 앱 실행 등)
```

### 3.2 컴포넌트 상세

#### 3.2.1 GameFace FaceLandmarkerHelper

Project GameFace에서 제공하는 핵심 클래스로, MediaPipe Face Landmarker 모델을 로드하고 카메라 프레임을 처리하여 52개의 블렌드쉐이프 값을 반환합니다.

- 모델 초기화: `assets/face_landmarker.task` 파일 로드
- 처리 모드: `LIVE_STREAM` (실시간 스트림)
- 출력: `FaceLandmarkerResult` → `blendshapes List<List<Category>>`
- 콜백: `onResults(result, inputImage, inferenceTime)` / `onError(error)`

#### 3.2.2 ExpressionAnalyzer

`FaceLandmarkerResult`를 받아 현재 표정 상태를 의미있는 형태로 변환합니다.

- 블렌드쉐이프 값을 Key-Value Map으로 변환
- 노이즈 필터링: EMA(Exponential Moving Average) 적용으로 떨림 제거
- 연속성 검사: N 프레임 연속 임계값 초과 시에만 표정으로 인식 (단발성 오트리거 방지)

#### 3.2.3 TriggerMatcher

현재 표정 상태와 활성화된 프로필의 트리거 목록을 비교하여 발동해야 할 액션을 결정합니다.

- 활성 프로필의 트리거 목록 구독 (Flow)
- 각 트리거의 표정 조건과 임계값(threshold) 비교
- 쿨다운(Cooldown) 관리: 동일 트리거의 연속 발동 방지
- 우선순위 처리: 복수의 트리거가 동시에 매칭될 경우 우선순위 적용

#### 3.2.4 ActionExecutor

TriggerMatcher에서 결정된 액션을 실제 안드로이드 시스템 기능으로 변환하여 실행합니다.

- `AccessibilityService.performGlobalAction()`으로 시스템 액션 수행
- `AccessibilityNodeInfo` 기반 화면 분석 및 클릭 수행
- `GestureDescription` API로 스와이프/드래그 제스처 실행
- `Intent`를 통한 앱 실행

### 3.3 백그라운드 서비스 아키텍처

두 개의 서비스로 구성됩니다:

| 서비스 | 역할 | 실행 방식 |
|--------|------|-----------|
| `MimicAccessibilityService` | 안드로이드 AccessibilityService 확장. 전체 시스템 컨트롤의 진입점. | 시스템에 등록된 접근성 서비스로 상시 실행 |
| `FaceDetectionForegroundService` | 카메라 스트림 유지 및 GameFace 엔진 실행. 포그라운드 알림 표시. | Foreground Service (알림 채널 필수) |

---

## 4. 핵심 기능 명세

### 4.1 온보딩 플로우

| 단계 | 화면 | 주요 내용 |
|------|------|-----------|
| Step 1 | 환영 화면 | 앱 소개, 핵심 가치 설명, 시작하기 버튼 |
| Step 2 | 권한 요청 | 카메라 권한 설명 및 요청, 접근성 서비스 활성화 안내 |
| Step 3 | 접근성 서비스 설정 | 시스템 설정으로 이동 유도, 돌아왔을 때 활성화 상태 확인 |
| Step 4 | 표정 테스트 소개 | 테스트 모드 사용법 설명, 직접 해보기로 이동 |
| Step 5 | 기본 프로필 생성 | 자동으로 추천 기본 프로필 생성 또는 직접 설정으로 이동 |

### 4.2 표정 테스트 모드 (Expression Test Mode)

사용자가 자신이 어떤 표정을 얼마나 명확하게 지을 수 있는지 실시간으로 확인하는 화면입니다.

#### 4.2.1 화면 구성

- 전면 카메라 프리뷰 (원형 또는 직사각형, 좌우 반전)
- 실시간 블렌드쉐이프 값 표시: 전체 52개 또는 주요 12개 선택 표시
- 표정별 게이지 바 (0.0 ~ 1.0 범위를 시각적으로 표현)
- 현재 가장 강하게 감지되는 표정 강조 표시 (Top 3)
- '이 표정 저장' 버튼으로 특정 표정의 감도 값을 트리거 설정으로 바로 연결

#### 4.2.2 기능 상세

1. 사용자가 카메라를 바라보면 실시간으로 블렌드쉐이프 값이 업데이트됩니다.
2. 각 표정 항목 옆에 현재 값(예: `0.73`)이 숫자로 표시되고, 게이지 바가 채워집니다.
3. 임계값 슬라이더를 드래그하여 트리거 발동 기준을 직접 테스트할 수 있습니다.
4. **녹화 모드**: 10초간 자신의 표정 변화를 기록하여 최대·평균·최소값을 확인합니다.
5. 표정 카테고리 탭: '눈', '입', '눈썹', '전체' 등 카테고리별 필터링 제공

> **구현 팁:** `FaceLandmarkerHelper`의 `onResults` 콜백에서 `blendshapes` 값을 Flow로 emit → `StateFlow<Map<String, Float>>`로 ViewModel이 UI에 전달 → `LazyColumn`으로 52개 항목 렌더링, 각 항목은 `LinearProgressIndicator`. 임계값 슬라이더는 Compose `Slider` composable (value: 0f ~ 1f, steps: 100).

### 4.3 프로필 관리 (Profile Management)

사용자는 여러 개의 프로필을 만들어 상황에 따라 전환할 수 있습니다. 예: '일반 사용', '영상 시청', '게임'.

#### 4.3.1 프로필 속성

| 속성 | 타입 | 설명 |
|------|------|------|
| `id` | UUID (String) | 프로필 고유 식별자 |
| `name` | String | 프로필 이름 (최대 30자) |
| `icon` | String | 이모지 또는 Material Icon 이름 |
| `isActive` | Boolean | 현재 활성화 여부 (한 번에 하나만 활성화) |
| `createdAt` | Long (timestamp) | 생성 일시 |
| `updatedAt` | Long (timestamp) | 최종 수정 일시 |
| `triggers` | List\<Trigger\> | 이 프로필에 속한 트리거 목록 (Room Relation) |
| `sensitivity` | Float (0.5~2.0) | 전역 감도 배율 (개별 트리거 임계값에 곱해짐) |
| `cooldownMs` | Int (ms) | 트리거 발동 후 전역 쿨다운 (기본값: 500ms) |

#### 4.3.2 프로필 관리 화면

- 프로필 목록 (카드 형태): 이름, 아이콘, 트리거 개수, 활성화 토글
- 프로필 추가: 이름·아이콘 설정 후 생성, 빈 트리거 목록으로 시작
- 프로필 복제: 기존 프로필을 복사하여 세부 수정
- 프로필 삭제: 확인 다이얼로그 후 삭제
- 프로필 전환: 홈 화면의 빠른 전환 또는 알림에서 직접 전환
- **기본 프로필**: 앱 설치 시 '기본' 프로필이 자동 생성됩니다

### 4.4 트리거 설정 (Trigger Configuration)

트리거는 '특정 표정을 특정 강도 이상으로 지을 때 발동'하는 규칙입니다.

#### 4.4.1 트리거 속성

| 속성 | 타입 | 설명 |
|------|------|------|
| `id` | UUID (String) | 트리거 고유 식별자 |
| `profileId` | UUID (String) | 소속 프로필 ID (FK) |
| `name` | String | 트리거 이름 (예: '오른쪽 눈 윙크 → 탭') |
| `blendShape` | String | 감지할 블렌드쉐이프 ID (예: `'eyeBlinkRight'`) |
| `threshold` | Float (0.0~1.0) | 발동 임계값. 이 값 이상이면 트리거 활성화. |
| `holdDurationMs` | Int | 표정을 이 시간(ms) 이상 유지해야 발동 (기본: 200ms) |
| `cooldownMs` | Int | 이 트리거 개별 쿨다운 (기본: 1000ms) |
| `action` | Action | 발동 시 수행할 액션 객체 |
| `isEnabled` | Boolean | 이 트리거 활성화 여부 |
| `priority` | Int | 복수 트리거 동시 발동 시 우선순위 (낮을수록 높음) |

#### 4.4.2 트리거 설정 화면 (Trigger Editor)

1. 표정 선택: 블렌드쉐이프 목록에서 선택 (카테고리별 탭 + 검색)
2. 실시간 미리보기: 선택한 표정의 현재 값이 게이지로 표시됨
3. 임계값 설정: 슬라이더로 0.0~1.0 사이 값 설정. 현재 값과의 비교선 표시.
4. 유지 시간 설정: 0ms ~ 2000ms 사이, 50ms 단위
5. 쿨다운 설정: 500ms ~ 5000ms 사이
6. 액션 연결: 이 트리거에 연결할 액션 선택 (4.5절 참조)
7. 테스트: '지금 테스트' 버튼으로 설정된 임계값 기준 발동 시뮬레이션

> **쿨다운 동작 방식**
> - **개별 쿨다운** (`Trigger.cooldownMs`): 해당 트리거가 발동된 후 같은 트리거가 다시 발동되기 전 최소 대기 시간.
> - **전역 쿨다운** (`Profile.cooldownMs`): 어떤 트리거든 발동된 후 모든 트리거가 잠시 대기하는 시간.
> - **홀드 시간** (`holdDurationMs`): 표정을 이 시간 이상 유지해야만 발동. 순간적인 표정 변화를 걸러냄.
> - **권장 시작값**: 홀드=200ms, 개별쿨다운=1000ms, 전역쿨다운=300ms

### 4.5 액션 설정 (Action Configuration)

#### 4.5.1 시스템 액션 (System Actions)

| 액션 ID | 액션 이름 | 구현 방법 | 필요 권한 |
|---------|----------|-----------|-----------|
| `GLOBAL_HOME` | 홈 버튼 | `performGlobalAction(GLOBAL_ACTION_HOME)` | AccessibilityService |
| `GLOBAL_BACK` | 뒤로가기 | `performGlobalAction(GLOBAL_ACTION_BACK)` | AccessibilityService |
| `GLOBAL_RECENTS` | 최근 앱 | `performGlobalAction(GLOBAL_ACTION_RECENTS)` | AccessibilityService |
| `GLOBAL_NOTIFICATIONS` | 알림 패널 열기 | `performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)` | AccessibilityService |
| `GLOBAL_QUICK_SETTINGS` | 빠른 설정 열기 | `performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)` | AccessibilityService |
| `SCREEN_LOCK` | 화면 잠금 | `performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)` | API 28+ |
| `TAKE_SCREENSHOT` | 스크린샷 | `performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)` | API 28+ |
| `POWER_DIALOG` | 전원 메뉴 | `performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)` | AccessibilityService |

#### 4.5.2 제스처 액션 (Gesture Actions)

| 액션 ID | 액션 이름 | 설명 | 구현 방법 |
|---------|----------|------|-----------|
| `TAP_CENTER` | 화면 중앙 탭 | 현재 화면 중앙 좌표 탭 | GestureDescription (단일 클릭) |
| `TAP_CUSTOM` | 커스텀 위치 탭 | 사용자 지정 x,y 좌표 탭 | GestureDescription + 좌표 설정 |
| `TAP_NODE` | 요소 탭 | 접근성 노드 기반 탭 | AccessibilityNodeInfo.performAction |
| `SWIPE_UP` | 위로 스와이프 | 화면을 위로 쓸기 (스크롤 다운) | GestureDescription (Path) |
| `SWIPE_DOWN` | 아래로 스와이프 | 화면을 아래로 쓸기 (스크롤 업) | GestureDescription (Path) |
| `SWIPE_LEFT` | 왼쪽 스와이프 | 다음 페이지/뒤로 | GestureDescription (Path) |
| `SWIPE_RIGHT` | 오른쪽 스와이프 | 이전 페이지/앞으로 | GestureDescription (Path) |
| `SCROLL_UP` | 위로 스크롤 | 접근성 노드 스크롤 업 | `ACTION_SCROLL_UP` |
| `SCROLL_DOWN` | 아래로 스크롤 | 접근성 노드 스크롤 다운 | `ACTION_SCROLL_DOWN` |
| `LONG_PRESS` | 길게 누르기 | 1초 길게 탭 | GestureDescription (duration: 1000ms) |
| `DRAG` | 드래그 | A 지점에서 B 지점으로 드래그 | GestureDescription (Path with pause) |
| `DOUBLE_TAP` | 두 번 탭 | 연속 두 번 탭 | GestureDescription (연속 두 stroke) |
| `PINCH_IN` | 핀치 인 (축소) | 두 손가락 모으기 제스처 | GestureDescription (멀티 스트로크) |
| `PINCH_OUT` | 핀치 아웃 (확대) | 두 손가락 벌리기 제스처 | GestureDescription (멀티 스트로크) |

#### 4.5.3 앱/미디어 액션 (App & Media Actions)

| 액션 ID | 액션 이름 | 설명 | 구현 방법 |
|---------|----------|------|-----------|
| `OPEN_APP` | 앱 열기 | 지정한 앱을 실행 | Intent(packageName) |
| `OPEN_APP_RECENT` | 최근 앱 열기 | 마지막으로 사용한 앱으로 전환 | ActivityManager 기반 |
| `MEDIA_PLAY_PAUSE` | 미디어 재생/일시정지 | 현재 재생 중인 미디어 제어 | AudioManager + KeyEvent |
| `MEDIA_NEXT` | 다음 트랙 | 미디어 다음 곡 | `KeyEvent.KEYCODE_MEDIA_NEXT` |
| `MEDIA_PREV` | 이전 트랙 | 미디어 이전 곡 | `KeyEvent.KEYCODE_MEDIA_PREVIOUS` |
| `VOLUME_UP` | 볼륨 업 | 미디어 볼륨 1 증가 | `AudioManager.adjustVolume` |
| `VOLUME_DOWN` | 볼륨 다운 | 미디어 볼륨 1 감소 | `AudioManager.adjustVolume` |
| `CUSTOM_SHORTCUT` | 커스텀 단축키 | 사용자 정의 인텐트 실행 | Intent (Custom URI/Action) |
| `MIMIC_PAUSE` | MimicEase 일시정지 | 서비스 일시적으로 비활성화 | 내부 서비스 명령 |

#### 4.5.4 제스처 액션 파라미터

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `x`, `y` | Float (0.0~1.0) | 화면 크기에 대한 상대적 좌표. 0.5, 0.5는 화면 중앙. |
| `startX`, `startY`, `endX`, `endY` | Float (0.0~1.0) | 드래그/스와이프의 시작·끝 좌표. |
| `duration` | Long (ms) | 제스처 지속 시간. 스와이프: 300ms, 드래그: 500ms 권장. |
| `packageName` | String | OPEN_APP 액션에서 실행할 앱의 패키지명. |
| `activityName` | String (Optional) | 특정 액티비티를 직접 지정할 경우. |

### 4.6 커스텀 좌표 설정 UI

`TAP_CUSTOM` 또는 `DRAG` 액션 설정 시 사용자가 직접 터치 위치를 지정하는 화면입니다.

- **화면 오버레이**: 반투명 오버레이 위에 격자가 표시되며, 터치한 위치에 마커가 생성됨
- **현재 화면 캡처 배경**: 실제 사용 화면을 배경으로 하여 직관적인 위치 선택
- **좌표 표시**: 선택한 좌표를 퍼센트(%)로 표시 (예: 중앙 탭 = 50%, 50%)
- **드래그 경로**: 시작점과 끝점 두 곳을 순서대로 탭하여 경로 설정
- **초기화 버튼**: 선택을 취소하고 다시 설정

---

## 5. 화면 설계 (UI/UX Specification)

### 5.1 네비게이션 구조

Bottom Navigation Bar, 총 4개 탭:

| 탭 | 아이콘 | 주요 기능 |
|---|--------|-----------|
| 홈 (Home) | `home` | 서비스 상태 확인, 활성 프로필 표시, 빠른 트리거 카드 |
| 테스트 (Test) | `face` | 표정 실시간 테스트 및 감도 확인 |
| 프로필 (Profiles) | `person` | 프로필 목록, 추가, 편집, 트리거 관리 |
| 설정 (Settings) | `settings` | 앱 환경설정, 권한 관리, 도움말 |

### 5.2 홈 화면

```
┌─────────────────────────────────────┐
│  ① 서비스 상태 카드                  │
│  ● 감지 중   [  일시정지  ]          │
│  FPS: 28   추론시간: 32ms            │
├─────────────────────────────────────┤
│  ② 활성 프로필 카드                  │
│  😊 기본 프로필   트리거 4개         │
│              [프로필 변경]            │
├─────────────────────────────────────┤
│  ③ 빠른 트리거 카드 목록             │
│  [오른쪽 눈 깜빡임  →  뒤로가기] ●  │
│  [입 벌리기        →  홈버튼   ] ●  │
│  [눈썹 올리기      →  스크롤 ↑ ] ●  │
│                   [전체 보기 →]      │
└─────────────────────────────────────┘
```

### 5.3 표정 테스트 화면

```
┌─────────────────────────────────────┐
│         ( 카메라 프리뷰 )            │
│              ◯ 얼굴                  │
├─────────────────────────────────────┤
│ [전체] [눈] [입] [눈썹] [기타]       │
├─────────────────────────────────────┤
│ 눈 깜빡임 (오른쪽)    0.73 ██████░░ │
│ 눈 깜빡임 (왼쪽)      0.12 █░░░░░░░ │
│ 눈썹 올리기           0.05 ░░░░░░░░ │
│ 미소 (오른쪽)         0.45 ███░░░░░ │
│ 입 벌리기             0.31 ██░░░░░░ │
├─────────────────────────────────────┤
│      [+ 이 표정으로 트리거 만들기]   │
└─────────────────────────────────────┘
```

### 5.4 트리거 편집 화면

1. 트리거 이름 입력 (TextField)
2. 표정 선택 (BlendShape Picker): 카드 그리드 또는 드롭다운
3. 실시간 미리보기: 선택한 표정의 현재 감지값 게이지 표시
4. 임계값 슬라이더: 0.0 ~ 1.0, 현재값 라인 오버레이
5. 유지 시간 설정 (0ms ~ 2000ms)
6. 쿨다운 설정 (500ms ~ 5000ms)
7. 액션 선택: 카테고리별 액션 목록 (시스템/제스처/앱)
8. 액션별 추가 파라미터 설정 UI (동적으로 표시)
9. 저장 / 취소 버튼

### 5.5 설정 화면

| 설정 항목 | 타입 | 설명 |
|----------|------|------|
| 카메라 위치 | Select | 전면/후면 (기본: 전면) |
| 표정 평활화(EMA) | Slider (0.1~0.9) | 값이 높을수록 부드럽지만 반응 느림 (기본: 0.5) |
| 연속 프레임 수 | Slider (1~10) | 표정 확정까지 연속 인식 프레임 수 (기본: 3) |
| 포그라운드 알림 | Toggle | 감지 중 상태 알림 표시 여부 |
| 알림 탭 동작 | Select | 알림 탭 시: 앱 열기 / 서비스 일시정지 |
| 배터리 최적화 | Toggle | 배터리 최적화 제외 요청 (화이트리스트) |
| 개발자 모드 | Toggle | FPS, 추론시간, 원시 블렌드쉐이프 값 표시 |
| 접근성 서비스 상태 | Info + Button | 현재 상태 표시 및 시스템 설정으로 이동 |
| 카메라 권한 | Info + Button | 현재 상태 표시 및 권한 요청 |
| 도움말 / 튜토리얼 | Button | 온보딩 재시작 |

---

## 6. 데이터 모델 (Data Model)

### 6.1 Room Database 스키마

#### 6.1.1 ProfileEntity

```kotlin
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,              // UUID
    val name: String,                        // 프로필 이름
    val icon: String,                        // 이모지 또는 아이콘 이름
    val isActive: Boolean,                   // 활성화 여부
    val sensitivity: Float = 1.0f,           // 감도 배율
    val globalCooldownMs: Int = 300,         // 전역 쿨다운(ms)
    val createdAt: Long,                     // 생성 타임스탬프
    val updatedAt: Long                      // 수정 타임스탬프
)
```

#### 6.1.2 TriggerEntity

```kotlin
@Entity(
    tableName = "triggers",
    foreignKeys = [ForeignKey(
        entity = ProfileEntity::class,
        parentColumns = ["id"],
        childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("profileId")]
)
data class TriggerEntity(
    @PrimaryKey val id: String,
    val profileId: String,                   // FK → ProfileEntity.id
    val name: String,
    val blendShape: String,                  // BlendShape 식별자
    val threshold: Float,                    // 0.0 ~ 1.0
    val holdDurationMs: Int = 200,
    val cooldownMs: Int = 1000,
    val actionType: String,                  // ActionType enum 이름
    val actionParams: String,                // JSON 직렬화된 파라미터
    val isEnabled: Boolean = true,
    val priority: Int = 100,
    val createdAt: Long,
    val updatedAt: Long
)
```

#### 6.1.3 ActionParams JSON 예시

```json
// TAP_CUSTOM
{ "x": 0.5, "y": 0.5 }

// SWIPE_UP
{ "startX": 0.5, "startY": 0.7, "endX": 0.5, "endY": 0.3, "durationMs": 300 }

// OPEN_APP
{ "packageName": "com.example.app", "activityName": null }

// DRAG
{ "startX": 0.2, "startY": 0.5, "endX": 0.8, "endY": 0.5, "durationMs": 500 }

// 파라미터 없는 액션 (GLOBAL_HOME 등)
{}
```

#### 6.1.4 AppSettings (DataStore Preferences)

```kotlin
data class AppSettings(
    val cameraFacing: Int = LENS_FACING_FRONT,
    val emaAlpha: Float = 0.5f,              // 표정 평활화 계수
    val consecutiveFrames: Int = 3,          // 표정 확정 프레임 수
    val showForegroundNotification: Boolean = true,
    val isDeveloperMode: Boolean = false,
    val isServiceEnabled: Boolean = false,
    val activeProfileId: String? = null,
    val onboardingCompleted: Boolean = false
)
```

### 6.2 Domain 모델

```kotlin
data class Profile(
    val id: String, val name: String, val icon: String,
    val isActive: Boolean, val sensitivity: Float,
    val globalCooldownMs: Int, val triggers: List<Trigger>
)

data class Trigger(
    val id: String, val profileId: String, val name: String,
    val blendShape: BlendShape,
    val threshold: Float, val holdDurationMs: Int,
    val cooldownMs: Int, val action: Action,
    val isEnabled: Boolean, val priority: Int
)

sealed class Action {
    object GlobalHome : Action()
    object GlobalBack : Action()
    object GlobalRecents : Action()
    data class TapCustom(val x: Float, val y: Float) : Action()
    data class SwipeUp(val duration: Long = 300L) : Action()
    data class SwipeDown(val duration: Long = 300L) : Action()
    data class SwipeLeft(val duration: Long = 300L) : Action()
    data class SwipeRight(val duration: Long = 300L) : Action()
    data class LongPress(val x: Float, val y: Float) : Action()
    data class Drag(
        val sx: Float, val sy: Float,
        val ex: Float, val ey: Float,
        val duration: Long
    ) : Action()
    data class OpenApp(val packageName: String) : Action()
    object MediaPlayPause : Action()
    object ScreenLock : Action()
    object TakeScreenshot : Action()
    object MimicPause : Action()
    // ... 기타
}
```

---

## 7. 접근성 서비스 구현

### 7.1 MimicAccessibilityService

#### 7.1.1 서비스 설정 (res/xml/accessibility_service_config.xml)

```xml
<accessibility-service
    android:accessibilityEventTypes="typeAllMask"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRequestTouchExplorationMode|
                               flagRetrieveInteractiveWindows"
    android:canPerformGestures="true"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100"
    android:settingsActivity=".presentation.ui.settings.SettingsActivity"
/>
```

#### 7.1.2 서비스 생명주기

| 콜백 | 설명 | MimicEase 처리 |
|------|------|----------------|
| `onServiceConnected()` | 서비스 활성화 완료 | FaceDetectionForegroundService 바인딩 시작. 코루틴 스코프 초기화. |
| `onAccessibilityEvent()` | 접근성 이벤트 수신 | 현재 포커스 노드 업데이트. 화면 상태 추적. |
| `onInterrupt()` | 서비스 중단 요청 | 카메라 스트림 일시 중지. |
| `onUnbind()` | 서비스 비활성화 | 모든 리소스 해제. 코루틴 취소. |

#### 7.1.3 제스처 실행 코드 패턴

```kotlin
// 탭 실행
fun executeTap(x: Float, y: Float) {
    val path = Path().apply { moveTo(x, y) }
    val stroke = GestureDescription.StrokeDescription(path, 0, 50)
    val gesture = GestureDescription.Builder().addStroke(stroke).build()
    dispatchGesture(gesture, null, null)
}

// 스와이프 실행
fun executeSwipe(sx: Float, sy: Float, ex: Float, ey: Float, duration: Long) {
    val path = Path().apply {
        moveTo(sx, sy)
        lineTo(ex, ey)
    }
    val stroke = GestureDescription.StrokeDescription(path, 0, duration)
    val gesture = GestureDescription.Builder().addStroke(stroke).build()
    dispatchGesture(gesture, null, null)
}

// 화면 절대 좌표 계산 (상대 좌표 0.0~1.0 → 픽셀)
fun relativeToAbsolute(relX: Float, relY: Float): Pair<Float, Float> {
    val dm = resources.displayMetrics
    return relX * dm.widthPixels to relY * dm.heightPixels
}
```

### 7.2 FaceDetectionForegroundService

1. `onCreate()`: CameraX 초기화, GameFace `FaceLandmarkerHelper` 생성
2. `onStartCommand()`: 포그라운드 알림 생성 및 표시 (알림 없이는 백그라운드 종료됨)
3. `ImageAnalysis.Analyzer` 설정: `FaceLandmarkerHelper.detectLiveStream()` 호출
4. `onResults` 콜백 → `ExpressionAnalyzer` → `TriggerMatcher` → `ActionExecutor` 순서로 처리
5. `onDestroy()`: CameraX 해제, FaceLandmarkerHelper 정리, 코루틴 취소

**포그라운드 서비스 알림 구성:**
- 알림 채널 ID: `"mimic_ease_service_channel"`
- 알림 우선순위: `PRIORITY_LOW` (상시 표시이므로 방해 최소화)
- 알림 텍스트: `"표정 감지 중 - [프로필 이름]"`
- 알림 액션 버튼: '일시정지' / '앱 열기'

### 7.3 ExpressionAnalyzer 상세

#### 7.3.1 EMA 필터 (노이즈 제거)

```kotlin
class ExpressionAnalyzer(private val alpha: Float = 0.5f) {
    private val smoothedValues = mutableMapOf<String, Float>()

    fun process(rawValues: Map<String, Float>): Map<String, Float> {
        rawValues.forEach { (key, newValue) ->
            val prev = smoothedValues[key] ?: newValue
            // EMA: smoothed = alpha * new + (1 - alpha) * prev
            smoothedValues[key] = alpha * newValue + (1 - alpha) * prev
        }
        return smoothedValues.toMap()
    }
}
// alpha가 낮을수록 더 부드럽지만 반응이 느림 (권장: 0.3 ~ 0.7)
```

#### 7.3.2 연속 프레임 확정 로직

```kotlin
class ConsecutiveFrameChecker(private val requiredFrames: Int = 3) {
    private val frameCounters = mutableMapOf<String, Int>()

    // 반환값: 이번 프레임에서 처음으로 확정된 표정 Set
    fun check(activeShapes: Set<String>): Set<String> {
        val confirmed = mutableSetOf<String>()
        val allKeys = frameCounters.keys + activeShapes

        allKeys.forEach { key ->
            if (key in activeShapes) {
                val count = (frameCounters[key] ?: 0) + 1
                frameCounters[key] = count
                // 정확히 requiredFrames번째 프레임에서만 emit
                if (count == requiredFrames) confirmed.add(key)
            } else {
                frameCounters.remove(key)
            }
        }
        return confirmed
    }
}
```

---

## 8. 권한 요구사항

### 8.1 AndroidManifest.xml 권한

| 권한 | 타입 | 필요 이유 |
|------|------|-----------|
| `CAMERA` | Dangerous (런타임) | 전면 카메라로 표정 감지 |
| `FOREGROUND_SERVICE` | Normal | 백그라운드 서비스 실행 |
| `FOREGROUND_SERVICE_CAMERA` | Normal (API 34+) | 카메라를 사용하는 포그라운드 서비스 |
| `RECEIVE_BOOT_COMPLETED` | Normal | 부팅 후 자동 서비스 시작 (선택사항) |
| `VIBRATE` | Normal | 트리거 발동 시 진동 피드백 |
| `WAKE_LOCK` | Normal | 화면 꺼짐 방지 옵션 (선택사항) |

> **접근성 서비스 특이사항**
> - `AccessibilityService`는 별도 권한 선언 없이 시스템이 제공하는 기능을 사용합니다.
> - 사용자가 **설정 > 접근성 > 다운로드된 앱 > MimicEase**에서 직접 활성화해야 합니다.

### 8.2 권한 처리 전략

1. 앱 최초 실행 시 온보딩 화면에서 카메라 권한 요청
2. 카메라 권한 거부 시: 기능 설명 후 재요청 또는 설정 화면으로 유도
3. 접근성 서비스 비활성화 상태 감지: `AccessibilityManager.getEnabledAccessibilityServiceList()`로 확인
4. 접근성 서비스 미활성화 시: `Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)` 딥링크 버튼 표시
5. 배터리 최적화 제외: `Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)` 실행

---

## 9. 성능 및 배터리 최적화

### 9.1 성능 목표

| 지표 | 목표값 | 측정 방법 |
|------|--------|-----------|
| 표정 감지 지연(Latency) | < 100ms (end-to-end) | 카메라 프레임 → 액션 실행 시간 |
| MediaPipe 추론 시간 | < 50ms (GPU 가속) | `FaceLandmarkerResult.inferenceTime()` |
| CPU 사용률 (백그라운드) | < 15% | Battery Historian / Android Profiler |
| 메모리 사용량 | < 150MB | Android Studio Memory Profiler |
| 배터리 소모 | < 5%/시간 | 실제 디바이스 측정 |
| 앱 시작 시간(Cold Start) | < 2초 | Jetpack Macrobenchmark |

### 9.2 최적화 전략

- **GPU 델리게이트 활성화**: MediaPipe FaceLandmarker의 `baseOptions`에 GPU 델리게이트 설정
- **카메라 해상도 제한**: 480x640 (또는 720p) 권장. 불필요한 고해상도 방지.
- **분석 FPS 제한**: `ImageAnalysis.Builder.setTargetFrameRate(15)` — 15fps도 충분
- **화면 꺼짐 시 분석 중단**: `ACTION_SCREEN_OFF` 브로드캐스트 감지 후 분석 일시정지
- **쿨다운 중 분석 스킵**: 전역 쿨다운 중에는 TriggerMatcher 처리 건너뜀
- **코루틴 디스패처 분리**: 카메라 처리는 `IO`, UI 업데이트는 `Main`

---

## 10. 개발 로드맵

### Phase 1: 기반 구축 (예상 3~4주)

- [ ] 프로젝트 초기 설정 (Gradle, Hilt, Room, Compose)
- [ ] Project GameFace 안드로이드 라이브러리 모듈 통합
- [ ] CameraX + FaceLandmarkerHelper 연동
- [ ] 기본 표정 테스트 화면 구현 (블렌드쉐이프 실시간 표시)
- [ ] Room DB 스키마 및 DAO 구현
- [ ] Repository 및 UseCase 구현
- [ ] DataStore 설정 저장 구현
- [ ] 기본 접근성 서비스 뼈대 구현

### Phase 2: 핵심 기능 (예상 3~4주)

- [ ] ExpressionAnalyzer (EMA 필터 + 연속 프레임 확정) 구현
- [ ] TriggerMatcher 구현 (임계값, 쿨다운, 우선순위)
- [ ] ActionExecutor 구현 (시스템 액션, 제스처 액션)
- [ ] FaceDetectionForegroundService 구현
- [ ] 프로필 CRUD UI 구현
- [ ] 트리거 편집 UI 구현
- [ ] 홈 화면 구현
- [ ] 온보딩 플로우 구현

### Phase 3: 완성 및 테스트 (예상 2~3주)

- [ ] 커스텀 좌표 설정 UI (오버레이) 구현
- [ ] 앱 열기 액션 + 설치된 앱 목록 UI 구현
- [ ] 설정 화면 완성
- [ ] 포그라운드 알림 완성 (액션 버튼 포함)
- [ ] 배터리 최적화 제외 요청 플로우
- [ ] 실제 기기 테스트 및 성능 최적화
- [ ] 접근성 감사 (TalkBack과의 호환성 확인)
- [ ] Play Store 배포 준비

---

## 11. 테스트 전략

### 11.1 단위 테스트

- **ExpressionAnalyzer**: 다양한 EMA alpha 값에 대한 수렴 테스트
- **TriggerMatcher**: 임계값 경계 조건, 쿨다운 타이밍, 우선순위 결정 테스트
- **ActionExecutor**: 각 액션 타입별 올바른 AccessibilityService 메서드 호출 검증 (Mock)
- **Repository**: Room DB CRUD 연산, Flow 방출 검증
- **UseCase**: 비즈니스 로직 단독 검증

### 11.2 통합 테스트

- FaceLandmarkerHelper + ExpressionAnalyzer 파이프라인 테스트 (샘플 이미지 사용)
- AccessibilityService + ActionExecutor 연동 테스트
- Room Migration 테스트 (스키마 버전 업 시)

### 11.3 UI 테스트 (Compose Testing)

- 온보딩 플로우 E2E 테스트
- 프로필 생성 / 트리거 추가 / 저장 플로우
- 표정 테스트 화면: 블렌드쉐이프 업데이트 시 UI 반응 확인

### 11.4 실제 기기 테스트

- 다양한 조명 환경에서의 표정 인식 정확도 테스트
- 안경 착용 / 마스크 착용 환경 (부분 얼굴) 테스트
- 다양한 피부 톤에서의 인식률 테스트
- 장시간(1시간 이상) 실행 시 배터리 소모 및 안정성 테스트
- 다양한 Android 버전 및 기기에서의 호환성 테스트

---

## 12. 주요 구현 주의사항 (Implementation Notes)

### 12.1 카메라 충돌 방지

FaceDetectionForegroundService가 카메라를 점유하는 동안, 사용자가 카메라 앱이나 영상통화 앱을 실행하면 충돌이 발생할 수 있습니다.

- CameraX의 `CameraSelector`를 통해 동시 사용 충돌 감지
- 다른 앱이 카메라를 점유할 경우 MimicEase 서비스 자동 일시정지
- 카메라 반환 시 자동 재시작 (`CameraAvailability` 콜백 활용)

### 12.2 화면 꺼짐(Screen Off) 처리

화면이 꺼진 상태에서는 리소스를 절약해야 합니다.

- `BroadcastReceiver`로 `ACTION_SCREEN_OFF` / `ACTION_SCREEN_ON` 감지
- 화면 꺼짐 시 카메라 분석 일시 중단 (카메라 바인딩 유지, 분석만 중단)
- 화면 켜짐 시 분석 자동 재개

### 12.3 TalkBack과의 공존

MimicEase는 TalkBack과 동시에 사용될 수 있어야 합니다.

- AccessibilityService 간 이벤트 충돌: MimicEase는 이벤트를 소비(consume)하지 않고 패스스루
- Touch Exploration 모드 활성화 시 제스처 처리 방식 변경 필요
- TalkBack 활성화 여부를 `AccessibilityManager`로 확인하여 필요 시 제스처 방식 조정

### 12.4 개인정보 보호

> ⚠️ **중요: 모든 처리는 온디바이스에서만 이루어집니다.**

- **온디바이스 처리**: 카메라 영상이나 표정 데이터는 절대 외부 서버로 전송되지 않습니다.
- **카메라 데이터 저장 없음**: 카메라 프레임은 처리 후 즉시 폐기됩니다.
- **사용 데이터 수집 없음**: 사용자의 표정 사용 이력, 액션 로그 등을 수집·저장하지 않습니다.
- **Play Store 개인정보처리방침**: 카메라 사용 목적을 명확히 기재해야 합니다.

---

*— 문서 끝 —*

*이 사양서는 MimicEase 프로젝트의 바이브코딩을 지원하기 위해 작성되었습니다.*
