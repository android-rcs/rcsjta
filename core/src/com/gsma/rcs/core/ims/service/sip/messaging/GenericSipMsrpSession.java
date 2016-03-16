/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 *      http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.sip.messaging;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpManager;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.SessionActivityManager;
import com.gsma.rcs.core.ims.service.sip.EnrichCallingService;
import com.gsma.rcs.core.ims.service.sip.GenericSipSession;
import com.gsma.rcs.core.ims.service.sip.SipService;
import com.gsma.rcs.core.ims.service.sip.SipSessionError;
import com.gsma.rcs.core.ims.service.sip.SipSessionListener;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.NetworkRessourceManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Generic SIP MSRP session
 *
 * @author jexa7410
 */
public abstract class GenericSipMsrpSession extends GenericSipSession implements MsrpEventListener {

    private final static String MIME_TYPE = "text/plain";

    private MsrpManager mMsrpMgr;

    private int mMaxMsgSize;

    private boolean mFlushRequested = false;

    private final SessionActivityManager mActivityMgr;

    private final Set<String> mTxPendingMessageIds;

    private final static Logger sLogger = Logger.getLogger(GenericSipMsrpSession.class
            .getSimpleName());

    /**
     * Constructor
     *
     * @param parent SIP service
     * @param contact Remote contact Id
     * @param featureTag Feature tag
     * @param rcsSettings RCS settings
     * @param timestamp Local timestamp for the session
     * @param contactManager Contact manager
     */
    public GenericSipMsrpSession(SipService parent, ContactId contact, String featureTag,
            RcsSettings rcsSettings, long timestamp, ContactManager contactManager) {
        super(parent, contact, featureTag, rcsSettings, timestamp, contactManager);

        mMaxMsgSize = rcsSettings.getMaxMsrpLengthForExtensions();
        mTxPendingMessageIds = new HashSet<>();
        /* Define the idle timeout depending of the type of service */
        long timeout;
        if (EnrichCallingService.CALL_COMPOSER_FEATURE_TAG.equals(featureTag)) {
            timeout = mRcsSettings.getCallComposerInactivityTimeout();
        } else {
            timeout = mRcsSettings.getChatIdleDuration();
        }
        mActivityMgr = new SessionActivityManager(this, timeout);

        /* Create the MSRP manager */
        int localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort(rcsSettings);
        String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface()
                .getNetworkAccess().getIpAddress();
        mMsrpMgr = new MsrpManager(localIpAddress, localMsrpPort, rcsSettings);
    }

    /**
     * Returns the max message size
     *
     * @return Max message size
     */
    public int getMaxMessageSize() {
        return mMaxMsgSize;
    }

    /**
     * Returns the MSRP manager
     *
     * @return MSRP manager
     */
    public MsrpManager getMsrpMgr() {
        return mMsrpMgr;
    }

    /**
     * Returns the session activity manager
     *
     * @return Activity manager
     */
    public SessionActivityManager getActivityManager() {
        return mActivityMgr;
    }

    /**
     * Generate SDP
     *
     * @param setup Setup mode
     * @param acceptTypes Accept-types related to exchanged messages
     * @param acceptWrappedTypes Accept-wrapped-types related to exchanged messages
     * @return SDP built
     */
    public String generateSdp(String setup, String[] acceptTypes, String[] acceptWrappedTypes) {
        int msrpPort;
        if ("active".equals(setup)) {
            msrpPort = 9; /* See RFC4145, Page 4 */
        } else {
            msrpPort = getMsrpMgr().getLocalMsrpPort();
        }

        String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
        String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();

        String addr = SdpUtils.formatAddressType(ipAddress);
        StringBuilder sdp = new StringBuilder("v=0").append(SipUtils.CRLF).append("o=- ")
                .append(ntpTime).append(" ").append(ntpTime).append(" ").append(addr)
                .append(SipUtils.CRLF).append("s=-").append(SipUtils.CRLF).append("c=")
                .append(addr).append(SipUtils.CRLF).append("t=0 0").append(SipUtils.CRLF)
                .append("m=message ").append(msrpPort).append(" ")
                .append(getMsrpMgr().getLocalSocketProtocol()).append(" *").append(SipUtils.CRLF)
                .append("a=setup:").append(setup).append(SipUtils.CRLF).append("a=path:")
                .append(getMsrpMgr().getLocalMsrpPath()).append(SipUtils.CRLF)
                .append("a=max-size:").append(getMaxMessageSize()).append(SipUtils.CRLF);

        if (acceptTypes != null && acceptTypes.length > 0) {
            StringBuilder types = new StringBuilder();
            types.append(acceptTypes[0]);
            for (int i = 1; i < acceptTypes.length; i++) {
                types.append(" ").append(acceptTypes[i]);
            }
            sdp.append("a=accept-types:").append(types).append(SipUtils.CRLF);
        } else {
            sdp.append("a=accept-types:").append(GenericSipMsrpSession.MIME_TYPE)
                    .append(SipUtils.CRLF);
        }

        if (acceptWrappedTypes != null && acceptWrappedTypes.length > 0) {
            StringBuilder types = new StringBuilder();
            types.append(acceptWrappedTypes[0]);
            for (int i = 1; i < acceptWrappedTypes.length; i++) {
                types.append(" ").append(acceptWrappedTypes[i]);
            }
            sdp.append("a=accept-wrapped-types:").append(types).append(SipUtils.CRLF);
        }

        sdp.append("a=sendrecv").append(SipUtils.CRLF);

        return sdp.toString();
    }

    @Override
    public void prepareMediaSession() {
        /* Parse the remote SDP part */
        SdpParser parser = new SdpParser(getDialogPath().getRemoteContent().getBytes(UTF8));
        Vector<MediaDescription> media = parser.getMediaDescriptions();
        MediaDescription mediaDesc = media.elementAt(0);
        MediaAttribute attr = mediaDesc.getMediaAttribute("path");
        String remoteMsrpPath = attr.getValue();
        String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription, mediaDesc);
        int remotePort = mediaDesc.mPort;

        /* Create the MSRP session */
        MsrpSession session = getMsrpMgr().createMsrpClientSession(remoteHost, remotePort,
                remoteMsrpPath, this, null);
        session.setFailureReportOption(true);
        session.setSuccessReportOption(false);
    }

    @Override
    public void startMediaTransfer() {
        // Not to be used here
    }

    @Override
    public void openMediaSession() throws NetworkException, PayloadException {
        getMsrpMgr().openMsrpSession();
    }

    @Override
    public void closeMediaSession() {
        getActivityManager().stop();
        if (mMsrpMgr != null) {
            mMsrpMgr.closeSession();
            if (sLogger.isActivated()) {
                sLogger.debug("MSRP session has been closed");
            }
        }
    }

    @Override
    public void handleInactivityEvent() throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("Session inactivity event");
        }
        terminateSession(TerminationReason.TERMINATION_BY_INACTIVITY);
    }

    @Override
    public void handle200OK(SipResponse resp) throws PayloadException, NetworkException,
            FileAccessException {
        super.handle200OK(resp);
        getActivityManager().start();
    }

    /**
     * Sends a message in real time
     *
     * @param content Message content
     * @param contentType Message content type
     * @throws NetworkException
     */
    public void sendMessage(byte[] content, String contentType) throws NetworkException {
        ByteArrayInputStream stream = new ByteArrayInputStream(content);
        String msgId = IdGenerator.getIdentifier().replace('_', '-');
        mMsrpMgr.sendChunks(stream, msgId, contentType, content.length, TypeMsrpChunk.Unknown);
        mTxPendingMessageIds.add(msgId);
    }

    /**
     * Flush messages
     */
    public void flushMessages() {
        if (sLogger.isActivated()) {
            sLogger.info("Request messages flush");
        }
        if (mTxPendingMessageIds.isEmpty()) {
            onMessagesFlushed();
        } else {
            mFlushRequested = true;
        }
    }

    private void onMessagesFlushed() {
        mFlushRequested = false;
        ContactId contact = getRemoteContact();
        for (ImsSessionListener listener : getListeners()) {
            ((SipSessionListener) listener).onDataFlushed(contact);
        }
    }

    @Override
    public void msrpDataTransferred(String msgId) {
        if (sLogger.isActivated()) {
            sLogger.info("Data transferred");
        }
        mActivityMgr.updateActivity();
        mTxPendingMessageIds.remove(msgId);
        if (mFlushRequested && mTxPendingMessageIds.isEmpty()) {
            onMessagesFlushed();
        }
    }

    @Override
    public void receiveMsrpData(String msgId, byte[] data, String mimeType) {
        if (sLogger.isActivated()) {
            sLogger.info("Data received (type " + mimeType + ")");
        }
        mActivityMgr.updateActivity();
        if (data == null || data.length == 0) {
            // By-pass empty data
            if (sLogger.isActivated()) {
                sLogger.debug("By-pass received empty data");
            }
            return;
        }
        ContactId contact = getRemoteContact();
        for (ImsSessionListener listener : getListeners()) {
            ((SipSessionListener) listener).onDataReceived(contact, data, mimeType);
        }
    }

    @Override
    public void msrpTransferProgress(long currentSize, long totalSize) {
        // Not used here
    }

    @Override
    public boolean msrpTransferProgress(long currentSize, long totalSize, byte[] data) {
        // Not used here
        return false;
    }

    @Override
    public void msrpTransferAborted() {
        // Not used here
    }

    @Override
    public void msrpTransferError(String msgId, String error, TypeMsrpChunk typeMsrpChunk) {
        if (isSessionInterrupted()) {
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.info("Data transfer error " + error);
        }
        ContactId contact = getRemoteContact();
        for (ImsSessionListener listener : getListeners()) {
            ((SipSessionListener) listener).onSessionError(contact, new SipSessionError(
                    SipSessionError.MEDIA_FAILED, error));
        }
        mTxPendingMessageIds.remove(msgId);
        if (mFlushRequested && mTxPendingMessageIds.isEmpty()) {
            onMessagesFlushed();
        }
    }

    @Override
    public void startSession() {
        getSipService().addSession(this);
        start();
    }

    @Override
    public void removeSession() {
        getSipService().removeSession(this);
    }
}
