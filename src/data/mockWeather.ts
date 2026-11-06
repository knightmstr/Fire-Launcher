export interface WeatherCityInfo {
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
}

export const WEATHER_CITIES: WeatherCityInfo[] = [
  {
    city: 'Seattle',
    country: 'WA, USA',
    tempF: 64,
    tempC: 18,
    condition: 'Partly Cloudy',
    icon: 'cloud-sun',
    highF: 68,
    lowF: 52,
    highC: 20,
    lowC: 11,
    humidity: 71,
    windMph: 8
  },
  {
    city: 'New York',
    country: 'NY, USA',
    tempF: 72,
    tempC: 22,
    condition: 'Sunny',
    icon: 'sun',
    highF: 76,
    lowF: 61,
    highC: 24,
    lowC: 16,
    humidity: 55,
    windMph: 11
  },
  {
    city: 'Los Angeles',
    country: 'CA, USA',
    tempF: 78,
    tempC: 26,
    condition: 'Clear Sky',
    icon: 'sun',
    highF: 82,
    lowF: 65,
    highC: 28,
    lowC: 18,
    humidity: 48,
    windMph: 6
  },
  {
    city: 'Chicago',
    country: 'IL, USA',
    tempF: 61,
    tempC: 16,
    condition: 'Breezy & Cloudy',
    icon: 'cloud',
    highF: 65,
    lowF: 48,
    highC: 18,
    lowC: 9,
    humidity: 62,
    windMph: 18
  },
  {
    city: 'London',
    country: 'UK',
    tempF: 59,
    tempC: 15,
    condition: 'Light Rain',
    icon: 'rain',
    highF: 63,
    lowF: 50,
    highC: 17,
    lowC: 10,
    humidity: 82,
    windMph: 12
  },
  {
    city: 'Tokyo',
    country: 'Japan',
    tempF: 66,
    tempC: 19,
    condition: 'Clear',
    icon: 'sun',
    highF: 70,
    lowF: 54,
    highC: 21,
    lowC: 12,
    humidity: 50,
    windMph: 7
  },
  {
    city: 'Paris',
    country: 'France',
    tempF: 63,
    tempC: 17,
    condition: 'Scattered Clouds',
    icon: 'cloud-sun',
    highF: 68,
    lowF: 52,
    highC: 20,
    lowC: 11,
    humidity: 65,
    windMph: 9
  },
  {
    city: 'Berlin',
    country: 'Germany',
    tempF: 57,
    tempC: 14,
    condition: 'Overcast',
    icon: 'cloud',
    highF: 61,
    lowF: 46,
    highC: 16,
    lowC: 8,
    humidity: 74,
    windMph: 10
  },
  {
    city: 'Sydney',
    country: 'Australia',
    tempF: 74,
    tempC: 23,
    condition: 'Sunny',
    icon: 'sun',
    highF: 79,
    lowF: 62,
    highC: 26,
    lowC: 17,
    humidity: 58,
    windMph: 14
  },
  {
    city: 'Miami',
    country: 'FL, USA',
    tempF: 84,
    tempC: 29,
    condition: 'Passing Showers',
    icon: 'rain',
    highF: 88,
    lowF: 76,
    highC: 31,
    lowC: 24,
    humidity: 78,
    windMph: 13
  },
  {
    city: 'Denver',
    country: 'CO, USA',
    tempF: 65,
    tempC: 18,
    condition: 'Sunny',
    icon: 'sun',
    highF: 71,
    lowF: 45,
    highC: 22,
    lowC: 7,
    humidity: 32,
    windMph: 8
  }
];
