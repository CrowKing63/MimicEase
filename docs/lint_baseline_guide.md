# Lint Baseline Guide

## What Changed?

We've implemented a **lint baseline system** to improve code quality without blocking current development.

### Before
- `abortOnError = false` - all lint issues ignored
- CI had `continue-on-error: true` - failures didn't block merges
- No visibility into new vs. existing issues

### After
- Existing issues captured in `lint-baseline.xml`
- **New critical issues will fail the build**
- CI uploads lint reports for review
- Clear separation between technical debt and new problems

---

## How It Works

### Baseline File
`app/lint-baseline.xml` contains a snapshot of all current lint issues. These are "grandfathered" and won't fail builds.

### New Issues
Any lint warning **not in the baseline** will be reported. Critical issues (fatal/error severity) will **fail the build**.

### Severity Levels
- **Fatal**: Build fails immediately (e.g., `NewApi` without guards)
- **Error**: Build fails (e.g., `MissingPermission`)
- **Warning**: Reported but doesn't fail (e.g., `MissingTranslation`)
- **Informational**: Logged only

---

## Developer Workflow

### Running Lint Locally

```bash
# Standard lint check
./gradlew :app:lintDebug

# View HTML report
# Windows: start app/build/reports/lint-results-debug.html
# Mac/Linux: open app/build/reports/lint-results-debug.html
```

### When You See a Lint Failure

1. **Check if it's a new issue**
   - Lint will show: "This issue is not in the baseline"
   
2. **Fix the issue immediately** (preferred)
   - Address the root cause
   - Re-run lint to verify
   
3. **Or suppress with justification** (rare cases)
   ```kotlin
   @SuppressLint("NewApi") // Justified: API check present at line 42
   fun myFunction() { ... }
   ```

### Updating the Baseline (Rare)

Only update baseline when:
- Resolving a batch of issues (e.g., Session 09 translations)
- Upgrading lint version with new rules
- Team agrees to defer specific issues

```bash
# Windows
./scripts/generate-lint-baseline.ps1

# Mac/Linux
./scripts/generate-lint-baseline.sh
```

**Important**: Baseline updates require code review approval.

---

## CI/CD Behavior

### Pull Requests
- Lint runs on every PR
- New critical issues block merge
- Lint report uploaded as artifact (check Actions tab)

### Viewing CI Lint Reports
1. Go to GitHub Actions for your PR
2. Find "Build and Test" workflow
3. Download "lint-report" artifact
4. Open `lint-results-debug.html` in browser

---

## Current Lint Configuration

### Fatal Issues (Build Fails)
- `NewApi` - Using API without version guard
- `InlinedApi` - Inlined constant from newer API
- `MissingPermission` - Missing required permission
- `ProtectedPermissions` - Using protected permission incorrectly

### Suppressed Issues
- `UnusedResources` - Cleaned up in maintenance cycles
- `IconDensities` - Vector drawables handle this
- `MissingTranslation` - Tracked in Session 09

---

## FAQ

### Q: Why did my build suddenly fail?
**A**: You introduced a new critical lint issue. Check the error message for details and fix it.

### Q: Can I just add my issue to the baseline?
**A**: No. Baseline is for existing technical debt only. New code must meet quality standards.

### Q: What if lint is wrong?
**A**: Use `@SuppressLint` with a comment explaining why it's a false positive. Example:
```kotlin
@SuppressLint("NewApi") // False positive: API check at line 42
```

### Q: How do I see all baseline issues?
**A**: Open `app/lint-baseline.xml` or view the triage report: `docs/lint_triage_report.md`

### Q: When will baseline issues be fixed?
**A**: See `docs/12_session_task_briefs.md` for the roadmap. Priority issues are in Sessions 01, 06, 09, 10, 12.

---

## Best Practices

### ✅ Do
- Fix new lint issues immediately
- Add comments when suppressing warnings
- Run lint before pushing code
- Review lint reports in CI

### ❌ Don't
- Suppress warnings without justification
- Update baseline to hide new issues
- Ignore lint failures in CI
- Use `@SuppressLint("all")`

---

## Lint Commands Reference

```bash
# Run lint on debug variant
./gradlew :app:lintDebug

# Run lint on all variants
./gradlew :app:lint

# Clean + lint (fresh analysis)
./gradlew clean :app:lintDebug

# Generate new baseline (requires approval)
./scripts/generate-lint-baseline.ps1  # Windows
./scripts/generate-lint-baseline.sh   # Mac/Linux
```

---

## Related Documentation

- **Triage Report**: `docs/lint_triage_report.md` - Full issue breakdown
- **Session Briefs**: `docs/12_session_task_briefs.md` - Remediation roadmap
- **AGENTS.md**: Build and lint command reference

---

## Support

If you have questions about lint failures or baseline updates:
1. Check this guide first
2. Review the triage report
3. Ask in team chat with lint error details
