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

package com.orangelabs.rcs.core.ims.service.ipcall;

import java.util.Vector;

import org.gsma.joyn.ipcall.AudioCodec;
import org.gsma.joyn.ipcall.IIPCallPlayer;
import org.gsma.joyn.ipcall.IIPCallRenderer;
import org.gsma.joyn.ipcall.VideoCodec;

import com.orangelabs.rcs.core.content.LiveAudioContent;
import com.orangelabs.rcs.core.content.LiveVideoContent;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.richcall.video.SdpOrientationExtension;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Originating IP call session
 *
 * @author opob7414
 */
public class OriginatingIPCallSession extends IPCallSession {
	
    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    /**
     * Constructor
     *
     * @param parent IMS service
     * @param contact Remote contact
     * @param audioContent Audio content
     * @param videoContent Video content
     * @param player IP call player
     * @param renderer IP call renderer
     */
    public OriginatingIPCallSession(ImsService parent, String contact,
    		LiveAudioContent audioContent, LiveVideoContent videoContent,
    		IIPCallPlayer player, IIPCallRenderer renderer) {
    	super(parent, contact, audioContent, videoContent);
    	
        // Create dialog path
        createOriginatingDialogPath();
        
        // Set the player
        setPlayer(player);
        
        // Set the renderer
        setRenderer(renderer);
    }
    
    /**
     * Background processing
     */
    public void run() {
        try {
            if (logger.isActivated()) {
                logger.info("Initiate a new IP call session as originating");
            }

            // Check audio parameters 
            if (getAudioContent() == null) {
                handleError(new IPCallError(IPCallError.UNSUPPORTED_AUDIO_TYPE, "Audio codec not supported"));
                return;
            }
            
            // build SDP proposal
            String sdp = buildAudioVideoSdpProposal();

            // Set the local SDP part in the dialog path
            getDialogPath().setLocalContent(sdp); 

            // Create an INVITE request
            if (logger.isActivated()) {
                logger.info("Send INVITE");
            }
            SipRequest invite;  
            if (getVideoContent() == null) {
            	// Voice call
            	invite = SipMessageFactory.createInvite(getDialogPath(), IPCallService.FEATURE_TAGS_IP_VOICE_CALL, sdp);
            } else {
            	// Video call
            	invite = SipMessageFactory.createInvite(getDialogPath(), IPCallService.FEATURE_TAGS_IP_VIDEO_CALL, sdp);
            } 

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
            handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION,
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

		// Extract the remote host (same between audio and video)
		String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription.connectionInfo);

		// Extract media ports
		MediaDescription mediaAudio = parser.getMediaDescription("audio");
		int audioRemotePort = mediaAudio.port;
		MediaDescription mediaVideo = parser.getMediaDescription("video");
		int videoRemotePort = -1;
		if (mediaVideo != null) {
			videoRemotePort = mediaVideo.port;
		}

		// Extract audio codecs from SDP
		Vector<MediaDescription> audio = parser.getMediaDescriptions("audio");
		Vector<AudioCodec> proposedAudioCodecs = AudioCodecManager.extractAudioCodecsFromSdp(audio);

		// Extract video codecs from SDP
		Vector<MediaDescription> video = parser.getMediaDescriptions("video");
		Vector<VideoCodec> proposedVideoCodecs = VideoCodecManager.extractVideoCodecsFromSdp(video);

		// Audio codec negotiation
		AudioCodec selectedAudioCodec = AudioCodecManager.negociateAudioCodec(
				getPlayer().getSupportedAudioCodecs(),
				proposedAudioCodecs);
		if (selectedAudioCodec == null) {
			if (logger.isActivated()) {
				logger.debug("Proposed audio codecs are not supported");
			}

			// Terminate session
			terminateSession(ImsServiceSession.TERMINATION_BY_SYSTEM);

			// Report error
			handleError(new IPCallError(IPCallError.UNSUPPORTED_AUDIO_TYPE));
			return;
		}

		// Video codec negotiation
		VideoCodec selectedVideoCodec = null;
		if ((mediaVideo != null) && (getPlayer() != null)) {
			selectedVideoCodec = VideoCodecManager.negociateVideoCodec(
					getPlayer().getSupportedVideoCodecs(),
					proposedVideoCodecs);
			if (selectedVideoCodec == null) {
				if (logger.isActivated()) {
					logger.debug("Proposed video codecs are not supported");
				}

				// Terminate session
				terminateSession(ImsServiceSession.TERMINATION_BY_SYSTEM);

				// Report error
				handleError(new IPCallError(IPCallError.UNSUPPORTED_VIDEO_TYPE));
				return;
			}
		}

		// Set the player listener
		getPlayer().addEventListener(new PlayerEventListener(this));

		// Set the renderer listener
		getRenderer().addEventListener(new RendererEventListener(this));

		// Set the OrientationHeaderID
		if (mediaVideo!= null) {
			SdpOrientationExtension extensionHeader = SdpOrientationExtension.create(mediaVideo);
			if ((getRenderer()!= null)&&(getPlayer()!= null)&&(extensionHeader != null)) {
				// TODO getRenderer().setOrientationHeaderId(extensionHeader.getExtensionId());
				// TODO getPlayer().setOrientationHeaderId(extensionHeader.getExtensionId());
			}
		}
		

//		// Open the audio renderer
//		getAudioRenderer().open(remoteHost, audioRemotePort);
//		// Open the audio player - always open the player after 
//		// the renderer when the RTP stream is shared
//		getAudioPlayer().open(remoteHost, audioRemotePort); 

		// Open the video player/renderer
		if ((getRenderer()!= null)&&(getPlayer()!= null)&&(selectedVideoCodec!= null)) {
			getRenderer().open(selectedAudioCodec, selectedVideoCodec, remoteHost, audioRemotePort, videoRemotePort);
			// always open the player after the renderer when the RTP stream is shared
			getPlayer().open(selectedAudioCodec, selectedVideoCodec, remoteHost, audioRemotePort, videoRemotePort);
			if (logger.isActivated()) {
				logger.debug("Open video renderer with remoteHost ("
						+ remoteHost + ") and remotePort (" + videoRemotePort
						+ ")");
				logger.debug("Open video player on renderer RTP stream");
			}
		}
	}

	@Override
	public void startMediaSession() throws Exception {
//		getAudioPlayer().start();	
//		getAudioRenderer().start();
		
		if ((getPlayer()!= null)&&(getRenderer()!= null) ){
			getPlayer().start();
			if (logger.isActivated()) {
              	logger.debug("Start video player");
              }
			
			getRenderer().start();
			if (logger.isActivated()) {
              	logger.debug("Start video renderer");	
              }
		}
	}
}
