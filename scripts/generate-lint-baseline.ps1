# Generate lint baseline to grandfather existing issues
# Run this once to create lint-baseline.xml

Write-Host "🔍 Generating lint baseline..." -ForegroundColor Cyan
Write-Host "This will capture all current lint issues as a baseline."
Write-Host "Future lint runs will only report NEW issues."
Write-Host ""

# Clean to ensure fresh analysis
Write-Host "Cleaning build..." -ForegroundColor Yellow
./gradlew clean

# Run lint and generate baseline
Write-Host "Running lint analysis..." -ForegroundColor Yellow
./gradlew :app:lintDebug

# Check if baseline was created
if (Test-Path "app/lint-baseline.xml") {
    Write-Host "✅ Baseline created: app/lint-baseline.xml" -ForegroundColor Green
    Write-Host ""
    
    $issueCount = (Select-String -Path "app/lint-baseline.xml" -Pattern "<issue" -AllMatches).Matches.Count
    Write-Host "Baseline contains: $issueCount issues"
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "1. Commit lint-baseline.xml to version control"
    Write-Host "2. New lint warnings will now fail the build"
    Write-Host "3. Work through sessions to resolve baseline issues"
} else {
    Write-Host "⚠️  Baseline not created - check lint configuration" -ForegroundColor Red
    exit 1
}
