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

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.gsma.joyn.capability.Capabilities;
import org.gsma.joyn.capability.ICapabilitiesListener;
import org.gsma.joyn.capability.ICapabilityService;

import android.os.RemoteCallbackList;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.server.ServerApiException;
import com.orangelabs.rcs.service.api.server.ServerApiUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Capability service API implementation
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
		Set<String> exts = new HashSet<String>(c.getSupportedExtensions());
		return new Capabilities(c.isImageSharingSupported(),
    			c.isVideoSharingSupported(),
    			c.isImSessionSupported(),
    			c.isFileTransferSupported(),
    			exts);
	}

    /**
     * Returns the capabilities of a given contact from the local database. This
     * method doesn’t request any network update to the remote contact. The parameter
     * contact supports the following formats: MSISDN in national or international
     * format, SIP address, SIP-URI or Tel-URI. If the format of the contact is not
     * supported an exception is thrown.
     * 
     * @param contact Contact
     * @return Capabilities
     */
	public Capabilities getContactCapabilities(String contact) {
		if (logger.isActivated()) {
			logger.info("Get capabilities for contact " + contact);
		}

		// Read capabilities in the local database
		com.orangelabs.rcs.core.ims.service.capability.Capabilities capabilities = ContactsManager.getInstance().getContactCapabilities(contact);
		if (capabilities != null) {
    		Set<String> exts = new HashSet<String>(capabilities.getSupportedExtensions());
			return new Capabilities(
    				capabilities.isImageSharingSupported(),
    				capabilities.isVideoSharingSupported(),
    				capabilities.isImSessionSupported(),
    				capabilities.isFileTransferSupported(),
    				exts); 
		} else {
			return null;
		}
	}

    /**
	 * Requests capabilities to a remote contact. This method initiates in background
	 * a new capability request to the remote contact by sending a SIP OPTIONS. The
	 * result of the capability request is sent asynchronously via callback method of
	 * the capabilities listener. A capability resfresh is only sent if the timestamp
	 * associated to the capability has expired (the expiration value is fixed via MNO
	 * provisioning). The parameter contact supports the following formats: MSISDN in
	 * national or international format, SIP address, SIP-URI or Tel-URI. If the format
	 * of the contact is not supported an exception is thrown. The result of the
	 * capability refresh request is provided to all the clients that have registered
	 * the listener for this event.
	 * 
	 * @param contact Contact
	 * @throws ServerApiException
	 */
	public void requestContactCapabilities(String contact) throws ServerApiException {
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
    		Set<String> exts = new HashSet<String>(capabilities.getSupportedExtensions());
    		Capabilities c = new Capabilities(
    				capabilities.isImageSharingSupported(),
    				capabilities.isVideoSharingSupported(),
    				capabilities.isImSessionSupported(),
    				capabilities.isFileTransferSupported(),
    				exts); 

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
            	listeners.getBroadcastItem(i).onCapabilitiesReceived(contact, capabilities);
            } catch(Exception e) {
            	if (logger.isActivated()) {
            		logger.error("Can't notify listener", e);
            	}
            }
        }
        listeners.finishBroadcast();
    }
    
    /**
	 * Requests capabilities for all contacts existing in the local address book. This
	 * method initiates in background new capability requests for each contact of the
	 * address book by sending SIP OPTIONS. The result of a capability request is sent
	 * asynchronously via callback method of the capabilities listener. A capability
	 * refresh is only sent if the timestamp associated to the capability has expired
	 * (the expiration value is fixed via MNO provisioning). The result of the capability
	 * refresh request is provided to all the clients that have registered the listener
	 * for this event.
	 * 
	 * @throws ServerApiException
	 */
	public void requestAllContactsCapabilities() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Request all contacts capabilities");
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Request all contacts capabilities
			List<String> contactList = ContactsManager.getInstance().getAllContacts();
			Core.getInstance().getCapabilityService().requestContactCapabilities(contactList);
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

	/**
	 * Registers a capabilities listener on any contact
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
	 * Unregisters a capabilities listener
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