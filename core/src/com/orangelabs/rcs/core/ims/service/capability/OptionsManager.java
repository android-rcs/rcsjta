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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.service.ContactInfo.RcsStatus;
import com.orangelabs.rcs.core.ims.service.ContactInfo.RegistrationState;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.utils.ContactUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Capability discovery manager using options procedure
 *  
 * @author jexa7410
 */
public class OptionsManager implements DiscoveryManager {
	/**
	 * Max number of threads for background processing
	 */
	private final static int MAX_PROCESSING_THREADS = 15;
	
    /**
     * IMS module
     */
    private ImsModule imsModule;
    
    /**
     * Thread pool to request capabilities in background
     */
    private ExecutorService threadPool;

    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(OptionsManager.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     */
    public OptionsManager(ImsModule parent) {
        this.imsModule = parent;
    }

    /**
     * Start the manager
     */
    public void start() {
    	threadPool = Executors.newFixedThreadPool(MAX_PROCESSING_THREADS);
    }

    /**
     * Stop the manager
     */
    public void stop() {
        try {
        	threadPool.shutdown();
        } catch (SecurityException e) {
            if (logger.isActivated()) {
            	logger.error("Could not stop all threads");
            }
        }
    }
    
	/**
     * Request contact capabilities
     * 
     * @param contact Remote contact identifier
     * @return Returns true if success
     */
    public boolean requestCapabilities(ContactId contact) {
    	if (logger.isActivated()) {
    		logger.debug("Request capabilities in background for " + contact);
    	}
    	
    	// Update capability time of last request
    	ContactsManager.getInstance().updateCapabilitiesTimeLastRequest(contact);
    	
    	// Start request in background
		try {
	    	boolean richcall = imsModule.getRichcallService().isCallConnectedWith(contact);
	    	OptionsRequestTask task = new OptionsRequestTask(imsModule, contact, CapabilityUtils.getSupportedFeatureTags(richcall));
	    	threadPool.submit(task);
	    	return true;
		} catch(Exception e) {
	    	if (logger.isActivated()) {
	    		logger.error("Can't submit task", e);
	    	}
	    	return false;
		}
    }

    /**
     * Request capabilities for a set of contacts
     *
     * @param contacts Contact set
     */
	public void requestCapabilities(Set<ContactId> contacts) {
        if (logger.isActivated()) {
            logger.debug("Request capabilities for " + contacts.size() + " contacts");
        }

        for (ContactId contact : contacts) {
			if (!requestCapabilities(contact)) {
		    	if (logger.isActivated()) {
		    		logger.debug("Processing has been stopped");
		    	}
				break;
			}
        }
	}

    /**
     * Receive a capability request (options procedure)
     * 
     * @param options Received options message
     */
	public void receiveCapabilityRequest(SipRequest options) {
		try {
			ContactId contact = ContactUtils.createContactId(SipUtils.getAssertedIdentity(options));
			if (logger.isActivated()) {
				logger.debug("OPTIONS request received from " + contact);
			}

			try {
				// Create 200 OK response
				String ipAddress = imsModule.getCurrentNetworkInterface().getNetworkAccess().getIpAddress();
				boolean richcall = imsModule.getRichcallService().isCallConnectedWith(contact);
				SipResponse resp = SipMessageFactory.create200OkOptionsResponse(options, imsModule.getSipManager().getSipStack()
						.getContact(), CapabilityUtils.getSupportedFeatureTags(richcall),
						CapabilityUtils.buildSdp(ipAddress, richcall));

				// Send 200 OK response
				imsModule.getSipManager().sendSipResponse(resp);
			} catch (Exception e) {
				if (logger.isActivated()) {
					logger.error("Can't send 200 OK for OPTIONS", e);
				}
			}

			// Read features tag in the request
			Capabilities capabilities = CapabilityUtils.extractCapabilities(options);

			// Update capabilities in database
			if (capabilities.isImSessionSupported()) {
				// RCS-e contact
				ContactsManager.getInstance().setContactCapabilities(contact, capabilities, RcsStatus.RCS_CAPABLE,
						RegistrationState.ONLINE);
			} else {
				// Not a RCS-e contact
				ContactsManager.getInstance().setContactCapabilities(contact, capabilities, RcsStatus.NOT_RCS,
						RegistrationState.UNKNOWN);
			}

			// Notify listener
			imsModule.getCore().getListener().handleCapabilitiesNotification(contact, capabilities);
		} catch (RcsContactFormatException e1) {
			if (logger.isActivated()) {
				logger.warn("Cannot parse contact from capability request");
			}
		}
	}
}