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

package com.gsma.rcs.ri.contacts;

import com.gsma.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.utils.ContactListAdapter;
import com.gsma.rcs.ri.utils.ContactUtil;
import com.gsma.rcs.ri.utils.Utils;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.RcsContact;

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
public class BlockingContact extends RcsActivity {

    /**
     * Spinner for contact selection
     */
    private Spinner mSpinner;

    private ToggleButton mToggleBtn;

    private OnClickListener mToggleListener;

    private OnItemSelectedListener mListenerContact;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.contacts_blocking);

        // Register to API connection manager
        if (!isServiceConnected(RcsServiceName.CONTACT)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(RcsServiceName.CONTACT);

        // Set the contact selector
        mSpinner = (Spinner) findViewById(R.id.contact);
        ContactListAdapter adapter = ContactListAdapter.createRcsContactListAdapter(this);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(mListenerContact);

        // Set button callback
        mToggleBtn = (ToggleButton) findViewById(R.id.block_btn);
        mToggleBtn.setOnClickListener(mToggleListener);

        // Update refresh button
        if (mSpinner.getAdapter().getCount() == 0) {
            // Disable button if no contact available
            mToggleBtn.setEnabled(false);
        } else {
            mToggleBtn.setEnabled(true);
        }
    }

    private void updateBlockingState(ContactId contactId) {
        try {
            RcsContact contact = getContactApi().getRcsContact(contactId);
            mToggleBtn.setChecked(contact.isBlocked());
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private ContactId getSelectedContact() {
        // get selected phone number
        ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
        return ContactUtil.formatContact(adapter.getSelectedNumber(mSpinner.getSelectedView()));
    }

    private void initialize() {
        mToggleListener = new OnClickListener() {
            public void onClick(View view) {
                try {
                    ContactId contact = getSelectedContact();
                    if (mToggleBtn.isChecked()) {
                        // Block the contact
                        getContactApi().blockContact(contact);
                        Utils.displayToast(BlockingContact.this,
                                getString(R.string.label_contact_blocked, contact.toString()));
                    } else {
                        // Unblock the contact
                        getContactApi().unblockContact(contact);
                        Utils.displayToast(BlockingContact.this,
                                getString(R.string.label_contact_unblocked, contact.toString()));
                    }
                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
            }
        };

        mListenerContact = new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ContactId contactId = getSelectedContact();
                updateBlockingState(contactId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        };

    }
}
