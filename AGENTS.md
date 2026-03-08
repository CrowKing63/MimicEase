# AGENTS.md - MimicEase Developer Guide

## Build, Lint, and Test Commands

### Build Commands
```bash
# Clean build (full rebuild)
./gradlew clean build

# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Quick Kotlin compile only (no APK)
./gradlew :app:compileDebugKotlin
```

### Test Commands
```bash
# All unit tests
./gradlew test

# App module unit tests only
./gradlew :app:test

# Single test class (run via test task filtering)
./gradlew :app:test --tests "com.mimicease.ExpressionAnalyzerTest"

# Single test method
./gradlew :app:test --tests "com.mimicease.ExpressionAnalyzerTest.EMA 필터 - 첫 번째 값은 그대로 반환"
```

### Lint
```bash
# Run lint
./gradlew lint

# Lint with auto-fix where possible
./gradlew lintDebug

# Generate lint baseline (requires approval)
./scripts/generate-lint-baseline.ps1  # Windows
./scripts/generate-lint-baseline.sh   # Mac/Linux

# View lint report
# Windows: start app/build/reports/lint-results-debug.html
# Mac/Linux: open app/build/reports/lint-results-debug.html
```

**Lint Baseline System**: We use a baseline to grandfather existing issues. New critical issues will fail the build. See `docs/lint_baseline_guide.md` for workflow details.

---

## Code Style Guidelines

### Architecture
- **Clean Architecture**: 3 layers - `data/`, `domain/`, `presentation/`
- `domain/` layer must have ZERO Android dependencies
- Use Repository pattern - UI never accesses data sources directly
- ViewModels paired with screens in same `.kt` file (e.g., `SettingsScreen.kt` contains `SettingsViewModel`)

### Language & Frameworks
- **Kotlin 2.0.21** - Use idiomatic Kotlin (extension functions, lambdas, etc.)
- **Jetpack Compose** for all UI - never use XML layouts
- **Hilt** for dependency injection (mandatory)
- **Coroutines + Flow** for async - never use blocking calls
- **Material 3** theming

### Package Structure
```
com.mimicease/
├── data/           # Room DB, DataStore, Repository implementations
├── domain/         # Pure Kotlin models, Repository interfaces
├── presentation/  # Compose UI + ViewModels
├── service/       # Background services, core logic
├── di/            # Hilt modules
├── navigation/    # NavGraph
└── ui/theme/      # Material3 theme (Color, Theme, Type)
```

### Naming Conventions
- **Classes**: PascalCase (e.g., `ExpressionAnalyzer`, `TriggerMatcher`)
- **Functions**: camelCase (e.g., `processSmoothed`, `updateSettings`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `DEFAULT_ALPHA`, `MAX_FRAMES`)
- **Files**: Same as class name (e.g., `ExpressionAnalyzer.kt`)
- **BlendShape names**: Use MediaPipe naming (e.g., `eyeBlinkRight`, `jawOpen`)
- **Sealed class entries**: PascalCase (e.g., `object GlobalHome : Action()`)

### Imports
- **Sort order**: android.* → androidx.* → other external → internal project
- **Avoid wildcard imports** except for Compose stable APIs
- **Group related imports**:
  ```kotlin
  import android.content.Context
  import android.os.Build
  import androidx.compose.foundation.layout.*
  import androidx.compose.material3.*
  import com.mimicease.domain.model.*
  ```

### Code Formatting
- **Indentation**: 4 spaces (not tabs)
- **Line length**: Soft limit 120 characters
- **Functions**: Prefer single-expression for simple returns:
  ```kotlin
  // Good
  fun isEnabled() = settings.isEnabled
  
  // Also good for longer bodies
  fun processValue(input: Float): Float {
      return input * alpha + previous * (1 - alpha)
  }
  ```
- **Data classes**: One parameter per line for clarity
- **Sealed class**: Use line separators between entries for readability

### Types
- Use **explicit types** for public APIs, prefer type inference for local variables
- **Float** for screen coordinates (normalized 0.0-1.0 or pixels)
- **Long** for millisecond durations
- **Int** for frame counts, array indices

### Error Handling
- **Never suppress exceptions silently** - use try-catch with logging
- **Never expose secrets/keys** - no logging of sensitive data
- **Wrap Android-specific code** in try-catch for robustness:
  ```kotlin
  try {
      // Accessibility action
  } catch (e: Exception) {
      Timber.e(e, "Action failed")
  }
  ```
- **Return sensible defaults** on failure rather than throwing

### Testing
- **Test file location**: `app/src/test/java/com/mimicease/`
- **Naming**: Use Korean test names with backticks (e.g., `EMA 필터 - 첫 번째 값은 그대로 반환`)
- **Assertions**: Use `assertEquals(expected, actual, delta)` for floats
- **Setup**: `@Before` for test fixture setup
- **No Android dependencies** in unit tests - mock or use pure Kotlin classes

### Critical Gotchas
- **SystemClock forbidden**: Use `System.currentTimeMillis()` NOT `android.os.SystemClock.elapsedRealtime()` - causes "not mocked" in JVM tests
- **TalkBack compatibility**: Never consume accessibility events in `onAccessibilityEvent()` - let TalkBack process them
- **Camera conflicts**: Handle `CameraState.ERROR_CAMERA_IN_USE` by calling `pauseAnalysis()`
- **Service restart**: `onStartCommand()` may receive null intent (START_STICKY)
- **Foreground service**: Call `startForeground()` early in `onCreate()` to avoid 5-second timeout
- **MediaPipe init**: Run on HandlerThread via `Handler(faceLandmarkerHelper.looper).post { init() }`

### Logging
- Use **Timber** (not `Log.d/e/w`)
- **Never log** user input, sensitive data, or BlendShape values in production
- Tag: use class name as tag

### UI/Compose Guidelines
- **State hoisting**: Push state up, events down
- **StateFlow**: Use for UI state from ViewModels
- **remember/rememberSaveable**: Use appropriately for compose state
- **Modifier order**: `.fillMaxWidth().padding(16.dp).clickable { }`
- **Colors/Theme**: Use MaterialTheme colors, not hardcoded hex

### Action Sealed Class Patterns
Actions are defined in `domain/model/Action.kt`:
```kotlin
sealed class Action {
    object GlobalHome : Action()
    data class TapCustom(val x: Float, val y: Float) : Action()
    data class SwipeUp(val duration: Long = 300L) : Action()
}
```

### BlendShape Reference
52 BlendShapes from MediaPipe - use `BLENDSHAPE_DISPLAY_NAMES` in `ExpressionTestScreen.kt` as source of truth.

---

## Quick Reference

| Task | Command |
|------|---------|
| Build debug | `./gradlew assembleDebug` |
| Single test | `./gradlew :app:test --tests "ClassName.TestName"` |
| Lint check | `./gradlew lint` |
| Full build | `./gradlew clean build` |
