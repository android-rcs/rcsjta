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
import com.gsma.services.rcs.RcsCommon.Direction;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.vsh.IVideoPlayer;
import com.gsma.services.rcs.vsh.IVideoSharing;
import com.gsma.services.rcs.vsh.IVideoSharingListener;
import com.gsma.services.rcs.vsh.IVideoSharingService;
import com.gsma.services.rcs.vsh.VideoSharing;
import com.gsma.services.rcs.vsh.VideoSharing.ReasonCode;
import com.gsma.services.rcs.vsh.VideoSharingServiceConfiguration;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.content.VideoContent;
import com.orangelabs.rcs.core.ims.service.SessionIdGenerator;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.sharing.RichCallHistory;
import com.orangelabs.rcs.service.broadcaster.RcsServiceRegistrationEventBroadcaster;
import com.orangelabs.rcs.service.broadcaster.VideoSharingEventBroadcaster;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Rich call API service
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoSharingServiceImpl extends IVideoSharingService.Stub {

	private final VideoSharingEventBroadcaster mVideoSharingEventBroadcaster = new VideoSharingEventBroadcaster();

	private final RcsServiceRegistrationEventBroadcaster mRcsServiceRegistrationEventBroadcaster = new RcsServiceRegistrationEventBroadcaster();

	/**
	 * List of video sharing sessions
	 */
    private static Hashtable<String, IVideoSharing> videoSharingSessions = new Hashtable<String, IVideoSharing>();

	/**
	 * Lock used for synchronization
	 */
	private final Object lock = new Object();

	/**
	 * The logger
	 */
	private static final  Logger logger = Logger.getLogger(VideoSharingServiceImpl.class.getSimpleName());

	/**
	 * Constructor
	 */
	public VideoSharingServiceImpl() {
		if (logger.isActivated()) {
			logger.info("Video sharing API is loaded");
		}
	}

	/**
	 * Close API
	 */
	public void close() {
		// Clear list of sessions
		videoSharingSessions.clear();
		
		if (logger.isActivated()) {
			logger.info("Video sharing service API is closed");
		}
	}

    /**
     * Add a video sharing session in the list
     * 
     * @param session Video sharing session
     */
	private static void addVideoSharingSession(VideoSharingImpl session) {
		if (logger.isActivated()) {
			logger.debug("Add a video sharing session in the list (size=" + videoSharingSessions.size() + ")");
		}
		
		videoSharingSessions.put(session.getSharingId(), session);
	}

    /**
     * Remove a video sharing session from the list
     * 
     * @param sessionId Session ID
     */
	/* package private */ static void removeVideoSharingSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove a video sharing session from the list (size=" + videoSharingSessions.size() + ")");
		}
		
		videoSharingSessions.remove(sessionId);
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
     * Get the remote contact Id involved in the current call
     * 
     * @return ContactId or null if there is no call in progress
     * @throws ServerApiException
     */
	public ContactId getRemotePhoneNumber() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get remote phone number");
		}

		// Test core availability
		ServerApiUtils.testCore();

		try {
			return Core.getInstance().getImsModule().getCallManager().getContact();
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

	/**
     * Receive a new video sharing invitation
     * 
     * @param session Video sharing session
     */
    public void receiveVideoSharingInvitation(VideoStreamingSession session) {
		ContactId contact = session.getRemoteContact();
		if (logger.isActivated()) {
			logger.info("Receive video sharing invitation from " + contact + " displayName=" + session.getRemoteDisplayName());
		}

		// Update displayName of remote contact
		ContactsManager.getInstance().setContactDisplayName(contact, session.getRemoteDisplayName());
		// Add session in the list
		VideoSharingImpl sessionApi = new VideoSharingImpl(session, mVideoSharingEventBroadcaster);
		VideoSharingServiceImpl.addVideoSharingSession(sessionApi);
    }
    
    /**
     * Returns the configuration of video sharing service
     * 
     * @return Configuration
     */
    public VideoSharingServiceConfiguration getConfiguration() {
    	return new VideoSharingServiceConfiguration(
    			RcsSettings.getInstance().getMaxVideoShareDuration());    	
	}

    /**
     * Shares a live video with a contact. The parameter renderer contains the video player
     * provided by the application. An exception if thrown if there is no ongoing CS call. The
     * parameter contact supports the following formats: MSISDN in national or international
     * format, SIP address, SIP-URI or Tel-URI. If the format of the contact is not supported
     * an exception is thrown.
     * 
     * @param contact Contact ID
     * @param player Video player
     * @return Video sharing
	 * @throws ServerApiException
     */
    public IVideoSharing shareVideo(ContactId contact, IVideoPlayer player) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate a live video session with " + contact);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		// Test if at least the audio media is configured
		if (player == null) {
			throw new ServerApiException("Missing video player");
		}

		try {
		     // Initiate a new session
            final VideoStreamingSession session = Core.getInstance().getRichcallService().initiateLiveVideoSharingSession(contact, player);

			String sharingId = session.getSessionID();
			RichCallHistory.getInstance().addVideoSharing(contact, sharingId,
					Direction.OUTGOING, (VideoContent)session.getContent(),
					VideoSharing.State.INITIATED, ReasonCode.UNSPECIFIED);
			mVideoSharingEventBroadcaster.broadcastStateChanged(contact, sharingId,
					VideoSharing.State.INITIATED, ReasonCode.UNSPECIFIED);

			// Add session listener
			VideoSharingImpl sessionApi = new VideoSharingImpl(session, mVideoSharingEventBroadcaster);

			addVideoSharingSession(sessionApi);

			// Start the session
	        Thread t = new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	};
	    	t.start();	
			return sessionApi;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

    /**
     * Returns a current video sharing from its unique ID
     * 
     * @return Video sharing or null if not found
     * @throws ServerApiException
     */
    public IVideoSharing getVideoSharing(String sharingId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get video sharing session " + sharingId);
		}

		return videoSharingSessions.get(sharingId);
	}

    /**
     * Returns the list of video sharings in progress
     * 
     * @return List of video sharings
     * @throws ServerApiException
     */
    public List<IBinder> getVideoSharings() throws ServerApiException {
    	if (logger.isActivated()) {
			logger.info("Get video sharing sessions");
		}

		try {
			ArrayList<IBinder> result = new ArrayList<IBinder>(videoSharingSessions.size());
			for (Enumeration<IVideoSharing> e = videoSharingSessions.elements() ; e.hasMoreElements() ;) {
				IVideoSharing sessionApi = e.nextElement() ;
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
	 * Add and broadcast video sharing invitation rejections
	 *
	 * @param contact Contact ID
	 * @param content Video content
	 * @param reasonCode Reason code
	 */
	public void addAndBroadcastVideoSharingInvitationRejected(ContactId contact, VideoContent content,
			int reasonCode) {
		String sessionId = SessionIdGenerator.getNewId();
		RichCallHistory.getInstance().addVideoSharing(contact, sessionId,
				Direction.INCOMING, content, VideoSharing.State.REJECTED, reasonCode);
		mVideoSharingEventBroadcaster.broadcastInvitation(sessionId);
	}

    /**
	 * Adds a listener on video sharing events
	 * 
	 * @param listener Listener
	 */
	public void addEventListener2(IVideoSharingListener listener) {
		if (logger.isActivated()) {
			logger.info("Add a video sharing event listener");
		}
		synchronized (lock) {
			mVideoSharingEventBroadcaster.addEventListener(listener);
		}
	}

	/**
	 * Removes a listener from video sharing events
	 * 
	 * @param listener Listener
	 */
	public void removeEventListener2(IVideoSharingListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove a video sharing event listener");
		}
		synchronized (lock) {
			mVideoSharingEventBroadcaster.removeEventListener(listener);
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
}
