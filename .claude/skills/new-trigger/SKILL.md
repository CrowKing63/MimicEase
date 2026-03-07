---
name: new-trigger
description: MimicEase에 새로운 얼굴 표정 트리거를 추가한다. 사용자가 BlendShape와 Action을 지정하면 Trigger 도메인 모델 생성, ActionSerializer 확인, TriggerEditScreen 연동까지 안내하거나 직접 구현한다.
---

사용자가 지정한 BlendShape와 Action으로 새 트리거를 추가하세요.

## 필수 확인 사항

### 1. BlendShape 선택
`app/src/main/java/com/mimicease/presentation/ui/common/BlendShapeUtils.kt`의
`BLENDSHAPE_DISPLAY_NAMES` 맵에서 유효한 BlendShape 키를 사용해야 합니다.
총 52개가 정의되어 있습니다.

### 2. Action 선택
`app/src/main/java/com/mimicease/domain/model/Action.kt`의 sealed class에서
사용할 Action 타입을 확인하세요. 새 Action 타입이라면 반드시:
- `ActionSerializer.kt`에 직렬화/역직렬화 등록
- `ModeManager.kt`에서 허용 여부 확인 (HEAD_MOUSE 모드에서 차단 여부)

### 3. Trigger 기본값
```kotlin
Trigger(
    id = 0L,                    // Room이 자동 생성
    profileId = <profileId>,
    blendShape = "<키>",
    threshold = 0.5f,           // 0.0~1.0
    holdDurationMs = 500L,      // 연속 감지 유지 시간 (ms)
    cooldownMs = 1000L,         // 쿨다운 (ms)
    priority = 0,               // 우선순위 (높을수록 먼저 평가)
    action = <Action>           // Action sealed class 인스턴스
)
```

### 4. 금지 사항
- **SystemClock 사용 금지**: `android.os.SystemClock` 대신 `System.currentTimeMillis()` 사용
- Domain 레이어에 Android import 금지

## 구현 흐름
1. `Action.kt`에서 원하는 Action 타입 확인
2. 새 Action 타입이면 `ActionSerializer.kt` 업데이트
3. `TriggerRepositoryImpl.kt`의 `insertTrigger()` 호출 (테스트용 코드 또는 UI 통해)
4. UI는 `TriggerEditScreen.kt` → `BlendShapePickerSheet` 경유

## 테스트
구현 후 반드시 실행:
```bash
./gradlew :app:test
```
`ActionSerializerTest.kt`, `TriggerMatcherTest.kt`가 통과해야 합니다.
