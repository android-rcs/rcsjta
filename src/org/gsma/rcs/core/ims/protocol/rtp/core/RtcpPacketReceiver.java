/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.core.ims.protocol.rtp.core;

/**
 * Class RtcpPacketReceiver.
 */
public class RtcpPacketReceiver extends Thread {
    /**
     * The datagram connection.
     */
    public org.gsma.rcs.platform.network.DatagramConnection datagramConnection;

    /**
     * Creates a new instance of RtcpPacketReceiver.
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @throws IOException if an i/o error occurs
     */
    public RtcpPacketReceiver(int arg1, RtcpSession arg2) throws java.io.IOException {
        super();
    }

    public void run() {

    }

    /**
     *  
     * @throws IOException if an i/o error occurs
     */
    public void close() throws java.io.IOException {

    }

    /**
     * Returns the connection.
     *  
     * @return  The connection.
     */
    public org.gsma.rcs.platform.network.DatagramConnection getConnection() {
        return (org.gsma.rcs.platform.network.DatagramConnection) null;
    }

    /**
     *  
     * @param arg1 The arg1.
     * @return  The rtcp packet.
     */
    public RtcpPacket handlePacket(org.gsma.rcs.core.ims.protocol.rtp.util.Packet arg1) {
        return (RtcpPacket) null;
    }

    /**
     * Parses the rtcp packet.
     *  
     * @param arg1 The arg1.
     * @return  The rtcp packet.
     */
    public RtcpPacket parseRtcpPacket(org.gsma.rcs.core.ims.protocol.rtp.util.Packet arg1) {
        return (RtcpPacket) null;
    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void notifyRtcpListeners(org.gsma.rcs.core.ims.protocol.rtp.event.RtcpEvent arg1) {

    }

    /**
     * Adds a rtcp listener.
     *  
     * @param arg1 The arg1.
     */
    public void addRtcpListener(org.gsma.rcs.core.ims.protocol.rtp.event.RtcpEventListener arg1) {

    }

    /**
     * Removes a rtcp listener.
     *  
     * @param arg1 The arg1.
     */
    public void removeRtcpListener(org.gsma.rcs.core.ims.protocol.rtp.event.RtcpEventListener arg1) {

    }

    /**
     * Returns the rtcp reception stats.
     *  
     * @return  The rtcp reception stats.
     */
    public RtcpStatisticsReceiver getRtcpReceptionStats() {
        return (RtcpStatisticsReceiver) null;
    }

} // end RtcpPacketReceiver
