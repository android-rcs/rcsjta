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

import org.gsma.joyn.IJoynServiceRegistrationListener;
import org.gsma.joyn.JoynService;
import org.gsma.joyn.session.IMultimediaSession;
import org.gsma.joyn.session.IMultimediaSessionListener;
import org.gsma.joyn.session.IMultimediaSessionService;
import org.gsma.joyn.session.MultimediaMessageIntent;
import org.gsma.joyn.session.MultimediaSessionIntent;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.sip.GenericSipSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * SIP API service
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
	private static Hashtable<String, IMultimediaSession> sipSessions = new Hashtable<String, IMultimediaSession>();  

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
		sipSessions.clear();
		
		if (logger.isActivated()) {
			logger.info("Multimedia session service API is closed");
		}
	}
	
	/**
	 * Add a SIP session in the list
	 * 
	 * @param session SIP session
	 */
	protected static void addSipSession(MultimediaSessionImpl session) {
		if (logger.isActivated()) {
			logger.debug("Add a multimedia session in the list (size=" + sipSessions.size() + ")");
		}
		
		sipSessions.put(session.getSessionId(), session);
	}

	/**
	 * Remove a SIP session from the list
	 * 
	 * @param sessionId Session ID
	 */
	protected static void removeSipSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove a multimedia session from the list (size=" + sipSessions.size() + ")");
		}
		
		sipSessions.remove(sessionId);
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
		MultimediaSessionImpl sessionApi = new MultimediaSessionImpl(session);
		MultimediaSessionServiceImpl.addSipSession(sessionApi);
		
		// Broadcast intent related to the received invitation
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());
    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
		intent.putExtra(MultimediaSessionIntent.EXTRA_CONTACT, number);
		intent.putExtra(MultimediaSessionIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
		intent.putExtra(MultimediaSessionIntent.EXTRA_SESSION_ID, session.getSessionID());
		
		// Broadcast intent related to the received invitation
		AndroidFactory.getApplicationContext().sendBroadcast(intent);    	
	}
	
    /**
     * Initiate a new multimedia session with a remote contact and for a given service.
     * The SDP (Session Description Protocol) parameter is used to describe the supported
     * media. The parameter contact supports the following formats: MSISDN in national or
     * international format, SIP address, SIP-URI or Tel-URI. If the format of the contact
     * is not supported an exception is thrown.
     * 
     * @param serviceId Service ID
     * @param contact Contact
     * @param sdp Local SDP
     * @param listener Multimedia session event listener
     * @return Multimedia session
	 * @throws ServerApiException
     */
    public IMultimediaSession initiateSession(String serviceId, String contact, String sdp, IMultimediaSessionListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate a multimedia session with " + contact);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Initiate a new session
			GenericSipSession session = Core.getInstance().getSipService().initiateSession(contact,	serviceId, sdp);
			
			// Add session listener
			MultimediaSessionImpl sessionApi = new MultimediaSessionImpl(session);
			sessionApi.addEventListener(listener);

			// Start the session
			session.startSession();
			
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
     * Returns a current session from its unique session ID
     * 
     * @return Multimedia session or null if not found
     * @throws ServerApiException
     */
    public IMultimediaSession getSession(String sessionId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get multimedia session " + sessionId);
		}

		return sipSessions.get(sessionId);
	}


    /**
     * Returns the list of sessions associated to a given service ID
     * 
     * @param serviceId Service ID
     * @return List of sessions
     * @throws ServerApiException
     */
    public List<IBinder> getSessions(String serviceId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get multimedia sessions for service " + serviceId);
		}

		try {
			ArrayList<IBinder> result = new ArrayList<IBinder>();
			for (IMultimediaSession sessionApi : sipSessions.values()) {
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
     * Sends an instant message to a contact and for a given service. The message may be any
     * type of content. The parameter contact supports the following formats: MSISDN in
     * national or international format, SIP address, SIP-URI or Tel-URI. If the format of the
     * contact is not supported an exception is thrown.
     * 
     * @param serviceId Service ID
     * @param contact Contact
     * @param content Message content
     * @param contentType Content type of the message
	 * @return Returns true if sent successfully else returns false
	 * @throws ServerApiException
     */
    public boolean sendMessage(String serviceId, String contact, String content, String contentType) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Send instant message to " + contact);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Send instant message
			return Core.getInstance().getSipService().sendInstantMessage(contact, serviceId, content, contentType);
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}
    
	/**
	 * Receive an instant message (SIP MESSAGE)
	 *  
     * @param intent Resolved intent
     * @param message Instant message request
   	 */
	public void receiveSipInstantMessage(Intent intent, SipRequest message) {
		// Broadcast intent related to the received invitation
		String contact = SipUtils.getAssertedIdentity(message);
		String number = PhoneUtils.extractNumberFromUri(contact);
		String displayName = SipUtils.getDisplayNameFromUri(message.getFrom());
		intent.putExtra(MultimediaMessageIntent.EXTRA_CONTACT, number);
		intent.putExtra(MultimediaMessageIntent.EXTRA_DISPLAY_NAME, displayName);
		intent.putExtra(MultimediaMessageIntent.EXTRA_CONTENT, message.getContent());
		intent.putExtra(MultimediaMessageIntent.EXTRA_CONTENT_TYPE, message.getContentType());
		
		// Broadcast intent related to the received invitation
		AndroidFactory.getApplicationContext().sendBroadcast(intent);    	
	}

	/**
	 * Returns service version.
	 */
	@Override
	public int getServiceVersion() throws RemoteException {
		if (logger.isActivated()) {
			logger.info("Service Version:" + JoynService.Build.GSMA_VERSION);
		}
		return JoynService.Build.GSMA_VERSION;
	}    
}
