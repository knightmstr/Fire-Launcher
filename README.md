# FireLauncher

A clean, ad-free home screen for **Amazon Fire TV**. Your wallpaper fills the screen, with a big clock, the weather and a dock of your favourite apps. No sponsored rows.

Built with Kotlin and Jetpack Compose for TV. Fire OS 6, 7 and 8 (Android 7.1 and later).

**[Download the latest release →](https://github.com/knightmstr/Fire-Launcher/releases/latest)**

<table>
  <tr>
    <td><img src="docs/screenshots/home-clean.jpg" alt="Clean layout: the clock, date and weather over the wallpaper"></td>
    <td><img src="docs/screenshots/home-clean-dock.jpg" alt="Clean layout with the top bar and app dock showing"></td>
  </tr>
  <tr>
    <td align="center"><sub>The <b>Clean</b> layout: just the clock, date and weather over your wallpaper</sub></td>
    <td align="center"><sub>Press any button and your apps and the top bar appear, then fade away again</sub></td>
  </tr>
</table>

---

## Features

**Home**
- Wallpaper-first layout: date, weather and a big clock in the middle, a dock of favourites at the bottom
- Two home layouts: **Dock** (favourites always at the bottom) or **Clean** (only the clock and wallpaper; any button brings the apps back, and they fade away after a few seconds)
- Wallpapers from your own photos (default) or r/TVWallpapersSFW, changing on a timer you choose
- Live weather for any city (Open-Meteo, no account), °C / °F
- Now playing: app, title and ⏮ ⏯ ⏭ for whatever's playing in Spotify, YouTube, Plex…

**Apps**
- All Apps grid with search (the on-screen keyboard opens straight away) and voice search from the remote's mic button
- Sort A–Z, most recent, most used, or your own order
- Hold OK (or ☰) on any app to favourite, reorder, rename, hide, or open App info
- Tile size slider and tile shapes (rounded, square, extra rounded, pill)

**Fire TV**
- Home button opens FireLauncher instead of Amazon's home ([Extras](docs/EXTRAS.md))
- FireLauncher's screensaver replaces Amazon's, with the same wallpaper, clock and weather
- Optional keep-awake, so the Fire TV doesn't sleep on a timer
- Wi-Fi and Fire TV settings one press away, opening Fire OS's own screens

**Family & upkeep**
- Kids mode: only the apps you allow, PIN to leave
- Updates itself from GitHub Releases (checksum-verified; you confirm every install)

---

## Get started

1. **Install FireLauncher.** It takes about 10 minutes and needs a phone or computer once. → **[Install guide](docs/INSTALL.md)**
2. **Make it your home screen** (optional): the Home button, screensaver and keep-awake. You can set these up on the TV itself. → **[Extras](docs/EXTRAS.md)**

After that, updates install from the TV. Not for you? [Going back to Amazon's home](docs/INSTALL.md#going-back-to-amazons-home) is two switches.

## More

- **[Using FireLauncher](docs/USING.md)**: remote buttons, Dock and Clean layouts, good to know
- **[Extras](docs/EXTRAS.md)**: Home button, screensaver, keep awake, Now playing, your own wallpapers
- **[Debloat](docs/DEBLOAT.md)**: turn off Amazon's ad and telemetry services (optional, can be undone)
- **[Permissions & privacy](docs/PRIVACY.md)**: what each permission is for. No tracking, no accounts
- **[Build from source](docs/BUILD.md)**: building, releases, signing, project layout

---

## Credits

- [couch-tv](https://github.com/Seagey/couch-tv) (MIT): home layout, Home button watcher and Fire TV settings approach
- [CleanYourTV](https://github.com/Gathin23/cleanyourtv): debloat workflow inspiration
- [dadb](https://github.com/mobile-dev-inc/dadb) (Apache 2.0): ADB key format and signing for on-TV setup
- [Open-Meteo](https://open-meteo.com): weather and city search
- [r/TVWallpapersSFW](https://www.reddit.com/r/TVWallpapersSFW): optional wallpapers
