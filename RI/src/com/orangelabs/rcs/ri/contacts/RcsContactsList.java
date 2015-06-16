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

import com.gsma.services.rcs.contact.RcsContact;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.ListActivity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * List of RCS contacts
 * 
 * @author Jean-Marc AUFFRET
 */
public class RcsContactsList extends ListActivity {

    /**
     * API connection manager
     */
    private ConnectionManager mCnxManager;

    /**
     * A locker to exit only once
     */
    private LockAccess mExitOnce = new LockAccess();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.contacts_rcs_list);

        // Register to API connection manager
        mCnxManager = ConnectionManager.getInstance();
        if (!mCnxManager.isServiceConnected(RcsServiceName.CONTACT)) {
            Utils.showMessageAndExit(this, getString(R.string.label_service_not_available),
                    mExitOnce);
            return;
        }
        mCnxManager.startMonitorServices(this, null, RcsServiceName.CONTACT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCnxManager.stopMonitorServices(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mExitOnce.isLocked()) {
            // Update the list of RCS contacts
            updateList();
        }
    }

    /**
     * Update the list
     */
    private void updateList() {
        try {
            // Get list of RCS contacts
            Set<RcsContact> allContacts = mCnxManager.getContactApi().getRcsContacts();
            List<RcsContact> contacts = new ArrayList<RcsContact>(allContacts);
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
                setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                        items));
            } else {
                setListAdapter(null);
            }
        } catch (Exception e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce, e);
        }
    }
}
