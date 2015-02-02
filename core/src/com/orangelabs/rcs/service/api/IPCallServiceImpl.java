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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.IBinder;

import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.Build.VERSION_CODES;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ipcall.IIPCall;
import com.gsma.services.rcs.ipcall.IIPCallListener;
import com.gsma.services.rcs.ipcall.IIPCallPlayer;
import com.gsma.services.rcs.ipcall.IIPCallRenderer;
import com.gsma.services.rcs.ipcall.IIPCallService;
import com.gsma.services.rcs.ipcall.IIPCallServiceConfiguration;
import com.gsma.services.rcs.ipcall.IPCall;
import com.gsma.services.rcs.ipcall.IPCall.ReasonCode;
import com.orangelabs.rcs.core.content.AudioContent;
import com.orangelabs.rcs.core.content.VideoContent;
import com.orangelabs.rcs.core.ims.service.SessionIdGenerator;
import com.orangelabs.rcs.core.ims.service.ipcall.IPCallPersistedStorageAccessor;
import com.orangelabs.rcs.core.ims.service.ipcall.IPCallService;
import com.orangelabs.rcs.core.ims.service.ipcall.IPCallSession;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.ipcall.IPCallHistory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.broadcaster.IPCallEventBroadcaster;
import com.orangelabs.rcs.service.broadcaster.RcsServiceRegistrationEventBroadcaster;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * IP call service API implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class IPCallServiceImpl extends IIPCallService.Stub {

	private final IPCallEventBroadcaster mBroadcaster = new IPCallEventBroadcaster();

	private final RcsServiceRegistrationEventBroadcaster mRcsServiceRegistrationEventBroadcaster = new RcsServiceRegistrationEventBroadcaster();

	private final IPCallService mIPCallService;

	private final IPCallHistory mIPCallLog;

	private final RcsSettings mRcsSettings;

	private final ContactsManager mContactsManager;

	private final Map<String, IIPCall> mIPCallCache = new HashMap<String, IIPCall>();

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
	 * 
	 * @param ipCallService IPCallService
	 * @param ipCallLog IPCallHistory
	 * @param contactsManager ContactsManager
	 * @param rcsSettings RcsSettings
	 */
	public IPCallServiceImpl(IPCallService ipCallService, IPCallHistory ipCallLog,
			ContactsManager contactsManager, RcsSettings rcsSettings) {
		if (logger.isActivated()) {
			logger.info("IP call service API is loaded");
		}
		mIPCallService = ipCallService;
		mIPCallLog = ipCallLog;
		mRcsSettings = rcsSettings;
		mContactsManager = contactsManager;
	}

		/**
	 * Close API
	 */
	public void close() {
		// Clear lists of sessions
		mIPCallCache.clear();

		if (logger.isActivated()) {
			logger.info("IP call service API is closed");
		}
	}

	/**
	 * Add an IP Call session in the list
	 * 
	 * @param ipCall IP call session
	 */

	protected void addIPCall(IPCallImpl ipCall) {
		if (logger.isActivated()) {
			logger.debug("Add an IP Call session in the list (size=" + mIPCallCache.size() + ")");
		}

		mIPCallCache.put(ipCall.getCallId(), ipCall);
	}

	/**
	 * Remove an IP Call session from the list
	 * 
	 * @param sessionId Session ID
	 */
	protected void removeIPCall(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove an IP Call session from the list (size=" + mIPCallCache.size()
					+ ")");
		}

		mIPCallCache.remove(sessionId);
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
     * Receive a new IP call invitation
     * 
     * @param session IP call session
     */
	public void receiveIPCallInvitation(IPCallSession session) {
		ContactId contact = session.getRemoteContact();
		if (logger.isActivated()) {
			logger.info("Receive IP call invitation from " + contact);
		}
		
		// Update displayName of remote contact
		mContactsManager.setContactDisplayName(contact, session.getRemoteDisplayName());

		String callId = session.getSessionID();
		IPCallPersistedStorageAccessor storageAccessor = new IPCallPersistedStorageAccessor(callId,
				mIPCallLog);
		IPCallImpl ipCall = new IPCallImpl(callId, mBroadcaster, mIPCallService, storageAccessor,
				this);
		addIPCall(ipCall);
		session.addListener(ipCall);
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
			final IPCallSession session = mIPCallService.initiateIPCallSession(contact, false, player, renderer);

			String callId = session.getSessionID();
			mIPCallLog.addCall(callId, contact,
					Direction.OUTGOING, session.getAudioContent(),
					session.getVideoContent(), IPCall.State.INITIATED, ReasonCode.UNSPECIFIED);
			mBroadcaster.broadcastIPCallStateChanged(contact, callId,
					IPCall.State.INITIATED, ReasonCode.UNSPECIFIED);

			IPCallPersistedStorageAccessor storageAccessor = new IPCallPersistedStorageAccessor(
					callId, contact, Direction.OUTGOING, mIPCallLog);
			IPCallImpl ipCall = new IPCallImpl(callId, mBroadcaster, mIPCallService,
					storageAccessor, this);

			addIPCall(ipCall);
			session.addListener(ipCall);

			// Start the session
	        Thread t = new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	};
	    	t.start();
			return ipCall;

		} catch (Exception e) {
			mIPCallLog.addCall(SessionIdGenerator.getNewId(), contact,
					Direction.OUTGOING, null, null, IPCall.State.FAILED,
					ReasonCode.FAILED_INITIATION);
			;
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
			final IPCallSession session = mIPCallService.initiateIPCallSession(contact, true, player, renderer);

			String callId = session.getSessionID();
			mIPCallLog.addCall(callId, contact,
					Direction.OUTGOING, session.getAudioContent(),
					session.getVideoContent(), IPCall.State.INITIATED, ReasonCode.UNSPECIFIED);
			mBroadcaster.broadcastIPCallStateChanged(contact, callId,
					IPCall.State.INITIATED, ReasonCode.UNSPECIFIED);

			IPCallPersistedStorageAccessor storageAccessor = new IPCallPersistedStorageAccessor(
					callId, contact, Direction.OUTGOING, mIPCallLog);
			IPCallImpl ipCall = new IPCallImpl(callId, mBroadcaster, mIPCallService,
					storageAccessor, this);

			addIPCall(ipCall);
			session.addListener(ipCall);

			// Start the session
	        Thread t = new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	};
	    	t.start();
			return ipCall;

		} catch (Exception e) {
			mIPCallLog.addCall(SessionIdGenerator.getNewId(), contact,
					Direction.OUTGOING, null, null, IPCall.State.FAILED,
					ReasonCode.FAILED_INITIATION);
			throw new ServerApiException(e.getMessage());
		}
	}     
    
    /**
     * Returns a current IP call from its unique ID
     * 
     * @param callId Call ID
     * @return IP call
     * @throws ServerApiException
     */
	public IIPCall getIPCall(String callId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get IP call " + callId);
		}

		IIPCall ipCall = mIPCallCache.get(callId);
		if (ipCall != null) {
			return ipCall;
		}
		IPCallPersistedStorageAccessor storageAccessor = new IPCallPersistedStorageAccessor(callId,
				mIPCallLog);
		return new IPCallImpl(callId, mBroadcaster, mIPCallService, storageAccessor, this);
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
			List<IBinder> ipCalls = new ArrayList<IBinder>(mIPCallCache.size());
			for (IIPCall ipCall : mIPCallCache.values()) {
				ipCalls.add(ipCall.asBinder());
			}
			return ipCalls;

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
	public IIPCallServiceConfiguration getConfiguration() {
		return new IPCallServiceConfigurationImpl(mRcsSettings);
	}

	/**
	 * Add and broadcast video sharing invitation rejections
	 *
	 * @param contact Contact ID
	 * @param audioContent Audio content
	 * @param videoContent Video content
	 * @param reasonCode Reason code
	 */
	public void addAndBroadcastIPCallInvitationRejected(ContactId contact, AudioContent audioContent,
			VideoContent videoContent, int reasonCode) {
		String sessionId = SessionIdGenerator.getNewId();
		mIPCallLog.addCall(sessionId, contact, Direction.INCOMING,
				audioContent, videoContent, IPCall.State.REJECTED, reasonCode);
		mBroadcaster.broadcastIPCallInvitation(sessionId);
	}

    /**
	 * Adds an event listener on IP call events
	 * 
	 * @param listener Listener
	 */
	public void addEventListener2(IIPCallListener listener) {
		if (logger.isActivated()) {
			logger.info("Add an IP call event listener");
		}
		synchronized (lock) {
			mBroadcaster.addEventListener(listener);
		}
	}

	/**
	 * Removes an event listener from IP call events
	 * 
	 * @param listener Listener
	 */
	public void removeEventListener2(IIPCallListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove an IP call event listener");
		}
		synchronized (lock) {
			mBroadcaster.removeEventListener(listener);
		}
	}

	/**
	 * Returns service version
	 * 
	 * @return Version
	 * @see VERSION_CODES
	 * @throws ServerApiException
	 */
	public int getServiceVersion() throws ServerApiException {
		return RcsService.Build.API_VERSION;
	}
	
	/**
	 * Returns the common service configuration
	 * 
	 * @return the common service configuration
	 */
	public ICommonServiceConfiguration getCommonConfiguration() {
		return new CommonServiceConfigurationImpl();
	}
}
