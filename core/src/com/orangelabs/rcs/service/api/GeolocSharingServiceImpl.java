/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/
package com.orangelabs.rcs.service.api;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import android.os.IBinder;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.gsh.GeolocSharing;
import com.gsma.services.rcs.gsh.IGeolocSharing;
import com.gsma.services.rcs.gsh.IGeolocSharingListener;
import com.gsma.services.rcs.gsh.IGeolocSharingService;
import com.gsma.services.rcs.gsh.GeolocSharing.ReasonCode;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.content.GeolocContent;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.service.SessionIdGenerator;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.GeolocTransferSession;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.service.broadcaster.GeolocSharingEventBroadcaster;
import com.orangelabs.rcs.service.broadcaster.RcsServiceRegistrationEventBroadcaster;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Geoloc sharing service implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class GeolocSharingServiceImpl extends IGeolocSharingService.Stub {

	/**
	 * List of geoloc sharing sessions
	 */
	private static Hashtable<String, IGeolocSharing> gshSessions = new Hashtable<String, IGeolocSharing>();  

	private final GeolocSharingEventBroadcaster mGeolocSharingEventBroadcaster = new GeolocSharingEventBroadcaster();

	private final RcsServiceRegistrationEventBroadcaster mRcsServiceRegistrationEventBroadcaster = new RcsServiceRegistrationEventBroadcaster();

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(GeolocSharingServiceImpl.class.getSimpleName());

	/**
	 * Lock used for synchronization
	 */
	private final Object lock = new Object();

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
	private static void addGeolocSharingSession(GeolocSharingImpl session) {
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
	/* package private */ static void removeGeolocSharingSession(String sessionId) {
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
	public void addEventListener(IRcsServiceRegistrationListener listener) {
		if (logger.isActivated()) {
			logger.info("Add a service listener");
		}
		synchronized (lock) {
			mRcsServiceRegistrationEventBroadcaster.addEventListener(listener);
		}
	}

	/**
	 * Unregisters a listener on service registration events
	 *
	 * @param listener Service registration listener
	 */
	public void removeEventListener(IRcsServiceRegistrationListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove a service listener");
		}
		synchronized (lock) {
			mRcsServiceRegistrationEventBroadcaster.removeEventListener(listener);
		}
	}

	/**
	 * Receive registration event
	 *
	 * @param state Registration state
	 */
	public void notifyRegistrationEvent(boolean state) {
		// Notify listeners
		synchronized (lock) {
			if (state) {
				mRcsServiceRegistrationEventBroadcaster.broadcastServiceRegistered();
			} else {
				mRcsServiceRegistrationEventBroadcaster.broadcastServiceUnRegistered();
			}
		}
	}

    /**
     * Receive a new geoloc sharing invitation
     * 
     * @param session Geoloc sharing session
     */
    public void receiveGeolocSharingInvitation(GeolocTransferSession session) {
		if (logger.isActivated()) {
			logger.info("Receive geoloc sharing invitation from " + session.getRemoteContact()+" displayName="+session.getRemoteDisplayName());
		}
		// TODO : Add entry into GeolocSharing provider (to be implemented as part of CR025)
		
		// Update displayName of remote contact
		ContactsManager.getInstance().setContactDisplayName(session.getRemoteContact(), session.getRemoteDisplayName());

		// Add session in the list
		GeolocSharingImpl sessionApi = new GeolocSharingImpl(session, mGeolocSharingEventBroadcaster);
		GeolocSharingServiceImpl.addGeolocSharingSession(sessionApi);
    }
    
    /**
     * Shares a geolocation with a contact. An exception if thrown if there is no ongoing
     * CS call. The parameter contact supports the following formats: MSISDN in national
     * or international format, SIP address, SIP-URI or Tel-URI. If the format of the
     * contact is not supported an exception is thrown.
     * 
     * @param contact Contact
     * @param geoloc Geolocation info
     * @return Geoloc sharing
     * @throws ServerApiException
     */
    public IGeolocSharing shareGeoloc(ContactId contact, Geoloc geoloc) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate a geoloc sharing session with " + contact);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Create a geoloc content
			String msgId = IdGenerator.generateMessageID();
			GeolocPush geolocPush = new GeolocPush(geoloc.getLabel(),
					geoloc.getLatitude(), geoloc.getLongitude(),
					geoloc.getExpiration(), geoloc.getAccuracy());
			String geolocDoc = ChatUtils.buildGeolocDocument(geolocPush, ImsModule.IMS_USER_PROFILE.getPublicUri(), msgId);
			MmContent content = new GeolocContent("geoloc.xml", geolocDoc.getBytes().length, geolocDoc.getBytes());

			// Initiate a sharing session
			final GeolocTransferSession session = Core.getInstance().getRichcallService().initiateGeolocSharingSession(contact, content, geolocPush);
			mGeolocSharingEventBroadcaster.broadcastGeolocSharingStateChanged(contact,
					session.getSessionID(), GeolocSharing.State.INITIATED, ReasonCode.UNSPECIFIED);

			// Add session listener
			GeolocSharingImpl sessionApi = new GeolocSharingImpl(session, mGeolocSharingEventBroadcaster);

			// Start the session
	        new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	}.start();
	    	
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
	 * Adds a listener on geoloc sharing events
	 * 
	 * @param listener Listener
	 */
	public void addEventListener2(IGeolocSharingListener listener) {
		if (logger.isActivated()) {
			logger.info("Add a Geoloc sharing event listener");
		}
		synchronized (lock) {
			mGeolocSharingEventBroadcaster.addEventListener(listener);
		}
	}

	/**
	 * Removes a listener on geoloc sharing events
	 *
	 * @param listener Listener
	 */
	public void removeEventListener2(IGeolocSharingListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove a Geoloc sharing event listener");
		}
		synchronized (lock) {
			mGeolocSharingEventBroadcaster.removeEventListener(listener);
		}
	}

	/**
	 * Returns service version
	 * 
	 * @return Version
	 * @see RcsService.Build.VERSION_CODES
	 * @throws ServerApiException
	 */
	public int getServiceVersion() throws ServerApiException {
		return RcsService.Build.API_VERSION;
	}

	public void addAndbroadcastGeolocSharingInvitationRejected(ContactId contact, GeolocContent content,
			int reasonCode) {
		String sharingId = SessionIdGenerator.getNewId();
		/* TODO: Persist in geoloc content provider */
		mGeolocSharingEventBroadcaster.broadcastInvitation(sharingId);
	}
}
