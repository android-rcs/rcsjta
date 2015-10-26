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

package com.orangelabs.rcs.ri.messaging.chat.single;

import com.gsma.services.rcs.contact.ContactId;

import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;
import com.orangelabs.rcs.ri.utils.ContactUtil;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Spinner;

/**
 * Initiate chat
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class InitiateSingleChat extends RcsActivity {

    /**
     * Spinner for contact selection
     */
    private Spinner mSpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.chat_initiate_single);

        if (!getChatApi().isServiceConnected()) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        /* Set contact selector */
        mSpinner = (Spinner) findViewById(R.id.contact);
        mSpinner.setAdapter(ContactListAdapter.createRcsContactListAdapter(this));

        Button inviteBtn = (Button) findViewById(R.id.invite_btn);
        inviteBtn.setOnClickListener(btnInviteListener);

        /* Disable button if no contact available */
        if (mSpinner.getAdapter().getCount() == 0) {
            inviteBtn.setEnabled(false);
        }
    }

    /**
     * Invite button listener
     */

    private OnClickListener btnInviteListener = new OnClickListener() {
        public void onClick(View v) {
            /* get selected phone number */
            ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
            String phoneNumber = adapter.getSelectedNumber(mSpinner.getSelectedView());
            ContactId contact = ContactUtil.formatContact(phoneNumber);
            startActivity(SingleChatView.forgeIntentToOpenConversation(InitiateSingleChat.this,
                    contact));
            /* Exit activity */
            finish();
        }
    };
}
