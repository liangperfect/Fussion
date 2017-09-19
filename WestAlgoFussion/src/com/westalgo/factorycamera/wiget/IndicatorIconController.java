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

package com.westalgo.factorycamera.wiget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import com.westalgo.factorycamera.R;
import com.westalgo.factorycamera.ButtonManager;
import com.westalgo.factorycamera.app.AppController;
import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.settings.Keys;
import com.westalgo.factorycamera.settings.SettingsManager;

/**
 * IndicatorIconController sets the visibility and icon state of
 * on screen indicators.
 *
 * Indicators are only visible if they are in a non-default state.  The
 * visibility of an indicator is set when an indicator's setting changes.
 */
public class IndicatorIconController
    implements SettingsManager.OnSettingChangedListener,
    ButtonManager.ButtonStatusListener {

    private final static Log.Tag TAG = new Log.Tag("IndicatorIconCtrlr");

    private ImageView mFlashIndicator;

    private ImageView mExposureIndicatorN2;
    private ImageView mExposureIndicatorN1;
    private ImageView mExposureIndicatorP1;
    private ImageView mExposureIndicatorP2;

    private ImageView mISOIndicator;

    private TypedArray mFlashIndicatorPhotoIcons;

    private AppController mController;

    public IndicatorIconController(AppController controller, View root) {
        mController = controller;
        Context context = controller.getAndroidContext();

        mFlashIndicator = (ImageView) root.findViewById(R.id.flash_indicator);
        mFlashIndicatorPhotoIcons = context.getResources().obtainTypedArray(
                                        R.array.camera_flashmode_indicator_icons);

        mExposureIndicatorN2 = (ImageView) root.findViewById(R.id.exposure_n2_indicator);
        mExposureIndicatorN1 = (ImageView) root.findViewById(R.id.exposure_n1_indicator);
        mExposureIndicatorP1 = (ImageView) root.findViewById(R.id.exposure_p1_indicator);
        mExposureIndicatorP2 = (ImageView) root.findViewById(R.id.exposure_p2_indicator);

        mISOIndicator = (ImageView) root.findViewById(R.id.iso_button);
    }

    @Override
    public void onButtonVisibilityChanged(ButtonManager buttonManager, int buttonId) {
        syncIndicatorWithButton(buttonId);
    }

    @Override
    public void onButtonEnabledChanged(ButtonManager buttonManager, int buttonId) {
        syncIndicatorWithButton(buttonId);
    }

    /**
     * Syncs a specific indicator's icon and visibility
     * based on the enabled state and visibility of a button.
     */
    private void syncIndicatorWithButton(int buttonId) {
        switch (buttonId) {
        case ButtonManager.BUTTON_FLASH: {
            syncFlashIndicator();
            break;
        }
        case ButtonManager.BUTTON_EXPOSURE_COMPENSATION: {
            syncExposureIndicator();
            break;
        }
        default:
            // Do nothing.  The indicator doesn't care
            // about button that don't correspond to indicators.
        }
    }

    /**
     * Sets all indicators to the correct resource and visibility
     * based on the current settings.
     */
    public void syncIndicators() {
        syncFlashIndicator();
        syncExposureIndicator();
    }

    /**
     * If the new visibility is different from the current visibility
     * on a view, change the visibility and call any registered
     * {@link OnIndicatorVisibilityChangedListener}.
     */
    private static void changeVisibility(View view, int visibility) {
        if (view.getVisibility() != visibility) {
            view.setVisibility(visibility);
        }
    }

    /**
     * Sync the icon and visibility of the flash indicator.
     */
    private void syncFlashIndicator() {
        ButtonManager buttonManager = mController.getButtonManager();
        // If flash isn't an enabled and visible option,
        // do not show the indicator.
        if (buttonManager.isEnabled(ButtonManager.BUTTON_FLASH)
                && buttonManager.isVisible(ButtonManager.BUTTON_FLASH)) {

            setIndicatorState(mController.getModuleScope(),
                              Keys.KEY_FLASH_MODE, mFlashIndicator,
                              mFlashIndicatorPhotoIcons, false);
        } else {
            changeVisibility(mFlashIndicator, View.GONE);
        }
    }


    private void syncExposureIndicator() {
        if (mExposureIndicatorN2 == null
                || mExposureIndicatorN1 == null
                || mExposureIndicatorP1 == null
                || mExposureIndicatorP2 == null) {
            Log.w(TAG, "Trying to sync exposure indicators that are not initialized.");
            return;
        }


        // Reset all exposure indicator icons.
        changeVisibility(mExposureIndicatorN2, View.GONE);
        changeVisibility(mExposureIndicatorN1, View.GONE);
        changeVisibility(mExposureIndicatorP1, View.GONE);
        changeVisibility(mExposureIndicatorP2, View.GONE);

        ButtonManager buttonManager = mController.getButtonManager();
        if (buttonManager.isEnabled(ButtonManager.BUTTON_EXPOSURE_COMPENSATION)
                && buttonManager.isVisible(ButtonManager.BUTTON_EXPOSURE_COMPENSATION)) {

            int compValue = mController.getSettingsManager().getInteger(
                                mController.getModuleScope(), Keys.KEY_EXPOSURE);
            int comp = Math.round(compValue * buttonManager.getExposureCompensationStep());

            // Turn on the appropriate indicator.
            switch (comp) {
            case -2:
                changeVisibility(mExposureIndicatorN2, View.VISIBLE);
                break;
            case -1:
                changeVisibility(mExposureIndicatorN1, View.VISIBLE);
                break;
            case 0:
                // Do nothing.
                break;
            case 1:
                changeVisibility(mExposureIndicatorP1, View.VISIBLE);
                break;
            case 2:
                changeVisibility(mExposureIndicatorP2, View.VISIBLE);
            }
        }
    }


    /**
     * Sets the image resource and visibility of the indicator
     * based on the indicator's corresponding setting state.
     */
    private void setIndicatorState(String scope, String key, ImageView imageView,
                                   TypedArray iconArray, boolean showDefault) {
        SettingsManager settingsManager = mController.getSettingsManager();

        int valueIndex = settingsManager.getIndexOfCurrentValue(scope, key);
        if (valueIndex < 0) {
            // This can happen when the setting is camera dependent
            // and the camera is not yet open.  CameraAppUI.onChangeCamera()
            // will call this again when the camera is open.
            Log.w(TAG, "The setting for this indicator is not available.");
            imageView.setVisibility(View.GONE);
            return;
        }
        Drawable drawable = iconArray.getDrawable(valueIndex);
        if (drawable == null) {
            throw new IllegalStateException("Indicator drawable is null.");
        }
        imageView.setImageDrawable(drawable);

        // Set the indicator visible if not in default state.
        boolean visibilityChanged = false;
        if (!showDefault && settingsManager.isDefault(scope, key)) {
            changeVisibility(imageView, View.GONE);
        } else {
            changeVisibility(imageView, View.VISIBLE);
        }
    }

    @Override
    public void onSettingChanged(SettingsManager settingsManager, String key) {
        if (key.equals(Keys.KEY_FLASH_MODE)) {
            syncFlashIndicator();
            return;
        }
        if (key.equals(Keys.KEY_EXPOSURE)) {
            syncExposureIndicator();
            return;
        }
        if (key.equals(Keys.KEY_ISO)) {
            //TODO
        }
    }

}
