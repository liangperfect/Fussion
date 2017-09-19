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

package com.westalgo.factorycamera.settings;

import android.content.Context;

import com.westalgo.factorycamera.R;
import com.westalgo.factorycamera.debug.Log;

/**
 * Keys is a class for storing SharedPreferences keys and configuring
 * their defaults.
 *
 * For each key that has a default value and set of possible values, it
 * stores those defaults so they can be used by the SettingsManager
 * on lookup.  This step is optional, and it can be done anytime before
 * a setting is accessed by the SettingsManager API.
 */
public class Keys {

    private static final Log.Tag TAG = new Log.Tag("Keys");

    public static final String KEY_PICTURE_SIZE_BACK_MAIN = "pref_camera_picturesize_back_main_key";
    public static final String KEY_PICTURE_SIZE_BACK_SUB = "pref_camera_picturesize_back_sub_key";
    public static final String KEY_PICTURE_SIZE_FRONT = "pref_camera_picturesize_front_key";
    public static final String KEY_JPEG_QUALITY = "pref_camera_jpegquality_key";
    public static final String KEY_FOCUS_MODE = "pref_camera_focusmode_key";
    public static final String KEY_FLASH_MODE = "pref_camera_flashmode_key";
    public static final String KEY_EXPOSURE = "pref_camera_exposure_key";
    public static final String KEY_CAMERA_ID = "pref_camera_id_key";

    //ISO
    public static final String KEY_ISO = "pref_camera_iso_key";
    public static final String KEY_ISO_ENABLED = "pref_camera_iso_enabled_key";
    // public static final String KEY_CONTINUOUS_ISO = "continuous-iso";
    public static final String KEY_AVAILABLE_ISO = "iso-values";
    public static final String KEY_CURRENT_ISO = "iso";
    public static final String KEY_CURRENT_ISO_VALUE = "cur-iso";

    //ZSL
    public static final String KEY_ZSL = "zsl";

    //ZSD
    public static final String KEY_ZSD = "zsd-mode";
    public static final String KEY_MTK_CAM_MODE = "mtk-cam-mode";

    public static final String KEY_CAMERA_FIRST_USE_HINT_SHOWN = "pref_camera_first_use_hint_shown_key";
    public static final String KEY_STARTUP_MODULE_INDEX = "camera.startup_module";
    public static final String KEY_CAMERA_MODULE_LAST_USED = "pref_camera_module_last_used_index";
    public static final String KEY_RELEASE_DIALOG_LAST_SHOWN_VERSION = "pref_release_dialog_last_shown_version";
    public static final String KEY_FLASH_SUPPORTED_BACK_CAMERA = "pref_flash_supported_back_camera";
    public static final String KEY_EXPOSURE_COMPENSATION_ENABLED = "pref_camera_exposure_compensation_key";

    public static final String KEY_USER_SELECTED_ASPECT_RATIO_BACK = "pref_user_selected_aspect_ratio_back";
    public static final String KEY_USER_SELECTED_ASPECT_RATIO_FRONT = "pref_user_selected_aspect_ratio_front";
    public static final String KEY_USER_SELECTED_SUBCAMEAR_IMAGE_FORMAT = "pref_user_selected_subcamera_image_format";

    public static final String KEY_DUALCAM_MODE = "dualcam-mode";

    public static final String KEY_AUTOEXPOSURE = "auto-exposure";
    public static final String KEY_SAF = "pref_camera_saf_key";
    public static final String KEY_AF = "pref_camera_af_key";
    public static final String KEY_AE = "pref_camera_ae_key";
    public static final String KEY_FRAME = "pref_camera_frame_key";
    public static final String KEY_PREF_ZSL = "pref_camera_zsl_key";
    public static final String KEY_OIS = "pref_camera_ois_key";
    //nature correction setting key
    public static final String KEY_NATURE_CORRECTION = "pref_camera_nature_correction_key";
    public static final String KEY_CAMERA_CORRECTION = "pref_camera_correction_key";

    /**
     * Set some number of defaults for the defined keys.
     * It's not necessary to set all defaults.
     */
    public static void setDefaults(SettingsManager settingsManager, Context context) {

        settingsManager.setDefaults(KEY_CAMERA_ID,
                                    context.getString(R.string.pref_camera_id_default),
                                    context.getResources().getStringArray(R.array.camera_id_entryvalues));

        settingsManager.setDefaults(KEY_FLASH_MODE,
                                    context.getString(R.string.pref_camera_flashmode_default),
                                    context.getResources().getStringArray(R.array.pref_camera_flashmode_entryvalues));

        settingsManager.setDefaults(KEY_CAMERA_FIRST_USE_HINT_SHOWN, true);

        settingsManager.setDefaults(KEY_FOCUS_MODE,
                                    context.getString(R.string.pref_camera_focusmode_default),
                                    context.getResources().getStringArray(R.array.pref_camera_focusmode_entryvalues));

        settingsManager.setDefaults(KEY_JPEG_QUALITY,
                                    context.getString(R.string.pref_camera_jpeg_quality_normal),
                                    context.getResources().getStringArray(
                                        R.array.pref_camera_jpeg_quality_entryvalues));

        settingsManager.setDefaults(KEY_STARTUP_MODULE_INDEX, 0,
                                    context.getResources().getIntArray(R.array.camera_modes));

        settingsManager.setDefaults(KEY_CAMERA_MODULE_LAST_USED,
                                    context.getResources().getInteger(R.integer.camera_mode_verify),
                                    context.getResources().getIntArray(R.array.camera_modes));

        settingsManager.setDefaults(KEY_USER_SELECTED_ASPECT_RATIO_BACK, false);
        settingsManager.setDefaults(KEY_USER_SELECTED_ASPECT_RATIO_FRONT, false);
        //set nature correction on by default
        settingsManager.setDefaults(KEY_NATURE_CORRECTION, true);

        // set ZSL on by default
        settingsManager.setDefaults(KEY_PREF_ZSL, true);
        // Weather all platform support exposure ??
        // I don't know how to query weather support exposure
        setManualExposureCompensation(settingsManager, true);

        setManualISO(settingsManager, true);

    }

    /** Helper functions for some defined keys. */

    /**
     * Returns whether the camera has been set to back facing in settings.
     */
    public static boolean isCameraBackFacing(SettingsManager settingsManager, String moduleScope) {
        return settingsManager.isDefault(moduleScope, KEY_CAMERA_ID);
    }


    /**
     * Sets the manual exposure compensation enabled setting
     * to on/off based on the given argument.
     */
    public static void setManualExposureCompensation(SettingsManager settingsManager, boolean on) {
        Log.d(TAG, "setManualExposureCompensation enabled: "+on);
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, KEY_EXPOSURE_COMPENSATION_ENABLED, on);
    }

    public static void setManualISO(SettingsManager settingsManager, boolean isEnabled){
        Log.d(TAG, "setManualISO enabled: "+isEnabled);
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, KEY_ISO_ENABLED, isEnabled);
    }

}

