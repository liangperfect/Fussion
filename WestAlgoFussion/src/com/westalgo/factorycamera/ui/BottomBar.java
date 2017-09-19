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
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.westalgo.factorycamera.R;
import com.westalgo.factorycamera.app.CameraAppUI;
import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.util.ApiHelper;
import com.westalgo.factorycamera.util.CameraUtil;
import com.westalgo.factorycamera.settings.CameraSettingsActivity;

/**
 * BottomBar swaps its width and height on rotation. In addition, it also
 * changes gravity and layout orientation based on the new orientation.
 * Specifically, in landscape it aligns to the right side of its parent and lays
 * out its children vertically, whereas in portrait, it stays at the bottom of
 * the parent and has a horizontal layout orientation.
 */
public class BottomBar extends FrameLayout {

    private static final Log.Tag TAG = new Log.Tag("BottomBar");

    private static final int CIRCLE_ANIM_DURATION_MS = 300;
    private static final int DRAWABLE_MAX_LEVEL = 10000;
    private static final int MODE_CAPTURE = 0;
    private static final int MODE_INTENT = 1;
    private static final int MODE_INTENT_REVIEW = 2;
    private static final int MODE_CANCEL = 3;

    private int mMode;

    private final int mBackgroundAlphaOverlay;
    private final int mBackgroundAlphaDefault;
    private boolean mOverLayBottomBar;

    private FrameLayout mCaptureLayout;
    private ShutterButton mShutterButton;
    private ImageView mSettingBtn;

    private FrameLayout mThumbnailLayout;
    private ImageView mThumbnailView;

    private int mBackgroundColor;
    private int mBackgroundPressedColor;
    private int mBackgroundAlpha = 0xff;

    private boolean mDrawCircle;
    private final float mCircleRadius;

    private final Drawable.ConstantState[] mShutterButtonBackgroundConstantStates;
    // a reference to the shutter background's first contained drawable
    // if it's an animated circle drawable (for video mode)
    private AnimatedCircleDrawable mAnimatedCircleDrawable;
    // a reference to the shutter background's first contained drawable
    // if it's a color drawable (for all other modes)
    private ColorDrawable mColorDrawable;

    private RectF mRect = new RectF();

    private CameraAppUI mCameraAppUI;

    public BottomBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCircleRadius = getResources()
                        .getDimensionPixelSize(R.dimen.video_capture_circle_diameter) / 2;
        mBackgroundAlphaOverlay = 153;
        mBackgroundAlphaDefault = 255;

        // preload all the drawable BGs
        TypedArray ar = context.getResources()
                        .obtainTypedArray(R.array.shutter_button_backgrounds);
        int len = ar.length();
        Log.d(TAG, "---->>>BottomBar: length = " + len);
        mShutterButtonBackgroundConstantStates = new Drawable.ConstantState[len];
        for (int i = 0; i < len; i++) {
            Log.d(TAG, "----->>>BottomBar: i = " + i);
            int drawableId = ar.getResourceId(i, -1);
            Log.d(TAG, "----->>>BottomBar: drawableId = " + drawableId);
            mShutterButtonBackgroundConstantStates[i] =
                context.getResources().getDrawable(drawableId).getConstantState();
        }
        ar.recycle();
    }

    private void setPaintColor(int alpha, int color) {
        if (mAnimatedCircleDrawable != null) {
            mAnimatedCircleDrawable.setColor(color);
            mAnimatedCircleDrawable.setAlpha(alpha);
        } else if (mColorDrawable != null) {
            mColorDrawable.setColor(color);
            mColorDrawable.setAlpha(alpha);
        }
    }

    private void refreshPaintColor() {
        setPaintColor(mBackgroundAlpha, mBackgroundColor);
    }


    private void setCaptureButtonUp() {
        setPaintColor(mBackgroundAlpha, mBackgroundColor);
    }

    private void setCaptureButtonDown() {
        if (!ApiHelper.isLOrHigher()) {
            setPaintColor(mBackgroundAlpha, mBackgroundPressedColor);
        }
    }


    @Override
    public void onFinishInflate() {
        mCaptureLayout =
            (FrameLayout) findViewById(R.id.bottombar_capture);

        mThumbnailLayout = (FrameLayout) findViewById(R.id.bottombar_thumbnail);
        mThumbnailView = (ImageView) findViewById(R.id.thumbnail_preview);

        mShutterButton =
            (ShutterButton) findViewById(R.id.shutter_button);
        mSettingBtn = (ImageView) findViewById(R.id.app_settings_button);

        mSettingBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), CameraSettingsActivity.class);
                getContext().startActivity(intent);
            }
        });

        enableThumbnail(true);
        mThumbnailLayout.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(mCameraAppUI != null) {
                    mCameraAppUI.gotoGallery();
                }
            }

        });
    }

    public void setThumbnailResource(Bitmap bm) {
        if(bm != null && !bm.isRecycled()) {
            mThumbnailView.setImageBitmap(bm);
            enableThumbnail(true);
            mThumbnailLayout.setVisibility(View.VISIBLE);
        }
    }

    public void enableThumbnail(boolean isEnable) {
        mThumbnailLayout.setClickable(isEnable);
    }

    public void hideThumbnailView() {
        mThumbnailLayout.setVisibility(View.GONE);
    }

    /**
     * Perform a transition from the bottom bar options layout to the bottom bar
     * capture layout.
     */
    public void transitionToCapture() {
        mCaptureLayout.setVisibility(View.VISIBLE);
        mMode = MODE_CAPTURE;
    }

    /**
     * Perform a transition from the bottom bar options layout to the bottom bar
     * capture layout.
     */
    public void transitionToCancel() {
        mCaptureLayout.setVisibility(View.GONE);
        mMode = MODE_CANCEL;
    }

    /**
     * Perform a transition to the global intent layout. The current layout
     * state of the bottom bar is irrelevant.
     */
    public void transitionToIntentCaptureLayout() {
        mCaptureLayout.setVisibility(View.VISIBLE);

        mMode = MODE_INTENT;
    }

    /**
     * Perform a transition to the global intent review layout. The current
     * layout state of the bottom bar is irrelevant.
     */
    public void transitionToIntentReviewLayout() {
        mCaptureLayout.setVisibility(View.GONE);

        mMode = MODE_INTENT_REVIEW;
    }

    /**
     * @return whether UI is in intent review mode
     */
    public boolean isInIntentReview() {
        return mMode == MODE_INTENT_REVIEW;
    }


    private void setOverlayBottomBar(boolean overlay) {
        mOverLayBottomBar = overlay;
        if (overlay) {
            setBackgroundAlpha(mBackgroundAlphaOverlay);
        } else {
            setBackgroundAlpha(mBackgroundAlphaDefault);
        }
    }

    /**
     * Sets a capture layout helper to query layout rect from.
     */
    public void setCameraAppUI(CameraAppUI cameraAppUI) {
        mCameraAppUI = cameraAppUI;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int measureHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (measureWidth == 0 || measureHeight == 0) {
            return;
        }

        if (mCameraAppUI == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            Log.e(TAG, "Capture layout helper needs to be set first.");
        } else {

            Rect bottomBarRect = mCameraAppUI.getBottomBarRect();
            super.onMeasure(MeasureSpec.makeMeasureSpec(
                                (int) bottomBarRect.width(), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec((int) bottomBarRect.height(), MeasureSpec.EXACTLY)
                           );
            boolean shouldOverlayBottomBar = mCameraAppUI.shouldOverlayBottomBar();
            setOverlayBottomBar(shouldOverlayBottomBar);
        }
    }

    // prevent touches on bottom bar (not its children)
    // from triggering a touch event on preview area
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
        setPaintColor(mBackgroundAlpha, mBackgroundColor);
    }

    private void setBackgroundPressedColor(int color) {
        if (ApiHelper.isLOrHigher()) {
            // not supported (setting a color on a RippleDrawable is hard =[ )
        } else {
            mBackgroundPressedColor = color;
        }
    }

    private LayerDrawable applyCircleDrawableToShutterBackground(LayerDrawable shutterBackground) {
        // the background for video has a circle_item drawable placeholder
        // that gets replaced by an AnimatedCircleDrawable for the cool
        // shrink-down-to-a-circle effect
        // all other modes need not do this replace
        Drawable d = shutterBackground.findDrawableByLayerId(R.id.circle_item);
        if (d != null) {
            Drawable animatedCircleDrawable =
                new AnimatedCircleDrawable((int) mCircleRadius);
            animatedCircleDrawable.setLevel(DRAWABLE_MAX_LEVEL);
            shutterBackground
            .setDrawableByLayerId(R.id.circle_item, animatedCircleDrawable);
        }

        return shutterBackground;
    }

    private LayerDrawable newDrawableFromConstantState(Drawable.ConstantState constantState) {
        return (LayerDrawable) constantState.newDrawable(getContext().getResources());
    }

    private void setupShutterBackgroundForModeIndex(int index) {
        Log.d(TAG, "----->> setupShutterBackgroundForModeIndex start index = " + index);
        LayerDrawable shutterBackground = applyCircleDrawableToShutterBackground(
                                              newDrawableFromConstantState(mShutterButtonBackgroundConstantStates[index]));
        Log.d(TAG, "---->> setupShutterBackgroundForModeIndex end index = " + index);
        //not need background color
        //mShutterButton.setBackground(shutterBackground);

        Drawable d = shutterBackground.getDrawable(0);
        mAnimatedCircleDrawable = null;
        mColorDrawable = null;
        if (d instanceof AnimatedCircleDrawable) {
            mAnimatedCircleDrawable = (AnimatedCircleDrawable) d;
        } else if (d instanceof ColorDrawable) {
            mColorDrawable = (ColorDrawable) d;
        }

        int colorId = CameraUtil.getCameraThemeColorId(index, getContext());
        int pressedColor = getContext().getResources().getColor(colorId);
        setBackgroundPressedColor(pressedColor);
        refreshPaintColor();
    }

    public void setColorsForModeIndex(int index) {
        setupShutterBackgroundForModeIndex(index);
    }

    public void setBackgroundAlpha(int alpha) {
        mBackgroundAlpha = alpha;
        setPaintColor(mBackgroundAlpha, mBackgroundColor);
    }

    /**
     * Sets the shutter button enabled if true, disabled if false.
     * <p>
     * Disabled means that the shutter button is not clickable and is greyed
     * out.
     */
    public void setShutterButtonEnabled(final boolean enabled) {
        mShutterButton.post(new Runnable() {
            @Override
            public void run() {
                mShutterButton.setEnabled(enabled);
                setShutterButtonImportantToA11y(enabled);
            }
        });
    }

    /**
     * Sets whether shutter button should be included in a11y announcement and
     * navigation
     */
    public void setShutterButtonImportantToA11y(boolean important) {
        if (important) {
            mShutterButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        } else {
            mShutterButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    /**
     * Returns whether the capture button is enabled.
     */
    public boolean isShutterButtonEnabled() {
        return mShutterButton.isEnabled();
    }

    private TransitionDrawable crossfadeDrawable(Drawable from, Drawable to) {
        Drawable[] arrayDrawable = new Drawable[2];
        arrayDrawable[0] = from;
        arrayDrawable[1] = to;
        TransitionDrawable transitionDrawable = new TransitionDrawable(arrayDrawable);
        transitionDrawable.setCrossFadeEnabled(true);
        return transitionDrawable;
    }

    /**
     * Sets the shutter button's icon resource. By default, all drawables
     * instances loaded from the same resource share a common state; if you
     * modify the state of one instance, all the other instances will receive
     * the same modification. In order to modify properties of this icon
     * drawable without affecting other drawables, here we use a mutable
     * drawable which is guaranteed to not share states with other drawables.
     */
    public void setShutterButtonIcon(int resId) {
        Drawable iconDrawable = getResources().getDrawable(resId);
        if (iconDrawable != null) {
            iconDrawable = iconDrawable.mutate();
        }
        mShutterButton.setImageDrawable(iconDrawable);
    }

    /**
     * Animates bar to a single stop button
     */
    public void animateToVideoStop(int resId) {
        if (mOverLayBottomBar && mAnimatedCircleDrawable != null) {
            mAnimatedCircleDrawable.animateToSmallRadius();
            mDrawCircle = true;
        }

        TransitionDrawable transitionDrawable = crossfadeDrawable(
                mShutterButton.getDrawable(),
                getResources().getDrawable(resId));
        mShutterButton.setImageDrawable(transitionDrawable);
        transitionDrawable.startTransition(CIRCLE_ANIM_DURATION_MS);
    }

    /**
     * Animates bar to full width / length with video capture icon
     */
    public void animateToFullSize(int resId) {
        if (mDrawCircle && mAnimatedCircleDrawable != null) {
            mAnimatedCircleDrawable.animateToFullSize();
            mDrawCircle = false;
        }

        TransitionDrawable transitionDrawable = crossfadeDrawable(
                mShutterButton.getDrawable(),
                getResources().getDrawable(resId));
        mShutterButton.setImageDrawable(transitionDrawable);
        transitionDrawable.startTransition(CIRCLE_ANIM_DURATION_MS);
    }
}
