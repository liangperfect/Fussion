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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import com.westalgo.factorycamera.R;
import com.westalgo.factorycamera.app.CameraAppUI;
import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.util.CameraUtil;

public class MainActivityLayout extends FrameLayout {

    private final Log.Tag TAG = new Log.Tag("MainActivityLayout");
    // Only check for intercepting touch events within first 500ms
    private static final int SWIPE_TIME_OUT = 500;

    private ModeListView mModeList;
    private boolean mCheckToIntercept;
    private MotionEvent mDown;
    private final int mSlop;
    private boolean mRequestToInterceptTouchEvents = false;
    private View mTouchReceiver = null;
    private final boolean mIsCaptureIntent;
    private NonDecorWindowSizeChangedListener mNonDecorWindowSizeChangedListener = null;

    // TODO: This can be removed once we come up with a new design for
    // b/13751653.
    private boolean mSwipeEnabled = true;
    private boolean mLayoutClickEnabled = true;

    public MainActivityLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        Activity activity = (Activity) context;
        Intent intent = activity.getIntent();
        String action = intent.getAction();
        mIsCaptureIntent = (MediaStore.ACTION_IMAGE_CAPTURE.equals(action)
                            || MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(action) || MediaStore.ACTION_VIDEO_CAPTURE
                            .equals(action));
    }

    /**
     * Enables or disables the swipe for modules not supporting the new swipe
     * logic yet.
     */
    public void setSwipeEnabled(boolean enabled) {
        mSwipeEnabled = enabled;
    }

    public void setLayoutClickEnable(boolean enabled) {
        mLayoutClickEnabled = enabled;
        mSwipeEnabled = enabled;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mNonDecorWindowSizeChangedListener != null) {
            mNonDecorWindowSizeChangedListener.onNonDecorWindowSizeChanged(
                MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec),
                CameraUtil.getDisplayRotation(getContext()));
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mCheckToIntercept = true;
            mDown = MotionEvent.obtain(ev);
            mTouchReceiver = null;
            mRequestToInterceptTouchEvents = false;
            return false;
        } else if (mRequestToInterceptTouchEvents) {
            mRequestToInterceptTouchEvents = false;
            onTouchEvent(mDown);
            return true;
        } else if (ev.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            // Do not intercept touch once child is in zoom mode
            mCheckToIntercept = false;
            return false;
        } else {
            // TODO: This can be removed once we come up with a new design for
            // b/13751653.
            if (!mCheckToIntercept) {
                return false;
            }
            if (ev.getEventTime() - ev.getDownTime() > SWIPE_TIME_OUT) {
                return false;
            }
            if (mIsCaptureIntent || !mSwipeEnabled) {
                return true;
            }
            int deltaX = (int) (ev.getX() - mDown.getX());
            int deltaY = (int) (ev.getY() - mDown.getY());
            if (ev.getActionMasked() == MotionEvent.ACTION_MOVE
                    && Math.abs(deltaX) > mSlop) {
                // Intercept right swipe
                if (deltaX >= Math.abs(deltaY) * 2) {
                    mTouchReceiver = mModeList;
                    onTouchEvent(mDown);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Log.v(TAG, "MainActivityLayout onTouchEvent");

        if(!mLayoutClickEnabled)return true;
        if (mTouchReceiver != null) {
            mTouchReceiver.setVisibility(VISIBLE);
            return mTouchReceiver.dispatchTouchEvent(ev);
        }
        return false;
    }

    @Override
    public void onFinishInflate() {
        mModeList = (ModeListView) findViewById(R.id.mode_list_layout);
    }

    public void redirectTouchEventsTo(View touchReceiver) {
        Log.e(TAG, "redirectTouchEventsTo:");
        if (touchReceiver == null) {
            Log.e(TAG, "Cannot redirect touch to a null receiver.");
            return;
        }
        mTouchReceiver = touchReceiver;
        mRequestToInterceptTouchEvents = true;
    }

    /**
     * Sets a listener that gets notified when the layout size is changed. This
     * size is the size of main activity layout. It is also the size of the window
     * excluding the system decor such as status bar and nav bar.
     */
    public void setNonDecorWindowSizeChangedListener(
        NonDecorWindowSizeChangedListener listener) {
        mNonDecorWindowSizeChangedListener = listener;
        if (mNonDecorWindowSizeChangedListener != null) {
            mNonDecorWindowSizeChangedListener.onNonDecorWindowSizeChanged(
                getMeasuredWidth(), getMeasuredHeight(),
                CameraUtil.getDisplayRotation(getContext()));
        }
    }

    /**
     * This listener gets called when the size of the window (excluding the system
     * decor such as status bar and nav bar) has changed.
     */
    public interface NonDecorWindowSizeChangedListener {
        public void onNonDecorWindowSizeChanged(int width, int height, int rotation);
    }
}