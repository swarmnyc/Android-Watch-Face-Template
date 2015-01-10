package com.swarmnyc.watchfaces;

import com.swarmnyc.watchfaces.weather.ISimpleWeatherApi;
import com.swarmnyc.watchfaces.weather.openweather.OpenWeatherApi;
import com.swarmnyc.watchfaces.weather.WeatherInfo;

import junit.framework.TestCase;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class SimpleWeatherApiTest extends TestCase {
    public SimpleWeatherApiTest() {

    }

    public void testGetWeatherData() {

        ISimpleWeatherApi api = new OpenWeatherApi();
        WeatherInfo info = api.getCurrentWeatherInfo(40.743273f,-73.991268f,true);

        assertNotNull(info);
        assertEquals("New York", info.getCityName());
    }
}