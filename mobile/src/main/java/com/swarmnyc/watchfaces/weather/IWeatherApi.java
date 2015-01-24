package com.swarmnyc.watchfaces.weather;

import android.content.Context;

public interface IWeatherApi {
    WeatherInfo getCurrentWeatherInfo(double lon, double lat);
}
