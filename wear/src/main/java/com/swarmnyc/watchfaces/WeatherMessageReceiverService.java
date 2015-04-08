package com.swarmnyc.watchfaces;


import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class WeatherMessageReceiverService extends WearableListenerService {
    private static final String TAG = WeatherMessageReceiverService.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;
    private static String condition;
    private static int temperature;
    private static long sunrise;
    private static long sunset;
    private static int temperature_scale;
    private static int theme;
    private static int time_unit;
    private static int interval;
    private static boolean alreadyInitilize;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        if (!alreadyInitilize) {
            if (!mGoogleApiClient.isConnected())
                mGoogleApiClient.connect();

            Wearable.NodeApi.getLocalNode(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetLocalNodeResult>() {
                @Override
                public void onResult(NodeApi.GetLocalNodeResult getLocalNodeResult) {
                    Uri uri = new Uri.Builder()
                            .scheme("wear")
                            .path(Consts.PATH_CONFIG)
                            .authority(getLocalNodeResult.getNode().getId())
                            .build();

                    Wearable.DataApi.getDataItem(mGoogleApiClient, uri)
                            .setResultCallback(
                                    new ResultCallback<DataApi.DataItemResult>() {
                                        @Override
                                        public void onResult(DataApi.DataItemResult dataItemResult) {
                                            if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
                                                fetchConfig(DataMapItem.fromDataItem(dataItemResult.getDataItem()).getDataMap());
                                            }
                                        }
                                    }
                            );
                }
            });
        }

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        while (!alreadyInitilize){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();

        DataMap dataMap = DataMap.fromByteArray(messageEvent.getData());

        if (messageEvent.getPath().equals(Consts.PATH_WEATHER_INFO)) {
            if (dataMap.containsKey(Consts.KEY_WEATHER_CONDITION)) {
                condition = dataMap.getString(Consts.KEY_WEATHER_CONDITION);
            }

            if (dataMap.containsKey(Consts.KEY_WEATHER_TEMPERATURE)) {
                temperature = dataMap.getInt(Consts.KEY_WEATHER_TEMPERATURE);
            }

            if (dataMap.containsKey(Consts.KEY_WEATHER_SUNRISE)) {
                sunrise = dataMap.getLong(Consts.KEY_WEATHER_SUNRISE);
            }

            if (dataMap.containsKey(Consts.KEY_WEATHER_SUNSET)) {
                sunset = dataMap.getLong(Consts.KEY_WEATHER_SUNSET);
            }
        }

        if (messageEvent.getPath().equals(Consts.PATH_CONFIG)) {
            if (dataMap.containsKey(Consts.KEY_CONFIG_TEMPERATURE_SCALE)) {
                temperature_scale = dataMap.getInt(Consts.KEY_CONFIG_TEMPERATURE_SCALE);
            }

            if (dataMap.containsKey(Consts.KEY_CONFIG_THEME)) {
                theme = dataMap.getInt(Consts.KEY_CONFIG_THEME);
            }

            if (dataMap.containsKey(Consts.KEY_CONFIG_TIME_UNIT)) {
                time_unit = dataMap.getInt(Consts.KEY_CONFIG_TIME_UNIT);
            }

            if (dataMap.containsKey(Consts.KEY_CONFIG_REQUIRE_INTERVAL)) {
                interval = dataMap.getInt(Consts.KEY_CONFIG_REQUIRE_INTERVAL);
            }
        }

        saveConfig();
    }

    private void saveConfig() {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Consts.PATH_CONFIG);

        DataMap config = putDataMapRequest.getDataMap();
        config.putLong(Consts.KEY_WEATHER_UPDATE_TIME, System.currentTimeMillis());
        config.putString(Consts.KEY_WEATHER_CONDITION, condition);
        config.putInt(Consts.KEY_WEATHER_TEMPERATURE, temperature);
        config.putLong(Consts.KEY_WEATHER_SUNRISE, sunrise);
        config.putLong(Consts.KEY_WEATHER_SUNSET, sunset);
        config.putInt(Consts.KEY_CONFIG_TEMPERATURE_SCALE, temperature_scale);
        config.putInt(Consts.KEY_CONFIG_THEME, theme);
        config.putInt(Consts.KEY_CONFIG_TIME_UNIT, time_unit);
        config.putInt(Consts.KEY_CONFIG_REQUIRE_INTERVAL, interval);

        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataMapRequest.asPutDataRequest())
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        Log.d(TAG, "SaveConfig: " + dataItemResult.getStatus() + ", " + dataItemResult.getDataItem().getUri());

                        mGoogleApiClient.disconnect();
                    }
                });
    }

    protected void fetchConfig(DataMap config) {
        if (config.containsKey(Consts.KEY_WEATHER_CONDITION)) {
            condition = config.getString(Consts.KEY_WEATHER_CONDITION);
        }

        if (config.containsKey(Consts.KEY_WEATHER_TEMPERATURE)) {
            temperature = config.getInt(Consts.KEY_WEATHER_TEMPERATURE);
        }

        if (config.containsKey(Consts.KEY_WEATHER_SUNRISE)) {
            sunrise = config.getLong(Consts.KEY_WEATHER_SUNRISE);
        }

        if (config.containsKey(Consts.KEY_WEATHER_SUNSET)) {
            sunset = config.getLong(Consts.KEY_WEATHER_SUNSET);
        }

        if (config.containsKey(Consts.KEY_CONFIG_TEMPERATURE_SCALE)) {
            temperature_scale = config.getInt(Consts.KEY_CONFIG_TEMPERATURE_SCALE);
        }

        if (config.containsKey(Consts.KEY_CONFIG_THEME)) {
            theme = config.getInt(Consts.KEY_CONFIG_THEME);
        }

        if (config.containsKey(Consts.KEY_CONFIG_TIME_UNIT)) {
            time_unit = config.getInt(Consts.KEY_CONFIG_TIME_UNIT);
        }

        if (config.containsKey(Consts.KEY_CONFIG_REQUIRE_INTERVAL)) {
            interval = config.getInt(Consts.KEY_CONFIG_REQUIRE_INTERVAL);
        }

        alreadyInitilize = true;
    }
}

