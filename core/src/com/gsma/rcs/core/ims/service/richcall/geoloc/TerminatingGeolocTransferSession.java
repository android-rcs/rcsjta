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
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpManager;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.SessionTimerManager;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.richcall.ContentSharingError;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.NetworkRessourceManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.contact.ContactId;

import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

/**
 * Terminating geoloc sharing session (transfer)
 * 
 * @author jexa7410
 */
public class TerminatingGeolocTransferSession extends GeolocTransferSession implements
        MsrpEventListener {
    /**
     * MSRP manager
     */
    private MsrpManager msrpMgr;

    /**
     * The logger
     */
    private final Logger mLogger = Logger.getLogger(getClass().getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param invite Initial INVITE request
     * @param contact Contact Id
     * @param rcsSettings
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public TerminatingGeolocTransferSession(ImsService parent, SipRequest invite,
            ContactId contact, RcsSettings rcsSettings, long timestamp,
            ContactManager contactManager) {
        super(parent, ContentManager.createMmContentFromSdp(invite, rcsSettings), contact,
                rcsSettings, timestamp, contactManager);

        // Create dialog path
        createTerminatingDialogPath(invite);
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (mLogger.isActivated()) {
                mLogger.info("Initiate a new sharing session as terminating");
            }

            send180Ringing(getDialogPath().getInvite(), getDialogPath().getLocalTag());

            // Check if the MIME type is supported
            if (getContent() == null) {
                if (mLogger.isActivated()) {
                    mLogger.debug("MIME type is not supported");
                }

                // Send a 415 Unsupported media type response
                send415Error(getDialogPath().getInvite());

                // Unsupported media type
                handleError(new ContentSharingError(ContentSharingError.UNSUPPORTED_MEDIA_TYPE));
                return;
            }

            Collection<ImsSessionListener> listeners = getListeners();
            ContactId contact = getRemoteContact();
            long timestamp = getTimestamp();
            for (ImsSessionListener listener : listeners) {
                ((GeolocTransferSessionListener) listener).handleSessionInvited(contact, timestamp);
            }

            InvitationStatus answer = waitInvitationAnswer();
            switch (answer) {
                case INVITATION_REJECTED:
                    if (mLogger.isActivated()) {
                        mLogger.debug("Session has been rejected by user");
                    }

                    removeSession();

                    for (ImsSessionListener listener : listeners) {
                        listener.handleSessionRejected(contact,
                                TerminationReason.TERMINATION_BY_USER);
                    }
                    return;

                case INVITATION_TIMEOUT:
                    if (mLogger.isActivated()) {
                        mLogger.debug("Session has been rejected on timeout");
                    }

                    // Ringing period timeout
                    send486Busy(getDialogPath().getInvite(), getDialogPath().getLocalTag());

                    removeSession();

                    for (ImsSessionListener listener : listeners) {
                        listener.handleSessionRejected(contact,
                                TerminationReason.TERMINATION_BY_TIMEOUT);
                    }
                    return;

                case INVITATION_REJECTED_BY_SYSTEM:
                    if (mLogger.isActivated()) {
                        mLogger.debug("Session has been aborted by system");
                    }
                    removeSession();
                    return;

                case INVITATION_CANCELED:
                    if (mLogger.isActivated()) {
                        mLogger.debug("Session has been rejected by remote");
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

                case INVITATION_DELETED:
                    if (mLogger.isActivated()) {
                        mLogger.debug("Session has been deleted");
                    }
                    removeSession();
                    return;

                default:
                    if (mLogger.isActivated()) {
                        mLogger.debug("Unknown invitation answer in run; answer=".concat(String
                                .valueOf(answer)));
                    }
                    return;
            }

            // Parse the remote SDP part
            String remoteSdp = getDialogPath().getInvite().getSdpContent();
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
            int remotePort = mediaDesc.port;

            // Extract the "setup" parameter
            String remoteSetup = "passive";
            MediaAttribute attr4 = mediaDesc.getMediaAttribute("setup");
            if (attr4 != null) {
                remoteSetup = attr4.getValue();
            }
            if (mLogger.isActivated()) {
                mLogger.debug("Remote setup attribute is " + remoteSetup);
            }

            // Set setup mode
            String localSetup = createSetupAnswer(remoteSetup);
            if (mLogger.isActivated()) {
                mLogger.debug("Local setup attribute is " + localSetup);
            }

            // Set local port
            int localMsrpPort;
            if (localSetup.equals("active")) {
                localMsrpPort = 9; // See RFC4145, Page 4
            } else {
                localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort(mRcsSettings);
            }

            // Create the MSRP manager
            String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface()
                    .getNetworkAccess().getIpAddress();
            msrpMgr = new MsrpManager(localIpAddress, localMsrpPort, mRcsSettings);

            // Build SDP part
            String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
            String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
            String sdp = "v=0" + SipUtils.CRLF + "o=- " + ntpTime + " " + ntpTime + " "
                    + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "s=-" + SipUtils.CRLF
                    + "c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "t=0 0"
                    + SipUtils.CRLF + "m=message " + localMsrpPort + " TCP/MSRP *" + SipUtils.CRLF
                    + "a=" + fileSelector + SipUtils.CRLF + "a=" + fileTransferId + SipUtils.CRLF
                    + "a=accept-types:" + getContent().getEncoding() + SipUtils.CRLF + "a=setup:"
                    + localSetup + SipUtils.CRLF + "a=path:" + msrpMgr.getLocalMsrpPath()
                    + SipUtils.CRLF + "a=recvonly" + SipUtils.CRLF;

            // Set the local SDP part in the dialog path
            getDialogPath().setLocalContent(sdp);

            // Test if the session should be interrupted
            if (isInterrupted()) {
                if (mLogger.isActivated()) {
                    mLogger.debug("Session has been interrupted: end of processing");
                }
                return;
            }

            // Create the MSRP server session
            if (localSetup.equals("passive")) {
                // Passive mode: client wait a connection
                msrpMgr.createMsrpServerSession(remotePath, this);

                /* Open the MSRP session */
                msrpMgr.openMsrpSession(GeolocTransferSession.DEFAULT_SO_TIMEOUT);

                /* Send an empty packet */
                sendEmptyDataChunk();
            }

            // Create a 200 OK response
            if (mLogger.isActivated()) {
                mLogger.info("Send 200 OK");
            }
            SipResponse resp = SipMessageFactory.create200OkInviteResponse(getDialogPath(),
                    RichcallService.FEATURE_TAGS_GEOLOC_SHARE, sdp);

            // The signalisation is established
            getDialogPath().sigEstablished();

            // Send response
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager()
                    .sendSipMessageAndWait(resp);

            // Analyze the received response
            if (ctx.isSipAck()) {
                // ACK received
                if (mLogger.isActivated()) {
                    mLogger.info("ACK request received");
                }

                // Create the MSRP client session
                if (localSetup.equals("active")) {
                    String fingerprint = SdpUtils.extractFingerprint(parser, mediaDesc);
                    // Active mode: client should connect
                    msrpMgr.createMsrpClientSession(remoteHost, remotePort, remotePath, this,
                            fingerprint);

                    // Open the MSRP session
                    msrpMgr.openMsrpSession(GeolocTransferSession.DEFAULT_SO_TIMEOUT);

                    // Send an empty packet
                    sendEmptyDataChunk();
                }

                // The session is established
                getDialogPath().sessionEstablished();
                for (ImsSessionListener listener : listeners) {
                    listener.handleSessionStarted(contact);
                }

                // Start session timer
                SessionTimerManager sessionTimerManager = getSessionTimerManager();
                if (sessionTimerManager.isSessionTimerActivated(resp)) {
                    sessionTimerManager.start(SessionTimerManager.UAS_ROLE, getDialogPath()
                            .getSessionExpireTime());
                }
            } else {
                if (mLogger.isActivated()) {
                    mLogger.debug("No ACK received for INVITE");
                }

                // No response received: timeout
                handleError(new ContentSharingError(ContentSharingError.SEND_RESPONSE_FAILED));
            }
        } catch (MsrpException e) {
            handleError(new ContentSharingError(ContentSharingError.SESSION_INITIATION_FAILED, e));
        } catch (IOException e) {
            handleError(new ContentSharingError(ContentSharingError.SESSION_INITIATION_FAILED, e));
        } catch (SipException e) {
            mLogger.error("Failed to send 200OK response!", e);
            handleError(new ContentSharingError(ContentSharingError.SEND_RESPONSE_FAILED, e));
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            mLogger.error("Failed to initiate a new geoloc sharing session as terminating!", e);
            handleError(new ContentSharingError(ContentSharingError.SESSION_INITIATION_FAILED, e));
        }

        if (mLogger.isActivated()) {
            mLogger.debug("End of thread");
        }
    }

    /**
     * Send an empty data chunk
     * 
     * @throws MsrpException
     */
    public void sendEmptyDataChunk() throws MsrpException {
        msrpMgr.sendEmptyChunk();
    }

    /**
     * Data has been transfered
     * 
     * @param msgId Message ID
     */
    public void msrpDataTransfered(String msgId) {
        // Not used in terminating side
    }

    /**
     * Data transfer has been received
     * 
     * @param msgId Message ID
     * @param data Last received data chunk
     * @param mimeType Data mime-type
     */
    public void msrpDataReceived(String msgId, byte[] data, String mimeType) {
        if (mLogger.isActivated()) {
            mLogger.info("Data received");
        }
        ContactId contact = getRemoteContact();
        try {
            // Parse received geoloc info
            String geolocDoc = new String(data, UTF8);
            Geoloc geoloc = ChatUtils.parseGeolocDocument(geolocDoc);

            // Set geoloc
            setGeoloc(geoloc);

            // Geoloc has been transfered
            geolocTransfered();

            boolean initiatedByRemote = isInitiatedByRemote();
            for (int j = 0; j < getListeners().size(); j++) {
                ((GeolocTransferSessionListener) getListeners().get(j)).handleContentTransfered(
                        contact, geoloc, initiatedByRemote);
            }
        } catch (RuntimeException e) {
            // Notify listeners
            for (int j = 0; j < getListeners().size(); j++) {
                ((GeolocTransferSessionListener) getListeners().get(j)).handleSharingError(contact,
                        new ContentSharingError(ContentSharingError.MEDIA_TRANSFER_FAILED));
            }
        }
    }

    /**
     * Data transfer in progress
     * 
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     */
    public void msrpTransferProgress(long currentSize, long totalSize) {
        // Not used
    }

    /**
     * Data transfer in progress
     * 
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     * @param data received data chunk
     * @return True if transfer in progress
     */
    public boolean msrpTransferProgress(long currentSize, long totalSize, byte[] data) {
        // Not used for geolocation sharing
        return true;
    }

    /**
     * Data transfer has been aborted
     */
    public void msrpTransferAborted() {
        if (mLogger.isActivated()) {
            mLogger.info("Data transfer aborted");
        }
    }

    /**
     * Data transfer error
     * 
     * @param msgId Message ID
     * @param error Error code
     * @param typeMsrpChunk Type of MSRP chunk
     */
    public void msrpTransferError(String msgId, String error, TypeMsrpChunk typeMsrpChunk) {
        if (isSessionInterrupted()) {
            return;
        }
        boolean logActivated = mLogger.isActivated();

        if (logActivated) {
            mLogger.info("Data transfer error " + error);
        }

        // Close the media session
        closeMediaSession();

        closeSession(TerminationReason.TERMINATION_BY_SYSTEM);

        ContactId contact = getRemoteContact();
        // Request capabilities to the remote
        getImsService().getImsModule().getCapabilityService().requestContactCapabilities(contact);

        // Remove the current session
        removeSession();

        for (ImsSessionListener listener : getListeners()) {
            ((GeolocTransferSessionListener) listener).handleSharingError(contact,
                    new ContentSharingError(ContentSharingError.MEDIA_TRANSFER_FAILED));
        }
    }

    /**
     * Prepare media session
     */
    public void prepareMediaSession() {
        /* Nothing to do in terminating side */
    }

    /**
     * Open media session
     */
    public void openMediaSession() {
        /* Nothing to do in terminating side */
    }

    /**
     * Start media transfer
     */
    public void startMediaTransfer() {
        /* Nothing to do in terminating side */
    }

    /**
     * Close media session
     */
    public void closeMediaSession() {
        // Close the MSRP session
        if (msrpMgr != null) {
            msrpMgr.closeSession();
        }
        if (mLogger.isActivated()) {
            mLogger.debug("MSRP session has been closed");
        }
    }

    @Override
    public boolean isInitiatedByRemote() {
        return true;
    }
}
