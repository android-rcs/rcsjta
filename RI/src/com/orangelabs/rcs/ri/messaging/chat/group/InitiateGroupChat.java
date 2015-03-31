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

package com.orangelabs.rcs.ri.messaging.chat.group;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactService;
import com.gsma.services.rcs.contact.RcsContact;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Initiate group chat
 * 
 * @author Jean-Marc AUFFRET
 * @author yplo6403
 */
public class InitiateGroupChat extends Activity implements OnItemClickListener {

    private ArrayList<String> mParticipants;

    private ListView mContactList;

    private List<ContactId> mAllowedContactIds;

    private Button mInviteBtn;

    private LockAccess mExitOnce = new LockAccess();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.chat_initiate_group);

        mContactList = (ListView) findViewById(R.id.contacts);

        /* Check if Group chat initialization is allowed */
        ContactService contactService = ConnectionManager.getInstance(this).getContactApi();
        ChatService chatService = ConnectionManager.getInstance(this).getChatApi();
        try {
            Set<RcsContact> rcsContacts = contactService.getRcsContacts();
            mAllowedContactIds = new ArrayList<ContactId>();
            List<String> allowedContacts = new ArrayList<String>();
            for (RcsContact rcsContact : rcsContacts) {
                ContactId contact = rcsContact.getContactId();
                if (chatService.isAllowedToInitiateGroupChat(contact)) {
                    mAllowedContactIds.add(contact);
                    if (rcsContact.getDisplayName() != null) {
                        allowedContacts.add(new StringBuilder(rcsContact.getDisplayName())
                                .append(" (").append(rcsContact.getContactId()).append(")")
                                .toString());
                    } else {
                        allowedContacts.add(contact.toString());
                    }

                }
            }
            if (allowedContacts.size() > 0) {
                String[] contacts = allowedContacts.toArray(new String[allowedContacts.size()]);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                        android.R.layout.simple_list_item_multiple_choice, contacts);
                mContactList.setAdapter(adapter);
                mContactList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                mContactList.setOnItemClickListener(this);
                for (int i = 0; i < mContactList.getCount(); i++) {
                    mContactList.setItemChecked(i, false);
                }

                // Set button callback
                mInviteBtn = (Button) findViewById(R.id.invite_btn);
                mInviteBtn.setOnClickListener(btnInviteListener);
                mInviteBtn.setEnabled(false);
            } else {
                Utils.showMessage(this, getString(R.string.label_no_participant_found));
                return;

            }
        } catch (RcsServiceException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_unavailable), mExitOnce);
        }
    }

    /**
     * Invite button listener
     */
    private OnClickListener btnInviteListener = new OnClickListener() {
        public void onClick(View v) {
            // Get subject
            EditText subjectTxt = (EditText) findViewById(R.id.subject);
            String subject = subjectTxt.getText().toString();
            GroupChatView.initiateGroupChat(InitiateGroupChat.this, subject, mParticipants);
            // Exit activity
            finish();
        }
    };

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        /* Build list of participant numbers */
        SparseBooleanArray checkedArray = mContactList.getCheckedItemPositions();
        mParticipants = new ArrayList<String>();
        for (int i = 0; i < checkedArray.size(); i++) {
            if (checkedArray.get(i)) {
                mParticipants.add(mAllowedContactIds.get(i).toString());
            }
        }
        /* Disable the invite button if no contact selected */
        if (mParticipants.size() == 0) {
            mInviteBtn.setEnabled(false);
        } else {
            mInviteBtn.setEnabled(true);
        }
    }
}
