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

package com.orangelabs.rcs.ri.messaging.chat;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract.Data;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Spinner;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Initiate chat
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 *
 */
public class InitiateSingleChat extends Activity {
	
	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(InitiateSingleChat.class.getSimpleName());
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.chat_initiate_single);
        
        // Set title
        setTitle(R.string.menu_initiate_chat);
        
        // Set contact selector
        Spinner spinner = (Spinner)findViewById(R.id.contact);
        spinner.setAdapter(Utils.createRcsContactListAdapter(this));
        
        // Set button callback
        Button inviteBtn = (Button)findViewById(R.id.invite_btn);
        inviteBtn.setOnClickListener(btnInviteListener);
               
        // Disable button if no contact available
        if (spinner.getAdapter().getCount() == 0) {
        	inviteBtn.setEnabled(false);
        }
       
        // Select the corresponding contact from the intent
        Intent intent = getIntent();
        Uri contactUri = intent.getData();
		if (contactUri != null) {
			String[] proj = { Data.DATA1 };
			Cursor cursor = getContentResolver().query(contactUri, proj, null, null, null);
			if (cursor.moveToNext()) {
				String selectedContact = cursor.getString(cursor.getColumnIndex(Data.DATA1));
				if (selectedContact != null) {
					for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
						MatrixCursor cursor2 = (MatrixCursor) spinner.getAdapter().getItem(i);
						if (PhoneNumberUtils.compare(selectedContact, cursor2.getString(1))) {
							// Select contact
							spinner.setSelection(i);
							spinner.setEnabled(false);
							break;
						}
					}
				}
			}
			cursor.close();
		}
    }
    
    /**
     * Invite button listener
     */
    private OnClickListener btnInviteListener = new OnClickListener() {
        public void onClick(View v) {
            // Build participant list
        	Spinner spinner = (Spinner)findViewById(R.id.contact);
        	MatrixCursor cursor = (MatrixCursor)spinner.getSelectedItem();
            String phoneNumber = cursor.getString(1);
            ContactUtils contactUtils = ContactUtils.getInstance(InitiateSingleChat.this);
            ContactId contact;
			try {
				contact = contactUtils.formatContact(phoneNumber);
			} catch (RcsContactFormatException e) {
				if (LogUtils.isActive) {
	    			Log.e(LOGTAG, "Cannot parse contact "+phoneNumber);
	    		}
				return;
			}
            // Display chat view
        	Intent intent = new Intent(InitiateSingleChat.this, SingleChatView.class);
        	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	intent.putExtra(SingleChatView.EXTRA_MODE, SingleChatView.MODE_OUTGOING);
        	intent.putExtra(SingleChatView.EXTRA_CONTACT, (Parcelable)contact);
        	startActivity(intent);
        	
        	// Exit activity
        	finish();
        }
    };
}    
