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
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.westalgo.factorycamera.R;
import com.westalgo.factorycamera.app.CameraAppUI;
import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.ui.PreviewOverlay;
import com.westalgo.factorycamera.ui.ShutterButton;

/**
 * ModeOptionsOverlay is a FrameLayout which positions mode options in
 * in the bottom of the preview that is visible above the bottom bar.
 */
public class ModeOptionsOverlay extends FrameLayout
    implements PreviewOverlay.OnPreviewTouchedListener,
    ShutterButton.OnShutterButtonListener {

    private final static Log.Tag TAG = new Log.Tag("ModeOptionsOverlay");

    private static final int BOTTOMBAR_OPTIONS_TIMEOUT_MS = 2000;
    private final static int BOTTOM_RIGHT = Gravity.BOTTOM | Gravity.RIGHT;
    private final static int TOP_RIGHT = Gravity.TOP | Gravity.RIGHT;

    private ModeOptions mModeOptions;
    // need a reference to set the onClickListener and fix the layout gravity on orientation change
    private LinearLayout mModeOptionsToggle;
    // need a reference to fix the rotation on orientation change
    private ImageView mThreeDots;
    private CameraAppUI mCameraAppUI;
    private TextView mInProgressTV;

    public ModeOptionsOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Sets CameraAppUI to query layout rect from.
     */
    public void setCameraAppUI(CameraAppUI cameraAppUI) {
        mCameraAppUI = cameraAppUI;
    }

    public void showProgressView(boolean isShow) {
        if(isShow) {
            mInProgressTV.setVisibility(View.VISIBLE);
        } else {
            mInProgressTV.setVisibility(View.GONE);
        }
    }

    public void setToggleClickable(boolean clickable) {
        mModeOptionsToggle.setClickable(clickable);
    }

    @Override
    public void onFinishInflate() {
        mModeOptions = (ModeOptions) findViewById(R.id.mode_options);
        mModeOptions.setClickable(true);
        mModeOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeModeOptions();
            }
        });

        mModeOptionsToggle = (LinearLayout) findViewById(R.id.mode_options_toggle);
        mModeOptionsToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mModeOptions.animateVisible();
            }
        });
        mModeOptions.setViewToShowHide(mModeOptionsToggle);

        mThreeDots = (ImageView) findViewById(R.id.three_dots);
        mInProgressTV = (TextView) findViewById(R.id.in_progress_tv);
        showProgressView(false);
    }

    @Override
    public void onPreviewTouched(MotionEvent ev) {
        closeModeOptions();
    }

    @Override
    public void onShutterButtonClick() {
        closeModeOptions();
    }


    /**
     * Schedule (or re-schedule) the options menu to be closed after a number
     * of milliseconds.  If the options menu is already closed, nothing is
     * scheduled.
     */
    public void closeModeOptions() {
        mModeOptions.animateHidden();
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        checkOrientation(configuration.orientation);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mCameraAppUI == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            Log.e(TAG, "mCameraAppUI needs to be set first.");
        } else {
            Rect uncoveredPreviewRect = mCameraAppUI.getPreviewRect();
            super.onMeasure(MeasureSpec.makeMeasureSpec(
                                uncoveredPreviewRect.width(), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec( uncoveredPreviewRect.height(),
                                    MeasureSpec.EXACTLY)
                           );
        }
    }

    /**
     * Set the layout gravity of the child layout to be bottom or top right
     * depending on orientation.
     */
    private void checkOrientation(int orientation) {
        final boolean isPortrait = (Configuration.ORIENTATION_PORTRAIT == orientation);

        final int modeOptionsDimension = (int) getResources()
                                         .getDimension(R.dimen.mode_options_height);

        FrameLayout.LayoutParams modeOptionsParams
        = (FrameLayout.LayoutParams) mModeOptions.getLayoutParams();
        FrameLayout.LayoutParams modeOptionsToggleParams
        = (FrameLayout.LayoutParams) mModeOptionsToggle.getLayoutParams();

        if (isPortrait) {
            modeOptionsParams.height = modeOptionsDimension;
            modeOptionsParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            modeOptionsParams.gravity = Gravity.BOTTOM;

            modeOptionsToggleParams.gravity = BOTTOM_RIGHT;

            mThreeDots.setImageResource(R.drawable.ic_options_port);
        } else {
            modeOptionsParams.width = modeOptionsDimension;
            modeOptionsParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            modeOptionsParams.gravity = Gravity.RIGHT;

            modeOptionsToggleParams.gravity = TOP_RIGHT;

            mThreeDots.setImageResource(R.drawable.ic_options_land);
        }

        requestLayout();
    }
}
