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

package com.orangelabs.rcs.core.ims.service.richcall;

import static com.orangelabs.rcs.utils.StringUtils.UTF8;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.gsh.GeolocSharing;
import com.gsma.services.rcs.ish.ImageSharing;
import com.gsma.services.rcs.vsh.IVideoPlayer;
import com.gsma.services.rcs.vsh.VideoSharing;
import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.GeolocContent;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.content.VideoContent;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.FeatureTags;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.GeolocTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.OriginatingGeolocTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.TerminatingGeolocTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.image.OriginatingImageTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.image.TerminatingImageTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.video.OriginatingVideoStreamingSession;
import com.orangelabs.rcs.core.ims.service.richcall.video.TerminatingVideoStreamingSession;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.orangelabs.rcs.utils.ContactUtils;
import com.orangelabs.rcs.utils.logger.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Rich call service has in charge to monitor the GSM call in order to stop the
 * current content sharing when the call terminates, to process capability
 * request from remote and to request remote capabilities.
 *
 * @author Jean-Marc AUFFRET
 */
public class RichcallService extends ImsService {
    /**
     * Video share features tags
     */
    public final static String[] FEATURE_TAGS_VIDEO_SHARE = { FeatureTags.FEATURE_3GPP_VIDEO_SHARE };

    /**
     * Image share features tags
     */
    public final static String[] FEATURE_TAGS_IMAGE_SHARE = { FeatureTags.FEATURE_3GPP_VIDEO_SHARE, FeatureTags.FEATURE_3GPP_IMAGE_SHARE };

    /**
     * Geoloc share features tags
     */
    public final static String[] FEATURE_TAGS_GEOLOC_SHARE = { FeatureTags.FEATURE_3GPP_VIDEO_SHARE, FeatureTags.FEATURE_3GPP_LOCATION_SHARE };

    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(RichcallService.class.getSimpleName());

	/**
	 * ImageTransferSessionCache with Session ID as key
	 */
	private Map<String, ImageTransferSession> mImageTransferSessionCache = new HashMap<String, ImageTransferSession>();

	/**
	 * VideoStreamingSessionCache with Session ID as key
	 */
	private Map<String, VideoStreamingSession> mVideoStremaingSessionCache = new HashMap<String, VideoStreamingSession>();

	/**
	 * GeolocTransferSessionCache with Session ID as key
	 */
	private Map<String, GeolocTransferSession> mGeolocTransferSessionCache = new HashMap<String, GeolocTransferSession>();

    /**
     * Constructor
     *
     * @param parent IMS module
     * @throws CoreException
     */
	public RichcallService(ImsModule parent) throws CoreException {
        super(parent, true);
	}

	private void handleImageSharingInvitationRejected(SipRequest invite, int reasonCode) {
		ContactId contact = ContactUtils.createContactId(SipUtils.getAssertedIdentity(invite));
		MmContent content = ContentManager.createMmContentFromSdp(invite);
		getImsModule().getCore().getListener()
				.handleImageSharingInvitationRejected(contact, content, reasonCode);
	}

	private void handleVideoSharingInvitationRejected(SipRequest invite, int reasonCode) {
		ContactId contact = ContactUtils.createContactId(SipUtils.getAssertedIdentity(invite));
		VideoContent content = ContentManager.createLiveVideoContentFromSdp(invite.getSdpContent()
				.getBytes(UTF8));
		getImsModule().getCore().getListener()
				.handleVideoSharingInvitationRejected(contact, content, reasonCode);
	}

	private void handleGeolocSharingInvitationRejected(SipRequest invite, int reasonCode) {
		ContactId contact = ContactUtils.createContactId(SipUtils.getAssertedIdentity(invite));
		GeolocContent content = (GeolocContent)ContentManager.createMmContentFromSdp(invite);
		getImsModule().getCore().getListener()
				.handleGeolocSharingInvitationRejected(contact, content, reasonCode);
	}

	/**
	 * Start the IMS service
	 */
	public synchronized void start() {
		if (isServiceStarted()) {
			// Already started
			return;
		}
		setServiceStarted(true);
    }

    /**
     * Stop the IMS service
     */
	public synchronized void stop() {
		if (!isServiceStarted()) {
			// Already stopped
			return;
		}
		setServiceStarted(false);
    }

	/**
     * Check the IMS service
     */
	public void check() {
	}

	private ImageTransferSession getUnidirectionalImageSharingSession() {
		if (logger.isActivated()) {
			logger.debug("Get Unidirection ImageTransferSession ");
		}
		synchronized (getImsServiceSessionOperationLock()) {
			return mImageTransferSessionCache.values().iterator().next();
		}
	}

	private boolean isCurrentlyImageSharingUniDirectional() {
		synchronized (getImsServiceSessionOperationLock()) {
			return mImageTransferSessionCache.size() >= SharingDirection.UNIDIRECTIONAL;
		}
	}

	private boolean isCurrentlyImageSharingBiDirectional() {
		synchronized (getImsServiceSessionOperationLock()) {
			return mImageTransferSessionCache.size() >= SharingDirection.BIDIRECTIONAL;
		}
	}

	private void assertMaximumImageTransferSize(long size, String errorMessage)
			throws CoreException {
		long maxSize = ImageTransferSession.getMaxImageSharingSize();
		if (maxSize > 0 && size > maxSize) {
			if (logger.isActivated()) {
				logger.error(errorMessage);
			}
			/*
			 * TODO : Proper exception handling will be added here as part of
			 * the CR037 implementation
			 */
			throw new CoreException(errorMessage);
		}
	}

	private VideoStreamingSession getUnidirectionalVideoSharingSession() {
		if (logger.isActivated()) {
			logger.debug("Get Unidirection VideoStreamingSession ");
		}
		synchronized (getImsServiceSessionOperationLock()) {
			return mVideoStremaingSessionCache.values().iterator().next();
		}
	}

	private boolean isCurrentlyVideoSharingUniDirectional() {
		synchronized (getImsServiceSessionOperationLock()) {
			return mVideoStremaingSessionCache.size() >= SharingDirection.UNIDIRECTIONAL;
		}
	}

	private boolean isCurrentlyVideoSharingBiDirectional() {
		synchronized (getImsServiceSessionOperationLock()) {
			return mVideoStremaingSessionCache.size() >= SharingDirection.BIDIRECTIONAL;
		}
	}

	private GeolocTransferSession getUnidirectionalGeolocSharingSession() {
		if (logger.isActivated()) {
			logger.debug("Get Unidirection GeolocTransferSession ");
		}
		synchronized (getImsServiceSessionOperationLock()) {
			return mGeolocTransferSessionCache.values().iterator().next();
		}
	}

	private boolean isCurrentlyGeolocSharingUniDirectional() {
		synchronized (getImsServiceSessionOperationLock()) {
			return mGeolocTransferSessionCache.size() >= SharingDirection.UNIDIRECTIONAL;
		}
	}

	private boolean isCurrentlyGeolocSharingBiDirectional() {
		synchronized (getImsServiceSessionOperationLock()) {
			return mGeolocTransferSessionCache.size() >= SharingDirection.BIDIRECTIONAL;
		}
	}

	public void addSession(ImageTransferSession session) {
		String sessionId = session.getSessionID();
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Add ImageTransferSession with sessionId '")
					.append(sessionId).append("'").toString());
		}
		synchronized (getImsServiceSessionOperationLock()) {
			mImageTransferSessionCache.put(sessionId, session);
			addImsServiceSession(session);
		}
	}

	public void removeSession(final ImageTransferSession session) {
		final String sessionId = session.getSessionID();
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Remove ImageTransferSession with sessionId '")
					.append(sessionId).append("'").toString());
		}
		/*
		 * Performing remove session operation on a new thread so that ongoing
		 * threads accessing that session can finish up before it is actually
		 * removed
		 */
		new Thread() {
			@Override
			public void run() {
				synchronized (getImsServiceSessionOperationLock()) {
					mImageTransferSessionCache.remove(sessionId);
					removeImsServiceSession(session);
				}
			}
		}.start();
	}

	public ImageTransferSession getImageTransferSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Get ImageTransferSession with sessionId '")
					.append(sessionId).append("'").toString());
		}
		synchronized (getImsServiceSessionOperationLock()) {
			return mImageTransferSessionCache.get(sessionId);
		}
	}

	public void addSession(VideoStreamingSession session) {
		String sessionId = session.getSessionID();
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Add VideoStreamingSession with sessionId '")
					.append(sessionId).append("'").toString());
		}
		synchronized (getImsServiceSessionOperationLock()) {
			mVideoStremaingSessionCache.put(sessionId, session);
			addImsServiceSession(session);
		}
	}

	public void removeSession(final VideoStreamingSession session) {
		final String sessionId = session.getSessionID();
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Remove VideoStreamingSession with sessionId '")
					.append(sessionId).append("'").toString());
		}
		/*
		 * Performing remove session operation on a new thread so that ongoing
		 * threads accessing that session can finish up before it is actually
		 * removed
		 */
		new Thread() {
			@Override
			public void run() {
				synchronized (getImsServiceSessionOperationLock()) {
					mVideoStremaingSessionCache.remove(sessionId);
					removeImsServiceSession(session);
				}
			}
		}.start();
	}

	public VideoStreamingSession getVideoSharingSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Get VideoStreamingSession with sessionId '")
					.append(sessionId).append("'").toString());
		}
		synchronized (getImsServiceSessionOperationLock()) {
			return mVideoStremaingSessionCache.get(sessionId);
		}
	}

	public void addSession(GeolocTransferSession session) {
		String sessionId = session.getSessionID();
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Add GeolocTransferSession with sessionId '")
					.append(sessionId).append("'").toString());
		}
		synchronized (getImsServiceSessionOperationLock()) {
			mGeolocTransferSessionCache.put(sessionId, session);
			addImsServiceSession(session);
		}
	}

	public void removeSession(final GeolocTransferSession session) {
		final String sessionId = session.getSessionID();
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Remove GeolocTransferSession with sessionId '")
					.append(sessionId).append("'").toString());
		}
		/*
		 * Performing remove session operation on a new thread so that ongoing
		 * threads accessing that session can finish up before it is actually
		 * removed
		 */
		new Thread() {
			@Override
			public void run() {
				synchronized (getImsServiceSessionOperationLock()) {
					mGeolocTransferSessionCache.remove(sessionId);
					removeImsServiceSession(session);
				}
			}
		}.start();
	}

	public GeolocTransferSession getGeolocTransferSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Get GeolocTransferSession with sessionId '")
					.append(sessionId).append("'").toString());
		}
		synchronized (getImsServiceSessionOperationLock()) {
			return mGeolocTransferSessionCache.get(sessionId);
		}
	}

	/**
     * Is call connected with a given contact
     *
     * @param contact Contact Id
     * @return Boolean
     */
	public boolean isCallConnectedWith(ContactId contact) {
		boolean csCall = (getImsModule() != null) &&
				(getImsModule().getCallManager() != null) &&
					getImsModule().getCallManager().isCallConnectedWith(contact);
		boolean ipCall = (getImsModule() != null) &&
				(getImsModule().getIPCallService() != null) &&
					getImsModule().getIPCallService().isCallConnectedWith(contact);
		return (csCall || ipCall);
	}

    /**
     * Initiate an image sharing session
     *
     * @param contact Remote contact identifier
     * @param content The file content to share
     * @param thumbnail The thumbnail content
     * @return CSh session
     * @throws CoreException
     */
	public ImageTransferSession initiateImageSharingSession(ContactId contact, MmContent content, MmContent thumbnail)
			throws CoreException {
		if (logger.isActivated()) {
			logger.info("Initiate image sharing session with contact " + contact + ", file " + content.toString());
		}

		// Test if call is established
		if (!isCallConnectedWith(contact)) {
			if (logger.isActivated()) {
				logger.debug("Rich call not established: cancel the initiation");
			}
			/*
			 * TODO : Proper exception handling will be added here as part of
			 * the CR037 implementation
			 */
            throw new CoreException("Call not established");
        }

		assertMaximumImageTransferSize(content.getSize(), "File exceeds max size.");

        // Reject if there are already 2 bidirectional sessions with a given contact
		boolean rejectInvitation = false;
        if (isCurrentlyImageSharingBiDirectional()) {
        	// Already a bidirectional session
            if (logger.isActivated()) {
                logger.debug("Max sessions reached");
            }
        	rejectInvitation = true;
        } else
        if (isCurrentlyImageSharingUniDirectional()) {
			ImageTransferSession currentSession = getUnidirectionalImageSharingSession();
        	if (isSessionOriginating(currentSession)){
        		// Originating session already used
				if (logger.isActivated()) {
				    logger.debug("Max originating sessions reached");
				}
            	rejectInvitation = true;
        	} else
        	if (contact == null || !contact.equals(currentSession.getRemoteContact())) {
        		// Not the same contact
				if (logger.isActivated()) {
				    logger.debug("Only bidirectional session with same contact authorized");
				}
            	rejectInvitation = true;
        	}
        }
        if (rejectInvitation) {
            if (logger.isActivated()) {
                logger.debug("The max number of sharing sessions is achieved: cancel the initiation");
            }
            throw new CoreException("Max content sharing sessions achieved");
        }

		// Create a new session
		OriginatingImageTransferSession session = new OriginatingImageTransferSession(this, content,
				contact, thumbnail);

		return session;
	}

    /**
     * Receive an image sharing invitation
     *
     * @param invite Initial invite
     */
	public void receiveImageSharingInvitation(SipRequest invite) {
		if (logger.isActivated()) {
    		logger.info("Receive an image sharing session invitation");
    	}
        // Reject if there are already 2 bidirectional sessions with a given contact
		boolean rejectInvitation = false;
        ContactId contact = null;
		try {
			contact = ContactUtils.createContactId(SipUtils.getAssertedIdentity(invite));
		} catch (RcsContactFormatException e) {
			if (logger.isActivated()) {
				logger.debug("Rich call not established: cannot parse contact");
			}
			rejectInvitation = true;
		}

        // Test if call is established
		if (!isCallConnectedWith(contact)) {
			if (logger.isActivated()) {
				logger.debug("Rich call not established: reject the invitation");
			}
			sendErrorResponse(invite, 606);
			return;
		}

        if (isCurrentlyImageSharingBiDirectional()) {
        	// Already a bidirectional session
            if (logger.isActivated()) {
                logger.debug("Max sessions reached");
            }
        	rejectInvitation = true;
        	handleImageSharingInvitationRejected(invite, ImageSharing.ReasonCode.REJECTED_MAX_SHARING_SESSIONS);
        } else
        	if (isCurrentlyImageSharingUniDirectional()) {
        		ImageTransferSession currentSession = getUnidirectionalImageSharingSession();
        	if (isSessionTerminating(currentSession)) {
        		// Terminating session already used
				if (logger.isActivated()) {
				    logger.debug("Max terminating sessions reached");
				}
            	rejectInvitation = true;
        	} else
        	if (contact == null || !contact.equals(currentSession.getRemoteContact())) {
        		// Not the same contact
				if (logger.isActivated()) {
				    logger.debug("Only bidirectional session with same contact authorized");
				}
            	rejectInvitation = true;
            	handleImageSharingInvitationRejected(invite, ImageSharing.ReasonCode.REJECTED_MAX_SHARING_SESSIONS);
        	}
        }
        if (rejectInvitation) {
            if (logger.isActivated()) {
                logger.debug("Reject the invitation");
            }
            sendErrorResponse(invite, 486);
            return;
        }

		// Create a new session
    	ImageTransferSession session = new TerminatingImageTransferSession(this, invite, contact);

		getImsModule().getCore().getListener().handleContentSharingTransferInvitation(session);

		session.startSession();
	}

    /**
     * Initiate a live video sharing session
     *
     * @param contact Remote contact Id
     * @param content Video content to share
     * @param player Media player
     * @return CSh session
     * @throws CoreException
     */
    public VideoStreamingSession initiateLiveVideoSharingSession(ContactId contact, IVideoPlayer player) throws CoreException {
		if (logger.isActivated()) {
			logger.info("Initiate a live video sharing session");
		}

		// Test if call is established
		if (!isCallConnectedWith(contact)) {
			if (logger.isActivated()) {
				logger.debug("Rich call not established: cancel the initiation");
			}
			/*
			 * TODO : Proper exception handling will be added here as part of
			 * the CR037 implementation
			 */
            throw new CoreException("Call not established");
        }

        // Reject if there are already 2 bidirectional sessions with a given contact
		boolean rejectInvitation = false;
        if (isCurrentlyVideoSharingBiDirectional()) {
        	// Already a bidirectional session
            if (logger.isActivated()) {
                logger.debug("Max sessions reached");
            }
        	rejectInvitation = true;
        } else
        if (isCurrentlyVideoSharingUniDirectional()) {
			VideoStreamingSession currentSession = getUnidirectionalVideoSharingSession();
        	if (isSessionOriginating(currentSession)) {
        		// Originating session already used
				if (logger.isActivated()) {
				    logger.debug("Max originating sessions reached");
				}
            	rejectInvitation = true;
        	} else
        	if (contact == null || !contact.equals(currentSession.getRemoteContact())) {
        		// Not the same contact
				if (logger.isActivated()) {
				    logger.debug("Only bidirectional session with same contact authorized");
				}
            	rejectInvitation = true;
        	}
        }
        if (rejectInvitation) {
            if (logger.isActivated()) {
                logger.debug("The max number of sharing sessions is achieved: cancel the initiation");
            }
			/*
			 * TODO : Proper exception handling will be added here as part of
			 * the CR037 implementation
			 */
            throw new CoreException("Max content sharing sessions achieved");
        }

		// Create a new session
		OriginatingVideoStreamingSession session = new OriginatingVideoStreamingSession(
				this,
				player,
                ContentManager.createGenericLiveVideoContent(),
				contact);

		return session;
	}

    /**
     * Receive a video sharing invitation
     *
     * @param invite Initial invite
     */
	public void receiveVideoSharingInvitation(SipRequest invite) {
		if (logger.isActivated()) {
    		logger.info("Receive a video sharing invitation");
    	}
        // Reject if there are already 2 bidirectional sessions with a given contact
		boolean rejectInvitation = false;
        ContactId contact = null;
		try {
			contact = ContactUtils.createContactId(SipUtils.getAssertedIdentity(invite));
		} catch (RcsContactFormatException e) {
			if (logger.isActivated()) {
				logger.debug("Rich call not established: cannot parse contact");
			}
			rejectInvitation = true;
			/*TODO: Skip catching exception, which should be implemented in CR037.*/
		}

        // Test if call is established
		if (!isCallConnectedWith(contact)) {
			if (logger.isActivated()) {
				logger.debug("Rich call not established: reject the invitation");
			}
			sendErrorResponse(invite, 606);
			return;
		}

        if (isCurrentlyVideoSharingBiDirectional()) {
        	// Already a bidirectional session
            if (logger.isActivated()) {
                logger.debug("Max sessions reached");
            }
        	rejectInvitation = true;
        	handleVideoSharingInvitationRejected(invite, VideoSharing.ReasonCode.REJECTED_MAX_SHARING_SESSIONS);
        } else
        	if (isCurrentlyVideoSharingUniDirectional()) {
        		VideoStreamingSession currentSession = getUnidirectionalVideoSharingSession();
			if (isSessionTerminating(currentSession)) {
        		// Terminating session already used
				if (logger.isActivated()) {
				    logger.debug("Max terminating sessions reached");
				}
            	rejectInvitation = true;
            	handleVideoSharingInvitationRejected(invite, VideoSharing.ReasonCode.REJECTED_MAX_SHARING_SESSIONS);
        	} else
        	if (contact == null || !contact.equals(currentSession.getRemoteContact())) {
        		// Not the same contact
				if (logger.isActivated()) {
				    logger.debug("Only bidirectional session with same contact authorized");
				}
            	rejectInvitation = true;
            	handleVideoSharingInvitationRejected(invite, VideoSharing.ReasonCode.REJECTED_MAX_SHARING_SESSIONS);
        	}
        }
        if (rejectInvitation) {
            if (logger.isActivated()) {
                logger.debug("Reject the invitation");
            }
            sendErrorResponse(invite, 486);
            return;
        }

		// Create a new session
		VideoStreamingSession session = new TerminatingVideoStreamingSession(this, invite, contact);

		getImsModule().getCore().getListener().handleContentSharingStreamingInvitation(session);

		session.startSession();
	}

    /**
     * Initiate a geoloc sharing session
     *
     * @param contact Remote contact
     * @param content Content to be shared
     * @param geoloc Geolocation
     * @return GeolocTransferSession
     * @throws CoreException
     */
	public GeolocTransferSession initiateGeolocSharingSession(ContactId contact, MmContent content, Geoloc geoloc)
			throws CoreException {
		if (logger.isActivated()) {
			logger.info(new StringBuilder("Initiate geoloc sharing session with contact ")
					.append(contact).append(".").toString());
		}
		if (!isCallConnectedWith(contact)) {
			if (logger.isActivated()) {
				logger.debug("Rich call not established: cancel the initiation.");
			}
			/*
			 * TODO : Proper exception handling will be added here as part of
			 * the CR037 implementation
			 */
			throw new CoreException("Call not established");
		}

		// Create a new session
		OriginatingGeolocTransferSession session = new OriginatingGeolocTransferSession(this, content, contact, geoloc);

		return session;
	}

	/**
	 * Receive a geoloc sharing invitation
	 *
	 * @param invite Initial invite
	 */
	public void receiveGeolocSharingInvitation(SipRequest invite) {
		if (logger.isActivated()) {
			logger.info("Receive a geoloc sharing session invitation");
		}

		ContactId contact = null;
		boolean rejectInvitation = false;
		try {
			contact = ContactUtils.createContactId(SipUtils.getAssertedIdentity(invite));
			// Test if call is established
			if (!isCallConnectedWith(contact)) {
				if (logger.isActivated()) {
					logger.debug("Rich call not established: reject the invitation");
				}
				sendErrorResponse(invite, 606);
				return;
			}
		} catch (RcsContactFormatException e) {
			if (logger.isActivated()) {
				logger.debug("Rich call not established: cannot parse contact");
			}
			rejectInvitation = true;
			return;
		}

		if (isCurrentlyGeolocSharingBiDirectional()) {
			// Already a bidirectional session
			if (logger.isActivated()) {
				logger.debug("Max sessions reached");
			}
			handleGeolocSharingInvitationRejected(invite,
					GeolocSharing.ReasonCode.REJECTED_MAX_SHARING_SESSIONS);
			rejectInvitation = true;
		} else if (isCurrentlyGeolocSharingUniDirectional()) {
    		GeolocTransferSession currentSession = getUnidirectionalGeolocSharingSession();
			if (isSessionTerminating(currentSession)) {
				// Terminating session already used
				if (logger.isActivated()) {
					logger.debug("Max terminating sessions reached");
				}
				handleGeolocSharingInvitationRejected(invite,
						GeolocSharing.ReasonCode.REJECTED_MAX_SHARING_SESSIONS);
				rejectInvitation = true;
			} else if (contact == null || !contact.equals(currentSession.getRemoteContact())) {
				// Not the same contact
				if (logger.isActivated()) {
					logger.debug("Only bidirectional session with same contact authorized");
				}
				handleGeolocSharingInvitationRejected(invite,
						GeolocSharing.ReasonCode.REJECTED_MAX_SHARING_SESSIONS);
				rejectInvitation = true;
			}
		}
		if (rejectInvitation) {
			if (logger.isActivated()) {
				logger.debug("Reject the invitation");
			}
			sendErrorResponse(invite, 486);
			return;
		}

		// Create a new session
		GeolocTransferSession session = new TerminatingGeolocTransferSession(this, invite, contact);

		getImsModule().getCore().getListener().handleContentSharingTransferInvitation(session);

		session.startSession();
	}

	/**
	 * Abort all pending sessions
	 */
	public void abortAllSessions() {
		if (logger.isActivated()) {
			logger.debug("Abort all pending sessions");
		}
		abortAllSessions(ImsServiceSession.TERMINATION_BY_SYSTEM);
    }

	/**
	 * Is the current session an originating one
	 *
	 * @param session
	 * @return true if session is an originating content sharing session (image or video)
	 */
	private boolean isSessionOriginating(ContentSharingSession session){
		return (session instanceof OriginatingImageTransferSession
				|| session instanceof OriginatingVideoStreamingSession);
	}

	/**
	 * Is the current session a terminating one
	 *
	 * @param session
	 * @return true if session is an terminating content sharing session (image or video)
	 */
	private boolean isSessionTerminating(ContentSharingSession session){
		return (session instanceof TerminatingImageTransferSession
				|| session instanceof TerminatingVideoStreamingSession);
	}
}
