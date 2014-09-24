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

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.gsma.services.rcs.capability.CapabilitiesLog;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.geoloc.ShowUsInMap;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * CHAT API
 * 
 * @author Jean-Marc AUFFRET
 */
public class TestChatApi extends ListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set items
        String[] items = {
    		getString(R.string.menu_initiate_chat),
    		getString(R.string.menu_chat_log),
    		getString(R.string.menu_initiate_group_chat),
    		getString(R.string.menu_group_chat_log),
    		getString(R.string.menu_showus_map),
    		getString(R.string.menu_spambox)
    	};
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch(position) {
	        case 0:
            	startActivity(new Intent(this, InitiateSingleChat.class));
	            break;
	            
	        case 1: 
            	startActivity(new Intent(this, ChatList.class));
	            break;

	        case 2:
	        	startActivity(new Intent(this, InitiateGroupChat.class));
	            break;
	            
	        case 3: 
            	startActivity(new Intent(this, GroupChatList.class));
	            break;

	        case 4:
	            String[] projection = new String[] {
	            	CapabilitiesLog.CONTACT_NUMBER
	            };
	        	ArrayList<String> list = new ArrayList<String>(); 
	    		Cursor cursor = getContentResolver().query(CapabilitiesLog.CONTENT_URI, projection, null, null, null);
	    		while(cursor.moveToNext()) {
	    			String contact = cursor.getString(0);
	    			list.add(contact);
	    		}
	    		cursor.close();
	        	Intent intent = new Intent(this, ShowUsInMap.class);
	        	intent.putStringArrayListExtra(ShowUsInMap.EXTRA_CONTACTS, list);
	        	startActivity(intent);
	        	break;
	        	
	        case 5:
            	Utils.showMessage(this, getString(R.string.label_not_implemented));
	        	// TODO startActivity(new Intent(this, SpamBox.class));
	        	break;
        }
    }
}
