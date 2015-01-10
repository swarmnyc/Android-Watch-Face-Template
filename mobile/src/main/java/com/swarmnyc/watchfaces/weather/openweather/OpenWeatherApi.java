package com.swarmnyc.watchfaces.weather.openweather;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swarmnyc.watchfaces.weather.ISimpleWeatherApi;
import com.swarmnyc.watchfaces.weather.WeatherInfo;

import java.net.URL;

public class OpenWeatherApi implements ISimpleWeatherApi {
    private static final String APIURL = "http://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&units=%s";
    private static final String FAHRENHEIT = "imperial";
    private static final String CELSIUS = "metric";

    @Override
    public WeatherInfo getCurrentWeatherInfo(float lat, float lon, boolean isFahrenheit) {
        WeatherInfo w = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            String url = String.format(APIURL, lat, lon, isFahrenheit ? FAHRENHEIT : CELSIUS);
            OpenWeatherQueryResult result = mapper.readValue(new URL(url), OpenWeatherQueryResult.class);

            if ("200".equals(result.getCod())) {
                w = new WeatherInfo();
                w.setCityName(result.getName());
                w.setIsFahrenheit(isFahrenheit);
                w.setTemperature((int) result.getMain().getTemp());
                OpenWeatherData[] datas = result.getWeather();
                if (datas != null && datas.length > 0) {
                    w.setCondition(datas[0].getMain());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return w;
    }
}
