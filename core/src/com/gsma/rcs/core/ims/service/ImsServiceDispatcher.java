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

package com.gsma.rcs.core.ims.service;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.ImsNetworkInterface;
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipNetworkException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.standfw.StoreAndForwardManager;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.core.ims.service.presence.PresenceService;
import com.gsma.rcs.core.ims.service.terms.TermsConditionsService;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.FifoBuffer;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Intent;

import java.text.ParseException;

import javax2.sip.address.SipURI;
import javax2.sip.header.ContactHeader;
import javax2.sip.header.EventHeader;
import javax2.sip.header.SubscriptionStateHeader;
import javax2.sip.message.Request;
import javax2.sip.message.Response;

/**
 * IMS service dispatcher
 * 
 * @author jexa7410
 */
public class ImsServiceDispatcher extends Thread {

    private ImsModule mImsModule;

    /**
     * Buffer of messages
     */
    private FifoBuffer mBuffer = new FifoBuffer();

    private SipIntentManager mIntentMgr = new SipIntentManager();

    private static final Logger sLogger = Logger.getLogger(ImsServiceDispatcher.class.getName());

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param imsModule IMS module
     * @param rcsSettings
     */
    public ImsServiceDispatcher(ImsModule imsModule, RcsSettings rcsSettings) {
        super("SipDispatcher");
        mImsModule = imsModule;
        mRcsSettings = rcsSettings;
    }

    /**
     * Terminate the SIP dispatcher
     */
    public void terminate() {
        if (sLogger.isActivated()) {
            sLogger.info("Terminate the multi-session manager");
        }
        mBuffer.close();
        if (sLogger.isActivated()) {
            sLogger.info("Multi-session manager has been terminated");
        }
    }

    /**
     * Post a SIP request in the buffer
     * 
     * @param request SIP request
     */
    public void postSipRequest(SipRequest request) {
        mBuffer.addObject(request);
    }

    /**
     * Background processing
     */
    public void run() {
        if (sLogger.isActivated()) {
            sLogger.info("Start background processing");
        }
        SipRequest request = null;
        while ((request = (SipRequest) mBuffer.getObject()) != null) {
            try {
                dispatch(request, System.currentTimeMillis());
            } catch (SipPayloadException e) {
                sLogger.error("Failed to dispatch received SIP request! CallId=".concat(request
                        .getCallId()), e);
                handleImsDispatchError(request);
            } catch (SipNetworkException e) {
                if (sLogger.isActivated()) {
                    sLogger.debug(e.getMessage());
                }
                handleImsDispatchError(request);
            } catch (RuntimeException e) {
                /*
                 * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
                 * which should be handled/fixed within the code. However the cases when we are
                 * executing operations on a thread unhandling such exceptions will eventually lead
                 * to exit the system and thus can bring the whole system down, which is not
                 * intended.
                 */
                sLogger.error("Failed to dispatch received SIP request! CallId=".concat(request
                        .getCallId()), e);
                handleImsDispatchError(request);
            }
        }
        if (sLogger.isActivated()) {
            sLogger.info("End of background processing");
        }
    }

    /**
     * Dispatch the received SIP request
     * 
     * @param request SIP request
     * @param timestamp Local timestamp when got SipRequest
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    private void dispatch(SipRequest request, long timestamp) throws SipPayloadException,
            SipNetworkException {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("Receive " + request.getMethod() + " request");
        }
        // Check the IP address of the request-URI
        String localIpAddress = mImsModule.getCurrentNetworkInterface().getNetworkAccess()
                .getIpAddress();
        ImsNetworkInterface imsNetIntf = mImsModule.getCurrentNetworkInterface();
        boolean isMatchingRegistered = false;
        SipURI requestURI;
        try {
            requestURI = SipUtils.ADDR_FACTORY.createSipURI(request.getRequestURI());
        } catch (ParseException e) {
            if (logActivated) {
                sLogger.error("Unable to parse request URI " + request.getRequestURI(), e);
            }
            sendFinalResponse(request, Response.BAD_REQUEST);
            return;
        }

        // First check if the request URI matches with the local interface address
        isMatchingRegistered = localIpAddress.equals(requestURI.getHost());

        // If no matching, perhaps we are behind a NAT
        if ((!isMatchingRegistered) && imsNetIntf.isBehindNat()) {
            // We are behind NAT: check if the request URI contains the previously
            // discovered public IP address and port number
            String natPublicIpAddress = imsNetIntf.getNatPublicAddress();
            int natPublicUdpPort = imsNetIntf.getNatPublicPort();
            if ((natPublicUdpPort != -1) && (natPublicIpAddress != null)) {
                isMatchingRegistered = natPublicIpAddress.equals(requestURI.getHost())
                        && (natPublicUdpPort == requestURI.getPort());
            } else {
                // NAT traversal and unknown public address/port
                isMatchingRegistered = false;
            }
        }

        if (!isMatchingRegistered) {
            // Send a 404 error
            if (logActivated) {
                sLogger.debug("Request-URI address and port do not match with registered contact: reject the request");
            }
            sendFinalResponse(request, 404);
            return;
        }

        // Check SIP instance ID: RCS client supporting the multidevice procedure shall respond to
        // the
        // invite with a 486 BUSY HERE if the identifier value of the "+sip.instance" tag included
        // in the Accept-Contact header of that incoming SIP request does not match theirs
        String instanceId = SipUtils.getInstanceID(request);
        if ((instanceId != null)
                && !instanceId.contains(mImsModule.getSipManager().getSipStack().getInstanceId())) {
            // Send 486 Busy Here
            if (logActivated) {
                sLogger.debug("SIP instance ID doesn't match: reject the request");
            }
            sendFinalResponse(request, 486);
            return;
        }

        // Check public GRUU : RCS client supporting the multidevice procedure shall respond to the
        // invite with a 486 BUSY HERE if the identifier value of the "pub-gruu" tag included
        // in the Accept-Contact header of that incoming SIP request does not match theirs
        String publicGruu = SipUtils.getPublicGruu(request);
        if ((publicGruu != null)
                && !publicGruu.contains(mImsModule.getSipManager().getSipStack().getPublicGruu())) {
            // Send 486 Busy Here
            if (logActivated) {
                sLogger.debug("SIP public-gruu doesn't match: reject the request");
            }
            sendFinalResponse(request, 486);
            return;
        }

        // Update remote SIP instance ID in the dialog path of the session
        ImsServiceSession session = getImsServiceSession(request.getCallId());
        if (session != null) {
            ContactHeader contactHeader = (ContactHeader) request.getHeader(ContactHeader.NAME);
            if (contactHeader != null) {
                String remoteInstanceId = contactHeader.getParameter(SipUtils.SIP_INSTANCE_PARAM);
                session.getDialogPath().setRemoteSipInstance(remoteInstanceId);
            }
        }

        if (request.getMethod().equals(Request.OPTIONS)) {
            // OPTIONS received

            // Capability discovery service
            mImsModule.getCapabilityService().receiveCapabilityRequest(request);
        } else if (request.getMethod().equals(Request.INVITE)) {
            // INVITE received
            if (session != null) {
                // Subsequent request received
                session.receiveReInvite(request);
                return;
            }

            // Send a 100 Trying response
            send100Trying(request);

            // Extract the SDP part
            String sdp = request.getSdpContent();
            if (sdp == null) {
                // No SDP found: reject the invitation with a 606 Not Acceptable
                if (logActivated) {
                    sLogger.debug("No SDP found: automatically reject");
                }
                sendFinalResponse(request, Response.SESSION_NOT_ACCEPTABLE);
                return;
            }
            sdp = sdp.toLowerCase();

            // New incoming session invitation
            if (isTagPresent(sdp, "msrp")
                    && SipUtils.isFeatureTagPresent(request, FeatureTags.FEATURE_3GPP_VIDEO_SHARE)
                    && (SipUtils.isFeatureTagPresent(request, FeatureTags.FEATURE_3GPP_IMAGE_SHARE) || SipUtils
                            .isFeatureTagPresent(request, FeatureTags.FEATURE_3GPP_IMAGE_SHARE_RCS2))) {
                // Image sharing
                if (mRcsSettings.isImageSharingSupported()) {
                    if (logActivated) {
                        sLogger.debug("Image content sharing transfer invitation");
                    }
                    mImsModule.getRichcallService().receiveImageSharingInvitation(request,
                            timestamp);
                } else {
                    // Service not supported: reject the invitation with a 603 Decline
                    if (logActivated) {
                        sLogger.debug("Image share service not supported: automatically reject");
                    }
                    sendFinalResponse(request, Response.DECLINE);
                }
            } else if (isTagPresent(sdp, "msrp")
                    && SipUtils.isFeatureTagPresent(request, FeatureTags.FEATURE_OMA_IM)
                    && isTagPresent(sdp, "file-selector")) {
                // File transfer
                if (mRcsSettings.isFileTransferSupported()) {
                    if (logActivated) {
                        sLogger.debug("File transfer invitation");
                    }
                    mImsModule.getInstantMessagingService().receiveMsrpFileTransferInvitation(
                            request, timestamp);
                } else {
                    // Service not supported: reject the invitation with a 603 Decline
                    if (logActivated) {
                        sLogger.debug("File transfer service not supported: automatically reject");
                    }
                    sendFinalResponse(request, Response.DECLINE);
                }
            } else if (isTagPresent(sdp, "msrp")
                    && SipUtils.isFeatureTagPresent(request, FeatureTags.FEATURE_OMA_IM)) {
                // IM service
                if (!mRcsSettings.isImSessionSupported()) {
                    // Service not supported: reject the invitation with a 603 Decline
                    if (logActivated) {
                        sLogger.debug("IM service not supported: automatically reject");
                    }
                    sendFinalResponse(request, Response.DECLINE);
                    return;
                }

                if (ChatUtils.isFileTransferOverHttp(request)) {
                    FileTransferHttpInfoDocument ftHttpInfo = FileTransferUtils.getHttpFTInfo(
                            request, mRcsSettings);
                    if (ftHttpInfo != null) {
                        // HTTP file transfer invitation
                        if (SipUtils.getReferredByHeader(request) != null) {
                            if (logActivated) {
                                sLogger.debug("Single S&F file transfer over HTTP invitation");
                            }
                            mImsModule.getInstantMessagingService()
                                    .receiveStoredAndForwardOneToOneHttpFileTranferInvitation(
                                            request, ftHttpInfo, timestamp);
                        } else {
                            if (logActivated) {
                                sLogger.debug("Single file transfer over HTTP invitation");
                            }
                            mImsModule.getInstantMessagingService()
                                    .receiveOneToOneHttpFileTranferInvitation(request, ftHttpInfo,
                                            timestamp);
                        }
                    } else {
                        // TODO : else return error to Originating side
                        // Malformed XML for FToHTTP: automatically reject with a 606 Not Acceptable
                        if (logActivated) {
                            sLogger.debug("Malformed xml for FToHTTP: automatically reject");
                        }
                        sendFinalResponse(request, Response.SESSION_NOT_ACCEPTABLE);
                    }
                } else {
                    String contentType = request.getContentType();
                    SipUtils.assertContentIsNotNull(contentType, request);
                    if (SipUtils.getAssertedIdentity(request).contains(
                            StoreAndForwardManager.SERVICE_URI)
                            && (!contentType.contains("multipart"))) {
                        // Store & Forward push notifs session
                        if (logActivated) {
                            sLogger.debug("Store & Forward push notifications");
                        }
                        mImsModule.getInstantMessagingService()
                                .receiveStoredAndForwardPushNotifications(request, timestamp);
                    } else if (ChatUtils.isGroupChatInvitation(request)) {
                        // Ad-hoc group chat session
                        if (logActivated) {
                            sLogger.debug("Ad-hoc group chat session invitation");
                        }
                        mImsModule.getInstantMessagingService().receiveAdhocGroupChatSession(
                                request, timestamp);
                    } else if (SipUtils.getReferredByHeader(request) != null) {
                        // Store & Forward push messages session
                        if (logActivated) {
                            sLogger.debug("Store & Forward push messages session");
                        }
                        mImsModule.getInstantMessagingService()
                                .receiveStoredAndForwardPushMessages(request, timestamp);
                    } else {
                        // 1-1 chat session
                        if (logActivated) {
                            sLogger.debug("1-1 chat session invitation");
                        }
                        mImsModule.getInstantMessagingService().receiveOne2OneChatSession(request,
                                timestamp);
                    }
                }
            } else if (isTagPresent(sdp, "rtp")
                    && SipUtils.isFeatureTagPresent(request, FeatureTags.FEATURE_3GPP_VIDEO_SHARE)) {
                // Video streaming
                if (mRcsSettings.isVideoSharingSupported()) {
                    if (logActivated) {
                        sLogger.debug("Video content sharing streaming invitation");
                    }
                    mImsModule.getRichcallService().receiveVideoSharingInvitation(request,
                            timestamp);
                } else {
                    // Service not supported: reject the invitation with a 603 Decline
                    if (logActivated) {
                        sLogger.debug("Video share service not supported: automatically reject");
                    }
                    sendFinalResponse(request, Response.DECLINE);
                }
            } else if (isTagPresent(sdp, "msrp")
                    && SipUtils.isFeatureTagPresent(request, FeatureTags.FEATURE_3GPP_VIDEO_SHARE)
                    && SipUtils.isFeatureTagPresent(request,
                            FeatureTags.FEATURE_RCSE_GEOLOCATION_PUSH)) {
                // Geoloc sharing
                if (mRcsSettings.isGeoLocationPushSupported()) {
                    if (logActivated) {
                        sLogger.debug("Geoloc content sharing transfer invitation");
                    }
                    mImsModule.getRichcallService().receiveGeolocSharingInvitation(request,
                            timestamp);
                } else {
                    // Service not supported: reject the invitation with a 603 Decline
                    if (logActivated) {
                        sLogger.debug("Geoloc share service not supported: automatically reject");
                    }
                    sendFinalResponse(request, Response.DECLINE);
                }
            } else if (SipUtils
                    .isFeatureTagPresent(request, FeatureTags.FEATURE_RCSE_IP_VOICE_CALL)
                    && SipUtils
                            .isFeatureTagPresent(request, FeatureTags.FEATURE_3GPP_IP_VOICE_CALL)) {
                // IP voice call

                // TODO: Add Ipcall support here in future releases
                // Service not supported: reject the invitation with a 603 Decline
                if (logActivated) {
                    sLogger.debug("IP Voice call service not supported: automatically reject");
                }
                sendFinalResponse(request, Response.DECLINE);
            } else if (SipUtils
                    .isFeatureTagPresent(request, FeatureTags.FEATURE_RCSE_IP_VOICE_CALL)
                    && SipUtils
                            .isFeatureTagPresent(request, FeatureTags.FEATURE_3GPP_IP_VOICE_CALL)
                    && SipUtils
                            .isFeatureTagPresent(request, FeatureTags.FEATURE_RCSE_IP_VIDEO_CALL)) {
                // IP video call

                // TODO: Add Ipcall support here in future releases
                // Service not supported: reject the invitation with a 603 Decline
                if (logActivated) {
                    sLogger.debug("IP video call service not supported: automatically reject");
                }
                sendFinalResponse(request, Response.DECLINE);
            } else {
                Intent intent = mIntentMgr.isSipRequestResolved(request);
                if (intent != null) {
                    // Generic SIP session
                    if (isTagPresent(sdp, "msrp")) {
                        if (logActivated) {
                            sLogger.debug("Generic SIP session invitation with MSRP media");
                        }
                        mImsModule.getSipService().receiveMsrpSessionInvitation(intent, request,
                                timestamp);
                    } else if (isTagPresent(sdp, "rtp")) {
                        if (logActivated) {
                            sLogger.debug("Generic SIP session invitation with RTP media");
                        }
                        mImsModule.getSipService().receiveRtpSessionInvitation(intent, request,
                                timestamp);
                    } else {
                        if (logActivated) {
                            sLogger.debug("Media not supported for a generic SIP session");
                        }
                        sendFinalResponse(request, Response.SESSION_NOT_ACCEPTABLE);
                    }
                } else {
                    // Unknown service: reject the invitation with a 403 forbidden
                    if (logActivated) {
                        sLogger.debug("Unknown IMS service: automatically reject");
                    }
                    sendFinalResponse(request, Response.FORBIDDEN, "Unsupported Extension");
                }
            }
        } else if (request.getMethod().equals(Request.MESSAGE)) {
            // MESSAGE received
            if (ChatUtils.isImdnService(request)) {
                // IMDN service
                mImsModule.getInstantMessagingService().receiveMessageDeliveryStatus(request);
            } else if (TermsConditionsService.isTermsRequest(request)) {
                // Terms & conditions service
                mImsModule.getTermsConditionsService().receiveMessage(request);
            } else {
                // Unknown service: reject the message with a 403 Forbidden
                if (logActivated) {
                    sLogger.debug("Unknown IMS service: automatically reject");
                }
                sendFinalResponse(request, Response.FORBIDDEN);
            }
        } else if (request.getMethod().equals(Request.NOTIFY)) {
            // NOTIFY received
            dispatchNotify(request, timestamp);
        } else if (request.getMethod().equals(Request.BYE)) {
            // BYE received

            // Route request to session
            if (session != null) {
                session.receiveBye(request);
            }

            // Send a 200 OK response
            if (logActivated) {
                sLogger.info("Send 200 OK");
            }
            SipResponse response = SipMessageFactory.createResponse(request, Response.OK);
            mImsModule.getSipManager().sendSipResponse(response);
        } else if (request.getMethod().equals(Request.CANCEL)) {
            // CANCEL received

            // Route request to session
            if (session != null) {
                session.receiveCancel(request);
            }
            if (logActivated) {
                sLogger.info("Send 200 OK");
            }
            mImsModule.getSipManager().sendSipResponse(
                    SipMessageFactory.createResponse(request, Response.OK));
        } else if (request.getMethod().equals(Request.UPDATE)) {
            // UPDATE received
            if (session != null) {
                session.receiveUpdate(request);
            }
        } else {
            // Unknown request: : reject the request with a 403 Forbidden
            if (logActivated) {
                sLogger.debug("Unknown request " + request.getMethod());
            }
            sendFinalResponse(request, Response.FORBIDDEN);
        }
    }

    /**
     * Dispatch the received SIP NOTIFY
     * 
     * @param notify SIP request
     * @param timestamp Local timestamp when got SipRequest
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    private void dispatchNotify(SipRequest notify, long timestamp) throws SipPayloadException,
            SipNetworkException {
        // Create 200 OK response
        SipResponse resp = SipMessageFactory.createResponse(notify, Response.OK);

        // Send 200 OK response
        mImsModule.getSipManager().sendSipResponse(resp);

        // Get the event type
        EventHeader eventHeader = (EventHeader) notify.getHeader(EventHeader.NAME);
        if (eventHeader == null) {
            if (sLogger.isActivated()) {
                sLogger.debug("Unknown notification event type");
            }
            return;
        }

        // Dispatch the notification to the corresponding service
        if (eventHeader.getEventType().equalsIgnoreCase("presence.winfo")) {
            // Presence service
            if (mRcsSettings.isSocialPresenceSupported()
                    && mImsModule.getPresenceService().isServiceStarted()) {
                mImsModule.getPresenceService().getWatcherInfoSubscriber()
                        .receiveNotification(notify);
            }
        } else if (eventHeader.getEventType().equalsIgnoreCase("presence")) {
            if (notify.getTo().indexOf("anonymous") != -1) {
                // Capability service
                mImsModule.getCapabilityService().receiveNotification(notify);
            } else {
                // Presence service
                mImsModule.getPresenceService().getPresenceSubscriber().receiveNotification(notify);
            }
        } else if (eventHeader.getEventType().equalsIgnoreCase("conference")) {
            // IM service
            mImsModule.getInstantMessagingService()
                    .receiveConferenceNotification(notify, timestamp);
        } else {
            // Not supported service
            if (sLogger.isActivated()) {
                sLogger.debug("Not supported notification event type");
            }
        }
    }

    /**
     * Test a tag is present or not in SIP message
     * 
     * @param message Message or message part
     * @param tag Tag to be searched
     * @return Boolean
     */
    private boolean isTagPresent(String message, String tag) {
        if ((message != null) && (tag != null) && (message.toLowerCase().indexOf(tag) != -1)) {
            return true;
        }
        return false;
    }

    /**
     * Search the IMS session that corresponds to a given call-ID
     * 
     * @param callId Call-ID
     * @return IMS session
     */
    private ImsServiceSession getImsServiceSession(String callId) {
        for (ImsService service : mImsModule.getImsServices()) {
            ImsServiceSession session = service.getImsServiceSession(callId);
            if (session != null) {
                return session;
            }
        }
        return null;
    }

    /**
     * Send a 100 Trying response to the remote party
     * 
     * @param request SIP request
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    private void send100Trying(SipRequest request) throws SipNetworkException, SipPayloadException {
        mImsModule.getCurrentNetworkInterface().getSipManager()
                .sendSipResponse(SipMessageFactory.createResponse(request, null, Response.TRYING));
    }

    /**
     * Send a final response
     * 
     * @param request SIP request
     * @param code Response code
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    private void sendFinalResponse(SipRequest request, int code) throws SipNetworkException,
            SipPayloadException {
        mImsModule
                .getCurrentNetworkInterface()
                .getSipManager()
                .sendSipResponse(
                        SipMessageFactory.createResponse(request, IdGenerator.getIdentifier(), code));
    }

    /**
     * Send a final response
     * 
     * @param request SIP request
     * @param code Response code
     * @param warning Warning message
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    private void sendFinalResponse(SipRequest request, int code, String warning)
            throws SipNetworkException, SipPayloadException {
        mImsModule
                .getCurrentNetworkInterface()
                .getSipManager()
                .sendSipResponse(
                        SipMessageFactory.createResponse(request, IdGenerator.getIdentifier(),
                                code, warning));
    }

    /**
     * Handle ims dispatch error
     * 
     * @param request
     */
    private void handleImsDispatchError(SipRequest request) {
        final PresenceService service = mImsModule.getPresenceService();
        if (request.getMethod().equals(Request.NOTIFY) && mRcsSettings.isSocialPresenceSupported()
                && service.isServiceStarted()) {
            SubscriptionStateHeader stateHeader = (SubscriptionStateHeader) request
                    .getHeader(SubscriptionStateHeader.NAME);
            if ((stateHeader != null) && stateHeader.getState().equalsIgnoreCase("terminated")) {
                if (sLogger.isActivated()) {
                    sLogger.info("Presence subscription has been terminated by server");
                }
                service.getPresenceSubscriber().terminatedByServer();
            }
        }
    }
}
