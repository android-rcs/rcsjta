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

package com.gsma.rcs.ri.messaging.chat.single;

import com.gsma.rcs.api.connection.ConnectionManager;
import com.gsma.rcs.api.connection.utils.ExceptionUtil;
import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.messaging.OneToOneTalkView;
import com.gsma.rcs.ri.utils.ContactListAdapter;
import com.gsma.rcs.ri.utils.ContactUtil;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.capability.CapabilitiesListener;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.contact.ContactId;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;

import java.util.Collections;
import java.util.HashSet;

/**
 * Initiate chat
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class InitiateSingleChat extends RcsActivity {

    private static final String LOGTAG = LogUtils.getTag(InitiateSingleChat.class.getSimpleName());

    /**
     * Spinner for contact selection
     */
    private Spinner mSpinner;
    private ChatService mChatService;
    private CapabilityService mCapabilityService;
    private Button mInviteBtn;
    private CapabilitiesListener mCapabilitiesListener;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.chat_initiate_single);
        initialize();

        if (!getChatApi().isServiceConnected()) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(ConnectionManager.RcsServiceName.CAPABILITY);
        try {
            getCapabilityApi().addCapabilitiesListener(mCapabilitiesListener);

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isServiceConnected(ConnectionManager.RcsServiceName.CAPABILITY)
                && mCapabilityService != null) {
            try {
                mCapabilityService.removeCapabilitiesListener(mCapabilitiesListener);
            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }
    }

    private void initialize() {
        mChatService = getChatApi();
        mHandler = new Handler();
        mCapabilityService = getCapabilityApi();
        mCapabilitiesListener = new CapabilitiesListener() {
            @Override
            public void onCapabilitiesReceived(ContactId contact, Capabilities capabilities) {
                if (contact.equals(getSelectedContact())
                        && capabilities.hasCapabilities(Capabilities.CAPABILITY_IM)) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mInviteBtn.setEnabled(true);
                        }
                    });
                }
            }
        };

        OnClickListener btnInviteListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(OneToOneTalkView.forgeIntentToOpenConversation(
                        InitiateSingleChat.this, getSelectedContact()));
                finish();
            }
        };
        mInviteBtn = (Button) findViewById(R.id.invite_btn);
        mInviteBtn.setOnClickListener(btnInviteListener);
        mInviteBtn.setEnabled(false);
        /* Set contact selector */
        AdapterView.OnItemSelectedListener mListenerContact = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    ContactId contact = getSelectedContact();
                    if (mChatService.getOneToOneChat(contact).isAllowedToSendMessage()) {
                        mInviteBtn.setEnabled(true);
                        return;
                    }
                    mInviteBtn.setEnabled(false);
                    mCapabilityService.requestContactCapabilities(new HashSet<>(Collections
                            .singletonList(contact)));
                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };
        mSpinner = (Spinner) findViewById(R.id.contact);
        mSpinner.setAdapter(ContactListAdapter.createContactListAdapter(this));
        mSpinner.setOnItemSelectedListener(mListenerContact);
    }

    private ContactId getSelectedContact() {
        /* get selected phone number */
        ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
        String phoneNumber = adapter.getSelectedNumber(mSpinner.getSelectedView());
        return ContactUtil.formatContact(phoneNumber);
    }
}
