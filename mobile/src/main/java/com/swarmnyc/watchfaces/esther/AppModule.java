package com.swarmnyc.watchfaces.esther;

import com.google.inject.AbstractModule;
import com.swarmnyc.watchfaces.esther.IWeatherApi;
import com.swarmnyc.watchfaces.esther.openweather.OpenWeatherApi;

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
