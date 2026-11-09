export interface SimulatedApp {
  id: string;
  packageName: string;
  activityName: string;
  label: string;
  iconName: string;
  bannerGradient: string;
  accentColor: string;
  category: 'streaming' | 'gaming' | 'utility' | 'system';
  isSystemShortcut?: boolean;
  systemAction?: string;
  description: string;
  launchCount?: number;
  lastLaunchedAt?: number;
}

export type AppSortOrder = 'alphabetical' | 'recent' | 'frequent';

export interface ProjectFile {
  path: string;
  name: string;
  language: string;
  description: string;
  category: 'gradle' | 'manifest' | 'kotlin' | 'res' | 'docs';
  content: string;
}
