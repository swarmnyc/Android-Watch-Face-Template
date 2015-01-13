package com.swarmnyc.watchfaces;


import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.swarmnyc.watchfaces.weather.ISimpleWeatherApi;
import com.swarmnyc.watchfaces.weather.WeatherInfo;
import com.swarmnyc.watchfaces.weather.openweather.OpenWeatherApi;

public class WeatherService extends WearableListenerService {
    private static final String TAG = "WeatherService";
    private GoogleApiClient mGoogleApiClient;
    private String mPeerId;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
    }

    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);
        Log.d(TAG, "Connected");

        mPeerId = peer.getId();

        Wearable.DataApi.addListener(mGoogleApiClient, this);

        if (!mGoogleApiClient.isConnected()){
            mGoogleApiClient.connect();
        }

        //TODO Make reload once a hour, and better code
        //start weather
        ISimpleWeatherApi api = new OpenWeatherApi();
        api.setContext(this.getApplicationContext());
        WeatherInfo info = api.getCurrentWeatherInfo(40.71, -74.01, true);

        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/WeatherWatchFace");
        DataMap config = putDataMapRequest.getDataMap();

        config.putInt("Temperature", info.getTemperature());
        config.putString("Condition", info.getCondition());

        try {
            Wearable.DataApi.putDataItem(mGoogleApiClient, putDataMapRequest.asPutDataRequest())
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            Log.d(TAG, "putDataItem Uri: " + dataItemResult.getDataItem().getUri());
                            Log.d(TAG, "putDataItem result status: " + dataItemResult.getStatus());
                        }
                    });
        } catch (Exception e) {
            Log.d(TAG, "Fail:" + e);
        }
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        Log.d(TAG, "Disconnected");

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
        }

        super.onPeerDisconnected(peer);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);
        Log.d(TAG, "DataChanged:" + dataEvents.getStatus());
    }
}

