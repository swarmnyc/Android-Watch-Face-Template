package com.swarmnyc.watchfaces;

import android.app.Application;
import android.content.Context;
import android.test.ApplicationTestCase;

import com.swarmnyc.watchfaces.weather.ISimpleWeatherApi;
import com.swarmnyc.watchfaces.weather.openweather.OpenWeatherApi;
import com.swarmnyc.watchfaces.weather.WeatherInfo;

import junit.framework.TestCase;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class SimpleWeatherApiTest extends ApplicationTestCase<Application> {
    public SimpleWeatherApiTest() {
        super(Application.class);
    }

    public void testGetWeatherData() {

        ISimpleWeatherApi api = new OpenWeatherApi();
        api.setContext(this.getContext());
        WeatherInfo info = api.getCurrentWeatherInfo(40.71,-74.01,true);

        assertNotNull(info);
    }
}