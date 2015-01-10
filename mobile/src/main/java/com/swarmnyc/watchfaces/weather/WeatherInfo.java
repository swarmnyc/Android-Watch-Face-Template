package com.swarmnyc.watchfaces.weather;

public class WeatherInfo {
    private String cityName;
    private boolean isFahrenheit;

    public String getCityName() {
        return cityName;
    }

    public boolean isFahrenheit() {
        return isFahrenheit;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    private String condition;

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    private int temperature;

    public int getTemperature() {
        return temperature;
    }

    public void setFahrenheit(boolean isFahrenheit) {
        this.isFahrenheit = isFahrenheit;
    }

    public void setIsFahrenheit(boolean isFahrenheit) {
        this.isFahrenheit = isFahrenheit;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }
}
