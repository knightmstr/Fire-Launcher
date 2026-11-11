#!/usr/bin/env bash
# ==============================================================================
# tools/clear_launcher_cache.sh
# FireLauncher Memory & Cache Recovery Utility
#
# Clears the launcher's runtime image cache and temporary application data
# over ADB to immediately free up RAM and flash storage on low-end Fire TV sticks.
# ==============================================================================

set -e

PACKAGE_NAME="com.example.firelauncher"
MAIN_ACTIVITY="com.example.firelauncher/.MainActivity"

echo "=========================================================="
echo "    🔥 FireLauncher Cache & Memory Wipe Utility 🔥        "
echo "=========================================================="

# 1. Verify ADB availability
if ! command -v adb &> /dev/null; then
    echo "[-] Error: 'adb' command not found. Please install Android Platform Tools and add to PATH."
    exit 1
fi

# 2. Determine target device
TARGET_DEVICE="$1"
ADB_CMD="adb"

if [ -n "$TARGET_DEVICE" ]; then
    echo "[+] Target device explicitly provided: $TARGET_DEVICE"
    ADB_CMD="adb -s $TARGET_DEVICE"
else
    # Check connected devices
    CONNECTED_DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | awk '{print $1}' || true)
    DEVICE_COUNT=$(echo "$CONNECTED_DEVICES" | grep -v '^$' | wc -l || true)

    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo "[-] Error: No ADB devices connected."
        echo "    Connect your Fire TV first using: adb connect <firetv-ip>:5555"
        exit 1
    elif [ "$DEVICE_COUNT" -eq 1 ]; then
        TARGET_DEVICE=$(echo "$CONNECTED_DEVICES" | tr -d ' \r\n')
        echo "[+] Detected connected device: $TARGET_DEVICE"
        ADB_CMD="adb -s $TARGET_DEVICE"
    else
        echo "[!] Multiple ADB devices detected:"
        echo "$CONNECTED_DEVICES"
        echo ""
        echo "Usage: ./tools/clear_launcher_cache.sh <device-ip:port or serial>"
        exit 1
    fi
fi

# 3. Verify package exists on device
echo "[+] Checking if $PACKAGE_NAME is installed on target..."
if ! $ADB_CMD shell pm list packages | grep -q "$PACKAGE_NAME"; then
    echo "[-] Error: Package $PACKAGE_NAME is not installed on $TARGET_DEVICE."
    exit 1
fi

# 4. Clear cache and application data
echo "[+] Clearing runtime cache and data for $PACKAGE_NAME..."
CLEAR_RESULT=$($ADB_CMD shell pm clear "$PACKAGE_NAME" 2>&1)
echo "    Result: $CLEAR_RESULT"

if echo "$CLEAR_RESULT" | grep -qi "Success"; then
    echo "[✓] Successfully cleared launcher cache and recovered storage."
else
    echo "[!] Warning: Command did not report explicit success ($CLEAR_RESULT)."
fi

# 5. Relaunch FireLauncher activity
echo "[+] Relaunching FireLauncher on target TV display..."
$ADB_CMD shell am start -n "$MAIN_ACTIVITY" > /dev/null 2>&1 || true

echo "=========================================================="
echo "    ✓ FireLauncher is running fresh with clean cache!     "
echo "=========================================================="
