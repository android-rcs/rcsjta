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

package com.orangelabs.rcs.service.api;

import java.util.Hashtable;

import org.gsma.joyn.capability.Capabilities;
import org.gsma.joyn.capability.ICapabilitiesListener;
import org.gsma.joyn.capability.ICapabilityService;

import android.os.RemoteCallbackList;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.server.ServerApiException;
import com.orangelabs.rcs.service.api.server.ServerApiUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Capability API service
 */
public class CapabilityServiceImpl extends ICapabilityService.Stub {
    /**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * List of capabilities listeners
	 */
	private RemoteCallbackList<ICapabilitiesListener> capabilitiesListeners = new RemoteCallbackList<ICapabilitiesListener>();

	/**
	 * List of listeners per contact
	 */
	private Hashtable<String, RemoteCallbackList<ICapabilitiesListener>> contactCapalitiesListeners = new Hashtable<String, RemoteCallbackList<ICapabilitiesListener>>();

	/**
	 * Lock used for synchronization
	 */
	private Object lock = new Object();

	/**
	 * Constructor
	 */
	public CapabilityServiceImpl() {
		if (logger.isActivated()) {
			logger.info("Capability service API is loaded");
		}
	}

	/**
	 * Close API
	 */
	public void close() {
	}
    
    /**
     * Returns the capabilities supported by the local end user. The supported
     * capabilities are fixed by the MNO and read during the provisioning.
     * 
     * @return Capabilities
     */
	public Capabilities getMyCapabilities() {
		com.orangelabs.rcs.core.ims.service.capability.Capabilities c = RcsSettings.getInstance().getMyCapabilities();
    	return new Capabilities(c.isImageSharingSupported(),
    			c.isVideoSharingSupported(),
    			c.isImSessionSupported(),
    			c.isFileTransferSupported(),
    			c.getSupportedExtensions());
	}

    /**
     * Returns the capabilities of a given contact from the local database. This
     * method doesn’t request any network update to the remote contact.
     * 
     * @param contact Contact
     * @return Capabilities
     */
	public Capabilities getContactCapabilities(String contact) {
		return null;
	}

    /**
	 * Requests capabilities of a remote contact. This method initiates in background a
	 * new capability request to the remote contact by sending a SIP OPTIONS. The result
	 * of the capability request is then broadcasted asynchronously to the applications
	 * via the Intent CONTACT_CAPABILITIES. A capability resfresh is only sent if the
	 * timestamp associated to the capability has expired (the expiration value is fixed
	 * via MNO provisioning).
	 * 
	 * @param contact Contact
	 * @throws ServerApiException
	 */
	public void requestCapabilities(String contact) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Request capabilities for contact " + contact);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Request contact capabilities
			Core.getInstance().getCapabilityService().requestContactCapabilities(contact);
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

	/**
     * Receive capabilities from a contact
     * 
     * @param contact Contact
     * @param capabilities Capabilities
     */
    public void receiveCapabilities(String contact, com.orangelabs.rcs.core.ims.service.capability.Capabilities capabilities) {
    	synchronized(lock) {
    		if (logger.isActivated()) {
    			logger.info("Receive capabilities for " + contact);
    		}
	
    		// Create capabilities instance
    		Capabilities c = new Capabilities(
    				capabilities.isImageSharingSupported(),
    				capabilities.isVideoSharingSupported(),
    				capabilities.isImSessionSupported(),
    				capabilities.isFileTransferSupported(),
    				capabilities.getSupportedExtensions()); 

    		// Notify capabilities listeners
        	notifyListeners(contact, c, capabilitiesListeners);

    		// Notify capabilities listeners for a given contact
	        RemoteCallbackList<ICapabilitiesListener> listeners = contactCapalitiesListeners.get(contact);
	        if (listeners != null) {
	        	notifyListeners(contact, c, listeners);
	        }
    	}
    }
	
    /**
     * Notify listeners
     * 
     * @param capabilities Capabilities
     * @param listeners Listeners
     */
    private void notifyListeners(String contact, Capabilities capabilities, RemoteCallbackList<ICapabilitiesListener> listeners) {
		final int N = listeners.beginBroadcast();
        for (int i=0; i < N; i++) {
            try {
            	listeners.getBroadcastItem(i).handleNewCapabilities(contact, capabilities);
            } catch(Exception e) {
            	if (logger.isActivated()) {
            		logger.error("Can't notify listener", e);
            	}
            }
        }
        listeners.finishBroadcast();
    }
    
	/**
	 * Registers a listener for receiving capabilities of contacts
	 * 
	 * @param listener Capabilities listener
	 */
	public void addCapabilitiesListener(ICapabilitiesListener listener) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Add a listener");
			}

			capabilitiesListeners.register(listener);
		}
	}
	
	/**
	 * Unregisters a listener of capabilities
	 * 
	 * @param listener Capabilities listener
	 */
	public void removeCapabilitiesListener(ICapabilitiesListener listener) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Remove a listener");
			}
			
			capabilitiesListeners.unregister(listener);
    	}	
	}
	
	/**
	 * Registers a listener for receiving capabilities of a given contact
	 * 
	 * @param contact Contact
	 * @param listener Capabilities listener
	 */
	public void addContactCapabilitiesListener(String contact, ICapabilitiesListener listener) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Add a listener for contact " + contact);
			}

			RemoteCallbackList<ICapabilitiesListener> listeners = contactCapalitiesListeners.get(contact);
			if (listeners == null) {
				listeners = new RemoteCallbackList<ICapabilitiesListener>();
				contactCapalitiesListeners.put(contact, listeners);
			}
			listeners.register(listener);
		}
	}
	
	/**
	 * Unregisters a listener of capabilities for a given contact
	 * 
	 * @param contact Contact
	 * @param listener Capabilities listener
	 */
	public void removeContactCapabilitiesListener(String contact, ICapabilitiesListener listener) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Remove a listener for contact " + contact);
			}

			RemoteCallbackList<ICapabilitiesListener> listeners = contactCapalitiesListeners.get(contact);
			if (listeners != null) {
				listeners.unregister(listener);
			}
		}	
	}
}