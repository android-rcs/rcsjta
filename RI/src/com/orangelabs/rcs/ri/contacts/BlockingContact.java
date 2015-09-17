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
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.RcsContact;

import com.orangelabs.rcs.api.connection.ConnectionManager;
import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.api.connection.utils.LockAccess;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;
import com.orangelabs.rcs.ri.utils.ContactUtil;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.ToggleButton;

/**
 * Block/unblock a contact
 * 
 * @author Jean-Marc AUFFRET
 */
public class BlockingContact extends Activity {

    private ConnectionManager mCnxManager;

    private LockAccess exitOnce = new LockAccess();

    /**
     * Spinner for contact selection
     */
    private Spinner mSpinner;

    private ToggleButton toggleBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.contacts_blocking);

        // Register to API connection manager
        mCnxManager = ConnectionManager.getInstance();
        if (!mCnxManager.isServiceConnected(RcsServiceName.CONTACT)) {
            Utils.showMessageAndExit(this, getString(R.string.label_service_not_available),
                    exitOnce);
            return;
        }
        mCnxManager.startMonitorServices(this, null, RcsServiceName.CONTACT);

        // Set the contact selector
        mSpinner = (Spinner) findViewById(R.id.contact);
        ContactListAdapter adapter = ContactListAdapter.createRcsContactListAdapter(this);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(listenerContact);

        // Set button callback
        toggleBtn = (ToggleButton) findViewById(R.id.block_btn);
        toggleBtn.setOnClickListener(toggleListener);

        // Update refresh button
        if (mSpinner.getAdapter().getCount() == 0) {
            // Disable button if no contact available
            toggleBtn.setEnabled(false);
        } else {
            toggleBtn.setEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCnxManager.stopMonitorServices(this);
    }

    private void updateBlockingState(ContactId contactId) {
        try {
            RcsContact contact = mCnxManager.getContactApi().getRcsContact(contactId);
            toggleBtn.setChecked(contact.isBlocked());
        } catch (RcsServiceNotAvailableException e) {
            e.printStackTrace();
            Utils.showMessageAndExit(BlockingContact.this,
                    getString(R.string.label_api_unavailable), exitOnce);
        } catch (RcsServiceException e) {
            e.printStackTrace();
            Utils.showMessageAndExit(BlockingContact.this, getString(R.string.label_api_failed),
                    exitOnce);
        }
    }

    /**
     * Spinner contact listener
     */
    private OnItemSelectedListener listenerContact = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            ContactId contactId = getSelectedContact();
            updateBlockingState(contactId);
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
        }
    };

    /**
     * Returns the selected contact
     * 
     * @return Contact ID
     */
    private ContactId getSelectedContact() {
        // get selected phone number
        ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
        return ContactUtil.formatContact(adapter.getSelectedNumber(mSpinner.getSelectedView()));
    }

    /**
     * Toggle button listener
     */
    private OnClickListener toggleListener = new OnClickListener() {
        public void onClick(View view) {
            try {
                ContactId contact = getSelectedContact();
                if (toggleBtn.isChecked()) {
                    // Block the contact
                    mCnxManager.getContactApi().blockContact(contact);
                    Utils.displayToast(BlockingContact.this,
                            getString(R.string.label_contact_blocked, contact.toString()));
                } else {
                    // Unblock the contact
                    mCnxManager.getContactApi().unblockContact(contact);
                    Utils.displayToast(BlockingContact.this,
                            getString(R.string.label_contact_unblocked, contact.toString()));
                }
            } catch (RcsServiceNotAvailableException e) {
                e.printStackTrace();
                Utils.showMessageAndExit(BlockingContact.this,
                        getString(R.string.label_api_unavailable), exitOnce);
            } catch (RcsServiceException e) {
                Utils.showMessageAndExit(BlockingContact.this,
                        getString(R.string.label_api_failed), exitOnce);
            }
        }
    };
}
