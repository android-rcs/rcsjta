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
import com.gsma.rcs.core.ims.protocol.rtp.RtpUtils;
import com.gsma.rcs.core.ims.protocol.rtp.core.RtcpPacketReceiver;
import com.gsma.rcs.core.ims.protocol.rtp.core.RtcpPacketTransmitter;
import com.gsma.rcs.core.ims.protocol.rtp.core.RtcpSession;
import com.gsma.rcs.core.ims.protocol.rtp.core.RtpExtensionHeader.ExtensionElement;
import com.gsma.rcs.core.ims.protocol.rtp.core.RtpPacket;
import com.gsma.rcs.core.ims.protocol.rtp.core.RtpPacketReceiver;
import com.gsma.rcs.core.ims.protocol.rtp.format.Format;
import com.gsma.rcs.core.ims.protocol.rtp.format.video.VideoOrientation;
import com.gsma.rcs.core.ims.protocol.rtp.util.Buffer;
import com.gsma.rcs.utils.CloseableUtils;

import java.io.IOException;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.TimeoutException;

/**
 * RTP input stream
 * 
 * @author jexa7410
 */
public class RtpInputStream implements ProcessorInputStream {
    /**
     * RTP Socket Timeout Used a 20s timeout value because the RTP packets can have a delay
     */
    private static final int RTP_SOCKET_TIMEOUT = 20000;

    private static final int MAX_RTP_PACKETS = 5;

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
    private int mLocalPort;

    /**
     * RTP receiver
     */
    private RtpPacketReceiver mRtpReceiver;

    /**
     * RTCP receiver
     */
    private RtcpPacketReceiver mRtcpReceiver;

    /**
     * RTCP transmitter
     */
    private RtcpPacketTransmitter mRtcpTransmitter;

    /**
     * Input buffer
     */
    private Buffer mBuffer = new Buffer();

    /**
     * Input format
     */
    private Format mInputFormat;

    /**
     * RTCP Session
     */
    private RtcpSession mRtcpSession;

    /**
     * RTP stream listener
     */
    private RtpStreamListener mRtpStreamListener;

    /**
     * The negotiated orientation extension header id
     */
    private int mExtensionHeaderId = RtpUtils.RTP_DEFAULT_EXTENSION_ID;

    /**
     * Indicates if the stream was closed
     */
    private boolean mIsClosed;

    /**
     * Sequence RTP packets buffer
     */
    private PriorityQueue<RtpPacket> mRtpPacketsBuffer;

    /**
     * Constructor
     * 
     * @param localPort Local port
     * @param inputFormat Input format
     */
    public RtpInputStream(String remoteAddress, int remotePort, int localPort, Format inputFormat) {
        mRemoteAddress = remoteAddress;
        mRemotePort = remotePort;
        mLocalPort = localPort;
        mInputFormat = inputFormat;

        mRtcpSession = new RtcpSession(false, 16000);

        mRtpPacketsBuffer = new PriorityQueue<RtpPacket>(10, new Comparator<RtpPacket>() {
            @Override
            public int compare(RtpPacket object1, RtpPacket object2) {
                if (object1.seqnum == object2.seqnum) {
                    return 0;
                } else if (object1.seqnum < object2.seqnum) {
                    return -1;
                }
                return 1;
            }
        });

    }

    /**
     * Open the input stream
     * 
     * @throws IOException
     */
    public void open() throws IOException {
        mRtpReceiver = new RtpPacketReceiver(mLocalPort, mRtcpSession, RTP_SOCKET_TIMEOUT);
        mRtpReceiver.start();

        mRtcpReceiver = new RtcpPacketReceiver(mLocalPort + 1, mRtcpSession);
        mRtcpReceiver.start();

        mRtcpTransmitter = new RtcpPacketTransmitter(mRemoteAddress, mRemotePort + 1, mRtcpSession,
                mRtcpReceiver.getConnection());
        mRtcpTransmitter.start();

        mIsClosed = false;
    }

    /**
     * Close the input stream
     */
    public void close() {
        mIsClosed = true;
        CloseableUtils.tryToClose(mRtcpTransmitter);
        CloseableUtils.tryToClose(mRtpReceiver);
        CloseableUtils.tryToClose(mRtcpReceiver);
        mRtpStreamListener = null;
    }

    /**
     * Returns the RTP receiver
     * 
     * @return RTP receiver
     */
    public RtpPacketReceiver getRtpReceiver() {
        return mRtpReceiver;
    }

    /**
     * Returns the RTCP receiver
     * 
     * @return RTCP receiver
     */
    public RtcpPacketReceiver getRtcpReceiver() {
        return mRtcpReceiver;
    }

    /**
     * Read from the input stream without blocking
     * 
     * @return Buffer
     * @throws NetworkException
     */
    public Buffer read() throws NetworkException {
        do {
            try {
                /* Wait and read a RTP packet */
                RtpPacket rtpPacket = mRtpReceiver.readRtpPacket();
                if (rtpPacket == null) {
                    throw new NetworkException("Unable to read RTP packet!");
                }

                mRtpPacketsBuffer.add(rtpPacket);
            } catch (TimeoutException e) {
                if (!mIsClosed) {
                    if (mRtpStreamListener != null) {
                        mRtpStreamListener.rtpStreamAborted();
                    }
                }
                throw new NetworkException("RTP Packet reading timeout!", e);
            }
        } while (mRtpPacketsBuffer.size() <= MAX_RTP_PACKETS);

        RtpPacket packet = mRtpPacketsBuffer.poll();

        mBuffer.setData(packet.mData);
        mBuffer.setLength(packet.payloadlength);
        mBuffer.setOffset(0);
        mBuffer.setFormat(mInputFormat);
        mBuffer.setSequenceNumber(packet.seqnum);
        mBuffer.setRTPMarker(packet.marker != 0);
        mBuffer.setTimestamp(packet.timestamp);

        if (packet.extensionHeader != null) {
            ExtensionElement element = packet.extensionHeader.getElementById(mExtensionHeaderId);
            if (element != null) {
                mBuffer.setVideoOrientation(VideoOrientation.parse(element.data[0]));
            }
        }

        mInputFormat = null;
        return mBuffer;
    }

    /**
     * Adds the RTP stream listener
     * 
     * @param rtpStreamListener
     */
    public void addRtpStreamListener(RtpStreamListener rtpStreamListener) {
        mRtpStreamListener = rtpStreamListener;
    }

    /**
     * Sets the negotiated orientation extension header id
     * 
     * @param extensionHeaderId Header id
     */
    public void setExtensionHeaderId(int extensionHeaderId) {
        mExtensionHeaderId = extensionHeaderId;
    }

}
