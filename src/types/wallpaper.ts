export interface TvWallpaper {
  id: string;
  title: string;
  url: string;
  thumbnail?: string;
  author: string;
  permalink: string;
  category?: string;
  source: 'reddit' | 'curated';
}
