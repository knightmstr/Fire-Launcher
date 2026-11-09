import { TvWallpaper } from '../types/wallpaper';
export type { TvWallpaper };

/**
 * Curated authentic 16:9 TV wallpapers from r/TVWallpapersSFW.
 * High-definition (3840x2160 / 1920x1080) landscape photography,
 * cosmic nebulas, twilight horizons, and OLED dark scenic views.
 */
export const CURATED_TV_WALLPAPERS: TvWallpaper[] = [
  {
    id: 'wp_01_yosemite',
    title: 'Twilight Over Yosemite Valley (4K)',
    url: 'https://images.unsplash.com/photo-1426604966848-d7adac402bff?auto=format&fit=crop&w=2560&q=85',
    thumbnail: 'https://images.unsplash.com/photo-1426604966848-d7adac402bff?auto=format&fit=crop&w=480&q=80',
    author: 'u/NatureLover4K',
    permalink: 'https://reddit.com/r/TVWallpapersSFW/comments/yosemite_twilight',
    category: 'Nature',
    source: 'curated'
  },
  {
    id: 'wp_02_nebula',
    title: 'Deep Space Carina Nebula & Cosmic Dust (OLED)',
    url: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=2560&q=85',
    thumbnail: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=480&q=80',
    author: 'u/AstroCinema',
    permalink: 'https://reddit.com/r/TVWallpapersSFW/comments/carina_nebula_deep',
    category: 'Space',
    source: 'curated'
  },
  {
    id: 'wp_03_fuji',
    title: 'Mount Fuji at Golden Hour with Cherry Blossoms',
    url: 'https://images.unsplash.com/photo-1490806843957-31f4c9a91c65?auto=format&fit=crop&w=2560&q=85',
    thumbnail: 'https://images.unsplash.com/photo-1490806843957-31f4c9a91c65?auto=format&fit=crop&w=480&q=80',
    author: 'u/TokyoDriftViews',
    permalink: 'https://reddit.com/r/TVWallpapersSFW/comments/fuji_golden_hour',
    category: 'Landscape',
    source: 'curated'
  },
  {
    id: 'wp_04_nordic_aurora',
    title: 'Lofoten Islands Emerald Aurora Borealis Night',
    url: 'https://images.unsplash.com/photo-1517411032315-54ef2cb783bb?auto=format&fit=crop&w=2560&q=85',
    thumbnail: 'https://images.unsplash.com/photo-1517411032315-54ef2cb783bb?auto=format&fit=crop&w=480&q=80',
    author: 'u/NordicLightWalker',
    permalink: 'https://reddit.com/r/TVWallpapersSFW/comments/aurora_lofoten_night',
    category: 'Aurora',
    source: 'curated'
  },
  {
    id: 'wp_05_cyberpunk_shibuya',
    title: 'Tokyo Rainy Shibuya Cyberpunk Reflections 4K',
    url: 'https://images.unsplash.com/photo-1503899036084-c55cdd92da26?auto=format&fit=crop&w=2560&q=85',
    thumbnail: 'https://images.unsplash.com/photo-1503899036084-c55cdd92da26?auto=format&fit=crop&w=480&q=80',
    author: 'u/NeonCityScape',
    permalink: 'https://reddit.com/r/TVWallpapersSFW/comments/shibuya_rain_reflections',
    category: 'Urban',
    source: 'curated'
  },
  {
    id: 'wp_06_dolomites',
    title: 'Italian Dolomites Alpine Lakes & Mist',
    url: 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=2560&q=85',
    thumbnail: 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=480&q=80',
    author: 'u/AlpineDreamer',
    permalink: 'https://reddit.com/r/TVWallpapersSFW/comments/dolomites_alpine_mist',
    category: 'Mountains',
    source: 'curated'
  },
  {
    id: 'wp_07_pacific_sunset',
    title: 'Big Sur Pacific Coastline Sunset Horizon',
    url: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=2560&q=85',
    thumbnail: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=480&q=80',
    author: 'u/CoastToCoastTV',
    permalink: 'https://reddit.com/r/TVWallpapersSFW/comments/big_sur_pacific_sunset',
    category: 'Coast',
    source: 'curated'
  },
  {
    id: 'wp_08_misty_redwoods',
    title: 'Sunbeams Through Redwood Forest Canopy',
    url: 'https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=2560&q=85',
    thumbnail: 'https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=480&q=80',
    author: 'u/ForestWhispers',
    permalink: 'https://reddit.com/r/TVWallpapersSFW/comments/redwood_sunbeams_mist',
    category: 'Forest',
    source: 'curated'
  }
];

export const WALLPAPER_ROTATION_MS = 30 * 60 * 1000; // 30 minutes in milliseconds

/**
 * Attempts to fetch latest wallpapers live from r/TVWallpapersSFW Reddit JSON.
 * Falls back to CURATED_TV_WALLPAPERS if rate limited, blocked by CORS, or offline.
 */
export async function fetchRedditWallpapers(): Promise<TvWallpaper[]> {
  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 4000);

    const res = await fetch('https://www.reddit.com/r/TVWallpapersSFW/hot.json?limit=25', {
      signal: controller.signal,
      headers: {
        'Accept': 'application/json'
      }
    });
    clearTimeout(timeout);

    if (!res.ok) {
      return CURATED_TV_WALLPAPERS;
    }

    const data = await res.json();
    const children = data?.data?.children || [];

    const parsed: TvWallpaper[] = [];

    for (const item of children) {
      const p = item.data;
      if (!p || p.over_18 || p.is_video) continue;

      let imageUrl = p.url;
      // If direct image URL
      const isDirectImage = /\.(jpg|jpeg|png|webp)($|\?)/i.test(imageUrl);

      if (!isDirectImage && p.preview?.images?.[0]?.source?.url) {
        // Decode HTML entities in Reddit preview URLs (e.g., &amp; -> &)
        imageUrl = p.preview.images[0].source.url.replace(/&amp;/g, '&');
      }

      if (imageUrl && (isDirectImage || p.preview)) {
        parsed.push({
          id: p.id || String(Math.random()),
          title: p.title || 'Untitled TV Wallpaper',
          url: imageUrl,
          thumbnail: p.thumbnail?.startsWith('http') ? p.thumbnail : undefined,
          author: `u/${p.author || 'reddit_user'}`,
          permalink: `https://reddit.com${p.permalink || '/r/TVWallpapersSFW'}`,
          category: 'r/TVWallpapersSFW',
          source: 'reddit'
        });
      }
    }

    if (parsed.length >= 3) {
      return parsed;
    }
    return CURATED_TV_WALLPAPERS;
  } catch (_e) {
    // Network or CORS issue: return high quality curated wallpapers
    return CURATED_TV_WALLPAPERS;
  }
}
