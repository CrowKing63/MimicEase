# Lint Quick Reference Card

## 🚨 Build Failed with Lint Error?

### Step 1: Identify the Issue
```bash
# Look for error message like:
# Error: Using API XX requires minSdk XX [NewApi]
# Error: Missing permission in manifest [MissingPermission]
```

### Step 2: Fix It
```kotlin
// Option A: Add API guard
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
    // API 34+ code
}

// Option B: Add permission to manifest
<uses-permission android:name="android.permission.CAMERA" />

// Option C: Suppress with justification (rare)
@SuppressLint("NewApi") // Justified: API check at line 42
fun myFunction() { ... }
```

### Step 3: Verify
```bash
./gradlew :app:lintDebug
```

---

## 📊 Common Lint Issues

| Error | Cause | Fix |
|-------|-------|-----|
| `NewApi` | Using API without version guard | Add `if (Build.VERSION.SDK_INT >= ...)` |
| `MissingPermission` | Permission not in manifest | Add `<uses-permission>` to `AndroidManifest.xml` |
| `QueryPermissionsNeeded` | Package query without `<queries>` | Add `<queries>` element to manifest |
| `MissingTranslation` | String not translated | Add to `values-XX/strings.xml` (tracked in Session 09) |
| `UnusedResources` | Unused drawable/string | Safe to ignore (suppressed) |

---

## 🔧 Lint Commands

```bash
# Standard check
./gradlew :app:lintDebug

# View report (after running lint)
start app/build/reports/lint-results-debug.html  # Windows
open app/build/reports/lint-results-debug.html   # Mac/Linux

# Clean + lint (fresh analysis)
./gradlew clean :app:lintDebug
```

---

## 📝 Suppression Template

```kotlin
// ✅ Good: Specific suppression with justification
@SuppressLint("NewApi") // Justified: API 34 check present at line 65
fun setMotionEventSources() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        serviceInfo?.setMotionEventSources(InputDevice.SOURCE_MOUSE)
    }
}

// ❌ Bad: No justification
@SuppressLint("NewApi")
fun myFunction() { ... }

// ❌ Worse: Suppressing everything
@SuppressLint("all")
```

---

## 🎯 Severity Levels

| Level | Behavior | Examples |
|-------|----------|----------|
| **Fatal** | Build fails immediately | `NewApi`, `InlinedApi` |
| **Error** | Build fails | `MissingPermission`, `ProtectedPermissions` |
| **Warning** | Reported, doesn't fail | `MissingTranslation` |
| **Info** | Logged only | `UnusedResources` (suppressed) |

---

## 🚫 Don't Update Baseline

**Baseline is for existing technical debt only.**

If you see:
```
This issue is not in the baseline
```

**Fix the issue** - don't add it to baseline.

Only update baseline when:
- Resolving a batch of issues (e.g., Session 09)
- Team agrees to defer specific issues
- Upgrading lint version

---

## 📚 Full Documentation

- **Workflow Guide**: `docs/lint_baseline_guide.md`
- **Triage Report**: `docs/lint_triage_report.md`
- **Session Summary**: `docs/session_08_summary.md`
- **Build Commands**: `AGENTS.md`

---

## 💡 Pro Tips

1. **Run lint before pushing**
   ```bash
   ./gradlew :app:lintDebug
   ```

2. **Check CI lint reports**
   - Go to GitHub Actions → Your PR
   - Download "lint-report" artifact
   - Open HTML file in browser

3. **Use IDE lint**
   - Android Studio shows lint warnings inline
   - Fix issues as you code

4. **Ask for help**
   - Include full error message
   - Share lint report if available
   - Reference this guide

---

**Remember**: New code must meet quality standards. Baseline is for existing issues only.
