# Using Onyx TV

[← Back to README](../README.md)

## Remote

| Button | What it does |
|---|---|
| **OK** | Open the app |
| **Hold OK** or **☰** | App options: favourite, move, rename, hide, Kids mode, App info |
| **Mic** | Voice search |
| **Back** | Closes menus and search. It never leaves Onyx TV; to leave, open another app |
| **⚙** (top right) | Launcher settings or Fire TV settings |

In settings, sliders move with **◀ ▶**, and every other row changes with **OK**.

## Home layouts

**Dock** is the default. Choose under **⚙ → Launcher settings → Home screen → Home layout**:

- **Dock**: your favourite apps are always along the bottom.
- **Clean**: just the clock, date and weather over your wallpaper. Press any button and your apps and the top bar appear, then fade away after a few seconds. That first press only brings them back, so it never opens an app by accident.

Your apps: open **All Apps** (the round button at the end of the dock), hold OK on an app and choose **Add to Favorites**. Hold OK on a favourite to move it.

## Set up from phone

**⚙ → Launcher settings → Set up from phone** shows a QR code. Scan it with a phone on the same Wi-Fi to:

- **Apps:** add, remove and reorder favourites, hide or show apps, and rename them
- **Photos:** upload wallpaper photos
- **Settings:** home layout, tile size and shape, wallpaper and screensaver options, your own wallpaper image links (when *Wallpapers from* is **My links**), and the hours the screensaver dims at night
- **Backup & restore:** save your setup to a file and load it again later or on another Fire TV. Kids mode and its PIN are never in the file.

There's no adb and no account. The link only works while that screen is open on the TV, stops after 10 minutes without use, and is new every time. While Kids mode is on, the phone can look but can't change anything. Kids mode, updates and the Fire TV extras can only be changed on the TV.

## Good to know

- **Amazon's home may flash** for a moment when you press Home, before Onyx TV comes back. Fire OS doesn't allow avoiding this without rooting.
- **Kids mode locks the launcher, not the device.** For device-wide limits, also use *Settings → Preferences → Parental Controls*.
- **Forgot the Kids mode PIN?** Clear Onyx TV's data with adb: `adb connect 192.168.1.50:5555`, then `adb shell pm clear com.knightmstr.onyxtv`. This also resets Onyx TV's settings, favourites and wallpapers. Open Onyx TV afterwards and set it up again.
- **When the screensaver starts** inside other apps follows Fire TV's *Display & Sounds → Screensaver → Start Time*.
- **Updates install over each other** only when they're signed with the same key, which is how the official releases are signed.

## Going back to Amazon's home

Turn off the Home button, screensaver and keep-awake options in **⚙ → Launcher settings**. See [Going back to Amazon's home](INSTALL.md#going-back-to-amazons-home).
