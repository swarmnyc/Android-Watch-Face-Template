package com.swarmnyc.watchfaces;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.wearable.companion.WatchFaceCompanion;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;


public class WeatherWatchFaceConfigActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
// ------------------------------ FIELDS ------------------------------
    public static final String PATH_CONFIG = "/WeatherWatchFace/Config";
    public static final String CONFIG_KEY_BACKGROUND_COLOR = "BackgroundColor";
    public static final String CONFIG_KEY_TEMPERATURE_SCALE = "TemperatureScale";
    private static final String TAG = "WeatherWatchFaceConfigActivity";
    private GoogleApiClient mGoogleApiClient;
    private RadioGroup mScaleRadioGroup;
    private Spinner mColorSpinner;
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

// -------------------------- OTHER METHODS --------------------------

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

        mScaleRadioGroup = (RadioGroup) findViewById(R.id.scaleRadioGroup);

        mScaleRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                DataMap config = new DataMap();
                config.putInt(CONFIG_KEY_TEMPERATURE_SCALE, checkedId == R.id.fahrenheitRadioButton ? 0 : 1);
                sendConfigUpdateMessage(config);
            }
        });

        mColorSpinner = (Spinner)findViewById(R.id.colorSpinner);
        mColorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String colorName = (String) adapterView.getItemAtPosition(position);
                DataMap map = new DataMap();
                map.putInt(CONFIG_KEY_BACKGROUND_COLOR,Color.parseColor(colorName));
                sendConfigUpdateMessage(map);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
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
            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, PATH_CONFIG, config.toByteArray()).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                    Log.d(TAG, "Send Config Result: " + sendMessageResult.getStatus());
                }
            });
        }
    }
}
