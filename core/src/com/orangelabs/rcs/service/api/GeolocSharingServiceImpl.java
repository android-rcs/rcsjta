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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.gsh.GeolocSharingIntent;
import com.gsma.services.rcs.gsh.IGeolocSharing;
import com.gsma.services.rcs.gsh.IGeolocSharingListener;
import com.gsma.services.rcs.gsh.IGeolocSharingService;
import com.gsma.services.rcs.gsh.INewGeolocSharingListener;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.content.GeolocContent;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.GeolocTransferSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Geoloc sharing service implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class GeolocSharingServiceImpl extends IGeolocSharingService.Stub {
	/**
	 * List of service event listeners
	 */
	private RemoteCallbackList<IJoynServiceRegistrationListener> serviceListeners = new RemoteCallbackList<IJoynServiceRegistrationListener>();

	/**
	 * List of geoloc sharing sessions
	 */
	private static Hashtable<String, IGeolocSharing> gshSessions = new Hashtable<String, IGeolocSharing>();  

	/**
	 * List of geoloc sharing invitation listeners
	 */
	private RemoteCallbackList<INewGeolocSharingListener> listeners = new RemoteCallbackList<INewGeolocSharingListener>();

	/**
	 * The logger
	 */
	private static Logger logger = Logger.getLogger(GeolocSharingServiceImpl.class.getName());

	/**
	 * Lock used for synchronization
	 */
	private Object lock = new Object();

	/**
	 * Constructor
	 */
	public GeolocSharingServiceImpl() {
		if (logger.isActivated()) {
			logger.info("Geoloc sharing service API is loaded");
		}
	}

	/**
	 * Close API
	 */
	public void close() {
		// Clear list of sessions
		gshSessions.clear();
		
		if (logger.isActivated()) {
			logger.info("Geoloc sharing service API is closed");
		}
	}

	/**
	 * Add an geoloc sharing session in the list
	 * 
	 * @param session Geoloc sharing session
	 */
	protected static void addGeolocSharingSession(GeolocSharingImpl session) {
		if (logger.isActivated()) {
			logger.debug("Add a geoloc sharing session in the list (size=" + gshSessions.size() + ")");
		}
		
		gshSessions.put(session.getSharingId(), session);
	}

	/**
	 * Remove an geoloc sharing session from the list
	 * 
	 * @param sessionId Session ID
	 */
	protected static void removeGeolocSharingSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove a geoloc sharing session from the list (size=" + gshSessions.size() + ")");
		}
		
		gshSessions.remove(sessionId);
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
     * Receive a new geoloc sharing invitation
     * 
     * @param session Geoloc sharing session
     */
    public void receiveGeolocSharingInvitation(GeolocTransferSession session) {
		if (logger.isActivated()) {
			logger.info("Receive geoloc sharing invitation from " + session.getRemoteContact());
		}

        // Extract number from contact
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());

		// Add session in the list
		GeolocSharingImpl sessionApi = new GeolocSharingImpl(session);
		GeolocSharingServiceImpl.addGeolocSharingSession(sessionApi);
    	
		// Broadcast intent related to the received invitation
    	Intent intent = new Intent(GeolocSharingIntent.ACTION_NEW_INVITATION);
    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
    	intent.putExtra(GeolocSharingIntent.EXTRA_CONTACT, number);
    	intent.putExtra(GeolocSharingIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
    	intent.putExtra(GeolocSharingIntent.EXTRA_SHARING_ID, session.getSessionID());
    	AndroidFactory.getApplicationContext().sendBroadcast(intent);
    	
    	// Notify geoloc sharing invitation listeners
    	synchronized(lock) {
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onNewGeolocSharing(session.getSessionID());
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	    }
    }
    
    /**
     * Shares a geolocation with a contact. An exception if thrown if there is no ongoing
     * CS call. The parameter contact supports the following formats: MSISDN in national
     * or international format, SIP address, SIP-URI or Tel-URI. If the format of the
     * contact is not supported an exception is thrown.
     * 
     * @param contact Contact
     * @param geoloc Geolocation info
     * @param listener Geoloc sharing event listener
     * @return Geoloc sharing
     * @throws ServerApiException
     */
    public IGeolocSharing shareGeoloc(String contact, Geoloc geoloc, IGeolocSharingListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate a geoloc sharing session with " + contact);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Create a geoloc content
			String msgId = ChatUtils.generateMessageId();
			GeolocPush geolocPush = new GeolocPush(geoloc.getLabel(),
					geoloc.getLatitude(), geoloc.getLongitude(),
					geoloc.getExpiration(), geoloc.getAccuracy());
			String geolocDoc = ChatUtils.buildGeolocDocument(geolocPush, ImsModule.IMS_USER_PROFILE.getPublicUri(), msgId);
			MmContent content = new GeolocContent("geoloc.xml", geolocDoc.getBytes().length, geolocDoc.getBytes());

			// Initiate a sharing session
			final GeolocTransferSession session = Core.getInstance().getRichcallService().initiateGeolocSharingSession(contact, content, geolocPush);

			// Add session listener
			GeolocSharingImpl sessionApi = new GeolocSharingImpl(session);
			sessionApi.addEventListener(listener);

			// Start the session
	        Thread t = new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	};
	    	t.start();
	    	
			// Add session in the list
			addGeolocSharingSession(sessionApi);
			return sessionApi;			
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

    /**
     * Returns the list of geoloc sharings in progress
     * 
     * @return List of geoloc sharings
     * @throws ServerApiException
     */
    public List<IBinder> getGeolocSharings() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get geoloc sharing sessions");
		}

		try {
			ArrayList<IBinder> result = new ArrayList<IBinder>(gshSessions.size());
			for (Enumeration<IGeolocSharing> e = gshSessions.elements() ; e.hasMoreElements() ;) {
				IGeolocSharing sessionApi = e.nextElement() ;
				result.add(sessionApi.asBinder());
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
     * Returns a current geoloc sharing from its unique ID
     * 
     * @return Geoloc sharing
     * @throws ServerApiException
     */
    public IGeolocSharing getGeolocSharing(String sharingId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get geoloc sharing session " + sharingId);
		}

		return gshSessions.get(sharingId);
    }    
    
    /**
	 * Registers a geoloc sharing invitation listener
	 * 
	 * @param listener New geoloc sharing listener
	 * @throws ServerApiException
	 */
	public void addNewGeolocSharingListener(INewGeolocSharingListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Add a geoloc sharing invitation listener");
		}
		
		listeners.register(listener);
	}

	/**
	 * Unregisters a geoloc sharing invitation listener
	 * 
	 * @param listener New geoloc sharing listener
	 * @throws ServerApiException
	 */
	public void removeNewGeolocSharingListener(INewGeolocSharingListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Remove a geoloc sharing invitation listener");
		}
		
		listeners.unregister(listener);
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
