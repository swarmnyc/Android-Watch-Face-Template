package com.swarmnyc.watchfaces;

import android.net.Uri;
import android.os.Bundle;
import android.support.wearable.companion.WatchFaceCompanion;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;
import com.google.inject.Inject;
import com.swarmnyc.watchfaces.weather.IWeatherApi;

import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

@ContentView(R.layout.activity_weather_watch_face_config)
public class WeatherWatchFaceConfigActivity extends RoboActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<DataApi.DataItemResult> {
// ------------------------------ FIELDS ------------------------------

    public static final String PATH_CONFIG = "/WeatherWatchFace/Config";
    public static final String KEY_CONFIG_THEME = "THEME";
    public static final String KEY_CONFIG_TEMPERATURE_SCALE = "TemperatureScale";
    public static final String KEY_CONFIG_REQUIRE_INTERVAL = "RequireInterval";
    private static final String TAG = "WeatherWatchFaceConfigActivity";
    private GoogleApiClient mGoogleApiClient;

    @InjectView(R.id.intervalSpinner)
    private Spinner mIntervalSpinner;

    @InjectView(R.id.scaleRadioGroup)
    private RadioGroup mScaleRadioGroup;

    private int mTheme = 3;
    private String mPeerId;

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ConnectionCallbacks ---------------------

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

// --------------------- Interface OnConnectionFailedListener ---------------------

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

// --------------------- Interface ResultCallback ---------------------


    @Override
    public void onResult(DataApi.DataItemResult result) {
        if (result.getStatus().isSuccess() && result.getDataItem() != null) {
            DataMap item = DataMapItem.fromDataItem(result.getDataItem()).getDataMap();
            if (item.containsKey(KEY_CONFIG_TEMPERATURE_SCALE)) {
                if (item.getInt(KEY_CONFIG_TEMPERATURE_SCALE) == 1) {
                    mScaleRadioGroup.check(R.id.celsiusRadioButton);
                } else {
                    mScaleRadioGroup.check(R.id.fahrenheitRadioButton);
                }
            }

            if (item.containsKey(KEY_CONFIG_THEME)) {
                mTheme=item.getInt(KEY_CONFIG_TEMPERATURE_SCALE);
            }

            

//            if (item.containsKey(KEY_CONFIG_BACKGROUND_COLOR)) {
//                int color = item.getInt(KEY_CONFIG_BACKGROUND_COLOR);
//                String[] names = getResources().getStringArray(R.array.color_array);
//                for (int i = 0; i < names.length; i++) {
//                    if (Color.parseColor(names[i]) == color) {
//                        mBackgroundColorSpinner.setSelection(i);
//                        break;
//                    }
//                }
//            }

            if (item.containsKey(KEY_CONFIG_REQUIRE_INTERVAL)) {
                int interval = item.getInt(KEY_CONFIG_REQUIRE_INTERVAL);
                String[] names = getResources().getStringArray(R.array.interval_array);
                for (int i = 0; i < names.length; i++) {
                    if (convertTimeStringToInt(names[i]) == interval) {
                        mIntervalSpinner.setSelection(i);
                        break;
                    }
                }
            }
        }

        mScaleRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                DataMap config = new DataMap();
                config.putInt(KEY_CONFIG_TEMPERATURE_SCALE, checkedId == R.id.fahrenheitRadioButton ? 0 : 1);
                sendConfigUpdateMessage(config);
            }
        });

//        mBackgroundColorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
//                String colorName = (String) adapterView.getItemAtPosition(position);
//                DataMap map = new DataMap();
//                map.putInt(KEY_CONFIG_BACKGROUND_COLOR, Color.parseColor(colorName));
//                sendConfigUpdateMessage(map);
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//            }
//        });

        mIntervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                int interval = convertTimeStringToInt((String) adapterView.getItemAtPosition(position));

                if (interval != 0) {
                    DataMap map = new DataMap();
                    map.putInt(KEY_CONFIG_REQUIRE_INTERVAL, interval);
                    sendConfigUpdateMessage(map);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

// -------------------------- OTHER METHODS --------------------------

    private int convertTimeStringToInt(String string) {
        int interval;
        String[] option = string.split(" ");

        if (option[1].startsWith("hour")) {
            interval = Integer.parseInt(option[0]) * (int) DateUtils.HOUR_IN_MILLIS;
        } else if (option[1].startsWith("min")) {
            interval = Integer.parseInt(option[0]) * (int) DateUtils.MINUTE_IN_MILLIS;
        } else if (option[1].startsWith("sec")) {
            interval = Integer.parseInt(option[0]) * (int) DateUtils.SECOND_IN_MILLIS;
        } else {
            interval = 0;
        }

        return interval;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_watch_face_config);

        mPeerId = getIntent().getStringExtra(WatchFaceCompanion.EXTRA_PEER_ID);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        Uri uri = new Uri.Builder()
                .scheme("wear")
                .path(PATH_CONFIG)
                .authority(mPeerId)
                .build();

        Wearable.DataApi.getDataItem(mGoogleApiClient, uri)
                .setResultCallback(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    private void sendConfigUpdateMessage(DataMap config) {
        if (mPeerId != null) {
            Log.d(TAG, "Sending Config: " + config);
            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, PATH_CONFIG, config.toByteArray())
                    .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                    Log.d(TAG, "Send Config Result: " + sendMessageResult.getStatus());
                }
            });
        }
    }
}
