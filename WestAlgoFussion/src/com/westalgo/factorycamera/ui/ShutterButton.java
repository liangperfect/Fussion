/*
 * Copyright (C) 2008 The Android Open Source Project
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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.R;

/**
 * A button designed to be used for the on-screen shutter button.
 * It's currently an {@code ImageView} that can call a delegate when the
 * pressed state changes.
 */
public class ShutterButton extends ImageView {
    private static final Log.Tag TAG = new Log.Tag("ShutterButton");
    public static final float ALPHA_WHEN_ENABLED = 1f;
    public static final float ALPHA_WHEN_DISABLED = 0.2f;
    private boolean mTouchEnabled = true;

    private Paint mPaint;
    private int outerRadius;
    private int innerRadius;
    private int innerSmallRadius;
    private int outerCircleColor;
    private int innerCircleColor;
    private int recordColor;
    private RectF rect;

    private String currentMode = PHOTO_MODE;
    public static final String PHOTO_MODE = "photo_mode";
    public static final String VIDEO_MODE = "video_mode";
    public static final String VIDEO_RECORDING_MODE = "video_record_mode";

    /**
     * A callback to be invoked when a ShutterButton's pressed state changes.
     */
    public interface OnShutterButtonListener {
        void onShutterButtonClick();
    }

    private List<OnShutterButtonListener> mListeners
    = new ArrayList<OnShutterButtonListener>();

    public ShutterButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        outerRadius = (int)context.getResources().getDimension(R.dimen.outer_radius);
        outerCircleColor = context.getResources().getColor(R.color.outer_circle_color);
        innerCircleColor = context.getResources().getColor(R.color.inner_circle_color);
        recordColor = context.getResources().getColor(R.color.color_record);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        rect = new RectF();
    }

    /**
     * Add an {@link OnShutterButtonListener} to a set of listeners.
     */
    public void addOnShutterButtonListener(OnShutterButtonListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    /**
     * Remove an {@link OnShutterButtonListener} from a set of listeners.
     */
    public void removeOnShutterButtonListener(OnShutterButtonListener listener) {
        if (mListeners.contains(listener)) {
            mListeners.remove(listener);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent m) {
        if (mTouchEnabled) {
            return super.dispatchTouchEvent(m);
        } else {
            return false;
        }
    }

    public void enableTouch(boolean enable) {
        mTouchEnabled = enable;
    }



    @Override
    public boolean performClick() {
        boolean result = super.performClick();
        if (getVisibility() == View.VISIBLE) {
            for (OnShutterButtonListener listener : mListeners) {
                listener.onShutterButtonClick();
            }
        }
        return result;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        innerRadius = (int) (getWidth() / 2 - 2.5 * outerRadius);
        innerSmallRadius = getWidth() / 10;
        rect.left = getWidth() / 2 - innerSmallRadius;
        rect.top = getHeight() / 2 - innerSmallRadius;
        rect.right = getWidth() / 2 + innerSmallRadius;
        rect.bottom = getHeight() / 2 + innerSmallRadius;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //super.onDraw(canvas);
        drawOuterCircle(canvas);
        drawInnerCircle(canvas);
        switch (currentMode) {
            case PHOTO_MODE:
                break;
            case VIDEO_MODE:
                drawSmallCircle(canvas);
                break;
            case VIDEO_RECORDING_MODE:
                drawSmallRect(canvas);
                break;
        }
    }

    public void setMode(String mode) {
        currentMode = mode;
        invalidate();
    }

    private void drawOuterCircle(Canvas canvas) {
        mPaint.setColor(outerCircleColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(outerRadius);
        canvas.drawCircle(getWidth()/2,getHeight()/2,getWidth()/2-outerRadius,mPaint);
    }
    private void drawInnerCircle(Canvas canvas) {
        mPaint.setColor(innerCircleColor);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, innerRadius, mPaint);
    }

    private void drawSmallCircle(Canvas canvas) {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(recordColor);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, innerSmallRadius, mPaint);
    }
    private void drawSmallRect(Canvas canvas) {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(recordColor);
        canvas.drawRoundRect(rect,5,5,mPaint);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            setAlpha(1.0f);
        } else {
            setAlpha(0.5f);
        }
    }
}
