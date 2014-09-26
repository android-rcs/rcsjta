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

import java.util.Set;

import com.gsma.services.rcs.contacts.ContactId;
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
    private final static Logger logger = Logger.getLogger(PollingManager.class.getSimpleName());
    
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
		Set<ContactId> contacts = ContactsManager.getInstance().getAllContacts();
		for (ContactId contact : contacts) {
			requestContactCapabilities(contact);
		}
		
		// Restart timer
		startTimer(pollingPeriod, 1);		
    }
    
	/**
	 * Request contact capabilities 
	 * 
	 * @param contact Contact identifier
	 */
	private void requestContactCapabilities(ContactId contact) {
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
			if (isCapabilityRefreshRequired(capabilities.getTimestampOfLastRefresh())) {
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

	/**
	 * Check if refresh of capability is required
	 * 
	 * @param timestampOfLastRefresh
	 *            time of last capability refresh in milliseconds
	 * @return true if capability refresh is required
	 */
	/* package private */ static boolean isCapabilityRefreshRequired(long timestampOfLastRefresh) {
		long now = System.currentTimeMillis();
		// Is current time before last capability refresh ? (may occur if system time has been modified)
		if (now < timestampOfLastRefresh) {
			return true;
		}
		// Is current time after capability expiration time ? 
		return (now > (timestampOfLastRefresh + RcsSettings.getInstance().getCapabilityExpiryTimeout() * 1000));
	}	    
}
