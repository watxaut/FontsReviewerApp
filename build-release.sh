#!/bin/bash

# FontsReviewer - Release Build Script
# This script builds a release APK for distribution

set -e  # Exit on error

echo "🚀 FontsReviewer - Building Release APK"
echo "========================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if keystore.properties exists
if [ ! -f "keystore.properties" ]; then
    echo -e "${RED}❌ ERROR: keystore.properties not found!${NC}"
    echo ""
    echo "Please create keystore.properties with:"
    echo "  storePassword=YOUR_PASSWORD"
    echo "  keyPassword=YOUR_PASSWORD"
    echo "  keyAlias=fontsreviewer"
    echo "  storeFile=fontsreviewer-release-key.jks"
    echo ""
    exit 1
fi

# Check if keystore file exists
KEYSTORE_FILE=$(grep "storeFile" keystore.properties | cut -d'=' -f2)
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo -e "${RED}❌ ERROR: Keystore file not found: $KEYSTORE_FILE${NC}"
    echo ""
    echo "Generate a keystore with:"
    echo "  keytool -genkey -v -keystore fontsreviewer-release-key.jks \\"
    echo "    -keyalg RSA -keysize 2048 -validity 10000 \\"
    echo "    -alias fontsreviewer"
    echo ""
    exit 1
fi

echo -e "${GREEN}✓ Keystore configuration found${NC}"
echo ""

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./gradlew clean

echo ""
echo "🔨 Building release APK..."
echo ""

# Build release APK
./gradlew assembleRelease

# Check if build succeeded
if [ $? -eq 0 ]; then
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
    
    if [ -f "$APK_PATH" ]; then
        # Get APK size
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        
        echo ""
        echo -e "${GREEN}✅ SUCCESS! APK built successfully!${NC}"
        echo "========================================"
        echo ""
        echo -e "📦 APK Location: ${YELLOW}$APK_PATH${NC}"
        echo -e "📏 APK Size: ${YELLOW}$APK_SIZE${NC}"
        echo ""
        echo "📤 Next steps:"
        echo "  1. Copy the APK to your phone"
        echo "  2. Install it (enable 'Install Unknown Apps' first)"
        echo "  3. Share with others!"
        echo ""
        echo "💡 Quick share options:"
        echo "  - Email: Attach the APK file"
        echo "  - Cloud: Upload to Google Drive/Dropbox"
        echo "  - AirDrop: Send to nearby devices (Mac/iOS)"
        echo ""
        
        # Optional: Copy to easier location
        read -p "Copy APK to current directory? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            VERSION_NAME=$(grep "versionName" app/build.gradle.kts | head -1 | sed 's/.*"\(.*\)".*/\1/')
            OUTPUT_NAME="fontsreviewer-v${VERSION_NAME}-release.apk"
            cp "$APK_PATH" "$OUTPUT_NAME"
            echo -e "${GREEN}✓ Copied to: $OUTPUT_NAME${NC}"
            echo ""
        fi
    else
        echo -e "${RED}❌ ERROR: APK file not found at $APK_PATH${NC}"
        exit 1
    fi
else
    echo -e "${RED}❌ ERROR: Build failed!${NC}"
    exit 1
fi
