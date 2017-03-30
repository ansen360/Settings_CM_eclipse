/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.dashboard.DashboardTile;

public class ProfileSelectDialog extends DialogFragment implements OnClickListener {

    private static final String ARG_SELECTED_TILE = "selectedTile";

    private DashboardTile mSelectedTile;

    public static void show(FragmentManager manager, DashboardTile tile) {
        ProfileSelectDialog dialog = new ProfileSelectDialog();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SELECTED_TILE, tile);
        dialog.setArguments(args);
        dialog.show(manager, "select_profile");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSelectedTile = getArguments().getParcelable(ARG_SELECTED_TILE);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        UserAdapter adapter = Utils.createUserAdapter(UserManager.get(context), context,
                mSelectedTile.userHandle);
        builder.setTitle(R.string.choose_profile)
                .setAdapter(adapter, this);

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        UserHandle user = mSelectedTile.userHandle.get(which);
        getActivity().startActivityAsUser(mSelectedTile.intent, user);
    }
}
