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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.contacts.ContactsService;
import com.gsma.services.rcs.contacts.JoynContact;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * List of joyn contacts supporting a given feature tag or extension
 * 
 * @author Jean-Marc AUFFRET
 */
public class SupportedContactsList extends Activity implements JoynServiceListener {
    
    /**
     * Refresh button
     */
    private Button refreshBtn;
    
	/**
	 * Contacts API
	 */
	private ContactsService contactsApi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.contacts_supported_list);
        
        // Set title
        setTitle(R.string.menu_list_supported_contacts);

		// Set a default tag
		EditText tagEdit = (EditText)findViewById(R.id.tag);			
		tagEdit.setText("game");

        // Set button callback
        refreshBtn = (Button)findViewById(R.id.refresh_btn);
        refreshBtn.setOnClickListener(btnRefreshListener);

        // Instanciate API
        contactsApi = new ContactsService(getApplicationContext(), this);
        
        // Connect API
        contactsApi.connect();
    }
    
	@Override
	protected void onDestroy() {
		super.onDestroy();

        // Disconnect API
		contactsApi.disconnect();
	}	
	
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
    	// Display the list of sessions
		updateList();
    }
    
    /**
     * Callback called when service has been disconnected. This method is called when
     * the service is disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     * @see JoynService.Error
     */
    public void onServiceDisconnected(int error) {
    	// Nothing to do here
    }    

    /**
     * Update the list
     */
    private void updateList() {
		try {
			// Get tag to check
			EditText tagEdit = (EditText)findViewById(R.id.tag);			
			String tag = tagEdit.getText().toString();
			
	    	// Get list of joyn contacts supporting a given tag
	    	Set<JoynContact> supportedContacts = contactsApi.getJoynContactsSupporting(tag);
	    	List<JoynContact> contacts = new ArrayList<JoynContact>(supportedContacts);
	        ListView listView = (ListView)findViewById(R.id.contacts);
			if (contacts.size() > 0){
				ArrayList<String> items = new ArrayList<String>();
		        for (int i = 0; i < contacts.size(); i++) {
		        	JoynContact contact = contacts.get(i);
		        	String status;
		        	if (contact.isRegistered()) {
						status = "online";
		        	} else {
						status = "offline";
		        	}
					items.add(contact.getContactId() + " (" + status + ")");
		        }
			    ContactListAdapter adapter = new ContactListAdapter(this, android.R.layout.simple_list_item_1, items);
			    listView.setAdapter(adapter);
			} else {
				listView.setAdapter(null);
			}
		} catch(Exception e) {
			Utils.showMessageAndExit(SupportedContactsList.this, getString(R.string.label_api_failed));
		}
    }
    
	/**
     * Refresh button listener
     */
    private OnClickListener btnRefreshListener = new OnClickListener() {
        public void onClick(View v) {
        	// Update list
        	updateList();
        }
    };
    
	/**
	 * Contact list adapter
	 */
	private class ContactListAdapter extends ArrayAdapter<String> {
		private List<String> list;

		public ContactListAdapter(Context context, int textViewResourceId, List<String> list) {
			super(context, textViewResourceId, list);
			
			this.list = list;
		}

		@Override
		public String getItem(int position) {
			return list.get(position);
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
	}
}

