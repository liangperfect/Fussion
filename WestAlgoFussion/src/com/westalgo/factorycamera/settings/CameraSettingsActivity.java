package com.westalgo.factorycamera.settings;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;

import com.westalgo.factorycamera.R;
import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.app.CameraApp;
import com.westalgo.factorycamera.util.CameraUtil;
import com.westalgo.factorycamera.settings.SettingsManager;

public class CameraSettingsActivity extends FragmentActivity {
    private static final Log.Tag TAG = new Log.Tag("CameraSettingsActivity");
    private static SettingsManager settingsManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsManager = ((CameraApp)(this.getApplication())).getSettingsManager();
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.mode_settings);

        CameraSettingsFragment dialog = new CameraSettingsFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content, dialog).commit();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        return true;
    }

    public static class CameraSettingsFragment extends PreferenceFragment implements
                                        SharedPreferences.OnSharedPreferenceChangeListener {

        public CameraSettingsFragment(){
            /*
             * We need void constructor to avoid below error
             * android.app.Fragment$InstantiationException: Unable to instantiate fragment
             * com.westalgo.factorycamera.settings.CameraSettingsActivity$CameraSettingsFragment:
             * make sure class name exists, is public, and has an empty constructor that is public
             */
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.camera_preferences);
            PreferenceManager.getDefaultSharedPreferences(getContext())
                            .registerOnSharedPreferenceChangeListener(this);
            setDefaultSummary();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            PreferenceManager.getDefaultSharedPreferences(getContext())
                            .unregisterOnSharedPreferenceChangeListener(this);
        }


        private void changeSummary(String key, String newValue) {
            switch (key) {
                case Keys.KEY_CAMERA_CORRECTION:
                    ListPreference mCorrectionPreference = (ListPreference) findPreference(key);
                    CharSequence[] entries = mCorrectionPreference.getEntries();
                    int mCorrectionMode = mCorrectionPreference.findIndexOfValue(newValue);
                    mCorrectionPreference.setSummary(entries[mCorrectionMode]);
                    break;
                default:
                    break;
            }
        }

        private void setDefaultSummary() {
            ListPreference mCorrectionPreference =
                    (ListPreference) findPreference(Keys.KEY_CAMERA_CORRECTION);
            CharSequence[] entries = mCorrectionPreference.getEntries();
            int mCorrectionMode = mCorrectionPreference
                    .findIndexOfValue(mCorrectionPreference.getValue());
            mCorrectionPreference.setSummary(entries[mCorrectionMode]);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            boolean switchIsOn;
            switch (key) {
                case Keys.KEY_AE:
                    switchIsOn = SettingsManager.convertToBoolean(
                                        sharedPreferences.getString(key, "false"));
                    CameraUtil.execCommand(CameraUtil.SYS_DUALCA_AE_SWITCH, switchIsOn);
                    break;
                case Keys.KEY_FRAME:
                    switchIsOn = SettingsManager.convertToBoolean(
                                        sharedPreferences.getString(key, "false"));
                    CameraUtil.execCommand(CameraUtil.SYS_DUALCA_FRAME_SWITCH, switchIsOn);
                    break;
                case Keys.KEY_CAMERA_CORRECTION:
                    changeSummary(key, sharedPreferences.getString(key, "false"));
                    break;
                default:
                    break;
            }
        }
    }
}
