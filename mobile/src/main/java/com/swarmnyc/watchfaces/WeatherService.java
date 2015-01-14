package com.swarmnyc.watchfaces;


import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.swarmnyc.watchfaces.weather.ISimpleWeatherApi;
import com.swarmnyc.watchfaces.weather.openweather.OpenWeatherApi;

import java.util.Random;

public class WeatherService extends WearableListenerService {
    private static final String TAG = "WeatherService";
    private GoogleApiClient mGoogleApiClient;
    private String mPeerId;
    private Handler mRunner = new Runner();


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.d(TAG, "MessageReceived: " + messageEvent.getPath());
        if (messageEvent.getPath().equals("/WeatherService/Start")) {
            mRunner.sendEmptyMessage(0);
        }
    }

    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);

        mPeerId = peer.getId();
        Log.d(TAG, "Connected: " + mPeerId);

       /* //Wearable.DataApi.addListener(mGoogleApiClient, this);

        if (!mGoogleApiClient.isConnected()){
            Log.d(TAG,"Start Connect");
            mGoogleApiClient.connect();
        }*/
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        Log.d(TAG, "Disconnected");

        /*if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.d(TAG,"Start Disconnect");
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }*/

        if (mRunner != null) {
            mRunner.removeMessages(0);
        }

        super.onPeerDisconnected(peer);
    }

    private class Runner extends Handler {

        @Override
        public void handleMessage(Message msg) {
            //super.handleMessage(msg);
            Log.d(TAG, "Running: " + msg);

            if (msg.what!=0)
                return;


            try {
                if (!mGoogleApiClient.isConnected())
                    mGoogleApiClient.connect();

                ISimpleWeatherApi api = new OpenWeatherApi();
                api.setContext(WeatherService.this.getApplicationContext());

                //TODO: GET GeoInfo
                //WeatherInfo info = api.getCurrentWeatherInfo(40.71, -74.01, true);

                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/WeatherWatchFace");
                DataMap config = putDataMapRequest.getDataMap();

                //real
                //config.putInt("Temperature", info.getTemperature());
                //config.putString("Condition", info.getCondition());

                //test
                Random random = new Random();
                config.putInt("Temperature",random.nextInt(100));
                config.putString("Condition", new String[]{"clear","rain","snow","thunder","cloudy"}[random.nextInt(4)]);


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

            //real
            //this.sendEmptyMessageDelayed(0, 60 * 60 * 1000);

            //test
            this.sendEmptyMessageDelayed(0, 10000);
        }
    }
}

