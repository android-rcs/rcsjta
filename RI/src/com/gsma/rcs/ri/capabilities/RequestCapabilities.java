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

package com.gsma.rcs.ri.capabilities;

import com.gsma.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.gsma.rcs.api.connection.utils.ExceptionUtil;
import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.utils.ContactListAdapter;
import com.gsma.rcs.ri.utils.ContactUtil;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.rcs.ri.utils.Utils;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.capability.CapabilitiesListener;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.contact.ContactId;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

/**
 * Refresh capabilities of a given contact
 * 
 * @author Jean-Marc AUFFRET
 */
public class RequestCapabilities extends RcsActivity {

    private final Handler mHandler = new Handler();

    private MyCapabilitiesListener mCapabilitiesListener = new MyCapabilitiesListener();

    private static final String EXTENSION_SEPARATOR = "\n";

    private static final String LOGTAG = LogUtils.getTag(RequestCapabilities.class.getSimpleName());

    /**
     * Spinner for contact selection
     */
    private Spinner mSpinner;

    private OnClickListener mBtnRefreshListener;

    private OnItemSelectedListener mListenerContact;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.capabilities_request);

        /* Set the contact selector */
        mSpinner = (Spinner) findViewById(R.id.contact);
        mSpinner.setAdapter(ContactListAdapter.createContactListAdapter(this));
        mSpinner.setOnItemSelectedListener(mListenerContact);

        /* Set button callback */
        Button refreshBtn = (Button) findViewById(R.id.refresh_btn);
        refreshBtn.setOnClickListener(mBtnRefreshListener);

        /* Update refresh button */
        if (mSpinner.getAdapter().getCount() == 0) {
            // Disable button if no contact available
            refreshBtn.setEnabled(false);
        } else {
            refreshBtn.setEnabled(true);
        }

        /* Register to API connection manager */
        if (!isServiceConnected(RcsServiceName.CAPABILITY)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(RcsServiceName.CAPABILITY);
        try {
            getCapabilityApi().addCapabilitiesListener(mCapabilitiesListener);

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isServiceConnected(RcsServiceName.CAPABILITY)) {
            // Remove image sharing listener
            try {
                getCapabilityApi().removeCapabilitiesListener(mCapabilitiesListener);

            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }
    }

    /**
     * Capabilities event listener
     */
    private class MyCapabilitiesListener extends CapabilitiesListener {
        /**
         * Callback called when new capabilities are received for a given contact
         * 
         * @param contact Contact
         * @param capabilities Capabilities
         */
        public void onCapabilitiesReceived(final ContactId contact, final Capabilities capabilities) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onCapabilitiesReceived " + contact);
            }
            final ContactId selectedContact = getSelectedContact();
            if (!contact.equals(selectedContact)) {
                // Discard capabilities if not for selected contact
                return;
            }
            mHandler.post(new Runnable() {
                public void run() {
                    // Check if this intent concerns the current selected
                    // contact
                    if (contact.equals(selectedContact)) {
                        // Update UI
                        displayCapabilities(capabilities);
                    }
                }
            });
        }
    }

    /**
     * Returns the selected contact
     * 
     * @return Contact
     */
    private ContactId getSelectedContact() {
        // get selected phone number
        ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
        return ContactUtil.formatContact(adapter.getSelectedNumber(mSpinner.getSelectedView()));
    }

    private void updateCapabilities(ContactId contact) {
        // Display info
        Utils.displayLongToast(RequestCapabilities.this,
                getString(R.string.label_request_in_background, contact));
        try {
            Set<ContactId> contactSet = new HashSet<>();
            contactSet.add(contact);
            getCapabilityApi().requestContactCapabilities(contactSet);

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private void displayCapabilities(Capabilities capabilities) {
        CheckBox imageCSh = (CheckBox) findViewById(R.id.image_sharing);
        CheckBox videoCSh = (CheckBox) findViewById(R.id.video_sharing);
        CheckBox ft = (CheckBox) findViewById(R.id.file_transfer);
        CheckBox im = (CheckBox) findViewById(R.id.im);
        CheckBox geoloc = (CheckBox) findViewById(R.id.geoloc_push);
        TextView extensions = (TextView) findViewById(R.id.extensions);
        TextView timestamp = (TextView) findViewById(R.id.last_refresh);
        CheckBox automata = (CheckBox) findViewById(R.id.automata);

        if (capabilities != null) {
            // Set capabilities
            imageCSh.setChecked(capabilities.hasCapabilities(Capabilities.CAPABILITY_IMAGE_SHARING));
            videoCSh.setChecked(capabilities.hasCapabilities(Capabilities.CAPABILITY_VIDEO_SHARING));
            ft.setChecked(capabilities.hasCapabilities(Capabilities.CAPABILITY_FILE_TRANSFER));
            im.setChecked(capabilities.hasCapabilities(Capabilities.CAPABILITY_IM));
            geoloc.setChecked(capabilities.hasCapabilities(Capabilities.CAPABILITY_GEOLOC_PUSH));
        }
        // Set extensions
        extensions.setVisibility(View.VISIBLE);
        extensions.setText(getExtensions(capabilities));
        automata.setChecked((capabilities != null) && capabilities.isAutomata());
        timestamp.setText((capabilities != null) ? DateUtils.getRelativeTimeSpanString(
                capabilities.getTimestamp(), System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE) : "");
    }

    /* package private */static String getExtensions(Capabilities capabilities) {
        if (capabilities == null || capabilities.getSupportedExtensions().isEmpty()) {
            return "";
        }
        StringBuilder extensions = new StringBuilder();
        for (String capability : capabilities.getSupportedExtensions()) {
            extensions.append(EXTENSION_SEPARATOR).append(capability);
        }
        return extensions.substring(EXTENSION_SEPARATOR.length());
    }

    private void initialize() {
        mBtnRefreshListener = new OnClickListener() {
            public void onClick(View v) {
                // Check if the service is available
                try {
                    if (!getCapabilityApi().isServiceRegistered()) {
                        showMessage(R.string.error_not_registered);
                        return;
                    }
                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                    return;
                }
                ContactId contact = getSelectedContact();
                if (contact != null) {
                    updateCapabilities(contact);
                }
            }
        };

        mListenerContact = new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CapabilityService capabilityApi = getCapabilityApi();
                try {
                    // Get selected contact
                    ContactId contactId = getSelectedContact();

                    // Get current capabilities
                    Capabilities currentCapabilities = capabilityApi
                            .getContactCapabilities(contactId);
                    // Display default capabilities
                    displayCapabilities(currentCapabilities);
                    if (currentCapabilities == null) {
                        Utils.displayLongToast(RequestCapabilities.this,
                                getString(R.string.label_no_capabilities, contactId.toString()));
                    }
                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        };
    }
}
