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

package com.westalgo.factorycamera.app;

import com.westalgo.factorycamera.ButtonManager;
import com.westalgo.factorycamera.MediaSaveService;
import com.westalgo.factorycamera.module.ModuleController;
import com.westalgo.factorycamera.settings.SettingsManager;

import android.content.Context;
import android.view.View;

/**
 * The controller at app level.
 */
public interface AppController {



    /**
     * @return the {@link android.content.Context} being used.
     */
    public Context getAndroidContext();


    public CameraAppUI getCameraAppUI();
    /**
     * This gets called when mode is changed.
     *
     * @param moduleIndex index of the new module to switch to
     */
    public void onModeSelected(int moduleIndex);

    /**
     * This gets called when settings is selected and settings dialog needs to open.
     */
    public void onSettingsSelected();

    public int getCurrentModuleIndex();

    public ModuleController getCurrentModuleController();

    public SettingsManager getSettingsManager();


    /**
     * @return a String scope uniquely identifing the current module.
     */
    public String getModuleScope();

    /**
     * @return a String scope uniquely identifing the current camera id.
     */
    public String getCameraScope();

    /**
     * Returns the {@link com.westalgo.factorycamera.ButtonManager}.
     */
    public ButtonManager getButtonManager();

    public MediaSaveService getMediaSaveService();

    public void gotoGallery();

    public void onViewClicked(View v);
}
