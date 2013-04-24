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

package com.orangelabs.rcs.service.api.server.sip;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import android.content.Intent;
import android.os.IBinder;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.sip.GenericSipSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.service.api.client.sip.ISipApi;
import com.orangelabs.rcs.service.api.client.sip.ISipSession;
import com.orangelabs.rcs.service.api.server.ServerApiException;
import com.orangelabs.rcs.service.api.server.ServerApiUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * SIP API service
 * 
 * @author jexa7410
 */
public class SipApiService extends ISipApi.Stub {
	/**
	 * List of sessions
	 */
	private static Hashtable<String, ISipSession> sipSessions = new Hashtable<String, ISipSession>();  

	/**
	 * The logger
	 */
	private static Logger logger = Logger.getLogger(SipApiService.class.getName());

	/**
	 * Constructor
	 */
	public SipApiService() {
		if (logger.isActivated()) {
			logger.info("SIP API is loaded");
		}
	}

	/**
	 * Close API
	 */
	public void close() {
		// Clear list of sessions
		sipSessions.clear();
	}
	
	/**
	 * Add a SIP session in the list
	 * 
	 * @param session SIP session
	 */
	protected static void addSipSession(SipSession session) {
		if (logger.isActivated()) {
			logger.debug("Add a SIP session in the list (size=" + sipSessions.size() + ")");
		}
		sipSessions.put(session.getSessionID(), session);
	}

	/**
	 * Remove a SIP session from the list
	 * 
	 * @param sessionId Session ID
	 */
	protected static void removeSipSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove a SIP session from the list (size=" + sipSessions.size() + ")");
		}
		sipSessions.remove(sessionId);
	}	
	
	/**
	 * Receive a new SIP session invitation
	 * 
	 * @param intent Resolved intent
     * @param session SIP session
	 */
	public void receiveSipSessionInvitation(Intent intent, GenericSipSession session) {
		// Add session in the list
		SipSession sessionApi = new SipSession(session);
		SipApiService.addSipSession(sessionApi);
		
		// Broadcast intent related to the received invitation
		AndroidFactory.getApplicationContext().sendBroadcast(intent);    	
	}
	
	/**
	 * Initiate a SIP session
	 * 
	 * @param contact Contact
	 * @param featureTag Feature tag of the service
	 * @param sdpOffer SDP offer
	 * @throws ServerApiException
	 */
	public ISipSession initiateSession(String contact, String featureTag, String sdpOffer) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate a SIP session with " + contact);
		}

		// Check permission
		ServerApiUtils.testPermissionForExtensions();

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Initiate a new session
			GenericSipSession session = Core.getInstance().getSipService().initiateSession(contact,
					featureTag, sdpOffer);
			
			// Add session in the list
			SipSession sessionApi = new SipSession(session);
			SipApiService.addSipSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

	/**
	 * Get current SIP session from its session ID
	 *
	 * @param id Session ID
	 * @return Session
	 * @throws ServerApiException
	 */
	public ISipSession getSession(String id) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get SIP session " + id);
		}

		// Check permission
		ServerApiUtils.testPermissionForExtensions();

		// Test core availability
		ServerApiUtils.testCore();
		
		// Return a session instance
		return sipSessions.get(id);
	}
	
	/**
	 * Get list of current SIP sessions with a contact
	 * 
	 * @param contact Contact
	 * @return List of sessions
	 * @throws ServerApiException
	 */
	public List<IBinder> getSessionsWith(String contact) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get SIP sessions with " + contact);
		}

		// Check permission
		ServerApiUtils.testPermissionForExtensions();

		// Test core availability
		ServerApiUtils.testCore();
		
		try {
			Vector<GenericSipSession> list = Core.getInstance().getSipService().getSipSessionsWith(contact);
			ArrayList<IBinder> result = new ArrayList<IBinder>(list.size());
			for(int i=0; i < list.size(); i++) {
				GenericSipSession session = list.elementAt(i);
				ISipSession sessionApi = sipSessions.get(session.getSessionID());
				if (sessionApi != null) {
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
	 * Get list of current SIP sessions
	 * 
	 * @return List of sessions
	 * @throws ServerApiException
	 */
	public List<IBinder> getSessions() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get SIP sessions");
		}

		// Check permission
		ServerApiUtils.testPermissionForExtensions();

		// Test core availability
		ServerApiUtils.testCore();
		
		try {
			Vector<GenericSipSession> list = Core.getInstance().getSipService().getSipSessions();
			ArrayList<IBinder> result = new ArrayList<IBinder>(list.size());
			for(int i=0; i < list.size(); i++) {
				GenericSipSession session = list.elementAt(i);
				SipDialogPath dialog = session.getDialogPath();
				if ((dialog != null) && (dialog.isSigEstablished())) {
					// Returns only sessions which are established
					ISipSession sessionApi = sipSessions.get(session.getSessionID());
					if (sessionApi != null) {
						result.add(sessionApi.asBinder());
					}
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
	 * Send an instant message (SIP MESSAGE)
	 * 
     * @param contact Contact
	 * @param featureTag Feature tag of the service
     * @param content Content
     * @param contentType Content type
	 * @return True if successful else returns false
	 * @throws ServerApiException
	 */
	public boolean sendSipInstantMessage(String contact, String featureTag, String content, String contentType) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Send an instant message to " + contact);
		}

		// Check permission
		ServerApiUtils.testPermissionForExtensions();

		// Test core availability
		ServerApiUtils.testCore();
		
		try {
			return Core.getInstance().getSipService().sendInstantMessage(contact,
					featureTag, content, contentType);
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
	 */
	public void receiveSipInstantMessage(Intent intent) {
		// Broadcast intent related to the received message
		AndroidFactory.getApplicationContext().sendBroadcast(intent);
	}
}
