# Install

[← Back to README](../README.md)

Takes about 10 minutes. You need a phone or computer **once**, to put FireLauncher on the Fire TV. After that it updates itself.

## 1. Get adb

adb is a small tool that sends commands to the Fire TV over Wi-Fi. Pick one:

**On an Android phone (Termux)**

1. Install **Termux from [F-Droid](https://f-droid.org/packages/com.termux/)** or its [GitHub releases](https://github.com/termux/termux-app/releases). The Play Store version is outdated, and `pkg install` often fails with it.
2. Open Termux and run the following. Answer `y` if it asks anything.

   ```bash
   pkg update && pkg install android-tools wget
   termux-setup-storage    # lets Termux see your Downloads folder; tap Allow
   ```

**On a computer (Windows, Mac or Linux)**

Download Google's [SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools), unzip it, and open a terminal in that folder. On Windows, type `.\adb` wherever this guide says `adb`.

Either way, your phone or computer must be on the **same Wi-Fi** as the Fire TV.

## 2. Turn on ADB debugging on the Fire TV

On newer Fire TVs, **My Fire TV** is called **Device & Software**.

1. Go to **Settings → My Fire TV → Developer options** and turn on **ADB debugging**.
   - No *Developer options*? Go to **My Fire TV → About**, highlight the device name (e.g. *Fire TV Stick 4K*) and press OK 7 times, until you see *You are now a developer*. Then go back and it's there.
2. Find the TV's IP address under **Settings → My Fire TV → About → Network**, for example `192.168.1.50`.
   - Or: **Settings → Network**, highlight your Wi-Fi and press **▶⏸** on the remote.

## 3. Install FireLauncher

**Connect to the TV** (use your TV's IP):

```bash
adb connect 192.168.1.50:5555
```

A box appears on the TV: *Allow USB debugging?* Tick **Always allow from this computer** and press **OK**. adb should say `connected to 192.168.1.50:5555`.
If it says `unauthorized` or `failed to authenticate`, accept the box on the TV and run `adb connect` again.

**Download and install.** Set `V` to the newest version on the [Releases page](https://github.com/knightmstr/Fire-Launcher/releases/latest), then copy the rest as is:

```bash
V=v1.4.0
wget https://github.com/knightmstr/Fire-Launcher/releases/download/$V/FireLauncher-$V-release.apk
adb install -r FireLauncher-$V-release.apk
adb shell am start -n com.knightmstr.firelauncher/.MainActivity
```

`adb install` prints `Success` when it's done, and FireLauncher opens on the TV.

<details>
<summary>Or download the APK in your phone's browser</summary>

On the [Releases page](https://github.com/knightmstr/Fire-Launcher/releases/latest), tap the file ending in `-release.apk`. Then in Termux:

```bash
adb install -r ~/storage/downloads/FireLauncher-v1.4.0-release.apk
```

Type `~/storage/downloads/Fire` and press **Tab** to fill in the name.

</details>

## 4. After installing

FireLauncher is installed, but **pressing Home still opens Amazon's home** until you turn on the *Home button* extra. Until then, open FireLauncher from **Your Apps & Channels** on Amazon's home.

- **Fill your dock:** open **All Apps** (the round button at the end of the dock), hold OK on an app and choose **Add to Favorites**.
- **Make it your home screen:** turn on the **Home button**, **screensaver** and **keep awake**. You can do all of these on the TV. → [Extras](EXTRAS.md)
- **Not for you?** See [Going back to Amazon's home](#going-back-to-amazons-home).

## Updating

You only need adb once. After that, an **Update** button appears at the top of the home screen when a new version is out, or go to **⚙ → Launcher settings → About → App updates**. Every download is checked against its published checksum.

The first time, the Fire TV may ask you to let FireLauncher install apps. Choose **Open "Install unknown apps"** and switch FireLauncher on. If that screen doesn't open, go to **Settings → My Fire TV → Developer options → Install unknown apps**. Android then asks you to confirm each update.

## Going back to Amazon's home

Go to **⚙ → Launcher settings** and turn off **Home button opens FireLauncher**, **Use as Fire TV screensaver** and **Keep Fire TV awake**. Home opens Amazon's home again, and Amazon's screensaver and sleep settings come back. FireLauncher stays installed, and you can open it from *Your Apps & Channels* whenever you like.

## Removing FireLauncher

1. Turn off the three options above first, so Amazon's own settings are put back.
2. If you disabled Amazon's screensaver while [debloating](DEBLOAT.md), turn it back on: `adb shell pm enable com.amazon.ftv.screensaver`.
3. Uninstall:

   ```bash
   adb uninstall com.knightmstr.firelauncher
   ```

## Installing other apps with adb

The same steps work for any APK:

```bash
adb install -r ~/storage/downloads/SomeApp.apk
```

Pick the **Android TV** build of an app if there is one. If you see `INSTALL_FAILED_NO_MATCHING_ABIS`, the APK isn't built for the stick's processor. Check which one you need with `adb shell getprop ro.product.cpu.abi`, which usually says `armeabi-v7a`. Only install APKs from the developer's own site or GitHub.
