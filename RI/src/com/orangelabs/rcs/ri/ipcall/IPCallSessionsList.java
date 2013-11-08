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
package com.orangelabs.rcs.ri.ipcall;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.ipcall.IPCall;
import org.gsma.joyn.ipcall.IPCallService;

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
 * List of current IP calls
 * 
 * @author Jean-Marc AUFFRET
 */
public class IPCallSessionsList extends ListActivity implements JoynServiceListener {
	/**
	 * IP call API
	 */
	private IPCallService callApi;

	/**
	 * List of calls
	 */
	private List<IPCall> calls = new ArrayList<IPCall>();

    /**
	 * API connection state
	 */
	private boolean apiEnabled = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.ipcall_list);

        // Set title
        setTitle(R.string.menu_ipcall_list);

        // Instanciate API
        callApi = new IPCallService(getApplicationContext(), this);
        
        // Connect API
        callApi.connect();
	}
	
	@Override
	protected void onResume() {
		super.onResume();

    	// Update the list of sessions
		updateList();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

        // Disconnect API
        callApi.disconnect();
	}	
	
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
		apiEnabled = true;

		// Display the list of calls
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
		apiEnabled = false;

		Utils.showMessageAndExit(IPCallSessionsList.this, getString(R.string.label_api_disabled));
    }    
    
    /**
     * Callback called when service is registered to the RCS/IMS platform
     */
    public void onServiceRegistered() {
    	// Nothing to do here
    }
    
    /**
     * Callback called when service is unregistered from the RCS/IMS platform
     */
    public void onServiceUnregistered() {
    	// Update the list of sessions
		updateList();
    }      
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		// Display the selected session
		try {
			Intent intent = new Intent(this, IPCallView.class);
			String callId = calls.get(position).getCallId();
			intent.putExtra(IPCallView.EXTRA_MODE, IPCallView.MODE_OPEN);
			intent.putExtra(IPCallView.EXTRA_CALL_ID, callId);
			startActivity(intent);
		} catch(JoynServiceException e) {
			Utils.showMessageAndExit(IPCallSessionsList.this, getString(R.string.label_api_failed));
		}
	}

    /**
     * Update the displayed list
     */
    private void updateList() {
		try {
			// Reset the list
			calls.clear();

			if (apiEnabled) {
		    	// Get list of pending sessions
		    	Set<IPCall> currentCalls = callApi.getIPCalls();
		    	calls = new ArrayList<IPCall>(currentCalls);
				if (calls.size() > 0){
			        String[] items = new String[calls.size()];    
			        for (int i = 0; i < items.length; i++) {
						items[i] = getString(R.string.label_session, calls.get(i).getCallId());
			        }
					setListAdapter(new ArrayAdapter<String>(IPCallSessionsList.this, android.R.layout.simple_list_item_1, items));
				} else {
					setListAdapter(null);
				}
			}
		} catch(Exception e) {
			Utils.showMessageAndExit(IPCallSessionsList.this, getString(R.string.label_api_failed));
		}
    }
}