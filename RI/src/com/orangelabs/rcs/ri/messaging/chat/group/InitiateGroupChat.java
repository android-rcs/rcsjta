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

import java.util.ArrayList;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.MultiContactListAdapter;

/**
 * Initiate group chat
 * 
 * @author Jean-Marc AUFFRET
 */
public class InitiateGroupChat extends Activity implements OnItemClickListener {
    /**
     * List of participants
     */
    private ArrayList<String> participants = new ArrayList<String>();
    
    private MultiContactListAdapter mAdapter;
    
    /**
     * Invite button
     */
    private Button inviteBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.chat_initiate_group);

        // Set contact selector
        ListView contactList = (ListView)findViewById(R.id.contacts);
        mAdapter = MultiContactListAdapter.createMultiRcsContactListAdapter(this);
        contactList.setAdapter(mAdapter);
        contactList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        contactList.setOnItemClickListener(this);
        
        // Set button callback
        inviteBtn = (Button)findViewById(R.id.invite_btn);
        inviteBtn.setOnClickListener(btnInviteListener);
    	inviteBtn.setEnabled(false);
    }
    
    /**
     * Invite button listener
     */
    private OnClickListener btnInviteListener = new OnClickListener() {
        public void onClick(View v) {
        	// Get subject
        	EditText subjectTxt = (EditText)findViewById(R.id.subject);
        	String subject = subjectTxt.getText().toString();
        	GroupChatView.initiateGroupChat(InitiateGroupChat.this, subject, participants);
        	// Exit activity
        	finish();
        }
    };
    
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		// Check if number is in participants list
		String number =  mAdapter.getSelectedNumber(view);
		if (participants.contains(number)){
			// Number is in list, we remove it
			participants.remove(number);
		} else {
			// Number is not in list, add it
			participants.add(number);
		}
		
		// Disable the invite button if no contact selected
    	if (participants.size() == 0) {
    		inviteBtn.setEnabled(false);
		} else {
			inviteBtn.setEnabled(true);
		}		
	}
}    
