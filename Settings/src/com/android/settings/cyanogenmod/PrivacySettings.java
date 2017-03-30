/*
 * Copyright (C) 2014 The CyanogenMod Project
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

package com.android.settings.cyanogenmod;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.internal.telephony.util.BlacklistUtils;
import org.cyanogenmod.internal.logging.CMMetricsLogger;

/**
 * Privacy settings
 */
public class PrivacySettings extends SettingsPreferenceFragment {

    private static final String KEY_BLACKLIST = "blacklist";
    private static final String KEY_STATS = "cmstats";

    private PreferenceScreen mBlacklist;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.privacy_settings_cyanogenmod);

        mBlacklist = (PreferenceScreen) findPreference(KEY_BLACKLIST);

        // Add package manager to check if features are available
        PackageManager pm = getPackageManager();

        boolean isOwner = Utils.isUserOwner();
        if (!isOwner) {
            PreferenceScreen root = getPreferenceScreen();
            root.removePreference(findPreference(KEY_STATS));
        }

        // Determine options based on device telephony support
        if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) || !isOwner) {
            // No telephony, remove dependent options
            PreferenceScreen root = getPreferenceScreen();
            root.removePreference(mBlacklist);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return CMMetricsLogger.PRIVACY_SETTINGS;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBlacklistSummary();
    }

    private void updateBlacklistSummary() {
        if (BlacklistUtils.isBlacklistEnabled(getActivity())) {
            mBlacklist.setSummary(R.string.blacklist_summary);
        } else {
            mBlacklist.setSummary(R.string.blacklist_summary_disabled);
        }
    }

}
