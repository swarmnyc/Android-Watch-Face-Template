package com.swarmnyc.watchfaces.weather;

import android.content.Context;

public interface ISimpleWeatherApi {
    WeatherInfo getCurrentWeatherInfo(double lon, double lat);

    void setContext(Context context);
}
