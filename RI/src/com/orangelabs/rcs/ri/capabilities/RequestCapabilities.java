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

package com.orangelabs.rcs.ri.capabilities;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.capability.CapabilitiesListener;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.contact.ContactId;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;
import com.orangelabs.rcs.ri.utils.ContactUtil;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.Activity;
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

/**
 * Refresh capabilities of a given contact
 * 
 * @author Jean-Marc AUFFRET
 */
public class RequestCapabilities extends Activity {

    private final Handler mHandler = new Handler();

    private LockAccess mExitOnce = new LockAccess();

    private ConnectionManager mCnxManager;

    private MyCapabilitiesListener capabilitiesListener = new MyCapabilitiesListener();

    private static final String EXTENSION_SEPARATOR = "\n";

    private static final String LOGTAG = LogUtils.getTag(RequestCapabilities.class.getSimpleName());

    /**
     * Spinner for contact selection
     */
    private Spinner mSpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.capabilities_request);

        mCnxManager = ConnectionManager.getInstance();

        /* Set the contact selector */
        mSpinner = (Spinner) findViewById(R.id.contact);
        mSpinner.setAdapter(ContactListAdapter.createContactListAdapter(this));
        mSpinner.setOnItemSelectedListener(listenerContact);

        /* Set button callback */
        Button refreshBtn = (Button) findViewById(R.id.refresh_btn);
        refreshBtn.setOnClickListener(btnRefreshListener);

        /* Update refresh button */
        if (mSpinner.getAdapter().getCount() == 0) {
            // Disable button if no contact available
            refreshBtn.setEnabled(false);
        } else {
            refreshBtn.setEnabled(true);
        }

        /* Register to API connection manager */
        if (!mCnxManager.isServiceConnected(RcsServiceName.CAPABILITY)) {
            Utils.showMessageAndExit(this, getString(R.string.label_service_not_available),
                    mExitOnce);
            return;
        }
        mCnxManager.startMonitorServices(this, null, RcsServiceName.CAPABILITY);
        try {
            // Add service listener
            mCnxManager.getCapabilityApi().addCapabilitiesListener(capabilitiesListener);
        } catch (RcsServiceException e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Failed to add listener", e);
            }
            Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCnxManager.stopMonitorServices(this);
        if (mCnxManager.isServiceConnected(RcsServiceName.CAPABILITY)) {
            // Remove image sharing listener
            try {
                mCnxManager.getCapabilityApi().removeCapabilitiesListener(capabilitiesListener);
            } catch (Exception e) {
                if (LogUtils.isActive) {
                    Log.e(LOGTAG, "Failed to remove listener", e);
                }
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
                    if (contact != null && contact.equals(selectedContact)) {
                        // Update UI
                        displayCapabilities(capabilities);
                    }
                }
            });
        };
    }

    /**
     * Spinner contact listener
     */
    private OnItemSelectedListener listenerContact = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            CapabilityService capabilityApi = mCnxManager.getCapabilityApi();
            try {
                // Get selected contact
                ContactId contactId = getSelectedContact();

                // Get current capabilities
                Capabilities currentCapabilities = capabilityApi.getContactCapabilities(contactId);
                // Display default capabilities
                displayCapabilities(currentCapabilities);
                if (currentCapabilities == null) {
                    Utils.displayLongToast(RequestCapabilities.this,
                            getString(R.string.label_no_capabilities, contactId.toString()));
                }
            } catch (RcsServiceNotAvailableException e) {
                Utils.showMessageAndExit(RequestCapabilities.this,
                        getString(R.string.label_api_unavailable), mExitOnce, e);
            } catch (RcsServiceException e) {
                Utils.showMessageAndExit(RequestCapabilities.this,
                        getString(R.string.label_api_failed), mExitOnce, e);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
        }
    };

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

    /**
     * Request button callback
     */
    private OnClickListener btnRefreshListener = new OnClickListener() {
        public void onClick(View v) {
            // Check if the service is available
            boolean registered = false;
            try {
                registered = mCnxManager.getCapabilityApi().isServiceRegistered();
            } catch (Exception e) {
                Utils.showMessageAndExit(RequestCapabilities.this,
                        getString(R.string.label_api_unavailable), mExitOnce, e);
                return;
            }
            if (!registered) {
                Utils.showMessage(RequestCapabilities.this,
                        getString(R.string.label_service_not_available));
                return;
            }

            ContactId contact = getSelectedContact();
            if (contact != null) {
                updateCapabilities(contact);
            }
        }
    };

    /**
     * Update capabilities of a given contact
     * 
     * @param contact Contact
     */
    private void updateCapabilities(ContactId contact) {
        // Display info
        Utils.displayLongToast(RequestCapabilities.this,
                getString(R.string.label_request_in_background, contact));
        // Request capabilities
        try {
            // Request new capabilities
            mCnxManager.getCapabilityApi().requestContactCapabilities(contact);
        } catch (RcsServiceNotAvailableException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_unavailable), mExitOnce, e);
        } catch (RcsServiceException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce, e);
        }
    }

    /**
     * Display the capabilities
     * 
     * @param capabilities Capabilities
     */
    private void displayCapabilities(Capabilities capabilities) {
        CheckBox imageCSh = (CheckBox) findViewById(R.id.image_sharing);
        CheckBox videoCSh = (CheckBox) findViewById(R.id.video_sharing);
        CheckBox ft = (CheckBox) findViewById(R.id.file_transfer);
        CheckBox im = (CheckBox) findViewById(R.id.im);
        CheckBox geoloc = (CheckBox) findViewById(R.id.geoloc_push);
        TextView extensions = (TextView) findViewById(R.id.extensions);
        TextView timestamp = (TextView) findViewById(R.id.last_refresh);
        CheckBox automata = (CheckBox) findViewById(R.id.automata);
        // Set capabilities
        imageCSh.setChecked((capabilities != null) ? capabilities.isImageSharingSupported() : false);
        videoCSh.setChecked((capabilities != null) ? capabilities.isVideoSharingSupported() : false);
        ft.setChecked((capabilities != null) ? capabilities.isFileTransferSupported() : false);
        im.setChecked((capabilities != null) ? capabilities.isImSessionSupported() : false);
        geoloc.setChecked((capabilities != null) ? capabilities.isGeolocPushSupported() : false);

        // Set extensions
        extensions.setVisibility(View.VISIBLE);
        extensions.setText(getExtensions(capabilities));
        automata.setChecked((capabilities != null) ? capabilities.isAutomata() : false);
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
}
