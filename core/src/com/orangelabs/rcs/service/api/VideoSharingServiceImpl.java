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
import org.gsma.joyn.JoynService;
import org.gsma.joyn.vsh.INewVideoSharingListener;
import org.gsma.joyn.vsh.IVideoPlayer;
import org.gsma.joyn.vsh.IVideoSharing;
import org.gsma.joyn.vsh.IVideoSharingListener;
import org.gsma.joyn.vsh.IVideoSharingService;
import org.gsma.joyn.vsh.VideoSharing;
import org.gsma.joyn.vsh.VideoSharingIntent;
import org.gsma.joyn.vsh.VideoSharingServiceConfiguration;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.content.VideoContent;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.sharing.RichCallHistory;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Rich call API service
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoSharingServiceImpl extends IVideoSharingService.Stub {
	/**
	 * List of service event listeners
	 */
	private RemoteCallbackList<IJoynServiceRegistrationListener> serviceListeners = new RemoteCallbackList<IJoynServiceRegistrationListener>();

	/**
	 * List of video sharing sessions
	 */
    private static Hashtable<String, IVideoSharing> videoSharingSessions = new Hashtable<String, IVideoSharing>();

	/**
	 * List of video sharing invitation listeners
	 */
	private RemoteCallbackList<INewVideoSharingListener> listeners = new RemoteCallbackList<INewVideoSharingListener>();

	/**
	 * Lock used for synchronization
	 */
	private Object lock = new Object();

	/**
	 * The logger
	 */
	private static Logger logger = Logger.getLogger(VideoSharingServiceImpl.class.getName());

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
	protected static void addVideoSharingSession(VideoSharingImpl session) {
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
	protected static void removeVideoSharingSession(String sessionId) {
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
     * Get the remote phone number involved in the current call
     * 
     * @return Phone number or null if there is no call in progress
     * @throws ServerApiException
     */
	public String getRemotePhoneNumber() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get remote phone number");
		}

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
		
		// Update rich call history
        VideoContent content = (VideoContent)session.getContent();
		RichCallHistory.getInstance().addVideoSharing(number, session.getSessionID(),
				VideoSharing.Direction.INCOMING,
				content,
    			VideoSharing.State.INVITED);

		// Add session in the list
		VideoSharingImpl sessionApi = new VideoSharingImpl(session);
		VideoSharingServiceImpl.addVideoSharingSession(sessionApi);

		// Broadcast intent related to the received invitation
    	Intent intent = new Intent(VideoSharingIntent.ACTION_NEW_INVITATION);
    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
    	intent.putExtra(VideoSharingIntent.EXTRA_CONTACT, number);
    	intent.putExtra(VideoSharingIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
    	intent.putExtra(VideoSharingIntent.EXTRA_SHARING_ID, session.getSessionID());
    	intent.putExtra(VideoSharingIntent.EXTRA_ENCODING, content.getEncoding());
        intent.putExtra(VideoSharingIntent.EXTRA_WIDTH, session.getVideoWidth());
        intent.putExtra(VideoSharingIntent.EXTRA_HEIGHT, session.getVideoHeight());
        AndroidFactory.getApplicationContext().sendBroadcast(intent);
        
    	// Notify video sharing invitation listeners
    	synchronized(lock) {
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onNewVideoSharing(session.getSessionID());
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
     * @param contact Contact
     * @param player Video player
     * @param listener Video sharing event listener
     * @return Video sharing
	 * @throws ServerApiException
     */
    public IVideoSharing shareVideo(String contact, IVideoPlayer player, IVideoSharingListener listener) throws ServerApiException {
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
            VideoStreamingSession session = Core.getInstance().getRichcallService().initiateLiveVideoSharingSession(contact, player);

			// Update rich call history
			RichCallHistory.getInstance().addVideoSharing(contact, session.getSessionID(),
					VideoSharing.Direction.OUTGOING,
	    			session.getContent(),
	    			VideoSharing.State.INITIATED);

			// Add session listener
			VideoSharingImpl sessionApi = new VideoSharingImpl(session);
			sessionApi.addEventListener(listener);
			
			// Start the session
			session.startSession();
			
			// Add session in the list
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
	 * Registers an video sharing invitation listener
	 * 
	 * @param listener New video sharing listener
	 * @throws ServerApiException
	 */
	public void addNewVideoSharingListener(INewVideoSharingListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Add an video sharing invitation listener");
		}
		
		listeners.register(listener);
	}

	/**
	 * Unregisters an video sharing invitation listener
	 * 
	 * @param listener New video sharing listener
	 * @throws ServerApiException
	 */
	public void removeNewVideoSharingListener(INewVideoSharingListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Remove an video sharing invitation listener");
		}
		
		listeners.unregister(listener);
	}

	/**
	 * Returns service version
	 * 
	 * @return Version
	 * @see JoynService.Build.GSMA_VERSION
	 * @throws ServerApiException
	 */
	public int getServiceVersion() throws ServerApiException {
		return JoynService.Build.GSMA_VERSION;
	}
}
