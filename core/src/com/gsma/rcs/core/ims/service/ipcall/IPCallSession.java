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
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsServiceError;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.core.ims.service.ImsSessionBasedServiceError;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.richcall.video.SdpOrientationExtension;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.ipcalldraft.AudioCodec;
import com.gsma.rcs.service.ipcalldraft.IIPCallPlayer;
import com.gsma.rcs.service.ipcalldraft.IIPCallPlayerListener;
import com.gsma.rcs.service.ipcalldraft.IIPCallRenderer;
import com.gsma.rcs.service.ipcalldraft.IIPCallRendererListener;
import com.gsma.rcs.service.ipcalldraft.VideoCodec;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.os.RemoteException;

import java.util.Vector;

/**
 * IP call session
 * 
 * @author opob7414
 */
public abstract class IPCallSession extends ImsServiceSession {

    /**
     * Constant values for session update request type
     */
    public final static int ADD_VIDEO = 0;
    public final static int REMOVE_VIDEO = 1;
    public final static int SET_ON_HOLD = 2;
    public final static int SET_ON_RESUME = 3;

    /**
     * Constant values for session direction type
     */
    public static final int TYPE_INCOMING_IPCALL = 16;
    public static final int TYPE_OUTGOING_IPCALL = 17;

    /**
     * Live audio content to be streamed
     */
    private AudioContent mAudioContent;

    /**
     * Video content to be streamed
     */
    private VideoContent mVideoContent;

    /**
     * IP call renderer
     */
    private IIPCallRenderer mRenderer;

    /**
     * IP call player
     */
    private IIPCallPlayer mPlayer;

    /**
     * Call hold manager
     */
    private CallHoldManager mHoldMgr;

    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(IPCallSession.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param imsService parent IMS service
     * @param contact Remote contactId
     * @param audioContent Audio content
     * @param videoContent Video content
     * @param rcsSettings
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public IPCallSession(ImsService imsService, ContactId contact, AudioContent audioContent,
            VideoContent videoContent, RcsSettings rcsSettings, long timestamp,
            ContactsManager contactManager) {
        super(imsService, contact, PhoneUtils.formatContactIdToUri(contact), rcsSettings,
                timestamp, contactManager);

        mAudioContent = audioContent;
        mVideoContent = videoContent;
    }

    /**
     * Is video activated
     * 
     * @return Boolean
     */
    public boolean isVideoActivated() {
        return (mVideoContent != null);
    }

    /**
     * Returns the video content
     * 
     * @return Video content
     */
    public VideoContent getVideoContent() {
        return mVideoContent;
    }

    /**
     * Set the video content
     * 
     * @param videoContent Video content
     */
    public void setVideoContent(VideoContent videoContent) {
        this.mVideoContent = videoContent;
    }

    /**
     * Returns the audio content
     * 
     * @return Audio content
     */
    public AudioContent getAudioContent() {
        return mAudioContent;
    }

    /**
     * Set the audio content
     * 
     * @param audioContent Audio content
     */
    public void setAudioContent(AudioContent audioContent) {
        this.mAudioContent = audioContent;
    }

    /**
     * Get the IP call renderer
     * 
     * @return Renderer
     */
    public IIPCallRenderer getRenderer() {
        return mRenderer;
    }

    /**
     * Set the IP call renderer
     * 
     * @param renderer Renderer
     */
    public void setRenderer(IIPCallRenderer renderer) {
        this.mRenderer = renderer;
    }

    /**
     * Get the IP call player
     * 
     * @return Player
     */
    public IIPCallPlayer getPlayer() {
        return mPlayer;
    }

    /**
     * Set the IP call player
     * 
     * @param player Player
     */
    public void setPlayer(IIPCallPlayer player) {
        this.mPlayer = player;
    }

    /**
     * Prepare media session
     * 
     * @throws Exception
     */
    public void prepareMediaSession() throws Exception {
        // Parse the remote SDP part
        SdpParser parser = new SdpParser(getDialogPath().getRemoteContent().getBytes(UTF8));

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
        AudioCodec selectedAudioCodec = AudioCodecManager.negociateAudioCodec(getPlayer()
                .getSupportedAudioCodecs(), proposedAudioCodecs);
        if (selectedAudioCodec == null) {
            if (logger.isActivated()) {
                logger.debug("Proposed audio codecs are not supported");
            }

            // Terminate session
            terminateSession(TerminationReason.TERMINATION_BY_SYSTEM);

            // Report error
            handleError(new IPCallError(IPCallError.UNSUPPORTED_AUDIO_TYPE));
            return;
        }

        // Video codec negotiation
        VideoCodec selectedVideoCodec = null;
        if ((mediaVideo != null) && (getPlayer() != null) && (getPlayer().getVideoCodec() != null)) {
            selectedVideoCodec = VideoCodecManager.negociateVideoCodec(getPlayer()
                    .getSupportedVideoCodecs(), proposedVideoCodecs);
            if (selectedVideoCodec == null) {
                if (logger.isActivated()) {
                    logger.debug("Proposed video codecs are not supported");
                }

                // Terminate session
                terminateSession(TerminationReason.TERMINATION_BY_SYSTEM);

                // Report error
                handleError(new IPCallError(IPCallError.UNSUPPORTED_VIDEO_TYPE));
                return;
            }
        }

        // Set the OrientationHeaderID
        if (mediaVideo != null) {
            SdpOrientationExtension extensionHeader = SdpOrientationExtension.create(mediaVideo);
            if ((getRenderer() != null) && (getPlayer() != null) && (extensionHeader != null)) {
                // TODO getRenderer().setOrientationHeaderId(extensionHeader.getExtensionId());
                // TODO getPlayer().setOrientationHeaderId(extensionHeader.getExtensionId());
            }
        }

        // Open the renderer
        if (getRenderer() != null) {
            getRenderer().addEventListener(new RendererEventListener());
            getRenderer().open(selectedAudioCodec, selectedVideoCodec, remoteHost, audioRemotePort,
                    videoRemotePort);
            if (logger.isActivated()) {
                logger.debug("Open renderer on " + remoteHost + ":" + audioRemotePort + ":"
                        + videoRemotePort);
            }
        }

        // Open the player
        if (getPlayer() != null) {
            getPlayer().addEventListener(new PlayerEventListener(this));
            getPlayer().open(selectedAudioCodec, selectedVideoCodec, remoteHost, audioRemotePort,
                    videoRemotePort);
            if (logger.isActivated()) {
                logger.debug("Open player on " + remoteHost + ":" + audioRemotePort + ":"
                        + videoRemotePort);
            }
        }

        // Open the video player/renderer
        if ((getRenderer() != null) && (getPlayer() != null) && (selectedVideoCodec != null)) {
            getRenderer().open(selectedAudioCodec, selectedVideoCodec, remoteHost, audioRemotePort,
                    videoRemotePort);
            // always open the player after the renderer when the RTP stream is shared
            getPlayer().open(selectedAudioCodec, selectedVideoCodec, remoteHost, audioRemotePort,
                    videoRemotePort);
            if (logger.isActivated()) {
                logger.debug("Open video player on renderer RTP stream");
            }
        }
    }

    /**
     * Start media session
     * 
     * @throws RemoteException
     */
    public void startMediaSession() throws RemoteException {
        if (getPlayer() != null) {
            if (logger.isActivated()) {
                logger.debug("Start player");
            }
            getPlayer().start();
        }

        if (getRenderer() != null) {
            if (logger.isActivated()) {
                logger.debug("Start renderer");
            }
            getRenderer().start();
        }
    }

    /**
     * Receive BYE request
     * 
     * @param bye BYE request
     */
    public void receiveBye(SipRequest bye) {
        super.receiveBye(bye);

        // Request capabilities to the remote
        getImsService().getImsModule().getCapabilityService()
                .requestContactCapabilities(getRemoteContact());
    }

    /**
     * Create an INVITE request
     * 
     * @return the INVITE request
     * @throws SipException
     */
    public SipRequest createInvite() throws SipException {
        return SipMessageFactory.createInvite(getDialogPath(), null, getDialogPath()
                .getLocalContent());
    }

    /**
     * Receive re-INVITE request
     * 
     * @param reInvite re-INVITE received request
     */
    public void receiveReInvite(SipRequest reInvite) {
        if (logger.isActivated()) {
            logger.info("receiveReInvite");
        }

        if (reInvite.getSdpContent() == null) {
            // "Keep Alive" ReInvite
            getSessionTimerManager().receiveReInvite(reInvite);
        } else {
            // ReInvite for Update of Session
            String content = reInvite.getSdpContent();

            // set received sdp proposal as remote sdp content in dialogPath
            getDialogPath().setRemoteContent(content);

            int requestType = -1;
            // Analyze sdp to dispatch according to sdp content
            if (isTagPresent(content, "a=sendonly")) {
                requestType = 3;// Set On Hold Sendonly
            } else if (isTagPresent(content, "a=inactive")) {
                requestType = 2;// Set On Hold Inactive
            } else if (isTagPresent(content, "a=sendrcv")) {
                if ((isTagPresent(content, "m=video"))) {
                    requestType = 0;// Add Video
                } else if (!isTagPresent(content, "m=video")) {
                    requestType = 1;// Remove Video
                }
                // else if ((!isTagPresent(content, "m=video"))&& (getVideoContent()!= null)){
                // requestType = 1;// Remove Video
                // }
                // else {
                // requestType = 5;// Set on Resume
                // }
            }
            ContactId contact = getRemoteContact();
            switch (requestType) {
                case (0): { // Case Add Video
                    // create Video Content and set it on session
                    VideoContent videocontent = ContentManager
                            .createLiveVideoContentFromSdp(reInvite.getContentBytes());
                    setVideoContent(mVideoContent);

                    // processes user Answer and SIP response
                    getUpdateSessionManager().waitUserAckAndSendReInviteResp(reInvite,
                            IPCallService.FEATURE_TAGS_IP_VIDEO_CALL, IPCallSession.ADD_VIDEO);

                    // get video Encoding , video Width and video Height
                    String videoEncoding = (videocontent == null) ? "" : videocontent.getEncoding();
                    int videoWidth = (videocontent == null) ? 0 : videocontent.getWidth();
                    int videoHeight = (videocontent == null) ? 0 : videocontent.getHeight();

                    // Notify listeners
                    for (int i = 0; i < getListeners().size(); i++) {
                        ((IPCallStreamingSessionListener) getListeners().get(i))
                                .handleAddVideoInvitation(contact, videoEncoding, videoWidth,
                                        videoHeight);
                    }
                }
                    break;
                case (1): { // Case Remove Video
                    // build sdp response
                    String sdp = buildRemoveVideoSdpResponse();

                    // set sdp response as local content
                    getDialogPath().setLocalContent(sdp);

                    // process user Answer and SIP response
                    getUpdateSessionManager().send200OkReInviteResp(reInvite,
                            IPCallService.FEATURE_TAGS_IP_VOICE_CALL, sdp,
                            IPCallSession.REMOVE_VIDEO);

                    // Notify listeners
                    for (int i = 0; i < getListeners().size(); i++) {
                        ((IPCallStreamingSessionListener) getListeners().get(i))
                                .handleRemoveVideo(contact);
                    }
                }
                    break;
                case (2): { // Case Set On Hold Inactive
                    // instanciate Hold Manager
                    mHoldMgr = new IPCall_RemoteHoldInactive(this);

                    // launhc callHold
                    mHoldMgr.setCallHold(true, reInvite);

                    // Notify listeners
                    for (int i = 0; i < getListeners().size(); i++) {
                        ((IPCallStreamingSessionListener) getListeners().get(i))
                                .handleCallHold(contact);
                    }
                }
                    break;

                case (5): { // Case Set On Resume
                    // instanciate Hold Manager
                    mHoldMgr = new IPCall_RemoteHoldInactive(this);

                    // launhc callHold
                    mHoldMgr.setCallHold(false, reInvite);

                    // Notify listeners
                    for (int i = 0; i < getListeners().size(); i++) {
                        ((IPCallStreamingSessionListener) getListeners().get(i))
                                .handleCallResume(contact);
                    }
                }
                    break;
            }
        }
    }

    /**
     * Add video in the current call
     */
    public void addVideo() {
        if (logger.isActivated()) {
            logger.info("Add video");
        }

        // Add video on IP call player and renderer
        // TODO

        // Build SDP
        String sdp = buildAudioVideoSdpProposal();

        // Set SDP proposal as the local SDP part in the dialog path
        getDialogPath().setLocalContent(sdp);

        // Create re-INVITE
        SipRequest reInvite = getUpdateSessionManager().createReInvite(
                IPCallService.FEATURE_TAGS_IP_VIDEO_CALL, sdp);

        // Send re-INVITE
        getUpdateSessionManager().sendReInvite(reInvite, IPCallSession.ADD_VIDEO);
    }

    /**
     * Remove video from the current call
     */
    public void removeVideo() {
        if (logger.isActivated()) {
            logger.info("Remove video");
        }

        // Build SDP
        String sdp = buildRemoveVideoSdpProposal();

        // Set the SDP proposal as local SDP content in the dialog path
        getDialogPath().setLocalContent(sdp);

        // Create re-INVITE
        SipRequest reInvite = getUpdateSessionManager().createReInvite(
                IPCallService.FEATURE_TAGS_IP_VOICE_CALL, sdp);

        // Send re-INVITE
        getUpdateSessionManager().sendReInvite(reInvite, IPCallSession.REMOVE_VIDEO);
    }

    public void setOnHold(boolean callHoldAction) {
        // instanciate Hold Manager
        mHoldMgr = new IPCall_HoldInactive(this);

        // launhc callHold
        mHoldMgr.setCallHold(callHoldAction);
    }

    /**
     * Handle Sip Response to ReInvite / originating side
     * 
     * @param InvitationStatus invitationStatus response code
     * @param response Sip response to sent ReInvite
     * @param requestType Type type of request (addVideo/RemoveVideo/Set on Hold/Set on Resume)
     */
    public void handleReInviteResponse(InvitationStatus status, SipResponse response,
            int requestType) {
        if (logger.isActivated()) {
            logger.info("handleReInviteResponse: " + status);
        }

        ContactId contact = getRemoteContact();
        // case Add video
        if (IPCallSession.ADD_VIDEO == requestType) {
            switch (status) {
                case INVITATION_ACCEPTED:
                    // 200 OK response
                    // prepare Video media session
                    // TODO prepareVideoSession();

                    // Notify listeners
                    for (ImsSessionListener listener : getListeners()) {
                        ((IPCallStreamingSessionListener) listener).handleAddVideoAccepted(contact);
                    }

                    try {
                        // TODO startVideoSession(true) ;
                    } catch (Exception e) {
                        if (logger.isActivated()) {
                            logger.error("Start Video session has failed", e);
                        }
                        handleError(new ImsSessionBasedServiceError(
                                ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION, e.getMessage()));
                    }
                    break;
                case INVITATION_REJECTED:
                case INVITATION_TIMEOUT:
                    // Notify listeners
                    for (ImsSessionListener listener : getListeners()) {
                        ((IPCallStreamingSessionListener) listener).handleAddVideoAborted(contact,
                                TerminationReason.TERMINATION_BY_TIMEOUT);
                    }
                default:
                    break;
            }
            // case Remove Video
        } else if (IPCallSession.REMOVE_VIDEO == requestType) {
            switch (status) {
                case INVITATION_ACCEPTED:
                    // 200 OK response
                    // close video media session
                    // TODO closeVideoSession();

                    // Remove video on IP call player & renderer
                    // TODO

                    // Notify listeners
                    for (ImsSessionListener listener : getListeners()) {
                        ((IPCallStreamingSessionListener) listener)
                                .handleRemoveVideoAccepted(contact);
                    }
                    break;
                case INVITATION_NOT_ANSWERED:
                case INVITATION_TIMEOUT:
                    // No answer or 408 TimeOut response
                    // Notify listeners
                    for (ImsSessionListener listener : getListeners()) {
                        ((IPCallStreamingSessionListener) listener).handleRemoveVideoAborted(
                                contact, TerminationReason.TERMINATION_BY_TIMEOUT);
                    }
                    break;
                default:
                    break;
            }
        } else if (IPCallSession.SET_ON_HOLD == requestType) {
            switch (status) {
                case INVITATION_ACCEPTED:
                    // 200 OK response
                    mHoldMgr.prepareSession();

                    // Notify listeners
                    for (ImsSessionListener listener : getListeners()) {
                        ((IPCallStreamingSessionListener) listener).handleCallHoldAccepted(contact);
                    }

                    // release hold
                    mHoldMgr = null;
                    break;
                case INVITATION_NOT_ANSWERED:
                case INVITATION_TIMEOUT:
                    // No answer or 408 TimeOut response
                    // Notify listeners
                    for (ImsSessionListener listener : getListeners()) {
                        ((IPCallStreamingSessionListener) listener).handleCallHoldAborted(contact,
                                TerminationReason.TERMINATION_BY_TIMEOUT);
                    }
                    break;
                default:
                    break;
            }
        } else if (IPCallSession.SET_ON_RESUME == requestType) {
            switch (status) {
                case INVITATION_ACCEPTED:
                    // 200 OK response
                    mHoldMgr.prepareSession();

                    // Notify listeners
                    for (ImsSessionListener listener : getListeners()) {
                        ((IPCallStreamingSessionListener) listener)
                                .handleCallResumeAccepted(contact);
                    }
                    // release hold
                    mHoldMgr = null;
                    break;
                case INVITATION_NOT_ANSWERED:
                case INVITATION_TIMEOUT:
                    // No answer or 408 TimeOut response
                    // Notify listeners
                    for (ImsSessionListener listener : getListeners()) {
                        ((IPCallStreamingSessionListener) listener)
                                .handleCallResumeAborted(contact);
                    }
                default:
                    break;
            }
        }
    }

    /**
     * Handle Sip Response to ReInvite/ terminating side
     * 
     * @param InvitationStatus invitationStatus response code
     * @param requestType Type type of request (addVideo/RemoveVideo/Set on Hold/Set on Resume)
     */
    public void handleReInviteUserAnswer(InvitationStatus status, int requestType) {
        if (logger.isActivated()) {
            logger.info("handleReInviteUserAnswer: " + status);
        }

        switch (status) {
            case INVITATION_ACCEPTED:
                if (IPCallSession.ADD_VIDEO == requestType) {
                    // TODO prepareVideoSession();
                }
                break;
            case INVITATION_NOT_ANSWERED:
            case INVITATION_REJECTED:
            case INVITATION_CANCELED:
            case INVITATION_TIMEOUT:
            default:
                if (IPCallSession.ADD_VIDEO == requestType) {
                    {
                        ContactId contact = getRemoteContact();
                        for (ImsSessionListener listener : getListeners()) {
                            ((IPCallStreamingSessionListener) listener).handleAddVideoAborted(
                                    contact, TerminationReason.TERMINATION_BY_TIMEOUT);
                        }
                    }
                }
                break;
        }
    }

    /**
     * Handle Sip Response to ReInvite/ terminating side
     * 
     * @param InvitationStatus invitationStatus response code
     * @param requestType Type type of request (addVideo/RemoveVideo/Set on Hold/Set on Resume)
     */
    public void handleReInviteAck(InvitationStatus status, int requestType) {
        if (logger.isActivated()) {
            logger.info("handleReInviteAck: " + status);
        }

        switch (status) {
            case INVITATION_ACCEPTED:
                // case Add video
                ContactId contact = getRemoteContact();
                if (IPCallSession.ADD_VIDEO == requestType) {
                    try {
                        // TODO startVideoSession(false);
                    } catch (Exception e) {
                        if (logger.isActivated()) {
                            logger.error("Start Video session has failed", e);
                        }
                        handleError(new ImsSessionBasedServiceError(
                                ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION, e.getMessage()));
                    }

                    // Notify listeners
                    for (ImsSessionListener listener : getListeners()) {
                        ((IPCallStreamingSessionListener) listener).handleAddVideoAccepted(contact);
                    }
                } else if (IPCallSession.REMOVE_VIDEO == requestType) {// case Remove Video
                    // close video media session
                    // TODO closeVideoSession();

                    // Remove video on IP call player & renderer
                    // TODO

                    // Notify listeners
                    for (ImsSessionListener listener : getListeners()) {
                        ((IPCallStreamingSessionListener) listener)
                                .handleRemoveVideoAccepted(contact);
                    }
                } else if (IPCallSession.SET_ON_HOLD == requestType) {// case On Hold
                    mHoldMgr.prepareSession();

                    // Notify listeners
                    for (ImsSessionListener listener : getListeners()) {
                        ((IPCallStreamingSessionListener) listener).handleCallHoldAccepted(contact);
                    }
                    // release hold manager
                    mHoldMgr = null;
                } else if (IPCallSession.SET_ON_RESUME == requestType) {// case On Resume
                    mHoldMgr.prepareSession();

                    // Notify listeners
                    for (ImsSessionListener listener : getListeners()) {
                        ((IPCallStreamingSessionListener) listener)
                                .handleCallResumeAccepted(contact);
                    }
                    // release hold manager
                    mHoldMgr = null;
                }
                break;
            default:
                break;
        }
    }

    /**
     * Handle 486 Busy
     * 
     * @param resp SipResponse
     */
    public void handle486Busy(SipResponse resp) {
        if (logger.isActivated()) {
            logger.info("486 Busy");
        }

        // Close audio and video session
        closeMediaSession();

        // Remove the current session
        removeSession();

        ContactId contact = getRemoteContact();

        // Request capabilities to the remote
        getImsService().getImsModule().getCapabilityService().requestContactCapabilities(contact);

        for (ImsSessionListener listener : getListeners()) {
            ((IPCallStreamingSessionListener) listener).handle486Busy(contact);
        }
    }

    /**
     * Handle 407 Proxy authent error
     * 
     * @param response SipResponse
     * @param requestType type of request (addVideo/RemoveVideo/Set on Hold/Set on Resume)
     */
    public void handleReInvite407ProxyAuthent(SipResponse response, int requestType) {

        // // Set the remote tag
        getDialogPath().setRemoteTag(response.getToTag());

        // Update the authentication agent
        getAuthenticationAgent().readProxyAuthenticateHeader(response);

        // get sdp content
        String content = getDialogPath().getLocalContent();

        SipRequest reInvite = null;
        // create reInvite request
        if (requestType == IPCallSession.ADD_VIDEO) {
            reInvite = getUpdateSessionManager().createReInvite(
                    IPCallService.FEATURE_TAGS_IP_VIDEO_CALL, content);
        } else if (requestType == IPCallSession.REMOVE_VIDEO) {
            reInvite = getUpdateSessionManager().createReInvite(
                    IPCallService.FEATURE_TAGS_IP_VIDEO_CALL, content);
        } else {
            // TODO for set On Hold
            reInvite = getUpdateSessionManager().createReInvite(
                    IPCallService.FEATURE_TAGS_IP_VIDEO_CALL, content);
        }

        // send reInvite request
        getUpdateSessionManager().sendReInvite(reInvite, requestType);

    }

    /**
     * Handle error
     * 
     * @param error Error
     */
    public void handleError(ImsServiceError error) {
        if (isSessionInterrupted()) {
            return;
        }

        if (logger.isActivated()) {
            logger.info("Session error: " + error.getErrorCode() + ", reason=" + error.getMessage());
        }

        // Close Audio and Video session
        closeMediaSession();

        // Remove the current session
        removeSession();

        ContactId contact = getRemoteContact();

        getImsService().getImsModule().getCapabilityService().requestContactCapabilities(contact);

        for (ImsSessionListener listener : getListeners()) {
            ((IPCallStreamingSessionListener) listener).handleCallError(contact, new IPCallError(
                    error));
        }
    }

    /**
     * Is tag present in SDP
     * 
     * @param sdp SDP
     * @param tag Tag to be searched
     * @return Boolean
     */
    public boolean isTagPresent(String sdp, String tag) {
        if ((sdp != null) && (sdp.toLowerCase().indexOf(tag) != -1)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Build SDP proposal for audio+ video session (call init or addVideo)
     * 
     * @return SDP content or null in case of error
     */
    protected String buildAudioVideoSdpProposal() {
        if (logger.isActivated()) {
            logger.debug("Build SDP proposal to add video stream in the session");
        }

        try {
            // Build SDP part
            String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
            String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();

            String audioSdp = AudioSdpBuilder.buildSdpOffer(getPlayer().getSupportedAudioCodecs(),
                    getPlayer().getLocalAudioRtpPort());

            String videoSdp = "";
            if ((getVideoContent() != null) && (getPlayer() != null) && (getRenderer() != null)) {
                videoSdp = VideoSdpBuilder.buildSdpOfferWithOrientation(getPlayer()
                        .getSupportedVideoCodecs(), getRenderer().getLocalVideoRtpPort());
            }

            String sdp = "v=0" + SipUtils.CRLF + "o=- " + ntpTime + " " + ntpTime + " "
                    + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "s=-" + SipUtils.CRLF
                    + "c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "t=0 0"
                    + SipUtils.CRLF + audioSdp + videoSdp + "a=sendrcv" + SipUtils.CRLF;

            return sdp;

        } catch (RemoteException e) {
            if (logger.isActivated()) {
                logger.error("Add video has failed", e);
            }

            // Unexpected error
            handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION, e.getMessage()));
            return null;
        }
    }

    /**
     * Build SDP proposal to remove video stream from the session
     * 
     * @return SDP content or null in case of error
     */
    private String buildRemoveVideoSdpProposal() {
        if (logger.isActivated()) {
            logger.debug("Build SDP proposal to remove video stream from the session");
        }

        try {
            // Build SDP part
            String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
            String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
            String audioSdp = AudioSdpBuilder.buildSdpOffer(getPlayer().getSupportedAudioCodecs(),
                    getPlayer().getLocalAudioRtpPort());

            return "v=0" + SipUtils.CRLF + "o=- " + ntpTime + " " + ntpTime + " "
                    + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "s=-" + SipUtils.CRLF
                    + "c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "t=0 0"
                    + SipUtils.CRLF + audioSdp + "a=sendrcv" + SipUtils.CRLF;
        } catch (RemoteException e) {
            if (logger.isActivated()) {
                logger.error("Remove video has failed", e);
            }

            // Unexpected error
            handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION, e.getMessage()));
            return null;
        }
    }

    /**
     * select sdp builder method (to build sdp response) according to serviceContext - build and
     * return sdp
     * 
     * @param reInvite reInvite received request
     * @param serviceContext context of service (Add Video, Remove Video ...)
     * @return sdp built by builder
     */
    public String buildReInviteSdpResponse(SipRequest reInvite, int serviceContext) {
        String localSdp = "";
        switch (serviceContext) {
            case (IPCallSession.ADD_VIDEO): {
                localSdp = buildAddVideoSdpResponse(reInvite);
                break;
            }
            case (IPCallSession.REMOVE_VIDEO): {
                localSdp = buildRemoveVideoSdpResponse(); // for remove Video: same sdp used for
                                                          // response as the one used for proposal
                break;
            }
        }
        return localSdp;
    }

    /**
     * Build sdp response for addVideo
     * 
     * @param reInvite reInvite Request received
     */
    private String buildAddVideoSdpResponse(SipRequest reInvite) {
        if (logger.isActivated()) {
            logger.info("buildAddVideoSdpResponse()");
        }

        StringBuilder sdp = new StringBuilder();

        // Parse the remote SDP part
        SdpParser parser = new SdpParser(reInvite.getSdpContent().getBytes(UTF8));
        MediaDescription mediaVideo = parser.getMediaDescription("video");

        // Extract video codecs from SDP
        Vector<MediaDescription> medias = parser.getMediaDescriptions("video");
        Vector<VideoCodec> proposedVideoCodecs = VideoCodecManager
                .extractVideoCodecsFromSdp(medias);
        try {
            // Check that a video player and renderer has been set
            if (getPlayer() == null) {
                handleError(new IPCallError(IPCallError.UNSUPPORTED_VIDEO_TYPE,
                        "Video player null or Video codec not selected"));
            } else if (getRenderer() == null) {
                handleError(new IPCallError(IPCallError.UNSUPPORTED_VIDEO_TYPE,
                        "Video renderer null or Video codec not selected"));
            } else {
                // Codec negotiation
                VideoCodec selectedVideoCodec = VideoCodecManager.negociateVideoCodec(getRenderer()
                        .getSupportedVideoCodecs(), proposedVideoCodecs);

                if (selectedVideoCodec == null) {
                    if (logger.isActivated()) {
                        logger.debug("Proposed codecs are not supported");
                    }

                    // Send a 415 Unsupported media type response
                    send415Error(reInvite);

                    // Unsupported media type
                    handleError(new IPCallError(IPCallError.UNSUPPORTED_VIDEO_TYPE));
                } else {
                    // Build SDP part for response
                    String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
                    String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
                    String videoSdp = VideoSdpBuilder.buildSdpAnswer(selectedVideoCodec,
                            getRenderer().getLocalVideoRtpPort(), mediaVideo);
                    String audioSdp = AudioSdpBuilder.buildSdpAnswer(getPlayer().getAudioCodec(),
                            getRenderer().getLocalAudioRtpPort());
                    sdp.append("v=0").append(SipUtils.CRLF).append("o=- ").append(ntpTime)
                            .append(" ").append(ntpTime).append(" ")
                            .append(SdpUtils.formatAddressType(ipAddress)).append(SipUtils.CRLF)
                            .append("s=-").append(SipUtils.CRLF)
                            .append("c=" + SdpUtils.formatAddressType(ipAddress))
                            .append(SipUtils.CRLF).append("t=0 0").append(SipUtils.CRLF)
                            .append(audioSdp).append("a=sendrcv").append(SipUtils.CRLF)
                            .append(videoSdp).append("a=sendrcv").append(SipUtils.CRLF);
                }
            }
        } catch (RemoteException e) {
            if (logger.isActivated()) {
                logger.error("Add Video has failed", e);
            }

            // Unexpected error
            handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }

        return sdp.toString();
    }

    /**
     * Build sdp response for removeVideo
     * 
     * @return sdp content
     */
    private String buildRemoveVideoSdpResponse() {
        if (logger.isActivated()) {
            logger.info("buildRemoveVideoSdpResponse()");
        }

        // Build SDP part
        String sdp = "";
        String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
        String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();

        try {
            String audioSdp = AudioSdpBuilder.buildSdpAnswer(getPlayer().getAudioCodec(),
                    getPlayer().getLocalAudioRtpPort());
            sdp = "v=0" + SipUtils.CRLF + "o=- " + ntpTime + " " + ntpTime + " "
                    + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "s=-" + SipUtils.CRLF
                    + "c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "t=0 0"
                    + SipUtils.CRLF + audioSdp + "a=sendrcv" + SipUtils.CRLF;
        } catch (RemoteException e) {
            if (logger.isActivated()) {
                logger.error("Remove Video has failed", e);
            }

            // Unexpected error
            handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
        return sdp;
    }

    // ******************************************************************************
    // ******************************************************************************
    // ****************** Media Session Management Methods ****************
    // ******************************************************************************
    // ******************************************************************************

    /**
     * Close media session
     */
    public void closeMediaSession() {
        if (logger.isActivated()) {
            logger.info("Close media session");
        }

        if (mRenderer != null) {
            // Close the video renderer
            try {
                mRenderer.stop();
                mRenderer.close();
                if (logger.isActivated()) {
                    logger.info("Stop and close video renderer");
                }
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Exception when closing the video renderer", e);
                }
            }
        }
        if (mPlayer != null) {
            // Close the video player
            try {
                mPlayer.stop();
                mPlayer.close();
                if (logger.isActivated()) {
                    logger.info("stop and close video player");
                }
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Exception when closing the video player", e);
                }
            }
        }
        setPlayer(null);
        setRenderer(null);
    }

    // ******************************************************************************
    // ******************************************************************************
    // ************************* Media Listeners ************************
    // ******************************************************************************
    // ******************************************************************************

    /**
     * Player event listener
     */
    protected class PlayerEventListener extends IIPCallPlayerListener.Stub {
        /**
         * Streaming session
         */
        private IPCallSession session;

        /**
         * Constructor
         * 
         * @param session Streaming session
         */
        public PlayerEventListener(IPCallSession session) {
            this.session = session;
        }

        /**
         * Callback called when the player is opened
         */
        public void onPlayerOpened() {
            if (logger.isActivated()) {
                logger.debug("Audio player is opened");
            }
        }

        /**
         * Callback called when the player is closed
         */
        public void onPlayerClosed() {
            if (logger.isActivated()) {
                logger.debug("Audio player is closed");
            }
        }

        /**
         * Callback called when the player is started
         */
        public void onPlayerStarted() {
            if (logger.isActivated()) {
                logger.debug("Audio player is started");
            }
        }

        /**
         * Callback called when the player is stopped
         */
        public void onPlayerStopped() {
            if (logger.isActivated()) {
                logger.debug("Audio player is stopped");
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
                logger.error("Audio player has failed: " + error);
            }

            // Close the media (audio, video) session
            closeMediaSession();

            // Terminate session
            terminateSession(TerminationReason.TERMINATION_BY_SYSTEM);

            ContactId contact = getRemoteContact();

            // Request capabilities to the remote
            getImsService().getImsModule().getCapabilityService()
                    .requestContactCapabilities(contact);

            // Remove the current session
            removeSession();

            for (ImsSessionListener listener : getListeners()) {
                ((IPCallStreamingSessionListener) listener).handleCallError(contact,
                        new IPCallError(IPCallError.PLAYER_FAILED));
            }

        }
    }

    /**
     * Renderer event listener
     */
    protected class RendererEventListener extends IIPCallRendererListener.Stub {
        /**
         * Constructor
         */
        public RendererEventListener() {
        }

        /**
         * Callback called when the renderer is opened
         */
        public void onRendererOpened() {
            if (logger.isActivated()) {
                logger.debug("Media renderer is opened");
            }
        }

        /**
         * Video stream has been resized
         * 
         * @param width Video width
         * @param height Video height
         */
        public void mediaResized(int width, int height) {
            if (logger.isActivated()) {
                logger.debug("The size of media has changed " + width + "x" + height);
            }

            // Notify listeners
            for (ImsSessionListener listener : getListeners()) {
                ((IPCallStreamingSessionListener) listener).handleVideoResized(width, height);
            }
        }

        /**
         * Callback called when the renderer is closed
         */
        public void onRendererClosed() {
            if (logger.isActivated()) {
                logger.debug("Media renderer is closed");
            }
        }

        /**
         * Callback called when the renderer is started
         */
        public void onRendererStarted() {
            if (logger.isActivated()) {
                logger.debug("Media renderer is started");
            }
        }

        /**
         * Callback called when the renderer is stopped
         */
        public void onRendererStopped() {
            if (logger.isActivated()) {
                logger.debug("Media renderer is stopped");
            }
        }

        /**
         * Callback called when the renderer has failed
         * 
         * @param error Error
         */
        public void onRendererError(int error) {
            if (isSessionInterrupted()) {
                return;
            }

            if (logger.isActivated()) {
                logger.error("Media renderer has failed: " + error);
            }

            // Close the audio and video session
            closeMediaSession();

            // Terminate session
            terminateSession(TerminationReason.TERMINATION_BY_SYSTEM);

            // Remove the current session
            removeSession();

            ContactId contact = getRemoteContact();
            for (ImsSessionListener listener : getListeners()) {
                ((IPCallStreamingSessionListener) listener).handleCallError(contact,
                        new IPCallError(IPCallError.RENDERER_FAILED));
            }

            // Request capabilities to the remote
            getImsService().getImsModule().getCapabilityService()
                    .requestContactCapabilities(contact);
        }
    }

    @Override
    public void receiveCancel(SipRequest cancel) {
        super.receiveCancel(cancel);

        // Request capabilities to the remote
        getImsService().getImsModule().getCapabilityService()
                .requestContactCapabilities(getRemoteContact());
    }

    @Override
    public void startSession() {
        getImsService().getImsModule().getIPCallService().addSession(this);
        start();
    }

    @Override
    public void removeSession() {
        getImsService().getImsModule().getIPCallService().removeSession(this);
    }
}
