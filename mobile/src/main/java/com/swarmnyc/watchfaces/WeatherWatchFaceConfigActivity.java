package com.swarmnyc.watchfaces;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.wearable.companion.WatchFaceCompanion;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;

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
    private static final String TAG = "WeatherWatchFaceConfig";
    public static final String PATH_CONFIG = "/WeatherWatchFace/Config/";
    private static final int TIMEUNIT12 = 0;
    private static final int TIMEUNIT24 = 1;
    private List<Integer> themes = new ArrayList<>();
    private List<View> themeButtons = new ArrayList<>();

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
                    mTimeUnit = item.getInt(KEY_CONFIG_TIMEUNIT);
                    mTimeUnitSwitch.setChecked(mTimeUnit == TIMEUNIT12);
                }

                if (item.containsKey(KEY_CONFIG_REQUIRE_INTERVAL)) {
                    interval = item.getInt(KEY_CONFIG_REQUIRE_INTERVAL);
                }
            }

            String[] names = getResources().getStringArray(R.array.interval_array);
            for (int i = 0; i < names.length; i++) {
                if (convertTimeStringToInt(names[i]) == interval) {
                    mIntervalSpinner.setSelection(i, false);
                    break;
                }
            }

            onColorViewClick.onClick(themeButtons.get(mTheme - 1));
            alreadyInitialize = true;
            mContainer.setVisibility(View.VISIBLE);
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
        }
    };

    View.OnClickListener onColorViewClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mTheme = (int) v.getTag();
            for (int j = 0; j < themeButtons.size(); j++) {
                themeButtons.get(j).setActivated(false);
            }
            v.setActivated(true);
            changeTheme();
        }
    };

    private GoogleApiClient mGoogleApiClient;

    @InjectView(R.id.container)
    private ViewGroup mContainer;

    @InjectView(R.id.preview_image)
    private ImageView mPreviewImage;

    @InjectView(R.id.scaleRadioGroup)
    private RadioGroup mScaleRadioGroup;

    @InjectView(R.id.intervalSpinner)
    private Spinner mIntervalSpinner;
    private String mPeerId;

    @InjectView(R.id.switch_time_unit)
    private Switch mTimeUnitSwitch;

    @InjectView(R.id.theme_button_container)
    private TableLayout mThemeButtonContainer;

    @InjectView(R.id.view_logo)
    private View mLogo;

    @InjectView(R.id.btn_refresh_button)
    private View mManualUpdateButton;

    private int mTheme = 3;
    private int mTimeUnit = TIMEUNIT12;
    private String mSource;
    private String mPath;
    private int interval;
    private boolean alreadyInitialize;

// -------------------------- OTHER METHODS --------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSource = getIntent().getAction();
        if (mSource.endsWith("SLIM")) {
            mSource = "slim";
            mPath = PATH_CONFIG + "Slim";
        } else {
            mSource = "runner";
            mPath = PATH_CONFIG + "Runner";
        }

        mPeerId = getIntent().getStringExtra(WatchFaceCompanion.EXTRA_PEER_ID);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        int id = this.getResources().getIdentifier(mSource + "_preview", "drawable", WeatherWatchFaceConfigActivity.class.getPackage().getName());
        mPreviewImage.setImageResource(id);

        mLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(getResources().getString(R.string.company_url)));
                startActivity(intent);
            }
        });

        mManualUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WeatherWatchFaceConfigActivity.this, WeatherService.class);
                intent.setAction(WeatherWatchFaceConfigActivity.class.getSimpleName());
                intent.putExtra("PeerId",mPeerId);
                startService(intent);
                Toast.makeText(WeatherWatchFaceConfigActivity.this, "Refresh Succeeded!", Toast.LENGTH_SHORT).show();
            }
        });

        int themeSize = this.getResources().getInteger(R.integer.theme_size);
        for (int i = 1; i <= themeSize; i++) {
            id = this.getResources().getIdentifier("theme_" + i, "color", WeatherWatchFaceConfigActivity.class.getPackage().getName());
            themes.add(this.getResources().getColor(id));
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!alreadyInitialize) {
            initColorButton();

            Uri uri = new Uri.Builder()
                    .scheme("wear")
                    .path(mPath)
                    .authority(mPeerId)
                    .build();

            Wearable.DataApi.getDataItem(mGoogleApiClient, uri)
                    .setResultCallback(getDataCallback);
        }
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
        mPreviewImage.setBackgroundColor(themes.get(mTheme - 1));


        DataMap dataMap = new DataMap();
        dataMap.putInt(KEY_CONFIG_THEME, mTheme);
        sendConfigUpdateMessage(dataMap);
    }

    private void initColorButton() {
        int rowSize = this.getResources().getInteger(R.integer.theme_row_size);
        int count = rowSize;
        int width = this.mThemeButtonContainer.getWidth() / (rowSize + 1);
        int margin = width / (rowSize + 1);

        TableRow row = null;

        for (int i = 0; i < this.themes.size(); i++) {
            if (count == rowSize) {
                row = new TableRow(this);
                TableLayout.LayoutParams lp = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
                lp.topMargin = (int) this.getResources().getDimension(R.dimen.main_margin);
                row.setLayoutParams(lp);

                this.mThemeButtonContainer.addView(row);
            }

            if (count == 1) {
                count = rowSize;
            } else {
                count--;
            }

            StateListDrawable drawable = new StateListDrawable();
            GradientDrawable shape = new GradientDrawable();
            shape.setColor(themes.get(i));
            shape.setStroke(10, this.getResources().getColor(R.color.theme_button_stroke_press));
            shape.setShape(GradientDrawable.OVAL);
            drawable.addState(new int[]{android.R.attr.state_pressed}, shape);

            shape = new GradientDrawable();
            shape.setColor(themes.get(i));
            shape.setStroke(10, this.getResources().getColor(R.color.theme_button_stroke));
            shape.setShape(GradientDrawable.OVAL);
            drawable.addState(new int[]{android.R.attr.state_activated}, shape);

            shape = new GradientDrawable();
            shape.setColor(themes.get(i));
            shape.setShape(GradientDrawable.OVAL);
            drawable.addState(new int[]{android.R.attr.state_enabled}, shape);

            ImageView view = new ImageView(this);
            view.setBackground(drawable);

            row.addView(view);
            TableRow.LayoutParams lp = (TableRow.LayoutParams) view.getLayoutParams();
            lp.width = width;
            lp.height = width;
            lp.leftMargin = margin;
            view.setTag(i + 1);
            view.setOnClickListener(onColorViewClick);

            themeButtons.add(view);
        }
    }

    private void sendConfigUpdateMessage(DataMap config) {
        if (mPeerId != null && alreadyInitialize) {
            Log.d(TAG, "Sending Config: " + config);
            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, mPath, config.toByteArray())
                    .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.d(TAG, "Send Config Result: " + sendMessageResult.getStatus());
                        }
                    });
        }
    }
}
