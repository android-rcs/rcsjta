/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.ri.intents;

import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Call each Intents
 * 
 * @author Jean-Marc AUFFRET
 */
public class TestIntentApps extends RcsActivity {

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

    /**
     * Button callback
     */
    private OnClickListener btnListener = new OnClickListener() {
        public void onClick(View v) {
            try {
                if (v.getId() == R.id.load_chat) {
                    Intent intent = new Intent(
                            com.gsma.services.rcs.Intents.Chat.ACTION_VIEW_ONE_TO_ONE_CHAT);
                    startActivity(intent);

                } else if (v.getId() == R.id.initiate_chat) {
                    Intent intent = new Intent(
                            com.gsma.services.rcs.Intents.Chat.ACTION_SEND_ONE_TO_ONE_CHAT_MESSAGE);
                    startActivity(intent);

                } else if (v.getId() == R.id.load_group_chat) {
                    Intent intent = new Intent(
                            com.gsma.services.rcs.Intents.Chat.ACTION_VIEW_GROUP_CHAT);
                    startActivity(intent);

                } else if (v.getId() == R.id.initiate_group_chat) {
                    Intent intent = new Intent(
                            com.gsma.services.rcs.Intents.Chat.ACTION_INITIATE_GROUP_CHAT);
                    startActivity(intent);

                } else if (v.getId() == R.id.load_ft) {
                    Intent intent = new Intent(
                            com.gsma.services.rcs.Intents.FileTransfer.ACTION_VIEW_FILE_TRANSFER);
                    startActivity(intent);

                } else if (v.getId() == R.id.initiate_ft) {
                    Intent intent = new Intent(
                            com.gsma.services.rcs.Intents.FileTransfer.ACTION_INITIATE_ONE_TO_ONE_FILE_TRANSFER);
                    startActivity(intent);
                }

            } catch (ActivityNotFoundException e) {
                showMessageThenExit(R.string.label_intent_failed);
            }

        }
    };
}
