# Install

[← Back to README](../README.md)

You need **adb** once, to put FireLauncher on the Fire TV. Use a computer, or your Android phone with **Termux**:

```bash
pkg install android-tools wget
```

## 1. Turn on ADB debugging

1. On the Fire TV, go to **Settings → My Fire TV → About** and press OK on the device name 7 times. This unlocks Developer options.
2. Go back to **My Fire TV → Developer options** and turn on **ADB debugging**.
3. Note the TV's IP address under **Settings → My Fire TV → About → Network**.

## 2. Install FireLauncher

```bash
adb connect <TV-IP>:5555          # press Allow on the TV
wget https://github.com/knightmstr/Fire-Launcher/releases/download/<version>/FireLauncher-<version>-release.apk
adb install -r FireLauncher-<version>-release.apk
adb shell am start -n com.knightmstr.firelauncher/.MainActivity
```

Replace `<version>` with the newest tag on the [Releases page](https://github.com/knightmstr/Fire-Launcher/releases/latest), for example `v1.4.0`.

## 3. Next steps

- Hold OK on apps in **All Apps** and choose **Add to Favorites** to fill your dock.
- Make the **Home button** open FireLauncher, and turn on the **screensaver** and **keep awake**: see [Extras](EXTRAS.md).

## Updating

You only need adb once. After that, new versions install from the TV: an **Update** button appears at the top of the home screen. You can also go to **⚙ → Launcher settings → App updates**. Every download is checked against its published checksum, and Android asks you before it installs.

## Installing other apps with adb

The same steps work for any APK:

```bash
adb install -r ~/storage/downloads/SomeApp.apk
```

In Termux, run `termux-setup-storage` once so it can see your phone's Downloads folder. Pick the **Android TV** build of an app if there is one. If you see `INSTALL_FAILED_NO_MATCHING_ABIS`, the APK isn't built for the stick's processor. Check which one you need with `adb shell getprop ro.product.cpu.abi`, which usually says `armeabi-v7a`.

## Removing FireLauncher

```bash
adb uninstall com.knightmstr.firelauncher
```

Before uninstalling, turn off the Home button, screensaver and keep-awake options in Launcher settings. That way Amazon's own settings are put back.
