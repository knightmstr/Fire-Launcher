/**
 * Live Weather Service using Open-Meteo (open, free, no API key required).
 * Provides live real-time weather conditions and geocoding for any city worldwide.
 */

export interface LiveWeatherData {
  city: string;
  country: string;
  tempF: number;
  tempC: number;
  condition: string;
  icon: 'sun' | 'cloud-sun' | 'cloud' | 'rain' | 'thunder' | 'snow';
  highF: number;
  lowF: number;
  highC: number;
  lowC: number;
  humidity: number;
  windMph: number;
  windKmh: number;
  weatherCode: number;
  isDay: boolean;
  latitude: number;
  longitude: number;
  updatedAt: string;
}

// Map WMO weather interpretation codes to conditions and icons
export function mapWmoToCondition(code: number, isDay = true): { condition: string; icon: LiveWeatherData['icon'] } {
  switch (code) {
    case 0:
      return { condition: isDay ? 'Clear Sky' : 'Clear Night', icon: 'sun' };
    case 1:
      return { condition: isDay ? 'Mainly Sunny' : 'Mainly Clear', icon: 'sun' };
    case 2:
      return { condition: 'Partly Cloudy', icon: 'cloud-sun' };
    case 3:
      return { condition: 'Overcast', icon: 'cloud' };
    case 45:
    case 48:
      return { condition: 'Foggy', icon: 'cloud' };
    case 51:
    case 53:
    case 55:
      return { condition: 'Drizzle', icon: 'rain' };
    case 56:
    case 57:
      return { condition: 'Freezing Drizzle', icon: 'snow' };
    case 61:
    case 63:
    case 65:
      return { condition: 'Rain', icon: 'rain' };
    case 66:
    case 67:
      return { condition: 'Freezing Rain', icon: 'rain' };
    case 71:
    case 73:
    case 75:
      return { condition: 'Snow Fall', icon: 'snow' };
    case 77:
      return { condition: 'Snow Grains', icon: 'snow' };
    case 80:
    case 81:
    case 82:
      return { condition: 'Rain Showers', icon: 'rain' };
    case 85:
    case 86:
      return { condition: 'Snow Showers', icon: 'snow' };
    case 95:
      return { condition: 'Thunderstorm', icon: 'thunder' };
    case 96:
    case 99:
      return { condition: 'Thunderstorm with Hail', icon: 'thunder' };
    default:
      return { condition: 'Fair', icon: 'cloud-sun' };
  }
}

// Popular quick-selection cities with coordinates pre-cached for instant switching
export const DEFAULT_CITIES = [
  { name: 'Seattle', country: 'United States', lat: 47.6062, lon: -122.3321 },
  { name: 'New York', country: 'United States', lat: 40.7128, lon: -74.006 },
  { name: 'Los Angeles', country: 'United States', lat: 34.0522, lon: -118.2437 },
  { name: 'Chicago', country: 'United States', lat: 41.8781, lon: -87.6298 },
  { name: 'London', country: 'United Kingdom', lat: 51.5074, lon: -0.1278 },
  { name: 'Tokyo', country: 'Japan', lat: 35.6762, lon: 139.6503 },
  { name: 'Paris', country: 'France', lat: 48.8566, lon: 2.3522 },
  { name: 'Berlin', country: 'Germany', lat: 52.52, lon: 13.405 },
  { name: 'Sydney', country: 'Australia', lat: -33.8688, lon: 151.2093 },
  { name: 'Miami', country: 'United States', lat: 25.7617, lon: -80.1918 },
  { name: 'Denver', country: 'United States', lat: 39.7392, lon: -104.9903 },
  { name: 'Toronto', country: 'Canada', lat: 43.6532, lon: -79.3832 },
  { name: 'Kathmandu', country: 'Nepal', lat: 27.7172, lon: 85.324 },
  { name: 'Singapore', country: 'Singapore', lat: 1.3521, lon: 103.8198 },
  { name: 'Dubai', country: 'United Arab Emirates', lat: 25.2048, lon: 55.2708 }
];

/**
 * Searches for city coordinates using Open-Meteo Geocoding API.
 */
export async function searchCityCoordinates(cityName: string): Promise<{ name: string; country: string; lat: number; lon: number } | null> {
  const trimmed = cityName.trim();
  if (!trimmed) return null;

  // Check pre-cached list first
  const cached = DEFAULT_CITIES.find(c => c.name.toLowerCase() === trimmed.toLowerCase());
  if (cached) {
    return { name: cached.name, country: cached.country, lat: cached.lat, lon: cached.lon };
  }

  try {
    const url = `https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(trimmed)}&count=1&language=en&format=json`;
    const res = await fetch(url);
    if (!res.ok) throw new Error('Geocoding request failed');
    const data = await res.json();
    if (data.results && data.results.length > 0) {
      const first = data.results[0];
      return {
        name: first.name,
        country: [first.admin1, first.country].filter(Boolean).join(', '),
        lat: first.latitude,
        lon: first.longitude
      };
    }
    return null;
  } catch (error) {
    console.error('Failed to geocode city:', error);
    return null;
  }
}

/**
 * Fetches live weather for a given city name from Open-Meteo.
 */
export async function fetchLiveWeather(cityName: string): Promise<LiveWeatherData> {
  let geo = await searchCityCoordinates(cityName);
  if (!geo) {
    // Fallback to Seattle if unknown city
    geo = { name: cityName, country: 'Location', lat: 47.6062, lon: -122.3321 };
  }

  const weatherUrl = `https://api.open-meteo.com/v1/forecast?latitude=${geo.lat}&longitude=${geo.lon}&current=temperature_2m,relative_humidity_2m,is_day,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min&timezone=auto`;

  const res = await fetch(weatherUrl);
  if (!res.ok) {
    throw new Error(`Open-Meteo weather fetch error: ${res.statusText}`);
  }
  const data = await res.json();

  const current = data.current;
  const daily = data.daily;

  const tempC = Math.round(current.temperature_2m);
  const tempF = Math.round((tempC * 9) / 5 + 32);

  const highC = daily && daily.temperature_2m_max?.length ? Math.round(daily.temperature_2m_max[0]) : tempC + 3;
  const lowC = daily && daily.temperature_2m_min?.length ? Math.round(daily.temperature_2m_min[0]) : tempC - 4;

  const highF = Math.round((highC * 9) / 5 + 32);
  const lowF = Math.round((lowC * 9) / 5 + 32);

  const windKmh = Math.round(current.wind_speed_10m || 0);
  const windMph = Math.round(windKmh * 0.621371);

  const wmo = mapWmoToCondition(current.weather_code, current.is_day === 1);

  return {
    city: geo.name,
    country: geo.country,
    tempC,
    tempF,
    condition: wmo.condition,
    icon: wmo.icon,
    highC,
    lowC,
    highF,
    lowF,
    humidity: current.relative_humidity_2m || 50,
    windMph,
    windKmh,
    weatherCode: current.weather_code,
    isDay: current.is_day === 1,
    latitude: geo.lat,
    longitude: geo.lon,
    updatedAt: new Date().toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })
  };
}
