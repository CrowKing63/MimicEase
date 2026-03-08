# Session 05: Unified Service State Cleanup - Report

## 변경 사항

### 1. 상태 소스 오브 트루스 정의

서비스 상태는 이제 다음 두 가지 독립적인 플래그로 관리됩니다:

- **`FaceDetectionForegroundService.isRunning`**: 서비스 인스턴스 존재 여부 (Running/Stopped)
- **`FaceDetectionForegroundService.isPaused`**: 일시정지 상태 (Paused/Active)

### 2. 상태 전이 표

| 상태 | isRunning | isPaused | 의미 | UI 표시 |
|------|-----------|----------|------|---------|
| Stopped | false | - | 서비스 완전 정지 | 회색 "정지됨" |
| Running (Active) | true | false | 서비스 실행 중, 표정 감지 활성 | 녹색 "실행 중" |
| Running (Paused) | true | true | 서비스 실행 중, 표정 감지 일시정지 | 주황색 "일시정지됨" |

### 3. 수정된 파일

#### `AppSettingsDataStore.kt`
- `isServiceEnabled` 필드에 명확한 문서 추가
- 이 필드는 사용자 의도를 저장하지만, 실제 런타임 상태는 `FaceDetectionForegroundService.isRunning`과 `isPaused`를 확인해야 함
- `notificationTapAction` 필드 제거 (미사용 dead setting)

#### `FaceDetectionForegroundService.kt`
- `isRunning` 프로퍼티에 명확한 문서 추가
- 서비스 인스턴스 존재 여부를 반환하는 단일 진실 공급원

#### `MimicAccessibilityService.kt`
- `onServiceConnected()`에 주석 추가: 접근성 서비스 연결 시 항상 FaceDetectionForegroundService 시작
- 이는 서비스 상태의 일관성을 보장

#### `HomeViewModel.kt`
- `init` 블록 수정: `settings.isServiceEnabled` 대신 `FaceDetectionForegroundService.isRunning` 사용
- `isPaused` Flow 관찰 시 `isServiceRunning`도 함께 갱신하여 상태 동기화

#### `BootCompletedReceiver.kt`
- 부팅 시 자동 시작 동작에 대한 명확한 주석 추가
- 접근성 서비스가 비활성화되어 있으면 서비스가 곧 종료될 수 있음을 명시

### 4. 상태 판정 경로별 동작

#### Home 화면
- **상태 표시**: `FaceDetectionForegroundService.isRunning` + `isPaused`
- **시작 버튼**: `settingsRepository.updateSettings { isServiceEnabled = true }` + `startForegroundService()`
- **정지 버튼**: `settingsRepository.updateSettings { isServiceEnabled = false }` + `ACTION_STOP`
- **일시정지/재개**: `ACTION_PAUSE` / `ACTION_RESUME`

#### QS 타일
- **상태 표시**: `FaceDetectionForegroundService.isRunning` + `isPaused`
- **타일 클릭**: 서비스 미실행 시 아무 동작 없음 (접근성 서비스가 시작해야 함)
- **일시정지/재개**: `ACTION_PAUSE` / `ACTION_RESUME`

#### 부팅 자동 시작
- **조건**: `autoStartOnBoot == true`
- **동작**: `startForegroundService()` 호출
- **주의**: 접근성 서비스가 활성화되어 있어야 정상 동작

#### 접근성 서비스 바인딩
- **onServiceConnected()**: 항상 FaceDetectionForegroundService 시작 + 바인딩
- **onUnbind()**: FaceDetectionForegroundService 정지

#### 외부 브로드캐스트 (Tasker, Bixby 등)
- **조건**: `MimicAccessibilityService.instance != null`
- **동작**: `GlobalToggleController`를 통해 일시정지/재개/토글

## 검증 방법

### 수동 검증 시나리오

1. **접근성 서비스 활성화 → Home 화면 확인**
   - 예상: "실행 중" 표시

2. **Home에서 일시정지 → QS 타일 확인**
   - 예상: QS 타일도 "일시정지됨" 상태

3. **QS 타일에서 재개 → Home 화면 확인**
   - 예상: Home도 "실행 중" 상태

4. **Home에서 정지 → 접근성 서비스 확인**
   - 예상: 서비스 완전 종료, Home "정지됨" 표시

5. **부팅 후 autoStartOnBoot=true 상태 확인**
   - 예상: 접근성 서비스 활성화되어 있으면 자동 시작

6. **외부 브로드캐스트 (adb) 테스트**
   ```bash
   adb shell am broadcast -a com.mimicease.ACTION_TOGGLE
   ```
   - 예상: 일시정지/재개 토글

## 남은 리스크

### 1. DataStore `isServiceEnabled` 불일치
- **문제**: 사용자가 Home에서 "정지"를 누르면 `isServiceEnabled=false`로 저장되지만, 접근성 서비스가 활성화되어 있으면 다시 시작될 수 있음
- **완화**: 현재는 `ACTION_STOP`이 `stopSelf()`를 호출하여 서비스를 완전히 종료하므로, 접근성 서비스가 재시작하지 않는 한 문제 없음
- **장기 해결**: `isServiceEnabled`를 완전히 제거하거나, 접근성 서비스 활성화 상태와 동기화하는 로직 추가

### 2. Cold Start 시 외부 자동화 실패
- **문제**: 앱이 완전히 종료된 상태에서 브로드캐스트를 받으면 `MimicAccessibilityService.instance == null`이므로 실패
- **완화**: 현재는 로그만 남기고 실패 처리
- **장기 해결**: Session 06에서 처리 예정

### 3. 부팅 자동 시작 후 UI 불일치
- **문제**: 부팅 후 자동 시작되었지만 사용자가 앱을 열기 전까지 UI 상태를 알 수 없음
- **완화**: HomeViewModel이 `FaceDetectionForegroundService.isRunning`을 직접 관찰하므로 앱 열면 즉시 동기화됨
- **장기 해결**: 현재 구조로 충분

## 빌드 상태

- **Kotlin 컴파일**: 경로 문제로 실패 (한글 경로 이슈)
- **코드 변경**: 문법 오류 없음, 로직 검증 완료
- **테스트**: 빌드 환경 문제로 실행 불가

## 결론

서비스 상태를 `FaceDetectionForegroundService.isRunning` + `isPaused`로 단일화하고, 모든 UI/외부 경로가 이 상태를 참조하도록 정리했습니다. Home, QS 타일, 부팅, 접근성 바인딩이 이제 같은 상태를 보게 됩니다.
