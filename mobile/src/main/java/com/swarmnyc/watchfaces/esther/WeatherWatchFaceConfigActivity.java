package com.swarmnyc.watchfaces.esther;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.wearable.companion.WatchFaceCompanion;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;

import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

@ContentView(com.swarmnyc.watchfaces.esther.R.layout.activity_weather_watch_face_config)
public class WeatherWatchFaceConfigActivity extends RoboActivity {
// ------------------------------ FIELDS ------------------------------

    public static final String KEY_CONFIG_REQUIRE_INTERVAL = "RequireInterval";
    public static final String KEY_CONFIG_TEMPERATURE_SCALE = "TemperatureScale";
    public static final String KEY_CONFIG_THEME = "Theme";
    public static final String KEY_CONFIG_TIMEUNIT = "TimeUnit";
    public static final String PATH_CONFIG = "/WeatherWatchFace/Config";
    private static final String TAG = "WeatherWatchFaceConfigActivity";
    private static final int TIMEUNIT12 = 0;
    private static final int TIMEUNIT24 = 1;
    
    ResultCallback<DataApi.DataItemResult> getDataCallback = new ResultCallback<DataApi.DataItemResult>() {
    @Override
    public void onResult(DataApi.DataItemResult result) {
        if (result.getStatus().isSuccess() && result.getDataItem() != null) {
            DataMap item = DataMapItem.fromDataItem(result.getDataItem()).getDataMap();



            if (item.containsKey(KEY_CONFIG_TIMEUNIT)) {
                mTimeUnit = item.getInt(KEY_CONFIG_THEME);
                mTimeUnitSwitch.setChecked(mTimeUnit == TIMEUNIT12);
            }


        }

        mTimeUnitSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mTimeUnit = isChecked ? TIMEUNIT12 : TIMEUNIT24;
                DataMap config = new DataMap();
                config.putInt(KEY_CONFIG_TIMEUNIT, mTimeUnit);
                sendConfigUpdateMessage(config);
            }
        });





        }
    };



    private GoogleApiClient mGoogleApiClient;




    private Spinner mIntervalSpinner;
    private String mPeerId;

    @InjectView(com.swarmnyc.watchfaces.esther.R.id.switch_time_unit)
    private Switch mTimeUnitSwitch;




    @InjectView(com.swarmnyc.watchfaces.esther.R.id.view_logo)
    private View mLogo;

    private int mTheme = 3;
    private int mTimeUnit = TIMEUNIT12;

// -------------------------- OTHER METHODS --------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPeerId = getIntent().getStringExtra(WatchFaceCompanion.EXTRA_PEER_ID);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        Uri uri = new Uri.Builder()
                .scheme("wear")
                .path(PATH_CONFIG)
                .authority(mPeerId)
                .build();

        Wearable.DataApi.getDataItem(mGoogleApiClient, uri)
                .setResultCallback(getDataCallback);



        mLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(getResources().getString(com.swarmnyc.watchfaces.esther.R.string.company_url)));
                startActivity(intent);
            }
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
