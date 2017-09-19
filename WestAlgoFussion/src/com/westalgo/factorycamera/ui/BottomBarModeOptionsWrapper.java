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
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.westalgo.factorycamera.R;
import com.westalgo.factorycamera.app.CameraAppUI;
import com.westalgo.factorycamera.debug.Log;

/**
 * The goal of this class is to ensure mode options is always laid out to
 * the left of or above bottom bar in landscape or portrait respectively.
 * All the other children in this view group can be expected to be laid out
 * the same way as they are in a normal FrameLayout.
 */
public class BottomBarModeOptionsWrapper extends FrameLayout {

    private final static Log.Tag TAG = new Log.Tag("BottomBarWrapper");
    private View mModeOptionsOverlay;
    private View mBottomBar;
    private CameraAppUI mCameraAppUI;

    public BottomBarModeOptionsWrapper(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onFinishInflate() {
        mModeOptionsOverlay = findViewById(R.id.mode_options_overlay);
        mBottomBar = findViewById(R.id.bottom_bar);
    }

    /**
     * Sets a capture layout helper to query layout rect from.
     */
    public void setCameraAppUI(CameraAppUI cameraAppUI) {
        mCameraAppUI = cameraAppUI;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (mCameraAppUI == null) {
            Log.e(TAG, "CameraAppUI needs to be set first.");
            return;
        }
        Rect previewRect = mCameraAppUI.getPreviewRect();
        Rect bottomBarRect = mCameraAppUI.getBottomBarRect();

        mModeOptionsOverlay.layout(previewRect.left,  previewRect.top,
                                   previewRect.right,  previewRect.bottom);
        mBottomBar.layout(bottomBarRect.left,  bottomBarRect.top,
                          bottomBarRect.right,  bottomBarRect.bottom);
    }
}