/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.westalgo.factorycamera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.westalgo.factorycamera.debug.Log;

/**
 * PreviewOverlay is a view that sits on top of the preview. It serves to
 * disambiguate touch events, as {@link com.westalgo.factorycamera.app.CameraAppUI} has
 * a touch listener set on it. As a result, touch events that happen on preview
 * will first go through the touch listener in AppUI, which filters out swipes
 * that should be handled on the app level. The rest of the touch events will be
 * handled here in {@link #onTouchEvent(android.view.MotionEvent)}.
 */
public class PreviewOverlay extends View {

    private static final Log.Tag TAG = new Log.Tag("PreviewOverlay");

    private GestureDetector mGestureDetector = null;
    private View.OnTouchListener mTouchListener = null;
    private OnPreviewTouchedListener mOnPreviewTouchedListener;

    public PreviewOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public boolean onTouchEvent(MotionEvent m) {
        // Pass the touch events to scale detector and gesture detector
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(m);
        }
        if (mTouchListener != null) {
            mTouchListener.onTouch(this, m);
        }
        if (mOnPreviewTouchedListener != null) {
            mOnPreviewTouchedListener.onPreviewTouched(m);
        }
        return true;
    }

    /**
     * Each module can pass in their own gesture listener through App UI. When a
     * gesture is detected, the {@link GestureDetector.OnGestureListener} will
     * be notified of the gesture.
     *
     * @param gestureListener
     *            a listener from a module that defines how to handle gestures
     */
    public void setGestureListener(
        GestureDetector.OnGestureListener gestureListener) {
        if (gestureListener != null) {
            mGestureDetector = new GestureDetector(getContext(),
                                                   gestureListener);
        }
    }

    /**
     * Set a touch listener on the preview overlay. When a module doesn't
     * support a {@link GestureDetector.OnGestureListener}, this can be used
     * instead.
     */
    public void setTouchListener(View.OnTouchListener touchListener) {
        mTouchListener = touchListener;
    }

    /**
     * During module switch, connections to the previous module should be
     * cleared.
     */
    public void reset() {
        mGestureDetector = null;
        mTouchListener = null;
    }

    /**
     * Set an {@link OnPreviewTouchedListener} to be executed on any preview
     * touch event.
     */
    public void setOnPreviewTouchedListener(OnPreviewTouchedListener listener) {
        mOnPreviewTouchedListener = listener;
    }

    public interface OnPreviewTouchedListener {
        /**
         * This gets called on any preview touch event.
         */
        public void onPreviewTouched(MotionEvent ev);
    }

}
