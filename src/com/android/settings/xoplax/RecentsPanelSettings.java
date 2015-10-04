/*
 * Copyright (C) 2015 XoplaX OS Project
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

package com.android.settings.xoplax;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.Gravity;
import android.util.Log;

import com.android.settings.R;
import java.util.List;

import com.android.settings.xoplax.util.Helpers;
import com.android.internal.util.xoplax.OmniSwitchConstants;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class RecentsPanelSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {
    private static final String TAG = "RecentsPanelSettings";

    private static final String RECENT_MENU_CLEAR_ALL = "recent_menu_clear_all";
    private static final String RECENT_MENU_CLEAR_ALL_LOCATION = "recent_menu_clear_all_location";
    private static final String RECENT_RAM_BAR = "recents_ram_bar";

    private static final String RECENTS_USE_OMNISWITCH = "recents_use_omniswitch";
    private static final String OMNISWITCH_START_SETTINGS = "omniswitch_start_settings";

    private static final String RECENTS_USE_SLIM = "recents_use_slim";
    private static final String RECENT_PANEL_SHOW_TOPMOST = "recent_panel_show_topmost";
    private static final String RECENT_PANEL_LEFTY_MODE = "recent_panel_lefty_mode";
    private static final String RECENT_PANEL_SCALE = "recent_panel_scale";
    private static final String RECENT_PANEL_EXPANDED_MODE = "recent_panel_expanded_mode";
    private static final String RECENT_PANEL_BG_COLOR = "recent_panel_bg_color";
    private static final String RECENT_CARD_BG_COLOR = "recent_card_bg_color";
    private static final String RECENT_CARD_TEXT_COLOR = "recent_card_text_color";

    // Package name of the omnniswitch app
    public static final String OMNISWITCH_PACKAGE_NAME = "org.omnirom.omniswitch";

    // Intent for launching the omniswitch settings actvity
    public static Intent INTENT_OMNISWITCH_SETTINGS = new Intent(Intent.ACTION_MAIN)
         .setClassName(OMNISWITCH_PACKAGE_NAME, OMNISWITCH_PACKAGE_NAME + ".SettingsActivity");

    private CheckBoxPreference mRecentClearAll;
    private CheckBoxPreference mRecentsUseOmniSwitch;
    private CheckBoxPreference mRecentsUseSlim;
    private CheckBoxPreference mRecentsShowTopmost;
    private CheckBoxPreference mRecentPanelLeftyMode;
    private ColorPickerPreference mRecentPanelBgColor;
    private ColorPickerPreference mRecentCardBgColor;
    private ColorPickerPreference mRecentCardTextColor;
    private ListPreference mRecentPanelScale;
    private ListPreference mRecentPanelExpandedMode;
    private ListPreference mRecentClearAllPosition;
    private Preference mOmniSwitchSettings;
    private Preference mRecentRamBar;

    private boolean mOmniSwitchStarted;

    private static final int DEFAULT_BACKGROUND_COLOR = 0x00ffffff;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.recents_panel_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        boolean useOmniSwitch = false;
        boolean useSlimRecents = false;

        int intColor;
        String hexColor;

        useOmniSwitch = Settings.XOPLAX.getInt(getContentResolver(), Settings.XOPLAX.RECENTS_USE_OMNISWITCH, 0) == 1
                            && isOmniSwitchServiceRunning();
        useSlimRecents = Settings.XOPLAX.getInt(getContentResolver(), Settings.XOPLAX.RECENTS_USE_SLIM, 0) == 1;

        // OmniSwitch
        mRecentsUseOmniSwitch = (CheckBoxPreference) prefSet.findPreference(RECENTS_USE_OMNISWITCH);
        mRecentsUseOmniSwitch.setChecked(useOmniSwitch);
        mRecentsUseOmniSwitch.setOnPreferenceChangeListener(this);
        mRecentsUseOmniSwitch.setEnabled(!useSlimRecents);

        mOmniSwitchSettings = (Preference) prefSet.findPreference(OMNISWITCH_START_SETTINGS);
        mOmniSwitchSettings.setEnabled(useOmniSwitch);

        // Default recents
        mRecentClearAll = (CheckBoxPreference) prefSet.findPreference(RECENT_MENU_CLEAR_ALL);
        mRecentClearAll.setChecked(Settings.XOPLAX.getInt(resolver,
            Settings.XOPLAX.SHOW_CLEAR_RECENTS_BUTTON, 1) == 1);
        mRecentClearAll.setOnPreferenceChangeListener(this);
        mRecentClearAll.setEnabled(!useOmniSwitch && !useSlimRecents);
        mRecentClearAllPosition = (ListPreference) prefSet.findPreference(RECENT_MENU_CLEAR_ALL_LOCATION);
        String recentClearAllPosition = Settings.XOPLAX.getString(resolver, Settings.XOPLAX.CLEAR_RECENTS_BUTTON_LOCATION);
        if (recentClearAllPosition != null) {
             mRecentClearAllPosition.setValue(recentClearAllPosition);
        }
        mRecentClearAllPosition.setOnPreferenceChangeListener(this);
        mRecentClearAllPosition.setEnabled(!useOmniSwitch && !useSlimRecents);

        // Slim recents
        mRecentsUseSlim = (CheckBoxPreference) prefSet.findPreference(RECENTS_USE_SLIM);
        mRecentsUseSlim.setChecked(useSlimRecents);
        mRecentsUseSlim.setOnPreferenceChangeListener(this);
        mRecentsUseSlim.setEnabled(!useOmniSwitch);

        boolean enableRecentsShowTopmost = Settings.XOPLAX.getInt(getContentResolver(),
        Settings.XOPLAX.RECENT_PANEL_SHOW_TOPMOST, 0) == 1;
        mRecentsShowTopmost = (CheckBoxPreference) findPreference(RECENT_PANEL_SHOW_TOPMOST);
        mRecentsShowTopmost.setChecked(enableRecentsShowTopmost);
        mRecentsShowTopmost.setOnPreferenceChangeListener(this);

        mRecentPanelLeftyMode = (CheckBoxPreference) findPreference(RECENT_PANEL_LEFTY_MODE);
        mRecentPanelLeftyMode.setOnPreferenceChangeListener(this);

        mRecentPanelScale = (ListPreference) findPreference(RECENT_PANEL_SCALE);
        mRecentPanelScale.setOnPreferenceChangeListener(this);

        final boolean recentLeftyMode = Settings.XOPLAX.getInt(getContentResolver(),
                Settings.XOPLAX.RECENT_PANEL_GRAVITY, Gravity.RIGHT) == Gravity.LEFT;
        mRecentPanelLeftyMode.setChecked(recentLeftyMode);

        final int recentScale = Settings.XOPLAX.getInt(getContentResolver(),
                Settings.XOPLAX.RECENT_PANEL_SCALE_FACTOR, 100);
        mRecentPanelScale.setValue(recentScale + "");

        mRecentPanelExpandedMode = (ListPreference) findPreference(RECENT_PANEL_EXPANDED_MODE);
        mRecentPanelExpandedMode.setOnPreferenceChangeListener(this);
        final int recentExpandedMode = Settings.XOPLAX.getInt(getContentResolver(),
        Settings.XOPLAX.RECENT_PANEL_EXPANDED_MODE, 0);
        mRecentPanelExpandedMode.setValue(recentExpandedMode + "");

        // Recent panel background color
        mRecentPanelBgColor = (ColorPickerPreference) findPreference(RECENT_PANEL_BG_COLOR);
        mRecentPanelBgColor.setOnPreferenceChangeListener(this);
        intColor = Settings.XOPLAX.getInt(getContentResolver(),
                Settings.XOPLAX.RECENT_PANEL_BG_COLOR, 0x00ffffff);
        hexColor = String.format("#%08x", (0x00ffffff & intColor));
        mRecentPanelBgColor.setSummary(hexColor);
        mRecentPanelBgColor.setNewPreviewColor(intColor);

        // Recent card background color
        mRecentCardBgColor =
                (ColorPickerPreference) findPreference(RECENT_CARD_BG_COLOR);
        mRecentCardBgColor.setOnPreferenceChangeListener(this);
        final int intColorCard = Settings.XOPLAX.getInt(getContentResolver(),
                Settings.XOPLAX.RECENT_CARD_BG_COLOR, 0x00ffffff);
        String hexColorCard = String.format("#%08x", (0x00ffffff & intColorCard));
        mRecentCardBgColor.setSummary(hexColorCard);
        mRecentCardBgColor.setNewPreviewColor(intColorCard);

        // Recent card text color
        mRecentCardTextColor =
               (ColorPickerPreference) findPreference(RECENT_CARD_TEXT_COLOR);
        mRecentCardTextColor.setOnPreferenceChangeListener(this);
        final int intColorText = Settings.XOPLAX.getInt(getContentResolver(),
                Settings.XOPLAX.RECENT_CARD_TEXT_COLOR, 0x00ffffff);
        String hexColorText = String.format("#%08x", (0x00ffffff & intColorText));
        mRecentCardTextColor.setSummary(hexColorText);
        mRecentCardTextColor.setNewPreviewColor(intColorText);

        // Ram Bar
        mRecentRamBar = findPreference(RECENT_RAM_BAR);
        mRecentRamBar.setEnabled(!useOmniSwitch && !useSlimRecents);
        updateRamBarStatus();

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
      if (preference == mOmniSwitchSettings) {
            startActivity(INTENT_OMNISWITCH_SETTINGS);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mRecentClearAll) {
            boolean value = (Boolean) objValue;
            Settings.XOPLAX.putInt(resolver, Settings.XOPLAX.SHOW_CLEAR_RECENTS_BUTTON, value ? 1 : 0);
        } else if (preference == mRecentClearAllPosition) {
            String value = (String) objValue;
            Settings.XOPLAX.putString(resolver, Settings.XOPLAX.CLEAR_RECENTS_BUTTON_LOCATION, value);
        } else if (preference == mRecentsUseOmniSwitch) {
            boolean omniSwitchEnabled = (Boolean) objValue;

            // Give user information that OmniSwitch service is not running
            if (omniSwitchEnabled && !isOmniSwitchServiceRunning()) {
                openOmniSwitchFirstTimeWarning();
            }

            Settings.XOPLAX.putInt(getContentResolver(), Settings.XOPLAX.RECENTS_USE_OMNISWITCH, omniSwitchEnabled ? 1 : 0);

            // Update OmniSwitch UI components
            mRecentsUseOmniSwitch.setChecked(omniSwitchEnabled);
            mOmniSwitchSettings.setEnabled(omniSwitchEnabled);

            // Update default recents UI components
            mRecentClearAll.setEnabled(!omniSwitchEnabled);
            mRecentClearAllPosition.setEnabled(!omniSwitchEnabled);

            // Update Slim recents UI components
            mRecentsUseSlim.setEnabled(!omniSwitchEnabled);
        } else if (preference == mRecentsUseSlim) {
            boolean useSlimRecents = (Boolean) objValue;

            Settings.XOPLAX.putInt(getContentResolver(), Settings.XOPLAX.RECENTS_USE_SLIM,
                    useSlimRecents ? 1 : 0);

            // Give user information that Slim Recents needs restart SystemUI
            openSlimRecentsWarning();

            // Update OmniSwitch UI components
            mRecentsUseOmniSwitch.setEnabled(!useSlimRecents);
            mRecentsUseSlim.setChecked(useSlimRecents);

            // Update default recents UI components
            mRecentClearAll.setEnabled(!useSlimRecents);
            mRecentClearAllPosition.setEnabled(!useSlimRecents);
        } else if (preference == mRecentPanelScale) {
            int value = Integer.parseInt((String) objValue);
            Settings.XOPLAX.putInt(getContentResolver(),
                    Settings.XOPLAX.RECENT_PANEL_SCALE_FACTOR, value);
        } else if (preference == mRecentPanelLeftyMode) {
            Settings.XOPLAX.putInt(getContentResolver(),
                    Settings.XOPLAX.RECENT_PANEL_GRAVITY,
                    ((Boolean) objValue) ? Gravity.LEFT : Gravity.RIGHT);
        } else if (preference == mRecentPanelExpandedMode) {
            int value = Integer.parseInt((String) objValue);
                    Settings.XOPLAX.putInt(getContentResolver(),
                    Settings.XOPLAX.RECENT_PANEL_EXPANDED_MODE, value);
        } else if (preference == mRecentsShowTopmost) {
                    Settings.XOPLAX.putInt(getContentResolver(),
                    Settings.XOPLAX.RECENT_PANEL_SHOW_TOPMOST,
                    ((Boolean) objValue) ? 1 : 0);
        } else if (preference == mRecentPanelBgColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(objValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.XOPLAX.putInt(getContentResolver(),
                    Settings.XOPLAX.RECENT_PANEL_BG_COLOR, intHex);
        } else if (preference == mRecentCardBgColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(objValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.XOPLAX.putInt(getContentResolver(),
                    Settings.XOPLAX.RECENT_CARD_BG_COLOR, intHex);
        } else if (preference == mRecentCardTextColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(objValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.XOPLAX.putInt(getContentResolver(),
                    Settings.XOPLAX.RECENT_CARD_TEXT_COLOR, intHex);
        } else {
            return false;
        }

        return true;
    }

    private boolean isOmniSwitchServiceRunning() {
        String serviceName = "org.omnirom.omniswitch.SwitchService";
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void openOmniSwitchFirstTimeWarning() {
        new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.omniswitch_first_time_title))
            .setMessage(getResources().getString(R.string.omniswitch_first_time_message))
            .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            }).show();
    }

    private void openSlimRecentsWarning() {
        new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.slim_recents_warning_title))
            .setMessage(getResources().getString(R.string.slim_recents_warning_message))
            .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Helpers.restartSystemUI();
                }
            }).show();
    }

    private void updateRamBarStatus() {
        int ramBarMode = Settings.XOPLAX.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.XOPLAX.RECENTS_RAM_BAR_MODE, 0);
        if (ramBarMode != 0)
            mRecentRamBar.setSummary(getResources().getString(R.string.ram_bar_color_enabled));
        else
            mRecentRamBar.setSummary(getResources().getString(R.string.ram_bar_color_disabled));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateRamBarStatus();
    }

    @Override
    public void onPause() {
        super.onResume();
        updateRamBarStatus();
    }

}
