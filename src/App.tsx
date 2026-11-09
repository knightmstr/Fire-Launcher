import React, { useState } from 'react';
import {
  Download,
  FileCode,
  Terminal,
  Tv,
  Flame,
  Star,
  Layers,
  Sparkles,
  CheckCircle2,
  ExternalLink,
  Info,
  Clock,
  Eye,
  Github,
  ShieldCheck,
  Volume2,
  VolumeX,
  Zap
} from 'lucide-react';
import { FireTvSimulator } from './components/FireTvSimulator';
import { FireRemote } from './components/FireRemote';
import { FileViewerModal } from './components/FileViewerModal';
import { AdbGuideModal } from './components/AdbGuideModal';
import { CleanTvModal } from './components/CleanTvModal';
import { downloadAndroidProjectZip } from './utils/zipExporter';
import { ANDROID_PROJECT_FILES } from './data/projectFiles';
import { isSoundEnabled, toggleSound } from './utils/soundEffects';

export default function App() {
  const [isFileModalOpen, setIsFileModalOpen] = useState(false);
  const [isAdbModalOpen, setIsAdbModalOpen] = useState(false);
  const [isCleanTvModalOpen, setIsCleanTvModalOpen] = useState(false);
  const [selectedInitialFile, setSelectedInitialFile] = useState<string | undefined>(undefined);

  const [isDownloading, setIsDownloading] = useState(false);
  const [showOverscanGuides, setShowOverscanGuides] = useState(false);
  const [timeFormat24, setTimeFormat24] = useState(false);
  const [audioMuted, setAudioMuted] = useState(!isSoundEnabled());

  // Communication between remote and simulator
  const [remoteTrigger, setRemoteTrigger] = useState<{ action: string; timestamp: number } | null>(
    null
  );
  const [holdProgress, setHoldProgress] = useState(0);
  const [isHoldingOk, setIsHoldingOk] = useState(false);

  const triggerRemoteAction = (action: string) => {
    setRemoteTrigger({ action, timestamp: Date.now() });
  };

  const handleDownloadZip = async () => {
    setIsDownloading(true);
    try {
      await downloadAndroidProjectZip();
    } finally {
      setIsDownloading(false);
    }
  };

  const openFileInModal = (path: string) => {
    setSelectedInitialFile(path);
    setIsFileModalOpen(true);
  };

  return (
    <div className="min-h-screen bg-[#0A0C10] text-neutral-100 flex flex-col font-sans selection:bg-orange-500 selection:text-white">
      {/* Top Application Bar */}
      <header className="sticky top-0 z-30 border-b border-neutral-800/80 bg-[#0F1117]/95 backdrop-blur-md px-6 py-3.5">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-4">
          {/* Project Title & Badges */}
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-amber-500 via-orange-500 to-red-600 flex items-center justify-center shadow-lg shadow-orange-600/25">
              <Flame className="w-6 h-6 text-white fill-white" />
            </div>
            <div>
              <div className="flex items-center space-x-2.5">
                <h1 className="text-lg font-bold text-white tracking-tight">FireLauncher</h1>
                <span className="px-2 py-0.5 rounded-full bg-orange-500/15 border border-orange-500/30 text-orange-400 text-[10px] font-semibold tracking-wide uppercase">
                  Fire TV • Compose TV
                </span>
                <span className="hidden sm:inline-block px-2 py-0.5 rounded-full bg-blue-500/15 border border-blue-500/30 text-blue-400 text-[10px] font-mono">
                  Kotlin 2.0.20 • AGP 8.5.2
                </span>
              </div>
              <p className="text-xs text-neutral-400 mt-0.5">
                Custom Leanback & Home screen launcher for Amazon Fire TV (Fire OS)
              </p>
            </div>
          </div>

          {/* Quick Action Buttons */}
          <div className="flex items-center space-x-2.5">
            {/* CleanYourTV Button */}
            <button
              onClick={() => setIsCleanTvModalOpen(true)}
              className="px-3.5 py-1.5 rounded-xl bg-emerald-950/40 hover:bg-emerald-900/50 text-emerald-300 text-xs font-semibold flex items-center space-x-2 border border-emerald-500/40 transition active:scale-95 shadow-md shadow-emerald-500/10"
              title="Open CleanYourTV safe non-root debloat script generator and RAM optimizer"
            >
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              <span>CleanYourTV Studio</span>
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
            </button>

            {/* Sound Toggle */}
            <button
              onClick={() => {
                const newState = toggleSound();
                setAudioMuted(!newState);
              }}
              className="p-2 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 hover:text-white border border-neutral-700/60 transition active:scale-95"
              title={audioMuted ? 'Unmute TV Navigation Sound Effects' : 'Mute TV Navigation Sound Effects'}
            >
              {audioMuted ? <VolumeX className="w-4 h-4 text-neutral-400" /> : <Volume2 className="w-4 h-4 text-amber-400" />}
            </button>

            <button
              onClick={() => {
                setSelectedInitialFile(undefined);
                setIsFileModalOpen(true);
              }}
              className="px-3.5 py-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-200 text-xs font-semibold flex items-center space-x-2 border border-neutral-700/60 transition active:scale-95"
            >
              <FileCode className="w-4 h-4 text-orange-400" />
              <span>Explore Code ({ANDROID_PROJECT_FILES.length} Files)</span>
            </button>

            <button
              onClick={() => setIsAdbModalOpen(true)}
              className="px-3.5 py-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-200 text-xs font-semibold flex items-center space-x-2 border border-neutral-700/60 transition active:scale-95"
            >
              <Terminal className="w-4 h-4 text-blue-400" />
              <span>ADB Guide</span>
            </button>

            <button
              onClick={handleDownloadZip}
              disabled={isDownloading}
              className="px-4 py-1.5 rounded-xl bg-gradient-to-r from-orange-600 to-amber-600 hover:from-orange-500 hover:to-amber-500 text-white text-xs font-bold flex items-center space-x-2 shadow-lg shadow-orange-600/30 transition active:scale-95"
            >
              <Download className="w-4 h-4" />
              <span>{isDownloading ? 'Packaging...' : 'Download Project (.zip)'}</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-6 space-y-8">
        {/* Simulator & Remote Split Layout */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Fire TV Simulator (16:9 Leanback Experience) */}
          <div className="lg:col-span-8 flex flex-col space-y-3">
            <div className="flex items-center justify-between px-1">
              <div className="flex items-center space-x-2 text-xs text-neutral-400 font-medium">
                <Tv className="w-4 h-4 text-orange-400" />
                <span>Interactive Fire TV Screen (1080p Landscape Emulation)</span>
              </div>

              {/* Viewport controls */}
              <div className="flex items-center space-x-2">
                <button
                  onClick={() => setShowOverscanGuides((prev) => !prev)}
                  className={`px-2.5 py-1 rounded-lg text-[11px] font-medium border transition flex items-center space-x-1.5 ${
                    showOverscanGuides
                      ? 'bg-amber-500/20 text-amber-300 border-amber-500/40'
                      : 'bg-neutral-900 text-neutral-400 border-neutral-800 hover:text-neutral-200'
                  }`}
                  title="Toggle 48dp TV overscan safe boundary indicator"
                >
                  <Eye className="w-3.5 h-3.5" />
                  <span>48dp Overscan Guides</span>
                </button>

                <button
                  onClick={() => setTimeFormat24((prev) => !prev)}
                  className="px-2.5 py-1 rounded-lg text-[11px] font-medium bg-neutral-900 border border-neutral-800 hover:text-neutral-200 text-neutral-400 transition flex items-center space-x-1.5"
                  title="Toggle between 12-hour (AM/PM) and 24-hour clock formats"
                >
                  <Clock className="w-3.5 h-3.5" />
                  <span>{timeFormat24 ? '24-Hour' : '12-Hour'}</span>
                </button>
              </div>
            </div>

            {/* TV Screen Display */}
            <FireTvSimulator
              onHoldProgressChange={setHoldProgress}
              onHoldingOkChange={setIsHoldingOk}
              showOverscanGuides={showOverscanGuides}
              timeFormat24={timeFormat24}
              onTimeFormatToggle={() => setTimeFormat24((prev) => !prev)}
              externalTrigger={remoteTrigger}
              onOpenCleanTv={() => setIsCleanTvModalOpen(true)}
            />

            {/* Quick Helper Tips */}
            <div className="flex flex-wrap items-center justify-between text-xs text-neutral-400 bg-neutral-900/60 border border-neutral-800 rounded-xl px-4 py-2.5">
              <div className="flex items-center space-x-4">
                <span>
                  <strong>D-Pad:</strong> Arrow Keys
                </span>
                <span>
                  <strong>Launch:</strong> Enter
                </span>
                <span className="text-yellow-400">
                  <strong>Toggle Fav:</strong> Hold Enter (or Hold Remote OK)
                </span>
              </div>
              <div className="flex items-center space-x-3 text-[11px]">
                <span className="text-amber-300 font-semibold">
                  <strong>S:</strong> Sort Library
                </span>
                <span>
                  <strong>W:</strong> Wallpaper
                </span>
                <span>
                  <strong>/:</strong> Search Apps
                </span>
                <span>
                  <strong>H:</strong> Home
                </span>
                <span>
                  <strong>Esc:</strong> Back
                </span>
              </div>
            </div>
          </div>

          {/* Remote Control & Quick Hardware Info */}
          <div className="lg:col-span-4 flex flex-col items-center space-y-4">
            <div className="text-center">
              <span className="text-xs font-semibold text-neutral-400 uppercase tracking-wider">
                Fire TV Alexa Remote
              </span>
              <p className="text-[11px] text-neutral-500">
                Click arrows to navigate, click OK to launch, hold OK to favorite
              </p>
            </div>

            <FireRemote
              onNavigate={(dir) => triggerRemoteAction(dir)}
              onSelectClick={() => triggerRemoteAction('click_ok')}
              onSelectLongPress={() => triggerRemoteAction('long_ok')}
              onBackClick={() => triggerRemoteAction('back')}
              onHomeClick={() => triggerRemoteAction('home')}
              isHoldingOk={isHoldingOk}
              holdProgress={holdProgress}
            />
          </div>
        </div>

        {/* Feature Verification Matrix */}
        <div className="bg-[#0F1117] border border-neutral-800/80 rounded-2xl p-6">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-base font-bold text-white flex items-center space-x-2">
                <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                <span>Fire TV Specification & Implementation Matrix</span>
              </h2>
              <p className="text-xs text-neutral-400 mt-0.5">
                All 10 required features implemented across Gradle Kotlin DSL, AndroidManifest, and Jetpack Compose for TV
              </p>
            </div>
            <button
              onClick={() => openFileInModal('app/src/main/AndroidManifest.xml')}
              className="text-xs text-orange-400 hover:text-orange-300 font-medium flex items-center space-x-1"
            >
              <span>View Manifest</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 text-xs">
            <div className="bg-neutral-900/60 border border-neutral-800 p-3.5 rounded-xl space-y-1.5">
              <div className="font-semibold text-white flex items-center space-x-2">
                <span className="w-2 h-2 rounded-full bg-orange-500"></span>
                <span>1. Dual Scanner & Explicit Launch</span>
              </div>
              <p className="text-neutral-400 text-[11px] leading-relaxed">
                Queries both <code className="text-neutral-300 font-mono">LEANBACK_LAUNCHER</code> and <code className="text-neutral-300 font-mono">LAUNCHER</code> categories with <code className="text-neutral-300 font-mono">&lt;queries&gt;</code> package visibility, deduplicates, excludes self, and launches with <code className="text-neutral-300 font-mono">ComponentName</code>.
              </p>
              <button
                onClick={() => openFileInModal('app/src/main/java/com/example/firelauncher/data/AppRepository.kt')}
                className="text-[10px] text-amber-400 hover:underline block pt-1"
              >
                Inspect AppRepository.kt →
              </button>
            </div>

            <div className="bg-neutral-900/60 border border-neutral-800 p-3.5 rounded-xl space-y-1.5">
              <div className="font-semibold text-white flex items-center space-x-2">
                <span className="w-2 h-2 rounded-full bg-orange-500"></span>
                <span>2. Dynamic App Rows</span>
              </div>
              <p className="text-neutral-400 text-[11px] leading-relaxed">
                Horizontal TvLazyRows: <strong>Favorites</strong> (rendered conditionally only when non-empty), <strong>All Apps</strong> (sorted A-Z), and <strong>System shortcuts</strong> (Wi-Fi, Display, Sound, Bluetooth, Settings).
              </p>
              <button
                onClick={() => openFileInModal('app/src/main/java/com/example/firelauncher/ui/components/AppRow.kt')}
                className="text-[10px] text-amber-400 hover:underline block pt-1"
              >
                Inspect AppRow.kt →
              </button>
            </div>

            <div className="bg-neutral-900/60 border border-neutral-800 p-3.5 rounded-xl space-y-1.5">
              <div className="font-semibold text-white flex items-center space-x-2">
                <span className="w-2 h-2 rounded-full bg-orange-500"></span>
                <span>3. 16:9 TV Surface Cards</span>
              </div>
              <p className="text-neutral-400 text-[11px] leading-relaxed">
                Cards built on <code className="text-neutral-300 font-mono">tv-material Surface</code>. On D-pad focus: scales to <strong>1.12x</strong>, renders a 2dp white border, and projects a glow tinted with the app accent colour.
              </p>
              <button
                onClick={() => openFileInModal('app/src/main/java/com/example/firelauncher/ui/components/AppCard.kt')}
                className="text-[10px] text-amber-400 hover:underline block pt-1"
              >
                Inspect AppCard.kt →
              </button>
            </div>

            <div className="bg-neutral-900/60 border border-neutral-800 p-3.5 rounded-xl space-y-1.5">
              <div className="font-semibold text-white flex items-center space-x-2">
                <span className="w-2 h-2 rounded-full bg-orange-500"></span>
                <span>4. Palette Accent & Clamped HSV</span>
              </div>
              <p className="text-neutral-400 text-[11px] leading-relaxed">
                AndroidX Palette extracts vibrant color from banner/icon. HSV saturation is clamped between 0.55–0.95 and value between 0.50–0.88 so backgrounds are never washed-out or black.
              </p>
              <button
                onClick={() => openFileInModal('app/src/main/java/com/example/firelauncher/data/PaletteHelper.kt')}
                className="text-[10px] text-amber-400 hover:underline block pt-1"
              >
                Inspect PaletteHelper.kt →
              </button>
            </div>

            <div className="bg-neutral-900/60 border border-neutral-800 p-3.5 rounded-xl space-y-1.5">
              <div className="font-semibold text-white flex items-center space-x-2">
                <span className="w-2 h-2 rounded-full bg-orange-500"></span>
                <span>5. Hero Area & Dynamic Hints</span>
              </div>
              <p className="text-neutral-400 text-[11px] leading-relaxed">
                Animates focused app name, category chip, and dynamic action text: <em>"Hold OK to add to favorites"</em> or <em>"Hold OK to remove from favorites"</em>.
              </p>
              <button
                onClick={() => openFileInModal('app/src/main/java/com/example/firelauncher/ui/components/HeroSection.kt')}
                className="text-[10px] text-amber-400 hover:underline block pt-1"
              >
                Inspect HeroSection.kt →
              </button>
            </div>

            <div className="bg-neutral-900/60 border border-neutral-800 p-3.5 rounded-xl space-y-1.5">
              <div className="font-semibold text-white flex items-center space-x-2">
                <span className="w-2 h-2 rounded-full bg-orange-500"></span>
                <span>6. Live Weather, City Search & r/TVWallpapersSFW</span>
              </div>
              <p className="text-neutral-400 text-[11px] leading-relaxed">
                <strong>Live Open-Meteo Weather</strong> with real-time global city search (search and add any city worldwide, °F/°C toggle, feels-like, humidity, wind). Fullscreen <strong>r/TVWallpapersSFW backgrounds</strong> that auto-change every 30 minutes with an ambient dimming vignette overlay.
              </p>
              <button
                onClick={() => openFileInModal('app/src/main/java/com/example/firelauncher/ui/components/TopBar.kt')}
                className="text-[10px] text-amber-400 hover:underline block pt-1"
              >
                Inspect TopBar.kt →
              </button>
            </div>

            <div className="bg-neutral-900/60 border border-neutral-800 p-3.5 rounded-xl space-y-1.5">
              <div className="font-semibold text-white flex items-center space-x-2">
                <span className="w-2 h-2 rounded-full bg-orange-500"></span>
                <span>7. DataStore Favorites & Yellow Dot</span>
              </div>
              <p className="text-neutral-400 text-[11px] leading-relaxed">
                Persisted in Preferences DataStore. Long-press OK triggers toggle. Cards belonging to favorites feature a bright yellow dot indicator.
              </p>
              <button
                onClick={() => openFileInModal('app/src/main/java/com/example/firelauncher/data/FavoritesStore.kt')}
                className="text-[10px] text-amber-400 hover:underline block pt-1"
              >
                Inspect FavoritesStore.kt →
              </button>
            </div>

            <div className="bg-neutral-900/60 border border-neutral-800 p-3.5 rounded-xl space-y-1.5">
              <div className="font-semibold text-white flex items-center space-x-2">
                <span className="w-2 h-2 rounded-full bg-orange-500"></span>
                <span>8. Home Key Rescroll & Back Ignored</span>
              </div>
              <p className="text-neutral-400 text-[11px] leading-relaxed">
                Home key presses deliver to <code className="text-neutral-300 font-mono">onNewIntent</code>, incrementing <code className="text-neutral-300 font-mono">homeTick</code> to scroll top and refocus first card. Hardware Back is intercepted and ignored.
              </p>
              <button
                onClick={() => openFileInModal('app/src/main/java/com/example/firelauncher/MainActivity.kt')}
                className="text-[10px] text-amber-400 hover:underline block pt-1"
              >
                Inspect MainActivity.kt →
              </button>
            </div>

            <div className="bg-neutral-900/60 border border-neutral-800 p-3.5 rounded-xl space-y-1.5">
              <div className="font-semibold text-white flex items-center space-x-2">
                <span className="w-2 h-2 rounded-full bg-orange-500"></span>
                <span>9 & 10. 48dp Overscan & Launch Focus</span>
              </div>
              <p className="text-neutral-400 text-[11px] leading-relaxed">
                48dp horizontal padding ensures 100% visibility on all HDMI overscan TVs. Focus requester auto-focuses first card on launch. OnResume triggers app list refresh.
              </p>
              <button
                onClick={() => openFileInModal('app/src/main/java/com/example/firelauncher/ui/LauncherRoot.kt')}
                className="text-[10px] text-amber-400 hover:underline block pt-1"
              >
                Inspect LauncherRoot.kt →
              </button>
            </div>
          </div>
        </div>

        {/* Quick File Explorer Bar */}
        <div className="bg-neutral-900/40 border border-neutral-800 rounded-2xl p-6">
          <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
            <div>
              <h3 className="text-sm font-bold text-white flex items-center space-x-2">
                <FileCode className="w-4 h-4 text-orange-400" />
                <span>All Generated Project Files (Ready in Workspace)</span>
              </h3>
              <p className="text-xs text-neutral-400">
                Click any file to view complete, production-ready source code with syntax highlighting
              </p>
            </div>
            <button
              onClick={handleDownloadZip}
              className="text-xs text-orange-400 hover:text-orange-300 font-semibold flex items-center space-x-1.5 bg-orange-500/10 hover:bg-orange-500/20 px-3 py-1.5 rounded-lg border border-orange-500/30 transition"
            >
              <Download className="w-3.5 h-3.5" />
              <span>Download Complete Zip</span>
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-2.5">
            {ANDROID_PROJECT_FILES.map((file) => (
              <button
                key={file.path}
                onClick={() => openFileInModal(file.path)}
                className="text-left px-3.5 py-2.5 rounded-xl bg-neutral-900 hover:bg-neutral-800 border border-neutral-800 hover:border-neutral-700 transition flex items-center space-x-3 group"
              >
                <div className="w-7 h-7 rounded-lg bg-neutral-800 group-hover:bg-orange-500/20 text-neutral-400 group-hover:text-orange-400 flex items-center justify-center shrink-0 transition">
                  <FileCode className="w-3.5 h-3.5" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="font-mono text-xs font-semibold text-neutral-200 group-hover:text-white truncate">
                    {file.name}
                  </div>
                  <div className="text-[10px] text-neutral-500 truncate">{file.path}</div>
                </div>
              </button>
            ))}
          </div>
        </div>
      </main>

      {/* Modals */}
      <FileViewerModal
        isOpen={isFileModalOpen}
        onClose={() => setIsFileModalOpen(false)}
        initialFilePath={selectedInitialFile}
      />

      <AdbGuideModal
        isOpen={isAdbModalOpen}
        onClose={() => setIsAdbModalOpen(false)}
      />

      <CleanTvModal
        isOpen={isCleanTvModalOpen}
        onClose={() => setIsCleanTvModalOpen(false)}
      />
    </div>
  );
}
