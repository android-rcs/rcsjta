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

package com.gsma.rcs.core.ims.service.sip.streaming;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.rtp.MediaRtpReceiver;
import com.gsma.rcs.core.ims.protocol.rtp.MediaRtpSender;
import com.gsma.rcs.core.ims.protocol.rtp.format.Format;
import com.gsma.rcs.core.ims.protocol.rtp.format.data.DataFormat;
import com.gsma.rcs.core.ims.protocol.rtp.stream.RtpStreamListener;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.sip.GenericSipSession;
import com.gsma.rcs.core.ims.service.sip.SipSessionError;
import com.gsma.rcs.core.ims.service.sip.SipSessionListener;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.NetworkRessourceManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

/**
 * Generic SIP RTP session
 * 
 * @author jexa7410
 */
public abstract class GenericSipRtpSession extends GenericSipSession implements RtpStreamListener {
    /**
     * RTP payload format
     */
    private DataFormat format = new DataFormat();

    /**
     * Local RTP port
     */
    private int localRtpPort = -1;

    /**
     * Data sender
     */
    private DataSender dataSender = new DataSender();

    /**
     * Data receiver
     */
    private DataReceiver dataReceiver = new DataReceiver(this);

    /**
     * RTP receiver
     */
    private MediaRtpReceiver rtpReceiver;

    /**
     * RTP sender
     */
    private MediaRtpSender rtpSender;

    /**
     * Startup flag
     */
    private boolean started = false;

    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(GenericSipRtpSession.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param contact Remote contact Id
     * @param featureTag Feature tag
     * @param rcsSettings
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public GenericSipRtpSession(ImsService parent, ContactId contact, String featureTag,
            RcsSettings rcsSettings, long timestamp, ContactsManager contactManager) {
        super(parent, contact, featureTag, rcsSettings, timestamp, contactManager);

        // Get local port
        localRtpPort = NetworkRessourceManager.generateLocalRtpPort(rcsSettings);

        // Create the RTP sender & receiver
        rtpReceiver = new MediaRtpReceiver(localRtpPort);
        rtpSender = new MediaRtpSender(format, localRtpPort);
    }

    /**
     * Get local port
     * 
     * @return RTP port
     */
    public int getLocalRtpPort() {
        return localRtpPort;
    }

    /**
     * Returns the RTP receiver
     * 
     * @return RTP receiver
     */
    public MediaRtpReceiver getRtpReceiver() {
        return rtpReceiver;
    }

    /**
     * Returns the RTP sender
     * 
     * @return RTP sender
     */
    public MediaRtpSender getRtpSender() {
        return rtpSender;
    }

    /**
     * Returns the RTP format
     * 
     * @return RTP format
     */
    public Format getRtpFormat() {
        return format;
    }

    /**
     * Generate SDP
     * 
     * @return SDP built
     */
    public String generateSdp() {
        String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
        String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
        return "v=0" + SipUtils.CRLF + "o=- " + ntpTime + " " + ntpTime + " "
                + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "s=-" + SipUtils.CRLF
                + "c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "t=0 0"
                + SipUtils.CRLF + "m=application " + localRtpPort + " RTP/AVP "
                + getRtpFormat().getPayload() + SipUtils.CRLF + "a=rtpmap:"
                + getRtpFormat().getPayload() + " " + getRtpFormat().getCodec() + "/90000"
                + SipUtils.CRLF + // TODO: hardcoded value for clock rate and codec
                "a=sendrecv" + SipUtils.CRLF;
    }

    /**
     * Prepare media session
     * 
     * @throws Exception
     */
    public void prepareMediaSession() throws Exception {
        // Parse the remote SDP part
        SdpParser parser = new SdpParser(getDialogPath().getRemoteContent().getBytes(UTF8));
        MediaDescription mediaApp = parser.getMediaDescription("application");
        String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription, mediaApp);
        int remotePort = mediaApp.port;

        // Prepare media
        rtpReceiver.prepareSession(remoteHost, remotePort, dataReceiver, format, this);
        rtpSender.prepareSession(dataSender, remoteHost, remotePort, rtpReceiver.getInputStream(),
                this);
    }

    /**
     * Start media session
     * 
     * @throws Exception
     */
    public void startMediaSession() throws Exception {
        synchronized (this) {
            // Start media
            rtpReceiver.startSession();
            rtpSender.startSession();

            started = true;
        }
    }

    /**
     * Close media session
     */
    public void closeMediaSession() {
        synchronized (this) {
            started = false;

            // Stop media
            rtpSender.stopSession();
            rtpReceiver.stopSession();
        }
    }

    /**
     * Sends a payload in real time
     * 
     * @param content Payload content
     * @return Returns true if sent successfully else returns false
     */
    public boolean sendPlayload(byte[] content) {
        if (started) {
            dataSender.addFrame(content, System.currentTimeMillis());
            return true;
        } else {
            return false;
        }
    }

    /**
     * Invoked when the RTP stream was aborted
     */
    public void rtpStreamAborted() {
        if (isSessionInterrupted()) {
            return;
        }

        if (logger.isActivated()) {
            logger.error("Media has failed: network failure");
        }

        // Close the media session
        closeMediaSession();

        // Terminate session
        terminateSession(TerminationReason.TERMINATION_BY_SYSTEM);

        // Remove the current session
        removeSession();

        ContactId contact = getRemoteContact();
        for (int j = 0; j < getListeners().size(); j++) {
            ((SipSessionListener) getListeners().get(j)).handleSessionError(contact,
                    new SipSessionError(SipSessionError.MEDIA_FAILED));
        }
    }

    /**
     * Receive media data
     * 
     * @param data Data
     */
    public void receiveData(byte[] data) {
        ContactId contact = getRemoteContact();
        for (int j = 0; j < getListeners().size(); j++) {
            ((SipSessionListener) getListeners().get(j)).handleReceiveData(contact, data);
        }
    }

    @Override
    public void startSession() {
        getImsService().getImsModule().getSipService().addSession(this);
        start();
    }

    @Override
    public void removeSession() {
        getImsService().getImsModule().getSipService().removeSession(this);
    }
}
