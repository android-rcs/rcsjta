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

package com.orangelabs.rcs.ri.intents;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Call each Intents
 * 
 * @author Jean-Marc AUFFRET
 */
public class TestIntentApps extends Activity {

    /**
     * A locker to exit only once
     */
    private LockAccess mExitOnce = new LockAccess();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.intents_apps);

        // Set button callback
        Button btn = (Button) findViewById(R.id.load_chat);
        btn.setOnClickListener(btnListener);
        btn = (Button) findViewById(R.id.load_ft);
        btn.setOnClickListener(btnListener);
        btn = (Button) findViewById(R.id.load_group_chat);
        btn.setOnClickListener(btnListener);
        btn = (Button) findViewById(R.id.initiate_ft);
        btn.setOnClickListener(btnListener);
        btn = (Button) findViewById(R.id.initiate_group_chat);
        btn.setOnClickListener(btnListener);
        btn = (Button) findViewById(R.id.initiate_chat);
        btn.setOnClickListener(btnListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Button callback
     */
    private OnClickListener btnListener = new OnClickListener() {
        public void onClick(View v) {
            if (v.getId() == R.id.load_chat) {
                try {
                    Intent intent = new Intent(
                            com.gsma.services.rcs.Intents.Chat.ACTION_VIEW_ONE_TO_ONE_CHAT);
                    startActivity(intent);
                } catch (Exception e) {
                    Utils.showMessageAndExit(TestIntentApps.this,
                            getString(R.string.label_intent_failed), mExitOnce, e);
                }
            } else
            if (v.getId() == R.id.initiate_chat) {
                try {
                    Intent intent = new Intent(
                            com.gsma.services.rcs.Intents.Chat.ACTION_SEND_ONE_TO_ONE_CHAT_MESSAGE);
                    startActivity(intent);
                } catch (Exception e) {
                    Utils.showMessageAndExit(TestIntentApps.this,
                            getString(R.string.label_intent_failed), mExitOnce, e);
                }
            } else
            if (v.getId() == R.id.load_group_chat) {
                try {
                    Intent intent = new Intent(
                            com.gsma.services.rcs.Intents.Chat.ACTION_VIEW_GROUP_CHAT);
                    startActivity(intent);
                } catch (Exception e) {
                    Utils.showMessageAndExit(TestIntentApps.this,
                            getString(R.string.label_intent_failed), mExitOnce, e);
                }
            } else
            if (v.getId() == R.id.initiate_group_chat) {
                try {
                    Intent intent = new Intent(
                            com.gsma.services.rcs.Intents.Chat.ACTION_INITIATE_GROUP_CHAT);
                    startActivity(intent);
                } catch (Exception e) {
                    Utils.showMessageAndExit(TestIntentApps.this,
                            getString(R.string.label_intent_failed), mExitOnce, e);
                }
            } else
            if (v.getId() == R.id.load_ft) {
                try {
                    Intent intent = new Intent(
                            com.gsma.services.rcs.Intents.FileTransfer.ACTION_VIEW_FILE_TRANSFER);
                    startActivity(intent);
                } catch (Exception e) {
                    Utils.showMessageAndExit(TestIntentApps.this,
                            getString(R.string.label_intent_failed), mExitOnce, e);
                }
            } else
            if (v.getId() == R.id.initiate_ft) {
                try {
                    Intent intent = new Intent(
                            com.gsma.services.rcs.Intents.FileTransfer.ACTION_INITIATE_ONE_TO_ONE_FILE_TRANSFER);
                    startActivity(intent);
                } catch (Exception e) {
                    Utils.showMessageAndExit(TestIntentApps.this,
                            getString(R.string.label_intent_failed), mExitOnce, e);
                }
            }
        }
    };
}
