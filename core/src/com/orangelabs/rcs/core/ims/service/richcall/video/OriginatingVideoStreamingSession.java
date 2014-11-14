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

package com.orangelabs.rcs.core.ims.service.richcall.video;

import java.util.Vector;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.vsh.IVideoPlayer;
import com.gsma.services.rcs.vsh.IVideoPlayerListener;
import com.gsma.services.rcs.vsh.VideoCodec;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.ImsSessionListener;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.RichcallService;
import com.orangelabs.rcs.utils.ContactUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Originating video content sharing session (streaming)
 *
 * @author hlxn7157
 */
public class OriginatingVideoStreamingSession extends VideoStreamingSession {
    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(OriginatingVideoStreamingSession.class.getSimpleName());

    /**
     * Constructor
     *
     * @param parent IMS service
     * @param player Media player
     * @param content Content to be shared
     * @param contact Remote contact Id
     */
    public OriginatingVideoStreamingSession(ImsService parent, IVideoPlayer player,
            MmContent content, ContactId contact) {
        super(parent, content, contact);

        // Create dialog path
        createOriginatingDialogPath();

        // Set the video player
        setVideoPlayer(player);
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (logger.isActivated()) {
                logger.info("Initiate a new live video sharing session as originating");
            }

            // Build SDP part
	    	String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
            String videoSdp = VideoSdpBuilder.buildSdpOfferWithOrientation(getVideoPlayer().getSupportedCodecs(), getVideoPlayer().getLocalRtpPort());
            String sdp = SdpUtils.buildVideoSDP(ipAddress, videoSdp, SdpUtils.DIRECTION_SENDONLY);

            // Set the local SDP part in the dialog path
            getDialogPath().setLocalContent(sdp);

            // Create an INVITE request
            if (logger.isActivated()) {
                logger.info("Send INVITE");
            }
            SipRequest invite = SipMessageFactory.createInvite(getDialogPath(),
                    RichcallService.FEATURE_TAGS_VIDEO_SHARE, sdp);

	        // Set the Authorization header
	        getAuthenticationAgent().setAuthorizationHeader(invite);

	        // Set initial request in the dialog path
            getDialogPath().setInvite(invite);

            // Send INVITE request
            sendInvite(invite);
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }

            // Unexpected error
            handleError(new ContentSharingError(ContentSharingError.UNEXPECTED_EXCEPTION,
                    e.getMessage()));
        }
    }

    /**
     * Prepare media session
     *
     * @throws Exception 
     */
    public void prepareMediaSession() throws Exception {
        // Parse the remote SDP part
        SdpParser parser = new SdpParser(getDialogPath().getRemoteContent().getBytes());
        MediaDescription mediaVideo = parser.getMediaDescription("video");
        String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription, mediaVideo);
        int remotePort = mediaVideo.port;

        // Extract video codecs from SDP
        Vector<MediaDescription> medias = parser.getMediaDescriptions("video");
        Vector<VideoCodec> proposedCodecs = VideoCodecManager.extractVideoCodecsFromSdp(medias);

        // Codec negotiation
        VideoCodec selectedVideoCodec = VideoCodecManager.negociateVideoCodec(
                getVideoPlayer().getSupportedCodecs(), proposedCodecs);
        if (selectedVideoCodec == null) {
            if (logger.isActivated()) {
                logger.debug("Proposed codecs are not supported");
            }
            
            // Terminate session
            terminateSession(ImsServiceSession.TERMINATION_BY_SYSTEM);
            
            // Report error
            handleError(new ContentSharingError(ContentSharingError.UNSUPPORTED_MEDIA_TYPE));
            return;
        }
        getContent().setEncoding("video/" + selectedVideoCodec.getEncoding());

        // Set the OrientationHeaderID
        SdpOrientationExtension extensionHeader = SdpOrientationExtension.create(mediaVideo);
        if (extensionHeader != null) {
        	// TODO getVideoPlayer().setOrientationHeaderId(extensionHeader.getExtensionId());
        }

        // Set video player event listener
        getVideoPlayer().addEventListener(new MyPlayerEventListener(this));

        // Open the video player
        getVideoPlayer().open(selectedVideoCodec, remoteHost, remotePort);
    }

    /**
     * Start media session
     *
     * @throws Exception 
     */
    public void startMediaSession() throws Exception {
        // Start the video player
    	getVideoPlayer().start();
    }


    /**
     * Close media session
     */
    public void closeMediaSession() {
        try {
            // Close the video player
            if (getVideoPlayer() != null) {
            	getVideoPlayer().stop();
            	getVideoPlayer().close();
            }
        } catch(Exception e) {
            if (logger.isActivated()) {
                logger.error("Exception when closing the media renderer", e);
            }
        }
    }


    /**
     * My player event listener
     */
    private class MyPlayerEventListener extends IVideoPlayerListener.Stub {
        /**
         * Streaming session
         */
        private VideoStreamingSession session;

        /**
         * Constructor
         *
         * @param session Streaming session
         */
        public MyPlayerEventListener(VideoStreamingSession session) {
            this.session = session;
        }

    	/**
    	 * Callback called when the player is opened
    	 */
    	public void onPlayerOpened() {
            if (logger.isActivated()) {
                logger.debug("Media player is opened");
            }
    	}

    	/**
    	 * Callback called when the player is started
    	 */
    	public void onPlayerStarted() {
            if (logger.isActivated()) {
                logger.debug("Media player is started");
            }
    	}

    	/**
    	 * Callback called when the player is stopped
    	 */
    	public void onPlayerStopped() {
            if (logger.isActivated()) {
                logger.debug("Media player is stopped");
            }
    	}

    	/**
    	 * Callback called when the player is closed
    	 */
    	public void onPlayerClosed() {
            if (logger.isActivated()) {
                logger.debug("Media player is closed");
            }
    	}

    	/**
    	 * Callback called when the player has failed
    	 * 
    	 * @param error Error
    	 */
    	public void onPlayerError(int error) {
            if (isSessionInterrupted()) {
                return;
            }

            if (logger.isActivated()) {
                logger.error("Media player has failed: " + error);
            }

            // Close the media session
            closeMediaSession();

            // Terminate session
            terminateSession(ImsServiceSession.TERMINATION_BY_SYSTEM);

            // Remove the current session
            getImsService().removeSession(session);

			// Notify listeners
			for (ImsSessionListener listener : getListeners()) {
				((VideoStreamingSessionListener) listener).handleSharingError(new ContentSharingError(
						ContentSharingError.MEDIA_STREAMING_FAILED));
			}

            try {
				ContactId remote = ContactUtils.createContactId(getDialogPath().getRemoteParty());
				// Request capabilities to the remote
		        getImsService().getImsModule().getCapabilityService().requestContactCapabilities(remote);
			} catch (RcsContactFormatException e) {
				if (logger.isActivated()) {
					logger.warn("Cannot parse contact "+getDialogPath().getRemoteParty());
				}
			}
    	}
    }

	@Override
	public boolean isInitiatedByRemote() {
		return false;
	}
	
	@Override
	public void handle180Ringing(SipResponse response) {
		if (logger.isActivated()) {
			logger.debug("handle180Ringing");
		}
		// Notify listeners
		for (ImsSessionListener listener : getListeners()) {
			((VideoStreamingSessionListener)listener).handle180Ringing();
		}
	}
}
