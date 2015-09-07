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

import com.gsma.rcs.core.ims.protocol.rtp.event.RtcpApplicationEvent;
import com.gsma.rcs.core.ims.protocol.rtp.event.RtcpByeEvent;
import com.gsma.rcs.core.ims.protocol.rtp.event.RtcpEvent;
import com.gsma.rcs.core.ims.protocol.rtp.event.RtcpEventListener;
import com.gsma.rcs.core.ims.protocol.rtp.event.RtcpReceiverReportEvent;
import com.gsma.rcs.core.ims.protocol.rtp.event.RtcpSdesEvent;
import com.gsma.rcs.core.ims.protocol.rtp.event.RtcpSenderReportEvent;
import com.gsma.rcs.core.ims.protocol.rtp.util.Packet;
import com.gsma.rcs.platform.network.DatagramConnection;
import com.gsma.rcs.platform.network.NetworkFactory;
import com.gsma.rcs.utils.logger.Logger;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Vector;

/**
 * RTCP packet receiver
 * 
 * @author jexa7410
 */
public class RtcpPacketReceiver extends Thread implements Closeable {

    private DatagramConnection mDatagramConnection;

    /**
     * Statistics
     */
    private RtcpStatisticsReceiver mStats = new RtcpStatisticsReceiver();

    /**
     * RTCP event listeners
     */
    private Vector<RtcpEventListener> mListeners = new Vector<RtcpEventListener>();

    private RtcpSession mRtcpSession;

    /**
     * Signals that thread is interrupted
     */
    private boolean mIsInterrupted = false;

    private static final Logger sLogger = Logger.getLogger(RtcpPacketReceiver.class.getName());

    /**
     * Constructor
     * 
     * @param port Listening port
     * @param rtcpSession the RTCP session
     * @param socketTimeout
     * @throws IOException
     */
    public RtcpPacketReceiver(int port, RtcpSession rtcpSession, int socketTimeout)
            throws IOException {
        super();

        mRtcpSession = rtcpSession;

        // Create the UDP server
        mDatagramConnection = NetworkFactory.getFactory().createDatagramConnection(socketTimeout);
        mDatagramConnection.open(port);

        if (sLogger.isActivated()) {
            sLogger.debug("RTCP receiver created at port " + port);
        }
    }

    /**
     * Constructor
     * 
     * @param port Listening port
     * @param rtcpSession the RTCP session
     * @throws IOException
     */
    public RtcpPacketReceiver(int port, RtcpSession rtcpSession) throws IOException {
        this(port, rtcpSession, 0);
    }

    /**
     * Close the receiver
     * 
     * @throws IOException
     */
    public void close() throws IOException {
        mIsInterrupted = true;
        interrupt();

        if (mDatagramConnection != null) {
            mDatagramConnection.close();
            mDatagramConnection = null;
        }
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            while (mDatagramConnection != null) {
                // Wait a packet
                byte[] data = mDatagramConnection.receive();

                // Create a packet object
                Packet packet = new Packet();
                packet.mData = data;
                packet.mLength = data.length;
                packet.mOffset = 0;
                packet.mReceivedAt = System.currentTimeMillis();

                // Process the received packet
                /* Update statistics */
                mStats.numRtcpPkts++;
                mStats.numRtcpBytes += packet.mLength;
                parseRtcpPacket(packet);
            }
        } catch (SocketTimeoutException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
            mStats.numBadRtcpPkts++;
            notifyRtcpListenersOfTimeout();
        } catch (IOException e) {
            if (!mIsInterrupted) {
                if (sLogger.isActivated()) {
                    sLogger.debug(e.getMessage());
                }
            }
            mStats.numBadRtcpPkts++;
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to establish datagramConnection!", e);
            mStats.numBadRtcpPkts++;
        }
    }

    private RtcpReport getRtcpReport(DataInputStream in) throws IOException {
        int ssrc = in.readInt();
        long val = in.readInt();
        val &= 0xffffffffL;
        int fractionLost = (int) (val >> 24);
        int packetsLost = (int) (val & 0xffffffL);
        long lastSeq = in.readInt() & 0xffffffffL;
        int jitter = in.readInt();
        long lsr = in.readInt() & 0xffffffffL;
        long dlsr = in.readInt() & 0xffffffffL;
        return new RtcpReport(ssrc, fractionLost, packetsLost, lastSeq, jitter, lsr, dlsr);
    }

    /**
     * Parse the RTCP packet
     * 
     * @param packet RTCP packet not yet parsed
     * @return RTCP packet
     * @throws IOException
     */
    private RtcpPacket parseRtcpPacket(Packet packet) throws IOException {
        RtcpCompoundPacket compoundPacket = new RtcpCompoundPacket(packet);
        Vector<RtcpPacket> subpackets = new Vector<RtcpPacket>();
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(compoundPacket.mData,
                compoundPacket.mOffset, compoundPacket.mLength));
        mRtcpSession.updateavgrtcpsize(compoundPacket.mLength);
        int length = 0;
        for (int offset = 0; offset < compoundPacket.mLength; offset += length) {
            int firstbyte = in.readUnsignedByte();
            if ((firstbyte & 0xc0) != 128) {
                throw new IOException(new StringBuilder("Bad RTCP packet version for firstbyte : ")
                        .append(firstbyte).toString());
            }

            /* Read type of subpacket */
            int type = in.readUnsignedByte();

            /* Read length of subpacket */
            length = in.readUnsignedShort();
            length = length + 1 << 2;
            int padlen = 0;
            if (offset + length > compoundPacket.mLength) {
                throw new IOException(new StringBuilder("Bad RTCP packet length : ").append(
                        offset + length).toString());
            }
            if (offset + length == compoundPacket.mLength) {
                if ((firstbyte & 0x20) != 0) {
                    padlen = compoundPacket.mData[compoundPacket.mOffset + compoundPacket.mLength
                            - 1] & 0xff;
                    if (padlen == 0) {
                        if (sLogger.isActivated()) {
                            sLogger.error("Bad RTCP packet format");
                        }
                        throw new IOException(new StringBuilder(
                                "Bad RTCP packet format with length : ").append(padlen).toString());
                    }
                }
            } else if ((firstbyte & 0x20) != 0) {
                throw new IOException("Bad RTCP packet format (P != 0)");
            }
            int inlength = length - padlen;
            firstbyte &= 0x1f;

            RtcpPacket subpacket;
            switch (type) {
                case RtcpPacket.RTCP_SR:
                    mStats.numSrPkts++;
                    if (inlength != 28 + 24 * firstbyte) {
                        mStats.numMalformedRtcpPkts++;
                        throw new IOException("Bad RTCP SR packet format");
                    }
                    RtcpSenderReportPacket srp = new RtcpSenderReportPacket(compoundPacket);
                    subpacket = srp;
                    srp.ssrc = in.readInt();
                    srp.ntptimestampmsw = in.readInt() & 0xffffffffL;
                    srp.ntptimestamplsw = in.readInt() & 0xffffffffL;
                    srp.rtptimestamp = in.readInt() & 0xffffffffL;
                    srp.packetcount = in.readInt() & 0xffffffffL;
                    srp.octetcount = in.readInt() & 0xffffffffL;
                    srp.reports = new RtcpReport[firstbyte];

                    RtpSource sourceSR = mRtcpSession.getMySource();
                    if (sourceSR != null) {
                        sourceSR.receivedSenderReport(srp);
                    }

                    for (int i = 0; i < srp.reports.length; i++) {
                        srp.reports[i] = getRtcpReport(in);
                    }

                    notifyRtcpListeners(new RtcpSenderReportEvent(srp));
                    break;

                case RtcpPacket.RTCP_RR:
                    if (inlength != 8 + 24 * firstbyte) {
                        mStats.numMalformedRtcpPkts++;
                        throw new IOException("Bad RTCP RR packet format");
                    }
                    RtcpReceiverReportPacket rrp = new RtcpReceiverReportPacket(compoundPacket);
                    subpacket = rrp;
                    rrp.ssrc = in.readInt();
                    rrp.reports = new RtcpReport[firstbyte];

                    for (int i = 0; i < rrp.reports.length; i++) {
                        rrp.reports[i] = getRtcpReport(in);
                    }

                    notifyRtcpListeners(new RtcpReceiverReportEvent(rrp));
                    break;

                case RtcpPacket.RTCP_SDES:
                    RtcpSdesPacket sdesp = new RtcpSdesPacket(compoundPacket);
                    subpacket = sdesp;
                    sdesp.sdes = new RtcpSdesBlock[firstbyte];
                    int sdesoff = 4;
                    for (int i = 0; i < sdesp.sdes.length; i++) {
                        RtcpSdesBlock chunk = new RtcpSdesBlock();
                        sdesp.sdes[i] = chunk;
                        chunk.ssrc = in.readInt();
                        sdesoff += 5;
                        Vector<RtcpSdesItem> items = new Vector<RtcpSdesItem>();
                        boolean gotcname = false;
                        int j;
                        while ((j = in.readUnsignedByte()) != 0) {
                            if (j < 1 || j > 8) {
                                mStats.numMalformedRtcpPkts++;
                                throw new IOException("Bad RTCP SDES packet format");
                            }
                            if (j == 1) {
                                gotcname = true;
                            }
                            RtcpSdesItem item = new RtcpSdesItem();
                            items.addElement(item);
                            item.type = j;
                            int sdeslen = in.readUnsignedByte();
                            item.data = new byte[sdeslen];
                            in.readFully(item.data);
                            sdesoff += 2 + sdeslen;
                        }
                        if (!gotcname) {
                            mStats.numMalformedRtcpPkts++;
                            throw new IOException("Bad RTCP SDES packet format");
                        }
                        chunk.items = new RtcpSdesItem[items.size()];
                        items.copyInto(chunk.items);
                        if ((sdesoff & 3) != 0) {
                            if (in.skip(4 - (sdesoff & 3)) != 4 - (sdesoff & 3)) {
                                throw new IOException("Bad RTCP SDES packet format");
                            }
                            sdesoff = sdesoff + 3 & -4;
                        }
                    }

                    if (inlength != sdesoff) {
                        mStats.numMalformedRtcpPkts++;
                        throw new IOException("Bad RTCP SDES packet format");
                    }

                    notifyRtcpListeners(new RtcpSdesEvent(sdesp));
                    break;

                case RtcpPacket.RTCP_BYE:
                    RtcpByePacket byep = new RtcpByePacket(compoundPacket);
                    subpacket = byep;
                    byep.mSsrc = new int[firstbyte];
                    for (int i = 0; i < byep.mSsrc.length; i++) {
                        byep.mSsrc[i] = in.readInt();
                    }

                    int reasonlen;
                    if (inlength > 4 + 4 * firstbyte) {
                        reasonlen = in.readUnsignedByte();
                        byep.mReason = new byte[reasonlen];
                        reasonlen++;
                    } else {
                        reasonlen = 0;
                        byep.mReason = new byte[0];
                    }
                    reasonlen = reasonlen + 3 & -4;
                    if (inlength != 4 + 4 * firstbyte + reasonlen) {
                        mStats.numMalformedRtcpPkts++;
                        throw new IOException("Bad RTCP BYE packet format");
                    }
                    in.readFully(byep.mReason);
                    int skipBye = reasonlen - byep.mReason.length;
                    if (in.skip(skipBye) != skipBye) {
                        throw new IOException("Bad RTCP BYE packet format");
                    }

                    notifyRtcpListeners(new RtcpByeEvent(byep));
                    break;

                case RtcpPacket.RTCP_APP:
                    if (inlength < 12) {
                        throw new IOException("Bad RTCP APP packet format");
                    }
                    RtcpAppPacket appp = new RtcpAppPacket(compoundPacket);
                    subpacket = appp;
                    appp.mSsrc = in.readInt();
                    appp.mName = in.readInt();
                    appp.mSubtype = firstbyte;
                    appp.mData = new byte[inlength - 12];
                    in.readFully(appp.mData);
                    int skipApp = inlength - 12 - appp.mData.length;
                    if (in.skip(skipApp) != skipApp) {
                        throw new IOException("Bad RTCP APP packet format");
                    }

                    notifyRtcpListeners(new RtcpApplicationEvent(appp));
                    break;

                default:
                    mStats.numUnknownTypes++;
                    throw new IOException("Bad RTCP packet format");
            }
            subpacket.mOffset = offset;
            subpacket.mLength = length;
            subpackets.addElement(subpacket);
            if (in.skipBytes(padlen) != padlen) {
                throw new IOException("Bad RTCP packet format");
            }
        }
        compoundPacket.mPackets = new RtcpPacket[subpackets.size()];
        subpackets.copyInto(compoundPacket.mPackets);
        return compoundPacket;
    }

    /**
     * Add a RTCP event listener
     * 
     * @param listener Listener
     */
    public void addRtcpListener(RtcpEventListener listener) {
        if (sLogger.isActivated()) {
            sLogger.debug("Add a RTCP event listener");
        }
        mListeners.addElement(listener);
    }

    /**
     * Remove a RTCP event listener
     * 
     * @param listener Listener
     */
    public void removeRtcpListener(RtcpEventListener listener) {
        if (sLogger.isActivated()) {
            sLogger.debug("Remove a RTCP event listener");
        }
        mListeners.removeElement(listener);
    }

    /**
     * Notify RTCP event listeners
     * 
     * @param event RTCP event
     */
    public void notifyRtcpListeners(RtcpEvent event) {
        for (int i = 0; i < mListeners.size(); i++) {
            RtcpEventListener listener = mListeners.elementAt(i);
            listener.receiveRtcpEvent(event);
        }
    }

    /**
     * Notify timeout on RTCP listener
     */
    private void notifyRtcpListenersOfTimeout() {
        for (RtcpEventListener listener : mListeners) {
            if (sLogger.isActivated()) {
                sLogger.debug("RTCP connection timeout");
            }
            listener.connectionTimeout();
        }
    }

    /**
     * Returns the statistics of RTCP reception
     * 
     * @return Statistics
     */
    public RtcpStatisticsReceiver getRtcpReceptionStats() {
        return mStats;
    }

    /**
     * Returns the DatagramConnection of RTCP
     * 
     * @return DatagramConnection
     */
    public DatagramConnection getConnection() {
        return mDatagramConnection;
    }
}
