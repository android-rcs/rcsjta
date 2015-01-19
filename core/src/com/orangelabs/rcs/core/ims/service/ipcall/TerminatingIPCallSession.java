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

package com.orangelabs.rcs.core.ims.service.ipcall;

import static com.orangelabs.rcs.utils.StringUtils.UTF8;

import java.util.Collection;
import java.util.Vector;

import android.os.RemoteException;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ipcall.AudioCodec;
import com.gsma.services.rcs.ipcall.VideoCodec;
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
import com.orangelabs.rcs.core.ims.service.ImsSessionListener;
import com.orangelabs.rcs.core.ims.service.SessionTimerManager;
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
    private final static Logger logger = Logger.getLogger(TerminatingIPCallSession.class.getSimpleName());

    /**
     * Constructor
     *
     * @param parent IMS service
     * @param invite Initial INVITE request
     */
    public TerminatingIPCallSession(ImsService parent, SipRequest invite, ContactId contact) {
        super(parent, contact,
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

			send180Ringing(getDialogPath().getInvite(), getDialogPath().getLocalTag());

			Collection<ImsSessionListener> listeners = getListeners();
			for (ImsSessionListener listener : listeners) {
				listener.handleSessionInvited();
			}

			int answer = waitInvitationAnswer();
			switch (answer) {
				case ImsServiceSession.INVITATION_REJECTED:
					if (logger.isActivated()) {
						logger.debug("Session has been rejected by user");
					}

					removeSession();

					for (ImsSessionListener listener : listeners) {
						listener.handleSessionRejectedByUser();
					}
					return;

				case ImsServiceSession.INVITATION_NOT_ANSWERED:
					if (logger.isActivated()) {
						logger.debug("Session has been rejected on timeout");
					}

					// Ringing period timeout
					send603Decline(getDialogPath().getInvite(), getDialogPath().getLocalTag());

					removeSession();

					for (ImsSessionListener listener : listeners) {
						listener.handleSessionRejectedByTimeout();
					}
					return;

				case ImsServiceSession.INVITATION_CANCELED:
					if (logger.isActivated()) {
						logger.debug("Session has been rejected by remote");
					}

					removeSession();

					for (ImsSessionListener listener : listeners) {
						listener.handleSessionRejectedByRemote();
					}
					return;

				case ImsServiceSession.INVITATION_ACCEPTED:
					setSessionAccepted();

					for (ImsSessionListener listener : listeners) {
						listener.handleSessionAccepted();
					}
					break;

				default:
					if (logger.isActivated()) {
						logger.debug("Unknown invitation answer in run; answer="
									.concat(String.valueOf(answer)));
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

	        // Test if the session should be interrupted
            if (isInterrupted()) {
            	if (logger.isActivated()) {
            		logger.debug("Session has been interrupted: end of processing");
            	}
            	return;
            }

            // Build SDP response
			String sdp = buildSdpAnswer();

			// Set the local SDP in the dialog path
			getDialogPath().setLocalContent(sdp);

			// Prepare media session
			prepareMediaSession();

			// Create a 200 OK response
			if (logger.isActivated()) {
				logger.info("Send 200 OK");
			}
			SipResponse resp = null;
			if ((getPlayer().getVideoCodec() != null) && (getRenderer().getVideoCodec() != null)) {
				// Visio Call
				resp = SipMessageFactory.create200OkInviteResponse(
						getDialogPath(),
						IPCallService.FEATURE_TAGS_IP_VIDEO_CALL, sdp);
			} else {
				// Audio Call
				resp = SipMessageFactory.create200OkInviteResponse(
						getDialogPath(),
						IPCallService.FEATURE_TAGS_IP_VOICE_CALL, sdp);
			}

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

				// Start media
				startMediaSession();

            	// Start session timer
            	if (getSessionTimerManager().isSessionTimerActivated(resp)) {
            		getSessionTimerManager().start(SessionTimerManager.UAS_ROLE, getDialogPath().getSessionExpireTime());
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
				handleError(new IPCallError(IPCallError.SESSION_INITIATION_FAILED));
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Session initiation has failed", e);
			}

			// Unexpected error
			handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION, e.getMessage()));
		}
	}

    /**
     * Handle error
     *
     * @param error Error
     */
    public void handleError(IPCallError error) {
        if (isSessionInterrupted()) {
            return;
        }

        // Error
        if (logger.isActivated()) {
            logger.info("Session error: " + error.getErrorCode() + ", reason=" + error.getMessage());
        }

        // Close media (audio, video) session
        closeMediaSession();

        // Remove the current session
        removeSession();

        // Notify listener
        for(int i=0; i < getListeners().size(); i++) {
            ((IPCallStreamingSessionListener)getListeners().get(i)).handleCallError(error);
        }
    }


	/**
	 * Build sdp response for addVideo
	 *
	 * @param reInvite  reInvite Request received
	 */
	private String buildSdpAnswer() {
		// Parse the remote SDP part
        SdpParser parser = new SdpParser(getDialogPath().getRemoteContent().getBytes(
                UTF8));

        // Extract the audio codecs from SDP
        Vector<MediaDescription> audio = parser.getMediaDescriptions("audio");
        Vector<AudioCodec> proposedAudioCodecs = AudioCodecManager.extractAudioCodecsFromSdp(audio);

        // Extract video codecs from SDP
        Vector<MediaDescription> video = parser.getMediaDescriptions("video");
        Vector<VideoCodec> proposedVideoCodecs = VideoCodecManager.extractVideoCodecsFromSdp(video);

        // Audio codec negotiation
		AudioCodec selectedAudioCodec;
		try {
			selectedAudioCodec = AudioCodecManager.negociateAudioCodec(getRenderer().getSupportedAudioCodecs(), proposedAudioCodecs);
			if (selectedAudioCodec == null) {
				if (logger.isActivated()) {
					logger.debug("Proposed audio codecs are not supported");
				}

				// Send a 415 Unsupported media type response
				send415Error(getDialogPath().getInvite());

				// Unsupported media type
				handleError(new IPCallError(IPCallError.UNSUPPORTED_AUDIO_TYPE));
				return null;
			}

	        // Video codec negotiation
			VideoCodec selectedVideoCodec = null;
			if ((proposedVideoCodecs != null) && (proposedVideoCodecs.size() > 0)) {
					selectedVideoCodec = VideoCodecManager.negociateVideoCodec(getPlayer().getSupportedVideoCodecs(), proposedVideoCodecs);
					if (selectedVideoCodec == null) {
			            if (logger.isActivated()) {
			                logger.debug("Proposed video codecs are not supported");
			            }
			        }
			} else {
	            if (logger.isActivated()) {
	                logger.debug("No video requested");
	            }
			}

			// Build SDP answer
	    	String audioSdp = AudioSdpBuilder.buildSdpAnswer(selectedAudioCodec, getPlayer().getLocalAudioRtpPort());
	    	String videoSdp = "";
        	if (selectedVideoCodec != null) {
                MediaDescription mediaVideo = parser.getMediaDescription("video");
            	videoSdp = VideoSdpBuilder.buildSdpAnswer(selectedVideoCodec,
            			getRenderer().getLocalVideoRtpPort(), mediaVideo);
            }
	        String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
	    	String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();

	        // Build SDP for response
	        String sdp =
	        	"v=0" + SipUtils.CRLF +
	        	"o=- " + ntpTime + " " + ntpTime + " " + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
	        	"s=-" + SipUtils.CRLF +
	        	"c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
	            "t=0 0" + SipUtils.CRLF +
	            audioSdp +
	            videoSdp +
	            "a=sendrcv" + SipUtils.CRLF;

			return sdp;

		} catch (RemoteException e) {
			if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }

            // Unexpected error
            handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION, e.getMessage()));
            return null;
		}
	}

	@Override
	public boolean isInitiatedByRemote() {
		return true;
	}
}

