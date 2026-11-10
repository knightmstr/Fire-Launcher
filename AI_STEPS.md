# AI Automation Manifest: Fire TV Deployment & Safe Debloating

This document is a sequential, machine-readable execution manifest intended for terminal-based AI agents (such as Gemini CLI, Claude Code, or automated CI/CD runners) to connect to an Amazon Fire TV device over Wi-Fi, deploy the FireLauncher APK, and execute the CleanYourTV debloat routine.

---

## Pre-Flight Requirements & Context

- **Target Architecture**: Android 7.1+ / 9.0 (Fire OS 6, 7, 8)
- **Target Device**: Amazon Fire TV Stick / Fire TV Cube / Fire TV Edition
- **Default ADB Port**: `5555`
- **Target Package Name**: `com.example.firelauncher`
- **Pre-compiled APK Path**: `app/build/outputs/apk/debug/app-debug.apk`

---

## Target Debloat Package Registry

The following 5 Amazon packages are targeted for user-space disabling (`pm disable-user --user 0`). Disabling these stops background telemetry, metric collection daemons, crash reporting services, and home screen video ads without affecting streaming apps (Netflix, Prime Video, YouTube, etc.):

| Package Identifier | Description | Purpose |
|---|---|---|
| `com.amazon.advertising.firetv` | Home banner & commercial video ad engine | Frees RAM, removes autoplay video banners |
| `com.amazon.device.sale.service` | Commercial deal & promotional checking service | Disables periodic rental and sales notification pings |
| `com.amazon.client.metrics` | Device metrics, clickstream, & analytics logger | Stops continuous network reporting and logging |
| `com.amazon.device.crashmanager` | Crash logging daemon & dump collection service | Eliminates continuous crash collection CPU cycles |
| `com.amazon.ftv.screensaver` | Sponsored commercial video screensaver | Replaces commercial screensavers |

---

## Sequential Execution Steps

### STEP 1: Verify Host Environment Dependencies
The agent must verify that the Android Debug Bridge (`adb`) and Python 3 are installed and reachable in `PATH`.

```bash
# Check ADB installation
adb version || { echo "ERROR: ADB is not installed or not in PATH"; exit 1; }

# Check Python 3 installation
python3 --version || { echo "ERROR: Python 3 is not installed or not in PATH"; exit 1; }
```

Expected output: Valid version strings for both `adb` and `python3`.

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

### STEP 4: Build APK (If Not Present) & Deploy FireLauncher
Verify the presence of `app/build/outputs/apk/debug/app-debug.apk`. If it is missing, compile using the Gradle wrapper. Then install via `adb install -r`.

```bash
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

# Compile APK if not pre-built
if [ ! -f "$APK_PATH" ]; then
    echo "APK not found at $APK_PATH. Compiling debug build with Gradle..."
    ./gradlew assembleDebug || { echo "ERROR: Gradle compilation failed"; exit 1; }
fi

# Install APK onto Fire TV
echo "Installing FireLauncher ($APK_PATH) onto Fire TV..."
adb -s "$FIRETV_IP:5555" install -r -d "$APK_PATH"

# Verify successful installation
adb -s "$FIRETV_IP:5555" shell pm list packages | grep "com.example.firelauncher" || {
    echo "ERROR: FireLauncher installation verification failed."
    exit 1
}

echo "SUCCESS: FireLauncher installed successfully."
```

---

### STEP 5: Execute Amazon Package Disable Routine (Debloating)
Execute the non-root safe package disabling commands for user 0 across all 5 specified packages.

```bash
DEBLOAT_PACKAGES=(
    "com.amazon.advertising.firetv"
    "com.amazon.device.sale.service"
    "com.amazon.client.metrics"
    "com.amazon.device.crashmanager"
    "com.amazon.ftv.screensaver"
)

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
Inspect the list of disabled packages on the device to confirm that all 5 target packages are deactivated.

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
    echo "SUCCESS: All 5 Amazon packages successfully disabled."
else
    echo "NOTE: Some packages were not reported in disabled state. They may already be inactive or restricted by this Fire OS build."
fi
```

---

### STEP 7: Launch FireLauncher & Reboot Device
Start FireLauncher once to register the leanback activity, then initiate a clean system reboot to flush RAM, terminate any cached Amazon ad services, and apply all modifications cleanly.

```bash
# Launch FireLauncher activity
echo "Launching FireLauncher activity..."
adb -s "$FIRETV_IP:5555" shell am start -n com.example.firelauncher/.MainActivity

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

If the user ever wishes to re-enable any or all of the 5 packages, the agent can execute:

```bash
# Re-enable all 5 packages
adb -s "$FIRETV_IP:5555" shell pm enable com.amazon.advertising.firetv
adb -s "$FIRETV_IP:5555" shell pm enable com.amazon.device.sale.service
adb -s "$FIRETV_IP:5555" shell pm enable com.amazon.client.metrics
adb -s "$FIRETV_IP:5555" shell pm enable com.amazon.device.crashmanager
adb -s "$FIRETV_IP:5555" shell pm enable com.amazon.ftv.screensaver
```
