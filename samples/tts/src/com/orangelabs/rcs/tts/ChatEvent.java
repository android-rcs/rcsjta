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

package com.orangelabs.rcs.tts;

import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.gsma.services.rcs.chat.ChatIntent;
import com.gsma.services.rcs.chat.ChatLog;

/**
 * Chat invitation event receiver
 * 
 * @author jexa7410
 */
public class ChatEvent extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Check activation state before to continue
        SharedPreferences preferences = context.getSharedPreferences(Registry.REGISTRY,
                Activity.MODE_PRIVATE);
        boolean flag = Registry.readBoolean(preferences, Registry.ACTIVATE_TTS, false);
        if (flag) {
            // Get the chat message ID from the Intent
            String msgId = intent.getParcelableExtra(ChatIntent.EXTRA_MESSAGE_ID);

            // Get the message content associated to the message ID from the database
            String message = null;
            Uri uri = ChatLog.Message.CONTENT_URI;
            String[] PROJECTION = new String[] {
                    ChatLog.Message.BODY
            };
            String where = ChatLog.Message.MESSAGE_ID + "='" + msgId + "'";
            Cursor cursor = context.getContentResolver().query(uri, PROJECTION, where, null, null);
            if ((cursor != null) && (cursor.getCount() > 0)) {
                message = cursor.getString(0);
            }

            if (!TextUtils.isEmpty(message)) {
                // Play TTS on the chat message
                ArrayList<String> messages = new ArrayList<String>();
                messages.add(context.getString(R.string.label_new_msg));
                messages.add(message);
                Intent serviceIntent = new Intent(context, PlayTextToSpeech.class);
                serviceIntent.putStringArrayListExtra("messages", messages);
                context.startService(serviceIntent);
            }
        }
    }
}
