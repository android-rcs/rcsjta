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
package com.orangelabs.rcs.ri.intents;

import java.util.List;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.gsma.services.rcs.RcsUtils;
import com.orangelabs.rcs.ri.R;

/**
 * List existing RCS clients
 * 
 * @author Jean-Marc AUFFRET
 */
public class IntentServices extends ListActivity {
	/**
	 * List of clients detected
	 */
    private List<ResolveInfo> clients;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	    // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.intents_clients);

		// Set UI title
        setTitle(getString(R.string.menu_services));
        
		// Get the list of clients
	    clients = RcsUtils.getRcsServices(this);

	    // Set list adapter
        String[] items = new String[clients.size()];
        for(int i=0; i < clients.size(); i++) {
        	items[i] = clients.get(i).activityInfo.packageName;
        	RcsUtils.isRcsServiceActivated(this, clients.get(i), receiverResult);
        }
        setListAdapter(new ArrayAdapter<String>(
        	      this,
        	      android.R.layout.simple_expandable_list_item_1,
        	      items)); 
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		RcsUtils.loadRcsServiceSettings(this, clients.get(position));
	}

	/**
	 * Receive client status
	 */
	private BroadcastReceiver receiverResult = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = getResultExtras(false);
			if (bundle == null) {
				return;
			}
			String client = bundle.getString(com.gsma.services.rcs.Intents.Service.EXTRA_SERVICE);
			boolean status = bundle.getBoolean(com.gsma.services.rcs.Intents.Service.EXTRA_STATUS, false);
			
			for(int i=0; i < clients.size(); i++) {
				if (clients.get(i).activityInfo.packageName.equals(client)) {
					View v = IntentServices.this.getListView().getChildAt(i);
					if (status) {
						v.setBackgroundColor(Color.GREEN);
					} else {
						v.setBackgroundColor(Color.RED);
					}
				}
			}
	    }		
	};
}