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

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.OneToOneChatIntent;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;

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
            String msgId = intent.getStringExtra(OneToOneChatIntent.EXTRA_MESSAGE_ID);

            // Get the message content associated to the message ID from the database
            Cursor cursor = null;
            String content = null;
            try {
                cursor = context.getContentResolver().query(Uri.withAppendedPath(ChatLog.Message.CONTENT_URI, msgId),
                        null, null, null, null);
                if (!cursor.moveToFirst()) {
                    // Failed to find message from its ID
                    return;
                }
                content = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.Message.CONTENT));
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }            
            
            if (!TextUtils.isEmpty(content)) {
                // Play TTS on the chat message
                ArrayList<String> messages = new ArrayList<String>();
                messages.add(context.getString(R.string.label_new_msg));
                messages.add(content);
                Intent serviceIntent = new Intent(context, PlayTextToSpeech.class);
                serviceIntent.putStringArrayListExtra("messages", messages);
                context.startService(serviceIntent);
            }
        }
    }
}
