/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settingslib.TetherUtil;

import java.util.ArrayList;

public class WifiApEnabler {
    private final Context mContext;
    private final SwitchPreference mSwitch;
    private final CharSequence mOriginalSummary;

    private WifiManager mWifiManager;
    private final IntentFilter mIntentFilter;

    ConnectivityManager mCm;
    private String[] mWifiRegexs;
    /* Indicates if we have to wait for WIFI_STATE_CHANGED intent */
    private boolean mWaitForWifiStateChange = false;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_AP_STATE, WifiManager.WIFI_AP_STATE_FAILED);
                if (state == WifiManager.WIFI_AP_STATE_FAILED) {
                    int reason = intent.getIntExtra(WifiManager.EXTRA_WIFI_AP_FAILURE_REASON,
                            WifiManager.SAP_START_FAILURE_GENERAL);
                    handleWifiApStateChanged(state, reason);
                } else {
                    handleWifiApStateChanged(state, WifiManager.SAP_START_FAILURE_GENERAL);
                }
             } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                if (mWaitForWifiStateChange == true) {
                    handleWifiStateChanged(intent.getIntExtra(
                            WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN));
                }
            } else if (ConnectivityManager.ACTION_TETHER_STATE_CHANGED.equals(action)) {
                ArrayList<String> available = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_AVAILABLE_TETHER);
                ArrayList<String> active = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ACTIVE_TETHER);
                ArrayList<String> errored = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ERRORED_TETHER);
                updateTetherState(available.toArray(), active.toArray(), errored.toArray());
            } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                enableWifiSwitch();
            }
        }
    };

    public WifiApEnabler(Context context, SwitchPreference switchPreference) {
        mContext = context;
        mSwitch = switchPreference;
        mOriginalSummary = switchPreference.getSummary();
        switchPreference.setPersistent(false);

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mCm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        mWifiRegexs = mCm.getTetherableWifiRegexs();

        mIntentFilter = new IntentFilter(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
    }

    public void resume() {
        mContext.registerReceiver(mReceiver, mIntentFilter);
        enableWifiSwitch();
    }

    public void pause() {
        mContext.unregisterReceiver(mReceiver);
    }

    private void enableWifiSwitch() {
        boolean isAirplaneMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        if(!isAirplaneMode) {
            mSwitch.setEnabled(true);
        } else {
            mSwitch.setSummary(mOriginalSummary);
            mSwitch.setEnabled(false);
        }
    }

    public void setSoftapEnabled(boolean enable) {
        int wifiSavedState = 0;
         /**
         * Check if we have to wait for the WIFI_STATE_CHANGED intent
         * before we re-enable the Checkbox.
         */
        if (!enable) {
            try {
                wifiSavedState = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.WIFI_SAVED_STATE);
            } catch (Settings.SettingNotFoundException e) {
                ;
            }
            /**
             * If Wi-Fi is turned of as part of SoftAp turn on process,
             * we need to restore, Wi-Fi state after SoftAp turn Off.
             * WIFI_SAVED_STATE inficates the state.
             */
            if (wifiSavedState == 1) {
                 mWaitForWifiStateChange = true;
            }
        }
        if (TetherUtil.setWifiTethering(enable, mContext)) {
            /* Disable here, enabled on receiving success broadcast */
            mSwitch.setEnabled(false);
        } else {
            mSwitch.setSummary(R.string.wifi_error);
        }

    }

    public void updateConfigSummary(WifiConfiguration wifiConfig) {
        String s = mContext.getString(
                com.android.internal.R.string.wifi_tether_configure_ssid_default);
        mSwitch.setSummary(String.format(
                    mContext.getString(R.string.wifi_tether_enabled_subtext),
                    (wifiConfig == null) ? s : wifiConfig.SSID));
    }

    private void updateTetherState(Object[] available, Object[] tethered, Object[] errored) {
        boolean wifiTethered = false;
        boolean wifiErrored = false;

        for (Object o : tethered) {
            String s = (String)o;
            for (String regex : mWifiRegexs) {
                if (s.matches(regex)) wifiTethered = true;
            }
        }
        for (Object o: errored) {
            String s = (String)o;
            for (String regex : mWifiRegexs) {
                if (s.matches(regex)) wifiErrored = true;
            }
        }

        if (wifiTethered) {
            WifiConfiguration wifiConfig = mWifiManager.getWifiApConfiguration();
            updateConfigSummary(wifiConfig);
        } else if (wifiErrored) {
            mSwitch.setSummary(R.string.wifi_error);
        }
    }

    private void handleWifiApStateChanged(int state, int reason) {
        switch (state) {
            case WifiManager.WIFI_AP_STATE_ENABLING:
                mSwitch.setSummary(R.string.wifi_tether_starting);
                mSwitch.setEnabled(false);
                break;
            case WifiManager.WIFI_AP_STATE_ENABLED:
                /**
                 * Summary on enable is handled by tether
                 * broadcast notice
                 */
                mSwitch.setChecked(true);
                /* Doesnt need the airplane check */
                mSwitch.setEnabled(true);
                break;
            case WifiManager.WIFI_AP_STATE_DISABLING:
                mSwitch.setSummary(R.string.wifi_tether_stopping);
                mSwitch.setChecked(false);
                mSwitch.setEnabled(false);
                break;
            case WifiManager.WIFI_AP_STATE_DISABLED:
                mSwitch.setChecked(false);
                mSwitch.setSummary(mOriginalSummary);
                if (mWaitForWifiStateChange == false) {
                    enableWifiSwitch();
                }
                break;
            default:
                mSwitch.setChecked(false);
                if (reason == WifiManager.SAP_START_FAILURE_NO_CHANNEL) {
                    mSwitch.setSummary(R.string.wifi_sap_no_channel_error);
                } else {
                    mSwitch.setSummary(R.string.wifi_error);
                }
                enableWifiSwitch();
        }
    }

     private void handleWifiStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLED:
            case WifiManager.WIFI_STATE_UNKNOWN:
                enableWifiSwitch();
                mWaitForWifiStateChange = false;
                break;
            default:
        }
    }
}
