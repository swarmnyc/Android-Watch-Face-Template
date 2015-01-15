package com.swarmnyc.watchfaces.weather.openweather;

public class OpenWeatherQueryResult {
// ------------------------------ FIELDS ------------------------------

    private OpenWeatherData[] weather;

    public OpenWeatherData[] getWeather() {
        return weather;
    }

    public void setWeather(OpenWeatherData[] weather) {
        this.weather = weather;
    }

    private OpenWeatherLocation sys;

    public OpenWeatherLocation getSys() {
        return sys;
    }

    public void setSys(OpenWeatherLocation sys) {
        this.sys = sys;
    }

    private OpenWeatherTemperature main;

    public OpenWeatherTemperature getMain() {
        return main;
    }

    public void setMain(OpenWeatherTemperature main) {
        this.main = main;
    }

    private String base;

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    private String cod;

    public String getCod() {
        return cod;
    }

    public void setCod(String cod) {
        this.cod = cod;
    }

    private String dt;

    public String getDt() {
        return dt;
    }

    public void setDt(String dt) {
        this.dt = dt;
    }

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
