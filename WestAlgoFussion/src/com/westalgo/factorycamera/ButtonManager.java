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

package com.westalgo.factorycamera;

import android.view.View;
import android.widget.ImageButton;

import com.westalgo.factorycamera.R;
import com.westalgo.factorycamera.app.AppController;
import com.westalgo.factorycamera.app.CameraAppUI;
import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.settings.Keys;
import com.westalgo.factorycamera.settings.SettingsManager;
import com.westalgo.factorycamera.ui.ModeOptions;
import com.westalgo.factorycamera.ui.MultiToggleImageButton;
import com.westalgo.factorycamera.ui.RadioOptions;

/**
 * A  class for generating pre-initialized
 * {@link #android.widget.ImageButton}s.
 */
public class ButtonManager implements SettingsManager.OnSettingChangedListener {

    private static final Log.Tag TAG = new Log.Tag("ButtonManager");
    public static final int BUTTON_FLASH = 0;
    public static final int BUTTON_CAMERA = 1;
    public static final int BUTTON_EXPOSURE_COMPENSATION = 2;
    public static final int BUTTON_ISO = 3;
    public static final int a =1;
    /** For two state MultiToggleImageButtons, the off index. */
    public static final int OFF = 0;
    /** For two state MultiToggleImageButtons, the on index. */
    public static final int ON = 1;

    /** A reference to the application's settings manager. */
    private final SettingsManager mSettingsManager;

    /** Bottom bar options toggle buttons. */
    private MultiToggleImageButton mButtonCamera;
    private MultiToggleImageButton mButtonFlash;


    private ImageButton mButtonExposureCompensation;
    private ImageButton mButtonISO;
    private ImageButton mExposureN2;
    private ImageButton mExposureN1;
    private ImageButton mExposure0;
    private ImageButton mExposureP1;
    private ImageButton mExposureP2;
    private RadioOptions mModeOptionsExposure;
    private View mModeOptionsButtons;
    private ModeOptions mModeOptions;

    private int mMinExposureCompensation;
    private int mMaxExposureCompensation;
    private float mExposureCompensationStep;

    /** A listener for button enabled and visibility
        state changes. */
    private ButtonStatusListener mListener;


    private final AppController mAppController;

    /**
     * Get a new global ButtonManager.
     */
    public ButtonManager(AppController app) {
        mAppController = app;

        mSettingsManager = app.getSettingsManager();
        mSettingsManager.addListener(this);
    }

    /**
     * Load references to buttons under a root View.
     * Call this after the root clears/reloads all of its children
     * to prevent stale references button views.
     */
    public void load(View root) {
        getButtonsReferences(root);
    }

    /**
     * ButtonStatusListener provides callbacks for when button's
     * visibility changes and enabled status changes.
     */
    public interface ButtonStatusListener {
        /**
         * A button's visibility has changed.
         */
        public void onButtonVisibilityChanged(ButtonManager buttonManager, int buttonId);

        /**
         * A button's enabled state has changed.
         */
        public void onButtonEnabledChanged(ButtonManager buttonManager, int buttonId);
    }

    /**
     * Sets the ButtonStatusListener.
     */
    public void setListener(ButtonStatusListener listener) {
        mListener = listener;
    }

    /**
     * Gets references to all known buttons.
     */
    private void getButtonsReferences(View root) {
        mButtonCamera
        = (MultiToggleImageButton) root.findViewById(R.id.camera_toggle_button);
        mButtonFlash
        = (MultiToggleImageButton) root.findViewById(R.id.flash_toggle_button);

        mButtonExposureCompensation = (ImageButton) root.findViewById(R.id.exposure_button);
        mButtonISO = (ImageButton) root.findViewById(R.id.iso_button);

        mExposureN2 = (ImageButton) root.findViewById(R.id.exposure_n2);
        mExposureN1 = (ImageButton) root.findViewById(R.id.exposure_n1);
        mExposure0 = (ImageButton) root.findViewById(R.id.exposure_0);
        mExposureP1 = (ImageButton) root.findViewById(R.id.exposure_p1);
        mExposureP2 = (ImageButton) root.findViewById(R.id.exposure_p2);
        mModeOptionsExposure = (RadioOptions) root.findViewById(R.id.mode_options_exposure);
        mModeOptionsButtons = root.findViewById(R.id.mode_options_buttons);
        mModeOptions = (ModeOptions) root.findViewById(R.id.mode_options);

    }

    @Override
    public void onSettingChanged(SettingsManager settingsManager, String key) {
        MultiToggleImageButton button = null;
        int index = 0;

        if (key.equals(Keys.KEY_FLASH_MODE)) {
            index = mSettingsManager.getIndexOfCurrentValue(mAppController.getModuleScope(),
                    Keys.KEY_FLASH_MODE);
            button = getButtonOrError(BUTTON_FLASH);
        } else if (key.equals(Keys.KEY_CAMERA_ID)) {
            index = mSettingsManager.getIndexOfCurrentValue(mAppController.getModuleScope(),
                    Keys.KEY_CAMERA_ID);
            button = getButtonOrError(BUTTON_CAMERA);
        } else if (key.equals(Keys.KEY_EXPOSURE)) {
            updateExposureButtons();
        } else if (key.equals(Keys.KEY_ISO)) {
            //TODO
        }

        if (button != null && button.getState() != index) {
            button.setState(Math.max(index, 0), false);
        }
    }

    /**
     * A callback executed in the state listener of a button.
     *
     * Used by a module to set specific behavior when a button's
     * state changes.
     */
    public interface ButtonCallback {
        public void onStateChanged(int state);
    }

    /**
     * Returns the appropriate {@link com.westalgo.factorycamera.MultiToggleImageButton}
     * based on button id.  An IllegalStateException will be throw if the
     * button could not be found in the view hierarchy.
     */
    private MultiToggleImageButton getButtonOrError(int buttonId) {
        switch (buttonId) {
        case BUTTON_FLASH:
            if (mButtonFlash == null) {
                throw new IllegalStateException("Flash button could not be found.");
            }
            return mButtonFlash;
        case BUTTON_CAMERA:
            if (mButtonCamera == null) {
                throw new IllegalStateException("Camera button could not be found.");
            }
            return mButtonCamera;

        default:
            throw new IllegalArgumentException("button not known by id=" + buttonId);
        }
    }

    /**
     * Returns the appropriate {@link android.widget.ImageButton}
     * based on button id.  An IllegalStateException will be throw if the
     * button could not be found in the view hierarchy.
     */
    private ImageButton getImageButtonOrError(int buttonId) {
        switch (buttonId) {
        case BUTTON_EXPOSURE_COMPENSATION:
            if (mButtonExposureCompensation == null) {
                throw new IllegalStateException("Exposure Compensation button could not be found.");
            }
            return mButtonExposureCompensation;
        case BUTTON_ISO:
            if (mButtonISO == null) {
                throw new IllegalStateException("ISO button could not be found.");
            }
            return mButtonISO;
        default:
            throw new IllegalArgumentException("button not known by id=" + buttonId);
        }
    }

    /**
     * Initialize a known button by id, with a state change callback and
     * a resource id that points to an array of drawables, and then enable
     * the button.
     */
    public void initializeButton(int buttonId, ButtonCallback cb) {
        MultiToggleImageButton button = getButtonOrError(buttonId);
        switch (buttonId) {
        case BUTTON_FLASH:
            initializeFlashButton(button, cb, R.array.camera_flashmode_icons);
            break;
        case BUTTON_CAMERA:
            initializeCameraButton(button, cb, R.array.camera_id_icons);
            break;
        default:
            throw new IllegalArgumentException("button not known by id=" + buttonId);
        }

        enableButton(buttonId);
    }

    /**
     * Initialize a known button with a click listener and a resource id.
     * Sets the button visible.
     */
    public void initializePushButton(int buttonId, View.OnClickListener cb,
                                     int imageId) {
        ImageButton button = getImageButtonOrError(buttonId);
        button.setOnClickListener(cb);
        button.setImageResource(imageId);

        if (!button.isEnabled()) {
            button.setEnabled(true);
            if (mListener != null) {
                mListener.onButtonEnabledChanged(this, buttonId);
            }
        }
        button.setTag(R.string.tag_enabled_id, buttonId);

        if (button.getVisibility() != View.VISIBLE) {
            button.setVisibility(View.VISIBLE);
            if (mListener != null) {
                mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    /**
     * Initialize a known button with a click listener. Sets the button visible.
     */
    public void initializePushButton(int buttonId, View.OnClickListener cb) {
        ImageButton button = getImageButtonOrError(buttonId);
        if (cb != null) {
            button.setOnClickListener(cb);
        }

        if (!button.isEnabled()) {
            button.setEnabled(true);
            if (mListener != null) {
                mListener.onButtonEnabledChanged(this, buttonId);
            }
        }
        button.setTag(R.string.tag_enabled_id, buttonId);

        if (button.getVisibility() != View.VISIBLE) {
            button.setVisibility(View.VISIBLE);
            if (mListener != null) {
                mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    /**
     * Sets a button in its disabled (greyed out) state.
     */
    public void disableButton(int buttonId) {
        MultiToggleImageButton button = getButtonOrError(buttonId);

        if (button.isEnabled()) {
            button.setEnabled(false);
            if (mListener != null) {
                mListener.onButtonEnabledChanged(this, buttonId);
            }
        }
        button.setTag(R.string.tag_enabled_id, null);

        if (button.getVisibility() != View.VISIBLE) {
            button.setVisibility(View.VISIBLE);
            if (mListener != null) {
                mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    /**
     * Enables a button that has already been initialized.
     */
    public void enableButton(int buttonId) {
        ImageButton button = getButtonOrError(buttonId);
        if (!button.isEnabled()) {
            button.setEnabled(true);
            if (mListener != null) {
                mListener.onButtonEnabledChanged(this, buttonId);
            }
        }
        button.setTag(R.string.tag_enabled_id, buttonId);

        if (button.getVisibility() != View.VISIBLE) {
            button.setVisibility(View.VISIBLE);
            if (mListener != null) {
                mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    /**
     * Disable click reactions for a button without affecting visual state.
     * For most cases you'll want to use {@link #disableButton(int)}.
     * @param buttonId The id of the button.
     */
    public void disableButtonClick(int buttonId) {
        ImageButton button = getButtonOrError(buttonId);
        if (button instanceof MultiToggleImageButton) {
            ((MultiToggleImageButton) button).setClickEnabled(false);
        }
    }

    /**
     * Enable click reactions for a button without affecting visual state.
     * For most cases you'll want to use {@link #enableButton(int)}.
     * @param buttonId The id of the button.
     */
    public void enableButtonClick(int buttonId) {
        ImageButton button = getButtonOrError(buttonId);
        if (button instanceof MultiToggleImageButton) {
            ((MultiToggleImageButton) button).setClickEnabled(true);
        }
    }

    /**
     * Hide a button by id.
     */
    public void hideButton(int buttonId) {
        View button;
        try {
            button = getButtonOrError(buttonId);
        } catch (IllegalArgumentException e) {
            button = getImageButtonOrError(buttonId);
        }
        if (button.getVisibility() == View.VISIBLE) {
            button.setVisibility(View.GONE);
            if (mListener != null) {
                mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    public void setToInitialState() {
        mModeOptions.setMainBar(ModeOptions.BAR_STANDARD);
    }

    public void setExposureCompensationCallback(final CameraAppUI.BottomBarUISpec
            .ExposureCompensationSetCallback cb) {
        if (cb == null) {
            mModeOptionsExposure.setOnOptionClickListener(null);
        } else {
            mModeOptionsExposure
            .setOnOptionClickListener(new RadioOptions.OnOptionClickListener() {
                @Override
                public void onOptionClicked(View v) {
                    int comp = Integer.parseInt((String)(v.getTag()));
                    if (mExposureCompensationStep != 0.0f) {
                        int compValue =Math.round(comp / mExposureCompensationStep);
                        cb.setExposure(compValue);
                    }
                }
            });
        }
    }

    public void setISOSettingCallback(final CameraAppUI.BottomBarUISpec.ISOSetCallback isoCb){
        if (isoCb == null) {
            mModeOptions.setISOClickListener(null);
        } else {
            mModeOptions.setISOClickListener(new ModeOptions.OnISOOptionClickListener() {
                @Override
                public void onISOOptionClicked(String value) {
                    if (value != null && value != "") {
                        isoCb.setISOValue(value);
                    }
                }
            });
        }
    }

    /**
     * Set the exposure compensation parameters supported by the current camera mode.
     * @param min Minimum exposure compensation value.
     * @param max Maximum exposure compensation value.
     * @param step Expsoure compensation step value.
     */
    public void setExposureCompensationParameters(int min, int max, float step) {
        mMaxExposureCompensation = max;
        mMinExposureCompensation = min;
        mExposureCompensationStep = step;


        setVisible(mExposureN2, (Math.round(min * step) <= -2));
        setVisible(mExposureN1, (Math.round(min * step) <= -1));
        setVisible(mExposureP1, (Math.round(max * step) >= 1));
        setVisible(mExposureP2, (Math.round(max * step) >= 2));

        updateExposureButtons();
    }

    private static void setVisible(View v, boolean visible) {
        if (visible) {
            v.setVisibility(View.VISIBLE);
        } else {
            v.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * @return The exposure compensation step value.
     **/
    public float getExposureCompensationStep() {
        return mExposureCompensationStep;
    }

    /**
     * Check if a button is enabled with the given button id..
     */
    public boolean isEnabled(int buttonId) {
        View button;
        try {
            button = getButtonOrError(buttonId);
        } catch (IllegalArgumentException e) {
            button = getImageButtonOrError(buttonId);
        }

        Integer enabledId = (Integer) button.getTag(R.string.tag_enabled_id);
        if (enabledId != null) {
            return (enabledId.intValue() == buttonId) && button.isEnabled();
        } else {
            return false;
        }
    }

    /**
     * Check if a button is visible.
     */
    public boolean isVisible(int buttonId) {
        View button;
        try {
            button = getButtonOrError(buttonId);
        } catch (IllegalArgumentException e) {
            button = getImageButtonOrError(buttonId);
        }
        return (button.getVisibility() == View.VISIBLE);
    }

    /**
     * Initialize a flash button.
     */
    private void initializeFlashButton(MultiToggleImageButton button,
                                       final ButtonCallback cb, int resIdImages) {

        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.camera_flash_descriptions);

        int index = mSettingsManager.getIndexOfCurrentValue(mAppController.getModuleScope(),
                    Keys.KEY_FLASH_MODE);
        button.setState(index >= 0 ? index : 0, false);

        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                mSettingsManager.setValueByIndex(mAppController.getModuleScope(),
                                                 Keys.KEY_FLASH_MODE, state);
                if (cb != null) {
                    cb.onStateChanged(state);
                }
            }
        });
    }


    /**
     * Update the visual state of the manual exposure buttons
     */
    public void updateExposureButtons() {
        int compValue = mSettingsManager.getInteger(mAppController.getModuleScope(),
                        Keys.KEY_EXPOSURE);
        if (mExposureCompensationStep != 0.0f) {
            int comp = Math.round(compValue * mExposureCompensationStep);
            mModeOptionsExposure.setSelectedOptionByTag(String.valueOf(comp));
        }
    }

    public void updateISOSettings() {
        String value = mSettingsManager.getString(mAppController.getModuleScope(), Keys.KEY_ISO);
        if (value != null && value != "") {
            mModeOptions.initISOSettings(value);
        }
    }
    /**
     * Initialize a camera button.
     */
    private void initializeCameraButton(final MultiToggleImageButton button,
                                        final ButtonCallback cb, int resIdImages) {

        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }

        int index = mSettingsManager.getIndexOfCurrentValue(mAppController.getModuleScope(),
                    Keys.KEY_CAMERA_ID);
        button.setState(index >= 0 ? index : 0, false);

        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                mSettingsManager.setValueByIndex(mAppController.getModuleScope(),
                                                 Keys.KEY_CAMERA_ID, state);
                int cameraId = mSettingsManager.getInteger(mAppController.getModuleScope(),
                               Keys.KEY_CAMERA_ID);
                // This is a quick fix for ISE in Gcam module which can be
                // found by rapid pressing camera switch button. The assumption
                // here is that each time this button is clicked, the listener
                // will do something and then enable this button again.
                button.setEnabled(false);
                if (cb != null) {
                    cb.onStateChanged(cameraId);
                }
                mAppController.getCameraAppUI().onChangeCamera();
            }
        });
    }


}
