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
import com.orangelabs.rcs.core.content.GeolocContent;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.content.VideoContent;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingSession;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.GeolocTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.file.FileDescription;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.provider.messaging.RichMessaging;
import com.orangelabs.rcs.provider.sharing.RichCall;
import com.orangelabs.rcs.provider.sharing.RichCallData;
import com.orangelabs.rcs.service.api.client.media.IMediaPlayer;
import com.orangelabs.rcs.service.api.client.messaging.GeolocMessage;
import com.orangelabs.rcs.service.api.client.messaging.GeolocPush;
import com.orangelabs.rcs.service.api.client.richcall.IGeolocSharingSession;
import com.orangelabs.rcs.service.api.client.richcall.IImageSharingSession;
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
 * @author jexa7410
 */
public class RichCallApiService extends IRichCallApi.Stub {
	/**
	 * List of image sharing sessions
	 */
    private static Hashtable<String, IImageSharingSession> imageSharingSessions = new Hashtable<String, IImageSharingSession>();

	/**
	 * List of video sharing sessions
	 */
    private static Hashtable<String, IVideoSharingSession> videoSharingSessions = new Hashtable<String, IVideoSharingSession>();

	/**
	 * List of geoloc sharing sessions
	 */
    private static Hashtable<String, IGeolocSharingSession> geolocSharingSessions = new Hashtable<String, IGeolocSharingSession>();

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
		imageSharingSessions.clear();
		videoSharingSessions.clear();
	}

	/**
     * Add an image sharing session in the list
     * 
     * @param session Image sharing session
     */
	protected static void addImageSharingSession(ImageSharingSession session) {
		if (logger.isActivated()) {
			logger.debug("Add an image sharing session in the list (size=" + imageSharingSessions.size() + ")");
		}
		imageSharingSessions.put(session.getSessionID(), session);
	}

    /**
     * Remove an image sharing session from the list
     * 
     * @param sessionId Session ID
     */
	protected static void removeImageSharingSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove an image sharing session from the list (size=" + imageSharingSessions.size() + ")");
		}
		imageSharingSessions.remove(sessionId);
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
     * Add a geoloc sharing session in the list
     * 
     * @param session Geoloc sharing session
     */
	protected static void addGeolocSharingSession(GeolocSharingSession session) {
		if (logger.isActivated()) {
			logger.debug("Add a geoloc sharing session in the list (size=" + geolocSharingSessions.size() + ")");
		}
		geolocSharingSessions.put(session.getSessionID(), session);
	}

    /**
     * Remove a geoloc sharing session from the list
     * 
     * @param sessionId Session ID
     */
	protected static void removeGeolocSharingSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove a geoloc sharing session from the list (size=" + geolocSharingSessions.size() + ")");
		}
		geolocSharingSessions.remove(sessionId);
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

    /**
     * Receive a new image sharing invitation
     * 
     * @param session Image sharing session
     */
    public void receiveImageSharingInvitation(ImageTransferSession session) {
		if (logger.isActivated()) {
			logger.info("Receive image sharing invitation from " + session.getRemoteContact());
		}

        // Extract number from contact
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());

		// Update rich call history
		RichCall.getInstance().addCall(number, session.getSessionID(),
				RichCallData.EVENT_INCOMING,
				session.getContent(),
				RichCallData.STATUS_STARTED);

		// Add session in the list
		ImageSharingSession sessionApi = new ImageSharingSession(session);
		addImageSharingSession(sessionApi);

		// Broadcast intent related to the received invitation
		Intent intent = new Intent(RichCallApiIntents.IMAGE_SHARING_INVITATION);
		intent.putExtra("contact", number);
		intent.putExtra("contactDisplayname", session.getRemoteDisplayName());
		intent.putExtra("sessionId", session.getSessionID());
		intent.putExtra("filename", session.getContent().getName());
		intent.putExtra("filesize", session.getContent().getSize());
		intent.putExtra("filetype", session.getContent().getEncoding());
    	intent.putExtra("thumbnail", session.getThumbnail());		
        AndroidFactory.getApplicationContext().sendBroadcast(intent);
    }

    /**
     * Initiate an image sharing session
     * 
     * @param contact Contact
     * @param file Image file
     * @param thumbnail Thumbnail option
     * @throws ServerApiException
     */
	public IImageSharingSession initiateImageSharing(String contact, String file, boolean thumbnail) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate an image sharing session with " + contact);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Create an image content
			FileDescription desc = FileFactory.getFactory().getFileDescription(file);
			MmContent content = ContentManager.createMmContentFromUrl(file, desc.getSize());
			
			// Initiate a sharing session
			ImageTransferSession session = Core.getInstance().getRichcallService().initiateImageSharingSession(contact, content, thumbnail);

			// Update rich call history
			RichCall.getInstance().addCall(contact, session.getSessionID(),
                    RichCallData.EVENT_OUTGOING,
	    			session.getContent(),
	    			RichCallData.STATUS_STARTED);

			// Add session in the list
			ImageSharingSession sessionApi = new ImageSharingSession(session);
			addImageSharingSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

    /**
     * Get current image sharing session from its session ID
     * 
     * @param id Session ID
     * @return Session
     * @throws ServerApiException
     */
	public IImageSharingSession getImageSharingSession(String id) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get image sharing session " + id);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		// Return a session instance
		return imageSharingSessions.get(id);
	}
	
	/**
	 * Get list of current image sharing sessions with a contact
	 * 
	 * @param contact Contact
	 * @return List of sessions
	 * @throws ServerApiException
	 */
	public List<IBinder> getImageSharingSessionsWith(String contact) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get image sharing sessions with " + contact);
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
				IImageSharingSession sessionApi = imageSharingSessions.get(session.getSessionID());
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
	 * Initiate a geoloc sharing session
	 * 
	 * @param contact Contact
	 * @param geoloc Geoloc info
	 * @return Geoloc sharing session
     * @throws ServerApiException
	 */
	public IGeolocSharingSession initiateGeolocSharing(String contact, GeolocPush geoloc) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate a geoloc sharing session with " + contact);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Create a geoloc content
			String msgId = ChatUtils.generateMessageId();
			String geolocDoc = ChatUtils.buildGeolocDocument(geoloc, ImsModule.IMS_USER_PROFILE.getPublicUri(), msgId);
			MmContent content = new GeolocContent("geoloc.xml", geolocDoc.getBytes().length, geolocDoc.getBytes());

			// Initiate a sharing session
			GeolocTransferSession session = Core.getInstance().getRichcallService().initiateGeolocSharingSession(contact, content, geoloc);

			// Update rich call
			RichCall.getInstance().addCall(contact, session.getSessionID(),
                    RichCallData.EVENT_OUTGOING,
	    			session.getContent(),
	    			RichCallData.STATUS_STARTED);

			// Update rich messaging history
			GeolocMessage geolocMsg = new GeolocMessage(null, contact, geoloc, false);
			RichMessaging.getInstance().addOutgoingGeoloc(geolocMsg);

			// Add session in the list
			GeolocSharingSession sessionApi = new GeolocSharingSession(session);
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

		// Update rich call history
		RichCall.getInstance().addCall(number, session.getSessionID(),
				RichCallData.EVENT_INCOMING,
				session.getContent(),
				RichCallData.STATUS_STARTED);
		
		// Add session in the list
		GeolocSharingSession sessionApi = new GeolocSharingSession(session);
		addGeolocSharingSession(sessionApi);

		// Broadcast intent related to the received invitation
		Intent intent = new Intent(RichCallApiIntents.GEOLOC_SHARING_INVITATION);
		intent.putExtra("contact", number);
		intent.putExtra("contactDisplayname", session.getRemoteDisplayName());
		intent.putExtra("sessionId", session.getSessionID());
        AndroidFactory.getApplicationContext().sendBroadcast(intent);
    }	
	
    /**
     * Get current geoloc sharing session from its session ID
     * 
     * @param id Session ID
     * @return Session
     * @throws ServerApiException
     */
	public IGeolocSharingSession getGeolocSharingSession(String id) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get geoloc sharing session " + id);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		// Return a session instance
		return geolocSharingSessions.get(id);
	}	

    /**
     * Set multiparty call
     * 
     * @param state State
     * @throws ServerApiException
     */
	public void setMultiPartyCall(boolean state) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Set multiparty call to " + state);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		// Update call manager
    	Core.getInstance().getImsModule().getCallManager().setMultiPartyCall(state);
	}

    /**
     * Set call hold
     * 
     * @param state State
     * @throws ServerApiException
     */
	public void setCallHold(boolean state) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Set call hold to " + state);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		// Update call manager
    	Core.getInstance().getImsModule().getCallManager().setCallHold(state);
	}
}
