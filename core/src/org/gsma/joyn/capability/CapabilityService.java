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

package org.gsma.joyn.capability;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceNotAvailableException;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * Capability service offers the main entry point to the Capability
 * service which permits to read capabilities of remote contacts, to
 * initiate capability discovery and to receive capabilities updates.
 * Several applications may connect/disconnect to the API.
 * 
 * The parameter contact in the API supports the following formats:
 * MSISDN in national or international format, SIP address, SIP-URI
 * or Tel-URI.
 */
public class CapabilityService extends JoynService {
    /**
     * Intent broadcasted to discover extensions
     */
    public final static String INTENT_EXTENSIONS = "org.gsma.joyn.capability.EXTENSION";
    
	/**
	 * Extension base anme
	 */
	public final static String EXTENSION_BASE_NAME = "+g.3gpp.iari-ref";

	/**
	 * Extension prefix name
	 */
	public final static String EXTENSION_PREFIX_NAME = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcse";	    

	/**
	 * API
	 */
	private ICapabilityService api = null;
	
    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public CapabilityService(Context ctx, JoynServiceListener listener) {
    	super(ctx, listener);
    }

    /**
     * Connects to the API
     */
    public void connect() {
    	ctx.bindService(new Intent(ICapabilityService.class.getName()), apiConnection, 0);
    }
    
    /**
     * Disconnects from the API
     */
    public void disconnect() {
    	try {
    		ctx.unbindService(apiConnection);
        } catch(IllegalArgumentException e) {
        	// Nothing to do
        }
    }
    
	/**
	 * Service connection
	 */
	private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	api = ICapabilityService.Stub.asInterface(service);
        	if (serviceListener != null) {
        		serviceListener.handleServiceConnected();
        	}
        }

        public void onServiceDisconnected(ComponentName className) {
        	api = null;
        	if (serviceListener != null) {
        		serviceListener.handleServiceDisconnected();
        	}
        }
    };
    
    /**
     * Returns the capabilities supported by the local end user. The supported
     * capabilities are fixed by the MNO and read during the provisioning.
     * 
     * @return Capabilities
     * @throws JoynServiceException
     */
    public Capabilities getMyCapabilities() throws JoynServiceException {
		if (api != null) {
			try {
				return api.getMyCapabilities();
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
    }
    
    /**
     * Returns the capabilities of a given contact from the local database. This
     * method doesn’t request any network update to the remote contact.
     * 
     * @param contact Contact
     * @return Capabilities
     * @throws JoynServiceException
     */
    public Capabilities getContactCapabilities(String contact) throws JoynServiceException {
		if (api != null) {
			try {
				return api.getContactCapabilities(contact);
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
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
	 * @throws JoynServiceException
	 */
	public void requestCapabilities(String contact) throws JoynServiceException {
		if (api != null) {
			try {
				api.requestCapabilities(contact);
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

    /**
	 * Requests capabilities for a group of remote contacts. This method initiates in
	 * background new capability requests to remote contacts by sending a SIP OPTIONS
	 * for each contact. The result of the capability request is then broadcasted
	 * asynchronously to the applications via the Intent CONTACT_CAPABILITIES. A
	 * capability resfresh is only sent if the timestamp associated to the capability
	 * has expired (the expiration value is fixed via MNO provisioning).
	 * 
	 * @param contacts List of contacts
	 * @throws JoynServiceException
	 */
	public void requestCapabilities(List<String> contacts) throws JoynServiceException {
		for(int i=0; i < contacts.size(); i++) {
			requestCapabilities(contacts.get(i));
		}
	}

	/**
	 * Registers a listener for receiving capabilities of contacts
	 * 
	 * @param listener Capabilities listener
	 * @throws JoynServiceException
	 */
	public void addCapabilitiesListener(ICapabilitiesListener listener) throws JoynServiceException {
		if (api != null) {
			try {
				api.addCapabilitiesListener(listener);
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

	/**
	 * Unregisters a listener of capabilities
	 * 
	 * @param listener Capabilities listener
	 * @throws JoynServiceException
	 */
	public void removeCapabilitiesListener(ICapabilitiesListener listener) throws JoynServiceException {
		if (api != null) {
			try {
				api.removeCapabilitiesListener(listener);
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

	/**
	 * Registers a listener for receiving capabilities of a given contact
	 * 
	 * @param contacts Set of contacts
	 * @param listener Capabilities listener
	 * @throws JoynServiceException
	 */
	public void addCapabilitiesListener(Set<String> contacts, ICapabilitiesListener listener) throws JoynServiceException {
		if (api != null) {
			try {
				Iterator<String> list = contacts.iterator();
				while(list.hasNext()) { 
					String contact = list.next();
					api.addContactCapabilitiesListener(contact, listener);
				}
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

	/**
	 * Unregisters a listener of capabilities for a given contact
	 * 
	 * @param contacts Set of contacts
	 * @param listener Capabilities listener
	 * @throws JoynServiceException
	 */
	public void removeCapabilitiesListener(Set<String> contacts, ICapabilitiesListener listener) throws JoynServiceException {
		if (api != null) {
			try {
				Iterator<String> list = contacts.iterator();
				while(list.hasNext()) { 
					String contact = list.next();
					api.removeContactCapabilitiesListener(contact, listener);
				}
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}
}
