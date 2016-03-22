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

package com.gsma.rcs.ri.extension;

import com.gsma.rcs.api.connection.ConnectionManager;
import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.extension.messaging.MessagingSessionUtils;
import com.gsma.rcs.ri.utils.ContactListAdapter;
import com.gsma.rcs.ri.utils.ContactUtil;
import com.gsma.rcs.ri.utils.Utils;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;

/**
 * Send an instant multimedia message
 *
 * @author Jean-Marc AUFFRET
 */
public class SendInstantMessage extends RcsActivity {

    /**
     * Spinner for contact selection
     */
    private Spinner mSpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.extension_send_instant_message);

        // Set contact selector
        mSpinner = (Spinner) findViewById(R.id.contact);
        mSpinner.setAdapter(ContactListAdapter.createContactListAdapter(this));

        // Set buttons callback
        Button sendBtn = (Button) findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(btnSendListener);

        /* Register to API connection manager */
        if (!isServiceConnected(ConnectionManager.RcsServiceName.MULTIMEDIA,
                ConnectionManager.RcsServiceName.CONTACT)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(ConnectionManager.RcsServiceName.MULTIMEDIA,
                ConnectionManager.RcsServiceName.CONTACT);

        // Disable button if no contact available
        if (mSpinner.getAdapter().getCount() == 0) {
            sendBtn.setEnabled(false);
        }
    }

    /**
     * Send button callback
     */
    private View.OnClickListener btnSendListener = new View.OnClickListener() {
        public void onClick(View v) {
            // get selected phone number
            ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
            String phoneNumber = adapter.getSelectedNumber(mSpinner.getSelectedView());
            ContactId contact = ContactUtil.formatContact(phoneNumber);
            // Initiate session
            sendMessage(contact);
        }
    };

    /**
     * Send message to a remote contact
     *
     * @param contact Remote contact
     */
    public void sendMessage(ContactId contact) {
        try {
            String content = "Hello world";
            getMultimediaSessionApi().sendInstantMultimediaMessage(
                    MessagingSessionUtils.SERVICE_ID, contact, content.getBytes(),
                    MessagingSessionUtils.SERVICE_CONTENT_TYPE);
            Utils.displayToast(this, getString(R.string.label_instant_message_sent));
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }
}
