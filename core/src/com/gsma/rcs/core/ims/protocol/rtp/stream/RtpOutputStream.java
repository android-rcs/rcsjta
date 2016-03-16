/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.core.ims.protocol.rtp.stream;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.rtp.core.RtcpPacketReceiver;
import com.gsma.rcs.core.ims.protocol.rtp.core.RtcpPacketTransmitter;
import com.gsma.rcs.core.ims.protocol.rtp.core.RtcpSession;
import com.gsma.rcs.core.ims.protocol.rtp.core.RtpPacketReceiver;
import com.gsma.rcs.core.ims.protocol.rtp.core.RtpPacketTransmitter;
import com.gsma.rcs.core.ims.protocol.rtp.event.RtcpEvent;
import com.gsma.rcs.core.ims.protocol.rtp.event.RtcpEventListener;
import com.gsma.rcs.core.ims.protocol.rtp.util.Buffer;
import com.gsma.rcs.utils.CloseableUtils;

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
    private String mRemoteAddress;

    /**
     * Remote port
     */
    private int mRemotePort;

    /**
     * Local port
     */
    private int mLocalRtpPort = -1;

    /**
     * RTP receiver
     */
    private RtpPacketReceiver mRtpReceiver;

    /**
     * RTCP receiver
     */
    private RtcpPacketReceiver mRtcpReceiver;

    /**
     * RTP transmitter
     */
    private RtpPacketTransmitter mRtpTransmitter;

    /**
     * RTCP transmitter
     */
    private RtcpPacketTransmitter mRtcpTransmitter;

    /**
     * RTCP Session
     */
    private RtcpSession mRtcpSession;

    /**
     * RTCP socket timeout
     */
    private int mRtcpSocketTimeout = 0;

    /**
     * RTP stream listener
     */
    private RtpStreamListener mRtpStreamListener;

    /**
     * RTP Input stream
     */
    private RtpInputStream mRtpInputStream;

    /**
     * Constructor
     * 
     * @param remoteAddress Remote address
     * @param remotePort Remote port
     */
    public RtpOutputStream(String remoteAddress, int remotePort) {
        mRemoteAddress = remoteAddress;
        mRemotePort = remotePort;
        mRtcpSession = new RtcpSession(true, 16000);
    }

    /**
     * Constructor
     * 
     * @param remoteAddress Remote address
     * @param remotePort Remote port
     * @param rtpInputStream RTP input stream
     */
    public RtpOutputStream(String remoteAddress, int remotePort, RtpInputStream rtpInputStream) {
        mRemoteAddress = remoteAddress;
        mRemotePort = remotePort;
        mRtpInputStream = rtpInputStream;
        mRtcpSession = new RtcpSession(true, 16000);
    }

    /**
     * Constructor
     * 
     * @param remoteAddress Remote address
     * @param remotePort Remote port
     * @param rtcpTimeout RTCP timeout
     */
    public RtpOutputStream(String remoteAddress, int remotePort, int localRtpPort, int rtcpTimeout) {
        mRemoteAddress = remoteAddress;
        mRemotePort = remotePort;
        mLocalRtpPort = localRtpPort;
        mRtcpSocketTimeout = rtcpTimeout;
        mRtcpSession = new RtcpSession(true, 16000);
    }

    /**
     * Open the output stream
     * 
     * @throws IOException
     */
    public void open() throws IOException {
        if (mLocalRtpPort != -1) {
            mRtpReceiver = new RtpPacketReceiver(mLocalRtpPort, mRtcpSession);
            mRtpReceiver.start();

            mRtcpReceiver = new RtcpPacketReceiver(mLocalRtpPort + 1, mRtcpSession,
                    mRtcpSocketTimeout);
            mRtcpReceiver.addRtcpListener(this);
            mRtcpReceiver.start();

            mRtpTransmitter = new RtpPacketTransmitter(mRemoteAddress, mRemotePort, mRtcpSession,
                    mRtpReceiver.getConnection());

            mRtcpTransmitter = new RtcpPacketTransmitter(mRemoteAddress, mRemotePort + 1,
                    mRtcpSession, mRtcpReceiver.getConnection());
            mRtcpTransmitter.start();
        } else if (mRtpInputStream != null) {
            mRtpTransmitter = new RtpPacketTransmitter(mRemoteAddress, mRemotePort, mRtcpSession,
                    mRtpInputStream.getRtpReceiver().getConnection());

            mRtcpTransmitter = new RtcpPacketTransmitter(mRemoteAddress, mRemotePort + 1,
                    mRtcpSession, mRtpInputStream.getRtcpReceiver().getConnection());
        } else {
            mRtpTransmitter = new RtpPacketTransmitter(mRemoteAddress, mRemotePort, mRtcpSession);

            mRtcpTransmitter = new RtcpPacketTransmitter(mRemoteAddress, mRemotePort + 1,
                    mRtcpSession);
        }
    }

    /**
     * Close the output stream
     */
    public void close() {
        CloseableUtils.tryToClose(mRtpTransmitter);
        CloseableUtils.tryToClose(mRtcpTransmitter);
        CloseableUtils.tryToClose(mRtpReceiver);
        CloseableUtils.tryToClose(mRtcpReceiver);
        mRtpStreamListener = null;
    }

    /**
     * Write to the stream without blocking
     * 
     * @param buffer Input buffer
     * @throws NetworkException
     */
    public void write(Buffer buffer) throws NetworkException {
        mRtpTransmitter.sendRtpPacket(buffer);
    }

    @Override
    public void receiveRtcpEvent(RtcpEvent event) {
        // Nothing to do
    }

    @Override
    public void connectionTimeout() {
        if (mRtpStreamListener != null) {
            mRtpStreamListener.rtpStreamAborted();
        }
    }

    /**
     * Adds the RTP stream listener
     * 
     * @param rtpStreamListener
     */
    public void addRtpStreamListener(RtpStreamListener rtpStreamListener) {
        mRtpStreamListener = rtpStreamListener;
    }
}
