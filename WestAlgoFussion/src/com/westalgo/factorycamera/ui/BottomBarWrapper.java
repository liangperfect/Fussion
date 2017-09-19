/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.westalgo.factorycamera.R;
import com.westalgo.factorycamera.app.CameraAppUI;
import com.westalgo.factorycamera.debug.Log;

/**
 * The goal of this class is to ensure mode options is always laid out to the
 * left of or above bottom bar in landscape or portrait respectively. All the
 * other children in this view group can be expected to be laid out the same way
 * as they are in a normal FrameLayout.
 */
public class BottomBarWrapper extends FrameLayout {

    private final static Log.Tag TAG = new Log.Tag("BottomBarWrapper");
    private CameraAppUI mCameraAppUI;

    // a reference to the shutter background's first contained drawable
    // if it's an animated circle drawable (for video mode)
    // private AnimatedCircleDrawable mAnimatedCircleDrawable;

    // a reference to the shutter background's first contained drawable
    // if it's a color drawable (for all other modes)
    private ColorDrawable mColorDrawable;
    private int mBackgroundColor;
    private int mBackgroundPressedColor;
    private int mBackgroundAlpha = 0xff;
    private ShutterButton mShutterButton;
    private RectF mRect = new RectF();

    public BottomBarWrapper(Context context, AttributeSet attrs) {
        super(context, attrs);
        mColorDrawable = (ColorDrawable)context.getResources().getDrawable(R.color.bottombar_pressed);
    }

    public void setCameraAppUI(CameraAppUI cameraAppUI) {
        mCameraAppUI = cameraAppUI;
    }

    @Override
    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
        setPaintColor(mBackgroundAlpha, mBackgroundColor);
    }

    //TODO related to index
    public void setBackgroundPressedColor(int color) {
        mBackgroundPressedColor = color;
    }

    private void setCaptureButtonUp() {
        setPaintColor(mBackgroundAlpha, mBackgroundColor);
    }

    private void setCaptureButtonDown() {
        setPaintColor(mBackgroundAlpha, mBackgroundPressedColor);
    }

    private void setPaintColor(int alpha, int color) {
        if (mColorDrawable != null) {
            mColorDrawable.setColor(color);
            mColorDrawable.setAlpha(alpha);
        }
    }

    @Override
    public void onFinishInflate() {
        mShutterButton = (ShutterButton) findViewById(R.id.shutter_button);
        mShutterButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEvent.ACTION_DOWN == event.getActionMasked()) {
                    setCaptureButtonDown();
                } else if (MotionEvent.ACTION_UP == event.getActionMasked()
                || MotionEvent.ACTION_CANCEL == event.getActionMasked()) {
                    setCaptureButtonUp();
                } else if (MotionEvent.ACTION_MOVE == event.getActionMasked()) {
                    mRect.set(0, 0, getWidth(), getHeight());
                    if (!mRect.contains(event.getX(), event.getY())) {
                        setCaptureButtonUp();
                    }
                }
                return false;
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {

        if (mCameraAppUI == null) {
            super.onLayout(changed, left, top, right, bottom);
        } else {
            int count = getChildCount();
            Rect rect = mCameraAppUI.getBottomBarRect();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() != GONE) {
                    child.layout(rect.left, rect.top, rect.right, rect.bottom);
                }
            }
        }
    }
}