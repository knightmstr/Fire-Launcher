import React, { useState } from 'react';
import { X, Copy, Check, FileCode, Folder, Search, Download } from 'lucide-react';
import { ANDROID_PROJECT_FILES } from '../data/projectFiles';
import { ProjectFile } from '../types';
import { downloadAndroidProjectZip } from '../utils/zipExporter';

interface FileViewerModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialFilePath?: string;
}

export const FileViewerModal: React.FC<FileViewerModalProps> = ({
  isOpen,
  onClose,
  initialFilePath
}) => {
  const [selectedFile, setSelectedFile] = useState<ProjectFile>(() => {
    if (initialFilePath) {
      const match = ANDROID_PROJECT_FILES.find((f) => f.path === initialFilePath);
      if (match) return match;
    }
    return ANDROID_PROJECT_FILES[0];
  });

  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState<string>('all');
  const [copied, setCopied] = useState(false);
  const [isDownloading, setIsDownloading] = useState(false);

  if (!isOpen) return null;

  const categories = [
    { id: 'all', label: 'All Files' },
    { id: 'kotlin', label: 'Kotlin (.kt)' },
    { id: 'manifest', label: 'Manifest' },
    { id: 'gradle', label: 'Gradle (.kts)' },
    { id: 'res', label: 'Resources' },
    { id: 'scripts', label: 'Automation & Scripts' },
    { id: 'docs', label: 'Documentation' }
  ];

  const filteredFiles = ANDROID_PROJECT_FILES.filter((file) => {
    const matchesCat = activeCategory === 'all' || file.category === activeCategory;
    const matchesSearch =
      file.path.toLowerCase().includes(searchQuery.toLowerCase()) ||
      file.description.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCat && matchesSearch;
  });

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(selectedFile.content);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // ignore
    }
  };

  const handleDownload = async () => {
    setIsDownloading(true);
    try {
      await downloadAndroidProjectZip();
    } finally {
      setIsDownloading(false);
    }
  };

  const lines = selectedFile.content.split('\n');

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl w-full max-w-6xl h-[88vh] flex flex-col shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 border-b border-neutral-800 flex items-center justify-between bg-neutral-950">
          <div className="flex items-center space-x-3">
            <div className="w-8 h-8 rounded-lg bg-orange-500/20 text-orange-400 flex items-center justify-center font-bold">
              🔥
            </div>
            <div>
              <h2 className="text-lg font-bold text-white">FireLauncher — Android Studio Project Explorer</h2>
              <p className="text-xs text-neutral-400">Complete, ready-to-compile source tree for Amazon Fire TV (Fire OS)</p>
            </div>
          </div>

          <div className="flex items-center space-x-3">
            <button
              onClick={handleDownload}
              disabled={isDownloading}
              className="px-3.5 py-1.5 rounded-lg bg-orange-600 hover:bg-orange-500 text-white text-xs font-semibold flex items-center space-x-2 transition active:scale-95 shadow-md"
            >
              <Download className="w-4 h-4" />
              <span>{isDownloading ? 'Packaging Zip...' : 'Download Project (.zip)'}</span>
            </button>

            <button
              onClick={onClose}
              className="w-8 h-8 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-neutral-400 hover:text-white flex items-center justify-center transition"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Body Content Split */}
        <div className="flex-1 flex overflow-hidden">
          {/* File Sidebar */}
          <div className="w-80 border-r border-neutral-800 flex flex-col bg-neutral-950/60">
            {/* Search */}
            <div className="p-3 border-b border-neutral-800">
              <div className="relative">
                <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-neutral-500" />
                <input
                  type="text"
                  placeholder="Filter files..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full bg-neutral-900 border border-neutral-800 rounded-lg pl-8 pr-3 py-1.5 text-xs text-neutral-200 placeholder-neutral-500 focus:outline-none focus:border-neutral-700"
                />
              </div>

              {/* Category Pills */}
              <div className="flex flex-wrap gap-1 mt-2.5">
                {categories.map((cat) => (
                  <button
                    key={cat.id}
                    onClick={() => setActiveCategory(cat.id)}
                    className={`px-2 py-0.5 rounded text-[10px] font-medium transition ${
                      activeCategory === cat.id
                        ? 'bg-neutral-700 text-white'
                        : 'bg-neutral-900 text-neutral-400 hover:text-neutral-300'
                    }`}
                  >
                    {cat.label}
                  </button>
                ))}
              </div>
            </div>

            {/* File List */}
            <div className="flex-1 overflow-y-auto p-2 space-y-1 text-xs">
              {filteredFiles.map((file) => {
                const isSelected = selectedFile.path === file.path;
                return (
                  <button
                    key={file.path}
                    onClick={() => setSelectedFile(file)}
                    className={`w-full text-left px-3 py-2 rounded-lg flex items-start space-x-2.5 transition ${
                      isSelected
                        ? 'bg-orange-500/15 border border-orange-500/30 text-white'
                        : 'hover:bg-neutral-850 text-neutral-300 border border-transparent'
                    }`}
                  >
                    <FileCode
                      className={`w-4 h-4 mt-0.5 shrink-0 ${
                        isSelected ? 'text-orange-400' : 'text-neutral-500'
                      }`}
                    />
                    <div className="min-w-0 flex-1">
                      <div className="font-mono text-[11px] font-semibold truncate">{file.name}</div>
                      <div className="text-[10px] text-neutral-500 truncate">{file.path}</div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Code Viewer */}
          <div className="flex-1 flex flex-col bg-neutral-900 overflow-hidden">
            {/* File Top Meta */}
            <div className="px-5 py-3 border-b border-neutral-800 bg-neutral-950/80 flex items-center justify-between">
              <div>
                <div className="flex items-center space-x-2">
                  <span className="font-mono text-xs font-bold text-white">{selectedFile.path}</span>
                  <span className="px-2 py-0.5 rounded bg-neutral-800 text-neutral-400 text-[10px] uppercase font-mono">
                    {selectedFile.language}
                  </span>
                  <span className="text-[11px] text-neutral-500">{lines.length} lines</span>
                </div>
                <p className="text-xs text-neutral-400 mt-0.5">{selectedFile.description}</p>
              </div>

              <button
                onClick={handleCopy}
                className="px-3 py-1.5 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-neutral-200 text-xs font-medium flex items-center space-x-1.5 transition active:scale-95"
              >
                {copied ? <Check className="w-3.5 h-3.5 text-green-400" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copied ? 'Copied!' : 'Copy Code'}</span>
              </button>
            </div>

            {/* Code Content */}
            <div className="flex-1 overflow-auto p-4 font-mono text-[12px] leading-relaxed select-text bg-[#0D0F14]">
              <pre className="text-neutral-300">
                {lines.map((line, idx) => (
                  <div key={idx} className="table-row hover:bg-neutral-800/40">
                    <span className="table-cell select-none pr-4 text-right text-neutral-600 text-[11px] w-10">
                      {idx + 1}
                    </span>
                    <span className="table-cell whitespace-pre">{line}</span>
                  </div>
                ))}
              </pre>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
