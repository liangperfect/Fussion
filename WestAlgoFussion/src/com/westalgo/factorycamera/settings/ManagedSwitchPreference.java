package com.westalgo.factorycamera.settings;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.westalgo.factorycamera.R;
import com.westalgo.factorycamera.app.CameraApp;
import com.westalgo.factorycamera.debug.Log;

public class ManagedSwitchPreference extends SwitchPreference {
    private static final Log.Tag TAG = new Log.Tag("DualBaseCameraMod");
    public static final String CAMERA_4_3_RATIO_KEY = "4:3";
    public static final String CAMERA_16_9_RATIO_KEY = "16:9";
    Context mContext;

    public ManagedSwitchPreference(Context context) {
        super(context);
        if (mContext == null) {
            mContext = context;
        }
    }

    public ManagedSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (mContext == null) {
            mContext = context;
        }
    }

    public ManagedSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (mContext == null) {
            mContext = context;
        }
    }

    @Override
    protected void onBindView(View view) {

        super.onBindView(view);
        Switch theSwitch = findSwitchInChildviews((ViewGroup) view);
        if (theSwitch != null) {
            if (getKey().equals(Keys.KEY_USER_SELECTED_ASPECT_RATIO_BACK)
                    || getKey().equals(Keys.KEY_USER_SELECTED_ASPECT_RATIO_FRONT)) {
                theSwitch.setThumbResource(R.drawable.switch_front);
                theSwitch.setTrackResource(R.drawable.switch_background_text);
                theSwitch.setTextOn(mContext.getResources().getText(R.string.camera_photo_ratio_16x9));
                theSwitch.setTextOff(mContext.getResources().getText(R.string.camera_photo_ratio_4x3));
            } else if (getKey().equals(Keys.KEY_USER_SELECTED_SUBCAMEAR_IMAGE_FORMAT)) {
                theSwitch.setThumbResource(R.drawable.switch_front);
                theSwitch.setTrackResource(R.drawable.switch_background_text_image_format);
                theSwitch.setTextOn(mContext.getResources().getText(R.string.subcamera_image_format_NV21));
                theSwitch.setTextOff(mContext.getResources().getText(R.string.subcamera_image_format_jpeg));
            } else {
                theSwitch.setThumbResource(R.drawable.switch_green_front);
                theSwitch.setTrackResource(R.drawable.switch_green_background);
                theSwitch.setSwitchMinWidth(160);
                theSwitch.setShowText(false);
                theSwitch.setEnabled(false);
            }
        }
    }

    @Override
    public boolean getPersistedBoolean(boolean defaultReturnValue) {
        CameraApp cameraApp = getCameraApp();
        if (cameraApp == null) {
            // The context and app may not be initialized upon initial inflation of the
            // preference from XML. In that case return the default value.
            return defaultReturnValue;
        }
        SettingsManager settingsManager = cameraApp.getSettingsManager();
        if (settingsManager != null) {
            return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, getKey());
        } else {
            // If the SettingsManager is for some reason not initialized,
            // perhaps triggered by a monkey, return default value.
            return defaultReturnValue;
        }
    }

    @Override
    public boolean persistBoolean(boolean value) {
        CameraApp cameraApp = getCameraApp();
        if (cameraApp == null) {
            // The context may not be initialized upon initial inflation of the
            // preference from XML. In that case return false to note the value won't
            // be persisted.
            return false;
        }
        SettingsManager settingsManager = cameraApp.getSettingsManager();
        if (settingsManager != null) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, getKey(), value);
            return true;
        } else {
            // If the SettingsManager is for some reason not initialized,
            // perhaps triggered by a monkey, return false to note the value
            // was not persisted.
            return false;
        }
    }

    private Switch findSwitchInChildviews(ViewGroup view) {
        for (int i = 0; i < view.getChildCount(); i++) {
            View thisChildview = view.getChildAt(i);
            if (thisChildview instanceof Switch) {
                return (Switch) thisChildview;
            } else if (thisChildview instanceof ViewGroup) {
                Switch theSwitch = findSwitchInChildviews((ViewGroup) thisChildview);
                if (theSwitch != null)
                    return theSwitch;
            }
        }
        return null;
    }

    private CameraApp getCameraApp() {
        Context context = getContext();
        if (context instanceof Activity) {
            Application application = ((Activity) context).getApplication();
            if (application instanceof CameraApp) {
                return (CameraApp) application;
            }
        }
        return null;
    }

}
