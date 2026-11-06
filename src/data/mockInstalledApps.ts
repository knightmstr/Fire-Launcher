import { SimulatedApp } from '../types';

export const SIMULATED_INSTALLED_APPS: SimulatedApp[] = [
  {
    id: 'com.amazon.avod',
    packageName: 'com.amazon.avod',
    activityName: 'com.amazon.avod.client.activity.HomeActivity',
    label: 'Prime Video',
    iconName: 'Tv',
    bannerGradient: 'from-blue-600 via-sky-600 to-indigo-900',
    accentColor: '#00A8E1', // Prime Blue
    category: 'streaming',
    description: 'Watch movies, TV shows, and Amazon Originals'
  },
  {
    id: 'com.netflix.ninja',
    packageName: 'com.netflix.ninja',
    activityName: 'com.netflix.ninja.MainActivity',
    label: 'Netflix',
    iconName: 'Film',
    bannerGradient: 'from-red-600 via-rose-700 to-black',
    accentColor: '#E50914', // Netflix Red
    category: 'streaming',
    description: 'Stream unlimited movies, TV shows, and mobile games'
  },
  {
    id: 'com.google.android.youtube.tv',
    packageName: 'com.google.android.youtube.tv',
    activityName: 'com.google.android.youtube.tv.MainActivity',
    label: 'YouTube',
    iconName: 'PlayCircle',
    bannerGradient: 'from-red-600 via-neutral-900 to-zinc-950',
    accentColor: '#FF0033', // YouTube Red
    category: 'streaming',
    description: 'Discover popular music videos, news, and gaming'
  },
  {
    id: 'com.plexapp.android',
    packageName: 'com.plexapp.android',
    activityName: 'com.plexapp.android.ui.activity.PreplayActivity',
    label: 'Plex',
    iconName: 'Clapperboard',
    bannerGradient: 'from-amber-500 via-yellow-600 to-neutral-900',
    accentColor: '#E5A00D', // Plex Amber
    category: 'streaming',
    description: 'Stream free movies and your personal media collection'
  },
  {
    id: 'org.xbmc.kodi',
    packageName: 'org.xbmc.kodi',
    activityName: 'org.xbmc.kodi.Splash',
    label: 'Kodi',
    iconName: 'MonitorPlay',
    bannerGradient: 'from-cyan-500 via-blue-700 to-slate-950',
    accentColor: '#13B5EA', // Kodi Cyan
    category: 'streaming',
    description: 'Ultimate open-source home theater entertainment hub'
  },
  {
    id: 'tv.twitch.android.app',
    packageName: 'tv.twitch.android.app',
    activityName: 'tv.twitch.android.apps.TvSearchActivity',
    label: 'Twitch',
    iconName: 'Gamepad2',
    bannerGradient: 'from-purple-600 via-indigo-700 to-violet-950',
    accentColor: '#9146FF', // Twitch Violet
    category: 'gaming',
    description: 'Live streams of your favorite games, esports, and IRL'
  },
  {
    id: 'com.spotify.tv.android',
    packageName: 'com.spotify.tv.android',
    activityName: 'com.spotify.tv.android.SpotifyTVActivity',
    label: 'Spotify Music',
    iconName: 'Music',
    bannerGradient: 'from-emerald-500 via-green-600 to-zinc-950',
    accentColor: '#1DB954', // Spotify Green
    category: 'streaming',
    description: 'Play music and podcasts on the big screen'
  },
  {
    id: 'org.videolan.vlc',
    packageName: 'org.videolan.vlc',
    activityName: 'org.videolan.vlc.gui.video.VideoPlayerActivity',
    label: 'VLC Player',
    iconName: 'Video',
    bannerGradient: 'from-orange-500 via-amber-600 to-neutral-900',
    accentColor: '#FF8800', // VLC Orange
    category: 'utility',
    description: 'Play local videos, audio, and network streams'
  },
  {
    id: 'com.disney.disneyplus',
    packageName: 'com.disney.disneyplus',
    activityName: 'com.disney.disneyplus.SplashActivity',
    label: 'Disney+',
    iconName: 'Sparkles',
    bannerGradient: 'from-sky-700 via-blue-900 to-black',
    accentColor: '#113CCF', // Disney Blue
    category: 'streaming',
    description: 'Home of Disney, Pixar, Marvel, Star Wars, and Nat Geo'
  },
  {
    id: 'com.retroarch',
    packageName: 'com.retroarch',
    activityName: 'com.retroarch.browser.mainmenu.MainMenuActivity',
    label: 'RetroArch',
    iconName: 'Gamepad',
    bannerGradient: 'from-fuchsia-600 via-purple-800 to-zinc-950',
    accentColor: '#D946EF', // Retro Magenta
    category: 'gaming',
    description: 'Classic gaming console and arcade frontend emulator'
  }
];

export const SIMULATED_SYSTEM_SHORTCUTS: SimulatedApp[] = [
  {
    id: 'sys:wifi',
    packageName: 'system.wifi',
    activityName: 'Settings.Wifi',
    label: 'Wi-Fi',
    iconName: 'Wifi',
    bannerGradient: 'from-sky-500/20 to-sky-950/40',
    accentColor: '#00B4D8',
    category: 'system',
    isSystemShortcut: true,
    systemAction: 'android.settings.WIFI_SETTINGS',
    description: 'Configure wireless networks and signal preferences'
  },
  {
    id: 'sys:display',
    packageName: 'system.display',
    activityName: 'Settings.Display',
    label: 'Display',
    iconName: 'Tv',
    bannerGradient: 'from-purple-500/20 to-purple-950/40',
    accentColor: '#7209B7',
    category: 'system',
    isSystemShortcut: true,
    systemAction: 'android.settings.DISPLAY_SETTINGS',
    description: 'Resolution, match original frame rate, HDR & display calibration'
  },
  {
    id: 'sys:sound',
    packageName: 'system.sound',
    activityName: 'Settings.Sound',
    label: 'Sound',
    iconName: 'Volume2',
    bannerGradient: 'from-emerald-500/20 to-emerald-950/40',
    accentColor: '#06D6A0',
    category: 'system',
    isSystemShortcut: true,
    systemAction: 'android.settings.SOUND_SETTINGS',
    description: 'Surround sound audio format, dialogue boost, volume leveling'
  },
  {
    id: 'sys:bluetooth',
    packageName: 'system.bluetooth',
    activityName: 'Settings.Bluetooth',
    label: 'Bluetooth',
    iconName: 'Bluetooth',
    bannerGradient: 'from-blue-500/20 to-blue-950/40',
    accentColor: '#4361EE',
    category: 'system',
    isSystemShortcut: true,
    systemAction: 'android.settings.BLUETOOTH_SETTINGS',
    description: 'Pair game controllers, headphones, and external remotes'
  },
  {
    id: 'sys:settings',
    packageName: 'system.settings',
    activityName: 'Settings.Main',
    label: 'All Settings',
    iconName: 'Settings',
    bannerGradient: 'from-amber-500/20 to-amber-950/40',
    accentColor: '#FF9F1C',
    category: 'system',
    isSystemShortcut: true,
    systemAction: 'android.settings.SETTINGS',
    description: 'Full Fire OS device preferences, accounts, and applications'
  },
  {
    id: 'sys:cleantv',
    packageName: 'system.cleantv',
    activityName: 'CleanTV.Optimizer',
    label: 'CleanTV Optimizer',
    iconName: 'ShieldCheck',
    bannerGradient: 'from-emerald-500/25 to-teal-950/50',
    accentColor: '#10B981',
    category: 'system',
    isSystemShortcut: true,
    systemAction: 'com.example.firelauncher.action.CLEANTV',
    description: 'Debloat Fire OS bloatware, disable telemetry, block ads, and free up RAM'
  }
];
