/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.rcs.core.ims.protocol.msrp;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.InetAddressUtils;
import com.gsma.rcs.utils.logger.Logger;

import java.io.InputStream;
import java.util.Vector;

/**
 * MSRP manager
 * 
 * @author jexa7410
 */
public class MsrpManager {

    private String mLocalMsrpAddress;

    private int mLocalMsrpPort;

    private MsrpSession mMsrpSession;

    private long mSessionId;

    private boolean mSecured = false;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param localMsrpAddress Local MSRP address
     * @param localMsrpPort Local MSRP port
     * @param rcsSettings RCS settings accessor
     */
    public MsrpManager(String localMsrpAddress, int localMsrpPort, RcsSettings rcsSettings) {
        mLocalMsrpAddress = localMsrpAddress;
        mLocalMsrpPort = localMsrpPort;
        mSessionId = System.currentTimeMillis();
        mRcsSettings = rcsSettings;
    }

    /**
     * Constructor
     * 
     * @param localMsrpAddress Local MSRP address
     * @param localMsrpPort Local MSRP port
     * @param service ImsService
     * @param rcsSettings RCS settings accessor
     */
    public MsrpManager(String localMsrpAddress, int localMsrpPort, ImsService service,
            RcsSettings rcsSettings) {
        this(localMsrpAddress, localMsrpPort, rcsSettings);
        ImsModule imsModule = service.getImsModule();
        if (imsModule.isConnectedToWifiAccess()) {
            mSecured = rcsSettings.isSecureMsrpOverWifi();

        } else if (imsModule.isConnectedToMobileAccess()) {
            mSecured = rcsSettings.isSecureMsrpOverMobile();
        }
    }

    /**
     * Returns the local MSRP port
     * 
     * @return Port number
     */
    public int getLocalMsrpPort() {
        return mLocalMsrpPort;
    }

    /**
     * Get the local socket protocol path
     * 
     * @return Protocol
     */
    public String getLocalSocketProtocol() {
        if (mSecured) {
            return MsrpConstants.SOCKET_MSRP_SECURED_PROTOCOL;
        }
        return MsrpConstants.SOCKET_MSRP_PROTOCOL;
    }

    /**
     * Get the local MSRP path
     * 
     * @return MSRP path
     */
    public String getLocalMsrpPath() {
        if (InetAddressUtils.isIPv6Address(mLocalMsrpAddress)) {
            return getMsrpProtocol() + "://[" + mLocalMsrpAddress + "]:" + mLocalMsrpPort + "/"
                    + mSessionId + ";tcp";
        }
        return getMsrpProtocol() + "://" + mLocalMsrpAddress + ":" + mLocalMsrpPort + "/"
                + mSessionId + ";tcp";
    }

    /**
     * Get the MSRP protocol
     * 
     * @return MSRP protocol
     */
    public String getMsrpProtocol() {
        if (mSecured) {
            return MsrpConstants.MSRP_SECURED_PROTOCOL;
        }
        return MsrpConstants.MSRP_PROTOCOL;
    }

    /**
     * Return the MSRP session
     * 
     * @return MSRP session
     */
    public MsrpSession getMsrpSession() {
        return mMsrpSession;
    }

    /**
     * Is secured
     * 
     * @return Boolean
     */
    public boolean isSecured() {
        return mSecured;
    }

    /**
     * Set secured
     * 
     * @param flag Boolean flag
     */
    public void setSecured(boolean flag) {
        mSecured = flag;
    }

    /**
     * Open the MSRP session
     * 
     * @throws NetworkException
     * @throws PayloadException
     */
    public void openMsrpSession() throws NetworkException, PayloadException {
        mMsrpSession.getConnection().open();
    }

    /**
     * Open the connection with SO_TIMEOUT on the socket
     * 
     * @param timeout Timeout value (in milliseconds)
     * @throws NetworkException
     * @throws PayloadException
     */
    public void openMsrpSession(long timeout) throws NetworkException, PayloadException {
        mMsrpSession.getConnection().open(timeout);
    }

    /**
     * Create either an MSRP client or server connection depending on media attribute "setup" in the
     * remote SDP answer.
     * 
     * @param sdp remote SDP answer
     * @param listener MsrpEventListener
     * @return MsrpSession
     */
    public MsrpSession createMsrpSession(byte[] sdp, MsrpEventListener listener) {
        SdpParser parser = new SdpParser(sdp);

        Vector<MediaDescription> media = parser.getMediaDescriptions();
        MediaDescription mediaDesc = media.elementAt(0);
        MediaAttribute pathAttribute = mediaDesc.getMediaAttribute("path");
        String remoteMsrpPath = pathAttribute.getValue();

        // Create the MSRP session
        MsrpSession session;
        MediaAttribute setupAttribute = mediaDesc.getMediaAttribute("setup");
        String setup = null;
        if (setupAttribute != null) {
            setup = setupAttribute.getValue();
        } else {
            logger.error("Media attribute \"setup\" is missing!");
            logger.warn("media=" + mediaDesc.toString());
            if (mediaDesc.mMediaAttributes != null)
                for (MediaAttribute attribute : mediaDesc.mMediaAttributes) {
                    logger.warn("attribute key=" + attribute.getName() + " value="
                            + attribute.getValue());
                }

        }
        // if remote peer is active this client needs to be passive (i.e. act as server)
        if ("active".equalsIgnoreCase(setup)) {
            session = createMsrpServerSession(remoteMsrpPath, listener);
        } else {
            String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription, mediaDesc);
            int remotePort = mediaDesc.mPort;
            String fingerprint = SdpUtils.extractFingerprint(parser, mediaDesc);
            session = createMsrpClientSession(remoteHost, remotePort, remoteMsrpPath, listener,
                    fingerprint);
        }

        return session;
    }

    /**
     * Create a MSRP client session
     * 
     * @param remoteHost Remote host
     * @param remotePort Remote port
     * @param remoteMsrpPath Remote MSRP path
     * @param listener Event listener
     * @param fingerprint the finger print
     * @return Created session
     */
    public MsrpSession createMsrpClientSession(String remoteHost, int remotePort,
            String remoteMsrpPath, MsrpEventListener listener, String fingerprint) {
        if (logger.isActivated()) {
            logger.info("Create MSRP client end point at " + remoteHost +
                    ":" + remotePort);
        }
        /* Create a new MSRP session */
        mMsrpSession = new MsrpSession(mRcsSettings);
        mMsrpSession.setFrom(getLocalMsrpPath());
        mMsrpSession.setTo(remoteMsrpPath);
        /* Create a MSRP client connection */
        MsrpConnection connection = new MsrpClientConnection(mMsrpSession, remoteHost, remotePort,
                mSecured, fingerprint);
        mMsrpSession.setConnection(connection);
        mMsrpSession.addMsrpEventListener(listener);
        return mMsrpSession;
    }

    /**
     * Create a MSRP server session
     * 
     * @param remoteMsrpPath Remote MSRP path
     * @param listener Event listener
     * @return Created session
     */
    public MsrpSession createMsrpServerSession(String remoteMsrpPath, MsrpEventListener listener) {
        if (logger.isActivated()) {
            logger.info("Create MSRP server end point at " + mLocalMsrpPort);
        }

        // Create a MSRP session
        mMsrpSession = new MsrpSession(mRcsSettings);
        mMsrpSession.setFrom(getLocalMsrpPath());
        mMsrpSession.setTo(remoteMsrpPath);

        // Create a MSRP server connection
        MsrpConnection connection = new MsrpServerConnection(mMsrpSession, mLocalMsrpPort);

        // Associate the connection to the session
        mMsrpSession.setConnection(connection);

        // Add event listener
        mMsrpSession.addMsrpEventListener(listener);

        // Return the created session
        return mMsrpSession;
    }

    /**
     * Send data chunks
     * 
     * @param inputStream Input stream
     * @param msgId Message ID
     * @param contentType Content type
     * @param contentSize Content size
     * @param typeMsrpChunk Type of MSRP chunk
     * @throws NetworkException
     */
    public void sendChunks(InputStream inputStream, String msgId, String contentType,
            long contentSize, TypeMsrpChunk typeMsrpChunk) throws NetworkException {
        mMsrpSession.sendChunks(inputStream, msgId, contentType, contentSize, typeMsrpChunk);
    }

    /**
     * Send an empty chunk
     * 
     * @throws NetworkException
     */
    public void sendEmptyChunk() throws NetworkException {
        mMsrpSession.sendEmptyChunk();
    }

    /**
     * Close the MSRP session
     */
    public synchronized void closeSession() {
        if (mMsrpSession != null) {
            if (logger.isActivated()) {
                logger.info("Close the MSRP session");
            }
            mMsrpSession.close();
            mMsrpSession = null;
        }
    }

    /**
     * Is established
     * 
     * @return true If the empty packet was sent successfully
     */
    public boolean isEstablished() {
        return mMsrpSession != null && mMsrpSession.isEstablished();
    }

}
