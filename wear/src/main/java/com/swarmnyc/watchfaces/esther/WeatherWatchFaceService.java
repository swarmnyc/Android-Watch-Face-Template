package com.swarmnyc.watchfaces.esther;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.TimeZone;

public class WeatherWatchFaceService extends CanvasWatchFaceService {
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "WeatherWatchFaceService";
    private static String PACKAGE_NAME = WeatherWatchFaceService.class.getPackage().getName();

// -------------------------- OTHER METHODS --------------------------

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

// -------------------------- INNER CLASSES --------------------------

    private class Engine extends CanvasWatchFaceService.Engine
            implements GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener,
            MessageApi.MessageListener,
            NodeApi.NodeListener {
// ------------------------------ FIELDS ------------------------------

        public static final String KEY_CONFIG_REQUIRE_INTERVAL = "RequireInterval";
        public static final String KEY_CONFIG_TEMPERATURE_SCALE = "TemperatureScale";
        public static final String KEY_WEATHER_CONDITION = "Condition";
        public static final String KEY_WEATHER_SUNRISE = "Sunrise";
        public static final String KEY_WEATHER_SUNSET = "Sunset";
        public static final String KEY_CONFIG_THEME = "Theme";
        public static final String KEY_CONFIG_TIMEUNIT = "TimeUnit";
        public static final String KEY_WEATHER_TEMPERATURE = "Temperature";
        public static final String PATH_CONFIG = "/WeatherWatchFace/Config";
        public static final String PATH_WEATHER_INFO = "/WeatherWatchFace/WeatherInfo";
        public static final String PATH_WEATHER_REQUIRE = "/WeatherService/Require";
        private static final String COLON_STRING = ":";
        private static final int TIMEUNIT12 = 0;
        private static final int TIMEUNIT24 = 1;
        private static final int MSG_UPDATE_TIME = 0;
        private int mTheme = 3;
        private int mTimeUnit = TIMEUNIT12;

        /**
         * Update rate in milliseconds for normal (not ambient and not mute) mode.
         * We update twice a second to blink the colons.
         */
        private static final long UPDATE_RATE_MS = 500;
        private static final long WEATHER_INFO_TIME_OUT = DateUtils.HOUR_IN_MILLIS * 4;

        AssetManager mAsserts;
        Bitmap mWeatherConditionDrawable;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(WeatherWatchFaceService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        /**
         * Handler to update the time periodically in interactive mode.
         */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();

                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs =
                                    UPDATE_RATE_MS - (timeMs % UPDATE_RATE_MS);
                            //log("UpdateDelayed: " + delayMs);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }

                        requireWeatherInfo();
                        break;
                }
            }
        };



        Resources mResources;



        boolean isRound;
        boolean mGotConfig;
        boolean mLowBitAmbient;
        boolean mRegisteredService = false;
        float mColonXOffset;
        float mDateSuffixYOffset;
        float mDateYOffset;
        float mDebugInfoYOffset;
        float mInternalDistance;
        float mTemperatureSuffixYOffset;
        float mTemperatureYOffset;
        float mTimeXOffset;
        float mTimeYOffset;
        int mBackgroundColor;
        int mBackgroundDefaultColor;
        int mRequireInterval;
        int mTemperature = Integer.MAX_VALUE;
        int mTemperatureScale;
        long mWeatherInfoReceivedTime;
        long mWeatherInfoRequiredTime;

        Paint mTextPaint;
        Paint dateText;
        Float mTextXOffset;
        Float mTextYOffset;

        Float dateTextOffset;
        Time mTime;
        Paint mRect;
        Paint mBottomRect;


        Typeface theFont;


// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ConnectionCallbacks ---------------------

        @Override
        public void onConnected(Bundle bundle) {
            log("Connected: " + bundle);
            getConfig();

            Wearable.MessageApi.addListener(mGoogleApiClient, this);
            Wearable.NodeApi.addListener(mGoogleApiClient, this);
            requireWeatherInfo();
        }

        @Override
        public void onConnectionSuspended(int i) {
            log("ConnectionSuspended: " + i);
        }

// --------------------- Interface MessageListener ---------------------

        @Override
        public void onMessageReceived(MessageEvent messageEvent) {
            byte[] rawData = messageEvent.getData();
            DataMap dataMap = DataMap.fromByteArray(rawData);
            log("onMessageReceived: " + dataMap);

            fetchConfig(dataMap);

            if (messageEvent.getPath().equals(PATH_WEATHER_INFO)) {
                mWeatherInfoReceivedTime = System.currentTimeMillis();
            }

            if (messageEvent.getPath().equals(PATH_CONFIG)) {
                saveConfig();
            }
        }

// --------------------- Interface NodeListener ---------------------

        @Override
        public void onPeerConnected(Node node) {
            log("PeerConnected: " + node);
            requireWeatherInfo();
        }

        @Override
        public void onPeerDisconnected(Node node) {
            log("PeerDisconnected: " + node);
        }

// --------------------- Interface OnConnectionFailedListener ---------------------

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            log("ConnectionFailed: " + connectionResult);

        }

// -------------------------- OTHER METHODS --------------------------

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            log("onAmbientModeChanged: " + inAmbientMode);



            if (inAmbientMode){
                mRect.setColor(Color.rgb(0,0,0));
                mBottomRect.setColor(Color.rgb(50,50,50));
            }
            else {
                mRect.setColor(Color.rgb(225,53,51));
                mBottomRect.setColor(Color.rgb(38,167,193));
            }


            invalidate();

            // Whether the timer should be running depends on whether we're in ambient mode (as well
            // as whether we're visible), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);


        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WeatherWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());




            theFont = Typeface.createFromAsset(getAssets(),
                    "fonts/Bebas-Regular.otf");


            // Create the Paint for later use
            mTextPaint = new Paint();
            mTextPaint.setTypeface(theFont);
            mTextPaint.setTextSize(60);
            mTextPaint.setColor(Color.WHITE);
            mTextPaint.setAntiAlias(true);

            mUpdateTimeHandler.sendEmptyMessageDelayed(0, 500);

            mTime = new Time();

            mRect = new Paint();
            mRect.setColor(Color.rgb(225,53,51));

            mBottomRect = new Paint();
            mBottomRect.setColor(Color.rgb(38,167,193));

            // In order to make text in the center, we need adjust its position
            mTextXOffset = mTextPaint.measureText("12:00") / 2;
            mTextYOffset = ((mTextPaint.ascent() + mTextPaint.descent()) / 2);


            dateText = new Paint();
            dateText.setTextSize(18);
            dateText.setColor(Color.WHITE);
            dateText.setAntiAlias(true);
            dateText.setTypeface(theFont);

            dateTextOffset = dateText.measureText("TUESDAY, SEPT 02") / 2;





            mGoogleApiClient.connect();
        }

        @Override
        public void onDestroy() {
            log("Destroy");
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            //log("Draw");
            mTime.setToNow();

            boolean mShouldDrawColons = (System.currentTimeMillis() % 1000) < 500;
            String fullTime;
            if (mShouldDrawColons) {
                fullTime = String.format("%02d", convertHour(mTime.hour)) + ":" + String.format("%02d", mTime.minute);
            } else {
                fullTime = String.format("%02d", convertHour(mTime.hour)) + " " + String.format("%02d", mTime.minute);
            }
            canvas.drawRect(0,0,bounds.width(), (float)(bounds.height() * .66), mRect);

            canvas.drawRect(0,(float)(bounds.height() * .66), bounds.width(), bounds.height(), mBottomRect);

            canvas.drawRect(0,(float)(bounds.height() * .66) - 8, bounds.width(), (float)(bounds.height() * .66) + 8, mTextPaint);


            mTextXOffset = mTextPaint.measureText(fullTime) / 2;

            canvas.drawText(fullTime,
                    bounds.centerX() - mTextXOffset,
                    bounds.centerY() - (float)(.11 * bounds.height()),
                    mTextPaint);



            String date = convertToDay(mTime.weekDay) + ", " + convertToMonth(mTime.month) + Integer.toString(mTime.monthDay);

            dateTextOffset = dateText.measureText(date) / 2;


            canvas.drawText(date,
                    bounds.centerX() - dateTextOffset,
                    (bounds.centerY() - (float)(.01 * bounds.height())),
                    dateText);


        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);

            log("onInterruptionFilterChanged: " + interruptionFilter);

            //TODO: to understand onInterruptionFilterChanged
            //boolean inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE;
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);

            log("onPropertiesChanged: LowBitAmbient=" + mLowBitAmbient);


        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            log("TimeTick");
            invalidate();
            requireWeatherInfo();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            log("onVisibilityChanged: " + visible);

            if (visible) {
                mGoogleApiClient.connect();
                registerTimeZoneService();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.MessageApi.removeListener(mGoogleApiClient, this);
                    Wearable.NodeApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }

                unregisterTimeZoneService();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private Paint createTextPaint(int color, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(color);
            if (typeface != null)
                paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        private String convertToMonth(int month) {
            switch (month) {
                case 0:
                    return "January ";
                case 1:
                    return "February ";
                case 2:
                    return "March ";
                case 3:
                    return "April ";
                case 4:
                    return "May ";
                case 5:
                    return "June ";
                case 6:
                    return "July ";
                case 7:
                    return "August ";
                case 8:
                    return "September ";
                case 9:
                    return "October ";
                case 10:
                    return "November ";
                case 11:
                    return "October ";
                default:
                    return "December";
            }
        }

        private String getDaySuffix(int monthDay) {
            switch (monthDay) {
                case 1:
                    return "st";
                case 2:
                    return "nd";
                case 3:
                    return "rd";
                default:
                    return "th";
            }
        }

        private String convertToDay(int day) {
            switch(day) {
                case 0:
                    return "Sunday";
                case 1:
                    return "Monday";
                case 2:
                    return "Tuesday";
                case 3:
                    return "Wednesday";
                case 4:
                    return "Thursday";
                case 5:
                    return "Friday";
                case 6:
                    return "Saturday";
                default:
                    return "Sunday";
            }

        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        private int convertHour(int hour) {
            if (mTimeUnit == TIMEUNIT12) {
                int result = hour % 12;
                return (result == 0) ? 12 : result;
            } else {
                return hour;
            }
        }

        private void fetchConfig(DataMap config) {


            if (config.containsKey(KEY_WEATHER_TEMPERATURE)) {
                mTemperature = config.getInt(KEY_WEATHER_TEMPERATURE);
                if (mTemperatureScale != ConverterUtil.FAHRENHEIT) {
                    mTemperature = ConverterUtil.convertFahrenheitToCelsius(mTemperature);
                }
            }



            if (config.containsKey(KEY_CONFIG_TEMPERATURE_SCALE)) {
                int scale = config.getInt(KEY_CONFIG_TEMPERATURE_SCALE);

                if (scale != mTemperatureScale) {
                    if (scale == ConverterUtil.FAHRENHEIT) {
                        mTemperature = ConverterUtil.convertCelsiusToFahrenheit(mTemperature);
                    } else {
                        mTemperature = ConverterUtil.convertFahrenheitToCelsius(mTemperature);
                    }
                }

                mTemperatureScale = scale;
            }



            if (config.containsKey(KEY_CONFIG_TIMEUNIT)) {
                mTimeUnit = config.getInt(KEY_CONFIG_TIMEUNIT);
            }

            if (config.containsKey(KEY_CONFIG_REQUIRE_INTERVAL)) {
                mRequireInterval = config.getInt(KEY_CONFIG_REQUIRE_INTERVAL);
            }

            invalidate();
        }

        private void getConfig() {
            if (mGotConfig) {
                return;
            }

            log("Start getting Config");
            Wearable.NodeApi.getLocalNode(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetLocalNodeResult>() {
                @Override
                public void onResult(NodeApi.GetLocalNodeResult getLocalNodeResult) {
                    Uri uri = new Uri.Builder()
                            .scheme("wear")
                            .path(PATH_CONFIG)
                            .authority(getLocalNodeResult.getNode().getId())
                            .build();

                    Wearable.DataApi.getDataItem(mGoogleApiClient, uri)
                            .setResultCallback(
                                    new ResultCallback<DataApi.DataItemResult>() {
                                        @Override
                                        public void onResult(DataApi.DataItemResult dataItemResult) {
                                            log("Finish Config: " + dataItemResult.getStatus());
                                            if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
                                                fetchConfig(DataMapItem.fromDataItem(dataItemResult.getDataItem()).getDataMap());
                                            }
                                        }
                                    }
                            );
                }
            });
        }

        private void log(String message) {
            Log.d(TAG, message);
        }

        private void registerTimeZoneService() {
            //TimeZone and TemperatureSensor
            if (mRegisteredService) {
                return;
            }

            mRegisteredService = true;

            // TimeZone
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WeatherWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void requireWeatherInfo() {
            if (!mGoogleApiClient.isConnected())
                return;

            long timeMs = System.currentTimeMillis();

            // The weather info is still up to date.
            if ((timeMs - mWeatherInfoReceivedTime) <= mRequireInterval)
                return;

            // Try once in a min.
            if ((timeMs - mWeatherInfoRequiredTime) <= DateUtils.MINUTE_IN_MILLIS)
                return;

            mWeatherInfoRequiredTime = timeMs;
            Wearable.MessageApi.sendMessage(mGoogleApiClient, "", PATH_WEATHER_REQUIRE, null)
                    .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            log("SendStartMessage:" + sendMessageResult.getStatus());
                        }
                    });
        }

        private void saveConfig() {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_CONFIG);
            DataMap config = putDataMapRequest.getDataMap();

            config.putInt(KEY_CONFIG_TEMPERATURE_SCALE, mTemperatureScale);
            config.putInt(KEY_CONFIG_THEME, mTheme);
            config.putInt(KEY_CONFIG_TIMEUNIT, mTimeUnit);
            config.putInt(KEY_CONFIG_REQUIRE_INTERVAL, mRequireInterval);

            Wearable.DataApi.putDataItem(mGoogleApiClient, putDataMapRequest.asPutDataRequest())
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            log("SaveConfig: " + dataItemResult.getStatus() + ", " + dataItemResult.getDataItem().getUri());
                        }
                    });
        }

        private void unregisterTimeZoneService() {
            if (!mRegisteredService) {
                return;
            }
            mRegisteredService = false;

            //TimeZone
            WeatherWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }
    }
}
