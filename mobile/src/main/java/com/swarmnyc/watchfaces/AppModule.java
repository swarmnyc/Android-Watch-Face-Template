package com.swarmnyc.watchfaces;

import android.content.Context;

import com.google.inject.AbstractModule;
import com.swarmnyc.watchfaces.weather.IWeatherApi;
import com.swarmnyc.watchfaces.weather.openweather.OpenWeatherApi;

public class AppModule extends AbstractModule {
    //private Context mContext;

    /*public AppModule(Context context) {
        this.mContext = context;
    }*/

    @Override
    protected void configure() {
        bind(IWeatherApi.class).toInstance(new OpenWeatherApi());
    }
}
