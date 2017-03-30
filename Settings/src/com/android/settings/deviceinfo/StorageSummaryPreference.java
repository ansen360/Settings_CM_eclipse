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

package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.preference.Preference;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.settings.R;

public class StorageSummaryPreference extends Preference {
    private int mPercent = -1;

    public StorageSummaryPreference(Context context) {
        super(context);

        setLayoutResource(R.layout.storage_summary);
        setEnabled(false);
    }

    public void setPercent(int percent) {
        mPercent = percent;
    }

    @Override
    protected void onBindView(View view) {
        Resources res = getContext().getResources();
        final ProgressBar progress = (ProgressBar) view.findViewById(android.R.id.progress);
        if (mPercent != -1) {
            progress.setVisibility(View.VISIBLE);
            progress.setProgress(mPercent);
        } else {
            progress.setVisibility(View.GONE);
        }

        final TextView summary = (TextView) view.findViewById(android.R.id.summary);
        summary.setTextColor(res.getColor(R.color.storage_summary_used_text_color));

        super.onBindView(view);
    }
}
