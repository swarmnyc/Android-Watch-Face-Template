package com.swarmnyc.watchfaces.esther;

public interface IWeatherApi {
    WeatherInfo getCurrentWeatherInfo(double lon, double lat);
}
