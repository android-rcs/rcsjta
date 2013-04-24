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

import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.SessionTimerManager;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.RichcallService;
import com.orangelabs.rcs.service.api.client.media.video.VideoCodec;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Terminating live video content sharing session (streaming)
 *
 * @author jexa7410
 */
public class TerminatingVideoStreamingSession extends VideoStreamingSession {
    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     *
     * @param parent IMS service
     * @param invite Initial INVITE request
     */
    public TerminatingVideoStreamingSession(ImsService parent, SipRequest invite) {
        super(parent, ContentManager.createLiveVideoContentFromSdp(invite.getContentBytes()), SipUtils.getAssertedIdentity(invite));

        // Create dialog path
        createTerminatingDialogPath(invite);
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (logger.isActivated()) {
                logger.info("Initiate a new live video sharing session as terminating");
            }

            // Send a 180 Ringing response
            send180Ringing(getDialogPath().getInvite(), getDialogPath().getLocalTag());

            // Parse the remote SDP part
            SdpParser parser = new SdpParser(getDialogPath().getRemoteContent().getBytes());
            String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription.connectionInfo);
            MediaDescription mediaVideo = parser.getMediaDescription("video");
            int remotePort = mediaVideo.port;

            // Extract video codecs from SDP
            Vector<MediaDescription> medias = parser.getMediaDescriptions("video");
            Vector<VideoCodec> proposedCodecs = VideoCodecManager.extractVideoCodecsFromSdp(medias);

            // Notify listener
            getImsService().getImsModule().getCore().getListener().handleContentSharingStreamingInvitation(this);

            // Wait invitation answer
            int answer = waitInvitationAnswer();
            if (answer == ImsServiceSession.INVITATION_REJECTED) {
                if (logger.isActivated()) {
                    logger.debug("Session has been rejected by user");
                }

                // Remove the current session
                getImsService().removeSession(this);

                // Notify listeners
                for (int i = 0; i < getListeners().size(); i++) {
                    getListeners().get(i).handleSessionAborted(ImsServiceSession.TERMINATION_BY_USER);
                }
                return;
            } else
            if (answer == ImsServiceSession.INVITATION_NOT_ANSWERED) {
                if (logger.isActivated()) {
                    logger.debug("Session has been rejected on timeout");
                }

                // Ringing period timeout
                send486Busy(getDialogPath().getInvite(), getDialogPath().getLocalTag());

                // Remove the current session
                getImsService().removeSession(this);

                // Notify listeners
                for (int i = 0; i < getListeners().size(); i++) {
                    getListeners().get(i).handleSessionAborted(ImsServiceSession.TERMINATION_BY_TIMEOUT);
                }
                return;
            } else
            if (answer == ImsServiceSession.INVITATION_CANCELED) {
                if (logger.isActivated()) {
                    logger.debug("Session has been canceled");
                }
                return;
            }

            // Check that a media renderer has been set
            if (getMediaRenderer() == null) {
                handleError(new ContentSharingError(
                        ContentSharingError.MEDIA_RENDERER_NOT_INITIALIZED));
                return;
            }

            // Codec negotiation
            VideoCodec selectedVideoCodec = VideoCodecManager.negociateVideoCodec(
                    getMediaRenderer().getSupportedMediaCodecs(), proposedCodecs);
            if (selectedVideoCodec == null) {
                if (logger.isActivated()){
                    logger.debug("Proposed codecs are not supported");
                }
                
                // Send a 415 Unsupported media type response
                send415Error(getDialogPath().getInvite());
                
                // Unsupported media type
                handleError(new ContentSharingError(ContentSharingError.UNSUPPORTED_MEDIA_TYPE));
                return;
            }

            // Set the OrientationHeaderID
            SdpOrientationExtension extensionHeader = SdpOrientationExtension.create(mediaVideo);
            if (extensionHeader != null) {
                getMediaRenderer().setOrientationHeaderId(extensionHeader.getExtensionId());
            }

            // Set the media codec in media renderer
            getMediaRenderer().setMediaCodec(selectedVideoCodec.getMediaCodec());

            // Set media renderer event listener
            getMediaRenderer().addListener(new MediaPlayerEventListener(this));

            // Open the media renderer
            getMediaRenderer().open(remoteHost, remotePort);

            // Build SDP part
            String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
	    	String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
            String videoSdp = VideoSdpBuilder.buildResponseSdp(selectedVideoCodec.getMediaCodec(), getMediaRenderer().getLocalRtpPort(), mediaVideo); 
            String sdp =
            	"v=0" + SipUtils.CRLF +
            	"o=- " + ntpTime + " " + ntpTime + " " + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
            	"s=-" + SipUtils.CRLF +
            	"c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
                "t=0 0" + SipUtils.CRLF +
                videoSdp +
                "a=recvonly" + SipUtils.CRLF;

            // Set the local SDP part in the dialog path
            getDialogPath().setLocalContent(sdp);

            // Create a 200 OK response
            if (logger.isActivated()) {
                logger.info("Send 200 OK");
            }
            SipResponse resp = SipMessageFactory.create200OkInviteResponse(getDialogPath(),
                    RichcallService.FEATURE_TAGS_VIDEO_SHARE, sdp);

            // The signalisation is established
            getDialogPath().sigEstablished();

            // Send response
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager().sendSipMessageAndWait(resp);

            // Analyze the received response
            if (ctx.isSipAck()) {
                // ACK received
                if (logger.isActivated()) {
                    logger.info("ACK request received");
                }

                // The session is established
                getDialogPath().sessionEstablished();

                // Start the media renderer
                getMediaRenderer().start();

                // Start session timer
                if (getSessionTimerManager().isSessionTimerActivated(resp)) {
                    getSessionTimerManager().start(SessionTimerManager.UAS_ROLE, getDialogPath().getSessionExpireTime());
                }

                // Notify listeners
                for(int i=0; i < getListeners().size(); i++) {
                    getListeners().get(i).handleSessionStarted();
                }
            } else {
                if (logger.isActivated()) {
                    logger.debug("No ACK received for INVITE");
                }

                // No response received: timeout
                handleError(new ContentSharingError(ContentSharingError.SESSION_INITIATION_FAILED));
            }
        } catch(Exception e) {
            if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }

            // Unexpected error
            handleError(new ContentSharingError(ContentSharingError.UNEXPECTED_EXCEPTION,
                    e.getMessage()));
        }
    }

    /**
     * Handle error
     *
     * @param error Error
     */
    public void handleError(ContentSharingError error) {
        // Error
        if (logger.isActivated()) {
            logger.info("Session error: " + error.getErrorCode() + ", reason=" + error.getMessage());
        }

        // Close media session
        closeMediaSession();

        // Remove the current session
        getImsService().removeSession(this);

        // Notify listener
        if (!isInterrupted()) {
            for(int i=0; i < getListeners().size(); i++) {
                ((VideoStreamingSessionListener)getListeners().get(i)).handleSharingError(error);
            }
        }
    }

    /**
     * Close media session
     */
    public void closeMediaSession() {
        try {
            // Close the media renderer
            if (getMediaRenderer() != null) {
                getMediaRenderer().stop();
                getMediaRenderer().close();
            }
        } catch(Exception e) {
            if (logger.isActivated()) {
                logger.error("Exception when closing the media renderer", e);
            }
        }
    }

    /**
     * Prepare media session
     * 
     * @throws Exception 
     */
    public void prepareMediaSession() throws Exception {
        // Nothing to do in terminating side
    }

    /**
     * Start media session
     * 
     * @throws Exception 
     */
    public void startMediaSession() throws Exception {
        // Nothing to do in terminating side
    }
}

