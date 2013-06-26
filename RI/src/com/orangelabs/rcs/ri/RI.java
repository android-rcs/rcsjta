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

package com.orangelabs.rcs.ri;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.orangelabs.rcs.ri.utils.Utils;

/**
 * RI application
 * 
 * @author Jean-Marc AUFFRET
 */
public class RI extends ListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		// Set title
        setTitle(getString(R.string.app_name));

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
       
		// Set items
        String[] items = {
    		getString(R.string.menu_address_book),
    		getString(R.string.menu_capabilities),
    		getString(R.string.menu_messaging),
    		getString(R.string.menu_richcall),
    		getString(R.string.menu_intents),
    		getString(R.string.menu_about)
        };
    	setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	switch(position) {
        	case 0:
        		try {
        			startActivity(new Intent(Intent.ACTION_VIEW).setType(ContactsContract.Contacts.CONTENT_TYPE));
        		} catch(ActivityNotFoundException e1) {
        			try {
        				startActivity(new Intent("com.android.contacts.action.LIST_DEFAULT"));
        			} catch(ActivityNotFoundException e2) {
        				Utils.showMessage(this, getString(R.string.label_ab_not_found));
        			}
        		}
        		break;
        		
        	case 1:
        		startActivity(new Intent(this, TestCapabilitiesApi.class));
        		break;
        		
        	case 2:
        		startActivity(new Intent(this, TestMessagingApi.class));
        		break;
        		
        	case 3:
        		startActivity(new Intent(this, TestRichCallApi.class));
        		break;
        		
        	case 4:
        		startActivity(new Intent(this, TestIntentsApi.class));
        		break;

        	case 5:
        		startActivity(new Intent(this, AboutRI.class));
        		break;
    	}
    }
}
