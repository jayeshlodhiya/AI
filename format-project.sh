#!/bin/bash

# Project Code Formatting Script
# This script formats all Java source files in the project using Google Java Format

echo "🔧 Starting project code formatting..."

# Check if Google Java Format is available
if ! command -v google-java-format &> /dev/null; then
    echo "❌ Google Java Format not found. Installing..."
    
    # Try to install via different methods
    if command -v brew &> /dev/null; then
        echo "📦 Installing via Homebrew..."
        brew install google-java-format
    elif command -v apt-get &> /dev/null; then
        echo "📦 Installing via apt-get..."
        sudo apt-get update
        sudo apt-get install google-java-format
    else
        echo "❌ Please install Google Java Format manually:"
        echo "   Download from: https://github.com/google/google-java-format"
        echo "   Or use: curl -L -o google-java-format https://github.com/google/google-java-format/releases/download/v1.17.0/google-java-format-1.17.0-all-deps.jar"
        exit 1
    fi
fi

# Find all Java files in the project
echo "🔍 Finding Java source files..."
JAVA_FILES=$(find src -name "*.java" -type f)

if [ -z "$JAVA_FILES" ]; then
    echo "❌ No Java files found in src directory"
    exit 1
fi

echo "📁 Found $(echo "$JAVA_FILES" | wc -l) Java files to format"

# Format each Java file
echo "🎨 Formatting Java files..."
FORMATTED_COUNT=0
ERROR_COUNT=0

for file in $JAVA_FILES; do
    echo "  📝 Formatting: $file"
    
    if google-java-format --replace --aosp "$file" 2>/dev/null; then
        echo "    ✅ Formatted successfully"
        ((FORMATTED_COUNT++))
    else
        echo "    ❌ Error formatting file"
        ((ERROR_COUNT++))
    fi
done

echo ""
echo "🎉 Formatting complete!"
echo "   ✅ Successfully formatted: $FORMATTED_COUNT files"
echo "   ❌ Errors: $ERROR_COUNT files"
echo ""

if [ $ERROR_COUNT -eq 0 ]; then
    echo "✨ All files formatted successfully!"
else
    echo "⚠️  Some files had formatting errors. Check the output above."
fi
