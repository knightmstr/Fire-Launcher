import React, { useState } from 'react';
import { X, Copy, Check, Terminal, AlertTriangle, Tv, CheckCircle2 } from 'lucide-react';

interface AdbGuideModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const AdbGuideModal: React.FC<AdbGuideModalProps> = ({ isOpen, onClose }) => {
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  if (!isOpen) return null;

  const copyText = async (text: string, key: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedKey(key);
      setTimeout(() => setCopiedKey(null), 2000);
    } catch {
      // ignore
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl w-full max-w-3xl max-h-[85vh] flex flex-col shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 border-b border-neutral-800 flex items-center justify-between bg-neutral-950">
          <div className="flex items-center space-x-3">
            <div className="w-8 h-8 rounded-lg bg-blue-500/20 text-blue-400 flex items-center justify-center">
              <Terminal className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-white">Fire TV ADB Installation & Setup Guide</h2>
              <p className="text-xs text-neutral-400">Step-by-step instructions for sideloading and configuring FireLauncher</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-neutral-400 hover:text-white flex items-center justify-center transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6 text-sm text-neutral-300">
          {/* Step 1 */}
          <div className="bg-neutral-950/60 border border-neutral-800/80 rounded-xl p-4">
            <div className="flex items-center space-x-2.5 mb-2">
              <div className="w-6 h-6 rounded-full bg-orange-600 text-white font-bold text-xs flex items-center justify-center">
                1
              </div>
              <h3 className="font-semibold text-white">Enable Developer Options on Amazon Fire TV</h3>
            </div>
            <ol className="list-decimal list-inside text-xs text-neutral-400 space-y-1.5 ml-1">
              <li>On Fire TV, go to <strong>Settings</strong> → <strong>My Fire TV</strong> (or <strong>Device & Software</strong>) → <strong>About</strong>.</li>
              <li>Highlight your Fire TV device name and press the <strong>Select / Center remote button 7 times</strong> until you see <em>"No need, you are already a developer."</em></li>
              <li>Press <strong>Back</strong>, open <strong>Developer Options</strong>, and turn <strong>ADB Debugging</strong> to <strong>ON</strong>.</li>
              <li>Check your device IP address in <strong>About</strong> → <strong>Network</strong> (e.g. <code className="text-amber-300">192.168.1.50</code>).</li>
            </ol>
          </div>

          {/* Step 2 */}
          <div className="bg-neutral-950/60 border border-neutral-800/80 rounded-xl p-4">
            <div className="flex items-center space-x-2.5 mb-2">
              <div className="w-6 h-6 rounded-full bg-orange-600 text-white font-bold text-xs flex items-center justify-center">
                2
              </div>
              <h3 className="font-semibold text-white">Connect via Wireless ADB & Sideload APK</h3>
            </div>

            <div className="space-y-3 mt-3">
              <div>
                <div className="flex justify-between text-xs text-neutral-400 mb-1">
                  <span>Connect to Fire TV over Wi-Fi:</span>
                  <button
                    onClick={() => copyText('adb connect <ip>:5555', 'connect')}
                    className="text-amber-400 hover:text-amber-300 flex items-center space-x-1"
                  >
                    {copiedKey === 'connect' ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
                    <span>{copiedKey === 'connect' ? 'Copied' : 'Copy'}</span>
                  </button>
                </div>
                <div className="bg-black/80 rounded-lg p-2.5 font-mono text-xs text-emerald-400 border border-neutral-800">
                  adb connect &lt;ip&gt;:5555
                </div>
              </div>

              <div>
                <div className="flex justify-between text-xs text-neutral-400 mb-1">
                  <span>Install the compiled debug APK:</span>
                  <button
                    onClick={() => copyText('adb install -r app-debug.apk', 'install')}
                    className="text-amber-400 hover:text-amber-300 flex items-center space-x-1"
                  >
                    {copiedKey === 'install' ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
                    <span>{copiedKey === 'install' ? 'Copied' : 'Copy'}</span>
                  </button>
                </div>
                <div className="bg-black/80 rounded-lg p-2.5 font-mono text-xs text-emerald-400 border border-neutral-800">
                  adb install -r app/build/outputs/apk/debug/app-debug.apk
                </div>
              </div>

              <div>
                <div className="flex justify-between text-xs text-neutral-400 mb-1">
                  <span>Launch FireLauncher immediately via ADB:</span>
                  <button
                    onClick={() => copyText('adb shell am start -n com.example.firelauncher/.MainActivity', 'start')}
                    className="text-amber-400 hover:text-amber-300 flex items-center space-x-1"
                  >
                    {copiedKey === 'start' ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
                    <span>{copiedKey === 'start' ? 'Copied' : 'Copy'}</span>
                  </button>
                </div>
                <div className="bg-black/80 rounded-lg p-2.5 font-mono text-xs text-emerald-400 border border-neutral-800">
                  adb shell am start -n com.example.firelauncher/.MainActivity
                </div>
              </div>
            </div>
          </div>

          {/* Step 3: Important Fire OS Limitation Note */}
          <div className="bg-amber-950/20 border border-amber-600/30 rounded-xl p-4">
            <div className="flex items-start space-x-3">
              <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
              <div>
                <h4 className="font-semibold text-amber-300 text-sm">
                  Crucial Note: Fire OS Has No Default Launcher Setting
                </h4>
                <p className="text-xs text-amber-200/80 mt-1 leading-relaxed">
                  Unlike standard Android TV or Google TV, <strong>Amazon Fire OS does NOT provide any user-facing settings menu to select or change the default home launcher</strong>. Amazon hardcodes the home button to the stock Amazon launcher (<code className="text-amber-200">com.amazon.tv.launcher</code>).
                </p>
                <div className="mt-3 text-xs text-neutral-300 space-y-2">
                  <div className="font-medium text-white">Recommended Approaches for Daily Use:</div>
                  <ul className="list-disc list-inside space-y-1 text-neutral-400">
                    <li><strong className="text-neutral-200">Button Mapper (Recommended)</strong>: Sideload <em>Button Mapper</em> or <em>Launcher Manager</em>. Remap the Home button single-press or double-press to launch <code className="text-amber-300">com.example.firelauncher/.MainActivity</code>.</li>
                    <li><strong className="text-neutral-200">Boot Startup</strong>: Configure an auto-start app on boot so FireLauncher loads upon Fire TV startup.</li>
                    <li><strong className="text-neutral-200">Top Row Pin</strong>: Keep FireLauncher pinned in your top favorite apps bar on stock Fire OS for instantaneous 1-click access to an ad-free experience.</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-3 border-t border-neutral-800 bg-neutral-950 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-white text-xs font-semibold transition"
          >
            Close Guide
          </button>
        </div>
      </div>
    </div>
  );
};
