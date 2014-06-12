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
import com.gsma.services.rcs.chat.ChatServiceConfiguration;
import com.gsma.services.rcs.extension.IMultimediaMessagingSession;
import com.gsma.services.rcs.extension.IMultimediaMessagingSessionListener;
import com.gsma.services.rcs.extension.IMultimediaSessionService;
import com.gsma.services.rcs.extension.IMultimediaStreamingSession;
import com.gsma.services.rcs.extension.IMultimediaStreamingSessionListener;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionIntent;
import com.gsma.services.rcs.extension.MultimediaSessionServiceConfiguration;
import com.gsma.services.rcs.extension.MultimediaStreamingSessionIntent;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.ims.network.sip.FeatureTags;
import com.orangelabs.rcs.core.ims.service.sip.messaging.GenericSipMsrpSession;
import com.orangelabs.rcs.core.ims.service.sip.streaming.GenericSipRtpSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
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
	 * List of messaging sessions
	 */
	private static Hashtable<String, IMultimediaMessagingSession> messagingSessions = new Hashtable<String, IMultimediaMessagingSession>();  

	/**
	 * List of streaming sessions
	 */
	private static Hashtable<String, IMultimediaStreamingSession> streamingSessions = new Hashtable<String, IMultimediaStreamingSession>();  

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
	 * Add a messaging session in the list
	 * 
	 * @param session SIP session
	 */
	protected static void addMessagingSipSession(MultimediaMessagingSessionImpl session) {
		if (logger.isActivated()) {
			logger.debug("Add a messaging session in the list (size=" + messagingSessions.size() + ")");
		}
		
		messagingSessions.put(session.getSessionId(), session);
	}

	/**
	 * Remove a messaging session from the list
	 * 
	 * @param sessionId Session ID
	 */
	protected static void removeMessagingSipSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove a messaging session from the list (size=" + messagingSessions.size() + ")");
		}
		
		messagingSessions.remove(sessionId);
	}	
	
	/**
	 * Add a streaming session in the list
	 * 
	 * @param session SIP session
	 */
	protected static void addStreamingSipSession(MultimediaStreamingSessionImpl session) {
		if (logger.isActivated()) {
			logger.debug("Add a streaming session in the list (size=" + messagingSessions.size() + ")");
		}
		
		streamingSessions.put(session.getSessionId(), session);
	}

	/**
	 * Remove a streaming session from the list
	 * 
	 * @param sessionId Session ID
	 */
	protected static void removeStreamingSipSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove a streaming session from the list (size=" + messagingSessions.size() + ")");
		}
		
		streamingSessions.remove(sessionId);
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
	 * Receive a new SIP session invitation with MRSP media
	 * 
     * @param intent Resolved intent
     * @param session SIP session
	 */
	public void receiveSipMsrpSessionInvitation(Intent intent, GenericSipMsrpSession session) {
		// Add session in the list
		MultimediaMessagingSessionImpl sessionApi = new MultimediaMessagingSessionImpl(session);
		MultimediaSessionServiceImpl.addMessagingSipSession(sessionApi);
		
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
	 * Receive a new SIP session invitation with RTP media
	 * 
     * @param intent Resolved intent
     * @param session SIP session
	 */
	public void receiveSipRtpSessionInvitation(Intent intent, GenericSipRtpSession session) {
		// Add session in the list
		MultimediaStreamingSessionImpl sessionApi = new MultimediaStreamingSessionImpl(session);
		MultimediaSessionServiceImpl.addStreamingSipSession(sessionApi);
		
		// Broadcast intent related to the received invitation
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());
    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
		intent.putExtra(MultimediaStreamingSessionIntent.EXTRA_CONTACT, number);
		intent.putExtra(MultimediaStreamingSessionIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
		intent.putExtra(MultimediaStreamingSessionIntent.EXTRA_SESSION_ID, session.getSessionID());
		
		// Broadcast intent related to the received invitation
		AndroidFactory.getApplicationContext().sendBroadcast(intent);    	
	}

    /**
     * Returns the configuration of the multimedia session service
     * 
     * @return Configuration
     */
    public MultimediaSessionServiceConfiguration getConfiguration() {
    	return new MultimediaSessionServiceConfiguration(
    			RcsSettings.getInstance().getMaxMsrpLengthForExtensions());
	}  
    
	/**
     * Initiates a new session for real time messaging with a remote contact and for a given
     * service extension. The messages are exchanged in real time during the session and may
     * be from any type. The parameter contact supports the following formats: MSISDN in
     * national or international format, SIP address, SIP-URI or Tel-URI. If the format of the
     * contact is not supported an exception is thrown.
     * 
     * @param serviceId Service ID
     * @param contact Contact
     * @param listener Multimedia messaging session event listener
     * @return Multimedia messaging session
	 * @throws ServerApiException
     */
    public IMultimediaMessagingSession initiateMessagingSession(String serviceId, String contact, IMultimediaMessagingSessionListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate a multimedia messaging session with " + contact);
		}

		// Test security extension
		ServerApiUtils.testSecurityExtension(serviceId);

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Initiate a new session
			String featureTag = FeatureTags.FEATURE_RCSE + "=\"" + FeatureTags.FEATURE_RCSE_EXTENSION + "." + serviceId + "\"";
			final GenericSipMsrpSession session = Core.getInstance().getSipService().initiateMsrpSession(contact, featureTag);
			
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
			MultimediaSessionServiceImpl.addMessagingSipSession(sessionApi);
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
			logger.info("Get multimedia messaging session " + sessionId);
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
			logger.info("Get multimedia messaging sessions for service " + serviceId);
		}

		try {
			ArrayList<IBinder> result = new ArrayList<IBinder>();
			for (IMultimediaMessagingSession sessionApi : messagingSessions.values()) {
				// Filter on the service ID
				if (sessionApi.getServiceId().contains(serviceId)) {
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
     * Initiates a new session for real time streaming with a remote contact and for a given
     * service extension. The payload are exchanged in real time during the session and may be
     * from any type. The parameter contact supports the following formats: MSISDN in national or
     * international format, SIP address, SIP-URI or Tel-URI. If the format of the contact is
     * not supported an exception is thrown.
     * 
     * @param serviceId Service ID
     * @param contact Contact
     * @param listener Multimedia streaming session event listener
     * @return Multimedia streaming session
	 * @throws ServerApiException
     */
    public IMultimediaStreamingSession initiateStreamingSession(String serviceId, String contact, IMultimediaStreamingSessionListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate a multimedia streaming session with " + contact);
		}

		// Test security extension
		ServerApiUtils.testSecurityExtension(serviceId);

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Initiate a new session
			String featureTag = FeatureTags.FEATURE_RCSE + "=\"" + FeatureTags.FEATURE_RCSE_EXTENSION + "." + serviceId + "\"";
			final GenericSipRtpSession session = Core.getInstance().getSipService().initiateRtpSession(contact, featureTag);
			
			// Add session listener
			MultimediaStreamingSessionImpl sessionApi = new MultimediaStreamingSessionImpl(session);
			sessionApi.addEventListener(listener);

			// Start the session
	        Thread t = new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	};
	    	t.start();
			
			// Add session in the list
			MultimediaSessionServiceImpl.addStreamingSipSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}

	}

    /**
     * Returns a current streaming session from its unique session ID
     * 
     * @return Multimedia streaming session or null if not found
     * @throws ServerApiException
     */
    public IMultimediaStreamingSession getStreamingSession(String sessionId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get multimedia streaming session " + sessionId);
		}

		return streamingSessions.get(sessionId);
	}

    /**
     * Returns the list of streaming sessions associated to a given service ID
     * 
     * @param serviceId Service ID
     * @return List of sessions
     * @throws ServerApiException
     */
    public List<IBinder> getStreamingSessions(String serviceId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get multimedia streaming sessions for service " + serviceId);
		}

		try {
			ArrayList<IBinder> result = new ArrayList<IBinder>();
			for (IMultimediaStreamingSession sessionApi : streamingSessions.values()) {
				// Filter on the service ID
				if (sessionApi.getServiceId().contains(serviceId)) {
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
