/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.cyanogenmod.settings.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.cyanogenmod.settings.device.R;

public class ScreenFragmentActivity extends PreferenceFragment implements OnPreferenceChangeListener {

    private static final String PREF_ENABLED = "1";
    private static final String TAG = "GalaxyS2Settings_Screen";

    private static final String FILE_TOUCHKEY_NOTIFICATION = "/sys/class/sec/sec_touchkey/notification";
    private static final String FILE_TOUCHKEY_LED_MODE = "/sys/class/sec/sec_touchkey/led_mode";
    private CABC mCABC;
    private PanelGamma mPanelGamma;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.screen_preferences);
        PreferenceScreen prefSet = getPreferenceScreen();

        mCABC = (CABC) findPreference(DeviceSettings.KEY_CABC);
        mCABC.setEnabled(CABC.isSupported());

        mPanelGamma = (PanelGamma) findPreference(DeviceSettings.KEY_PANEL_GAMMA);
        mPanelGamma.setEnabled(mPanelGamma.isSupported());

        final ListPreference modePref = (ListPreference)findPreference(DeviceSettings.KEY_TOUCHKEY_LED_MODE);
        int mode = Integer.parseInt(modePref.getValue());
        modePref.setOnPreferenceChangeListener(this);

          if (mode > 0) {
            prefSet.findPreference(DeviceSettings.KEY_TOUCHKEY_TIMEOUT).setEnabled(true);
        } else {
            prefSet.findPreference(DeviceSettings.KEY_TOUCHKEY_TIMEOUT).setEnabled(false);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Utils.writeValue(FILE_TOUCHKEY_LED_MODE, (String) newValue);
        PreferenceScreen prefSet = getPreferenceScreen();

        final String val = newValue.toString();
        final ListPreference modePref = (ListPreference)findPreference(DeviceSettings.KEY_TOUCHKEY_LED_MODE);
        int mode = modePref.findIndexOfValue(val);

        if (mode > 0) {
            Utils.writeValue(FILE_TOUCHKEY_NOTIFICATION, ("0"));
            prefSet.findPreference(DeviceSettings.KEY_TOUCHKEY_TIMEOUT).setEnabled(true);
        } else {
            Utils.writeValue(FILE_TOUCHKEY_NOTIFICATION, ("0"));
            prefSet.findPreference(DeviceSettings.KEY_TOUCHKEY_TIMEOUT).setEnabled(false);
        }

                return true;
    }

    public static boolean isSupported(String FILE) {
        return Utils.fileExists(FILE);
    }

    public static void restore(Context context) {
        if (!isSupported(FILE_TOUCHKEY_LED_MODE)) {
            return;
        }

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        Utils.writeValue(FILE_TOUCHKEY_LED_MODE, sharedPrefs.getString(DeviceSettings.KEY_TOUCHKEY_LED_MODE, "1"));
    }
}
