package com.swarmnyc.watchfaces;

import android.app.Application;
import android.content.Context;
import android.test.ApplicationTestCase;

import com.swarmnyc.watchfaces.weather.IWeatherApi;
import com.swarmnyc.watchfaces.weather.openweather.OpenWeatherApi;
import com.swarmnyc.watchfaces.weather.WeatherInfo;

import junit.framework.TestCase;

public class WeatherApiTest extends ApplicationTestCase<Application> {
    public WeatherApiTest() {
        super(Application.class);
    }

    public void testGetWeatherData() {

        IWeatherApi api = new OpenWeatherApi();
        api.setContext(this.getContext());
        WeatherInfo info = api.getCurrentWeatherInfo(40.71, -74.01);

        assertNotNull(info);
    }
}