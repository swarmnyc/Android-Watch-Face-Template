package com.swarmnyc.watchfaces.weather.openweather;

public class OpenWeatherTemperature {
// ------------------------------ FIELDS ------------------------------

    private String grnd_level;

    public String getGrnd_level() {
        return grnd_level;
    }

    public void setGrnd_level(String grnd_level) {
        this.grnd_level = grnd_level;
    }

    private String humidity;

    public String getHumidity() {
        return humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    private String pressure;

    public String getPressure() {
        return pressure;
    }

    public void setPressure(String pressure) {
        this.pressure = pressure;
    }

    private String sea_level;

    public String getSea_level() {
        return sea_level;
    }

    public void setSea_level(String sea_level) {
        this.sea_level = sea_level;
    }

    private float temp;

    public float getTemp() {
        return temp;
    }

    public void setTemp(float temp) {
        this.temp = temp;
    }

    private String temp_max;

    public String getTemp_max() {
        return temp_max;
    }

    public void setTemp_max(String temp_max) {
        this.temp_max = temp_max;
    }

    private String temp_min;

    public String getTemp_min() {
        return temp_min;
    }

    public void setTemp_min(String temp_min) {
        this.temp_min = temp_min;
    }
}

