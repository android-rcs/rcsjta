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

package com.gsma.rcs.core.ims.service.ipcall;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.content.AudioContent;
import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.content.VideoContent;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.rtp.media.MediaException;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipNetworkException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.SessionTimerManager;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.ipcalldraft.AudioCodec;
import com.gsma.rcs.service.ipcalldraft.VideoCodec;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.os.RemoteException;

import java.util.Collection;
import java.util.Vector;

/**
 * Terminating IP call session
 * 
 * @author opob7414
 */
public class TerminatingIPCallSession extends IPCallSession {
    /**
     * The logger
     */
    private final static Logger sLogger = Logger
            .getLogger(TerminatingIPCallSession.class.getName());

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param invite Initial INVITE request
     * @param contact
     * @param rcsSettings
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public TerminatingIPCallSession(ImsService parent, SipRequest invite, ContactId contact,
            RcsSettings rcsSettings, long timestamp, ContactManager contactManager) {
        super(parent, contact, ContentManager.createLiveAudioContentFromSdp(invite
                .getContentBytes()), ContentManager.createLiveVideoContentFromSdp(invite
                .getContentBytes()), rcsSettings, timestamp, contactManager);

        // Create dialog path
        createTerminatingDialogPath(invite);
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Initiate a new IP call session as terminating");
            }

            SipDialogPath dialogPath = getDialogPath();
            send180Ringing(dialogPath.getInvite(), dialogPath.getLocalTag());

            Collection<ImsSessionListener> listeners = getListeners();
            ContactId contact = getRemoteContact();
            AudioContent audio = getAudioContent();
            VideoContent video = getVideoContent();
            long timestamp = getTimestamp();
            for (ImsSessionListener listener : listeners) {
                ((IPCallStreamingSessionListener) listener).handleSessionInvited(contact, audio,
                        video, timestamp);
            }

            InvitationStatus answer = waitInvitationAnswer();
            switch (answer) {
                case INVITATION_REJECTED_DECLINE:
                    /* Intentional fall through */
                case INVITATION_REJECTED_BUSY_HERE:
                    if (sLogger.isActivated()) {
                        sLogger.debug("Session has been rejected by user");
                    }
                    sendErrorResponse(dialogPath.getInvite(), dialogPath.getLocalTag(), answer);
                    removeSession();

                    for (ImsSessionListener listener : listeners) {
                        listener.handleSessionRejected(contact,
                                TerminationReason.TERMINATION_BY_USER);
                    }
                    return;

                case INVITATION_NOT_ANSWERED:
                    if (sLogger.isActivated()) {
                        sLogger.debug("Session has been rejected on timeout");
                    }

                    /* Ringing period timeout */
                    send603Decline(dialogPath.getInvite(), dialogPath.getLocalTag());

                    removeSession();

                    for (ImsSessionListener listener : listeners) {
                        listener.handleSessionRejected(contact,
                                TerminationReason.TERMINATION_BY_TIMEOUT);
                    }
                    return;

                case INVITATION_CANCELED:
                    if (sLogger.isActivated()) {
                        sLogger.debug("Session has been rejected by remote");
                    }

                    removeSession();

                    for (ImsSessionListener listener : listeners) {
                        listener.handleSessionRejected(contact,
                                TerminationReason.TERMINATION_BY_REMOTE);
                    }
                    return;

                case INVITATION_ACCEPTED:
                    setSessionAccepted();

                    for (ImsSessionListener listener : listeners) {
                        listener.handleSessionAccepted(contact);
                    }
                    break;

                default:
                    throw new IllegalArgumentException(
                            "Unknown invitation answer in run; answer=".concat(String
                                    .valueOf(answer)));
            }

            if (getRenderer() == null) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Renderer not initialized");
                }
                handleError(new IPCallError(IPCallError.RENDERER_NOT_INITIALIZED));
                return;
            }

            if (getPlayer() == null) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Player not initialized");
                }
                handleError(new IPCallError(IPCallError.PLAYER_NOT_INITIALIZED));
                return;
            }

            if (isInterrupted()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Session has been interrupted: end of processing");
                }
                return;
            }

            String sdp = buildSdpAnswer();

            dialogPath.setLocalContent(sdp);

            prepareMediaSession();

            if (sLogger.isActivated()) {
                sLogger.info("Send 200 OK");
            }
            SipResponse resp = null;
            if ((getPlayer().getVideoCodec() != null) && (getRenderer().getVideoCodec() != null)) {
                /* Video Call */
                resp = SipMessageFactory.create200OkInviteResponse(dialogPath,
                        IPCallService.FEATURE_TAGS_IP_VIDEO_CALL, sdp);
            } else {
                /* Audio Call */
                resp = SipMessageFactory.create200OkInviteResponse(dialogPath,
                        IPCallService.FEATURE_TAGS_IP_VOICE_CALL, sdp);
            }

            dialogPath.setSigEstablished();

            /* Send response */
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager()
                    .sendSipMessageAndWait(resp);

            /* Analyze the received response */
            if (ctx.isSipAck()) {
                if (sLogger.isActivated()) {
                    sLogger.info("ACK request received");
                }

                dialogPath.setSessionEstablished();

                startMediaTransfer();

                /* Start session timer */
                SessionTimerManager sessionTimerManager = getSessionTimerManager();
                if (sessionTimerManager.isSessionTimerActivated(resp)) {
                    sessionTimerManager.start(SessionTimerManager.UAS_ROLE,
                            dialogPath.getSessionExpireTime());
                }

                for (int i = 0; i < getListeners().size(); i++) {
                    getListeners().get(i).handleSessionStarted(contact);
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.debug("No ACK received for INVITE");
                }

                /* No response received: timeout */
                handleError(new IPCallError(IPCallError.SESSION_INITIATION_FAILED));
            }
        } catch (SipPayloadException e) {
            sLogger.error("Session initiation has failed!", e);
            handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION, e.getMessage()));
        } catch (SipNetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
            handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION, e.getMessage()));
        } catch (MediaException e) {
            sLogger.error("Session initiation has failed!", e);
            handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION, e.getMessage()));
        } catch (RemoteException e) {
            sLogger.error("Session initiation has failed!", e);
            handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION, e.getMessage()));
        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error("Session initiation has failed!", e);
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
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Session error: ").append(error.getErrorCode())
                    .append(", reason=").append(error.getMessage()).toString());
        }
        closeMediaSession();
        removeSession();
        ContactId contact = getRemoteContact();
        for (int i = 0; i < getListeners().size(); i++) {
            ((IPCallStreamingSessionListener) getListeners().get(i))
                    .handleCallError(contact, error);
        }
    }

    /**
     * Build sdp response for addVideo
     * 
     * @param reInvite reInvite Request received
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    private String buildSdpAnswer() throws SipPayloadException, SipNetworkException {
        // Parse the remote SDP part
        SdpParser parser = new SdpParser(getDialogPath().getRemoteContent().getBytes(UTF8));

        // Extract the audio codecs from SDP
        Vector<MediaDescription> audio = parser.getMediaDescriptions("audio");
        Vector<AudioCodec> proposedAudioCodecs = AudioCodecManager.extractAudioCodecsFromSdp(audio);

        // Extract video codecs from SDP
        Vector<MediaDescription> video = parser.getMediaDescriptions("video");
        Vector<VideoCodec> proposedVideoCodecs = VideoCodecManager.extractVideoCodecsFromSdp(video);

        // Audio codec negotiation
        AudioCodec selectedAudioCodec;
        try {
            selectedAudioCodec = AudioCodecManager.negociateAudioCodec(getRenderer()
                    .getSupportedAudioCodecs(), proposedAudioCodecs);
            if (selectedAudioCodec == null) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Proposed audio codecs are not supported");
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
                selectedVideoCodec = VideoCodecManager.negociateVideoCodec(getPlayer()
                        .getSupportedVideoCodecs(), proposedVideoCodecs);
                if (selectedVideoCodec == null) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Proposed video codecs are not supported");
                    }
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.debug("No video requested");
                }
            }

            // Build SDP answer
            String audioSdp = AudioSdpBuilder.buildSdpAnswer(selectedAudioCodec, getPlayer()
                    .getLocalAudioRtpPort());
            String videoSdp = "";
            if (selectedVideoCodec != null) {
                MediaDescription mediaVideo = parser.getMediaDescription("video");
                videoSdp = VideoSdpBuilder.buildSdpAnswer(selectedVideoCodec, getRenderer()
                        .getLocalVideoRtpPort(), mediaVideo);
            }
            String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
            String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();

            // Build SDP for response
            String sdp = "v=0" + SipUtils.CRLF + "o=- " + ntpTime + " " + ntpTime + " "
                    + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "s=-" + SipUtils.CRLF
                    + "c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "t=0 0"
                    + SipUtils.CRLF + audioSdp + videoSdp + "a=sendrcv" + SipUtils.CRLF;

            return sdp;

        } catch (RemoteException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Session initiation has failed", e);
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
