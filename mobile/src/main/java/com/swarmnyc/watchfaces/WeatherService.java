package com.swarmnyc.watchfaces;


import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.swarmnyc.watchfaces.weather.ISimpleWeatherApi;
import com.swarmnyc.watchfaces.weather.WeatherInfo;
import com.swarmnyc.watchfaces.weather.openweather.OpenWeatherApi;

import java.util.Timer;
import java.util.TimerTask;

public class WeatherService extends WearableListenerService
        implements LocationListener {
// ------------------------------ FIELDS ------------------------------

    public static final String PATH_WEATHER_INFO = "/WeatherWatchFace/WeatherInfo";
    private static final String TAG = "WeatherService";
    private GoogleApiClient mGoogleApiClient;
    private LocationManager mLocationManager;
    private Location mLocation;
    private String mPeerId;
    private Timer mTimer;

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface LocationListener ---------------------

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: " + location);
        mLocation = location;

        if (mTimer == null) {
            int interval = WeatherService.this.getResources().getInteger(R.integer.WeatherServiceInterval);
            Log.d("TAG","Task Interval:" + interval);
            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(new Task(), 0, interval);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "onLocationStatusChanged: " + provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "onLocationProviderEnabled: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "onLocationProviderDisabled: " + provider);
    }

// --------------------- Interface MessageListener ---------------------

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.d(TAG, "MessageReceived: " + messageEvent.getPath());
        /*if (messageEvent.getPath().equals("/WeatherService/Start")) {
            mRunner.sendEmptyMessage(0);
        }*/
    }

// --------------------- Interface NodeListener ---------------------

    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);

        Log.d(TAG, "Connected: " + peer.getId());
        mPeerId = peer.getId();
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        Log.d(TAG, "Disconnected");

        if (mLocationManager != null) {
            mLocationManager.removeUpdates(this);
        }

        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }

        super.onPeerDisconnected(peer);
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        mLocationManager = (LocationManager) WeatherService.this.getSystemService(Context.LOCATION_SERVICE);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
    }

// -------------------------- INNER CLASSES --------------------------

    private class Task extends TimerTask {
// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface Runnable ---------------------

        @Override
        public void run() {
            try {
                Log.d(TAG, "Task Running");

                if (mLocation == null) {
                    Log.d(TAG, "Task : No location");
                } else {
                    if (!mGoogleApiClient.isConnected())
                        mGoogleApiClient.connect();

                    ISimpleWeatherApi api = new OpenWeatherApi();
                    api.setContext(WeatherService.this.getApplicationContext());

                    DataMap config = new DataMap();
                    WeatherInfo info = api.getCurrentWeatherInfo(mLocation.getLatitude(), mLocation.getLongitude(), true);

                    //real
                    config.putInt("Temperature", info.getTemperature());
                    config.putString("Condition", info.getCondition());

                    //test
                    //Random random = new Random();
                    //config.putInt("Temperature",random.nextInt(100));
                    //config.putString("Condition", new String[]{"clear","rain","snow","thunder","cloudy"}[random.nextInt(4)]);

                    Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, PATH_WEATHER_INFO, config.toByteArray()).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.d(TAG, "sendUpdateMessage: " + sendMessageResult.getStatus());
                        }
                    });
                }
            } catch (Exception e) {
                Log.d(TAG, "Task Fail:" + e);
            }
        }
    }
}

