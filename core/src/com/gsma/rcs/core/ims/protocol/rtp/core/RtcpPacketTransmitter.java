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

package com.gsma.rcs.core.ims.protocol.rtp.core;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.platform.network.DatagramConnection;
import com.gsma.rcs.platform.network.NetworkFactory;
import com.gsma.rcs.utils.logger.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;

/**
 * RTCP packet transmitter
 * 
 * @author jexa7410
 */
public class RtcpPacketTransmitter extends Thread implements Closeable {

    private String mRemoteAddress;

    private int mRemotePort;

    /**
     * Statistics
     */
    private RtcpStatisticsTransmitter mStats = new RtcpStatisticsTransmitter();

    private DatagramConnection mDatagramConnection;

    private RtcpSession mRtcpSession;

    /**
     * Flag used to determine when to terminate after sending a BYE
     */
    private boolean mWaitingForByeBackoff = false;

    /**
     * Flag used to properly close
     */
    private boolean mClosed = false;

    /**
     * Random value
     */
    private Random mRand = new Random();

    private static final Logger sLogger = Logger.getLogger(RtcpPacketTransmitter.class.getName());

    /**
     * Constructor
     * 
     * @param address Remote address
     * @param port Remote port
     * @param rtcpSession the RTCP session
     * @throws IOException
     */
    public RtcpPacketTransmitter(String address, int port, RtcpSession rtcpSession)
            throws IOException {
        super();

        mRemoteAddress = address;
        mRemotePort = port;
        mRtcpSession = rtcpSession;

        // Open the connection
        mDatagramConnection = NetworkFactory.getFactory().createDatagramConnection();
        mDatagramConnection.open();

        if (sLogger.isActivated()) {
            sLogger.debug("RTCP transmitter connected to " + mRemoteAddress + ":" + mRemotePort);
        }
    }

    /**
     * Constructor - used for SYMETRIC_RTP
     * 
     * @param address Remote address
     * @param port Remote port
     * @param rtcpSession the RTCP session
     * @param DatagramConnection datagram connection of the RtpPacketReceiver
     * @throws IOException
     */
    public RtcpPacketTransmitter(String address, int port, RtcpSession rtcpSession,
            DatagramConnection connection) throws IOException {
        super();

        mRemoteAddress = address;
        mRemotePort = port;
        mRtcpSession = rtcpSession;

        // Open the connection
        if (connection != null) {
            mDatagramConnection = connection;
        } else {
            mDatagramConnection = NetworkFactory.getFactory().createDatagramConnection();
            mDatagramConnection.open();
        }

        if (sLogger.isActivated()) {
            sLogger.debug("RTCP transmitter connected to " + mRemoteAddress + ":" + mRemotePort);
        }
    }

    /**
     * Close the transmitter
     * 
     * @throws IOException
     */
    public void close() throws IOException {
        if (mClosed) {
            return;
        }

        mRtcpSession.isByeRequested = true;
        mClosed = true;

        // Close the datagram connection
        if (mDatagramConnection != null) {
            mDatagramConnection.close();
        }
        if (sLogger.isActivated()) {
            sLogger.debug("RTCP transmitter closed");
        }
        // If the method start() was never invoked this Thread will be on NEW
        // state and the resources won't be freed. We need to force the start()
        // to allow it to die gracefully
        if (getState() == State.NEW) {
            start();
        }

    }

    /**
     * Background processing
     */
    public void run() {
        if (mClosed) {
            return;
        }

        try {
            // Send a SDES packet
            sendSdesPacket();

            boolean terminate = false;
            while (!terminate) {
                try {
                    // Wait the RTCP report interval.
                    Thread.sleep((long) mRtcpSession.getReportInterval());

                    // Right time to send a RTCP packet or reschedule ?
                    if ((mRtcpSession.timeOfLastRTCPSent + mRtcpSession.T) <= mRtcpSession
                            .currentTime()) {
                        // We know that it is time to send a RTCP packet, is it
                        // a BYE packet
                        if ((mRtcpSession.isByeRequested && mWaitingForByeBackoff)) {
                            // If it is bye then did we ever sent anything
                            if (mRtcpSession.timeOfLastRTCPSent > 0
                                    && mRtcpSession.timeOfLastRTPSent > 0) {
                                mRtcpSession.getMySource().activeSender = false;
                                mRtcpSession.timeOfLastRTCPSent = mRtcpSession.currentTime();
                            } else {
                                // We never sent anything and we have to quit :(
                                // do not send BYE
                                terminate = true;
                            }
                        } else {
                            if (!mClosed) {
                                byte[] data = assembleRtcpPacket();
                                if (data != null) {
                                    transmit(assembleRtcpPacket());
                                }
                                if (mRtcpSession.isByeRequested && !mWaitingForByeBackoff) {
                                    // We have sent a BYE packet, so terminate
                                    terminate = true;
                                } else {
                                    mRtcpSession.timeOfLastRTCPSent = mRtcpSession.currentTime();
                                }
                            } else {
                                terminate = true;
                            }

                        }
                    }
                    mWaitingForByeBackoff = false;

                } catch (InterruptedException e) {
                    mWaitingForByeBackoff = true;
                    mRtcpSession.isByeRequested = true;
                }
            }
        } catch (NetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Can't send the RTCP packet", e);
        }
    }

    /**
     * assemble RTCP packet
     */
    private byte[] assembleRtcpPacket() {
        byte data[] = new byte[0];

        // Sender or receiver packet
        RtpSource s = mRtcpSession.getMySource();
        if ((s.activeSender) && (mRtcpSession.timeOfLastRTCPSent < mRtcpSession.timeOfLastRTPSent)) {
            data = RtcpPacketUtils.append(data, assembleSenderReportPacket());
        } else {
            data = RtcpPacketUtils.append(data, assembleReceiverReportPacket());
        }

        // SDES packets
        Vector<RtcpSdesPacket> repvec = makereports();
        for (int i = 0; i < repvec.size(); i++) {
            if (repvec.elementAt(i).mData != null)
                data = RtcpPacketUtils.append(data, repvec.elementAt(i).mData);
        }

        // BYE packet
        RtcpByePacket byepacket = null;
        if (mRtcpSession.isByeRequested) {
            int ssrc[] = {
                mRtcpSession.SSRC
            };
            byepacket = new RtcpByePacket(ssrc, null);
            data = RtcpPacketUtils.append(data, byepacket.mData);
        }

        return data;
    }

    /**
     * assemble RTCP SR packet
     * 
     * @return packet data
     */
    private byte[] assembleSenderReportPacket() {
        final int FIXED_HEADER_SIZE = 4;
        byte V_P_RC = (byte) ((RtcpPacket.VERSION << 6) | (RtcpPacket.PADDING << 5) | (0x00));
        byte ss[] = RtcpPacketUtils.longToBytes(mRtcpSession.SSRC, 4);
        byte PT[] = RtcpPacketUtils.longToBytes(RtcpPacket.RTCP_SR, 1);
        byte NTP_Timestamp[] = RtcpPacketUtils.longToBytes(mRtcpSession.currentTime(), 8);
        short randomOffset = (short) Math.abs(mRand.nextInt() & 0x000000FF);
        byte RTP_Timestamp[] = RtcpPacketUtils
                .longToBytes((long) mRtcpSession.tc + randomOffset, 4);
        byte SenderPacketCount[] = RtcpPacketUtils.longToBytes(mRtcpSession.packetCount, 4);
        byte SenderOctetCount[] = RtcpPacketUtils.longToBytes(mRtcpSession.octetCount, 4);

        // report block
        byte receptionReportBlocks[] = new byte[0];
        receptionReportBlocks = RtcpPacketUtils.append(receptionReportBlocks,
                assembleRTCPReceptionReport());
        byte receptionReports = (byte) (receptionReportBlocks.length / 24);
        V_P_RC = (byte) (V_P_RC | (byte) (receptionReports & 0x1F));

        // Length is 32 bit words contained in the packet -1
        byte length[] = RtcpPacketUtils.longToBytes((FIXED_HEADER_SIZE + ss.length
                + NTP_Timestamp.length + RTP_Timestamp.length + SenderPacketCount.length
                + SenderOctetCount.length + receptionReportBlocks.length) / 4 - 1, 2);

        // Build RTCP SR Packet
        byte rtcpSRPacket[] = new byte[1];
        rtcpSRPacket[0] = V_P_RC;
        rtcpSRPacket = RtcpPacketUtils.append(rtcpSRPacket, PT);
        rtcpSRPacket = RtcpPacketUtils.append(rtcpSRPacket, length);
        rtcpSRPacket = RtcpPacketUtils.append(rtcpSRPacket, ss);
        rtcpSRPacket = RtcpPacketUtils.append(rtcpSRPacket, NTP_Timestamp);
        rtcpSRPacket = RtcpPacketUtils.append(rtcpSRPacket, RTP_Timestamp);
        rtcpSRPacket = RtcpPacketUtils.append(rtcpSRPacket, SenderPacketCount);
        rtcpSRPacket = RtcpPacketUtils.append(rtcpSRPacket, SenderOctetCount);
        rtcpSRPacket = RtcpPacketUtils.append(rtcpSRPacket, receptionReportBlocks);

        return rtcpSRPacket;
    }

    /**
     * assemble RTCP RR packet
     * 
     * @return packet data
     */
    private byte[] assembleReceiverReportPacket() {
        final int FIXED_HEADER_SIZE = 4;
        byte V_P_RC = (byte) ((RtcpPacket.VERSION << 6) | (RtcpPacket.PADDING << 5) | (0x00));
        byte ss[] = RtcpPacketUtils.longToBytes(mRtcpSession.SSRC, 4);
        byte PT[] = RtcpPacketUtils.longToBytes(RtcpPacket.RTCP_RR, 1);

        // report block
        byte receptionReportBlocks[] = new byte[0];
        receptionReportBlocks = RtcpPacketUtils.append(receptionReportBlocks,
                assembleRTCPReceptionReport());
        byte receptionReports = (byte) (receptionReportBlocks.length / 24);
        V_P_RC = (byte) (V_P_RC | (byte) (receptionReports & 0x1F));

        byte length[] = RtcpPacketUtils.longToBytes(
                (FIXED_HEADER_SIZE + ss.length + receptionReportBlocks.length) / 4 - 1, 2);

        // Build RTCP RR Packet
        byte RRPacket[] = new byte[1];
        RRPacket[0] = V_P_RC;
        RRPacket = RtcpPacketUtils.append(RRPacket, PT);
        RRPacket = RtcpPacketUtils.append(RRPacket, length);
        RRPacket = RtcpPacketUtils.append(RRPacket, ss);
        RRPacket = RtcpPacketUtils.append(RRPacket, receptionReportBlocks);
        return RRPacket;
    }

    /**
     * assemble RTCP Reception report block
     * 
     * @return report data
     */
    private byte[] assembleRTCPReceptionReport() {
        byte reportBlock[] = new byte[0];
        RtpSource source = mRtcpSession.getMySource();

        ReceptionReport rr = source.generateReceptionReport();
        byte SSRC[] = RtcpPacketUtils.longToBytes(rr.getSsrc(), 4);
        byte fraction_lost[] = RtcpPacketUtils.longToBytes((long) rr.getFractionLost(), 1);
        byte pkts_lost[] = RtcpPacketUtils.longToBytes(rr.getCumulativeNumberOfPacketsLost(), 3);
        byte last_seq[] = RtcpPacketUtils.longToBytes(
                rr.getExtendedHighestSequenceNumberReceived(), 4);
        byte jitter[] = RtcpPacketUtils.longToBytes(rr.getInterarrivalJitter(), 4);
        byte lst[] = RtcpPacketUtils.longToBytes(rr.getLastSenderReport(), 4);
        byte dlsr[] = RtcpPacketUtils.longToBytes(rr.getDelaySinceLastSenderReport(), 4);

        reportBlock = RtcpPacketUtils.append(reportBlock, SSRC);
        reportBlock = RtcpPacketUtils.append(reportBlock, fraction_lost);
        reportBlock = RtcpPacketUtils.append(reportBlock, pkts_lost);
        reportBlock = RtcpPacketUtils.append(reportBlock, last_seq);
        reportBlock = RtcpPacketUtils.append(reportBlock, jitter);
        reportBlock = RtcpPacketUtils.append(reportBlock, lst);
        reportBlock = RtcpPacketUtils.append(reportBlock, dlsr);

        return reportBlock;
    }

    /**
     * Generate a RTCP report
     * 
     * @return Vector
     */
    public Vector<RtcpSdesPacket> makereports() {
        Vector<RtcpSdesPacket> packets = new Vector<RtcpSdesPacket>();

        RtcpSdesPacket rtcpsdespacket = new RtcpSdesPacket(new RtcpSdesBlock[1]);
        rtcpsdespacket.sdes[0] = new RtcpSdesBlock();
        rtcpsdespacket.sdes[0].ssrc = mRtcpSession.SSRC;

        Vector<RtcpSdesItem> vector = new Vector<RtcpSdesItem>();
        vector.addElement(new RtcpSdesItem(1, RtpSource.getCname()));
        rtcpsdespacket.sdes[0].items = new RtcpSdesItem[vector.size()];
        vector.copyInto(rtcpsdespacket.sdes[0].items);

        packets.addElement(rtcpsdespacket);
        return packets;
    }

    /**
     * Transmit a RTCP compound packet to the remote destination
     * 
     * @param packet Compound packet to be sent
     * @throws NetworkException
     */
    private void transmit(RtcpCompoundPacket packet) throws NetworkException {
        // Prepare data to be sent
        byte[] data = packet.mData;
        if (packet.mOffset > 0) {
            System.arraycopy(data, packet.mOffset, data = new byte[packet.mLength], 0,
                    packet.mLength);
        }

        // Update statistics
        mStats.numBytes += packet.mLength;
        mStats.numPackets++;
        mRtcpSession.updateavgrtcpsize(packet.mLength);
        mRtcpSession.timeOfLastRTCPSent = mRtcpSession.currentTime();
        // Send data over UDP
        if (data == null) {
            return;
        }
        mDatagramConnection.send(mRemoteAddress, mRemotePort, data);
    }

    /**
     * Transmit a RTCP compound packet to the remote destination
     * 
     * @param packet Compound packet to be sent
     * @throws NetworkException
     */
    private void transmit(byte packet[]) throws NetworkException {
        mStats.numBytes += packet.length;
        mStats.numPackets++;
        mRtcpSession.updateavgrtcpsize(packet.length);
        mRtcpSession.timeOfLastRTCPSent = mRtcpSession.currentTime();
        /* Send data over UDP */
        mDatagramConnection.send(mRemoteAddress, mRemotePort, packet);
    }

    /**
     * Returns the statistics of RTCP transmission
     * 
     * @return Statistics
     */
    public RtcpStatisticsTransmitter getStatistics() {
        return mStats;
    }

    /**
     * Send a SDES packet
     * 
     * @throws NetworkException
     */
    private void sendSdesPacket() throws NetworkException {
        try {
            // Create a report
            Vector<RtcpSdesPacket> repvec = makereports();
            RtcpPacket packets[] = new RtcpPacket[repvec.size()];
            repvec.copyInto(packets);

            // Create a RTCP compound packet
            RtcpCompoundPacket cp = new RtcpCompoundPacket(packets);

            // Assemble the RTCP packet
            int i = cp.calcLength();
            cp.assemble(i, false);

            // Send the RTCP packet
            transmit(cp);
        } catch (IOException e) {
            throw new NetworkException("Failed to send a SDES packet!", e);
        }
    }
}
