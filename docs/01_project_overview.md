> **[MimicEase 사양서 — 01/11]** 독립 작업 가능 단위
> **프로젝트**: Google Project GameFace(Android) 기반 표정 인식 안드로이드 접근성 앱
> **스택**: Kotlin + Jetpack Compose, API 29+, MediaPipe 온디바이스 ML
> **전체 목차**: [`docs/00_INDEX.md`](./00_INDEX.md)

---

# 01. 프로젝트 개요

## 1.1 앱 소개

MimicEase는 신체적 장애로 인해 스마트폰 터치스크린 조작이 어려운 사용자를 위한 안드로이드 접근성 앱입니다.

**핵심 동작 원리**: 전면 카메라 → 표정 인식(GameFace) → 트리거 매칭 → 액션 실행

앱 이름 'MimicEase'는 표정 모방(Mimic)과 조작 용이성(Ease)을 결합한 것으로, '표정으로 편안하게'라는 핵심 가치를 담습니다.

**핵심 가치 제안**

- **개인화**: 자신이 편안하게 지을 수 있는 표정을 탐색하고, 그 표정에 원하는 기능을 자유롭게 매핑
- **접근성**: Android `AccessibilityService` 위에 구축 → 시스템 전반에서 동작, 특정 앱에 종속되지 않음
- **간결함**: 직관적인 온보딩으로 빠른 시작
- **신뢰성**: 백그라운드 서비스로 안정 운영, 배터리 효율 최우선

## 1.2 대상 사용자

- 근위축증, 뇌성마비, 척수 손상 등으로 손가락·손목 정밀 조작이 어려운 사용자
- 손을 전혀 사용할 수 없어 대체 입력 수단이 필요한 사용자
- 피로·떨림·통증으로 장시간 스마트폰 조작이 힘든 사용자
- 기존 스위치 컨트롤, 음성 인식 등이 충분히 효과적이지 않은 사용자

## 1.3 기반 기술: Google Project GameFace

| 항목 | 상세 내용 |
|------|-----------|
| 오픈소스 레포지토리 | `github.com/google/project-gameface` (Android 브랜치) |
| 핵심 기술 | MediaPipe Face Landmarker (52개 블렌드쉐이프) |
| 인식 대상 | 눈썹, 눈, 볼, 입술, 코 등 얼굴 근육 움직임 |
| 처리 방식 | **온디바이스 ML** — 서버 전송 없음, 개인정보 완전 보호 |
| 라이선스 | Apache License 2.0 |

**통합 방식**: GameFace를 별도 Gradle 모듈(`gameFace/`)로 소스 포함. `FaceLandmarkerHelper` 클래스가 핵심 진입점.

## 1.4 지원 블렌드쉐이프 전체 목록

각 값은 `0.0 ~ 1.0` 범위의 Float. `FaceLandmarkerResult.blendshapes()`로 접근.

| 블렌드쉐이프 ID | 설명 | 권장 트리거 임계값 |
|----------------|------|------------------|
| `eyeBlinkLeft` | 왼쪽 눈 깜빡임 | 0.5 |
| `eyeBlinkRight` | 오른쪽 눈 깜빡임 | 0.5 |
| `eyeWideLeft` | 왼쪽 눈 크게 뜨기 | 0.6 |
| `eyeWideRight` | 오른쪽 눈 크게 뜨기 | 0.6 |
| `eyeSquintLeft` | 왼쪽 눈 찡그리기 | 0.5 |
| `eyeSquintRight` | 오른쪽 눈 찡그리기 | 0.5 |
| `eyeLookUpLeft` | 왼쪽 눈 위로 | 0.4 |
| `eyeLookUpRight` | 오른쪽 눈 위로 | 0.4 |
| `eyeLookDownLeft` | 왼쪽 눈 아래로 | 0.4 |
| `eyeLookDownRight` | 오른쪽 눈 아래로 | 0.4 |
| `eyeLookInLeft` | 왼쪽 눈 안쪽으로 | 0.4 |
| `eyeLookInRight` | 오른쪽 눈 안쪽으로 | 0.4 |
| `eyeLookOutLeft` | 왼쪽 눈 바깥으로 | 0.4 |
| `eyeLookOutRight` | 오른쪽 눈 바깥으로 | 0.4 |
| `browInnerUp` | 눈썹 안쪽 올리기 | 0.4 |
| `browOuterUpLeft` | 왼쪽 눈썹 바깥쪽 올리기 | 0.5 |
| `browOuterUpRight` | 오른쪽 눈썹 바깥쪽 올리기 | 0.5 |
| `browDownLeft` | 왼쪽 눈썹 찌푸리기 | 0.5 |
| `browDownRight` | 오른쪽 눈썹 찌푸리기 | 0.5 |
| `mouthSmileLeft` | 왼쪽 미소 | 0.5 |
| `mouthSmileRight` | 오른쪽 미소 | 0.5 |
| `mouthFrownLeft` | 왼쪽 입꼬리 내리기 | 0.5 |
| `mouthFrownRight` | 오른쪽 입꼬리 내리기 | 0.5 |
| `mouthOpen` | 입 벌리기 | 0.5 |
| `mouthPucker` | 입 모으기 (뽀뽀 표정) | 0.5 |
| `mouthLeft` | 입 왼쪽으로 당기기 | 0.4 |
| `mouthRight` | 입 오른쪽으로 당기기 | 0.4 |
| `mouthRollLower` | 아랫입술 안으로 말기 | 0.4 |
| `mouthRollUpper` | 윗입술 안으로 말기 | 0.4 |
| `mouthShrugLower` | 아랫입술 내밀기 | 0.4 |
| `mouthShrugUpper` | 윗입술 내밀기 | 0.4 |
| `mouthClose` | 입 꼭 다물기 | 0.4 |
| `mouthDimpleLeft` | 왼쪽 보조개 | 0.4 |
| `mouthDimpleRight` | 오른쪽 보조개 | 0.4 |
| `mouthStretchLeft` | 왼쪽 입 늘리기 | 0.4 |
| `mouthStretchRight` | 오른쪽 입 늘리기 | 0.4 |
| `mouthPressLeft` | 왼쪽 입술 누르기 | 0.4 |
| `mouthPressRight` | 오른쪽 입술 누르기 | 0.4 |
| `mouthLowerDownLeft` | 왼쪽 아랫입술 내리기 | 0.4 |
| `mouthLowerDownRight` | 오른쪽 아랫입술 내리기 | 0.4 |
| `mouthUpperUpLeft` | 왼쪽 윗입술 올리기 | 0.4 |
| `mouthUpperUpRight` | 오른쪽 윗입술 올리기 | 0.4 |
| `cheekPuff` | 볼 부풀리기 | 0.5 |
| `cheekSquintLeft` | 왼쪽 볼 찡그리기 | 0.4 |
| `cheekSquintRight` | 오른쪽 볼 찡그리기 | 0.4 |
| `noseSneerLeft` | 왼쪽 코 찡그리기 | 0.4 |
| `noseSneerRight` | 오른쪽 코 찡그리기 | 0.4 |
| `jawOpen` | 턱 벌리기 | 0.4 |
| `jawLeft` | 턱 왼쪽으로 | 0.4 |
| `jawRight` | 턱 오른쪽으로 | 0.4 |
| `jawForward` | 턱 앞으로 | 0.4 |
| `tongueOut` | 혀 내밀기 | 0.6 |

> **참고**: 모든 블렌드쉐이프는 `FaceLandmarkerResult.blendshapes()[0][index].score()`로 접근. GameFace 내부에서 이미 Map 형태로 변환하는 헬퍼를 제공함.
