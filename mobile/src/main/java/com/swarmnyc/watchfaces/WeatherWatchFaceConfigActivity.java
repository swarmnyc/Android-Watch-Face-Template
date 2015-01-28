package com.swarmnyc.watchfaces;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.wearable.companion.WatchFaceCompanion;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
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

@ContentView(R.layout.activity_weather_watch_face_config)
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
            if (item.containsKey(KEY_CONFIG_TEMPERATURE_SCALE)) {
                if (item.getInt(KEY_CONFIG_TEMPERATURE_SCALE) == 1) {
                    mScaleRadioGroup.check(R.id.celsiusRadioButton);
                } else {
                    mScaleRadioGroup.check(R.id.fahrenheitRadioButton);
                }
            } else {
                mScaleRadioGroup.check(R.id.fahrenheitRadioButton);
            }

            if (item.containsKey(KEY_CONFIG_THEME)) {
                mTheme = item.getInt(KEY_CONFIG_THEME);
            }

            if (item.containsKey(KEY_CONFIG_TIMEUNIT)) {
                mTimeUnit = item.getInt(KEY_CONFIG_THEME);
                mTimeUnitSwitch.setChecked(mTimeUnit == TIMEUNIT12);
            }

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

        mTimeUnitSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mTimeUnit = isChecked ? TIMEUNIT12 : TIMEUNIT24;
                DataMap config = new DataMap();
                config.putInt(KEY_CONFIG_TIMEUNIT, mTimeUnit);
                sendConfigUpdateMessage(config);
            }
        });

        mScaleRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                DataMap config = new DataMap();
                config.putInt(KEY_CONFIG_TEMPERATURE_SCALE, checkedId == R.id.fahrenheitRadioButton ? 0 : 1);
                sendConfigUpdateMessage(config);
            }
        });

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

        onColorViewClick.onClick(mColorButtonContainer.getChildAt(mTheme - 1));
        }
    };

    View.OnClickListener onColorViewClick =new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mTheme = (int) v.getTag();
            for (int j = 0; j < mColorButtonContainer.getChildCount(); j++) {
                mColorButtonContainer.getChildAt(j).setActivated(false);
            }
            v.setActivated(true);
            changeTheme();
        }
    };

    private GoogleApiClient mGoogleApiClient;

    @InjectView(R.id.preview_image)
    private ImageView mPreviewImage;

    @InjectView(R.id.scaleRadioGroup)
    private RadioGroup mScaleRadioGroup;

    @InjectView(R.id.intervalSpinner)
    private Spinner mIntervalSpinner;
    private String mPeerId;

    @InjectView(R.id.switch_time_unit)
    private Switch mTimeUnitSwitch;

    @InjectView(R.id.colorbutton_container)
    private ViewGroup mColorButtonContainer;

    @InjectView(R.id.view_logo)
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

        initColorButton();

        mLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(getResources().getString(R.string.company_url)));
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

    private void changeTheme() {
        int id = this.getResources().getIdentifier("weather_preview_" + (mTheme), "drawable", WeatherWatchFaceConfigActivity.class.getPackage().getName());
        mPreviewImage.setImageResource(id);

        DataMap dataMap = new DataMap();
        dataMap.putInt(KEY_CONFIG_THEME, mTheme);
        sendConfigUpdateMessage(dataMap);
    }

    private void initColorButton() {
        mColorButtonContainer.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mColorButtonContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        int count = mColorButtonContainer.getChildCount();
                        int size = mColorButtonContainer.getMeasuredWidth() / (count + 1);
                        int margin = size / (count + 1);
                        for (int i = 0; i < count; i++) {
                            View view = mColorButtonContainer.getChildAt(i);
                            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) view.getLayoutParams();
                            lp.width = size;
                            lp.height = size;
                            lp.leftMargin = margin;
                            view.setTag(i + 1);
                            view.setOnClickListener(onColorViewClick);
                        }
                    }
                });
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
