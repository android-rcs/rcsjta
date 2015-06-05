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
import java.io.DataInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Vector;

/**
 * RTCP packet receiver
 * 
 * @author jexa7410
 */
public class RtcpPacketReceiver extends Thread {
    /**
     * Datagram connection
     */
    public DatagramConnection datagramConnection = null;

    /**
     * Statistics
     */
    private RtcpStatisticsReceiver stats = new RtcpStatisticsReceiver();

    /**
     * RTCP event listeners
     */
    private Vector<RtcpEventListener> listeners = new Vector<RtcpEventListener>();

    /**
     * RTCP Session
     */
    private RtcpSession rtcpSession = null;

    /**
     * Signals that thread is interrupted
     */
    private boolean isInterrupted = false;

    /**
     * The logger
     */
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

        this.rtcpSession = rtcpSession;

        // Create the UDP server
        datagramConnection = NetworkFactory.getFactory().createDatagramConnection(socketTimeout);
        datagramConnection.open(port);

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
        isInterrupted = true;
        interrupt();

        if (datagramConnection != null) {
            datagramConnection.close();
            datagramConnection = null;
        }
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            while (datagramConnection != null) {
                // Wait a packet
                byte[] data = datagramConnection.receive();

                // Create a packet object
                Packet packet = new Packet();
                packet.data = data;
                packet.length = data.length;
                packet.offset = 0;
                packet.receivedAt = System.currentTimeMillis();

                // Process the received packet
                /* Update statistics */
                stats.numRtcpPkts++;
                stats.numRtcpBytes += packet.length;
                parseRtcpPacket(packet);
            }
        } catch (SocketTimeoutException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
            stats.numBadRtcpPkts++;
            notifyRtcpListenersOfTimeout();
        } catch (IOException e) {
            if (!isInterrupted) {
                if (sLogger.isActivated()) {
                    sLogger.debug(e.getMessage());
                }
            }
            stats.numBadRtcpPkts++;
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to establish datagramConnection!", e);
            stats.numBadRtcpPkts++;
        }
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
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(compoundPacket.data,
                compoundPacket.offset, compoundPacket.length));
        rtcpSession.updateavgrtcpsize(compoundPacket.length);
        int length = 0;
        for (int offset = 0; offset < compoundPacket.length; offset += length) {
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
            if (offset + length > compoundPacket.length) {
                throw new IOException(new StringBuilder("Bad RTCP packet length : ").append(
                        offset + length).toString());
            }
            if (offset + length == compoundPacket.length) {
                if ((firstbyte & 0x20) != 0) {
                    padlen = compoundPacket.data[compoundPacket.offset + compoundPacket.length - 1] & 0xff;
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
                    stats.numSrPkts++;
                    if (inlength != 28 + 24 * firstbyte) {
                        stats.numMalformedRtcpPkts++;
                        throw new IOException("Bad RTCP SR packet format");
                    }
                    RtcpSenderReportPacket srp = new RtcpSenderReportPacket(compoundPacket);
                    subpacket = srp;
                    srp.ssrc = in.readInt();
                    srp.ntptimestampmsw = (long) in.readInt() & 0xffffffffL;
                    srp.ntptimestamplsw = (long) in.readInt() & 0xffffffffL;
                    srp.rtptimestamp = (long) in.readInt() & 0xffffffffL;
                    srp.packetcount = (long) in.readInt() & 0xffffffffL;
                    srp.octetcount = (long) in.readInt() & 0xffffffffL;
                    srp.reports = new RtcpReport[firstbyte];

                    RtpSource sourceSR = rtcpSession.getMySource();
                    if (sourceSR != null) {
                        sourceSR.receivedSenderReport(srp);
                    }

                    for (int i = 0; i < srp.reports.length; i++) {
                        RtcpReport report = new RtcpReport();
                        srp.reports[i] = report;
                        report.ssrc = in.readInt();
                        long val = in.readInt();
                        val &= 0xffffffffL;
                        report.fractionlost = (int) (val >> 24);
                        report.packetslost = (int) (val & 0xffffffL);
                        report.lastseq = (long) in.readInt() & 0xffffffffL;
                        report.jitter = in.readInt();
                        report.lsr = (long) in.readInt() & 0xffffffffL;
                        report.dlsr = (long) in.readInt() & 0xffffffffL;
                    }

                    notifyRtcpListeners(new RtcpSenderReportEvent(srp));
                    break;

                case RtcpPacket.RTCP_RR:
                    if (inlength != 8 + 24 * firstbyte) {
                        stats.numMalformedRtcpPkts++;
                        throw new IOException("Bad RTCP RR packet format");
                    }
                    RtcpReceiverReportPacket rrp = new RtcpReceiverReportPacket(compoundPacket);
                    subpacket = rrp;
                    rrp.ssrc = in.readInt();
                    rrp.reports = new RtcpReport[firstbyte];

                    for (int i = 0; i < rrp.reports.length; i++) {
                        RtcpReport report = new RtcpReport();
                        rrp.reports[i] = report;
                        report.ssrc = in.readInt();
                        long val = in.readInt();
                        val &= 0xffffffffL;
                        report.fractionlost = (int) (val >> 24);
                        report.packetslost = (int) (val & 0xffffffL);
                        report.lastseq = (long) in.readInt() & 0xffffffffL;
                        report.jitter = in.readInt();
                        report.lsr = (long) in.readInt() & 0xffffffffL;
                        report.dlsr = (long) in.readInt() & 0xffffffffL;
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
                                stats.numMalformedRtcpPkts++;
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
                            stats.numMalformedRtcpPkts++;
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
                        stats.numMalformedRtcpPkts++;
                        throw new IOException("Bad RTCP SDES packet format");
                    }

                    notifyRtcpListeners(new RtcpSdesEvent(sdesp));
                    break;

                case RtcpPacket.RTCP_BYE:
                    RtcpByePacket byep = new RtcpByePacket(compoundPacket);
                    subpacket = byep;
                    byep.ssrc = new int[firstbyte];
                    for (int i = 0; i < byep.ssrc.length; i++) {
                        byep.ssrc[i] = in.readInt();
                    }

                    int reasonlen;
                    if (inlength > 4 + 4 * firstbyte) {
                        reasonlen = in.readUnsignedByte();
                        byep.reason = new byte[reasonlen];
                        reasonlen++;
                    } else {
                        reasonlen = 0;
                        byep.reason = new byte[0];
                    }
                    reasonlen = reasonlen + 3 & -4;
                    if (inlength != 4 + 4 * firstbyte + reasonlen) {
                        stats.numMalformedRtcpPkts++;
                        throw new IOException("Bad RTCP BYE packet format");
                    }
                    in.readFully(byep.reason);
                    int skipBye = reasonlen - byep.reason.length;
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
                    appp.ssrc = in.readInt();
                    appp.name = in.readInt();
                    appp.subtype = firstbyte;
                    appp.data = new byte[inlength - 12];
                    in.readFully(appp.data);
                    int skipApp = inlength - 12 - appp.data.length;
                    if (in.skip(skipApp) != skipApp) {
                        throw new IOException("Bad RTCP APP packet format");
                    }

                    notifyRtcpListeners(new RtcpApplicationEvent(appp));
                    break;

                default:
                    stats.numUnknownTypes++;
                    throw new IOException("Bad RTCP packet format");
            }
            subpacket.offset = offset;
            subpacket.length = length;
            subpackets.addElement(subpacket);
            if (in.skipBytes(padlen) != padlen) {
                throw new IOException("Bad RTCP packet format");
            }
        }
        compoundPacket.packets = new RtcpPacket[subpackets.size()];
        subpackets.copyInto(compoundPacket.packets);
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
        listeners.addElement(listener);
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
        listeners.removeElement(listener);
    }

    /**
     * Notify RTCP event listeners
     * 
     * @param event RTCP event
     */
    public void notifyRtcpListeners(RtcpEvent event) {
        for (int i = 0; i < listeners.size(); i++) {
            RtcpEventListener listener = (RtcpEventListener) listeners.elementAt(i);
            listener.receiveRtcpEvent(event);
        }
    }

    /**
     * Notify timeout on RTCP listener
     */
    private void notifyRtcpListenersOfTimeout() {
        for (RtcpEventListener listener : listeners) {
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
        return stats;
    }

    /**
     * Returns the DatagramConnection of RTCP
     * 
     * @return DatagramConnection
     */
    public DatagramConnection getConnection() {
        return datagramConnection;
    }
}
