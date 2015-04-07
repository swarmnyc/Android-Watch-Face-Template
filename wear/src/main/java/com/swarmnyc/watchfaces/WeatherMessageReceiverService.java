package com.swarmnyc.watchfaces;


import android.location.Location;
import android.location.LocationManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class WeatherMessageReceiverService extends WearableListenerService {
    private static final String TAG = WeatherMessageReceiverService.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();

        DataMap dataMap = DataMap.fromByteArray(messageEvent.getData());
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Consts.PATH_CONFIG);
        DataMap config = putDataMapRequest.getDataMap();

        if (messageEvent.getPath().equals(Consts.PATH_WEATHER_INFO)){
            config.putLong(Consts.KEY_WEATHER_UPDATE_TIME, System.currentTimeMillis());

            if (dataMap.containsKey(Consts.KEY_WEATHER_CONDITION)) {
                config.putString(Consts.KEY_WEATHER_CONDITION, dataMap.getString(Consts.KEY_WEATHER_CONDITION));
            }

            if (dataMap.containsKey(Consts.KEY_WEATHER_TEMPERATURE)) {
                config.putInt(Consts.KEY_WEATHER_TEMPERATURE, dataMap.getInt(Consts.KEY_WEATHER_TEMPERATURE));
            }

            if (dataMap.containsKey(Consts.KEY_WEATHER_SUNRISE)) {
                config.putLong(Consts.KEY_WEATHER_SUNRISE, dataMap.getLong(Consts.KEY_WEATHER_SUNRISE));
            }

            if (dataMap.containsKey(Consts.KEY_WEATHER_SUNSET)) {
                config.putLong(Consts.KEY_WEATHER_SUNSET, dataMap.getLong(Consts.KEY_WEATHER_SUNSET));
            }
        }

        if (messageEvent.getPath().equals(Consts.PATH_CONFIG)){
            if (dataMap.containsKey(Consts.KEY_CONFIG_TEMPERATURE_SCALE)) {
                config.putInt(Consts.KEY_CONFIG_TEMPERATURE_SCALE, dataMap.getInt(Consts.KEY_CONFIG_TEMPERATURE_SCALE));
            }

            if (dataMap.containsKey(Consts.KEY_CONFIG_THEME)) {
                config.putInt(Consts.KEY_CONFIG_THEME, dataMap.getInt(Consts.KEY_CONFIG_THEME));
            }

            if (dataMap.containsKey(Consts.KEY_CONFIG_TIME_UNIT)) {
                config.putInt(Consts.KEY_CONFIG_TIME_UNIT, dataMap.getInt(Consts.KEY_CONFIG_TIME_UNIT));
            }

            if (dataMap.containsKey(Consts.KEY_CONFIG_REQUIRE_INTERVAL)) {
                config.putInt(Consts.KEY_CONFIG_REQUIRE_INTERVAL, dataMap.getInt(Consts.KEY_CONFIG_REQUIRE_INTERVAL));
            }
        }

        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataMapRequest.asPutDataRequest())
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        Log.d(TAG, "SaveConfig: " + dataItemResult.getStatus() + ", " + dataItemResult.getDataItem().getUri());

                        mGoogleApiClient.disconnect();
                    }
                });
    }
}

