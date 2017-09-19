package com.westalgo.factorycamera.app;

import android.app.Application;

import com.westalgo.factorycamera.settings.SettingsManager;
import com.westalgo.factorycamera.settings.ProjectConfig;

/**
 * The Camera application class containing important services and functionality
 * to be used across modules.
 */
public class CameraApp extends Application  {

    private SettingsManager mSettingsManager;


    static {
        //System.loadLibrary("dcs_verify");
        //System.loadLibrary("opencv_java3");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSettingsManager = new SettingsManager(this);
        ProjectConfig.init(this);
    }

    public SettingsManager getSettingsManager() {
        return mSettingsManager;
    }

}
