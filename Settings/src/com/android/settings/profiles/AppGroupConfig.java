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

package com.android.settings.profiles;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationGroup;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import cyanogenmod.app.ProfileManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.cyanogenmod.PackageListAdapter;
import com.android.settings.cyanogenmod.PackageListAdapter.PackageItem;

import org.cyanogenmod.internal.logging.CMMetricsLogger;

public class AppGroupConfig extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static String TAG = "AppGroupConfig";

    private static final int DIALOG_APPS = 0;

    private static final int DELETE_CONFIRM = 1;

    private static final int DELETE_GROUP_CONFIRM = 2;

    public static final String PROFILE_SERVICE = "profile";

    private ListView mListView;

    private PackageManager mPackageManager;

    private NotificationGroup mNotificationGroup;

    private ProfileManager mProfileManager;

    private NamePreference mNamePreference;

    private static final int MENU_DELETE = Menu.FIRST;

    private static final int MENU_ADD = Menu.FIRST + 1;

    private PackageListAdapter mAppAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mPackageToDelete = savedInstanceState.getString("package_delete");
        }

        mProfileManager = ProfileManager.getInstance(getActivity());
        addPreferencesFromResource(R.xml.application_list);

        final Bundle args = getArguments();
        if (args != null) {
            mNotificationGroup = (NotificationGroup) args.getParcelable("NotificationGroup");
            mPackageManager = getPackageManager();
            mAppAdapter = new PackageListAdapter(getActivity());

            updatePackages();

            setHasOptionsMenu(true);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem delete = menu.add(0, MENU_DELETE, 0, R.string.profile_menu_delete_title)
                .setIcon(R.drawable.ic_menu_trash_holo_dark);
        delete.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        MenuItem addApplication = menu.add(0, MENU_ADD, 0, R.string.profiles_add)
                .setIcon(R.drawable.ic_menu_add)
                .setAlphabeticShortcut('a');
        addApplication.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_DELETE:
                deleteNotificationGroup();
                return true;
            case MENU_ADD:
                addNewApp();
                return true;
            default:
                return false;
        }
    }

    Preference mAddPreference;

    Preference mDeletePreference;

    private void updatePackages() {
        PreferenceScreen prefSet = getPreferenceScreen();

        // Add the General section
        PreferenceGroup generalPrefs = (PreferenceGroup) prefSet.findPreference("general_section");
        if (generalPrefs != null) {
            generalPrefs.removeAll();

            // Name preference
            mNamePreference = new NamePreference(getActivity(), mNotificationGroup.getName());
            mNamePreference.setOnPreferenceChangeListener(this);
            generalPrefs.addPreference(mNamePreference);
        }

        PreferenceGroup applicationsList = (PreferenceGroup) prefSet.findPreference("applications_list");
        if (applicationsList != null) {
            applicationsList.removeAll();
            for (String pkg : mNotificationGroup.getPackages()) {
                Preference pref = new Preference(getActivity());
                try {
                    PackageInfo group = mPackageManager.getPackageInfo(pkg, 0);
                    pref.setKey(group.packageName);
                    pref.setTitle(group.applicationInfo.loadLabel(mPackageManager));
                    Drawable icon = group.applicationInfo.loadIcon(mPackageManager);
                    pref.setIcon(icon);
                    pref.setSelectable(true);
                    pref.setPersistent(false);
                    applicationsList.addPreference(pref);
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.add(0, R.string.profile_menu_delete_title, 0, R.string.profile_menu_delete_title);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo aMenuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
        PackageItem selectedGroup = (PackageItem) mListView.getItemAtPosition(aMenuInfo.position);
        switch (item.getItemId()) {
            case R.string.profile_menu_delete_title:
                deleteAppFromGroup(selectedGroup);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteAppFromGroup(PackageItem selectedGroup) {
        if (selectedGroup != null) {
            mNotificationGroup.removePackage(selectedGroup.packageName);
            updatePackages();
        }
    }

    @Override
    protected int getMetricsCategory() {
        return CMMetricsLogger.APP_GROUP_CONFIG;
    }

    @Override
    public void onPause() {
        if (mNotificationGroup != null) {
            mProfileManager.addNotificationGroup(mNotificationGroup);
        }
        super.onPause();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNamePreference) {
            String name = mNamePreference.getName().toString();
            if (!name.equals(mNotificationGroup.getName())) {
                if (!mProfileManager.notificationGroupExists(name)) {
                    mNotificationGroup.setName(name);
                } else {
                    mNamePreference.setName(mNotificationGroup.getName());
                    Toast.makeText(getActivity(), R.string.duplicate_appgroup_name, Toast.LENGTH_LONG).show();
                }
            }
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof Preference) {
            String deleteItem = preference.getKey();
            removeApp(deleteItem);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void addNewApp() {
        showDialog(DIALOG_APPS);
        // TODO: switch to using the built in app list rather than dialog box?
    }

    private void removeApp(String key) {
        mPackageToDelete = key.toString();
        showDialog(DELETE_CONFIRM);
    }

    private void deleteNotificationGroup() {
        showDialog(DELETE_GROUP_CONFIRM);
    }

    @Override
    public Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final Dialog dialog;
        switch (id) {
            case DIALOG_APPS:
                final ListView list = new ListView(getActivity());
                list.setAdapter(mAppAdapter);
                builder.setTitle(R.string.profile_choose_app);
                builder.setView(list);
                dialog = builder.create();
                list.setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        PackageItem info = (PackageItem) parent.getItemAtPosition(position);
                        mNotificationGroup.addPackage(info.packageName);
                        updatePackages();
                        dialog.cancel();
                    }
                });
                break;
            case DELETE_CONFIRM:
                builder.setMessage(R.string.profile_app_delete_confirm);
                builder.setTitle(R.string.profile_menu_delete_title);
                builder.setIconAttribute(android.R.attr.alertDialogIcon);
                builder.setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                doDelete();
                            }
                        });
                builder.setNegativeButton(android.R.string.no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                dialog = builder.create();
                break;
            case DELETE_GROUP_CONFIRM:
                builder.setMessage(R.string.profile_delete_appgroup);
                builder.setTitle(R.string.profile_menu_delete_title);
                builder.setIconAttribute(android.R.attr.alertDialogIcon);
                builder.setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mProfileManager.removeNotificationGroup(mNotificationGroup);
                                mNotificationGroup = null;
                                finish();
                            }
                        });
                builder.setNegativeButton(android.R.string.no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                dialog = builder.create();
                break;
            default:
                dialog = null;
        }
        return dialog;
    }

    String mPackageToDelete;

    private void doDelete() {
        mNotificationGroup.removePackage(mPackageToDelete);
        updatePackages();
    }

    @Override
    public void onSaveInstanceState(Bundle in) {
        super.onSaveInstanceState(in);
        in.putString("package_delete", mPackageToDelete);
    }
}