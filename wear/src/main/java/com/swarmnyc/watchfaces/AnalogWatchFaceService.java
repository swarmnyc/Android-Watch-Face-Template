package com.swarmnyc.watchfaces;

import android.graphics.Paint;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

public class AnalogWatchFaceService extends CanvasWatchFaceService {
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "AnalogWatchFaceService";

// -------------------------- OTHER METHODS --------------------------

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

// -------------------------- INNER CLASSES --------------------------

    private class Engine extends CanvasWatchFaceService.Engine{
// ------------------------------ FIELDS ------------------------------

        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mSecondPaint;

// -------------------------- OTHER METHODS --------------------------

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(AnalogWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
        }
    }
}
