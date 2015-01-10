package com.swarmnyc.watchfaces.weather;

public interface ISimpleWeatherApi {
    WeatherInfo getCurrentWeatherInfo(float lon, float lat, boolean isFahrenheit);
}
