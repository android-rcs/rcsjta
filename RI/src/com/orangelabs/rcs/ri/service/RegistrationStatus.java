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
package com.orangelabs.rcs.ri.service;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceNotAvailableException;
import com.gsma.services.rcs.JoynServiceRegistrationListener;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsService;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Display and monitor the registration status
 *  
 * @author Jean-Marc AUFFRET
 */
public class RegistrationStatus extends Activity {
	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();

  	/**
	 * API connection manager
	 */
	private ApiConnectionManager connectionManager;
	
	/**
   	 * A locker to exit only once
   	 */
   	private LockAccess exitOnce = new LockAccess();
    
    /**
     * Registration listener
     */
    private MyRegistrationListener registrationListener = new MyRegistrationListener(); 

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.service_registration);
        
        // Set title
        setTitle(R.string.menu_registration_status);
        
    	// Display registration status by default
    	displayRegistrationStatus(false);        
        
		// Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);
		if (connectionManager == null || !connectionManager.isServiceConnected(RcsService.CAPABILITY)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
			return;
		}
		connectionManager.startMonitorServices(this, null, RcsService.CAPABILITY);
		try {
			// Add service listener
			connectionManager.getCapabilityApi().addServiceRegistrationListener(registrationListener);
		} catch (JoynServiceException e) {
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
		}
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	if (connectionManager == null) {
    		return;
    	}
		connectionManager.stopMonitorServices(this);
    	if (connectionManager.isServiceConnected(RcsService.CAPABILITY)) {
			// Remove listener
			try {
				connectionManager.getCapabilityApi().removeServiceRegistrationListener(registrationListener);
			} catch (Exception e) {
			}
		}
    }

    
    @Override
	protected void onResume() {
		super.onResume();
		try {
			// Display registration status
			displayRegistrationStatus(connectionManager.getCapabilityApi().isServiceRegistered());
		} catch (JoynServiceNotAvailableException e) {
			Utils.showMessageAndExit(RegistrationStatus.this, getString(R.string.label_api_failed));
		} catch (JoynServiceException e) {
			Utils.showMessageAndExit(RegistrationStatus.this, getString(R.string.label_api_failed));
		}
	}
    
    /**
     * Registration event listener
     */
    private class MyRegistrationListener extends JoynServiceRegistrationListener {
    	// Service is registered to the network platform
    	public void onServiceRegistered() {
			handler.post(new Runnable(){
				public void run(){
		        	// Display registration status
		        	displayRegistrationStatus(true);
				}
			});
    	}
    	
    	// Service is unregistered from the network platform
    	public void onServiceUnregistered() {
			handler.post(new Runnable(){
				public void run(){
		        	// Display registration status
		        	displayRegistrationStatus(false);
				}
			});
    	}
    }
    
    /**
     * Display registration status
     * 
     * @param status Status
     */
    private void displayRegistrationStatus(boolean status) {
    	TextView statusTxt = (TextView)findViewById(R.id.registration_status);			
    	statusTxt.setText("" + status);
    }
}
