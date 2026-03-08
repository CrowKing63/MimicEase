#!/bin/bash
# Generate lint baseline to grandfather existing issues
# Run this once to create lint-baseline.xml

set -e

echo "🔍 Generating lint baseline..."
echo "This will capture all current lint issues as a baseline."
echo "Future lint runs will only report NEW issues."
echo ""

# Clean to ensure fresh analysis
./gradlew clean

# Run lint and generate baseline
./gradlew :app:lintDebug

# Check if baseline was created
if [ -f "app/lint-baseline.xml" ]; then
    echo "✅ Baseline created: app/lint-baseline.xml"
    echo ""
    echo "Baseline contains:"
    grep -c "<issue" app/lint-baseline.xml || echo "0"
    echo "issues"
    echo ""
    echo "Next steps:"
    echo "1. Commit lint-baseline.xml to version control"
    echo "2. New lint warnings will now fail the build"
    echo "3. Work through sessions to resolve baseline issues"
else
    echo "⚠️  Baseline not created - check lint configuration"
    exit 1
fi
