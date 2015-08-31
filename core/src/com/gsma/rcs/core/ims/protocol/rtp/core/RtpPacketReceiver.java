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

package com.gsma.rcs.core.ims.protocol.rtp.core;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import com.gsma.rcs.platform.network.DatagramConnection;
import com.gsma.rcs.platform.network.NetworkFactory;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.FifoBuffer;
import com.gsma.rcs.utils.logger.Logger;

/**
 * RTP packet receiver
 * 
 * @author jexa7410
 */
public class RtpPacketReceiver extends Thread implements Closeable {
    /**
     * Statistics
     */
    private RtpStatisticsReceiver mStats = new RtpStatisticsReceiver();

    /**
     * Datagram connection
     */
    public DatagramConnection mDatagramConnection;

    /**
     * RTCP Session
     */
    private RtcpSession mRtcpSession;

    /**
     * Signals that connection is closed
     */
    private boolean mClosed;

    /**
     * Fifo buffer for received packet
     */
    private FifoBuffer mBuffer = new FifoBuffer();

    /**
     * Max size for the fifo
     */
    private static final int FIFO_MAX_NUMBER = 100;

    /**
     * Number of element to clean in the fifo
     */
    private static final int FIFO_CLEAN_NUMBER = 20;

    /**
     * Signals that thread is interrupted
     */
    private boolean mInterrupted;

    /**
     * Last sequence number
     */
    private int mLastSeqnum;

    /**
     * timeout
     */
    private int mTimeout;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(RtpPacketReceiver.class.getName());

    /**
     * Constructor
     * 
     * @param port Listening port
     * @param rtcpSession
     * @param socketTimeout
     * @throws IOException
     */
    public RtpPacketReceiver(int port, RtcpSession rtcpSession, int socketTimeout)
            throws IOException {
        super();

        mRtcpSession = rtcpSession;
        mTimeout = socketTimeout;
        // Create the UDP server
        mDatagramConnection = NetworkFactory.getFactory().createDatagramConnection(socketTimeout);
        mDatagramConnection.open(port);
        if (sLogger.isActivated()) {
            sLogger.debug("RTP receiver created on port " + port);
        }
    }

    /**
     * Constructor
     * 
     * @param port Listenning port
     * @param rtcpSession
     * @throws IOException
     */
    public RtpPacketReceiver(int port, RtcpSession rtcpSession) throws IOException {
        this(port, rtcpSession, 0);
    }

    /**
     * Close the receiver
     */
    public void close() {
        mInterrupted = true;
        interrupt();
        mClosed = true;
        CloseableUtils.tryToClose(mDatagramConnection);
    }

    /**
     * Background processing
     */
    public void run() {
        if (sLogger.isActivated()) {
            sLogger.debug("RTP Receiver processing is started");
        }
        try {
            while (mDatagramConnection != null) {
                // Wait a new packet
                byte[] data = mDatagramConnection.receive();

                if (data.length >= 12) {
                    // Drop empty packet (payload 20)
                    int payloadType = (byte) ((data[1] & 0xff) & 0x7f);
                    if (payloadType != 20) {
                        // Drop too old packet
                        int seqnum = (char) ((data[2] << 8) | (data[3] & 0xff));
                        if (seqnum > mLastSeqnum - 10) {
                            // Clean the FIFO if full
                            if (mBuffer.size() >= FIFO_MAX_NUMBER) {
                                mBuffer.clean(FIFO_CLEAN_NUMBER);
                            }
                            mBuffer.addObject(data);
                            mLastSeqnum = seqnum;
                        } else {
                            mStats.numBadRtpPkts++;
                        }
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            if (!mInterrupted) {
                if (sLogger.isActivated()) {
                    sLogger.debug(e.getMessage());
                }
            }
        } catch (IOException e) {
            if (!mInterrupted) {
                if (sLogger.isActivated()) {
                    sLogger.debug(e.getMessage());
                }
            }
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Datagram socket server failed!", e);
        }
    }

    /**
     * Read a RTP packet (blocking method)
     * 
     * @return RTP packet
     * @throws TimeoutException
     */
    public RtpPacket readRtpPacket() throws TimeoutException {
        // Get a new packet in FIFO
        byte[] data = (byte[]) mBuffer.getObject(mTimeout);
        if (data == null) {
            throw new TimeoutException("Unable to fetch packet from FIFO queue!");
        }

        // Parse the RTP packet
        RtpPacket pkt = parseRtpPacket(data);

        if (pkt != null) {
            // Update statistics
            mStats.numPackets++;
            mStats.numBytes += data.length;

            RtpSource s = mRtcpSession.getMySource();
            s.setSsrc(pkt.ssrc);
            s.activeSender = true;
            s.receiveRtpPacket(pkt);
            pkt.seqnum = s.generateExtendedSequenceNumber(pkt.seqnum);

            return pkt;
        } else {
            return readRtpPacket();
        }
    }

    /**
     * Parse the RTP packet
     * 
     * @param data RTP packet not yet parsed
     * @return RTP packet
     */
    private RtpPacket parseRtpPacket(byte[] data) {
        RtpPacket packet = new RtpPacket();
        // Read RTP packet length
        packet.mLength = data.length;

        // Set received timestamp
        packet.mReceivedAt = System.currentTimeMillis();

        // Read extension bit
        packet.extension = (data[0] & 0x10) > 0;

        // Read marker
        if ((byte) ((data[1] & 0xff) & 0x80) == (byte) 0x80) {
            packet.marker = 1;
        } else {
            packet.marker = 0;
        }

        // Read payload type
        packet.payloadType = (byte) ((data[1] & 0xff) & 0x7f);

        // Read sequence number (it's a unsigned 16 bit value. Because Java only supports
        // signed values for int and short we use char to do the correct conversion.)
        packet.seqnum = (char) ((data[2] << 8) | (data[3] & 0xff));

        // Read timestamp
        packet.timestamp = (((data[4] & 0xff) << 24) | ((data[5] & 0xff) << 16)
                | ((data[6] & 0xff) << 8) | (data[7] & 0xff));

        // Read SSRC
        packet.ssrc = (((data[8] & 0xff) << 24) | ((data[9] & 0xff) << 16)
                | ((data[10] & 0xff) << 8) | (data[11] & 0xff));

        // Extract the extension header
        if (packet.extension) {
            int dataId = 11;
            int extensionHeaderId = ((data[++dataId] & 0xff) << 8) | (data[++dataId] & 0xff);
            int length = ((data[++dataId] & 0xff) << 8) | (data[++dataId] & 0xff);

            if (extensionHeaderId == RtpExtensionHeader.RTP_EXTENSION_HEADER_ID) {
                extractExtensionHeader(data, length, dataId, packet);
            }

            // increment payload offset = RtpHeader size (12) + Extension Header ID (2) + Header
            // Length (2) +
            // elements * 4 (32 bits each) + 1 (to set at correct index)
            packet.payloadoffset = 16 + length * 4;
        } else {
            packet.payloadoffset = 12;
        }
        packet.payloadlength = packet.mLength - packet.payloadoffset;
        packet.mData = new byte[packet.payloadlength];
        System.arraycopy(data, packet.payloadoffset, packet.mData, 0, packet.payloadlength);

        return packet;
    }

    /**
     * Returns the statistics of RTP reception
     * 
     * @return Statistics
     */
    public RtpStatisticsReceiver getRtpReceptionStats() {
        return mStats;
    }

    /**
     * Returns the DatagramConnection of RTP
     * 
     * @return DatagramConnection
     */
    public DatagramConnection getConnection() {
        return mDatagramConnection;
    }

    /**
     * Extract Extension Header
     * 
     * @param data
     * @param length
     * @param dataId
     * @param packet
     */
    private void extractExtensionHeader(byte[] data, int length, int dataId, RtpPacket packet) {
        byte[] extensionHeaderData = new byte[length * 4];
        System.arraycopy(data, ++dataId, extensionHeaderData, 0, extensionHeaderData.length);
        packet.extensionHeader = new RtpExtensionHeader();

        int i = 0;
        while (packet.extensionHeader.elementsCount() < length) {
            byte idAndLength = extensionHeaderData[i];
            if (idAndLength == 0x00) {
                // its a padding byte, skip it
                i = i + 1;
                continue;
            }

            int elementId = (idAndLength & 0xf0) >>> 4;

            // Each extension element id must have a value between 1 and 14 inclusive
            if (elementId > 0 && elementId < 15) {
                int elementLength = (idAndLength & 0x0f);
                byte[] elementData = new byte[elementLength + 1];
                System.arraycopy(extensionHeaderData, i + 1, elementData, 0, elementData.length);
                packet.extensionHeader.addElement(elementId, elementData);
                i = i + elementData.length + 1;
            } else {
                break;
            }
        }
    }
}
