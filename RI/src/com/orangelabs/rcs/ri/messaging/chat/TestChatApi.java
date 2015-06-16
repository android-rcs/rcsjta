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

package com.orangelabs.rcs.ri.messaging.chat;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.capability.CapabilitiesLog;
import com.gsma.services.rcs.chat.ChatService;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.chat.group.GroupChatList;
import com.orangelabs.rcs.ri.messaging.chat.group.GroupChatView;
import com.orangelabs.rcs.ri.messaging.chat.group.InitiateGroupChat;
import com.orangelabs.rcs.ri.messaging.chat.single.InitiateSingleChat;
import com.orangelabs.rcs.ri.messaging.chat.single.SingleChatList;
import com.orangelabs.rcs.ri.messaging.geoloc.ShowUsInMap;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * CHAT API
 * 
 * @author Jean-Marc AUFFRET
 */
public class TestChatApi extends ListActivity {

    private static final String[] PROJECTION = new String[] {
        CapabilitiesLog.CONTACT
    };

    private static final String LOGTAG = LogUtils.getTag(GroupChatView.class.getSimpleName());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // @formatter:off
        String[] items = {
                getString(R.string.menu_initiate_chat), 
                getString(R.string.menu_chat_log),
                getString(R.string.menu_initiate_group_chat),
                getString(R.string.menu_group_chat_log),
                getString(R.string.menu_chat_service_config), 
                getString(R.string.menu_showus_map),
        };
        // @formatter:on
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
            case 0:
                startActivity(new Intent(this, InitiateSingleChat.class));
                break;

            case 1:
                startActivity(new Intent(this, SingleChatList.class));
                break;

            case 2:
                /* Check if Group chat initialization is allowed */
                ChatService chatService = ConnectionManager.getInstance().getChatApi();
                try {
                    if (chatService.isAllowedToInitiateGroupChat()) {
                        startActivity(new Intent(this, InitiateGroupChat.class));
                    } else {
                        Utils.showMessage(this,
                                getString(R.string.label_NotAllowedToInitiateGroupChat));
                    }
                } catch (RcsServiceException e) {
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "Cannot check if Group chat initialization is allowed", e);
                    }
                    Utils.showMessage(this, getString(R.string.label_api_failed));
                }
                break;

            case 3:
                startActivity(new Intent(this, GroupChatList.class));
                break;

            case 4:
                startActivity(new Intent(this, ChatServiceConfigActivity.class));
                break;

            case 5:
                ArrayList<String> list = new ArrayList<String>();
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(CapabilitiesLog.CONTENT_URI, PROJECTION,
                            null, null, null);
                    while (cursor.moveToNext()) {
                        String contact = cursor.getString(cursor
                                .getColumnIndex(CapabilitiesLog.CONTACT));
                        list.add(contact);
                    }
                    ShowUsInMap.startShowUsInMap(this, list);
                } catch (Exception e) {
                    // Skip intentionally
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                break;

        }
    }

}
