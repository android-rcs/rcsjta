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
 * Class RtpPacketReceiver.
 */
public class RtpPacketReceiver {
    /**
     * The datagram connection.
     */
    public org.gsma.rcs.platform.network.DatagramConnection datagramConnection;

    /**
     * Creates a new instance of RtpPacketReceiver.
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @throws IOException if an i/o error occurs
     */
    public RtpPacketReceiver(int arg1, RtcpSession arg2) throws java.io.IOException {

    }

    public void close() {

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
     * @return  The rtp packet.
     */
    public RtpPacket readRtpPacket() {
        return (RtpPacket) null;
    }

    /**
     * Returns the rtp reception stats.
     *  
     * @return  The rtp reception stats.
     */
    public RtpStatisticsReceiver getRtpReceptionStats() {
        return (RtpStatisticsReceiver) null;
    }

} // end RtpPacketReceiver
