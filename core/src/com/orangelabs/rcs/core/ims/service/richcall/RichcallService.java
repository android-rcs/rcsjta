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

package com.orangelabs.rcs.core.ims.service.richcall;

import java.util.Enumeration;
import java.util.Vector;

import org.gsma.joyn.vsh.IVideoPlayer;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.FeatureTags;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.service.ContactInfo;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.capability.Capabilities;
import com.orangelabs.rcs.core.ims.service.capability.CapabilityUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.GeolocTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.OriginatingGeolocTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.TerminatingGeolocTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.image.OriginatingImageTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.image.TerminatingImageTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.video.OriginatingVideoStreamingSession;
import com.orangelabs.rcs.core.ims.service.richcall.video.TerminatingVideoStreamingSession;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

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
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     *
     * @param parent IMS module
     * @throws CoreException
     */
	public RichcallService(ImsModule parent) throws CoreException {
        super(parent, true);
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

    /**
     * Returns CSh sessions
     *
     * @return List of sessions
     */
    public Vector<ContentSharingSession> getCShSessions() {
        Vector<ContentSharingSession> result = new Vector<ContentSharingSession>();
        Enumeration<ImsServiceSession> list = getSessions();
        while (list.hasMoreElements()) {
            ImsServiceSession session = list.nextElement();
            result.add((ContentSharingSession) session);
        }
        return result;
    }

    /**
     * Returns CSh sessions with a contact
     *
     * @param Contact
     * @return List of sessions
     */
    public Vector<ContentSharingSession> getCShSessions(String contact) {
        Vector<ContentSharingSession> result = new Vector<ContentSharingSession>();
        Enumeration<ImsServiceSession> list = getSessions();
        while (list.hasMoreElements()) {
            ImsServiceSession session = list.nextElement();
			if (PhoneUtils.compareNumbers(session.getRemoteContact(), contact)) {
				result.add((ContentSharingSession) session);
			}
        }
        return result;
    }    
    
    /**
     * Initiate an image sharing session
     *
     * @param contact Remote contact
     * @param content Content to be shared
     * @param thumbnail Thumbnail option
     * @return CSh session
     * @throws CoreException
     */
	public ImageTransferSession initiateImageSharingSession(String contact, MmContent content, boolean thumbnail) throws CoreException {
		if (logger.isActivated()) {
			logger.info("Initiate image sharing session with contact " + contact + ", file " + content.toString());
		}

		// Test if call is established
		if (!getImsModule().getCallManager().isCallConnected()) {
			if (logger.isActivated()) {
				logger.debug("Rich call not established: cancel the initiation");
			}
            throw new CoreException("Call not established");
        }

        // Test max size
        int maxSize = ImageTransferSession.getMaxImageSharingSize();
        if (maxSize > 0 && content.getSize() > maxSize) {
            if (logger.isActivated()) {
                logger.debug("File exceeds max size: cancel the initiation");
            }
            throw new CoreException("File exceeds max size");
        }

        // Reject if there are already 2 bidirectional sessions with a given contact
		boolean rejectInvitation = false;
        Vector<ContentSharingSession> currentSessions = getCShSessions();
        if (currentSessions.size() >= 2) {
        	// Already a bidirectional session
            if (logger.isActivated()) {
                logger.debug("Max sessions reached");
            }
        	rejectInvitation = true;
        } else
        if (currentSessions.size() == 1) {
        	ContentSharingSession currentSession = currentSessions.elementAt(0);
        	if (!(currentSession instanceof TerminatingImageTransferSession)) {
        		// Originating session already used
				if (logger.isActivated()) {
				    logger.debug("Max originating sessions reached");
				}
            	rejectInvitation = true;
        	} else
        	if (!PhoneUtils.compareNumbers(contact, currentSession.getRemoteContact())) {
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

        // Create the thumbnail
        byte[] thumbnailImage = null;
        if (thumbnail && content.getEncoding().startsWith("image/")) {
        	thumbnailImage = ChatUtils.createFileThumbnail(content.getUrl());
        }             
        
		// Create a new session
		OriginatingImageTransferSession session = new OriginatingImageTransferSession(
				this,
				content,
				PhoneUtils.formatNumberToSipUri(contact),
				thumbnailImage);

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

		// Test if call is established
		if (!getImsModule().getCallManager().isCallConnected()) {
			if (logger.isActivated()) {
				logger.debug("Rich call not established: reject the invitation");
			}
			sendErrorResponse(invite, 606);
			return;
		}

        // Reject if there are already 2 bidirectional sessions with a given contact
		boolean rejectInvitation = false;
        String contact = SipUtils.getAssertedIdentity(invite);
        Vector<ContentSharingSession> currentSessions = getCShSessions();
        if (currentSessions.size() >= 2) {
        	// Already a bidirectional session
            if (logger.isActivated()) {
                logger.debug("Max sessions reached");
            }
        	rejectInvitation = true;
        } else
        if (currentSessions.size() == 1) {
        	ContentSharingSession currentSession = currentSessions.elementAt(0);
        	if (currentSession instanceof TerminatingImageTransferSession) {
        		// Terminating session already used
				if (logger.isActivated()) {
				    logger.debug("Max terminating sessions reached");
				}
            	rejectInvitation = true;
        	} else
        	if (!PhoneUtils.compareNumbers(contact, currentSession.getRemoteContact())) {
        		// Not the same contact
				if (logger.isActivated()) {
				    logger.debug("Only bidirectional session with same contact authorized");
				}
            	rejectInvitation = true;
        	}
        }
        if (rejectInvitation) {
            if (logger.isActivated()) {
                logger.debug("The max number of sharing sessions is achieved: reject the invitation");
            }
            sendErrorResponse(invite, 486);
            return;
        }

		// Create a new session
    	ImageTransferSession session = new TerminatingImageTransferSession(this, invite);

        // Auto reject if file too big
        int maxSize = ImageTransferSession.getMaxImageSharingSize();
        if (maxSize > 0 && session.getContent().getSize() > maxSize) {
            if (logger.isActivated()) {
                logger.debug("Auto reject image sharing invitation");
            }

            // Decline the invitation
            session.sendErrorResponse(invite, session.getDialogPath().getLocalTag(), 603);

            // File too big
            session.handleError(new ContentSharingError(ContentSharingError.MEDIA_SIZE_TOO_BIG));
            return;
        }

		// Start the session
		session.startSession();

		// Notify listener
		getImsModule().getCore().getListener().handleContentSharingTransferInvitation(session);
	}

    /**
     * Initiate a live video sharing session
     *
     * @param contact Remote contact
     * @param content Video content to share
     * @param player Media player
     * @return CSh session
     * @throws CoreException
     */
    public VideoStreamingSession initiateLiveVideoSharingSession(String contact, IVideoPlayer player) throws CoreException {
		if (logger.isActivated()) {
			logger.info("Initiate a live video sharing session");
		}

		// Test if call is established
		if (!getImsModule().getCallManager().isCallConnected()) {
			if (logger.isActivated()) {
				logger.debug("Rich call not established: cancel the initiation");
			}
            throw new CoreException("Call not established");
        }

        // Reject if there are already 2 bidirectional sessions with a given contact
		boolean rejectInvitation = false;
        Vector<ContentSharingSession> currentSessions = getCShSessions();
        if (currentSessions.size() >= 2) {
        	// Already a bidirectional session
            if (logger.isActivated()) {
                logger.debug("Max sessions reached");
            }
        	rejectInvitation = true;
        } else
        if (currentSessions.size() == 1) {
        	ContentSharingSession currentSession = currentSessions.elementAt(0);
        	if (!(currentSession instanceof TerminatingVideoStreamingSession)) {
        		// Originating session already used
				if (logger.isActivated()) {
				    logger.debug("Max originating sessions reached");
				}
            	rejectInvitation = true;
        	} else
        	if (!PhoneUtils.compareNumbers(contact, currentSession.getRemoteContact())) {
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
        OriginatingVideoStreamingSession session = new OriginatingVideoStreamingSession(
				this,
				player,
                ContentManager.createGenericLiveVideoContent(),
				PhoneUtils.formatNumberToSipUri(contact));

		// Start the session
		session.startSession();
		return session;
	}

    /**
     * Receive a video sharing invitation
     *
     * @param invite Initial invite
     */
	public void receiveVideoSharingInvitation(SipRequest invite) {
		// Test if call is established
		if (!getImsModule().getCallManager().isCallConnected()) {
			if (logger.isActivated()) {
				logger.debug("Rich call not established: reject the invitation");
			}
			sendErrorResponse(invite, 606);
			return;
		}

        // Reject if there are already 2 bidirectional sessions with a given contact
		boolean rejectInvitation = false;
        String contact = SipUtils.getAssertedIdentity(invite);
        Vector<ContentSharingSession> currentSessions = getCShSessions();
        if (currentSessions.size() >= 2) {
        	// Already a bidirectional session
            if (logger.isActivated()) {
                logger.debug("Max sessions reached");
            }
        	rejectInvitation = true;
        } else
        if (currentSessions.size() == 1) {
        	ContentSharingSession currentSession = currentSessions.elementAt(0);
			if (currentSession instanceof TerminatingVideoStreamingSession) {
        		// Terminating session already used
				if (logger.isActivated()) {
				    logger.debug("Max terminating sessions reached");
				}
            	rejectInvitation = true;
        	} else
        	if (!PhoneUtils.compareNumbers(contact, currentSession.getRemoteContact())) {
        		// Not the same contact
				if (logger.isActivated()) {
				    logger.debug("Only bidirectional session with same contact authorized");
				}
            	rejectInvitation = true;
        	}
        }
        if (rejectInvitation) {
            if (logger.isActivated()) {
                logger.debug("The max number of sharing sessions is achieved: reject the invitation");
            }
            sendErrorResponse(invite, 486);
            return;
        }

		// Create a new session
		VideoStreamingSession session = new TerminatingVideoStreamingSession(this, invite);

		// Start the session
		session.startSession();
	}

    /**
     * Initiate a geoloc sharing session
     *
     * @param contact Remote contact
     * @param content Content to be shared
     * @param geoloc Geoloc info
     * @return CSh session
     * @throws CoreException
     */
	public GeolocTransferSession initiateGeolocSharingSession(String contact, MmContent content, GeolocPush geoloc) throws CoreException {
		if (logger.isActivated()) {
			logger.info("Initiate geoloc sharing session with contact " + contact);
		}

		// Test if call is established
		if (!getImsModule().getCallManager().isCallConnected()) {
			if (logger.isActivated()) {
				logger.debug("Rich call not established: cancel the initiation");
			}
            throw new CoreException("Call not established");
        }

		// Create a new session
		OriginatingGeolocTransferSession session = new OriginatingGeolocTransferSession(
				this,
				content,
				PhoneUtils.formatNumberToSipUri(contact),
				geoloc);

		// Start the session
		session.startSession();
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

		// Test if call is established
		if (!getImsModule().getCallManager().isCallConnected()) {
			if (logger.isActivated()) {
				logger.debug("Rich call not established: reject the invitation");
			}
			sendErrorResponse(invite, 606);
			return;
		}

		// Create a new session
    	GeolocTransferSession session = new TerminatingGeolocTransferSession(this, invite);

		// Start the session
		session.startSession();

		// Notify listener
		getImsModule().getCore().getListener().handleContentSharingTransferInvitation(session);
	}
	
	/**
     * Receive a capability request (options procedure)
     *
     * @param options Received options message
     */
    public void receiveCapabilityRequest(SipRequest options) {
    	String contact = SipUtils.getAssertedIdentity(options);

    	if (logger.isActivated()) {
			logger.debug("OPTIONS request received during a call from " + contact);
		}

	    try {
	    	// Create 200 OK response
	    	String ipAddress = getImsModule().getCurrentNetworkInterface().getNetworkAccess().getIpAddress();
			boolean richcall = getImsModule().getCallManager().isRichcallSupportedWith(contact);
	        SipResponse resp = SipMessageFactory.create200OkOptionsResponse(options,
	        		getImsModule().getSipManager().getSipStack().getContact(),
	        		CapabilityUtils.getSupportedFeatureTags(richcall),
	        		CapabilityUtils.buildSdp(ipAddress, richcall));

	        // Send 200 OK response
	        getImsModule().getSipManager().sendSipResponse(resp);
	    } catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Can't send 200 OK for OPTIONS", e);
        	}
	    }

		// Extract capabilities from the request
    	Capabilities capabilities = CapabilityUtils.extractCapabilities(options);
    	if (capabilities.isImSessionSupported()) {
    		// The contact is RCS capable
   			ContactsManager.getInstance().setContactCapabilities(contact, capabilities, ContactInfo.RCS_CAPABLE, ContactInfo.REGISTRATION_STATUS_ONLINE);
    	} else {
    		// The contact is not RCS
    		ContactsManager.getInstance().setContactCapabilities(contact, capabilities, ContactInfo.NOT_RCS, ContactInfo.REGISTRATION_STATUS_UNKNOWN);
    	}

    	// Notify listener
    	getImsModule().getCore().getListener().handleCapabilitiesNotification(contact, capabilities);
    }

	/**
	 * Abort all pending sessions
	 */
	public void abortAllSessions() {
		if (logger.isActivated()) {
			logger.debug("Abort all pending sessions");
		}
		for (Enumeration<ImsServiceSession> e = getSessions(); e.hasMoreElements() ;) {
			ImsServiceSession session = (ImsServiceSession)e.nextElement();
			if (logger.isActivated()) {
				logger.debug("Abort pending session " + session.getSessionID());
			}
			session.abortSession(ImsServiceSession.TERMINATION_BY_SYSTEM);
		}
    }
}
