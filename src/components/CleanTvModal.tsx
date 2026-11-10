import React, { useState } from 'react';
import {
  X,
  ShieldCheck,
  Zap,
  Trash2,
  RotateCcw,
  Copy,
  Check,
  Terminal,
  Cpu,
  Tv,
  CheckCircle2,
  Sparkles,
  ExternalLink
} from 'lucide-react';

interface CleanTvModalProps {
  isOpen: boolean;
  onClose: () => void;
}

interface DebloatItem {
  id: string;
  pkg: string;
  name: string;
  desc: string;
  category: 'Ads' | 'Telemetry' | 'Autoplay' | 'Background';
  ramMb: number;
  disabled: boolean;
}

export const CleanTvModal: React.FC<CleanTvModalProps> = ({ isOpen, onClose }) => {
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  const [packages, setPackages] = useState<DebloatItem[]>([
    {
      id: 'ads-1',
      pkg: 'com.amazon.advertising.firetv',
      name: 'Amazon Sponsored Ads Engine',
      desc: 'Injects banner video ads and sponsored carousels on the home screen',
      category: 'Ads',
      ramMb: 65,
      disabled: true
    },
    {
      id: 'ads-2',
      pkg: 'com.amazon.device.sale.service',
      name: 'Promotions & Deals Broadcaster',
      desc: 'Runs background checks for shopping events and Prime Video rental deals',
      category: 'Ads',
      ramMb: 35,
      disabled: true
    },
    {
      id: 'telem-1',
      pkg: 'com.amazon.client.metrics',
      name: 'Amazon Telemetry & Metrics',
      desc: 'Logs app usage patterns, clickstreams, and sends telemetry data to Amazon',
      category: 'Telemetry',
      ramMb: 50,
      disabled: true
    },
    {
      id: 'telem-2',
      pkg: 'com.amazon.device.crashmanager',
      name: 'Crash Reporter Daemon',
      desc: 'Continuously monitors system logs and dumps stack traces over the network',
      category: 'Telemetry',
      ramMb: 30,
      disabled: true
    },
    {
      id: 'screensaver-1',
      pkg: 'com.amazon.ftv.screensaver',
      name: 'Sponsored Ambient Screensaver',
      desc: 'Replaces ambient landscape photos with commercial video ads when idle',
      category: 'Autoplay',
      ramMb: 55,
      disabled: true
    },
    {
      id: 'music-1',
      pkg: 'com.amazon.bueller.music',
      name: 'Amazon Music Background Preloader',
      desc: 'Pre-caches promotional audio tracks even if using Spotify or YouTube',
      category: 'Background',
      ramMb: 40,
      disabled: false
    }
  ]);

  if (!isOpen) return null;

  const totalDisabled = packages.filter((p) => p.disabled);
  const totalRamFreed = totalDisabled.reduce((acc, p) => acc + p.ramMb, 0);

  const togglePackage = (id: string) => {
    setPackages((prev) =>
      prev.map((item) => (item.id === id ? { ...item, disabled: !item.disabled } : item))
    );
  };

  const copyScript = async (type: 'debloat' | 'restore') => {
    let script = '';
    if (type === 'debloat') {
      script = `# CleanYourTV — Safe Fire TV Debloat Script\n# Inspired by github.com/Gathin23/cleanyourtv\n\n`;
      totalDisabled.forEach((item) => {
        script += `adb shell pm disable-user --user 0 ${item.pkg}\n`;
      });
      script += `\necho "Done! Freed ~${totalRamFreed}MB RAM and removed ads."`;
    } else {
      script = `# CleanYourTV — Factory Restore Script\n\n`;
      packages.forEach((item) => {
        script += `adb shell pm enable ${item.pkg}\n`;
      });
      script += `\necho "Restored all packages to factory default."`;
    }

    try {
      await navigator.clipboard.writeText(script);
      setCopiedKey(type);
      setTimeout(() => setCopiedKey(null), 2000);
    } catch {
      // ignore
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
      <div className="bg-neutral-900 border border-neutral-800 rounded-3xl w-full max-w-4xl max-h-[90vh] flex flex-col shadow-2xl overflow-hidden animate-fade-in">
        {/* Header */}
        <div className="px-6 py-4 border-b border-neutral-800 bg-[#12151E] flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-emerald-500 to-teal-600 flex items-center justify-center shadow-lg shadow-emerald-500/25">
              <ShieldCheck className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h2 className="text-base font-bold text-white">CleanYourTV — Fire TV Debloat & Performance Studio</h2>
                <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-[10px] font-bold">
                  NON-ROOT • SAFE
                </span>
              </div>
              <p className="text-xs text-neutral-400">
                Inspired by <span className="text-emerald-400 font-medium">Gathin23/cleanyourtv</span> • Disables ads, telemetry, and background trackers over ADB
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="w-8 h-8 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-neutral-400 hover:text-white flex items-center justify-center transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Stats Row */}
        <div className="grid grid-cols-3 gap-3 p-6 pb-2">
          <div className="bg-neutral-950/80 border border-neutral-800 rounded-2xl p-4 flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/15 text-emerald-400 flex items-center justify-center">
              <Zap className="w-5 h-5" />
            </div>
            <div>
              <div className="text-xl font-extrabold text-white">+{totalRamFreed} MB</div>
              <div className="text-[11px] text-neutral-400">RAM Memory Recovered</div>
            </div>
          </div>

          <div className="bg-neutral-950/80 border border-neutral-800 rounded-2xl p-4 flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-orange-500/15 text-orange-400 flex items-center justify-center">
              <Tv className="w-5 h-5" />
            </div>
            <div>
              <div className="text-xl font-extrabold text-white">0 Ads</div>
              <div className="text-[11px] text-neutral-400">100% Ad-Free Home Screen</div>
            </div>
          </div>

          <div className="bg-neutral-950/80 border border-neutral-800 rounded-2xl p-4 flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-blue-500/15 text-blue-400 flex items-center justify-center">
              <Cpu className="w-5 h-5" />
            </div>
            <div>
              <div className="text-xl font-extrabold text-white">
                {totalDisabled.length} / {packages.length}
              </div>
              <div className="text-[11px] text-neutral-400">Bloat Daemons Inactive</div>
            </div>
          </div>
        </div>

        {/* Content Body */}
        <div className="flex-1 overflow-y-auto px-6 py-4 space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-neutral-300 uppercase tracking-wider">
              Safe Fire OS Debloat Packages
            </span>
            <div className="flex items-center space-x-2">
              <button
                onClick={() => copyScript('debloat')}
                className="px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold flex items-center space-x-1.5 transition active:scale-95 shadow-md shadow-emerald-600/20"
              >
                {copiedKey === 'debloat' ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copiedKey === 'debloat' ? 'Copied ADB Commands!' : 'Copy Debloat Script'}</span>
              </button>

              <button
                onClick={() => copyScript('restore')}
                className="px-3 py-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 text-xs font-medium flex items-center space-x-1.5 transition active:scale-95"
              >
                {copiedKey === 'restore' ? <Check className="w-3.5 h-3.5" /> : <RotateCcw className="w-3.5 h-3.5" />}
                <span>{copiedKey === 'restore' ? 'Copied Restore Script!' : 'Copy Restore Script'}</span>
              </button>
            </div>
          </div>

          {/* Package List */}
          <div className="space-y-2">
            {packages.map((pkg) => (
              <div
                key={pkg.id}
                onClick={() => togglePackage(pkg.id)}
                className={`p-3.5 rounded-xl border transition cursor-pointer flex items-center justify-between select-none ${
                  pkg.disabled
                    ? 'bg-neutral-950/70 border-emerald-500/30 hover:border-emerald-500/50'
                    : 'bg-neutral-900/40 border-neutral-800 hover:border-neutral-700 opacity-60'
                }`}
              >
                <div className="flex items-center space-x-3.5 min-w-0 flex-1">
                  <div
                    className={`w-6 h-6 rounded-lg flex items-center justify-center shrink-0 ${
                      pkg.disabled
                        ? 'bg-emerald-500 text-black font-bold'
                        : 'bg-neutral-800 text-neutral-500'
                    }`}
                  >
                    {pkg.disabled ? <Check className="w-3.5 h-3.5 stroke-[3]" /> : null}
                  </div>

                  <div className="min-w-0 flex-1">
                    <div className="flex items-center space-x-2">
                      <span className="text-xs font-bold text-white truncate">{pkg.name}</span>
                      <span className="px-1.5 py-0.5 rounded bg-neutral-800 text-[9px] font-medium text-neutral-400">
                        {pkg.category}
                      </span>
                    </div>
                    <div className="text-[11px] text-neutral-400 truncate">{pkg.desc}</div>
                    <div className="text-[10px] font-mono text-neutral-500 truncate mt-0.5">
                      {pkg.pkg}
                    </div>
                  </div>
                </div>

                <div className="text-right shrink-0 ml-4">
                  <span className="text-xs font-bold text-emerald-400">+{pkg.ramMb} MB</span>
                  <div className="text-[10px] text-neutral-500">
                    {pkg.disabled ? 'Disabled' : 'Enabled'}
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* CleanYourTV Non-Root Explanation */}
          <div className="bg-emerald-950/20 border border-emerald-600/30 rounded-2xl p-4 text-xs text-neutral-300 space-y-1.5">
            <div className="font-semibold text-emerald-300 flex items-center space-x-1.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              <span>How CleanYourTV Works (100% Reversible Non-Root Command)</span>
            </div>
            <p className="text-[11px] text-neutral-400 leading-relaxed">
              CleanYourTV uses Android's native <code className="text-amber-300 font-mono">pm disable-user --user 0 &lt;package&gt;</code> command over ADB. It does not tamper with system partitions or require root access. Any disabled app can be instantly restored at any time with <code className="text-amber-300 font-mono">pm enable &lt;package&gt;</code>.
            </p>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-3 border-t border-neutral-800 bg-[#12151E] flex items-center justify-between text-xs text-neutral-400">
          <div className="flex items-center space-x-1">
            <span>Official repository:</span>
            <a
              href="https://github.com/Gathin23/cleanyourtv"
              target="_blank"
              rel="noopener noreferrer"
              className="text-emerald-400 hover:underline flex items-center space-x-1"
            >
              <span>Gathin23/cleanyourtv</span>
              <ExternalLink className="w-3 h-3" />
            </a>
          </div>
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-white font-semibold transition"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
};
