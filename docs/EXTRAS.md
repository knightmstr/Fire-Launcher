# Extras

[← Back to README](../README.md)

These are optional. FireLauncher works without them, but they make it feel like the Fire TV's own home screen.

| Extra | What you get |
|---|---|
| [Home button](#home-button) | Pressing **Home** on the remote opens FireLauncher, not Amazon's home |
| [Screensaver](#screensaver) | When the TV is idle, it shows your wallpaper, clock and weather instead of Amazon's screensaver |
| [Keep awake](#keep-awake) | The Fire TV doesn't go to sleep and show *No signal*, so the screensaver keeps running |
| [Now playing](#now-playing) | The home screen shows what's playing, with pause and skip |
| [Your own wallpapers](#your-own-wallpapers) | Your photos as the background |

The first three need a [one-time permission](#the-one-time-permission). You set it up once, and it covers all three.

> **On a computer:** run these in the `platform-tools` folder and type `.\adb` (Windows) or `./adb` (Mac, Linux) instead of `adb`. See [Get adb](INSTALL.md#1-get-adb).


---

## Home button

Pressing Home on the remote normally opens Amazon's home screen. With this on, it opens FireLauncher instead.

1. Set up the [one-time permission](#the-one-time-permission).
2. Go to **⚙ → Launcher settings → Fire TV → Home button opens FireLauncher** and turn it on.

This uses a small accessibility service that only notices when Amazon's home opens. FireLauncher switches it on by itself, so there's nothing to do in Accessibility settings.

Amazon's home may flash for a moment before FireLauncher comes back. Fire OS doesn't allow avoiding that without rooting.

## Screensaver

When the TV is idle, Amazon's screensaver is replaced by FireLauncher's. You get your wallpaper with a slow zoom, the clock, date and weather, and what's playing. The clock moves around slowly so nothing burns into the screen, and the screen goes darker at night.

1. Set up the [one-time permission](#the-one-time-permission).
2. Go to **⚙ → Launcher settings → Screensaver → Use as Fire TV screensaver** and turn it on.
3. On Fire OS 8, restart the Fire TV once.

In the same section you can choose **Start after**, which sets how long FireLauncher waits on the home screen, and turn **Dim at night** (10pm to 6am) on or off. Inside other apps, Fire TV's own *Display & Sounds → Screensaver → Start Time* decides when it starts.

## Keep awake

By default the Fire TV goes to sleep after a while. The screen goes black and the TV says *No signal*. With this on, it stays awake and the screensaver keeps showing.

1. Set up the [one-time permission](#the-one-time-permission).
2. Go to **⚙ → Launcher settings → Screensaver → Keep Fire TV awake** and turn it on.

## Now playing

Shows the app, the title and ⏮ ⏯ ⏭ controls for whatever's playing in Spotify, YouTube, Plex and others. It appears on the home screen and in the screensaver. FireLauncher only reads what's playing, never your notifications.

Many Fire TVs hide the screen where you'd allow this, so allow it once with adb (see [Install](INSTALL.md) for getting adb):

```bash
adb connect 192.168.1.50:5555    # your TV's IP
adb shell cmd notification allow_listener com.knightmstr.firelauncher/com.knightmstr.firelauncher.media.MediaListenerService
```

To undo it, run the same command with `disallow_listener` in place of `allow_listener`. Uninstalling FireLauncher also removes it.

## Your own wallpapers

Copy photos straight in, with no permission needed:

```bash
adb connect 192.168.1.50:5555    # your TV's IP
adb push ~/storage/dcim/Camera/IMG_1234.jpg /sdcard/Android/data/com.knightmstr.firelauncher/files/Pictures/
```

That path is for Termux on a phone (run `termux-setup-storage` once first). From a Windows computer, it looks like this (keep the quotes if the path has spaces):

```powershell
.\adb push "C:\Users\You\Pictures\beach.jpg" /sdcard/Android/data/com.knightmstr.firelauncher/files/Pictures/
```

Landscape photos look best.

Photos in an album called **FireLauncher** also work. Turn on **⚙ → Launcher settings → Wallpaper → Allow photo access** (it shows when *Wallpapers from* is set to your photos). Choose which wallpapers to use under **Launcher settings → Wallpaper → Wallpapers from**.

---

## The one-time permission

The Home button, screensaver and keep-awake options change Fire TV settings, so FireLauncher needs a permission Android only grants through ADB. You do this once, and it stays until you uninstall FireLauncher. Turning an option off later puts Amazon's setting back.

### On the TV (no phone needed)

1. Turn on **ADB debugging** (it's already on if you installed with adb): **Settings → My Fire TV** (called **Device & Software** on newer models) **→ Developer options**. If Developer options is missing, go to **My Fire TV → About** and press OK on your device name 7 times.
2. In FireLauncher, go to **⚙ → Launcher settings** and choose any of the three options. The setup screen opens.
3. Choose **Set up on this TV**. When the TV asks *Allow USB debugging?*, tick **Always allow** and press **OK**.

The option turns on by itself. FireLauncher connects to the TV's own ADB, only to grant itself this permission. You can turn ADB debugging off afterwards.

### Or from a phone (Termux) or computer

```bash
adb connect 192.168.1.50:5555    # your TV's IP
adb shell pm grant com.knightmstr.firelauncher android.permission.WRITE_SECURE_SETTINGS
```

This only gives the permission. **Then turn each option on in Launcher settings.** Nothing changes until you do.

---

## If it doesn't work

**The TV shows *No signal* after a while.** Keep awake isn't on. Check it:

```bash
adb shell settings get secure sleep_timeout             # -1 means keep awake is on
adb shell settings get secure screensaver_components    # should name com.knightmstr.firelauncher
```

If the option shows **On** in Launcher settings but these show Amazon's values, open FireLauncher once. It puts its settings back after updates and restarts.

**Home still opens Amazon's home.** Check that *Home button opens FireLauncher* shows **On**. If it does, go to **Settings → Accessibility** and check that **FireLauncher** is on there. Updates and restarts can switch it off; opening FireLauncher puts it back.

**Set up on this TV fails.** Check that ADB debugging is on, then try again and choose **Always allow**. If it still fails, use the phone method above.
