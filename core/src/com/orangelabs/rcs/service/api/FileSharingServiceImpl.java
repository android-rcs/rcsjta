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
import android.net.Uri;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteCallbackList;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.fsh.FileSharing;
import com.gsma.services.rcs.fsh.FileSharingIntent;
import com.gsma.services.rcs.fsh.IFileSharing;
import com.gsma.services.rcs.fsh.IFileSharingListener;
import com.gsma.services.rcs.fsh.IFileSharingService;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.file.FileDescription;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.sharing.RichCallHistory;
import com.orangelabs.rcs.service.broadcaster.FileSharingEventBroadcaster;
import com.orangelabs.rcs.service.broadcaster.JoynServiceRegistrationEventBroadcaster;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * File sharing service implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileSharingServiceImpl extends IFileSharingService.Stub {

	/**
	 * List of file sharing sessions
	 */
	private static Hashtable<String, IFileSharing> fshSessions = new Hashtable<String, IFileSharing>();

	private final FileSharingEventBroadcaster mFileSharingEventBroadcaster = new FileSharingEventBroadcaster();

	private final JoynServiceRegistrationEventBroadcaster mJoynServiceRegistrationEventBroadcaster = new JoynServiceRegistrationEventBroadcaster();

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(FileSharingServiceImpl.class.getSimpleName());

	/**
	 * Lock used for synchronization
	 */
	private Object lock = new Object();

	/**
	 * Constructor
	 */
	public FileSharingServiceImpl() {
		if (logger.isActivated()) {
			logger.info("File sharing service API is loaded");
		}
	}

	/**
	 * Close API
	 */
	public void close() {
		// Clear list of sessions
		fshSessions.clear();
		
		if (logger.isActivated()) {
			logger.info("File sharing service API is closed");
		}
	}

	/**
	 * Add a file sharing session in the list
	 * 
	 * @param session File sharing session
	 */
	protected static void addFileSharingSession(FileSharingImpl session) {
		if (logger.isActivated()) {
			logger.debug("Add a file sharing session in the list (size=" + fshSessions.size() + ")");
		}
		
		fshSessions.put(session.getSharingId(), session);
	}

	/**
	 * Remove a file sharing session from the list
	 * 
	 * @param sessionId Session ID
	 */
	protected static void removeFileSharingSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove a file sharing session from the list (size=" + fshSessions.size() + ")");
		}
		
		fshSessions.remove(sessionId);
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
     * Receive a new file sharing invitation
     * 
     * @param session File sharing session
     */
    public void receiveFileSharingInvitation(FileSharingSession session) {
		if (logger.isActivated()) {
			logger.info("Receive file sharing invitation from " + session.getRemoteContact());
		}
		ContactId contact = session.getRemoteContact();
		// Update rich call history
		RichCallHistory.getInstance().addFileSharing(contact, session.getSessionID(),
				FileSharing.Direction.INCOMING,
				session.getContent(),
				FileSharing.State.INVITED);
		// TODO : Update displayName of remote contact
		/*
		 * ContactsManager.getInstance().setContactDisplayName(contact,
		 * session.getRemoteDisplayName());
		 */
		// Add session in the list
		FileSharingImpl sessionApi = new FileSharingImpl(session, mFileSharingEventBroadcaster);
		FileSharingServiceImpl.addFileSharingSession(sessionApi);

		// Broadcast intent related to the received invitation
		Intent intent = new Intent(FileSharingIntent.ACTION_NEW_INVITATION);
		intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
		intent.putExtra(FileSharingIntent.EXTRA_SHARING_ID, session.getSessionID());
		AndroidFactory.getApplicationContext().sendBroadcast(intent);
    }

    /**
     * Shares a file with a contact. The parameter file contains the URI of the file to be
     * shared (for a local or a remote file). An exception if thrown if there is
     * no ongoing CS call. The parameter contact supports the following formats: MSISDN
     * in national or international format, SIP address, SIP-URI or Tel-URI. If the format
     * of the contact is not supported an exception is thrown.
     * 
     * @param contact Contact ID
     * @param file URI of file to share
     * @return File sharing
     * @throws ServerApiException
     */
    public IFileSharing shareFile(ContactId contact, Uri file) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate a file sharing session with " + contact);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Create a file content
			FileDescription desc = FileFactory.getFactory().getFileDescription(file);
			MmContent content = ContentManager.createMmContent(file, desc.getSize(), desc.getName());

			// Initiate a sharing session
			final FileSharingSession session = Core.getInstance().getRichcallService().initiateFileSharingSession(contact, content, null);

			// Update rich call history
			RichCallHistory.getInstance().addFileSharing(contact, session.getSessionID(),
					FileSharing.Direction.OUTGOING,
	    			session.getContent(),
	    			FileSharing.State.INITIATED);

			// Add session listener
			FileSharingImpl sessionApi = new FileSharingImpl(session, mFileSharingEventBroadcaster);

			// Start the session
	        new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	}.start();	
			
			// Add session in the list
			addFileSharingSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			// TODO:Handle Security exception in CR026
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

    /**
     * Returns the list of file sharings in progress
     * 
     * @return List of file sharings
     * @throws ServerApiException
     */
    public List<IBinder> getFileSharings() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get file sharing sessions");
		}

		try {
			ArrayList<IBinder> result = new ArrayList<IBinder>(fshSessions.size());
			for (Enumeration<IFileSharing> e = fshSessions.elements() ; e.hasMoreElements() ;) {
				IFileSharing sessionApi = e.nextElement() ;
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
     * Returns a current file sharing from its unique ID
     * 
     * @return File sharing
     * @throws ServerApiException
     */
    public IFileSharing getFileSharing(String sharingId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get file sharing session " + sharingId);
		}

		return fshSessions.get(sharingId);
    }    

	/**
	 * Adds an event listener on file sharing events
	 * 
	 * @param listener Listener
	 */
	public void addEventListener(IFileSharingListener listener) {
		if (logger.isActivated()) {
			logger.info("Add a file sharing event listener");
		}
		synchronized (lock) {
			mFileSharingEventBroadcaster.addEventListener(listener);
		}
	}

	/**
	 * Removes an event listener on file sharing events
	 * 
	 * @param listener Listener
	 */
	public void removeEventListener(IFileSharingListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove a file sharing event listener");
		}
		synchronized (lock) {
			mFileSharingEventBroadcaster.removeEventListener(listener);
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
