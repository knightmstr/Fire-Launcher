import React, { useState, useEffect, useRef } from 'react';
import {
  Wifi,
  Tv,
  Volume2,
  Bluetooth,
  Settings,
  Film,
  PlayCircle,
  Clapperboard,
  MonitorPlay,
  Gamepad2,
  Music,
  Video,
  Sparkles,
  Gamepad,
  Check,
  Star,
  ExternalLink,
  Flame,
  Clock,
  Eye,
  SlidersHorizontal,
  Sun,
  CloudSun,
  Cloud,
  CloudRain,
  CloudLightning,
  MapPin,
  ChevronDown as ChevronDownIcon,
  ShieldCheck,
  Sparkles as SparklesIcon,
  Search,
  SearchX,
  X as CloseIcon
} from 'lucide-react';
import { SimulatedApp } from '../types';
import { SIMULATED_INSTALLED_APPS, SIMULATED_SYSTEM_SHORTCUTS } from '../data/mockInstalledApps';
import { WEATHER_CITIES, WeatherCityInfo } from '../data/mockWeather';
import {
  playTvNavSound,
  playTvSelectSound,
  playTvFavoriteSound,
  isSoundEnabled,
  toggleSound
} from '../utils/soundEffects';

interface FireTvSimulatorProps {
  onHoldProgressChange?: (progress: number) => void;
  onHoldingOkChange?: (isHolding: boolean) => void;
  showOverscanGuides?: boolean;
  timeFormat24?: boolean;
  onTimeFormatToggle?: () => void;
  externalTrigger?: { action: string; timestamp: number } | null;
  onOpenCleanTv?: () => void;
}

export const FireTvSimulator: React.FC<FireTvSimulatorProps> = ({
  onHoldProgressChange,
  onHoldingOkChange,
  showOverscanGuides = false,
  timeFormat24 = false,
  onTimeFormatToggle,
  externalTrigger,
  onOpenCleanTv
}) => {
  // Favorites set: default to a few favorites (e.g. Netflix, Prime Video)
  const [favorites, setFavorites] = useState<Set<string>>(
    () => new Set(['com.amazon.avod', 'com.netflix.ninja'])
  );

  // Active focus coordinates: row (0: favorites, 1: all apps, 2: system) and column index
  const [focusedRow, setFocusedRow] = useState<number>(0);
  const [focusedCol, setFocusedCol] = useState<number>(0);

  // Live clock
  const [currentTime, setCurrentTime] = useState<string>('');
  const [currentDate, setCurrentDate] = useState<string>('');

  // Toast notifications for TV actions (launching app, back ignored, etc.)
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [launchedApp, setLaunchedApp] = useState<SimulatedApp | null>(null);

  // Weather state (selected city & temperature unit)
  const [selectedWeather, setSelectedWeather] = useState<WeatherCityInfo>(WEATHER_CITIES[0]);
  const [isCelsius, setIsCelsius] = useState<boolean>(false);
  const [isWeatherModalOpen, setIsWeatherModalOpen] = useState<boolean>(false);

  // Global Search state
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [isSearchActive, setIsSearchActive] = useState<boolean>(false);

  // Long press OK simulation state
  const [isPressingOk, setIsPressingOk] = useState(false);
  const [pressProgress, setPressProgress] = useState(0);
  const pressTimerRef = useRef<number | null>(null);
  const pressStartTimeRef = useRef<number>(0);

  // Scroll container ref
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  // Prepare all apps sorted A-Z
  const allApps = React.useMemo(() => {
    return [...SIMULATED_INSTALLED_APPS].sort((a, b) => a.label.localeCompare(b.label));
  }, []);

  // Filter favorite apps
  const favoriteApps = React.useMemo(() => {
    return allApps.filter((app) => favorites.has(app.packageName));
  }, [allApps, favorites]);

  // Filter apps by search query
  const filteredApps = React.useMemo(() => {
    if (!searchQuery.trim()) return allApps;
    const q = searchQuery.toLowerCase().trim();
    return allApps.filter(
      (app) => app.label.toLowerCase().includes(q) || app.packageName.toLowerCase().includes(q)
    );
  }, [allApps, searchQuery]);

  const systemShortcuts = SIMULATED_SYSTEM_SHORTCUTS;

  // Compute row definitions dynamically (when search is active or non-empty, show Search Results row!)
  const rows = React.useMemo(() => {
    if (isSearchActive || searchQuery.trim().length > 0) {
      if (filteredApps.length === 0) {
        return [];
      }
      return [
        {
          id: 'search_results',
          title: searchQuery.trim()
            ? `Search Results (${filteredApps.length})`
            : `All Installed Apps (${filteredApps.length})`,
          apps: filteredApps
        }
      ];
    }

    const r: { id: string; title: string; apps: SimulatedApp[] }[] = [];
    if (favoriteApps.length > 0) {
      r.push({ id: 'favorites', title: 'Favorites', apps: favoriteApps });
    }
    r.push({ id: 'all_apps', title: 'All Apps', apps: allApps });
    r.push({ id: 'system', title: 'System', apps: systemShortcuts });
    return r;
  }, [isSearchActive, searchQuery, filteredApps, favoriteApps, allApps, systemShortcuts]);

  // Ensure focus bounds remain valid
  useEffect(() => {
    if (focusedRow >= rows.length) {
      setFocusedRow(Math.max(0, rows.length - 1));
    } else {
      const currentRowApps = rows[focusedRow]?.apps || [];
      if (focusedCol >= currentRowApps.length) {
        setFocusedCol(Math.max(0, currentRowApps.length - 1));
      }
    }
  }, [rows, focusedRow, focusedCol]);

  // Current focused app
  const currentApp: SimulatedApp | undefined = rows[focusedRow]?.apps[focusedCol];

  // Clock tick
  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      const timeStr = now.toLocaleTimeString([], {
        hour: 'numeric',
        minute: '2-digit',
        hour12: !timeFormat24
      });
      const dateStr = now.toLocaleDateString([], {
        weekday: 'short',
        month: 'short',
        day: 'numeric'
      });
      setCurrentTime(timeStr);
      setCurrentDate(dateStr);
    };

    updateTime();
    const interval = setInterval(updateTime, 1000);
    return () => clearInterval(interval);
  }, [timeFormat24]);

  // Toast auto-clear
  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage((prev) => (prev === msg ? null : prev));
    }, 2800);
  };

  // Toggle favorite for an app
  const toggleFavorite = (app: SimulatedApp) => {
    if (app.isSystemShortcut) return;
    playTvFavoriteSound();
    setFavorites((prev) => {
      const next = new Set(prev);
      if (next.has(app.packageName)) {
        next.delete(app.packageName);
        showToast(`Removed "${app.label}" from Favorites`);
      } else {
        next.add(app.packageName);
        showToast(`Added "${app.label}" to Favorites ⭐`);
      }
      return next;
    });
  };

  // Launch app
  const launchApp = (app: SimulatedApp) => {
    playTvSelectSound();
    if (app.id === 'sys:cleantv' || app.packageName === 'system.cleantv') {
      onOpenCleanTv?.();
      showToast('Opening CleanYourTV Debloat & Optimizer...');
      return;
    }

    setLaunchedApp(app);
    if (app.isSystemShortcut) {
      showToast(`Opening System Setting: ${app.label}`);
    } else {
      showToast(`Launching ${app.label} (${app.packageName})...`);
    }
  };

  // Navigation handlers
  const handleNavigate = (dir: 'up' | 'down' | 'left' | 'right') => {
    if (launchedApp) {
      setLaunchedApp(null);
      return;
    }
    playTvNavSound();

    if (dir === 'up') {
      if (focusedRow > 0) {
        const targetRow = focusedRow - 1;
        setFocusedRow(targetRow);
        setFocusedCol((prev) => Math.min(prev, rows[targetRow].apps.length - 1));
      }
    } else if (dir === 'down') {
      if (focusedRow < rows.length - 1) {
        const targetRow = focusedRow + 1;
        setFocusedRow(targetRow);
        setFocusedCol((prev) => Math.min(prev, rows[targetRow].apps.length - 1));
      }
    } else if (dir === 'left') {
      if (focusedCol > 0) {
        setFocusedCol((prev) => prev - 1);
      }
    } else if (dir === 'right') {
      const currentRowApps = rows[focusedRow]?.apps || [];
      if (focusedCol < currentRowApps.length - 1) {
        setFocusedCol((prev) => prev + 1);
      }
    }
  };

  // Scroll to top and refocus first card (Home pressed)
  const handleHomePressed = () => {
    setLaunchedApp(null);
    setFocusedRow(0);
    setFocusedCol(0);
    if (scrollContainerRef.current) {
      scrollContainerRef.current.scrollTo({ top: 0, behavior: 'smooth' });
    }
    showToast('Home pressed: Scrolled to top & refocused first card');
  };

  // Back pressed: Ignored per Fire TV spec (dismisses search/modals if open)
  const handleBackPressed = () => {
    if (isWeatherModalOpen) {
      setIsWeatherModalOpen(false);
      return;
    }
    if (isSearchActive || searchQuery.trim().length > 0) {
      setIsSearchActive(false);
      setSearchQuery('');
      showToast('Search closed');
      return;
    }
    if (launchedApp) {
      setLaunchedApp(null);
      showToast('Returned to FireLauncher');
      return;
    }
    showToast('Back button ignored: FireLauncher is the active home screen');
  };

  // Listen to external remote triggers
  useEffect(() => {
    if (!externalTrigger) return;
    const { action } = externalTrigger;
    if (action === 'up' || action === 'down' || action === 'left' || action === 'right') {
      handleNavigate(action);
    } else if (action === 'click_ok') {
      if (currentApp) launchApp(currentApp);
    } else if (action === 'long_ok') {
      if (currentApp) toggleFavorite(currentApp);
    } else if (action === 'back') {
      handleBackPressed();
    } else if (action === 'home') {
      handleHomePressed();
    }
  }, [externalTrigger]);

  // Keyboard navigation listener (Arrow keys, Enter, Back, Home)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Avoid intercepting if user is typing in a modal input
      if (['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement)?.tagName)) return;

      if (e.key === 'ArrowUp') {
        e.preventDefault();
        handleNavigate('up');
      } else if (e.key === 'ArrowDown') {
        e.preventDefault();
        handleNavigate('down');
      } else if (e.key === 'ArrowLeft') {
        e.preventDefault();
        handleNavigate('left');
      } else if (e.key === 'ArrowRight') {
        e.preventDefault();
        handleNavigate('right');
      } else if (e.key === 'Enter') {
        e.preventDefault();
        if (!isPressingOk) {
          setIsPressingOk(true);
          onHoldingOkChange?.(true);
          pressStartTimeRef.current = Date.now();
          pressTimerRef.current = window.setInterval(() => {
            const elapsed = Date.now() - pressStartTimeRef.current;
            const progress = Math.min(1, elapsed / 700);
            setPressProgress(progress);
            onHoldProgressChange?.(progress);

            if (elapsed >= 700) {
              clearInterval(pressTimerRef.current!);
              pressTimerRef.current = null;
              setIsPressingOk(false);
              setPressProgress(0);
              onHoldingOkChange?.(false);
              onHoldProgressChange?.(0);
              if (currentApp) toggleFavorite(currentApp);
            }
          }, 40);
        }
      } else if (e.key === 'Escape') {
        e.preventDefault();
        handleBackPressed();
      } else if (e.key === 'h' || e.key === 'H') {
        e.preventDefault();
        handleHomePressed();
      } else if (e.key === '/' && !isSearchActive) {
        e.preventDefault();
        setIsSearchActive(true);
        setTimeout(() => searchInputRef.current?.focus(), 50);
      }
    };

    const handleKeyUp = (e: KeyboardEvent) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        if (pressTimerRef.current) {
          clearInterval(pressTimerRef.current);
          pressTimerRef.current = null;
          const elapsed = Date.now() - pressStartTimeRef.current;
          setIsPressingOk(false);
          setPressProgress(0);
          onHoldingOkChange?.(false);
          onHoldProgressChange?.(0);
          // If released before 700ms, it is a click/launch!
          if (elapsed < 700 && currentApp) {
            launchApp(currentApp);
          }
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
      if (pressTimerRef.current) clearInterval(pressTimerRef.current);
    };
  }, [focusedRow, focusedCol, currentApp, isPressingOk, launchedApp, isSearchActive, searchQuery, isWeatherModalOpen]);

  // Render Icon helper
  const renderAppIcon = (iconName: string, className = 'w-6 h-6') => {
    switch (iconName) {
      case 'Film':
        return <Film className={className} />;
      case 'PlayCircle':
        return <PlayCircle className={className} />;
      case 'Clapperboard':
        return <Clapperboard className={className} />;
      case 'MonitorPlay':
        return <MonitorPlay className={className} />;
      case 'Gamepad2':
        return <Gamepad2 className={className} />;
      case 'Music':
        return <Music className={className} />;
      case 'Video':
        return <Video className={className} />;
      case 'Sparkles':
        return <Sparkles className={className} />;
      case 'Gamepad':
        return <Gamepad className={className} />;
      case 'Wifi':
        return <Wifi className={className} />;
      case 'Volume2':
        return <Volume2 className={className} />;
      case 'Bluetooth':
        return <Bluetooth className={className} />;
      case 'Settings':
        return <Settings className={className} />;
      case 'ShieldCheck':
        return <ShieldCheck className={className} />;
      default:
        return <Tv className={className} />;
    }
  };

  const renderWeatherIcon = (iconType: string, className = 'w-5 h-5') => {
    switch (iconType) {
      case 'sun':
        return <Sun className={`${className} text-amber-400`} />;
      case 'cloud-sun':
        return <CloudSun className={`${className} text-sky-300`} />;
      case 'cloud':
        return <Cloud className={`${className} text-neutral-300`} />;
      case 'rain':
        return <CloudRain className={`${className} text-cyan-400`} />;
      case 'thunder':
        return <CloudLightning className={`${className} text-yellow-400`} />;
      default:
        return <Sun className={`${className} text-amber-400`} />;
    }
  };

  const accentColor = currentApp ? currentApp.accentColor : '#FF9900';
  const isCurrentFavorite = currentApp ? favorites.has(currentApp.packageName) : false;

  return (
    <div className="relative w-full aspect-[16/9] max-w-5xl mx-auto rounded-3xl overflow-hidden shadow-2xl border-4 border-neutral-800 bg-[#0F1117] select-none text-white">
      {/* Dynamic Animated Ambient Background Gradient */}
      <div
        className="absolute inset-0 transition-colors duration-500 ease-out pointer-events-none"
        style={{
          background: `radial-gradient(ellipse at 50% 10%, ${accentColor}44 0%, ${accentColor}18 45%, #0F1117 85%)`
        }}
      />
      <div className="absolute inset-0 bg-gradient-to-b from-transparent via-[#0F1117]/60 to-[#0F1117] pointer-events-none" />

      {/* 48dp Overscan Safe Boundaries Overlay (Optional visual guide) */}
      {showOverscanGuides && (
        <div className="absolute inset-0 pointer-events-none border-2 border-dashed border-amber-400/40 m-[48px] rounded-2xl flex flex-col justify-between p-2 text-[10px] text-amber-300 font-mono">
          <div className="flex justify-between">
            <span>48dp Overscan Safe Zone</span>
            <span>Fire OS TV Safe Area</span>
          </div>
          <div className="flex justify-between">
            <span>HDMI 1080p/4K TV Boundary</span>
            <span>Scale 1.12 Safe Margin</span>
          </div>
        </div>
      )}

      {/* Main Content Area with 48dp Horizontal Padding */}
      <div
        ref={scrollContainerRef}
        className="relative z-10 w-full h-full overflow-y-auto px-[48px] pt-6 pb-8 flex flex-col justify-between no-scrollbar"
      >
        {/* Top Bar: Brand Logo, Weather Widget & Live Clock */}
        <div className="flex items-center justify-between h-14 shrink-0">
          {/* Brand Logo */}
          <div className="flex items-center space-x-3">
            <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-amber-500 to-orange-500 flex items-center justify-center shadow-lg shadow-orange-500/30">
              <Flame className="w-4 h-4 text-white fill-white" />
            </div>
            <div className="flex items-baseline space-x-2">
              <span className="text-xl font-bold tracking-tight text-white font-sans">
                FireLauncher
              </span>
              <span className="text-[10px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded bg-orange-500/20 text-orange-400 border border-orange-500/30">
                Fire OS
              </span>
            </div>
          </div>

          {/* Current Weather Widget for Selected City */}
          <button
            onClick={() => setIsWeatherModalOpen(true)}
            className="flex items-center space-x-3 px-3.5 py-1.5 rounded-2xl bg-neutral-900/80 hover:bg-neutral-800/90 border border-neutral-700/60 hover:border-amber-400/60 transition group cursor-pointer shadow-lg backdrop-blur-md active:scale-95"
            title="Click or press OK to change city and weather settings"
          >
            <div className="p-1 rounded-lg bg-black/40">
              {renderWeatherIcon(selectedWeather.icon, 'w-5 h-5')}
            </div>
            <div className="text-left">
              <div className="flex items-center space-x-2">
                <span className="text-sm font-bold text-white tracking-tight">
                  {isCelsius ? `${selectedWeather.tempC}°C` : `${selectedWeather.tempF}°F`}
                </span>
                <span className="text-[11px] text-neutral-300 font-medium">
                  {selectedWeather.condition}
                </span>
              </div>
              <div className="flex items-center space-x-1.5 text-[10px] text-neutral-400">
                <MapPin className="w-2.5 h-2.5 text-amber-400" />
                <span className="text-amber-300 font-medium">{selectedWeather.city}</span>
                <span>•</span>
                <span>
                  H:{isCelsius ? selectedWeather.highC : selectedWeather.highF}° L:{isCelsius ? selectedWeather.lowC : selectedWeather.lowF}°
                </span>
              </div>
            </div>
            <ChevronDownIcon className="w-3.5 h-3.5 text-neutral-500 group-hover:text-amber-400 transition" />
          </button>

          {/* Top Bar Center & Right Controls */}
          <div className="flex items-center space-x-3">
            {/* Global Search Bar & Icon Button */}
            <div className="flex items-center">
              {isSearchActive ? (
                <div className="flex items-center space-x-2 bg-neutral-900/90 border border-amber-400/80 rounded-2xl px-3 py-1.5 shadow-lg backdrop-blur-md">
                  <Search className="w-4 h-4 text-amber-400 shrink-0" />
                  <input
                    ref={searchInputRef}
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Search apps..."
                    className="bg-transparent text-white text-xs placeholder-neutral-400 focus:outline-none w-36 sm:w-48"
                    autoFocus
                    onKeyDown={(e) => {
                      if (e.key === 'Escape') {
                        setIsSearchActive(false);
                        setSearchQuery('');
                      } else if (e.key === 'ArrowDown') {
                        e.currentTarget.blur();
                        setFocusedRow(0);
                        setFocusedCol(0);
                      }
                    }}
                  />
                  {searchQuery && (
                    <button
                      onClick={() => {
                        setSearchQuery('');
                        searchInputRef.current?.focus();
                      }}
                      className="text-neutral-400 hover:text-white transition p-0.5 cursor-pointer"
                      title="Clear search"
                    >
                      <CloseIcon className="w-3.5 h-3.5" />
                    </button>
                  )}
                  <button
                    onClick={() => {
                      setIsSearchActive(false);
                      setSearchQuery('');
                    }}
                    className="text-[11px] text-neutral-400 hover:text-neutral-200 px-1.5 py-0.5 rounded bg-neutral-800 cursor-pointer"
                  >
                    Close
                  </button>
                </div>
              ) : (
                <button
                  onClick={() => {
                    setIsSearchActive(true);
                    setTimeout(() => searchInputRef.current?.focus(), 50);
                  }}
                  className="flex items-center space-x-2 px-3 py-1.5 rounded-2xl bg-neutral-900/80 hover:bg-neutral-800/90 border border-neutral-700/60 hover:border-amber-400/60 transition group cursor-pointer shadow-lg backdrop-blur-md active:scale-95"
                  title="Search installed apps (/)"
                >
                  <Search className="w-4 h-4 text-neutral-300 group-hover:text-amber-400 transition" />
                  <span className="text-xs font-medium text-neutral-300 group-hover:text-white hidden sm:inline">
                    Search
                  </span>
                  <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-neutral-800 text-neutral-400 border border-neutral-700">
                    /
                  </span>
                </button>
              )}
            </div>

            {/* Live Clock & Date */}
            <div className="flex items-center space-x-4 pl-2 border-l border-neutral-800">
              <button
                onClick={onTimeFormatToggle}
                title="Click to toggle 12h / 24h format"
                className="text-right hover:opacity-80 transition cursor-pointer"
              >
                <div className="text-base font-semibold tracking-wide text-neutral-100 flex items-center justify-end space-x-1">
                  <span>{currentTime}</span>
                  <span className="text-[9px] font-mono text-neutral-400">
                    {timeFormat24 ? '24H' : ''}
                  </span>
                </div>
                <div className="text-[11px] text-neutral-400 font-medium">{currentDate}</div>
              </button>
            </div>
          </div>
        </div>

        {/* Hero Section: Focused App Details & Hint */}
        <div className="my-4 h-20 shrink-0 flex flex-col justify-center transition-all duration-300">
          {currentApp ? (
            <div>
              <div className="flex items-center space-x-3">
                <h1 className="text-2xl lg:text-3xl font-extrabold tracking-tight text-white drop-shadow-md">
                  {currentApp.label}
                </h1>
                {isCurrentFavorite && (
                  <span className="px-2 py-0.5 rounded-full bg-yellow-400/20 text-yellow-300 border border-yellow-400/40 text-[10px] font-bold flex items-center space-x-1">
                    <Star className="w-2.5 h-2.5 fill-yellow-400" />
                    <span>FAVORITE</span>
                  </span>
                )}
                {currentApp.isSystemShortcut && (
                  <span className="px-2 py-0.5 rounded-full bg-neutral-700/60 text-neutral-300 text-[10px] font-medium">
                    SYSTEM SETTING
                  </span>
                )}
              </div>

              {/* Dynamic Context Hint */}
              <div className="flex items-center space-x-2 mt-1">
                <span className="text-xs text-neutral-400">
                  {currentApp.isSystemShortcut
                    ? 'Press OK to open Fire TV system settings'
                    : isCurrentFavorite
                    ? 'Hold OK to remove from favorites'
                    : 'Hold OK to add to favorites'}
                </span>
                {/* Visual hold indicator on screen */}
                {isPressingOk && (
                  <div className="flex items-center space-x-1.5 ml-2 bg-neutral-800/80 px-2 py-0.5 rounded-full border border-yellow-400/40 text-[11px] text-yellow-300 animate-pulse">
                    <span className="w-1.5 h-1.5 rounded-full bg-yellow-400"></span>
                    <span>Holding OK ({Math.round(pressProgress * 100)}%)</span>
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="text-neutral-500 text-lg">Select an app using the remote</div>
          )}
        </div>

        {/* Scrollable Rows Container */}
        <div className="space-y-6 overflow-x-hidden pb-4">
          {/* Empty Search State */}
          {(isSearchActive || searchQuery.trim().length > 0) && filteredApps.length === 0 && (
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <div className="w-14 h-14 rounded-2xl bg-neutral-800/80 border border-neutral-700/80 flex items-center justify-center mb-3 text-neutral-400 shadow-xl">
                <SearchX className="w-7 h-7 text-amber-400/80" />
              </div>
              <h3 className="text-base font-bold text-neutral-200">No apps found</h3>
              <p className="text-xs text-neutral-400 mt-1 max-w-sm">
                No installed apps match &ldquo;<span className="text-amber-300 font-mono">{searchQuery}</span>&rdquo;. Try searching with a different keyword or app name.
              </p>
              <button
                onClick={() => {
                  setSearchQuery('');
                  searchInputRef.current?.focus();
                }}
                className="mt-4 px-3.5 py-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-xs text-amber-400 font-semibold border border-neutral-700 transition active:scale-95 cursor-pointer shadow-md"
              >
                Clear Search
              </button>
            </div>
          )}

          {rows.map((row, rIdx) => (
            <div key={row.id} className="space-y-2.5">
              <div className="flex items-center space-x-2">
                <h2 className="text-sm font-semibold tracking-wide text-neutral-300">
                  {row.title}
                </h2>
                <span className="text-[10px] text-neutral-500 font-mono">
                  ({row.apps.length})
                </span>
              </div>

              {/* 16:9 TV Cards Horizontal Row */}
              <div className="flex items-center space-x-4 overflow-x-visible py-3 px-2">
                {row.apps.map((app, cIdx) => {
                  const isFocused = focusedRow === rIdx && focusedCol === cIdx;
                  const isAppFav = favorites.has(app.packageName);
                  const cardAccent = app.accentColor;

                  return (
                    <div
                      key={app.id}
                      onClick={() => {
                        setFocusedRow(rIdx);
                        setFocusedCol(cIdx);
                        launchApp(app);
                      }}
                      onMouseEnter={() => {
                        setFocusedRow(rIdx);
                        setFocusedCol(cIdx);
                      }}
                      className={`relative shrink-0 w-44 aspect-[16/9] rounded-xl cursor-pointer transition-all duration-200 select-none ${
                        isFocused
                          ? 'scale-[1.12] z-30 ring-2 ring-white shadow-2xl'
                          : 'scale-100 z-10 opacity-85 hover:opacity-100 ring-1 ring-white/10'
                      }`}
                      style={{
                        boxShadow: isFocused
                          ? `0 14px 28px -6px ${cardAccent}99, 0 0 16px 2px ${cardAccent}66`
                          : undefined
                      }}
                    >
                      {/* Card Interior */}
                      <div className="w-full h-full rounded-xl overflow-hidden bg-neutral-900 flex flex-col justify-between p-3 relative">
                        {/* Background Banner / Gradient */}
                        {app.isSystemShortcut ? (
                          <div
                            className="absolute inset-0 bg-gradient-to-br from-neutral-800 to-neutral-950 flex flex-col items-center justify-center p-2"
                            style={{
                              background: `radial-gradient(circle at 50% 30%, ${cardAccent}33, #151821 80%)`
                            }}
                          >
                            <div
                              className="w-10 h-10 rounded-full flex items-center justify-center mb-1"
                              style={{ color: cardAccent }}
                            >
                              {renderAppIcon(app.iconName, 'w-6 h-6')}
                            </div>
                            <span className="text-xs font-semibold text-neutral-200 truncate max-w-[90%]">
                              {app.label}
                            </span>
                          </div>
                        ) : (
                          <>
                            {/* 16:9 Banner graphic */}
                            <div
                              className={`absolute inset-0 bg-gradient-to-br ${app.bannerGradient} opacity-90`}
                            />
                            <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent" />

                            {/* Top Left Icon or Category */}
                            <div className="relative z-10 flex items-center justify-between w-full">
                              <div className="p-1 rounded bg-black/40 backdrop-blur-sm text-white">
                                {renderAppIcon(app.iconName, 'w-4 h-4')}
                              </div>
                            </div>

                            {/* App Label */}
                            <div className="relative z-10">
                              <span className="text-xs font-bold text-white drop-shadow truncate block">
                                {app.label}
                              </span>
                            </div>
                          </>
                        )}

                        {/* Yellow Favorite Dot Indicator (Top-Right Corner) */}
                        {isAppFav && (
                          <div
                            className="absolute top-2.5 right-2.5 w-2.5 h-2.5 rounded-full bg-yellow-400 shadow-md ring-2 ring-black/60 z-20"
                            title="Favorite App"
                          />
                        )}

                        {/* Cinematic Apple TV Specular Flare Sheen */}
                        {isFocused && (
                          <div className="absolute inset-0 bg-gradient-to-tr from-transparent via-white/15 to-transparent pointer-events-none rounded-xl" />
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Screen Overlay Toast (TV Notification) */}
      {toastMessage && (
        <div className="absolute top-6 left-1/2 -translate-x-1/2 z-50 bg-neutral-900/95 border border-neutral-700/80 text-white px-4 py-2 rounded-xl text-xs font-semibold shadow-2xl flex items-center space-x-2 backdrop-blur-md animate-fade-in">
          <div className="w-2 h-2 rounded-full bg-orange-500 animate-ping"></div>
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Simulated App Fullscreen Modal (When an app is launched) */}
      {launchedApp && (
        <div className="absolute inset-0 z-40 bg-neutral-950 flex flex-col items-center justify-center p-8 text-center animate-fade-in">
          <div
            className="w-20 h-20 rounded-2xl flex items-center justify-center mb-4 shadow-2xl"
            style={{
              background: `linear-gradient(135deg, ${launchedApp.accentColor}, #111)`
            }}
          >
            {renderAppIcon(launchedApp.iconName, 'w-10 h-10 text-white')}
          </div>
          <h2 className="text-2xl font-bold text-white">{launchedApp.label}</h2>
          <p className="text-xs font-mono text-neutral-400 mt-1">{launchedApp.packageName}</p>
          <p className="text-sm text-neutral-300 mt-3 max-w-md">{launchedApp.description}</p>

          <div className="mt-8 flex items-center space-x-3">
            <button
              onClick={() => setLaunchedApp(null)}
              className="px-4 py-2 rounded-xl bg-orange-600 hover:bg-orange-500 text-white text-xs font-semibold shadow-lg transition active:scale-95"
            >
              Return to FireLauncher (Home)
            </button>
          </div>
          <span className="text-[11px] text-neutral-500 mt-3">
            Press Remote Back or Home to exit this app
          </span>
        </div>
      )}

      {/* Weather City Selection TV Modal */}
      {isWeatherModalOpen && (
        <div className="absolute inset-0 z-50 bg-black/85 backdrop-blur-md flex items-center justify-center p-6 animate-fade-in">
          <div className="bg-[#141824] border border-neutral-700/80 rounded-3xl w-full max-w-xl p-6 shadow-2xl flex flex-col max-h-[85%]">
            <div className="flex items-center justify-between pb-4 border-b border-neutral-800">
              <div className="flex items-center space-x-2.5">
                <div className="w-8 h-8 rounded-xl bg-amber-500/20 text-amber-400 flex items-center justify-center font-bold">
                  <Sun className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white">Weather Location & Settings</h3>
                  <p className="text-[11px] text-neutral-400">Select city for the FireLauncher home-screen widget</p>
                </div>
              </div>

              {/* °F / °C Unit Toggle */}
              <button
                onClick={() => setIsCelsius((prev) => !prev)}
                className="px-3 py-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 border border-neutral-700 text-xs font-semibold text-neutral-200 transition active:scale-95 flex items-center space-x-1.5"
              >
                <span>Unit:</span>
                <span className="text-amber-400 font-bold">{isCelsius ? '°C (Celsius)' : '°F (Fahrenheit)'}</span>
              </button>
            </div>

            {/* City Grid List */}
            <div className="flex-1 overflow-y-auto py-4 space-y-2 pr-1">
              <div className="text-[11px] font-semibold text-neutral-400 uppercase tracking-wider px-1">
                Preset Cities
              </div>
              <div className="grid grid-cols-2 gap-2">
                {WEATHER_CITIES.map((item) => {
                  const isSelected = selectedWeather.city === item.city;
                  return (
                    <button
                      key={item.city}
                      onClick={() => {
                        setSelectedWeather(item);
                        setIsWeatherModalOpen(false);
                        showToast(`Weather location set to ${item.city}`);
                      }}
                      className={`text-left p-3 rounded-xl border transition flex items-center justify-between ${
                        isSelected
                          ? 'bg-amber-500/20 border-amber-500 text-white shadow-lg'
                          : 'bg-neutral-900/80 hover:bg-neutral-850 border-neutral-800 text-neutral-300 hover:border-neutral-700'
                      }`}
                    >
                      <div className="flex items-center space-x-3">
                        <div className="p-1.5 rounded-lg bg-black/40">
                          {renderWeatherIcon(item.icon, 'w-4 h-4')}
                        </div>
                        <div>
                          <div className="text-xs font-bold text-white flex items-center space-x-1.5">
                            <span>{item.city}</span>
                            {isSelected && <span className="text-[9px] px-1.5 py-0.2 rounded bg-amber-500/30 text-amber-300 font-bold">CURRENT</span>}
                          </div>
                          <div className="text-[10px] text-neutral-400">{item.country} • {item.condition}</div>
                        </div>
                      </div>
                      <div className="text-xs font-bold text-right text-neutral-200">
                        {isCelsius ? `${item.tempC}°C` : `${item.tempF}°F`}
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Footer */}
            <div className="pt-3 border-t border-neutral-800 flex justify-end">
              <button
                onClick={() => setIsWeatherModalOpen(false)}
                className="px-4 py-2 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-white text-xs font-semibold transition"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
