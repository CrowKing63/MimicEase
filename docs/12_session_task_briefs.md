# MimicEase Session Task Briefs

이 문서는 컨택스트 프리하게 바로 투입할 수 있는 세션 단위 작업 지시서 모음이다.
원칙은 다음과 같다.

- 한 세션에는 한 작업만 수행한다.
- 작업자는 문서에 적힌 범위 밖의 리팩터링을 임의로 넓히지 않는다.
- 각 작업은 코드 수정, 검증, 결과 보고까지 한 번에 끝내는 것을 목표로 한다.
- 결과 보고에는 변경 파일, 테스트 결과, 남은 리스크를 반드시 포함한다.

## Session 01. API 33 Accessibility Crash Fix

### 멘션용 한 줄
`Session 01 수행: Android 13(API 33)에서 MimicAccessibilityService 연결 시 발생 가능한 setMotionEventSources 크래시를 수정하고 검증해줘.`

### 배경
`MimicAccessibilityService`는 마우스 입력 가로채기를 위해 `setMotionEventSources(InputDevice.SOURCE_MOUSE)`를 호출한다. 현재 API 가드가 33 기준으로 잡혀 있지만, 이 메서드는 API 34 메서드다.

### 목표
- Android 13(API 33)에서 크래시 가능성을 제거한다.
- Android 14+(API 34+)에서는 기존 동작을 유지한다.

### 수정 범위
- `app/src/main/java/com/mimicease/service/MimicAccessibilityService.kt`
- 필요 시 관련 주석만 함께 수정

### 해야 할 일
- 잘못된 SDK 가드를 수정한다.
- lint의 `NewApi` 오류가 사라지도록 정리한다.
- 동작 의도를 주석으로 짧게 남긴다.

### 완료 기준
- `:app:test` 통과
- `:app:lintDebug`에서 해당 `NewApi` 이슈 해소
- 변경 요약에 API 33/34 동작 차이를 명시

### 범위 밖
- 마우스 입력 설계 자체 변경
- 접근성 서비스 바인딩 구조 변경

## Session 02. Consecutive Frames Setting Restoration

### 멘션용 한 줄
`Session 02 수행: consecutiveFrames 설정이 실제 표정 확정 로직에 반영되도록 복구하고 테스트까지 마무리해줘.`

### 배경
설정 화면과 서비스는 `consecutiveFrames` 값을 주입하고 있지만, `ExpressionAnalyzer` 구현은 EMA만 수행하고 연속 프레임 확정 로직을 사용하지 않는다.

### 목표
- 사용자가 설정한 `consecutiveFrames`가 실제 트리거 발동 안정성에 영향을 주도록 만든다.
- 기존 EMA 동작은 유지한다.

### 수정 범위
- `app/src/main/java/com/mimicease/service/ExpressionAnalyzer.kt`
- `app/src/main/java/com/mimicease/service/FaceDetectionForegroundService.kt`
- `app/src/test/java/com/mimicease/ExpressionAnalyzerTest.kt`
- 필요 시 `TriggerMatcherTest.kt`

### 해야 할 일
- 연속 프레임 확정 규칙을 명시적으로 구현한다.
- 현재 서비스 파이프라인에 맞게 반환 타입과 호출 지점을 정리한다.
- 설정값이 1일 때와 3 이상일 때 동작 차이가 테스트로 보이게 한다.

### 완료 기준
- 단위 테스트가 연속 프레임 게이트를 검증한다.
- `:app:test` 통과
- 결과 보고에 “EMA”와 “연속 프레임 게이트”가 어떻게 분리됐는지 설명

### 범위 밖
- TriggerMatcher의 hold/cooldown 정책 전면 개편
- 새로운 설정 추가

## Session 03. Active Profile Null Safety

### 멘션용 한 줄
`Session 03 수행: 활성 프로필이 없어졌을 때 이전 트리거가 계속 남지 않도록 서비스 상태를 정리해줘.`

### 배경
`FaceDetectionForegroundService`는 활성 프로필이 있을 때만 `TriggerMatcher`를 새로 만든다. 활성 프로필이 삭제되거나 비활성화되면 직전 프로필의 트리거가 남을 수 있다.

### 목표
- 활성 프로필이 없을 때 트리거 실행이 완전히 비활성화되도록 한다.
- 알림 문구와 내부 상태도 함께 정리한다.

### 수정 범위
- `app/src/main/java/com/mimicease/service/FaceDetectionForegroundService.kt`
- 필요 시 `HomeViewModel.kt`, `HomeScreen.kt`

### 해야 할 일
- 활성 프로필이 `null`인 경우 matcher/state를 초기화한다.
- 알림에 프로필 없음 상태가 분명히 드러나게 한다.
- 관련 회귀 시나리오를 테스트 또는 수동 검증 절차로 정리한다.

### 완료 기준
- 활성 프로필 삭제 후 이전 액션이 더 이상 실행되지 않음
- `:app:test` 통과
- 결과 보고에 재현 절차와 수정 후 기대 동작 포함

### 범위 밖
- 프로필 UX 전면 개편
- 프로필 저장소 구조 변경

## Session 04. Dwell Click Toggle Wiring

### 멘션용 한 줄
`Session 04 수행: HEAD_MOUSE의 dwell click on/off 설정이 실제 런타임 동작에 반영되도록 연결해줘.`

### 배경
설정 화면에는 dwell click 활성/비활성 토글이 있지만 서비스는 매 프레임 dwell 업데이트를 계속 수행한다.

### 목표
- `dwellClickEnabled=false`면 dwell click이 절대 발동하지 않게 한다.
- `true`일 때만 기존 로직이 동작하게 한다.

### 수정 범위
- `app/src/main/java/com/mimicease/service/FaceDetectionForegroundService.kt`
- `app/src/main/java/com/mimicease/service/DwellClickController.kt`
- 필요 시 테스트 추가

### 해야 할 일
- 설정값을 서비스 루프에 반영한다.
- 비활성 상태에서 progress 표시 정책도 정리한다.
- 수동 검증 방법을 결과에 적는다.

### 완료 기준
- 설정 off 시 dwell click 미발동
- 설정 on 시 기존처럼 발동
- 테스트 또는 수동 검증 절차 포함

### 범위 밖
- dwell 알고리즘 자체 튜닝
- head mouse 감도 체계 변경

## Session 05. Unified Service State Cleanup

### 멘션용 한 줄
`Session 05 수행: MimicEase 서비스의 Running/Paused/Stopped 상태를 단일 기준으로 정리해서 Home, QS 타일, 부팅, 접근성 바인딩이 같은 상태를 보게 해줘.`

### 배경
현재 서비스 상태는 DataStore의 `isServiceEnabled`, 서비스의 `_isPaused`, `_instance`, 접근성 서비스 바인딩 여부가 섞여 있다. 화면과 실제 런타임이 다르게 보일 수 있다.

### 목표
- 단일 상태 모델을 도입하거나 이에 준하는 일관된 판정 체계를 만든다.
- Home, QS tile, boot auto-start, accessibility bind 경로가 같은 상태를 보게 한다.

### 수정 범위
- `app/src/main/java/com/mimicease/service/FaceDetectionForegroundService.kt`
- `app/src/main/java/com/mimicease/service/MimicAccessibilityService.kt`
- `app/src/main/java/com/mimicease/service/MimicToggleTileService.kt`
- `app/src/main/java/com/mimicease/service/BootCompletedReceiver.kt`
- `app/src/main/java/com/mimicease/presentation/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/mimicease/data/local/AppSettingsDataStore.kt`
- `app/src/main/java/com/mimicease/data/repository/SettingsRepositoryImpl.kt`

### 해야 할 일
- 상태 소스 오브 트루스를 정한다.
- stop/pause/resume/start 각각이 어떤 상태를 남겨야 하는지 명확히 정의한다.
- 최소한 다음 불일치를 없앤다:
  - 접근성 서비스가 서비스 시작했는데 Home은 stopped로 보이는 문제
  - boot auto-start 후 UI 상태 불일치
  - QS tile이 실제 상태와 다르게 보이는 문제

### 완료 기준
- 상태 전이 표를 결과 보고에 포함
- 관련 경로 수동 검증 절차 포함
- `:app:test`와 `:app:lintDebug` 통과

### 범위 밖
- 전체 아키텍처 재작성
- Compose UI 디자인 개편

## Session 06. Broadcast and External Automation Reliability

### 멘션용 한 줄
`Session 06 수행: 외부 브로드캐스트/딥링크/QS 타일 토글이 cold start 상황에서도 신뢰성 있게 동작하도록 정리해줘.`

### 배경
현재 `ToggleBroadcastReceiver`는 `MimicAccessibilityService.instance`가 없으면 바로 실패한다. 외부 자동화가 앱이 떠 있지 않은 상태에서 취약하다.

### 목표
- cold start 또는 부분 초기화 상태에서도 실패 모드를 명확히 한다.
- 가능한 범위에서 외부 자동화 성공률을 높인다.

### 수정 범위
- `app/src/main/java/com/mimicease/service/ToggleBroadcastReceiver.kt`
- `app/src/main/java/com/mimicease/ToggleActionActivity.kt`
- `app/src/main/java/com/mimicease/service/MimicToggleTileService.kt`
- 필요 시 `FaceDetectionForegroundService.kt`, `MimicAccessibilityService.kt`

### 해야 할 일
- 외부 트리거가 실패하는 조건을 코드로 줄이거나 사용자에게 명확한 경로를 제공한다.
- 브로드캐스트 보안 정책도 함께 검토하고 결과에 남긴다.
- 구현 선택 이유를 결과 보고에 적는다.

### 완료 기준
- cold start 시나리오에 대한 동작 정의 존재
- 실패 시 로그/처리 흐름이 명확함
- 수동 검증 절차 포함

### 범위 밖
- 완전한 백그라운드 자동화 플랫폼 구축
- 외부 앱 연동 문서 대규모 작성

## Session 07. Action Deserialization Fail-Safe

### 멘션용 한 줄
`Session 07 수행: ActionSerializer의 알 수 없는 액션 타입 처리 방식을 fail-open에서 fail-safe로 바꾸고 회귀 테스트를 추가해줘.`

### 배경
현재 알 수 없는 액션 타입은 `Action.GlobalHome`으로 역직렬화된다. 데이터 손상이나 마이그레이션 누락 시 의도치 않은 홈 이동이 발생할 수 있다.

### 목표
- 알 수 없는 액션 타입이 위험한 기본 동작으로 실행되지 않게 한다.
- 이전 데이터와의 호환성은 가능한 범위에서 보존한다.

### 수정 범위
- `app/src/main/java/com/mimicease/data/model/ActionSerializer.kt`
- 관련 테스트 파일
- 필요 시 `Action.kt`

### 해야 할 일
- 안전한 fallback 정책을 설계한다.
- 기존 마이그레이션 예외(`DragStartAtCursor`, `DragEndAtCursor`)는 유지한다.
- 예기치 않은 액션 데이터가 UI/런타임에서 어떻게 보일지 정리한다.

### 완료 기준
- 새로운 fallback 정책이 테스트로 고정됨
- 기존 정상 액션 직렬화 테스트 유지
- 결과 보고에 데이터 호환성 영향 포함

### 범위 밖
- 전체 저장 포맷 교체
- Room 스키마 버전 업을 수반하는 대규모 마이그레이션

## Session 08. Lint Debt Triage and Quality Gate Step 1

### 멘션용 한 줄
`Session 08 수행: 현재 lint 리포트를 분석해서 즉시 고쳐야 할 실제 결함과 무시 가능한 노이즈를 분리하고, 1차 품질 게이트 개선까지 해줘.`

### 배경
현재 lint는 `abortOnError=false`이며 CI도 `continue-on-error`다. 리포트에는 다수의 번역/정책/호환성 이슈가 섞여 있다.

### 목표
- lint 이슈를 카테고리별로 정리한다.
- 실제 릴리스 리스크 이슈와 번역/정리 이슈를 분리한다.
- 바로 올릴 수 있는 최소 품질 게이트 개선안을 적용한다.

### 수정 범위
- `app/build.gradle.kts`
- `.github/workflows/android.yml`
- 필요 시 lint로 잡힌 소수의 실제 결함 파일

### 해야 할 일
- 현재 lint 리포트를 요약한다.
- 즉시 수정 가능한 high-signal 이슈를 우선 해결한다.
- CI에서 적어도 치명 이슈는 놓치지 않도록 개선한다.

### 완료 기준
- 결과 보고에 lint 이슈 분류표 포함
- CI/lint 정책 변경 내용 명시
- 팀이 다음 세션에서 처리할 lint backlog가 남아 있어도 구조가 명확함

### 범위 밖
- 247개 번역 누락을 한 세션에 모두 해결
- 디자인/문구 전체 재작성

## Session 09. Missing Translation Backlog Batch 1

### 멘션용 한 줄
`Session 09 수행: lint의 MissingTranslation 중 사용자 핵심 흐름에 걸린 문자열부터 1차 배치로 정리하고 검증해줘.`

### 배경
다국어 리소스 파일은 많지만 핵심 흐름 문자열 번역 누락이 많아 lint가 대량 발생한다.

### 목표
- 온보딩, 홈, 설정, 프로필/트리거 핵심 흐름 문자열부터 우선 보완한다.
- 한 세션에 끝날 수 있도록 범위를 제한한다.

### 수정 범위
- `app/src/main/res/values*/strings.xml`

### 해야 할 일
- 우선순위:
  - 홈
  - 온보딩
  - 설정
  - 프로필/트리거
- 번역 누락만 메우고 의미 변경은 최소화한다.
- lint 결과에서 `MissingTranslation` 감소 수치를 보고한다.

### 완료 기준
- 대상 화면의 번역 누락 정리
- `:app:lintDebug` 재실행
- 남은 번역 backlog 수치 보고

### 범위 밖
- 전체 언어 품질 감수
- 문체 통일 프로젝트

## Session 10. App Picker Package Visibility Compliance

### 멘션용 한 줄
`Session 10 수행: 앱 실행 액션용 App Picker가 Android 11+ 패키지 가시성 정책에 맞게 동작하도록 정리해줘.`

### 배경
`TriggerEditScreen`의 앱 선택기는 `queryIntentActivities()`를 사용하지만 manifest의 `queries` 선언이 없다. 일부 기기에서 결과가 제한될 수 있다.

### 목표
- App Picker가 정책적으로 올바른 방식으로 동작하게 한다.
- 필요한 최소한의 manifest 선언 또는 대체 구현을 적용한다.

### 수정 범위
- `app/src/main/java/com/mimicease/presentation/ui/profile/TriggerEditScreen.kt`
- `app/src/main/AndroidManifest.xml`

### 해야 할 일
- 현재 사용 시나리오에 필요한 package visibility 범위를 정의한다.
- 과도한 조회 권한 없이 구현한다.
- lint의 `QueryPermissionsNeeded`를 해소하거나 정당한 이유로 억제한다.

### 완료 기준
- App Picker 동작 정책이 결과 보고에 정리됨
- 관련 lint 이슈 처리
- 수동 검증 절차 포함

### 범위 밖
- 앱 실행 액션 UI 전면 개편

## Session 11. Dead Setting and Metric Cleanup

### 멘션용 한 줄
`Session 11 수행: 현재 노출돼 있지만 실제로 쓰이지 않거나 값이 고정인 설정/메트릭을 정리해줘.`

### 배경
예를 들어 `notificationTapAction`은 저장 모델에 있지만 사용처가 없고, 홈의 `currentFps`는 항상 0으로 보인다.

### 목표
- 죽은 설정/메트릭을 제거하거나 실제로 연결한다.
- 사용자와 개발자 모두에게 혼란을 줄인다.

### 후보
- `notificationTapAction`
- `currentFps`
- `activeProfileId` 실제 사용 여부
- 기타 검색으로 확인되는 미사용 상태

### 수정 범위
- 관련 DataStore/Repository/ViewModel/UI 파일

### 해야 할 일
- 각 항목을 제거할지 연결할지 결정하고 이유를 남긴다.
- 반쯤 남은 상태를 없앤다.

### 완료 기준
- 대상 항목별 처리 결과가 표로 정리됨
- 테스트/빌드 통과

### 범위 밖
- 신규 메트릭 체계 전체 설계

## Session 12. Release Readiness for Accessibility Utility App

### 멘션용 한 줄
`Session 12 수행: 접근성 유틸리티 앱 관점에서 릴리스 직전 점검 항목을 코드/설정 기준으로 정리하고 즉시 수정 가능한 부분까지 반영해줘.`

### 배경
현재 manifest, 배터리 최적화 요청, exported receiver, overlay, foreground service, accessibility tool 속성이 릴리스 정책과 직결된다.

### 목표
- Play 정책/배포 리스크가 큰 항목을 점검한다.
- 즉시 수정 가능한 것은 고치고, 정책 판단이 필요한 것은 보고서로 남긴다.

### 수정 범위
- `AndroidManifest.xml`
- 설정/권한 관련 UI
- CI/문서 일부

### 해야 할 일
- 다음 항목을 확인한다:
  - 배터리 최적화 예외 요청 노출 방식
  - exported receiver 필요성
  - overlay 권한 안내
  - foreground service 안내/실행 조건
  - accessibility 관련 사용자 안내

### 완료 기준
- 릴리스 리스크 목록과 조치 여부 정리
- 수정했다면 테스트/빌드 결과 포함

### 범위 밖
- 법률 자문 수준의 정책 확정

## 추천 실행 순서

1. Session 01
2. Session 02
3. Session 03
4. Session 04
5. Session 05
6. Session 06
7. Session 07
8. Session 08
9. Session 10
10. Session 11
11. Session 09
12. Session 12

## 작업자 공통 보고 형식

모든 세션 결과 보고는 아래 형식을 따른다.

1. 무엇을 바꿨는지
2. 어떤 파일을 수정했는지
3. 어떻게 검증했는지
4. 남은 리스크나 후속 세션 제안
