# AI_STEPS: set up Onyx TV on a Fire TV, hands-free

**For AI coding agents** (Claude Code, Gemini CLI, Codex…). A person can hand you this file, or its URL, with something like *"set up Onyx TV on my Fire TV"*. You do the whole job: install adb, connect to the TV, install Onyx TV, switch on the extras, debloat, check everything works, and report back.

**Ground rules**
- **Do everything yourself.** Never ask the person to type commands.
- **Only ask for what you can't do or know:** their choices (Step 0), the TV's IP address (Step 3), and the two physical actions on the TV (turning on ADB debugging, pressing OK on the *Allow USB debugging?* box).
- **Don't scan the network** or probe other devices. Only connect to the address the person gives you.
- **Ask before anything that needs admin rights** (`sudo`, an installer), and before uninstalling anything.
- **Keep the person informed** in plain words, one line per stage. No jargon.
- Everything here can be undone (see [Undo](#undo)). Nothing needs root.
- Commands are bash. On Windows, use PowerShell equivalents (noted where they differ) or Git Bash if it's installed. This file doesn't depend on the repo: don't clone it.

The values used below:

```bash
PKG=com.knightmstr.onyxtv
REPO=knightmstr/Onyx-TV
```

---

## Step 0: Ask once, up front

Send **one** message with these choices. Recommended answers are marked. If the person says "just do it" or "defaults", use the recommended ones.

1. **Home button opens Onyx TV** instead of Amazon's home. *(Recommended: yes)*
2. **Onyx TV's screensaver** (wallpaper, clock and weather) instead of Amazon's. *(Recommended: yes)*
3. **Keep the Fire TV awake**, so it doesn't sleep and show *No signal*. *(Recommended: yes)*
4. **Now playing** on the home screen, for music and video controls. *(Recommended: yes)*
5. **Debloat:** turn off Amazon's ad and usage-data services. *(Recommended: yes)*
   - If 2 is yes, also turn off Amazon's own screensaver app. If 2 is no, leave it alone, or they'd have no screensaver at all.
6. **Restart the Fire TV at the end** to finish cleanly. *(Recommended: yes)*

In the same message, tell them what you'll need from them: to have the TV on and the remote nearby, and that their computer or phone must be on the **same Wi-Fi** as the Fire TV.

---

## Step 1: Make sure adb is available

```bash
adb version
```

If that fails, install it. Pick the line for this system and ask first if it needs admin rights:

| System | Command |
|---|---|
| macOS (Homebrew) | `brew install android-platform-tools` |
| Debian / Ubuntu | `sudo apt-get install -y adb` |
| Fedora | `sudo dnf install -y android-tools` |
| Arch | `sudo pacman -S android-tools` |
| Windows | `winget install --id Google.PlatformTools -e` (open a new terminal afterwards) |
| Android (Termux) | `pkg update -y && pkg install -y android-tools` |

**No package manager?** Download Google's Platform-Tools zip into a folder you create, unzip it, and use that `adb` by its full path:
`https://dl.google.com/android/repository/platform-tools-latest-{windows|darwin|linux}.zip`

You also need a way to download files: `curl` (built into macOS, Windows 10+ as `curl.exe`, and most Linux). On Termux, `pkg install -y curl`.

---

## Step 2: Ask for ADB debugging on the TV

Tell the person (one message, plain words):

> On the Fire TV, go to **Settings → My Fire TV → Developer options** and turn on **ADB debugging**. On newer models *My Fire TV* is called **Device & Software**.
> If there's no *Developer options*: go to **My Fire TV → About**, highlight the device name and press OK 7 times, until it says *You are now a developer*. Then go back and it's there.
> Tell me when it's on.

Skip this if adb already connects in Step 4.

---

## Step 3: Get the Fire TV's IP address

Don't scan the network or go looking for devices. Ask the person for the IP address.

If `adb devices` already lists a connected Fire TV (a line ending in `device`, with `:5555`), you can confirm that one with them instead: "I can see a Fire TV at 192.168.1.50. Is that the one?"

Otherwise, ask (you can include this in the Step 2 message):

> On the Fire TV, go to **Settings → My Fire TV → About → Network** (on newer models *My Fire TV* is called **Device & Software**). Tell me the **IP address**, like 192.168.1.50.
> Quick way: **Settings → Network**, highlight your Wi-Fi and press **▶⏸** on the remote.

Check that it looks like an IPv4 address (four numbers from 0 to 255, separated by dots) before using it. If it doesn't, ask again and show an example.

---

## Step 4: Connect and get approved

```bash
TV=192.168.1.50:5555            # the IP the person gave you, plus :5555
adb connect $TV
adb devices
```

If the state is `unauthorized`, or this is the first connection, tell the person:

> A box saying **Allow USB debugging?** is on the TV. Tick **Always allow from this computer** and press **OK**.

Then poll `adb devices` every few seconds for up to 2 minutes until `$TV` shows `device`. If it shows `offline`, run `adb disconnect $TV` and then `adb connect $TV` again. If it never connects, ask the person to check the IP address, that ADB debugging is on, and that both devices are on the same Wi-Fi. Try again once before giving up.

Use `adb -s $TV …` for every command from here on.

---

## Step 5: Check the TV

```bash
adb -s $TV shell getprop ro.product.manufacturer   # Amazon
adb -s $TV shell getprop ro.build.version.sdk       # 25 or higher (Fire OS 6+)
adb -s $TV shell pm list packages | grep -iE "onyxtv|firelauncher"
```

- An SDK below 25 isn't supported. Stop and tell the person.
- If `package:com.knightmstr.onyxtv` is already there, this is an update. Go on; settings are kept.
- `package:com.knightmstr.firelauncher` is **FireLauncher**, this app's old name. It's a separate app now, and it can't be updated to Onyx TV. Tell the person, then put Amazon's settings back and remove it:

  ```bash
  adb -s $TV shell am broadcast -a com.knightmstr.firelauncher.SETUP -n com.knightmstr.firelauncher/.home.AdbSetupReceiver \
      --ez home_button false --ez screensaver false --ez keep_awake false   # only works on FireLauncher v1.4.1; fine if it does nothing
  adb -s $TV uninstall com.knightmstr.firelauncher
  ```

  Older FireLauncher versions don't have that switch. If the Home button or screensaver still points to FireLauncher afterwards, Step 7 takes over both anyway.
- `package:com.example.firelauncher` is an old test build. Offer to remove it: `adb -s $TV uninstall com.example.firelauncher`.

---

## Step 6: Download and install the latest release

```bash
API=https://api.github.com/repos/$REPO/releases/latest
APK_URL=$(curl -fsSL $API | grep -o '"browser_download_url": *"[^"]*-release\.apk"' | sed 's/.*"\(https[^"]*\)"/\1/')
SUM_URL=$(curl -fsSL $API | grep -o '"browser_download_url": *"[^"]*\.sha256"' | sed 's/.*"\(https[^"]*\)"/\1/')
curl -fL -o OnyxTV-release.apk "$APK_URL"
curl -fL -o OnyxTV-checksums.sha256 "$SUM_URL"
```

**Check the download** against the published checksum. The SHA-256 of `OnyxTV-release.apk` must match the line for the `-release.apk` file in `OnyxTV-checksums.sha256`. Use `sha256sum`, `shasum -a 256`, or `Get-FileHash` on Windows. If it doesn't match, delete it, download again once, and stop if it still fails.

```bash
adb -s $TV install -r OnyxTV-release.apk      # prints: Success
```

- `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: a Onyx TV signed with another key is installed. **Ask first.** Uninstalling resets its settings and favourites. If they agree, run `adb -s $TV uninstall $PKG` and install again.
- `INSTALL_FAILED_NO_MATCHING_ABIS` or other errors: stop and report the exact message.

Always use the signed release APK, never a local build. In-app updates only install over the release signature.

---

## Step 7: Start Onyx TV and switch on the extras

```bash
adb -s $TV shell am start -n $PKG/.MainActivity
sleep 3
```

**Home button, screensaver and keep awake** (choices 1–3). Grant the permission once, then switch on only what they chose. Use `false` for any they declined, or leave that extra out.

```bash
adb -s $TV shell pm grant $PKG android.permission.WRITE_SECURE_SETTINGS
adb -s $TV shell am broadcast -a $PKG.SETUP -n $PKG/.home.AdbSetupReceiver \
    --ez home_button true --ez screensaver true --ez keep_awake true
```

The reply ends with something like `data="granted=true home_button=true screensaver=true keep_awake=true"`. Check that every extra they chose says `true`. `restart_needed=true` means Fire OS 8: the restart in Step 9 is then required, not optional.

This does the same as the switches in Onyx TV's *Launcher settings*. The person can turn any of them off there later, and Amazon's setting comes back.

**Now playing** (choice 4):

```bash
adb -s $TV shell cmd notification allow_listener $PKG/$PKG.media.MediaListenerService
```

---

## Step 8: Debloat (choice 5)

```bash
for p in com.amazon.advertising.firetv com.amazon.device.sale.service \
         com.amazon.client.metrics com.amazon.device.crashmanager; do
  adb -s $TV shell pm disable-user --user 0 $p
done
# Only if they chose Onyx TV's screensaver (choice 2):
adb -s $TV shell pm disable-user --user 0 com.amazon.ftv.screensaver
```

Each prints `new state: disabled-user`. If a package isn't found or can't be disabled on this Fire OS, that's fine. Note it in the report and go on.

---

## Step 9: Restart and check it works

If they chose to restart (choice 6), or `restart_needed=true`:

```bash
adb -s $TV reboot
```

Tell the person the TV is restarting. Wait about a minute, then run `adb connect $TV` until it shows `device` again; it can take up to 2 minutes.

Then **check** (after a restart, or straight away if there wasn't one):

```bash
adb -s $TV shell settings get secure enabled_accessibility_services   # contains com.knightmstr.onyxtv (Home button)
adb -s $TV shell settings get secure screensaver_components           # com.knightmstr.onyxtv/... (screensaver)
adb -s $TV shell settings get secure sleep_timeout                    # -1 (keep awake)
adb -s $TV shell pm list packages -d                                  # the debloated packages
adb -s $TV shell input keyevent KEYCODE_HOME; sleep 3
adb -s $TV shell dumpsys activity activities | grep -m1 -E "mResumedActivity|topResumedActivity"
```

The last line should name `com.knightmstr.onyxtv` if the Home button was chosen. Amazon's home may show for a moment first.

If an extra isn't on, open Onyx TV once (`am start` as in Step 7). It puts its settings back after updates and restarts. Then check again. If something still fails, say exactly what, and point to **Set up on this TV** in Onyx TV's *Launcher settings* as a fallback.

---

## Step 10: Report back

Tell the person, briefly:
- what's done: the version installed, each extra on or off, and the services turned off
- anything that didn't work, and what to do about it
- that updates now install from the TV: an **Update** button appears on the home screen when a new version is out
- that they can turn ADB debugging off again if they like. Onyx TV keeps working, and its permissions stay
- how to undo: switch the extras off in *⚙ → Launcher settings*. You can also undo everything for them (below)

Delete the downloaded `OnyxTV-release.apk` and `OnyxTV-checksums.sha256`.

---

## Undo

Run these when the person asks. Connect first, as in Steps 3–4.

```bash
# Extras off: puts Amazon's Home, screensaver and sleep settings back
adb -s $TV shell am broadcast -a $PKG.SETUP -n $PKG/.home.AdbSetupReceiver \
    --ez home_button false --ez screensaver false --ez keep_awake false
adb -s $TV shell cmd notification disallow_listener $PKG/$PKG.media.MediaListenerService

# Debloat undone
for p in com.amazon.advertising.firetv com.amazon.device.sale.service com.amazon.client.metrics \
         com.amazon.device.crashmanager com.amazon.ftv.screensaver; do
  adb -s $TV shell pm enable $p
done

# Remove Onyx TV (only if asked; turn the extras off first, as above)
adb -s $TV uninstall $PKG
adb -s $TV reboot
```
