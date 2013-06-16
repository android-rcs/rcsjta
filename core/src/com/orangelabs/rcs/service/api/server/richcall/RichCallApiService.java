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

package com.orangelabs.rcs.service.api.server.richcall;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import android.content.Intent;
import android.os.IBinder;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.VideoContent;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingSession;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.file.FileDescription;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.provider.sharing.RichCall;
import com.orangelabs.rcs.provider.sharing.RichCallData;
import com.orangelabs.rcs.service.api.client.media.IMediaPlayer;
import com.orangelabs.rcs.service.api.client.richcall.IRichCallApi;
import com.orangelabs.rcs.service.api.client.richcall.IVideoSharingSession;
import com.orangelabs.rcs.service.api.client.richcall.RichCallApiIntents;
import com.orangelabs.rcs.service.api.server.ServerApiException;
import com.orangelabs.rcs.service.api.server.ServerApiUtils;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Rich call API service
 * 
 * @author Jean-Marc AUFFRET
 */
public class RichCallApiService extends IRichCallApi.Stub {
	/**
	 * List of video sharing sessions
	 */
    private static Hashtable<String, IVideoSharingSession> videoSharingSessions = new Hashtable<String, IVideoSharingSession>();

    /**
	 * The logger
	 */
	private static Logger logger = Logger.getLogger(RichCallApiService.class.getName());

	/**
	 * Constructor
	 */
	public RichCallApiService() {
		if (logger.isActivated()) {
			logger.info("Rich call API is loaded");
		}
	}

	/**
	 * Close API
	 */
	public void close() {
		// Clear lists of sessions
		videoSharingSessions.clear();
	}

    /**
     * Add a video sharing session in the list
     * 
     * @param session Video sharing session
     */
	protected static void addVideoSharingSession(VideoSharingSession session) {
		if (logger.isActivated()) {
			logger.debug("Add a video sharing session in the list (size=" + videoSharingSessions.size() + ")");
		}
		videoSharingSessions.put(session.getSessionID(), session);
	}

    /**
     * Remove a video sharing session from the list
     * 
     * @param sessionId Session ID
     */
	protected static void removeVideoSharingSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove a video sharing session from the list (size=" + videoSharingSessions.size() + ")");
		}
		videoSharingSessions.remove(sessionId);
	}

    /**
     * Get the remote phone number involved in the current call
     * 
     * @return Phone number or null if there is no call in progress
     * @throws ServerApiException
     */
	public String getRemotePhoneNumber() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get remote phone number");
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		try {
			return Core.getInstance().getImsModule().getCallManager().getRemoteParty();
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
		if (logger.isActivated()) {
			logger.info("Receive video sharing invitation from " + session.getRemoteContact());
		}

        // Extract number from contact
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());
        VideoContent content = (VideoContent) session.getContent();

		// Update rich call history
		RichCall.getInstance().addCall(number, session.getSessionID(),
				RichCallData.EVENT_INCOMING,
				content,
    			RichCallData.STATUS_STARTED);

		// Add session in the list
		VideoSharingSession sessionApi = new VideoSharingSession(session);
		addVideoSharingSession(sessionApi);

		// Broadcast intent related to the received invitation
    	Intent intent = new Intent(RichCallApiIntents.VIDEO_SHARING_INVITATION);
    	intent.putExtra("contact", number);
    	intent.putExtra("contactDisplayname", session.getRemoteDisplayName());
    	intent.putExtra("sessionId", session.getSessionID());
    	intent.putExtra("videotype", content.getEncoding());
        intent.putExtra("videowidth", content.getWidth());
        intent.putExtra("videoheight", content.getHeight());
        AndroidFactory.getApplicationContext().sendBroadcast(intent);
    }

	/**
     * Initiate a live video sharing session
     * 
     * @param contact Contact
     * @param player Media player
     * @throws ServerApiException
     */
	public IVideoSharingSession initiateLiveVideoSharing(String contact, IMediaPlayer player) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate a live video session with " + contact);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
		     // Initiate a new session
            VideoStreamingSession session = Core.getInstance().getRichcallService()
                    .initiateLiveVideoSharingSession(contact, player);

			// Update rich call history
			RichCall.getInstance().addCall(contact, session.getSessionID(),
                    RichCallData.EVENT_OUTGOING,
	    			session.getContent(),
	    			RichCallData.STATUS_STARTED);

			// Add session in the list
			VideoSharingSession sessionApi = new VideoSharingSession(session);
			addVideoSharingSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

    /**
     * Initiate a pre-recorded video sharing session
     * 
     * @param contact Contact
     * @param file Video file
     * @param player Media player
     * @throws ServerApiException
     */
	public IVideoSharingSession initiateVideoSharing(String contact, String file, IMediaPlayer player) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate a pre-recorded video session with " + contact);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Create a video content
			FileDescription desc = FileFactory.getFactory().getFileDescription(file);
			VideoContent content = (VideoContent)ContentManager.createMmContentFromUrl(file, desc.getSize());
			VideoStreamingSession session = Core.getInstance().getRichcallService().initiatePreRecordedVideoSharingSession(contact, content, player);

			// Update rich call history
			RichCall.getInstance().addCall(contact, session.getSessionID(),
                    RichCallData.EVENT_OUTGOING,
	    			session.getContent(),
	    			RichCallData.STATUS_STARTED);

			// Add session in the list
			VideoSharingSession sessionApi = new VideoSharingSession(session);
			addVideoSharingSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

	/**
	 * Get current video sharing session from its session ID
	 *
	 * @param id Session ID
	 * @return Session
	 * @throws ServerApiException
	 */
	public IVideoSharingSession getVideoSharingSession(String id) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get video sharing session " + id);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		// Return a session instance
		return videoSharingSessions.get(id);
	}
	
	/**
	 * Get list of current video sharing sessions with a contact
	 * 
	 * @param contact Contact
	 * @return List of sessions
	 * @throws ServerApiException
	 */
	public List<IBinder> getVideoSharingSessionsWith(String contact) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get video sharing sessions with " + contact);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();
		
		try {
			Vector<ContentSharingSession> list = Core.getInstance().getRichcallService().getCShSessions(contact);
			ArrayList<IBinder> result = new ArrayList<IBinder>(list.size());
			for(int i=0; i < list.size(); i++) {
				ContentSharingSession session = list.elementAt(i);
				IVideoSharingSession sessionApi = videoSharingSessions.get(session.getSessionID());
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
}
