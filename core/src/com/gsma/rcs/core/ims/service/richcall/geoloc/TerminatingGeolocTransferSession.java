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

package com.gsma.rcs.core.ims.service.richcall.geoloc;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpManager;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.SessionTimerManager;
import com.gsma.rcs.core.ims.service.capability.CapabilityService;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.richcall.ContentSharingError;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.NetworkRessourceManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Collection;
import java.util.Vector;

/**
 * Terminating geoloc sharing session (transfer)
 * 
 * @author jexa7410
 */
public class TerminatingGeolocTransferSession extends GeolocTransferSession implements
        MsrpEventListener {

    private MsrpManager msrpMgr;

    private static final Logger sLogger = Logger.getLogger(TerminatingGeolocTransferSession.class
            .getName());

    /**
     * Constructor
     * 
     * @param parent Richcall service
     * @param invite Initial INVITE request
     * @param contact Contact Id
     * @param rcsSettings The RCS settings accessor
     * @param timestamp Local timestamp for the session
     * @param contactManager The contact manager accessor
     * @param capabilityService The capability service
     * @throws PayloadException
     */
    public TerminatingGeolocTransferSession(RichcallService parent, SipRequest invite,
            ContactId contact, RcsSettings rcsSettings, long timestamp,
            ContactManager contactManager, CapabilityService capabilityService)
            throws PayloadException {
        super(parent, ContentManager.createMmContentFromSdp(invite, rcsSettings), contact,
                rcsSettings, timestamp, contactManager, capabilityService);
        createTerminatingDialogPath(invite);
    }

    @Override
    public void run() {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Initiate a new sharing session as terminating");
            }
            SipDialogPath dialogPath = getDialogPath();
            send180Ringing(dialogPath.getInvite(), dialogPath.getLocalTag());

            if (getContent() == null) {
                if (sLogger.isActivated()) {
                    sLogger.debug("MIME type is not supported");
                }
                send415Error(dialogPath.getInvite());
                handleError(new ContentSharingError(ContentSharingError.UNSUPPORTED_MEDIA_TYPE));
                return;
            }

            Collection<ImsSessionListener> listeners = getListeners();
            ContactId contact = getRemoteContact();
            long timestamp = getTimestamp();
            for (ImsSessionListener listener : listeners) {
                ((GeolocTransferSessionListener) listener).onInvitationReceived(contact, timestamp);
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
                        listener.onSessionRejected(contact, TerminationReason.TERMINATION_BY_USER);
                    }
                    return;

                case INVITATION_TIMEOUT:
                    if (sLogger.isActivated()) {
                        sLogger.debug("Session has been rejected on timeout");
                    }
                    // Ringing period timeout
                    send486Busy(dialogPath.getInvite(), dialogPath.getLocalTag());
                    removeSession();
                    for (ImsSessionListener listener : listeners) {
                        listener.onSessionRejected(contact,
                                TerminationReason.TERMINATION_BY_TIMEOUT);
                    }
                    return;

                case INVITATION_REJECTED_BY_SYSTEM:
                    if (sLogger.isActivated()) {
                        sLogger.debug("Session has been aborted by system");
                    }
                    removeSession();
                    return;

                case INVITATION_CANCELED:
                    if (sLogger.isActivated()) {
                        sLogger.debug("Session has been rejected by remote");
                    }
                    removeSession();
                    for (ImsSessionListener listener : listeners) {
                        listener.onSessionRejected(contact, TerminationReason.TERMINATION_BY_REMOTE);
                    }
                    return;

                case INVITATION_ACCEPTED:
                    setSessionAccepted();
                    for (ImsSessionListener listener : listeners) {
                        listener.onSessionAccepting(contact);
                    }
                    break;

                case INVITATION_DELETED:
                    if (sLogger.isActivated()) {
                        sLogger.debug("Session has been deleted");
                    }
                    removeSession();
                    return;

                default:
                    throw new IllegalArgumentException(new StringBuilder(
                            "Unknown invitation answer in run; answer=").append(answer).toString());
            }

            // Parse the remote SDP part
            final SipRequest invite = dialogPath.getInvite();
            String remoteSdp = invite.getSdpContent();
            SipUtils.assertContentIsNotNull(remoteSdp, invite);
            SdpParser parser = new SdpParser(remoteSdp.getBytes(UTF8));
            Vector<MediaDescription> media = parser.getMediaDescriptions();
            MediaDescription mediaDesc = media.elementAt(0);
            MediaAttribute attr1 = mediaDesc.getMediaAttribute("file-selector");
            String fileSelector = attr1.getName() + ":" + attr1.getValue();
            MediaAttribute attr2 = mediaDesc.getMediaAttribute("file-transfer-id");
            String fileTransferId = attr2.getName() + ":" + attr2.getValue();
            MediaAttribute attr3 = mediaDesc.getMediaAttribute("path");
            String remotePath = attr3.getValue();
            String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription, mediaDesc);
            int remotePort = mediaDesc.mPort;

            // Extract the "setup" parameter
            String remoteSetup = "passive";
            MediaAttribute attr4 = mediaDesc.getMediaAttribute("setup");
            if (attr4 != null) {
                remoteSetup = attr4.getValue();
            }
            if (sLogger.isActivated()) {
                sLogger.debug("Remote setup attribute is ".concat(remoteSetup));
            }
            String localSetup = createSetupAnswer(remoteSetup);
            if (sLogger.isActivated()) {
                sLogger.debug("Local setup attribute is " + localSetup);
            }
            int localMsrpPort;
            if (localSetup.equals("active")) {
                localMsrpPort = 9; // See RFC4145, Page 4
            } else {
                localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort(mRcsSettings);
            }
            String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface()
                    .getNetworkAccess().getIpAddress();
            msrpMgr = new MsrpManager(localIpAddress, localMsrpPort, mRcsSettings);
            String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
            String ipAddress = dialogPath.getSipStack().getLocalIpAddress();
            String sdp = "v=0" + SipUtils.CRLF + "o=- " + ntpTime + " " + ntpTime + " "
                    + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "s=-" + SipUtils.CRLF
                    + "c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "t=0 0"
                    + SipUtils.CRLF + "m=message " + localMsrpPort + " TCP/MSRP *" + SipUtils.CRLF
                    + "a=" + fileSelector + SipUtils.CRLF + "a=" + fileTransferId + SipUtils.CRLF
                    + "a=accept-types:" + getContent().getEncoding() + SipUtils.CRLF + "a=setup:"
                    + localSetup + SipUtils.CRLF + "a=path:" + msrpMgr.getLocalMsrpPath()
                    + SipUtils.CRLF + "a=recvonly" + SipUtils.CRLF;
            dialogPath.setLocalContent(sdp);

            if (isInterrupted()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Session has been interrupted: end of processing");
                }
                return;
            }
            if (sLogger.isActivated()) {
                sLogger.info("Send 200 OK");
            }
            SipResponse resp = SipMessageFactory.create200OkInviteResponse(dialogPath,
                    RichcallService.FEATURE_TAGS_GEOLOC_SHARE, sdp);
            dialogPath.setSigEstablished();
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager()
                    .sendSipMessage(resp);

            if (localSetup.equals("passive")) {
                // Passive mode: client wait a connection
                msrpMgr.createMsrpServerSession(remotePath, this);
                msrpMgr.openMsrpSession(GeolocTransferSession.DEFAULT_SO_TIMEOUT);
            }
            getImsService().getImsModule().getSipManager().waitResponse(ctx);
            if (isInterrupted()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Session has been interrupted: end of processing");
                }
                return;
            }
            if (ctx.isSipAck()) {
                if (sLogger.isActivated()) {
                    sLogger.info("ACK request received");
                }
                if (localSetup.equals("active")) {
                    String fingerprint = SdpUtils.extractFingerprint(parser, mediaDesc);
                    // Active mode: client should connect
                    msrpMgr.createMsrpClientSession(remoteHost, remotePort, remotePath, this,
                            fingerprint);
                    msrpMgr.openMsrpSession(GeolocTransferSession.DEFAULT_SO_TIMEOUT);
                    sendEmptyDataChunk();
                }
                dialogPath.setSessionEstablished();
                for (ImsSessionListener listener : listeners) {
                    listener.onSessionStarted(contact);
                }
                SessionTimerManager sessionTimerManager = getSessionTimerManager();
                if (sessionTimerManager.isSessionTimerActivated(resp)) {
                    sessionTimerManager.start(SessionTimerManager.UAS_ROLE,
                            dialogPath.getSessionExpireTime());
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.debug("No ACK received for INVITE");
                }
                handleError(new ContentSharingError(ContentSharingError.SEND_RESPONSE_FAILED));
            }

        } catch (PayloadException e) {
            sLogger.error("Failed to send 200OK response!", e);
            handleError(new ContentSharingError(ContentSharingError.SEND_RESPONSE_FAILED, e));

        } catch (NetworkException e) {
            handleError(new ContentSharingError(ContentSharingError.SEND_RESPONSE_FAILED, e));

        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to initiate a new geoloc sharing session as terminating!", e);
            handleError(new ContentSharingError(ContentSharingError.SESSION_INITIATION_FAILED, e));
        }
        if (sLogger.isActivated()) {
            sLogger.debug("End of thread");
        }
    }

    /**
     * Send an empty data chunk
     * 
     * @throws NetworkException
     */
    public void sendEmptyDataChunk() throws NetworkException {
        msrpMgr.sendEmptyChunk();
    }

    @Override
    public void msrpDataTransferred(String msgId) {
        // Not used in terminating side
    }

    @Override
    public void receiveMsrpData(String msgId, byte[] data, String mimeType) throws PayloadException {
        if (sLogger.isActivated()) {
            sLogger.info("Data received");
        }
        ContactId contact = getRemoteContact();
        String geolocDoc = new String(data, UTF8);
        Geoloc geoloc = ChatUtils.parseGeolocDocument(geolocDoc);
        setGeoloc(geoloc);
        setGeolocTransferred();
        boolean initiatedByRemote = isInitiatedByRemote();
        for (ImsSessionListener listener : getListeners()) {
            ((GeolocTransferSessionListener) listener).onContentTransferred(contact, geoloc,
                    initiatedByRemote);
        }
    }

    @Override
    public void msrpTransferProgress(long currentSize, long totalSize) {
        // Not used
    }

    @Override
    public boolean msrpTransferProgress(long currentSize, long totalSize, byte[] data) {
        // Not used for geolocation sharing
        return true;
    }

    @Override
    public void msrpTransferAborted() {
        if (sLogger.isActivated()) {
            sLogger.info("Data transfer aborted");
        }
    }

    @Override
    public void msrpTransferError(String msgId, String error, TypeMsrpChunk typeMsrpChunk) {
        try {
            if (isSessionInterrupted()) {
                return;
            }
            boolean logActivated = sLogger.isActivated();

            if (logActivated) {
                sLogger.info("Data transfer error ".concat(error));
            }
            closeMediaSession();
            closeSession(TerminationReason.TERMINATION_BY_SYSTEM);

            ContactId contact = getRemoteContact();
            mCapabilityService.requestContactCapabilities(contact);
            removeSession();

            if (isGeolocTransferred()) {
                return;
            }
            for (ImsSessionListener listener : getListeners()) {
                ((GeolocTransferSessionListener) listener).onSharingError(contact,
                        new ContentSharingError(ContentSharingError.MEDIA_TRANSFER_FAILED));
            }

        } catch (PayloadException e) {
            sLogger.error(
                    new StringBuilder("Failed to handle msrp error").append(error)
                            .append(" for message ").append(msgId).toString(), e);

        } catch (NetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }

        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error(
                    new StringBuilder("Failed to handle msrp error").append(error)
                            .append(" for message ").append(msgId).toString(), e);
        }
    }

    @Override
    public void prepareMediaSession() {
        /* Nothing to do in terminating side */
    }

    @Override
    public void openMediaSession() {
        /* Nothing to do in terminating side */
    }

    @Override
    public void startMediaTransfer() {
        /* Nothing to do in terminating side */
    }

    @Override
    public void closeMediaSession() {
        // Close the MSRP session
        if (msrpMgr != null) {
            msrpMgr.closeSession();
        }
        if (sLogger.isActivated()) {
            sLogger.debug("MSRP session has been closed");
        }
    }

    @Override
    public boolean isInitiatedByRemote() {
        return true;
    }
}
