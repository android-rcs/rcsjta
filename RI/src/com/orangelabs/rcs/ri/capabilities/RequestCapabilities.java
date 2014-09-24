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

import java.util.Set;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.gsma.services.rcs.JoynContactFormatException;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceNotAvailableException;
import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.capability.CapabilitiesListener;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsService;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Refresh capabilities of a given contact
 * 
 * @author Jean-Marc AUFFRET
 */
public class RequestCapabilities extends Activity {
	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();
	
    /**
   	 * A locker to exit only once
   	 */
   	private LockAccess exitOnce = new LockAccess();
   	
  	/**
	 * API connection manager
	 */
	private ApiConnectionManager connectionManager;
	
    /**
     * Capabilities listener
     */
    private MyCapabilitiesListener capabilitiesListener = new MyCapabilitiesListener();
    
    /**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(RequestCapabilities.class.getSimpleName());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.capabilities_request);
        
        // Set title
        setTitle(R.string.menu_capabilities);
        
        // Set the contact selector
        Spinner spinner = (Spinner)findViewById(R.id.contact);
        spinner.setAdapter(Utils.createContactListAdapter(this));
        spinner.setOnItemSelectedListener(listenerContact);
        
		// Set button callback
        Button refreshBtn = (Button)findViewById(R.id.refresh_btn);
        refreshBtn.setOnClickListener(btnRefreshListener);
        
        // Update refresh button
        if (spinner.getAdapter().getCount() == 0) {
        	 // Disable button if no contact available
            refreshBtn.setEnabled(false);
        } else {
            refreshBtn.setEnabled(true);        	
        }
        
		// Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);
		if (connectionManager == null || !connectionManager.isServiceConnected(RcsService.CAPABILITY)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
			return;
		}
		connectionManager.startMonitorServices(this, null, RcsService.CAPABILITY);
		try {
			// Add service listener
			connectionManager.getCapabilityApi().addCapabilitiesListener(capabilitiesListener);
		} catch (JoynServiceException e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Failed to add listener", e);
			}
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
			// Remove image sharing listener
			try {
				connectionManager.getCapabilityApi().removeCapabilitiesListener(capabilitiesListener);
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Failed to remove listener", e);
				}
			}
		}
    }
    
    /**
     * Capabilities event listener
     */
    private class MyCapabilitiesListener extends CapabilitiesListener {
	    /**
	     * Callback called when new capabilities are received for a given contact
	     * 
	     * @param contact Contact
	     * @param capabilities Capabilities
	     */
		public void onCapabilitiesReceived(final ContactId contact, final Capabilities capabilities) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onCapabilitiesReceived " + contact);
			}
			handler.post(new Runnable() {
				public void run() {
					// Check if this intent concerns the current selected contact
					if (contact != null && contact.equals(getSelectedContact())) {
						// Update UI
						displayCapabilities(capabilities);
					}
				}
			});
		};
    }
    
    /**
     * Spinner contact listener
     */
    private OnItemSelectedListener listenerContact = new OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			CapabilityService capabilityApi = connectionManager.getCapabilityApi();
            try {
		        // Get selected contact
				String contact = getContactAtPosition(position);
				ContactId contactId = ContactUtils.getInstance(RequestCapabilities.this).formatContactId(contact);
				// Get current capabilities
				Capabilities currentCapabilities = capabilityApi.getContactCapabilities(contactId);
	
				// Display default capabilities
		        displayCapabilities(currentCapabilities);

		        // Check if the service is available
		    	boolean registered = false;
		    	try {
		    		registered = capabilityApi.isServiceRegistered();
		    	} catch(Exception e) {
		    		e.printStackTrace();
		    	}
		    	if (!registered) {
		    		Utils.showMessage(RequestCapabilities.this, getString(R.string.label_service_not_available));
			    	return;
		        }      	
		    	
		    	// Update capabilities
				updateCapabilities(contactId);
		    } catch(JoynServiceNotAvailableException e) {
		    	e.printStackTrace();
				Utils.showMessageAndExit(RequestCapabilities.this, getString(R.string.label_api_disabled), exitOnce);
		    } catch(JoynServiceException e) {
		    	e.printStackTrace();
				Utils.showMessageAndExit(RequestCapabilities.this, getString(R.string.label_api_failed), exitOnce);
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
		}
	};
	
    /**
     * Returns the contact at given position
     * 
     * @param position Position in the adapter
     * @return Contact
     */
    private String getContactAtPosition(int position) {
	    Spinner spinner = (Spinner)findViewById(R.id.contact);
	    MatrixCursor cursor = (MatrixCursor)spinner.getItemAtPosition(position);
	    return cursor.getString(1);
    }
    
    /**
     * Returns the selected contact
     * 
     * @return Contact
     */
    private ContactId getSelectedContact() {
	    Spinner spinner = (Spinner)findViewById(R.id.contact);
	    MatrixCursor cursor = (MatrixCursor)spinner.getSelectedItem();
	    ContactUtils contactUtils = ContactUtils.getInstance(RequestCapabilities.this);
		try {
			return contactUtils.formatContactId(cursor.getString(1));
		} catch (JoynContactFormatException e) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "getSelectedContact cannot parse contact " + cursor.getString(1));
			}
			return null;
		}
    }

    /**
     * Request button callback
     */
    private OnClickListener btnRefreshListener = new OnClickListener() {
        public void onClick(View v) {
            // Check if the service is available
        	boolean registered = false;
        	try {
        		registered = connectionManager.getCapabilityApi().isServiceRegistered();
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
            if (!registered) {
    	    	Utils.showMessage(RequestCapabilities.this, getString(R.string.label_service_not_available));
    	    	return;
            }  

			ContactId contact = getSelectedContact();
			if (contact != null) {
				updateCapabilities(contact);
			}
        }
    };

    /**
     * Update capabilities of a given contact
     * 
     * @param contact Contact
     */
    private void updateCapabilities(ContactId contact) {
        // Display info
        Utils.displayLongToast(RequestCapabilities.this, getString(R.string.label_request_in_background, contact));
        // Request capabilities
    	try {
	        // Request new capabilities
	        connectionManager.getCapabilityApi().requestContactCapabilities(contact);
	    } catch(JoynServiceNotAvailableException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_disabled), exitOnce);
	    } catch(JoynServiceException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
		}
    }
    
    /**
     * Display the capabilities
     * 
     * @param capabilities Capabilities
     */
    private void displayCapabilities(Capabilities capabilities) {
    	CheckBox imageCSh = (CheckBox)findViewById(R.id.image_sharing);
		CheckBox videoCSh = (CheckBox)findViewById(R.id.video_sharing);
		CheckBox ft = (CheckBox)findViewById(R.id.file_transfer);
		CheckBox im = (CheckBox)findViewById(R.id.im);
		CheckBox geoloc = (CheckBox)findViewById(R.id.geoloc_push);
		CheckBox ipVoiceCall = (CheckBox)findViewById(R.id.ip_voice_call);
		CheckBox ipVideoCall = (CheckBox)findViewById(R.id.ip_video_call);
		TextView extensions = (TextView)findViewById(R.id.extensions);
    	if (capabilities != null) {
    		// Set capabilities
    		imageCSh.setChecked(capabilities.isImageSharingSupported());
    		videoCSh.setChecked(capabilities.isVideoSharingSupported());
    		ft.setChecked(capabilities.isFileTransferSupported());
    		im.setChecked(capabilities.isImSessionSupported());
    		geoloc.setChecked(capabilities.isGeolocPushSupported());
    		ipVoiceCall.setChecked(capabilities.isIPVoiceCallSupported());
    		ipVideoCall.setChecked(capabilities.isIPVideoCallSupported());

            // Set extensions
    		extensions.setVisibility(View.VISIBLE);
            String result = "";
            Set<String> extensionList = capabilities.getSupportedExtensions();
	        for(String value : extensionList) {
	        	if (value.length() > 0) {
	        		result += value + "\n";
	        	}
            }
            extensions.setText(result);    		
    	}
    }
}
