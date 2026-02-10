#!/bin/bash
# build_deploy_launch.sh
# Automates build, deploy, and launch for Zebra RFID SDK Sample Application

set -e

# Set your Android device ID (optional, for multiple devices)
DEVICE_ID=""

# Build the project
./gradlew assembleDebug

# Find the APK path
echo "Locating APK..."
APK_PATH=$(find ./app/build/outputs/apk/debug -name "*.apk" | head -n 1)
if [ ! -f "$APK_PATH" ]; then
  echo "APK not found! Build may have failed."
  exit 1
fi

echo "APK found at $APK_PATH"

# Deploy to device
echo "Deploying APK to device..."
if [ -z "$DEVICE_ID" ]; then
  adb install -r "$APK_PATH"
else
  adb -s "$DEVICE_ID" install -r "$APK_PATH"
fi

echo "APK deployed. Launching app..."

# Launch the app (replace with your actual package/activity)
PACKAGE="com.zebra.rfid.demo.sdksample"
ACTIVITY=".MainActivity"
if [ -z "$DEVICE_ID" ]; then
  adb shell am start -n "$PACKAGE/$ACTIVITY"
else
  adb -s "$DEVICE_ID" shell am start -n "$PACKAGE/$ACTIVITY"
fi

echo "App launched!"
