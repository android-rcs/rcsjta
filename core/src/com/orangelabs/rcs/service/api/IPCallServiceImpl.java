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

import org.gsma.joyn.IJoynServiceRegistrationListener;
import org.gsma.joyn.ipcall.IIPCall;
import org.gsma.joyn.ipcall.IIPCallListener;
import org.gsma.joyn.ipcall.IIPCallPlayer;
import org.gsma.joyn.ipcall.IIPCallRenderer;
import org.gsma.joyn.ipcall.IIPCallService;
import org.gsma.joyn.ipcall.INewIPCallListener;
import org.gsma.joyn.ipcall.IPCall;
import org.gsma.joyn.ipcall.IPCallIntent;
import org.gsma.joyn.ipcall.IPCallServiceConfiguration;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.content.AudioContent;
import com.orangelabs.rcs.core.content.VideoContent;
import com.orangelabs.rcs.core.ims.service.ipcall.IPCallSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.ipcall.IPCallHistory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * IP call service API implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class IPCallServiceImpl extends IIPCallService.Stub {
	/**
	 * List of service event listeners
	 */
	private RemoteCallbackList<IJoynServiceRegistrationListener> serviceListeners = new RemoteCallbackList<IJoynServiceRegistrationListener>();

	/**
	 * List of IP call sessions
	 */
	private static Hashtable<String, IIPCall> ipCallSessions = new Hashtable<String, IIPCall>();

	/**
	 * List of IP call invitation listeners
	 */
	private RemoteCallbackList<INewIPCallListener> listeners = new RemoteCallbackList<INewIPCallListener>();

	/**
	 * The logger
	 */
	private static Logger logger = Logger.getLogger(IPCallServiceImpl.class.getName());

	/**
	 * Lock used for synchronization
	 */
	private Object lock = new Object();

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
     * Receive a new IP call invitation
     * 
     * @param session IP call session
     */
	public void receiveIPCallInvitation(IPCallSession session) {
		if (logger.isActivated()) {
			logger.info("Receive IP call invitation from " + session.getRemoteContact());
		}

		// Extract number from contact
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());

		// Get audio encoding
		AudioContent audiocontent = session.getAudioContent();
		
		// Get video encoding
		VideoContent videocontent = session.getVideoContent();

		// Update IP call history
		IPCallHistory.getInstance().addCall(number, session.getSessionID(),
				IPCall.Direction.INCOMING,
				audiocontent, videocontent,
				IPCall.State.INVITED);

		// Add session in the list
		IPCallImpl sessionApi = new IPCallImpl(session);
		IPCallServiceImpl.addIPCallSession(sessionApi);

		// Broadcast intent related to the received invitation
		Intent intent = new Intent(IPCallIntent.ACTION_NEW_INVITATION);
		intent.putExtra(IPCallIntent.EXTRA_CONTACT, number);
		intent.putExtra(IPCallIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
		intent.putExtra(IPCallIntent.EXTRA_CALL_ID, session.getSessionID());
    	intent.putExtra(IPCallIntent.EXTRA_AUDIO_ENCODING, audiocontent.getEncoding());
    	if (videocontent != null) {
	    	intent.putExtra(IPCallIntent.EXTRA_VIDEO_ENCODING, videocontent.getEncoding());
	        intent.putExtra(IPCallIntent.EXTRA_VIDEO_FORMAT, ""); // TODO
    	}
        AndroidFactory.getApplicationContext().sendBroadcast(intent);

    	// Notify IP call invitation listeners
    	synchronized(lock) {
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onNewCall(session.getSessionID());
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
     * Initiates an IP call with a contact. The parameter contact supports the following
     * formats: MSISDN in national or international format, SIP address, SIP-URI or
     * el-URI. If the format of the contact is not supported an exception is thrown.
     * 
     * @param contact Contact
     * @param player IP call player
     * @param renderer IP call renderer
     * @param listener IP call event listener
     * @return IP call
	 * @throws ServerApiException 
     */
    public IIPCall initiateCall(String contact, IIPCallPlayer player, IIPCallRenderer renderer, IIPCallListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate an IP call session with " + contact);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		// Test if at least the audio media is configured
		if ((player == null) || (renderer == null)) {
			throw new ServerApiException("Missing audio player or renderer");
		}
		
		try {
			// Initiate a new session
			IPCallSession session = Core.getInstance().getIPCallService().initiateIPCallSession(contact, player, renderer);

			// Update IP call history
			IPCallHistory.getInstance().addCall(contact, session.getSessionID(),
					IPCall.Direction.OUTGOING,
					session.getAudioContent(), session.getVideoContent(),
					IPCall.State.INITIATED);

			// Add session in the list
			IPCallImpl sessionApi = new IPCallImpl(session);
			sessionApi.addEventListener(listener);
			
			// Start the session
			session.startSession();
			
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
    			RcsSettings.getInstance().isVoiceBreakoutSupported());    	
	}
    
    /**
	 * Registers an IP call invitation listener
	 * 
	 * @param listener New IP call listener
	 * @throws ServerApiException
	 */
	public void addNewIPCallListener(INewIPCallListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Add an IP call invitation listener");
		}
		
		listeners.register(listener);
	}

	/**
	 * Unregisters an IP call invitation listener
	 * 
	 * @param listener New IP call listener
	 * @throws ServerApiException
	 */
	public void removeNewIPCallListener(INewIPCallListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Remove an IP call invitation listener");
		}
		
		listeners.unregister(listener);
	}
}
