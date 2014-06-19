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

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.JoynServiceRegistrationListener;
import com.gsma.services.rcs.capability.CapabilityService;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Display and monitor the registration status
 *  
 * @author Jean-Marc AUFFRET
 */
public class RegistrationStatus extends Activity implements JoynServiceListener {
	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();

	/**
	 * Service API
	 */
    private JoynService serviceApi;
    
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
        
        // Instanciate API
        serviceApi = new CapabilityService(getApplicationContext(), this);
                
        // Connect API
        serviceApi.connect();
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	// Unregister API listener
        try {
        	serviceApi.removeServiceRegistrationListener(registrationListener);
        } catch(JoynServiceException e) {
        	e.printStackTrace();
        }

        // Disconnect API
        serviceApi.disconnect();
    }

    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
        try {
        	// Display registration status
        	displayRegistrationStatus(serviceApi.isServiceRegistered());
        	
	        // Register registration listener
	        serviceApi.addServiceRegistrationListener(registrationListener);
        } catch(JoynServiceException e) {
        	e.printStackTrace();
    		Utils.showMessageAndExit(RegistrationStatus.this, getString(R.string.label_api_failed));
    	}
    }
    
    /**
     * Callback called when service has been disconnected. This method is called when
     * the service is disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     * @see JoynService.Error
     */
    public void onServiceDisconnected(int error) {
    	// Display registration status
    	displayRegistrationStatus(false);
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
