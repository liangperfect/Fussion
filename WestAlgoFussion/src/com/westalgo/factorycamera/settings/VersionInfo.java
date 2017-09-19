
/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.dcs.verify.DCSVerify;

import com.westalgo.factorycamera.R;

public class VersionInfo extends PreferenceActivity {

    private static final String KEY_APP_VER = "app_version";
    private static final String KEY_VERIFY_VER = "verify_version";

    private Preference mAppVer;
    private Preference mVerifyVer;

    private String unKnownStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.version_info);
        mAppVer = findPreference(KEY_APP_VER);
        mVerifyVer = findPreference(KEY_VERIFY_VER);
        unKnownStr = getResources().getString(R.string.default_version);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String verifyVerStr = DCSVerify.getVersion();
        if (verifyVerStr != null) {
            mVerifyVer.setSummary(verifyVerStr);
        }
        PackageManager packageManager = this.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(this.getPackageName(), 0);
            String appVersion = packageInfo.versionName;
            if (appVersion != null) {
                mAppVer.setSummary(appVersion);
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            mAppVer.setSummary(unKnownStr);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
