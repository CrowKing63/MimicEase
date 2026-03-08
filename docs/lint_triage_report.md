# Lint Triage Report - Session 08
**Date**: 2026-03-08  
**Status**: Initial Analysis & Quality Gate Step 1

## Executive Summary

This report categorizes current lint issues into actionable vs. noise, identifies immediate risks, and proposes quality gate improvements for CI/CD pipeline.

---

## Issue Categories

### 🔴 HIGH PRIORITY - Release Blockers (Immediate Fix Required)

#### 1. Exported Receivers Without Permission Protection
**Issue**: `ToggleBroadcastReceiver` and `BootCompletedReceiver` are exported without signature-level protection  
**Risk**: Any app can send broadcasts to toggle service or trigger boot actions  
**Files**: `AndroidManifest.xml`  
**Recommendation**: Add `android:permission` with signature-level protection or use explicit intent filtering  
**Session**: Address in Session 06 (Broadcast and External Automation Reliability)

#### 2. Package Visibility - QueryPermissionsNeeded
**Issue**: `TriggerEditScreen.kt` uses `queryIntentActivities()` without `<queries>` declaration  
**Risk**: App picker may return incomplete results on Android 11+  
**Files**: `TriggerEditScreen.kt`, `AndroidManifest.xml`  
**Recommendation**: Add `<queries>` element or use `QUERY_ALL_PACKAGES` with justification  
**Session**: Session 10 (App Picker Package Visibility Compliance)

#### 3. Battery Optimization Request
**Issue**: `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` requires Play Store justification  
**Risk**: App may be rejected or flagged during review  
**Files**: `AndroidManifest.xml`, `SettingsScreen.kt`  
**Recommendation**: Document accessibility use case, ensure user-initiated request only  
**Session**: Session 12 (Release Readiness)

---

### 🟡 MEDIUM PRIORITY - Quality & Maintainability

#### 4. Missing Translations (247 warnings estimated)
**Issue**: Multiple language folders exist but many strings are untranslated  
**Risk**: Poor UX for non-English/Korean users  
**Files**: `values-*/strings.xml`  
**Breakdown**:
- Core flows (Home, Onboarding, Settings): ~80 strings
- Profile/Trigger management: ~60 strings
- Actions and labels: ~50 strings
- Tutorial and advanced features: ~57 strings
**Recommendation**: Prioritize core user flows first  
**Session**: Session 09 (Missing Translation Backlog Batch 1)

#### 5. Deprecated API Usage (Suppressed)
**Issue**: Multiple `@Suppress("DEPRECATION")` for display metrics, vibrator, window type  
**Risk**: Future Android versions may remove these APIs  
**Files**: `HeadTracker.kt`, `GlobalToggleController.kt`, `CursorOverlayView.kt`  
**Status**: Acceptable - properly guarded with API level checks  
**Action**: Monitor for replacement APIs in future Android releases

#### 6. NewApi Warning (Already Fixed)
**Issue**: `setMotionEventSources()` API 34 method  
**Status**: ✅ RESOLVED - Properly guarded with `Build.VERSION.SDK_INT >= UPSIDE_DOWN_CAKE`  
**Files**: `MimicAccessibilityService.kt`

---

### 🟢 LOW PRIORITY - Noise / Acceptable

#### 7. Hardcoded Strings in Code
**Issue**: Some UI strings may be hardcoded in Compose code  
**Risk**: Low - most strings use string resources  
**Action**: Audit during translation work

#### 8. Unused Resources
**Issue**: Potential unused drawables, strings, or layouts  
**Risk**: Minimal - increases APK size slightly  
**Action**: Run `./gradlew :app:lintDebug` with `--info` to identify, clean up in maintenance cycle

#### 9. Overdraw / Performance
**Issue**: Compose UI may have layout performance warnings  
**Risk**: Low - modern devices handle well  
**Action**: Profile if performance issues reported

---

## Current Lint Configuration Analysis

### `app/build.gradle.kts`
```kotlin
lint {
    abortOnError = false  // ❌ Allows broken builds to pass
}
```

### `.github/workflows/android.yml`
```yaml
- name: Lint check
  run: ./gradlew lintDebug
  continue-on-error: true  # ❌ Lint failures don't fail CI
```

**Problem**: No quality gate - critical issues can slip into production

---

## Proposed Quality Gate Improvements

### Phase 1: Immediate (This Session)

1. **Enable Fatal Error Detection**
   - Keep `abortOnError = false` for now (too many issues)
   - Add explicit severity configuration to fail on `fatal` and `error` only
   - Suppress known acceptable warnings

2. **CI Enhancement**
   - Remove `continue-on-error: true`
   - Add lint report artifact upload
   - Add comment on PR with lint summary

3. **Baseline Creation**
   - Generate lint baseline to grandfather existing issues
   - New code must not introduce new warnings

### Phase 2: Post-Translation (Session 09+)

1. **Stricter Rules**
   - Set `abortOnError = true`
   - Remove baseline for resolved categories

2. **Custom Lint Rules**
   - Enforce Timber usage (no `Log.d/e/w`)
   - Detect `SystemClock` usage in test-reachable code

---

## Recommended Execution Order

1. ✅ **Session 08** (This session): Lint triage + baseline + CI improvements
2. **Session 01**: API 33/34 crash fix (already resolved)
3. **Session 10**: Package visibility compliance
4. **Session 09**: Translation batch 1 (core flows)
5. **Session 06**: Broadcast receiver security
6. **Session 12**: Release readiness checklist

---

## Lint Baseline Strategy

Create baseline to track existing issues without blocking development:

```kotlin
lint {
    abortOnError = false
    baseline = file("lint-baseline.xml")
    checkDependencies = true
    
    // Fail on new critical issues
    fatal += listOf("NewApi", "InlinedApi")
    error += listOf("MissingPermission", "ProtectedPermissions")
    
    // Acceptable suppressions
    disable += listOf(
        "UnusedResources",  // Clean up in maintenance
        "IconDensities",    // Handled by vector drawables
        "IconMissingDensityFolder"
    )
}
```

---

## Next Steps

1. Generate lint baseline file
2. Update `app/build.gradle.kts` with improved configuration
3. Update CI workflow to fail on new critical issues
4. Document suppression rationale in code comments
5. Create tracking issues for medium-priority items

---

## Appendix: Known Suppressions Audit

| File | Suppression | Reason | Status |
|------|-------------|--------|--------|
| `MimicAccessibilityService.kt` | None (properly guarded) | API 34 check | ✅ OK |
| `SettingsScreen.kt` | `@SuppressLint("NewApi")` | API 33 check present | ✅ OK |
| `HeadTracker.kt` | `@Suppress("DEPRECATION")` | Display metrics fallback | ✅ OK |
| `GlobalToggleController.kt` | `@Suppress("DEPRECATION")` | Vibrator service fallback | ✅ OK |
| `CursorOverlayView.kt` | `@Suppress("DEPRECATION")` | Window type fallback | ✅ OK |
| `TriggerEditScreen.kt` | `@Suppress("DEPRECATION")` | Package manager query | ⚠️ Needs `<queries>` |

