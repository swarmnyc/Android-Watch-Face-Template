package com.swarmnyc.watchfaces;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Wearable;

import java.util.TimeZone;

public class WeatherWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "WeatherWatchFaceService";


    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {
        /**
         * Update rate in milliseconds for normal (not ambient and not mute) mode.
         * We update twice a second to blink the colons.
         */
        static final long NORMAL_UPDATE_RATE_MS = 500;

        /**
         * Update rate in milliseconds for mute mode. We update every minute, like in ambient mode.
         */
        static final long MUTE_UPDATE_RATE_MS = 1000;

        static final String COLON_STRING = ":";
        static final int MSG_UPDATE_TIME = 0;
        /**
         * Alpha value for drawing time when in mute mode.
         */
        static final int MUTE_ALPHA = 100;
        /**
         * Alpha value for drawing time when not in mute mode.
         */
        static final int NORMAL_ALPHA = 255;
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
                                    mInteractiveUpdateRateMs - (timeMs % mInteractiveUpdateRateMs);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        boolean mLowBitAmbient;
        Paint mBackgroundPaint;
        Paint mTimePaint;
        Paint mDatePaint;
        float mColonWidth;
        Paint mTemperaturePaint;
        long mInteractiveUpdateRateMs = NORMAL_UPDATE_RATE_MS;
        Time mTime;
        boolean mRegisteredTimeZoneReceiver = false;
        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(WeatherWatchFaceService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        boolean isRound;
        boolean mMute;
        private Resources resources;
        private AssetManager asserts;
        private float mTimeOffset;
        private float mDateOffset;
        private float mInternalDistance;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WeatherWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            resources = WeatherWatchFaceService.this.getResources();
            asserts = WeatherWatchFaceService.this.getAssets();

            mInternalDistance = resources.getDimension(R.dimen.weather_internal_distance);
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.weather_bg_color));
            mTimePaint = createTextPaint(resources.getColor(R.color.weather_time_color),
                    Typeface.createFromAsset(asserts, resources.getString(R.string.weather_date_font)));

            mDatePaint = createTextPaint(resources.getColor(R.color.weather_date_color),
                    Typeface.createFromAsset(asserts, resources.getString(R.string.weather_time_font)));

            mTemperaturePaint = createTextPaint(resources.getColor(R.color.weather_temperature_color),
                    Typeface.createFromAsset(asserts, resources.getString(R.string.weather_temperature_font)));

            mTime = new Time();

        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

            boolean mShouldDrawColons = (System.currentTimeMillis() % 1000) < 500;

            String hourString = String.valueOf(convertTo12Hour(mTime.hour));
            String minString = String.valueOf(mTime.minute);

            float hourWidth = mTimePaint.measureText(hourString);
            float minWidth = mTimePaint.measureText(minString);
            float x = (bounds.width() - (hourWidth + minWidth + mColonWidth)) / 2;
            float y = ((canvas.getHeight() - mTimeOffset) / 2);

            canvas.drawText(hourString, x, y, mTimePaint);
            x += hourWidth;
            if (isInAmbientMode() || mMute || mShouldDrawColons) {
                canvas.drawText(COLON_STRING, x, y, mTimePaint);
            }
            x += mColonWidth;
            canvas.drawText(minString, x, y, mTimePaint);

            y += mInternalDistance - mDateOffset / 2;

            canvas.drawText("123", x, y, mDatePaint);
        }

        private int convertTo12Hour(int hour) {
            int result = hour % 12;
            return (result == 0) ? 12 : result;
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mTimePaint.setAntiAlias(antiAlias);
                mDatePaint.setAntiAlias(antiAlias);
                mTemperaturePaint.setAntiAlias(antiAlias);

            }
            invalidate();

            // Whether the timer should be running depends on whether we're in ambient mode (as well
            // as whether we're visible), so we may need to start or stop the timer.
            updateTimer();
        }

        public void setInteractiveUpdateRateMs(long updateRateMs) {
            if (updateRateMs == mInteractiveUpdateRateMs) {
                return;
            }
            mInteractiveUpdateRateMs = updateRateMs;

            // Stop and restart the timer so the new update rate takes effect immediately.
            if (shouldTimerBeRunning()) {
                updateTimer();
            }
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE;
            // We only need to update once a minute in mute mode.
            setInteractiveUpdateRateMs(inMuteMode ? MUTE_UPDATE_RATE_MS : NORMAL_UPDATE_RATE_MS);

            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                int alpha = inMuteMode ? MUTE_ALPHA : NORMAL_ALPHA;
                mTimePaint.setAlpha(alpha);
                mDatePaint.setAlpha(alpha);
                mTemperaturePaint.setAlpha(alpha);
                invalidate();
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                mGoogleApiClient.connect();

                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();

                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        @Override
        public void onConnected(Bundle bundle) {

        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {

        }

        private Paint createTextPaint(int color, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(color);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {

        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WeatherWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WeatherWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            isRound = insets.isRound();

            float timeTextSize = resources.getDimension(isRound ?
                    R.dimen.weather_time_size_round : R.dimen.weather_time_size);

            float dateTextSize = resources.getDimension(isRound ?
                    R.dimen.weather_date_size_round : R.dimen.weather_date_size);

            float tempTextSize = resources.getDimension(isRound ?
                    R.dimen.weather_temperature_size_round : R.dimen.weather_temperature_size);

            mTimePaint.setTextSize(timeTextSize);
            mDatePaint.setTextSize(dateTextSize);
            mTemperaturePaint.setTextSize(tempTextSize);
            mColonWidth = mTimePaint.measureText(COLON_STRING);

            mTimeOffset = (mTimePaint.descent() + mTimePaint.ascent());
            mDateOffset = (mDatePaint.descent() + mDatePaint.ascent());
        }
    }
}
