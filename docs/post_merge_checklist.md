# Post-Merge Checklist: Session 08

**⚠️ IMPORTANT**: Complete these steps immediately after merging Session 08 changes.

---

## Step 1: Generate Lint Baseline (REQUIRED)

The baseline file must be created before any new development can proceed.

### Windows
```powershell
cd C:\Users\CrowKing63\Developments\MimicEase
./gradlew --stop
./scripts/generate-lint-baseline.ps1
```

### Mac/Linux
```bash
cd ~/Developments/MimicEase
./gradlew --stop
./scripts/generate-lint-baseline.sh
```

### Expected Output
```
✅ Baseline created: app/lint-baseline.xml
Baseline contains: XXX issues
```

---

## Step 2: Commit Baseline File

```bash
git add app/lint-baseline.xml
git commit -m "chore: add lint baseline from Session 08"
git push origin main
```

**Do NOT skip this step** - builds will fail without the baseline file.

---

## Step 3: Verify CI Pipeline

1. Create a test branch with a trivial change
2. Push to GitHub
3. Check Actions tab
4. Verify:
   - ✅ Lint step runs successfully
   - ✅ "lint-report" artifact is uploaded
   - ✅ Build completes without errors

---

## Step 4: Test Lint Enforcement

Create a test branch with an intentional lint error:

```kotlin
// In any .kt file, add:
fun testLintEnforcement() {
    if (true) {
        setMotionEventSources(InputDevice.SOURCE_MOUSE) // Missing API guard
    }
}
```

Push and verify:
- ❌ Build should FAIL with NewApi error
- ✅ Lint report should show the issue

Then revert the test change.

---

## Step 5: Team Communication

Send this message to the team:

```
📢 Lint Quality Gate Active

Session 08 changes are now merged. Key points:

1. ✅ Lint baseline created - existing issues grandfathered
2. 🚨 New critical issues will FAIL builds
3. 📊 Lint reports available in CI artifacts
4. 📚 Documentation: docs/lint_baseline_guide.md

Before your next PR:
- Run `./gradlew :app:lintDebug` locally
- Fix any new issues before pushing
- See docs/lint_quick_reference.md for help

Questions? Check the docs or ask in chat.
```

---

## Step 6: Update Project Board

Mark these items as complete:
- [x] Session 08: Lint Debt Triage and Quality Gate Step 1
- [x] Lint baseline system implemented
- [x] CI quality gate active
- [x] Developer documentation published

Add to backlog:
- [ ] Session 10: App Picker Package Visibility Compliance
- [ ] Session 06: Broadcast and External Automation Reliability
- [ ] Session 09: Missing Translation Backlog Batch 1

---

## Troubleshooting

### Issue: Baseline generation fails
**Solution**: 
```bash
./gradlew --stop
./gradlew clean
./scripts/generate-lint-baseline.ps1
```

### Issue: Build fails with "baseline file not found"
**Solution**: 
```bash
# Generate baseline immediately
./scripts/generate-lint-baseline.ps1
git add app/lint-baseline.xml
git commit -m "chore: add missing lint baseline"
git push
```

### Issue: CI still shows continue-on-error
**Solution**: 
- Verify `.github/workflows/android.yml` was updated
- Check that changes were pushed to main branch
- Clear GitHub Actions cache if needed

### Issue: Too many lint errors after baseline
**Solution**: 
- This is expected - baseline captures current state
- Errors are tracked in `docs/lint_triage_report.md`
- Follow session roadmap to resolve incrementally

---

## Verification Checklist

Before considering Session 08 complete:

- [ ] Baseline file generated (`app/lint-baseline.xml` exists)
- [ ] Baseline file committed to main branch
- [ ] CI pipeline runs successfully
- [ ] Test lint enforcement (intentional error fails build)
- [ ] Team notified of new workflow
- [ ] Documentation reviewed by at least one team member
- [ ] Next sessions (06, 09, 10) prioritized in backlog

---

## Timeline

| Task | Owner | Deadline | Status |
|------|-------|----------|--------|
| Generate baseline | First merger | Immediately | ⏳ Pending |
| Commit baseline | First merger | Within 1 hour | ⏳ Pending |
| Verify CI | First merger | Within 2 hours | ⏳ Pending |
| Test enforcement | Any developer | Within 1 day | ⏳ Pending |
| Team notification | Tech lead | Within 1 day | ⏳ Pending |
| Documentation review | Team | Within 2 days | ⏳ Pending |

---

## Success Criteria

Session 08 is fully deployed when:
1. ✅ Baseline file exists in repository
2. ✅ CI fails on new critical lint issues
3. ✅ Lint reports uploaded for every PR
4. ✅ Team understands new workflow
5. ✅ No blockers for ongoing development

---

**Questions?** See `docs/lint_baseline_guide.md` or contact the team lead.
