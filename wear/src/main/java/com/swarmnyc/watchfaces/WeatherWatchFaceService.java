package com.swarmnyc.watchfaces;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public abstract class WeatherWatchFaceService extends CanvasWatchFaceService {
    public class WeatherWatchFaceEngine extends CanvasWatchFaceService.Engine
            implements
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener,
            DataApi.DataListener, NodeApi.NodeListener {

        protected static final int MSG_UPDATE_TIME = 0;
        protected long UPDATE_RATE_MS;
        protected static final long WEATHER_INFO_TIME_OUT = DateUtils.HOUR_IN_MILLIS * 6;
        protected final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Time zone changed
                mWeatherInfoReceivedTime = 0;
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        /**
         * Handler to update the time periodically in interactive mode.
         */
        protected final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();

                        if (shouldUpdateTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = UPDATE_RATE_MS - (timeMs % UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                            requireWeatherInfo();
                        }
                        break;
                }
            }
        };
        protected int mTheme = 3;
        protected int mTimeUnit = ConverterUtil.TIME_UNIT_12;
        protected AssetManager mAsserts;
        protected Bitmap mWeatherConditionDrawable;
        protected GoogleApiClient mGoogleApiClient;
        protected Paint mBackgroundPaint;
        protected Paint mDatePaint;
        protected Paint mDateSuffixPaint;
        protected Paint mDebugInfoPaint;
        protected Paint mTemperatureBorderPaint;
        protected Paint mTemperaturePaint;
        protected Paint mTemperatureSuffixPaint;
        protected Paint mTimePaint;
        protected Resources mResources;
        protected String mWeatherCondition;
        protected String mWeatherConditionResourceName;
        protected Time mSunriseTime;
        protected Time mSunsetTime;
        protected Time mTime;
        protected boolean isRound;
        protected boolean mLowBitAmbient;
        protected boolean mRegisteredService = false;

        protected int mBackgroundColor;
        protected int mBackgroundDefaultColor;
        protected int mRequireInterval;
        protected int mTemperature = Integer.MAX_VALUE;
        protected int mTemperatureScale;
        protected long mWeatherInfoReceivedTime;
        protected long mWeatherInfoRequiredTime;
        private String mName;

        public WeatherWatchFaceEngine(String name) {
            mName = name;
            mGoogleApiClient = new GoogleApiClient.Builder(WeatherWatchFaceService.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();
        }

        @Override
        public void onConnected(Bundle bundle) {
            log("Connected: " + bundle);
            getConfig();

            Wearable.NodeApi.addListener(mGoogleApiClient, this);
            Wearable.DataApi.addListener(mGoogleApiClient, this);
            requireWeatherInfo();
        }

        @Override
        public void onConnectionSuspended(int i) {
            log("ConnectionSuspended: " + i);
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            for (int i = 0; i < dataEvents.getCount(); i++) {
                DataEvent event = dataEvents.get(i);
                DataMap dataMap = DataMap.fromByteArray(event.getDataItem().getData());
                log("onDataChanged: " + dataMap);

                fetchConfig(dataMap);
            }
        }

        @Override
        public void onPeerConnected(Node node) {
            log("PeerConnected: " + node);
            requireWeatherInfo();
        }

        @Override
        public void onPeerDisconnected(Node node) {
            log("PeerDisconnected: " + node);
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            log("ConnectionFailed: " + connectionResult);

        }


        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WeatherWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            mResources = WeatherWatchFaceService.this.getResources();
            mAsserts = WeatherWatchFaceService.this.getAssets();

            mDebugInfoPaint = new Paint();
            mDebugInfoPaint.setColor(Color.parseColor("White"));
            mDebugInfoPaint.setTextSize(20);
            mDebugInfoPaint.setAntiAlias(true);

            mTime = new Time();
            mSunriseTime = new Time();
            mSunsetTime = new Time();

            mRequireInterval = mResources.getInteger(R.integer.weather_default_require_interval);
            mWeatherInfoRequiredTime = System.currentTimeMillis() - (DateUtils.SECOND_IN_MILLIS * 58);
            mGoogleApiClient.connect();
        }

        @Override
        public void onDestroy() {
            log("Destroy");
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);

            log("onInterruptionFilterChanged: " + interruptionFilter);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false);

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
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    Wearable.NodeApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }

                unregisterTimeZoneService();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        protected Paint createTextPaint(int color, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(color);
            if (typeface != null)
                paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        protected boolean shouldUpdateTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        protected void fetchConfig(DataMap config) {
            if (config.containsKey(Consts.KEY_WEATHER_UPDATE_TIME)) {
                mWeatherInfoReceivedTime = config.getLong(Consts.KEY_WEATHER_UPDATE_TIME);
            }

            if (config.containsKey(Consts.KEY_WEATHER_CONDITION)) {
                String cond = config.getString(Consts.KEY_WEATHER_CONDITION);
                if (TextUtils.isEmpty(cond)) {
                    mWeatherCondition = null;
                } else {
                    mWeatherCondition = cond;
                }
            }

            if (config.containsKey(Consts.KEY_WEATHER_TEMPERATURE)) {
                mTemperature = config.getInt(Consts.KEY_WEATHER_TEMPERATURE);
                if (mTemperatureScale != ConverterUtil.FAHRENHEIT) {
                    mTemperature = ConverterUtil.convertFahrenheitToCelsius(mTemperature);
                }
            }

            if (config.containsKey(Consts.KEY_WEATHER_SUNRISE)) {
                mSunriseTime.set(config.getLong(Consts.KEY_WEATHER_SUNRISE) * 1000);
                log("SunriseTime: " + mSunriseTime);
            }

            if (config.containsKey(Consts.KEY_WEATHER_SUNSET)) {
                mSunsetTime.set(config.getLong(Consts.KEY_WEATHER_SUNSET) * 1000);
                log("SunsetTime: " + mSunsetTime);
            }

            if (config.containsKey(Consts.KEY_CONFIG_TEMPERATURE_SCALE)) {
                int scale = config.getInt(Consts.KEY_CONFIG_TEMPERATURE_SCALE);

                if (scale != mTemperatureScale) {
                    if (scale == ConverterUtil.FAHRENHEIT) {
                        mTemperature = ConverterUtil.convertCelsiusToFahrenheit(mTemperature);
                    } else {
                        mTemperature = ConverterUtil.convertFahrenheitToCelsius(mTemperature);
                    }
                }

                mTemperatureScale = scale;
            }

            if (config.containsKey(Consts.KEY_CONFIG_THEME)) {
                mTheme = config.getInt(Consts.KEY_CONFIG_THEME);
            }

            if (config.containsKey(Consts.KEY_CONFIG_TIME_UNIT)) {
                mTimeUnit = config.getInt(Consts.KEY_CONFIG_TIME_UNIT);
            }

            if (config.containsKey(Consts.KEY_CONFIG_REQUIRE_INTERVAL)) {
                mRequireInterval = config.getInt(Consts.KEY_CONFIG_REQUIRE_INTERVAL);
            }

            invalidate();
        }

        protected void getConfig() {
            log("Start getting Config");
            Wearable.NodeApi.getLocalNode(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetLocalNodeResult>() {
                @Override
                public void onResult(NodeApi.GetLocalNodeResult getLocalNodeResult) {
                    Uri uri = new Uri.Builder()
                            .scheme("wear")
                            .path(Consts.PATH_CONFIG + mName)
                            .authority(getLocalNodeResult.getNode().getId())
                            .build();

                    getConfig(uri);

                    uri = new Uri.Builder()
                            .scheme("wear")
                            .path(Consts.PATH_WEATHER_INFO)
                            .authority(getLocalNodeResult.getNode().getId())
                            .build();

                    getConfig(uri);
                }
            });
        }

        protected void getConfig(Uri uri) {

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

        protected void log(String message) {
            Log.d(WeatherWatchFaceService.this.getClass().getSimpleName(), message);
        }

        protected void registerTimeZoneService() {
            //TimeZone
            if (mRegisteredService) {
                return;
            }

            mRegisteredService = true;

            // TimeZone
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WeatherWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        protected void requireWeatherInfo() {
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
            Wearable.MessageApi.sendMessage(mGoogleApiClient, "", Consts.PATH_WEATHER_REQUIRE, null)
                    .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            log("SendRequireMessage:" + sendMessageResult.getStatus());
                        }
                    });
        }

        protected void unregisterTimeZoneService() {
            if (!mRegisteredService) {
                return;
            }
            mRegisteredService = false;

            //TimeZone
            WeatherWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        protected void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldUpdateTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }
    }
}

