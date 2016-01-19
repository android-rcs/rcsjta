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

package com.orangelabs.rcs.ri.contacts;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.RcsContact;

import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.api.connection.utils.RcsListActivity;
import com.orangelabs.rcs.ri.R;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * List of RCS contacts who are online (i.e. registered)
 * 
 * @author Jean-Marc AUFFRET
 */
public class OnlineContactsList extends RcsListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.contacts_rcs_list);

        // Register to API connection manager
        if (!isServiceConnected(RcsServiceName.CONTACT)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(RcsServiceName.CONTACT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isExiting()) {
            return;
        }
        updateList();
    }

    private void updateList() {
        try {
            // Get list of RCS contacts who are online
            Set<RcsContact> onlineContacts = getContactApi().getRcsContactsOnline();
            List<RcsContact> contacts = new ArrayList<>(onlineContacts);
            if (contacts.size() > 0) {
                String[] items = new String[contacts.size()];
                for (int i = 0; i < contacts.size(); i++) {
                    RcsContact contact = contacts.get(i);
                    String status;
                    if (contact.isOnline()) {
                        status = "online";
                    } else {
                        status = "offline";
                    }
                    items[i] = contact.getContactId() + " (" + status + ")";
                }
                setListAdapter(new ArrayAdapter<>(OnlineContactsList.this,
                        android.R.layout.simple_list_item_1, items));
            } else {
                setListAdapter(null);
            }
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }
}
