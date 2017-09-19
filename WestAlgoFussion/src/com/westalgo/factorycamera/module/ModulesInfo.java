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

package com.westalgo.factorycamera.module;

import android.content.Context;

import com.westalgo.factorycamera.R;
import com.westalgo.factorycamera.debug.Log;

import com.westalgo.factorycamera.settings.ProjectConfig;

/**
 * A class holding the module information and registers them to
 * {@link com.westalgo.factorycamera.module.ModuleManager}.
 */
public class ModulesInfo {
    private static final Log.Tag TAG = new Log.Tag("ModulesInfo");

    /**
     * when change registerModule order, we should also change below value
     */
    public static final int VERIFY_MODE = 0;
    public static final int DUAL_CAMERA_MODE = 1;
    public static final int PHOTO_MODE = 2;

    public static void setupModules(Context context, ModuleManager moduleManager) {
        int defaultModuleId = context.getResources().getInteger(R.integer.camera_mode_verify);
        moduleManager.setDefaultModuleIndex(defaultModuleId);

        registerVerifyModule(moduleManager, context.getResources().getInteger(R.integer.camera_mode_verify));
        if (!ProjectConfig.VERIFY_MODE_ONLY) {
            registerDualCameraModule(moduleManager, context.getResources().getInteger(R.integer.camera_mode_dualcamera));
            registerPhotoModule(moduleManager, context.getResources().getInteger(R.integer.camera_mode_photo));
        }
    }

    private static void registerPhotoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public ModuleController createModule() {
                return new PhotoModule();
            }
        });
    }

    private static void registerVerifyModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public ModuleController createModule() {
                return new DualVerifyModule();
            }
        });
    }

    private static void registerDualCameraModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public ModuleController createModule() {
                return new DualCameraModule();
            }
        });
    }

}
