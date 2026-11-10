export interface CityLocation {
  id: string;
  name: string;
  admin1?: string; // State / Region
  country: string;
  countryCode?: string;
  latitude: number;
  longitude: number;
}

export interface LiveWeatherData {
  city: string;
  country: string;
  latitude: number;
  longitude: number;
  tempF: number;
  tempC: number;
  feelsLikeF: number;
  feelsLikeC: number;
  condition: string;
  icon: 'sun' | 'cloud-sun' | 'cloud' | 'rain' | 'thunder' | 'snow';
  highF: number;
  lowF: number;
  highC: number;
  lowC: number;
  humidity: number;
  windMph: number;
  isDay: boolean;
  updatedAt: string;
  isLive: boolean;
}

// Default popular starter cities
export const DEFAULT_SAVED_CITIES: CityLocation[] = [
  { id: 'city_sea', name: 'Seattle', admin1: 'Washington', country: 'United States', countryCode: 'US', latitude: 47.6062, longitude: -122.3321 },
  { id: 'city_nyc', name: 'New York', admin1: 'New York', country: 'United States', countryCode: 'US', latitude: 40.7128, longitude: -74.0060 },
  { id: 'city_lax', name: 'Los Angeles', admin1: 'California', country: 'United States', countryCode: 'US', latitude: 34.0522, longitude: -118.2437 },
  { id: 'city_lon', name: 'London', admin1: 'England', country: 'United Kingdom', countryCode: 'GB', latitude: 51.5074, longitude: -0.1278 },
  { id: 'city_tok', name: 'Tokyo', admin1: 'Tokyo Prefecture', country: 'Japan', countryCode: 'JP', latitude: 35.6762, longitude: 139.6503 },
  { id: 'city_par', name: 'Paris', admin1: 'Île-de-France', country: 'France', countryCode: 'FR', latitude: 48.8566, longitude: 2.3522 },
  { id: 'city_syd', name: 'Sydney', admin1: 'New South Wales', country: 'Australia', countryCode: 'AU', latitude: -33.8688, longitude: 151.2093 },
  { id: 'city_den', name: 'Denver', admin1: 'Colorado', country: 'United States', countryCode: 'US', latitude: 39.7392, longitude: -104.9903 }
];

const STORAGE_SAVED_CITIES_KEY = 'firelauncher_saved_cities_v2';
const STORAGE_SELECTED_CITY_KEY = 'firelauncher_selected_city_v2';

export function getSavedCities(): CityLocation[] {
  try {
    const raw = localStorage.getItem(STORAGE_SAVED_CITIES_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed) && parsed.length > 0) return parsed;
    }
  } catch (_e) {
    // fallback
  }
  return DEFAULT_SAVED_CITIES;
}

export function saveCityToStorage(city: CityLocation) {
  try {
    const current = getSavedCities();
    const exists = current.some(
      (c) => c.name.toLowerCase() === city.name.toLowerCase() && c.country === city.country
    );
    let nextList = current;
    if (!exists) {
      nextList = [city, ...current];
    } else {
      // Move to top
      nextList = [city, ...current.filter((c) => c.id !== city.id)];
    }
    localStorage.setItem(STORAGE_SAVED_CITIES_KEY, JSON.stringify(nextList));
    localStorage.setItem(STORAGE_SELECTED_CITY_KEY, JSON.stringify(city));
  } catch (_e) {
    // ignore
  }
}

export function getSelectedCityFromStorage(): CityLocation {
  try {
    const raw = localStorage.getItem(STORAGE_SELECTED_CITY_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (parsed?.name && typeof parsed.latitude === 'number') return parsed;
    }
  } catch (_e) {
    // ignore
  }
  return DEFAULT_SAVED_CITIES[0];
}

/**
 * Searches global cities using Open-Meteo Free Geocoding API (Zero API Keys required).
 */
export async function searchCitiesApi(query: string): Promise<CityLocation[]> {
  const cleanQuery = (query || '').trim().slice(0, 64);
  if (cleanQuery.length < 2) return [];

  const url = `https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(
    cleanQuery
  )}&count=8&language=en&format=json`;

  try {
    const res = await fetch(url);
    if (!res.ok) return [];
    const data = await res.json();
    const results = data.results || [];

    return results.map((r: any) => ({
      id: `geo_${r.id || Math.random()}`,
      name: r.name,
      admin1: r.admin1,
      country: r.country,
      countryCode: r.country_code,
      latitude: r.latitude,
      longitude: r.longitude
    }));
  } catch (error) {
    console.warn('Geocoding search error:', error);
    return [];
  }
}

/**
 * Maps Open-Meteo / WMO weather codes to human friendly condition label and icon.
 */
function mapWmoCode(code: number, isDay: boolean): { condition: string; icon: 'sun' | 'cloud-sun' | 'cloud' | 'rain' | 'thunder' | 'snow' } {
  switch (code) {
    case 0:
      return { condition: isDay ? 'Sunny' : 'Clear Sky', icon: 'sun' };
    case 1:
      return { condition: isDay ? 'Mainly Sunny' : 'Mainly Clear', icon: 'sun' };
    case 2:
      return { condition: 'Partly Cloudy', icon: 'cloud-sun' };
    case 3:
      return { condition: 'Overcast', icon: 'cloud' };
    case 45:
    case 48:
      return { condition: 'Fog & Mist', icon: 'cloud' };
    case 51:
    case 53:
    case 55:
      return { condition: 'Light Drizzle', icon: 'rain' };
    case 56:
    case 57:
      return { condition: 'Freezing Drizzle', icon: 'rain' };
    case 61:
      return { condition: 'Light Rain', icon: 'rain' };
    case 63:
      return { condition: 'Moderate Rain', icon: 'rain' };
    case 65:
      return { condition: 'Heavy Rain', icon: 'rain' };
    case 66:
    case 67:
      return { condition: 'Freezing Rain', icon: 'rain' };
    case 71:
      return { condition: 'Light Snowfall', icon: 'snow' };
    case 73:
      return { condition: 'Moderate Snow', icon: 'snow' };
    case 75:
      return { condition: 'Heavy Snowstorm', icon: 'snow' };
    case 77:
      return { condition: 'Snow Grains', icon: 'snow' };
    case 80:
    case 81:
    case 82:
      return { condition: 'Passing Showers', icon: 'rain' };
    case 85:
    case 86:
      return { condition: 'Snow Showers', icon: 'snow' };
    case 95:
      return { condition: 'Thunderstorm', icon: 'thunder' };
    case 96:
    case 99:
      return { condition: 'Thunderstorm & Hail', icon: 'thunder' };
    default:
      return { condition: 'Fair', icon: isDay ? 'sun' : 'cloud-sun' };
  }
}

/**
 * Fetches accurate live weather from Open-Meteo for the specified coordinates.
 */
export async function fetchLiveWeather(city: CityLocation): Promise<LiveWeatherData> {
  const url = `https://api.open-meteo.com/v1/forecast?latitude=${city.latitude}&longitude=${city.longitude}&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto&temperature_unit=fahrenheit&wind_speed_unit=mph`;

  try {
    const res = await fetch(url);
    if (!res.ok) {
      throw new Error(`Open-Meteo returned status ${res.status}`);
    }
    const data = await res.json();
    const current = data.current;
    const daily = data.daily;

    const tempF = Math.round(current.temperature_2m);
    const tempC = Math.round(((tempF - 32) * 5) / 9);

    const feelsLikeF = Math.round(current.apparent_temperature);
    const feelsLikeC = Math.round(((feelsLikeF - 32) * 5) / 9);

    const highF = Math.round(daily?.temperature_2m_max?.[0] ?? tempF + 5);
    const lowF = Math.round(daily?.temperature_2m_min?.[0] ?? tempF - 6);
    const highC = Math.round(((highF - 32) * 5) / 9);
    const lowC = Math.round(((lowF - 32) * 5) / 9);

    const isDay = current.is_day === 1;
    const { condition, icon } = mapWmoCode(current.weather_code, isDay);

    const countryDisplay = city.admin1 ? `${city.admin1}, ${city.countryCode || city.country}` : city.country;

    return {
      city: city.name,
      country: countryDisplay,
      latitude: city.latitude,
      longitude: city.longitude,
      tempF,
      tempC,
      feelsLikeF,
      feelsLikeC,
      condition,
      icon,
      highF,
      lowF,
      highC,
      lowC,
      humidity: Math.round(current.relative_humidity_2m || 50),
      windMph: Math.round(current.wind_speed_10m || 5),
      isDay,
      updatedAt: new Date().toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' }),
      isLive: true
    };
  } catch (err) {
    console.error('Failed to fetch live weather from Open-Meteo:', err);
    // Return reliable cached/fallback with location name preserved
    return {
      city: city.name,
      country: city.country,
      latitude: city.latitude,
      longitude: city.longitude,
      tempF: 68,
      tempC: 20,
      feelsLikeF: 68,
      feelsLikeC: 20,
      condition: 'Partly Cloudy',
      icon: 'cloud-sun',
      highF: 73,
      lowF: 57,
      highC: 23,
      lowC: 14,
      humidity: 60,
      windMph: 8,
      isDay: true,
      updatedAt: 'Offline Mode',
      isLive: false
    };
  }
}
