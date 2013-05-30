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
package com.orangelabs.rcs.core.ims.service.capability;

import java.util.List;

import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.PeriodicRefresher;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Polling manager which updates capabilities periodically
 * 
 * @author Jean-Marc AUFFRET
 */
public class PollingManager extends PeriodicRefresher {
	/**
	 * Capability expiry timeout in seconds
	 */
	private static final int CAPABILITY_EXPIRY_PERIOD = RcsSettings.getInstance().getCapabilityExpiryTimeout();

	/**
     * Capability service
     */
    private CapabilityService imsService;
    
    /**
	 * Polling period (in seconds)
	 */
	private int pollingPeriod;
	
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    /**
	 * Constructor
	 * 
     * @param parent IMS service
	 */
	public PollingManager(CapabilityService parent) {
		this.imsService = parent;
		this.pollingPeriod = RcsSettings.getInstance().getCapabilityPollingPeriod();
	}
	
	/**
	 * Start polling
	 */
	public void start() {
		if (pollingPeriod == 0) {
			return;
		}
		startTimer(pollingPeriod, 1);
	}
	
	/**
	 * Stop polling
	 */
	public void stop() {
		stopTimer();
	}
	
	/**
     * Update processing
     */
    public void periodicProcessing() {
        // Make a registration
    	if (logger.isActivated()) {
    		logger.info("Execute new capabilities update");
    	}
    	
    	// Update all contacts capabilities if refresh timeout has not expired
		List<String> contactList = ContactsManager.getInstance().getAllContacts();
		for (int i=0; i < contactList.size(); i++) {
			String contact = contactList.get(i);
			requestContactCapabilities(contact);
		}
		
		// Restart timer
		startTimer(pollingPeriod, 1);		
    }
    
	/**
	 * Request contact capabilities 
	 * 
	 * @param contact Contact
	 */
	private void requestContactCapabilities(String contact) {
    	if (logger.isActivated()) {
    		logger.debug("Request capabilities for " + contact);
    	}

		// Read capabilities from the database
		Capabilities capabilities = ContactsManager.getInstance().getContactCapabilities(contact);
		if (capabilities == null) {
	    	if (logger.isActivated()) {
	    		logger.debug("No capability exist for " + contact);
	    	}

            // New contact: request capabilities from the network
    		imsService.getOptionsManager().requestCapabilities(contact);
		} else {
	    	if (logger.isActivated()) {
	    		logger.debug("Capabilities exist for " + contact);
	    	}
			long delta = (System.currentTimeMillis()-capabilities.getTimestamp())/1000;
			if ((delta >= CAPABILITY_EXPIRY_PERIOD) || (delta < 0)) {
		    	if (logger.isActivated()) {
		    		logger.debug("Capabilities have expired for " + contact);
		    	}

		    	// Capabilities are too old: request capabilities from the network
		    	if (capabilities.isPresenceDiscoverySupported()) {
			    	// If contact supports capability discovery via presence, use the selected discoveryManager
		    		imsService.getAnonymousFetchManager().requestCapabilities(contact);
		    	} else {
		    		// The contact only supports OPTIONS requests
		    		imsService.getOptionsManager().requestCapabilities(contact);
		    	}
			}
		}
	}	    
}
