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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.extension.IMultimediaMessagingSession;
import com.gsma.services.rcs.extension.IMultimediaMessagingSessionListener;
import com.gsma.services.rcs.extension.IMultimediaSessionService;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionIntent;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.ims.network.sip.FeatureTags;
import com.orangelabs.rcs.core.ims.service.sip.GenericSipSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Multimedia session API service
 * 
 * @author Jean-Marc AUFFRET
 */
public class MultimediaSessionServiceImpl extends IMultimediaSessionService.Stub {
	/**
	 * List of service event listeners
	 */
	private RemoteCallbackList<IJoynServiceRegistrationListener> serviceListeners = new RemoteCallbackList<IJoynServiceRegistrationListener>();

	/**
	 * List of sessions
	 */
	private static Hashtable<String, IMultimediaMessagingSession> messagingSessions = new Hashtable<String, IMultimediaMessagingSession>();  

	/**
	 * Lock used for synchronization
	 */
	private Object lock = new Object();
	
	/**
	 * The logger
	 */
	private static Logger logger = Logger.getLogger(MultimediaSessionServiceImpl.class.getName());

	/**
	 * Constructor
	 */
	public MultimediaSessionServiceImpl() {
		if (logger.isActivated()) {
			logger.info("Multimedia session API is loaded");
		}
	}

	/**
	 * Close API
	 */
	public void close() {
		// Clear list of sessions
		messagingSessions.clear();
		
		if (logger.isActivated()) {
			logger.info("Multimedia session service API is closed");
		}
	}
	
	/**
	 * Add a SIP session in the list
	 * 
	 * @param session SIP session
	 */
	protected static void addSipSession(MultimediaMessagingSessionImpl session) {
		if (logger.isActivated()) {
			logger.debug("Add a multimedia session in the list (size=" + messagingSessions.size() + ")");
		}
		
		messagingSessions.put(session.getSessionId(), session);
	}

	/**
	 * Remove a SIP session from the list
	 * 
	 * @param sessionId Session ID
	 */
	protected static void removeSipSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove a multimedia session from the list (size=" + messagingSessions.size() + ")");
		}
		
		messagingSessions.remove(sessionId);
	}	
	
    /**
     * Returns true if the service is registered to the platform, else returns false
     * 
	 * @return Returns true if registered else returns false
     */
    public boolean isServiceRegistered() {
    	return ServerApiUtils.isImsConnected();
    }

	/**
	 * Registers a listener on service registration events
	 * 
	 * @param listener Service registration listener
	 */
	public void addServiceRegistrationListener(IJoynServiceRegistrationListener listener) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Add a service listener");
			}

			serviceListeners.register(listener);
		}
	}
	
	/**
	 * Unregisters a listener on service registration events
	 * 
	 * @param listener Service registration listener
	 */
	public void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Remove a service listener");
			}
			
			serviceListeners.unregister(listener);
    	}	
	}

    /**
     * Receive registration event
     * 
     * @param state Registration state
     */
    public void notifyRegistrationEvent(boolean state) {
    	// Notify listeners
    	synchronized(lock) {
			final int N = serviceListeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	if (state) {
	            		serviceListeners.getBroadcastItem(i).onServiceRegistered();
	            	} else {
	            		serviceListeners.getBroadcastItem(i).onServiceUnregistered();
	            	}
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        serviceListeners.finishBroadcast();
	    }    	    	
    }	
	
	/**
	 * Receive a new SIP session invitation
	 * 
     * @param intent Resolved intent
     * @param session SIP session
	 */
	public void receiveSipSessionInvitation(Intent intent, GenericSipSession session) {
		// Add session in the list
		MultimediaMessagingSessionImpl sessionApi = new MultimediaMessagingSessionImpl(session);
		MultimediaSessionServiceImpl.addSipSession(sessionApi);
		
		// Broadcast intent related to the received invitation
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());
    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
		intent.putExtra(MultimediaMessagingSessionIntent.EXTRA_CONTACT, number);
		intent.putExtra(MultimediaMessagingSessionIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
		intent.putExtra(MultimediaMessagingSessionIntent.EXTRA_SESSION_ID, session.getSessionID());
		
		// Broadcast intent related to the received invitation
		AndroidFactory.getApplicationContext().sendBroadcast(intent);    	
	}
	
    /**
     * Initiates a new session for real time messaging with a remote contact and for a given
     * service extension. The messages are exchanged in real time during the session may be from
     * any type. The parameter contact supports the following formats: MSISDN in national or
     * international format, SIP address, SIP-URI or Tel-URI. If the format of the contact is
     * not supported an exception is thrown.
     * 
     * @param serviceId Service ID
     * @param contact Contact
     * @param listener Multimedia messaging session event listener
     * @return Multimedia messaging session
     * @return Multimedia session
	 * @throws ServerApiException
     */
    public IMultimediaMessagingSession initiateMessagingSession(String serviceId, String contact, IMultimediaMessagingSessionListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate a multimedia session with " + contact);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Initiate a new session
			String featureTag = FeatureTags.FEATURE_RCSE + "=\"" + FeatureTags.FEATURE_RCSE_EXTENSION + "." + serviceId + "\"";
			final GenericSipSession session = Core.getInstance().getSipService().initiateSession(contact, featureTag);
			
			// Add session listener
			MultimediaMessagingSessionImpl sessionApi = new MultimediaMessagingSessionImpl(session);
			sessionApi.addEventListener(listener);

			// Start the session
	        Thread t = new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	};
	    	t.start();
			
			// Add session in the list
			MultimediaSessionServiceImpl.addSipSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

    /**
     * Returns a current messaging session from its unique session ID
     * 
     * @return Multimedia messaging session or null if not found
     * @throws ServerApiException
     */
    public IMultimediaMessagingSession getMessagingSession(String sessionId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get multimedia session " + sessionId);
		}

		return messagingSessions.get(sessionId);
	}


    /**
     * Returns the list of messaging sessions associated to a given service ID
     * 
     * @param serviceId Service ID
     * @return List of sessions
     * @throws ServerApiException
     */
    public List<IBinder> getMessagingSessions(String serviceId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get multimedia sessions for service " + serviceId);
		}

		try {
			ArrayList<IBinder> result = new ArrayList<IBinder>();
			for (IMultimediaMessagingSession sessionApi : messagingSessions.values()) {
				// Filter on the service ID
				if (sessionApi.getServiceId().equals(serviceId)) {
					result.add(sessionApi.asBinder());
				}
			}
			return result;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

	/**
	 * Returns service version
	 * 
	 * @return Version
	 * @see JoynService.Build.VERSION_CODES
	 * @throws ServerApiException
	 */
	public int getServiceVersion() throws ServerApiException {
		return JoynService.Build.API_VERSION;
	}
}
