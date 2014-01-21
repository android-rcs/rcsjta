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

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.contacts.ContactsService;
import com.gsma.services.rcs.contacts.JoynContact;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * List of joyn contacts who are online (i.e. registered)
 *  
 * @author Jean-Marc AUFFRET
 */
public class OnlineContactsList extends ListActivity implements JoynServiceListener {
	/**
	 * Contacts API
	 */
	private ContactsService contactsApi;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.contacts_rcs_list);
        
        // Set title
        setTitle(R.string.menu_list_online_contacts);

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
	    	// Get list of joyn contacts who are online
	    	Set<JoynContact> onlineContacts = contactsApi.getJoynContactsOnline();
	    	List<JoynContact> contacts = new ArrayList<JoynContact>(onlineContacts);
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
				setListAdapter(new ArrayAdapter<String>(OnlineContactsList.this, android.R.layout.simple_list_item_1, items));
			} else {
				setListAdapter(null);
			}
		} catch(Exception e) {
			e.printStackTrace();
			Utils.showMessageAndExit(OnlineContactsList.this, getString(R.string.label_api_failed));
		}
    }
}

