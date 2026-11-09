import React, { useRef, useState, useEffect } from 'react';
import {
  ChevronUp,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  RotateCcw,
  Home,
  Menu,
  Volume2,
  VolumeX,
  Play,
  Mic,
  Power
} from 'lucide-react';

interface FireRemoteProps {
  onNavigate: (direction: 'up' | 'down' | 'left' | 'right') => void;
  onSelectClick: () => void;
  onSelectLongPress: () => void;
  onBackClick: () => void;
  onHomeClick: () => void;
  onVoiceSearchClick?: () => void;
  isHoldingOk: boolean;
  holdProgress: number;
  isVoiceListening?: boolean;
}

export const FireRemote: React.FC<FireRemoteProps> = ({
  onNavigate,
  onSelectClick,
  onSelectLongPress,
  onBackClick,
  onHomeClick,
  onVoiceSearchClick,
  isHoldingOk,
  holdProgress,
  isVoiceListening = false
}) => {
  const holdTimerRef = useRef<number | null>(null);
  const [localHolding, setLocalHolding] = useState(false);
  const [activeButton, setActiveButton] = useState<string | null>(null);

  const startHold = () => {
    setLocalHolding(true);
    const startTime = Date.now();
    holdTimerRef.current = window.setInterval(() => {
      const elapsed = Date.now() - startTime;
      if (elapsed >= 750) {
        clearInterval(holdTimerRef.current!);
        holdTimerRef.current = null;
        setLocalHolding(false);
        onSelectLongPress();
      }
    }, 50);
  };

  const cancelHold = () => {
    if (holdTimerRef.current) {
      clearInterval(holdTimerRef.current);
      holdTimerRef.current = null;
      // If released before 750ms, it was a click
      onSelectClick();
    }
    setLocalHolding(false);
  };

  useEffect(() => {
    return () => {
      if (holdTimerRef.current) clearInterval(holdTimerRef.current);
    };
  }, []);

  const triggerNav = (dir: 'up' | 'down' | 'left' | 'right') => {
    setActiveButton(dir);
    onNavigate(dir);
    setTimeout(() => setActiveButton(null), 150);
  };

  return (
    <div className="w-56 bg-neutral-900 border border-neutral-800 rounded-[38px] p-4 shadow-2xl flex flex-col items-center select-none text-neutral-400">
      {/* Remote Top Branding / Mic */}
      <div className="w-full flex items-center justify-between px-2 pt-1 pb-3">
        <button
          onClick={() => onHomeClick()}
          className="w-7 h-7 rounded-full bg-neutral-800 hover:bg-neutral-700 active:scale-95 flex items-center justify-center text-neutral-400 transition"
          title="Power / Standby"
        >
          <Power className="w-3.5 h-3.5" />
        </button>
        <div className="flex flex-col items-center">
          <div className="w-2 h-2 rounded-full bg-blue-500 animate-pulse mb-1"></div>
          <span className="text-[10px] font-bold tracking-widest text-neutral-500">FIRE TV</span>
        </div>
        <button
          onClick={onVoiceSearchClick}
          className={`w-7 h-7 rounded-full flex items-center justify-center transition active:scale-95 cursor-pointer ${
            isVoiceListening
              ? 'bg-blue-500 text-white shadow-lg shadow-blue-500/50 ring-2 ring-blue-400 animate-pulse'
              : 'bg-blue-600/30 text-blue-400 hover:bg-blue-600/50 hover:text-white'
          }`}
          title="Alexa Voice Search (Click to speak app name)"
        >
          <Mic className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* D-Pad Circular Controller */}
      <div className="relative w-36 h-36 my-3 flex items-center justify-center">
        {/* Outer Ring Background */}
        <div className="absolute inset-0 rounded-full bg-neutral-800/90 border border-neutral-700/60 shadow-inner"></div>

        {/* Up Arrow */}
        <button
          onClick={() => triggerNav('up')}
          className={`absolute top-1 left-1/2 -translate-x-1/2 w-10 h-8 flex items-center justify-center rounded-t-full transition hover:text-white ${
            activeButton === 'up' ? 'text-amber-400 bg-neutral-700/50' : 'text-neutral-400'
          }`}
          title="D-Pad Up"
        >
          <ChevronUp className="w-5 h-5" />
        </button>

        {/* Down Arrow */}
        <button
          onClick={() => triggerNav('down')}
          className={`absolute bottom-1 left-1/2 -translate-x-1/2 w-10 h-8 flex items-center justify-center rounded-b-full transition hover:text-white ${
            activeButton === 'down' ? 'text-amber-400 bg-neutral-700/50' : 'text-neutral-400'
          }`}
          title="D-Pad Down"
        >
          <ChevronDown className="w-5 h-5" />
        </button>

        {/* Left Arrow */}
        <button
          onClick={() => triggerNav('left')}
          className={`absolute left-1 top-1/2 -translate-y-1/2 w-8 h-10 flex items-center justify-center rounded-l-full transition hover:text-white ${
            activeButton === 'left' ? 'text-amber-400 bg-neutral-700/50' : 'text-neutral-400'
          }`}
          title="D-Pad Left"
        >
          <ChevronLeft className="w-5 h-5" />
        </button>

        {/* Right Arrow */}
        <button
          onClick={() => triggerNav('right')}
          className={`absolute right-1 top-1/2 -translate-y-1/2 w-8 h-10 flex items-center justify-center rounded-r-full transition hover:text-white ${
            activeButton === 'right' ? 'text-amber-400 bg-neutral-700/50' : 'text-neutral-400'
          }`}
          title="D-Pad Right"
        >
          <ChevronRight className="w-5 h-5" />
        </button>

        {/* Center Select / OK Button with Hold-to-Favorite ring */}
        <div className="relative">
          {/* SVG Progress Ring for Hold-to-Favorite */}
          {(localHolding || isHoldingOk) && (
            <svg className="absolute -inset-1.5 w-19 h-19 -rotate-90 pointer-events-none">
              <circle
                cx="38"
                cy="38"
                r="34"
                stroke="rgba(255, 214, 0, 0.2)"
                strokeWidth="3"
                fill="none"
              />
              <circle
                cx="38"
                cy="38"
                r="34"
                stroke="#FFD600"
                strokeWidth="3.5"
                fill="none"
                strokeDasharray="213"
                strokeDashoffset={213 - (213 * (holdProgress || (localHolding ? 0.7 : 0)))}
                className="transition-all duration-75"
              />
            </svg>
          )}

          <button
            onMouseDown={startHold}
            onMouseUp={cancelHold}
            onTouchStart={startHold}
            onTouchEnd={cancelHold}
            className={`w-16 h-16 rounded-full bg-neutral-900 border-2 border-neutral-700 flex flex-col items-center justify-center shadow-lg transition active:scale-95 ${
              localHolding || isHoldingOk
                ? 'border-amber-400 text-amber-300 ring-2 ring-amber-400/30'
                : 'text-neutral-300 hover:text-white hover:border-neutral-500'
            }`}
            title="Press OK to Launch / Hold OK to Favorite"
          >
            <span className="text-xs font-bold tracking-wider">OK</span>
            <span className="text-[8px] text-neutral-500 mt-0.5">HOLD FAV</span>
          </button>
        </div>
      </div>

      {/* Primary Action Buttons: Back, Home, Menu */}
      <div className="w-full grid grid-cols-3 gap-2 px-1 my-2">
        {/* Back Button */}
        <button
          onClick={onBackClick}
          className="flex flex-col items-center justify-center py-2.5 rounded-xl bg-neutral-800 hover:bg-neutral-750 hover:text-white active:scale-95 transition"
          title="Back (Ignored on FireLauncher Home)"
        >
          <RotateCcw className="w-4 h-4 mb-1" />
          <span className="text-[9px] font-medium">BACK</span>
        </button>

        {/* Home Button */}
        <button
          onClick={onHomeClick}
          className="flex flex-col items-center justify-center py-2.5 rounded-xl bg-neutral-800 hover:bg-neutral-750 hover:text-white active:scale-95 transition"
          title="Home (Scrolls to top & refocused)"
        >
          <Home className="w-4 h-4 mb-1 text-amber-400" />
          <span className="text-[9px] font-medium text-amber-300">HOME</span>
        </button>

        {/* Menu Button */}
        <button
          onClick={onSelectLongPress}
          className="flex flex-col items-center justify-center py-2.5 rounded-xl bg-neutral-800 hover:bg-neutral-750 hover:text-white active:scale-95 transition"
          title="Menu / Options"
        >
          <Menu className="w-4 h-4 mb-1" />
          <span className="text-[9px] font-medium">MENU</span>
        </button>
      </div>

      {/* Secondary Controls: Play/Pause, Volume */}
      <div className="w-full grid grid-cols-3 gap-2 px-1 mb-3">
        <button
          className="flex items-center justify-center py-2 rounded-lg bg-neutral-800/60 hover:bg-neutral-800 active:scale-95 text-neutral-400"
          title="Mute"
        >
          <VolumeX className="w-3.5 h-3.5" />
        </button>
        <button
          className="flex items-center justify-center py-2 rounded-lg bg-neutral-800/60 hover:bg-neutral-800 active:scale-95 text-neutral-400"
          title="Play/Pause"
        >
          <Play className="w-3.5 h-3.5" />
        </button>
        <button
          className="flex items-center justify-center py-2 rounded-lg bg-neutral-800/60 hover:bg-neutral-800 active:scale-95 text-neutral-400"
          title="Volume Up"
        >
          <Volume2 className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* Keyboard Shortcut Instructions */}
      <div className="w-full bg-neutral-950/80 rounded-xl p-2.5 text-[10px] text-neutral-400 space-y-1 border border-neutral-800/80">
        <div className="font-semibold text-neutral-300 text-[11px] mb-1">Keyboard Shortcuts:</div>
        <div className="flex justify-between">
          <span>Navigate</span>
          <span className="text-neutral-200 font-mono">Arrow Keys</span>
        </div>
        <div className="flex justify-between">
          <span>Launch</span>
          <span className="text-neutral-200 font-mono">Enter</span>
        </div>
        <div className="flex justify-between">
          <span>Favorite</span>
          <span className="text-amber-400 font-mono">Hold Enter</span>
        </div>
        <div className="flex justify-between">
          <span>Scroll Top</span>
          <span className="text-neutral-200 font-mono">H (Home)</span>
        </div>
        <div className="flex justify-between">
          <span>Test Back</span>
          <span className="text-neutral-200 font-mono">Esc (Back)</span>
        </div>
      </div>
    </div>
  );
};
