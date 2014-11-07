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
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.orangelabs.rcs.ri.capabilities.TestCapabilitiesApi;
import com.orangelabs.rcs.ri.contacts.TestContactsApi;
import com.orangelabs.rcs.ri.extension.TestMultimediaSessionApi;
import com.orangelabs.rcs.ri.intents.TestIntentsApi;
import com.orangelabs.rcs.ri.ipcall.TestIPCallApi;
import com.orangelabs.rcs.ri.messaging.TestMessagingApi;
import com.orangelabs.rcs.ri.service.TestServiceApi;
import com.orangelabs.rcs.ri.sharing.TestSharingApi;
import com.orangelabs.rcs.ri.upload.InitiateFileUpload;

/**
 * RI application
 * 
 * @author Jean-Marc AUFFRET
 */
public class RI extends ListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
               
		// Set items
        String[] items = {
    		getString(R.string.menu_contacts),
    		getString(R.string.menu_capabilities),
    		getString(R.string.menu_messaging),
    		getString(R.string.menu_sharing),
    		getString(R.string.menu_mm_session),
    		getString(R.string.menu_ipcall),
    		getString(R.string.menu_intents),
    		getString(R.string.menu_service),
    		getString(R.string.menu_upload),
    		getString(R.string.menu_about)
        };
    	setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
    	// Create the API connection manager
    	ApiConnectionManager.getInstance(this);
    }		

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	switch(position) {
        	case 0:
        		startActivity(new Intent(this, TestContactsApi.class));
        		break;
        		
        	case 1:
        		startActivity(new Intent(this, TestCapabilitiesApi.class));
        		break;
        		
        	case 2:
        		startActivity(new Intent(this, TestMessagingApi.class));
        		break;
        		
        	case 3:
        		startActivity(new Intent(this, TestSharingApi.class));
        		break;
        		
        	case 4:
        		startActivity(new Intent(this, TestMultimediaSessionApi.class));
        		break;

        	case 5:
        		startActivity(new Intent(this, TestIPCallApi.class));
        		break;

        	case 6:
        		startActivity(new Intent(this, TestIntentsApi.class));
        		break;

        	case 7:
        		startActivity(new Intent(this, TestServiceApi.class));
        		break;

        	case 8:
        		startActivity(new Intent(this, InitiateFileUpload.class));
        		break;

        	case 9:
        		startActivity(new Intent(this, AboutRI.class));
        		break;
    	}
    }
    
}
