package com.swarmnyc.watchfaces.esther;

import android.app.Application;
import android.test.ApplicationTestCase;

import com.swarmnyc.watchfaces.esther.IWeatherApi;
import com.swarmnyc.watchfaces.esther.openweather.OpenWeatherApi;
import com.swarmnyc.watchfaces.esther.WeatherInfo;

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