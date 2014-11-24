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

package com.orangelabs.rcs.core.ims.protocol.rtp.stream;

import com.orangelabs.rcs.core.ims.protocol.rtp.core.RtcpPacketReceiver;
import com.orangelabs.rcs.core.ims.protocol.rtp.core.RtcpPacketTransmitter;
import com.orangelabs.rcs.core.ims.protocol.rtp.core.RtcpSession;
import com.orangelabs.rcs.core.ims.protocol.rtp.core.RtpPacketReceiver;
import com.orangelabs.rcs.core.ims.protocol.rtp.core.RtpPacketTransmitter;
import com.orangelabs.rcs.core.ims.protocol.rtp.event.RtcpEvent;
import com.orangelabs.rcs.core.ims.protocol.rtp.event.RtcpEventListener;
import com.orangelabs.rcs.core.ims.protocol.rtp.util.Buffer;
import com.orangelabs.rcs.utils.logger.Logger;

import java.io.IOException;

/**
 * RTP output stream
 *
 * @author jexa7410
 */
public class RtpOutputStream implements ProcessorOutputStream, RtcpEventListener {
    /**
     * RTCP Socket Timeout
     */
    public static final int RTCP_SOCKET_TIMEOUT = 20000;

    /**
     * Remote address
     */
    private String remoteAddress;

    /**
     * Remote port
     */
    private int remotePort;

    /**
     * Local port
     */
    private int localRtpPort = -1;

    /**
     * RTP receiver
     */
    private RtpPacketReceiver rtpReceiver;

    /**
     * RTCP receiver
     */
    private RtcpPacketReceiver rtcpReceiver;

	/**
	 * RTP transmitter
	 */
	private RtpPacketTransmitter rtpTransmitter;

	/**
	 * RTCP transmitter
	 */
	private RtcpPacketTransmitter rtcpTransmitter;

    /**
     * RTCP Session
     */
    private RtcpSession rtcpSession;

    /**
     * RTCP socket timeout
     */
    private int rtcpSocketTimeout = 0;

    /**
     * RTP stream listener
     */
    private RtpStreamListener rtpStreamListener;

    /**
     * RTP Input stream
     */
    private RtpInputStream rtpInputStream;

    /**
     * The logger
     */
	private final static Logger logger = Logger.getLogger(RtpOutputStream.class.getSimpleName());

    /**
     * Constructor
     *
     * @param remoteAddress Remote address
     * @param remotePort Remote port
     */
    public RtpOutputStream(String remoteAddress, int remotePort) {
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;

        rtcpSession = new RtcpSession(true, 16000);
    }

    /**
     * Constructor
     *
     * @param remoteAddress Remote address
     * @param remotePort Remote port
     * @param rtpInputStream RTP input stream
     */
    public RtpOutputStream(String remoteAddress, int remotePort, RtpInputStream rtpInputStream) {
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.rtpInputStream = rtpInputStream;

        rtcpSession = new RtcpSession(true, 16000);
    }

    /**
     * Constructor
     *
     * @param remoteAddress Remote address
     * @param remotePort Remote port
     * @param rtcpTimeout RTCP timeout
     */
    public RtpOutputStream(String remoteAddress, int remotePort, int localRtpPort, int rtcpTimeout) {
		this.remoteAddress = remoteAddress;
		this.remotePort = remotePort;
        this.localRtpPort = localRtpPort;
        this.rtcpSocketTimeout = rtcpTimeout;

        rtcpSession = new RtcpSession(true, 16000);
    }

    /**
     * Open the output stream
     *
     * @throws Exception
     */
    public void open() throws Exception {
        if (localRtpPort != -1) {
            // Create the RTP receiver
            rtpReceiver = new RtpPacketReceiver(localRtpPort, rtcpSession);
            rtpReceiver.start();

            // Create the RTCP receiver
            rtcpReceiver = new RtcpPacketReceiver(localRtpPort + 1, rtcpSession, rtcpSocketTimeout);
            rtcpReceiver.addRtcpListener(this);
            rtcpReceiver.start();

            // Create the RTP transmitter
            rtpTransmitter = new RtpPacketTransmitter(remoteAddress,
            		remotePort,
            		rtcpSession,
                    rtpReceiver.getConnection());
            
            // Create the RTCP transmitter
            rtcpTransmitter = new RtcpPacketTransmitter(remoteAddress,
            		remotePort + 1,
                    rtcpSession,
                    rtcpReceiver.getConnection());
            rtcpTransmitter.start();
        } else if (rtpInputStream != null) { 
            // Create the RTP transmitter
            rtpTransmitter = new RtpPacketTransmitter(remoteAddress, remotePort, rtcpSession,
                    rtpInputStream.getRtpReceiver().getConnection());
            
            // Create the RTCP transmitter
            rtcpTransmitter = new RtcpPacketTransmitter(remoteAddress, remotePort + 1, rtcpSession,
                    rtpInputStream.getRtcpReceiver().getConnection());
        } else {
            // Create the RTP transmitter
            rtpTransmitter = new RtpPacketTransmitter(remoteAddress, remotePort, rtcpSession);
            
            // Create the RTCP transmitter
            rtcpTransmitter = new RtcpPacketTransmitter(remoteAddress, remotePort + 1, rtcpSession);
        }
    }

    /**
     * Close the output stream
     */
    public void close() {
		try {
			// Close the RTP transmitter
            if (rtpTransmitter != null)
				rtpTransmitter.close();

            // Close the RTCP transmitter
            if (rtcpTransmitter != null)
                rtcpTransmitter.close();

            // Close the RTP receiver
            if (rtpReceiver != null)
                rtpReceiver.close();

            // Close the RTCP receiver
            if (rtcpReceiver != null)
                rtcpReceiver.close();

            // Remove rtpStreamListener
            rtpStreamListener = null;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't close correctly RTP ressources", e);
			}
		}
	}

    /**
     * Write to the stream without blocking
     *
     * @param buffer Input buffer
     * @throws IOException
     */
    public void write(Buffer buffer) throws IOException {
		rtpTransmitter.sendRtpPacket(buffer);
    }

    @Override
    public void receiveRtcpEvent(RtcpEvent event) {
        // Nothing to do 
    }

    @Override
    public void connectionTimeout() {
        if (rtpStreamListener != null) {
            rtpStreamListener.rtpStreamAborted();
        }
    }

    /**
     * Adds the RTP stream listener
     *
     * @param rtpStreamListener
     */
    public void addRtpStreamListener(RtpStreamListener rtpStreamListener) {
        this.rtpStreamListener = rtpStreamListener;
    }
}
