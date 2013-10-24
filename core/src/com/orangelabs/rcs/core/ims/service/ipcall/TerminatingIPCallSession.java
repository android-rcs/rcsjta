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
import org.gsma.joyn.ipcall.VideoCodec;

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
import com.orangelabs.rcs.core.ims.service.ipcall.IPCallSession.RendererEventListener;
import com.orangelabs.rcs.core.ims.service.richcall.video.SdpOrientationExtension;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Terminating IP call session
 *
 * @author opob7414
 */
public class TerminatingIPCallSession extends IPCallSession {
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
    public TerminatingIPCallSession(ImsService parent, SipRequest invite) {
        super(parent, SipUtils.getAssertedIdentity(invite),
        		ContentManager.createLiveAudioContentFromSdp(invite.getContentBytes()),
        		ContentManager.createLiveVideoContentFromSdp(invite.getContentBytes()));

        // Create dialog path
        createTerminatingDialogPath(invite);
    }

    /**
     * Background processing
     */
	public void run() {
		try {
			if (logger.isActivated()) {
				logger.info("Initiate a new IP call session as terminating");
			}

			// Send a 180 Ringing response
			send180Ringing(getDialogPath().getInvite(), getDialogPath()
					.getLocalTag());

			// Notify listener
			getImsService().getImsModule().getCore().getListener()
					.handleIPCallInvitation(this);

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
					getListeners().get(i).handleSessionAborted(
							ImsServiceSession.TERMINATION_BY_USER);
				}
				return;
			} else if (answer == ImsServiceSession.INVITATION_NOT_ANSWERED) {
				if (logger.isActivated()) {
					logger.debug("Session has been rejected on timeout");
				}

				// Ringing period timeout
				send603Decline(getDialogPath().getInvite(), getDialogPath()
						.getLocalTag());

				// Remove the current session
				getImsService().removeSession(this);

				// Notify listeners
				for (int i = 0; i < getListeners().size(); i++) {
					getListeners().get(i).handleSessionAborted(
							ImsServiceSession.TERMINATION_BY_TIMEOUT);
				}
				return;
			} else if (answer == ImsServiceSession.INVITATION_CANCELED) {
				if (logger.isActivated()) {
					logger.debug("Session has been canceled");
				}
				return;
			}

			// Check if a renderer has been set
			if (getRenderer() == null) {
				if (logger.isActivated()) {
					logger.debug("Renderer not initialized");
				}
				handleError(new IPCallError(IPCallError.RENDERER_NOT_INITIALIZED));
				return;
			}

			// Check if a player has been set
			if (getPlayer() == null) {
				if (logger.isActivated()) {
					logger.debug("Player not initialized");
				}
				handleError(new IPCallError(IPCallError.PLAYER_NOT_INITIALIZED));
				return;
			}

			// user has accepted the call invitation
			String sdp = buildCallInitSdpResponse();
			if (logger.isActivated()) {
				logger.info("buildCallInitSdpResponse() - Done");
			}

			// Set the local SDP in the dialog path
			getDialogPath().setLocalContent(sdp);

			 // prepare media session			
			prepareMediaSession();
			
			// Create a 200 OK response
			SipResponse resp = null;
			if (getPlayer() != null) {
				if (getPlayer() != null) {
					// audio+video IP Call
					resp = SipMessageFactory.create200OkInviteResponse(
							getDialogPath(),
							IPCallService.FEATURE_TAGS_IP_VIDEO_CALL, sdp);
				} else {
					// audio IP Call
					resp = SipMessageFactory.create200OkInviteResponse(
							getDialogPath(),
							IPCallService.FEATURE_TAGS_IP_VOICE_CALL, sdp);
				}
			} else {
				handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION,
						"Audio player not initialized"));
			}

			// The signalisation is established
			getDialogPath().sigEstablished();

			if (logger.isActivated()) {
				logger.info("Send 200 OK");
			}
			// Send response
			SipTransactionContext ctx = getImsService().getImsModule()
					.getSipManager().sendSipMessageAndWait(resp);

			// Analyze the received response
			if (ctx.isSipAck()) {
				// ACK received
				if (logger.isActivated()) {
					logger.info("ACK request received");
				}

				// The session is established
				getDialogPath().sessionEstablished();

				// startmediaSession
				startMediaSession();
				

				// Start session timer
				if (getSessionTimerManager().isSessionTimerActivated(resp)) {
					getSessionTimerManager().start(
							SessionTimerManager.UAS_ROLE,
							getDialogPath().getSessionExpireTime());
				}

				// Notify listeners
				for (int i = 0; i < getListeners().size(); i++) {
					getListeners().get(i).handleSessionStarted();
				}
			} else {
				if (logger.isActivated()) {
					logger.debug("No ACK received for INVITE");
				}

				// No response received: timeout
				handleError(new IPCallError(
						IPCallError.SESSION_INITIATION_FAILED));
			}
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
     * Handle error
     *
     * @param error Error
     */
    public void handleError(IPCallError error) {
        // Error
        if (logger.isActivated()) {
            logger.info("Session error: " + error.getErrorCode() + ", reason=" + error.getMessage());
        }

        // Close media (audio, video) session
        closeMediaSession();

        // Remove the current session
        getImsService().removeSession(this);
        
        // Notify listener
        if (!isInterrupted()) {
            for(int i=0; i < getListeners().size(); i++) {
                ((IPCallStreamingSessionListener)getListeners().get(i)).handleCallError(error);
            }
        }
    }
    

    /**
     * Prepare media session
     * 
     * @throws Exception 
     */
	public void prepareMediaSession() throws Exception {
	
		// Parse the remote SDP part
		SdpParser remoteParser = new SdpParser(getDialogPath()
				.getRemoteContent().getBytes());

		// Extract the remote host (same between audio and video)
		String remoteHost = SdpUtils
				.extractRemoteHost(remoteParser.sessionDescription.connectionInfo);

		// Extract the audio port
		MediaDescription mediaAudio = remoteParser.getMediaDescription("audio");
		int audioRemotePort = mediaAudio.port;

		// Extract the video port
		MediaDescription mediaVideo = remoteParser.getMediaDescription("video");
		// int videoRemotePort = mediaVideo.port;
		int videoRemotePort = -1;
		if (mediaVideo != null) {
			videoRemotePort = mediaVideo.port;
		}

		if (logger.isActivated()) {
			logger.info("Extract Audio/Video ports - Done");
		}

		// Extract the audio codecs from SDP
		Vector<MediaDescription> audio = remoteParser
				.getMediaDescriptions("audio");
		Vector<AudioCodec> proposedAudioCodecs = AudioCodecManager
				.extractAudioCodecsFromSdp(audio);

		// Extract video codecs from SDP
		Vector<MediaDescription> video = remoteParser
				.getMediaDescriptions("video");
		Vector<VideoCodec> proposedVideoCodecs = null;
		if (mediaVideo != null) {
			proposedVideoCodecs = VideoCodecManager.extractVideoCodecsFromSdp(video);
		}

		// Audio codec negotiation
		AudioCodec selectedAudioCodec;

		selectedAudioCodec = AudioCodecManager.negociateAudioCodec(
				getRenderer().getSupportedAudioCodecs(),
				proposedAudioCodecs);
		if (selectedAudioCodec == null) {
			if (logger.isActivated()) {
				logger.debug("Proposed audio codecs are not supported");
			}

			// Send a 415 Unsupported media type response
			send415Error(getDialogPath().getInvite());

			// Unsupported media type
			handleError(new IPCallError(IPCallError.UNSUPPORTED_AUDIO_TYPE));

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
			}
		}

		// Set the audio codec and listener in Audio Renderer
		getRenderer().addEventListener(new RendererEventListener(this));
		if (logger.isActivated()) {
			logger.debug("Set audio codec in the audio renderer: "
					+ selectedAudioCodec.getEncoding());
		}

		// Set the audio codec and listener in Audio Player
		getPlayer().addEventListener(new PlayerEventListener(this));
		if (logger.isActivated()) {
			logger.debug("Set audio codec in the audio player: "
					+ selectedAudioCodec.getEncoding());
		}

		// // Open the audio renderer
		// getAudioRenderer().open(remoteHost, audioRemotePort);
		// if (logger.isActivated()) {
		// logger.debug("Open audio renderer with remoteHost ("+remoteHost+") and remotePort ("+audioRemotePort+")");
		// }
		//
		// // Open the audio player
		// getAudioPlayer().open(remoteHost, audioRemotePort);
		// if (logger.isActivated()) {
		// logger.debug("Open audio player on renderer RTP stream");
		// }

		// Set the listeners on video player and renderer
		if ((getRenderer() != null) && (getPlayer() != null)) {
			getRenderer().addEventListener(new RendererEventListener(this));
			getPlayer().addEventListener(new PlayerEventListener(this));
		}

		// Set the OrientationHeaderID in renderer and player
		if (mediaVideo != null) {
			SdpOrientationExtension extensionHeader = SdpOrientationExtension
					.create(mediaVideo);
			if ((getRenderer() != null) && (getPlayer() != null)
					&& (extensionHeader != null)) {
				// TODO getRenderer().setOrientationHeaderId(extensionHeader.getExtensionId());
				// TODO getPlayer().setOrientationHeaderId(extensionHeader.getExtensionId());
			}
		}

		// Open the Video Renderer and Player
		// always open the player after the renderer when the RTP stream is shared
		if ((getRenderer() != null) && (getPlayer() != null)) {
			getRenderer().open(selectedAudioCodec, selectedVideoCodec, remoteHost, audioRemotePort, videoRemotePort);
			getPlayer().open(selectedAudioCodec, selectedVideoCodec, remoteHost, audioRemotePort, videoRemotePort);
			if (logger.isActivated()) {
				logger.debug("Open video renderer with remoteHost ("
						+ remoteHost + ") and remotePort (" + videoRemotePort
						+ ")");
				logger.debug("Open video player on renderer RTP stream");
			}
		}

		if (logger.isActivated()) {
			logger.debug("AudioContent = " + this.getAudioContent());
			logger.debug("VideoContent = " + this.getVideoContent());
		}
	}

    /**
     * Start media session
     * 
     * @throws Exception 
     */
    public void startMediaSession() throws Exception {
        // Already done in run() method
    	
    	// Start the audio renderer
		// getAudioRenderer().start();
		// if (logger.isActivated()) {
		// logger.debug("Start audio renderer");
		// }

		// Start the audio player
		// getAudioPlayer().start();
		// if (logger.isActivated()) {
		// logger.debug("Start audio player");
		// }

		// Start the video renderer and video player
		if ((getPlayer() != null) && (getRenderer() != null)) {
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

