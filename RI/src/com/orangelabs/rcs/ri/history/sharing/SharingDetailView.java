/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.ri.history.sharing;

import com.orangelabs.rcs.ri.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.TextView;

/**
 * @author yplo6403
 */
public class SharingDetailView extends Activity {

    private final static String EXTRA_INFOS = "EXTRA_INFOS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.history_log_item_detail);

        SharingInfos infos = (SharingInfos) getIntent().getParcelableExtra(EXTRA_INFOS);
        ((TextView) findViewById(R.id.history_log_item_contact)).setText(infos.getContact());
        ((TextView) findViewById(R.id.history_log_item_filename)).setText(infos.getFilename());
        ((TextView) findViewById(R.id.history_log_item_size)).setText(infos.getFilesize());
        ((TextView) findViewById(R.id.history_log_item_state)).setText(infos.getSate());
        ((TextView) findViewById(R.id.history_log_item_direction)).setText(infos.getDirection());
        ((TextView) findViewById(R.id.history_log_item_date)).setText(infos.getTimestamp());
        ((TextView) findViewById(R.id.history_log_item_duration)).setText(infos.getDuration());
    }

    /**
     * Start activity
     * 
     * @param context
     * @param infos
     */
    public static void startActivity(Context context, SharingInfos infos) {
        Intent intent = new Intent(context, SharingDetailView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_INFOS, (Parcelable) infos);
        context.startActivity(intent);
    }
    

}
