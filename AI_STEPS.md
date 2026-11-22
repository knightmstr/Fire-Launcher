# AI Automation Manifest: Fire TV Deployment & Safe Debloating

This document is a sequential, machine-readable execution manifest intended for terminal-based AI agents (such as Gemini CLI, Claude Code, or automated CI/CD runners) to connect to an Amazon Fire TV device over Wi-Fi, deploy the FireLauncher APK, and execute the CleanYourTV debloat routine.

---

## Pre-Flight Requirements & Context

- **Target Architecture**: Fire OS 6, 7 and 8 (Android 7.1 and later)
- **Target Device**: Amazon Fire TV Stick / Fire TV Cube / Fire TV Edition
- **Default ADB Port**: `5555`
- **Target Package Name**: `com.knightmstr.firelauncher`
- **APK**: the signed `FireLauncher-vX.Y.Z-release.apk` from the latest GitHub Release (`knightmstr/Fire-Launcher`). Use the release APK, not a local debug build: releases are signed with the project key, so in-app updates install over them. A debug build is signed differently and can't be installed over a release (or updated by one) without uninstalling first.

---

## Target Debloat Package Registry

The following Amazon packages are targeted (the screensaver only if the user opts in) for user-space disabling (`pm disable-user --user 0`). Disabling these stops background telemetry, metric collection daemons, crash reporting services, and home screen video ads without affecting streaming apps (Netflix, Prime Video, YouTube, etc.):

| Package Identifier | Description | Purpose |
|---|---|---|
| `com.amazon.advertising.firetv` | Home banner & commercial video ad engine | Frees RAM, removes autoplay video banners |
| `com.amazon.device.sale.service` | Commercial deal & promotional checking service | Disables periodic rental and sales notification pings |
| `com.amazon.client.metrics` | Device metrics, clickstream, & analytics logger | Stops continuous network reporting and logging |
| `com.amazon.device.crashmanager` | Crash logging daemon & dump collection service | Eliminates continuous crash collection CPU cycles |
| `com.amazon.ftv.screensaver` | Sponsored commercial video screensaver | Optional: only if FireLauncher's screensaver will be used |

---

## Sequential Execution Steps

### STEP 1: Verify Host Environment Dependencies
The agent must verify that the Android Debug Bridge (`adb`), Python 3 and `curl` are installed and reachable in `PATH`.

```bash
# Check ADB installation
adb version || { echo "ERROR: ADB is not installed or not in PATH"; exit 1; }

# Check Python 3 installation
python3 --version || { echo "ERROR: Python 3 is not installed or not in PATH"; exit 1; }

# Check curl (downloads the release APK)
curl --version > /dev/null || { echo "ERROR: curl is not installed or not in PATH"; exit 1; }
```

Expected output: Valid version strings for `adb`, `python3` and `curl`.

---

### STEP 2: Obtain and Validate Fire TV IP Address
Execute the CLI input helper script. The script prompts the user with instructions, validates the IPv4 regex pattern with octet range verification (0–255), and outputs the token `SUCCESS:<IP_ADDRESS>`.

```bash
# Execute Python IP collector and capture result
IP_OUTPUT=$(python3 tools/get_tv_ip.py)
FIRETV_IP=$(echo "$IP_OUTPUT" | grep "^SUCCESS:" | cut -d':' -f2 | tr -d ' \r\n')

if [ -z "$FIRETV_IP" ]; then
    echo "ERROR: Failed to retrieve a valid Fire TV IP address."
    exit 1
fi

echo "Target Fire TV IP: $FIRETV_IP"
```

---

### STEP 3: Establish Authenticated ADB Connection
Connect over TCP/IP to port `5555`. If this is the first time connecting, the Fire TV screen will show an authorization prompt: *"Allow USB debugging?"* or *"Allow network debugging?"*. The agent must pause and notify the user to accept this on-screen prompt using their physical TV remote.

```bash
# Terminate any stale daemon instances
adb kill-server
adb start-server

# Initiate TCP/IP connection to Fire TV
echo "Connecting to $FIRETV_IP:5555..."
adb connect "$FIRETV_IP:5555"

# Verification loop for device authorization
echo "=========================================================="
echo "ACTION REQUIRED: Look at your Fire TV screen."
echo "If prompted, check 'Always allow from this computer' and"
echo "press 'OK' using your Fire TV remote control."
echo "=========================================================="

# Poll device status until state changes to 'device'
MAX_RETRIES=12
RETRY_COUNT=0
AUTH_SUCCESS=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    DEVICE_STATE=$(adb devices | grep "$FIRETV_IP:5555" | awk '{print $2}')
    
    if [ "$DEVICE_STATE" = "device" ]; then
        echo "SUCCESS: Authenticated and connected to Fire TV ($FIRETV_IP:5555)."
        AUTH_SUCCESS=1
        break
    elif [ "$DEVICE_STATE" = "unauthorized" ]; then
        echo "Waiting for user to accept the authorization dialog on TV screen (Attempt $((RETRY_COUNT + 1))/$MAX_RETRIES)..."
        sleep 5
    else
        echo "Connecting... State: ${DEVICE_STATE:-connecting} (Attempt $((RETRY_COUNT + 1))/$MAX_RETRIES)..."
        adb connect "$FIRETV_IP:5555"
        sleep 4
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
done

if [ $AUTH_SUCCESS -ne 1 ]; then
    echo "ERROR: Device not authenticated or unreachable at $FIRETV_IP:5555."
    exit 1
fi
```

---

### STEP 4: Download the Latest Release APK & Deploy FireLauncher
Download the signed release APK from the latest GitHub Release, then install it with `adb install -r`.

```bash
APK_URL=$(curl -fsSL https://api.github.com/repos/knightmstr/Fire-Launcher/releases/latest \
  | grep -o '"browser_download_url": *"[^"]*-release\.apk"' | sed 's/.*"\(https[^"]*\)"/\1/')
[ -n "$APK_URL" ] || { echo "ERROR: could not find the latest release APK"; exit 1; }
APK_PATH="FireLauncher-release.apk"
curl -fL -o "$APK_PATH" "$APK_URL" || { echo "ERROR: download failed"; exit 1; }

# Install APK onto Fire TV
echo "Installing FireLauncher ($APK_URL) onto Fire TV..."
adb -s "$FIRETV_IP:5555" install -r "$APK_PATH"

# Verify successful installation
adb -s "$FIRETV_IP:5555" shell pm list packages | grep "com.knightmstr.firelauncher" || {
    echo "ERROR: FireLauncher installation verification failed."
    exit 1
}

echo "SUCCESS: FireLauncher installed successfully."
```

If `adb install` fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, an older FireLauncher signed with a different key is installed. Ask the user before running `adb -s "$FIRETV_IP:5555" uninstall com.knightmstr.firelauncher` (this resets their FireLauncher settings), then install again. Also check for the pre-rename package with `pm list packages | grep com.example.firelauncher`, and offer to uninstall it.

---

### STEP 5: Execute Amazon Package Disable Routine (Debloating)
Execute the non-root safe package disabling commands for user 0 across the 4 ad and telemetry packages. Disable `com.amazon.ftv.screensaver` only if the user confirms they'll use FireLauncher's screensaver, since otherwise they'd have none.

```bash
DEBLOAT_PACKAGES=(
    "com.amazon.advertising.firetv"
    "com.amazon.device.sale.service"
    "com.amazon.client.metrics"
    "com.amazon.device.crashmanager"
)
# Only if the user will use FireLauncher's screensaver (ask first; see STEP 6b):
# DEBLOAT_PACKAGES+=("com.amazon.ftv.screensaver")

echo "Executing safe non-root debloat routine on Fire TV..."

for PKG in "${DEBLOAT_PACKAGES[@]}"; do
    echo "Disabling package: $PKG"
    # Execute user-0 package disable
    RESULT=$(adb -s "$FIRETV_IP:5555" shell pm disable-user --user 0 "$PKG" 2>&1)
    echo "  -> Result: $RESULT"
done
```

---

### STEP 6: Verify Debloat Success
Inspect the list of disabled packages on the device to confirm that the target packages are deactivated.

```bash
echo "Verifying disabled packages on target device..."
DISABLED_LIST=$(adb -s "$FIRETV_IP:5555" shell pm list packages -d)

ALL_DISABLED=1
for PKG in "${DEBLOAT_PACKAGES[@]}"; do
    if echo "$DISABLED_LIST" | grep -q "$PKG"; then
        echo "  [VERIFIED DISABLED] $PKG"
    else
        echo "  [WARNING: NOT LISTED DISABLED] $PKG"
        ALL_DISABLED=0
    fi
done

if [ $ALL_DISABLED -eq 1 ]; then
    echo "SUCCESS: All targeted Amazon packages disabled."
else
    echo "NOTE: Some packages were not reported in disabled state. They may already be inactive or restricted by this Fire OS build."
fi
```

---

### STEP 6b (Optional, ask first): Fire TV Extras
Only if the user wants them. Explain each in one line and wait for a yes:

- **Home button opens FireLauncher, FireLauncher screensaver, keep awake**: lets FireLauncher change these Fire TV settings; each is then switched on in FireLauncher's *Launcher settings*, and turning it off restores Amazon's setting.
- **Now playing**: lets FireLauncher see what's playing (it never reads notifications).

The grant can also be done on the TV without adb (Launcher settings → Screensaver or Fire TV section → any of the three options → *Set up on this TV*). Since adb is already connected here, use the command. Afterwards, remind the user that the grant alone changes nothing: they must switch each option on in *Launcher settings*. Verify with `adb -s "$FIRETV_IP:5555" shell settings get secure sleep_timeout` (`-1` once keep-awake is on).

```bash
# Home button / screensaver / keep awake
adb -s "$FIRETV_IP:5555" shell pm grant com.knightmstr.firelauncher android.permission.WRITE_SECURE_SETTINGS

# Now playing
adb -s "$FIRETV_IP:5555" shell cmd notification allow_listener com.knightmstr.firelauncher/com.knightmstr.firelauncher.media.MediaListenerService
```

---

### STEP 7: Launch FireLauncher & Reboot Device
Start FireLauncher once to register the leanback activity, then initiate a clean system reboot to flush RAM, terminate any cached Amazon ad services, and apply all modifications cleanly.

```bash
# Launch FireLauncher activity
echo "Launching FireLauncher activity..."
adb -s "$FIRETV_IP:5555" shell am start -n com.knightmstr.firelauncher/.MainActivity

sleep 3

# Reboot device to apply system service shutdowns
echo "Initiating Fire TV reboot..."
adb -s "$FIRETV_IP:5555" reboot

echo "=========================================================="
echo "DEPLOYMENT & DEBLOAT COMPLETE!"
echo "Your Fire TV is now rebooting."
echo "FireLauncher is ready to use upon startup."
echo "=========================================================="
```

---

## Rollback Command Reference

If the user ever wishes to re-enable any or all of the packages, the agent can execute:

```bash
# Re-enable all packages (the screensaver line is harmless if it was never disabled)
adb -s "$FIRETV_IP:5555" shell pm enable com.amazon.advertising.firetv
adb -s "$FIRETV_IP:5555" shell pm enable com.amazon.device.sale.service
adb -s "$FIRETV_IP:5555" shell pm enable com.amazon.client.metrics
adb -s "$FIRETV_IP:5555" shell pm enable com.amazon.device.crashmanager
adb -s "$FIRETV_IP:5555" shell pm enable com.amazon.ftv.screensaver
```
