# Session 06 Verification Guide

## Cold Start Test Scenarios

### Scenario 1: Broadcast Receiver - Accessibility Service Not Bound
**Setup:**
1. Disable MimicEase accessibility service in Settings
2. Ensure app is not running

**Test:**
```bash
adb shell am broadcast -a com.mimicease.ACTION_TOGGLE -n com.mimicease/.service.ToggleBroadcastReceiver
```

**Expected:**
- Toast message: "MimicEase 접근성 서비스를 먼저 활성화하세요"
- Log: "ToggleBroadcastReceiver: 서비스 미실행 — 접근성 서비스 활성화 필요"
- No crash

### Scenario 2: Broadcast Receiver - Service Running, Accessibility Not Bound
**Setup:**
1. Start FaceDetectionForegroundService manually
2. Accessibility service not bound (unusual but possible)

**Test:**
```bash
adb shell am broadcast -a com.mimicease.ACTION_ENABLE -n com.mimicease/.service.ToggleBroadcastReceiver
```

**Expected:**
- Service receives ACTION_RESUME intent
- Toast feedback: "MimicEase 활성화"
- Service resumes analysis
- No crash

### Scenario 3: Deep Link - Cold Start
**Setup:**
1. Force stop app
2. Disable accessibility service

**Test:**
```bash
adb shell am start -a android.intent.action.VIEW -d "mimicease://toggle"
```

**Expected:**
- ToggleActionActivity launches and finishes immediately
- Broadcast sent to ToggleBroadcastReceiver
- Toast: "MimicEase 접근성 서비스를 먼저 활성화하세요"
- No crash

### Scenario 4: QS Tile - Service Not Running
**Setup:**
1. Disable accessibility service
2. Pull down notification shade
3. Tap MimicEase QS tile

**Expected:**
- Tile shows STATE_UNAVAILABLE
- Toast: "MimicEase 접근성 서비스를 먼저 활성화하세요"
- No service start attempt
- No crash

### Scenario 5: Normal Operation - All Services Bound
**Setup:**
1. Enable accessibility service
2. Service running normally

**Test:**
```bash
adb shell am broadcast -a com.mimicease.ACTION_TOGGLE -n com.mimicease/.service.ToggleBroadcastReceiver
```

**Expected:**
- GlobalToggleController.handleBroadcastToggle() called
- Service toggles pause/resume
- TTS + vibration feedback
- No Toast (feedback via TTS)

## Security Validation

### Broadcast Receiver Security
**Verification:**
1. Check AndroidManifest.xml: `exported="true"` with intent-filter
2. Verify action validation in onReceive()
3. Confirm no sensitive data in logs

**Expected:**
- Only ACTION_TOGGLE/ENABLE/DISABLE accepted
- Unknown actions logged and ignored
- No PII or sensitive state exposed

### Deep Link Security
**Verification:**
1. Test invalid schemes: `mimicease://invalid`
2. Test malformed URIs: `mimicease://`

**Expected:**
- Invalid hosts logged and ignored
- Toast feedback for user
- No crash or undefined behavior

## Failure Mode Documentation

| Trigger | Accessibility Service | FaceDetectionService | Behavior |
|---------|----------------------|---------------------|----------|
| Broadcast | Not bound | Not running | Toast: "접근성 서비스 활성화 필요" |
| Broadcast | Not bound | Running | Direct service intent, Toast feedback |
| Broadcast | Bound | Running | GlobalToggleController (optimal path) |
| Deep Link | Not bound | Not running | Broadcast → Toast feedback |
| QS Tile | Not bound | Not running | Toast: "접근성 서비스 활성화 필요" |
| QS Tile | Bound | Running | Direct service intent |

## Regression Tests

### Test 1: Normal Toggle Still Works
```bash
# Enable accessibility service first
adb shell am broadcast -a com.mimicease.ACTION_TOGGLE
# Verify service toggles pause/resume
```

### Test 2: Bixby Routines Compatibility
1. Create Bixby routine with "빠른 설정 변경" → MimicEase tile
2. Trigger routine with service running
3. Verify tile state updates correctly

### Test 3: Tasker Integration
1. Create Tasker task: Send Intent → com.mimicease.ACTION_ENABLE
2. Run task with service stopped
3. Verify graceful failure with user feedback

## Known Limitations

1. **Cold start from broadcast cannot start accessibility service**
   - Android security: apps cannot programmatically enable accessibility
   - User must manually enable in Settings
   - Clear feedback provided via Toast

2. **QS tile cannot start service from cold**
   - Requires accessibility service to be bound first
   - Tile shows UNAVAILABLE state when service not running

3. **Deep links require user interaction**
   - Cannot silently start service in background
   - Activity launches briefly to send broadcast

## Manual Verification Checklist

- [ ] Broadcast receiver handles all 3 cold start paths
- [ ] Toast feedback appears for all failure modes
- [ ] No crashes in any scenario
- [ ] Security validation passes
- [ ] Normal operation unchanged
- [ ] QS tile state reflects actual service state
- [ ] Deep links work from external apps
- [ ] Bixby routines integration functional
