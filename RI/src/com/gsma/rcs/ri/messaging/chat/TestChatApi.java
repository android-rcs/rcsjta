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

package com.gsma.rcs.ri.messaging.chat;

import com.gsma.rcs.api.connection.utils.RcsListActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.messaging.chat.group.InitiateGroupChat;
import com.gsma.rcs.ri.messaging.chat.single.InitiateSingleChat;
import com.gsma.rcs.ri.messaging.geoloc.DisplayGeoloc;
import com.gsma.rcs.ri.utils.ContactUtil;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.capability.CapabilitiesLog;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.HashSet;
import java.util.Set;

/**
 * Chat API
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class TestChatApi extends RcsListActivity {

    private static final String[] PROJECTION = new String[] {
        CapabilitiesLog.CONTACT
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // @formatter:off
        String[] items = {
                getString(R.string.menu_initiate_chat), 
                getString(R.string.menu_initiate_group_chat),
                getString(R.string.menu_chat_service_config),
                getString(R.string.menu_showus_map),
        };
        // @formatter:on
        setListAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
            case 0:
                startActivity(new Intent(this, InitiateSingleChat.class));
                break;

            case 1:
                /* Check if Group chat initialization is allowed */
                ChatService chatService = getChatApi();
                try {
                    if (chatService.isAllowedToInitiateGroupChat()) {
                        startActivity(new Intent(this, InitiateGroupChat.class));
                    } else {
                        showMessage(R.string.label_NotAllowedToInitiateGroupChat);
                    }
                } catch (RcsServiceNotAvailableException e) {
                    showMessage(R.string.label_service_not_available);

                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
                break;

            case 2:
                startActivity(new Intent(this, ChatServiceConfigActivity.class));
                break;

            case 3:
                Set<ContactId> contacts = new HashSet<>();
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(CapabilitiesLog.CONTENT_URI, PROJECTION,
                            null, null, null);
                    if (cursor == null) {
                        showMessageThenExit(R.string.label_db_failed);
                        return;
                    }
                    if (!cursor.moveToFirst()) {
                        showMessage(getString(R.string.label_geoloc_not_found));
                        return;
                    }
                    int contactColumIdx = cursor.getColumnIndexOrThrow(CapabilitiesLog.CONTACT);
                    do {
                        String contact = cursor.getString(contactColumIdx);
                        contacts.add(ContactUtil.formatContact(contact));
                    } while (cursor.moveToNext());
                    DisplayGeoloc.showContactsOnMap(this, contacts);

                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                break;
        }
    }

}
