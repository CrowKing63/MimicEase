# MimicEase 사양서 — 전체 목차

> **프로젝트 한 줄 요약**: Google Project GameFace(Android) 기반으로, 전면 카메라가 사용자 표정을 인식하여 스마트폰을 조작하는 안드로이드 접근성 앱 (Kotlin + Jetpack Compose, API 29+, 온디바이스 ML)

---

## 문서 목록

| 파일 | 내용 | 주요 키워드 |
|------|------|------------|
| [01_project_overview.md](./01_project_overview.md) | 프로젝트 목적, 대상 사용자, GameFace 기반 기술, 52개 블렌드쉐이프 전체 목록 | BlendShape, MediaPipe, 접근성 |
| [02_tech_stack.md](./02_tech_stack.md) | 개발 환경, 라이브러리 의존성, Clean Architecture 프로젝트 디렉터리 구조 | Kotlin, Hilt, Room, CameraX, Compose |
| [03_architecture.md](./03_architecture.md) | 전체 데이터 흐름, 4개 핵심 컴포넌트 상세, 2개 서비스 구조 | ExpressionAnalyzer, TriggerMatcher, ActionExecutor |
| [04_expression_test.md](./04_expression_test.md) | 온보딩 플로우(5단계), 표정 테스트 모드 화면·기능 | 온보딩, 실시간 감지, 게이지 바 |
| [05_profile_trigger.md](./05_profile_trigger.md) | 프로필 CRUD, 트리거 속성·설정 화면, 쿨다운 동작 방식 | Profile, Trigger, Threshold, Cooldown |
| [06_actions.md](./06_actions.md) | 시스템·제스처·앱/미디어 액션 전체 목록, 파라미터 명세, 커스텀 좌표 설정 UI | GestureDescription, AccessibilityService, Intent |
| [07_ui_screens.md](./07_ui_screens.md) | 4개 탭 네비게이션, 홈·테스트·트리거 편집·설정 화면 레이아웃 | Bottom Navigation, Compose, UI Layout |
| [08_data_model.md](./08_data_model.md) | Room Entity, DataStore, Domain 모델, Action sealed class, JSON 파라미터 구조 | ProfileEntity, TriggerEntity, Action, Room DB |
| [09_accessibility_service.md](./09_accessibility_service.md) | 서비스 설정 XML, 생명주기, 제스처 실행 코드, EMA 필터, 연속 프레임 확정 로직 | AccessibilityService, GestureDescription, EMA |
| [10_permissions_performance.md](./10_permissions_performance.md) | 권한 목록·처리 전략, 성능 목표 수치, 배터리 최적화 전략 | CAMERA, ForegroundService, GPU Delegate |
| [11_roadmap_testing_notes.md](./11_roadmap_testing_notes.md) | 3단계 개발 로드맵, 테스트 전략, 카메라 충돌·TalkBack·개인정보 주의사항 | Phase 1~3, Unit Test, TalkBack, Privacy |

---

## 빠른 참조: 핵심 구조

```
전면 카메라
  → FaceLandmarkerHelper (GameFace)
  → ExpressionAnalyzer (EMA 필터 + 연속 프레임)
  → TriggerMatcher (임계값 + 쿨다운)
  → ActionExecutor
  → 실제 액션 수행
```

**주요 데이터 타입**

- `BlendShape`: `eyeBlinkRight`, `mouthSmileLeft` 등 52개 String ID
- `Trigger`: blendShape + threshold(0.0~1.0) + holdDurationMs + cooldownMs + Action
- `Action`: sealed class — `GlobalBack`, `TapCustom(x,y)`, `SwipeUp`, `OpenApp(pkg)` 등
- `Profile`: 복수의 Trigger를 담는 그룹. 한 번에 하나만 활성화.

**2개 서비스**

- `MimicAccessibilityService` — 시스템 접근성 서비스, 항상 실행
- `FaceDetectionForegroundService` — 카메라 스트림 유지, Foreground Service

---

## 파일 작업 시 참고사항

각 파일은 이 인덱스를 읽지 않아도 **독립적으로 작업 가능**하도록 설계되었습니다.
모든 파일 상단에 프로젝트 컨텍스트 요약이 포함되어 있습니다.
