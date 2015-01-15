package com.swarmnyc.watchfaces.weather.openweather;

public class OpenWeatherLocation {
// ------------------------------ FIELDS ------------------------------

    private long sunrise;

    public long getSunrise() {
        return sunrise;
    }

    public void setSunrise(long sunrise) {
        this.sunrise = sunrise;
    }

    private long sunset;

    public long getSunset() {
        return sunset;
    }

    public void setSunset(long sunset) {
        this.sunset = sunset;
    }
}
