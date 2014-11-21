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
package com.orangelabs.rcs.ri.capabilities;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Request capabilities of all contacts
 * 
 * @author Jean-Marc AUFFRET
 */
public class RequestAllCapabilities extends Activity {
   
  	/**
	 * API connection manager
	 */
	private ApiConnectionManager connectionManager;
	
	/**
   	 * A locker to exit only once
   	 */
   	private LockAccess exitOnce = new LockAccess();
   	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.capabilities_refresh);
        
		// Set buttons callback
        Button refreshBtn = (Button)findViewById(R.id.refresh_btn);
        refreshBtn.setOnClickListener(btnSyncListener);        
        
		// Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);
		if (connectionManager == null || !connectionManager.isServiceConnected(RcsServiceName.CAPABILITY)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
			return;
		}
		connectionManager.startMonitorServices(this, null, RcsServiceName.CAPABILITY);
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	if (connectionManager != null) {
			connectionManager.stopMonitorServices(this);
    	}
    }

    /**
     * Publish button listener
     */
    private OnClickListener btnSyncListener = new OnClickListener() {
        public void onClick(View v) {
            // Check if the service is available
        	boolean registered = false;
        	try {
        		registered = connectionManager.getCapabilityApi().isServiceRegistered();
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
            if (!registered) {
    	    	Utils.showMessage(RequestAllCapabilities.this, getString(R.string.label_service_not_available));
    	    	return;
            }        	
        	
        	try {
    			// Refresh all contacts
                connectionManager.getCapabilityApi().requestAllContactsCapabilities();
                
        		// Display message
    			Utils.displayLongToast(RequestAllCapabilities.this, getString(R.string.label_refresh_success));
        	} catch(Exception e) {
        		Utils.showMessageAndExit(RequestAllCapabilities.this, getString(R.string.label_refresh_failed), exitOnce);
        	}
        }
    };
}
