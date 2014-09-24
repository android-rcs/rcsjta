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

import android.app.ListActivity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.gsma.services.rcs.contacts.JoynContact;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsService;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * List of joyn contacts
 *  
 * @author Jean-Marc AUFFRET
 */
public class JoynContactsList extends ListActivity {

  	/**
	 * API connection manager
	 */
	private ApiConnectionManager connectionManager;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.contacts_rcs_list);
        
        // Set title
        setTitle(R.string.menu_list_rcs_contacts);

        // Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);
		if (connectionManager == null || !connectionManager.isServiceConnected(RcsService.CONTACTS)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), null);
			return;
		}
		connectionManager.startMonitorServices(this, null, RcsService.CONTACTS);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (connectionManager != null) {
			connectionManager.stopMonitorServices(this);
    	}
	}	
	
	@Override
	protected void onResume() {
		super.onResume();

    	// Update the list of RCS contacts
		updateList();
	}
    
    /**
     * Update the list
     */
    private void updateList() {
		try {
	    	// Get list of joyn contacts
	    	Set<JoynContact> allContacts = connectionManager.getContactsApi().getJoynContacts();
	    	List<JoynContact> contacts = new ArrayList<JoynContact>(allContacts);
			if (contacts.size() > 0){
		        String[] items = new String[contacts.size()];    
		        for (int i = 0; i < contacts.size(); i++) {
		        	JoynContact contact = contacts.get(i);
		        	String status;
		        	if (contact.isRegistered()) {
						status = "online";
		        	} else {
						status = "offline";
		        	}
					items[i] = contact.getContactId() + " (" + status + ")";
		        }
				setListAdapter(new ArrayAdapter<String>(JoynContactsList.this, android.R.layout.simple_list_item_1, items));
			} else {
				setListAdapter(null);
			}
		} catch(Exception e) {
			e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed));
		}
    }
}

