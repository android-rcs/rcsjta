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

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Chat API
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
    		getString(R.string.menu_one_to_one_chat),
    		getString(R.string.menu_adhoc_group_chat),
    		getString(R.string.menu_chat_log),
    		getString(R.string.menu_spambox)
    	};
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch(position) {
	        case 0:
            	Utils.showMessage(this, getString(R.string.label_not_implemented));
            	// TODO startActivity(new Intent(this, InitiateSingleChat.class));
	            break;
	            
	        case 1:
	        	startActivity(new Intent(this, InitiateGroupChat.class));
	            break;
	            
	        case 2: 
            	Utils.showMessage(this, getString(R.string.label_not_implemented));
            	// TODO startActivity(new Intent(this, ChatList.class));
	            break;

	        case 3:
            	Utils.showMessage(this, getString(R.string.label_not_implemented));
	        	// TODO startActivity(new Intent(this, SpamBox.class));
	        	break;
        }
    }
}
