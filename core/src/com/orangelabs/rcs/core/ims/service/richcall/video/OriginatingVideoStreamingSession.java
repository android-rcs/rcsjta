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

package com.orangelabs.rcs.core.ims.service.richcall.video;

import static com.orangelabs.rcs.utils.StringUtils.UTF8;

import java.util.Vector;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.vsh.IVideoPlayer;
import com.gsma.services.rcs.vsh.VideoCodec;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.ImsSessionListener;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.RichcallService;
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
        setPlayer(player);
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (logger.isActivated()) {
                logger.info("Initiate a new live video sharing session as originating");
            }

            SipDialogPath dialogPath = getDialogPath();
            // Build SDP part
	    	String ipAddress = dialogPath.getSipStack().getLocalIpAddress();
	    	IVideoPlayer player = getPlayer();
            String videoSdp = VideoSdpBuilder.buildSdpOfferWithOrientation(player.getSupportedCodecs(),
            		player.getLocalRtpPort());
            String sdp = SdpUtils.buildVideoSDP(ipAddress, videoSdp, SdpUtils.DIRECTION_SENDONLY);

            // Set the local SDP part in the dialog path
            dialogPath.setLocalContent(sdp);

            // Create an INVITE request
            if (logger.isActivated()) {
                logger.info("Send INVITE");
            }
            SipRequest invite = SipMessageFactory.createInvite(dialogPath,
                    RichcallService.FEATURE_TAGS_VIDEO_SHARE, sdp);

	        // Set the Authorization header
	        getAuthenticationAgent().setAuthorizationHeader(invite);

	        // Set initial request in the dialog path
	        dialogPath.setInvite(invite);

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
        SdpParser parser = new SdpParser(getDialogPath().getRemoteContent().getBytes(
                UTF8));
        MediaDescription mediaVideo = parser.getMediaDescription("video");
        String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription, mediaVideo);
        int remotePort = mediaVideo.port;

        // Extract video codecs from SDP
        Vector<MediaDescription> medias = parser.getMediaDescriptions("video");
        Vector<VideoCodec> proposedCodecs = VideoCodecManager.extractVideoCodecsFromSdp(medias);

        IVideoPlayer player = getPlayer();
        
        // Codec negotiation
        VideoCodec selectedVideoCodec = VideoCodecManager.negociateVideoCodec(
        		player.getSupportedCodecs(), proposedCodecs);
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

        // Set the video player orientation
        SdpOrientationExtension extensionHeader = SdpOrientationExtension.create(mediaVideo);
        if (extensionHeader != null) {
        	// Update the orientation ID
        	setOrientation(extensionHeader.getExtensionId());
        }
        
        // Set the video player remote info
        player.setRemoteInfo(selectedVideoCodec, remoteHost, remotePort, getOrientation());
    }

    /**
     * Start media session
     *
     * @throws Exception 
     */
    public void startMediaSession() throws Exception {
		// Nothing to do in case of external codec
    }


    /**
     * Close media session
     */
    public void closeMediaSession() {
		// Nothing to do in case of external codec
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
