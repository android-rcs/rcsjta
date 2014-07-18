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

import android.content.Intent;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteCallbackList;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ipcall.IIPCall;
import com.gsma.services.rcs.ipcall.IIPCallListener;
import com.gsma.services.rcs.ipcall.IIPCallPlayer;
import com.gsma.services.rcs.ipcall.IIPCallRenderer;
import com.gsma.services.rcs.ipcall.IIPCallService;
import com.gsma.services.rcs.ipcall.IPCall;
import com.gsma.services.rcs.ipcall.IPCallIntent;
import com.gsma.services.rcs.ipcall.IPCallServiceConfiguration;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.content.AudioContent;
import com.orangelabs.rcs.core.content.VideoContent;
import com.orangelabs.rcs.core.ims.service.ipcall.IPCallSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.ipcall.IPCallHistory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.broadcaster.IPCallEventBroadcaster;
import com.orangelabs.rcs.service.broadcaster.JoynServiceRegistrationEventBroadcaster;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * IP call service API implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class IPCallServiceImpl extends IIPCallService.Stub {

	private final IPCallEventBroadcaster mIPCallEventBroadcaster = new IPCallEventBroadcaster();

	private final JoynServiceRegistrationEventBroadcaster mJoynServiceRegistrationEventBroadcaster = new JoynServiceRegistrationEventBroadcaster();

	/**
	 * List of IP call sessions
	 */
	private static Hashtable<String, IIPCall> ipCallSessions = new Hashtable<String, IIPCall>();

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(IPCallServiceImpl.class.getSimpleName());

	/**
	 * Lock used for synchronization
	 */
	private final Object lock = new Object();

	/**
	 * Constructor
	 */
	public IPCallServiceImpl() {
		if (logger.isActivated()) {
			logger.info("IP call service API is loaded");
		}
	}

	/**
	 * Close API
	 */
	public void close() {
		// Clear lists of sessions
		ipCallSessions.clear();

		if (logger.isActivated()) {
			logger.info("IP call service API is closed");
		}
	}

	/**
     * Add an IP Call session in the list
     * 
     * @param session IP call session
     */
	
	protected static void addIPCallSession(IPCallImpl session) {
		if (logger.isActivated()) {
			logger.debug("Add an IP Call session in the list (size=" + ipCallSessions.size() + ")");
		}
		
		ipCallSessions.put(session.getCallId(), session);
	}

    /**
     * Remove an IP Call session from the list
     * 
     * @param sessionId Session ID
     */
	protected static void removeIPCallSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove an IP Call session from the list (size=" + ipCallSessions.size() + ")");
		}
		
		ipCallSessions.remove(sessionId);
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
		if (logger.isActivated()) {
			logger.info("Add a service listener");
		}
		synchronized (lock) {
			mJoynServiceRegistrationEventBroadcaster.addServiceRegistrationListener(listener);
		}
	}

	/**
	 * Unregisters a listener on service registration events
	 *
	 * @param listener Service registration listener
	 */
	public void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove a service listener");
		}
		synchronized (lock) {
			mJoynServiceRegistrationEventBroadcaster.removeServiceRegistrationListener(listener);
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
				mJoynServiceRegistrationEventBroadcaster.broadcastServiceRegistered();
			} else {
				mJoynServiceRegistrationEventBroadcaster.broadcastServiceUnRegistered();
			}
		}
	}

	/**
     * Receive a new IP call invitation
     * 
     * @param session IP call session
     */
	public void receiveIPCallInvitation(IPCallSession session) {
		ContactId contact = session.getRemoteContact();
		if (logger.isActivated()) {
			logger.info("Receive IP call invitation from " + contact);
		}

		// Get audio encoding
		AudioContent audiocontent = session.getAudioContent();
		
		// Get video encoding
		VideoContent videocontent = session.getVideoContent();

		// Update IP call history
		IPCallHistory.getInstance().addCall(contact, session.getSessionID(),
				IPCall.Direction.INCOMING,
				audiocontent, videocontent,
				IPCall.State.INVITED);

		// Add session in the list
		IPCallImpl sessionApi = new IPCallImpl(session, mIPCallEventBroadcaster);
		IPCallServiceImpl.addIPCallSession(sessionApi);

		// Broadcast intent related to the received invitation
		Intent intent = new Intent(IPCallIntent.ACTION_NEW_INVITATION);
    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
		intent.putExtra(IPCallIntent.EXTRA_CONTACT, (Parcelable)contact);
		intent.putExtra(IPCallIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
		intent.putExtra(IPCallIntent.EXTRA_CALL_ID, session.getSessionID());
    	intent.putExtra(IPCallIntent.EXTRA_AUDIO_ENCODING, audiocontent.getEncoding());
    	if (videocontent != null) {
	    	intent.putExtra(IPCallIntent.EXTRA_VIDEO_ENCODING, videocontent.getEncoding());
	        intent.putExtra(IPCallIntent.EXTRA_VIDEO_FORMAT, ""); // TODO
    	}
        AndroidFactory.getApplicationContext().sendBroadcast(intent);

	}

    /**
     * Initiates an IP call with a contact. The parameter contact supports the following
     * formats: MSISDN in national or international format, SIP address, SIP-URI or
     * el-URI. If the format of the contact is not supported an exception is thrown.
     * 
     * @param contact Contact ID
     * @param player IP call player
     * @param renderer IP call renderer
     * @return IP call
	 * @throws ServerApiException 
     */
    public IIPCall initiateCall(ContactId contact, IIPCallPlayer player, IIPCallRenderer renderer) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate an IP call audio session with " + contact);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		// Test if at least the audio media is configured
		if ((player == null) || (renderer == null)) {
			throw new ServerApiException("Missing audio player or renderer");
		}
		
		try {
			// Initiate a new session
			final IPCallSession session = Core.getInstance().getIPCallService().initiateIPCallSession(contact, false, player, renderer);

			// Update IP call history
			IPCallHistory.getInstance().addCall(contact, session.getSessionID(),
					IPCall.Direction.OUTGOING,
					session.getAudioContent(), session.getVideoContent(),
					IPCall.State.INITIATED);

			// Add session in the list
			IPCallImpl sessionApi = new IPCallImpl(session, mIPCallEventBroadcaster);

			// Start the session
	        Thread t = new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	};
	    	t.start();
			
			// Add session in the list
			IPCallServiceImpl.addIPCallSession(sessionApi);
			return sessionApi;
		} catch (Exception e) {
			throw new ServerApiException(e.getMessage());
		}
	} 
	
    /**
     * Initiates an IP call visio with a contact (audio and video). The parameter contact supports the following
     * formats: MSISDN in national or international format, SIP address, SIP-URI or Tel-URI. If the format of
     * the contact is not supported an exception is thrown.
     * 
     * @param contact Contact ID
     * @param player IP call player
     * @param renderer IP call renderer
     * @return IP call
	 * @throws ServerApiException 
     */
    public IIPCall initiateVisioCall(ContactId contact, IIPCallPlayer player, IIPCallRenderer renderer) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate an IP call visio session with " + contact);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		// Test if at least the audio media is configured
		if ((player == null) || (renderer == null)) {
			throw new ServerApiException("Missing audio player or renderer");
		}
		
		try {
			// Initiate a new session
			final IPCallSession session = Core.getInstance().getIPCallService().initiateIPCallSession(contact, true, player, renderer);

			// Update IP call history
			IPCallHistory.getInstance().addCall(contact, session.getSessionID(),
					IPCall.Direction.OUTGOING,
					session.getAudioContent(), session.getVideoContent(),
					IPCall.State.INITIATED);

			// Add session in the list
			IPCallImpl sessionApi = new IPCallImpl(session, mIPCallEventBroadcaster);

			// Start the session
	        Thread t = new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	};
	    	t.start();
	    	
			// Add session in the list
			IPCallServiceImpl.addIPCallSession(sessionApi);
			return sessionApi;
		} catch (Exception e) {
			throw new ServerApiException(e.getMessage());
		}
	}     
    
    /**
     * Returns a current IP call from its unique ID
     * 
     * @param callId Call ID
     * @return IP call or null if not found
     * @throws ServerApiException
     */
    public IIPCall getIPCall(String callId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get IP call session " + callId);
		}

		return ipCallSessions.get(callId);
	}
	
    /**
     * Returns the list of IP calls in progress
     * 
     * @return List of IP calls
     * @throws ServerApiException
     */
    public List<IBinder> getIPCalls() throws ServerApiException {
    	if (logger.isActivated()) {
			logger.info("Get IP call sessions");
		}

		try {
			ArrayList<IBinder> result = new ArrayList<IBinder>(ipCallSessions.size());
			for (Enumeration<IIPCall> e = ipCallSessions.elements() ; e.hasMoreElements() ;) {
				IIPCall sessionApi = e.nextElement() ;
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
     * Returns the configuration of IP call service
     * 
     * @return Configuration
     */
    public IPCallServiceConfiguration getConfiguration() {
    	return new IPCallServiceConfiguration(
    			RcsSettings.getInstance().isIPVoiceCallBreakoutAA() || RcsSettings.getInstance().isIPVoiceCallBreakoutAA());    	
	}

	/**
	 * Adds an event listener on IP call events
	 * 
	 * @param listener Listener
	 */
	public void addEventListener(IIPCallListener listener) {
		if (logger.isActivated()) {
			logger.info("Add an IP call event listener");
		}
		synchronized (lock) {
			mIPCallEventBroadcaster.addEventListener(listener);
		}
	}

	/**
	 * Removes an event listener from IP call events
	 * 
	 * @param listener Listener
	 */
	public void removeEventListener(IIPCallListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove an IP call event listener");
		}
		synchronized (lock) {
			mIPCallEventBroadcaster.removeEventListener(listener);
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
