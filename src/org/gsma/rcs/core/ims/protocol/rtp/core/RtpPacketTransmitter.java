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
 * Class RtpPacketTransmitter.
 */
public class RtpPacketTransmitter {
    /**
     * Creates a new instance of RtpPacketTransmitter.
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @param arg3 The arg3.
     * @throws IOException if an i/o error occurs
     */
    public RtpPacketTransmitter(String arg1, int arg2, RtcpSession arg3) throws java.io.IOException {

    }

    /**
     * Creates a new instance of RtpPacketTransmitter.
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @param arg3 The arg3.
     * @param arg4 The arg4.
     * @throws IOException if an i/o error occurs
     */
    public RtpPacketTransmitter(String arg1, int arg2, RtcpSession arg3, org.gsma.rcs.platform.network.DatagramConnection arg4) throws java.io.IOException {

    }

    /**
     *  
     * @throws IOException if an i/o error occurs
     */
    public void close() throws java.io.IOException {

    }

    /**
     * Returns the statistics.
     *  
     * @return  The statistics.
     */
    public RtpStatisticsTransmitter getStatistics() {
        return (RtpStatisticsTransmitter) null;
    }

    /**
     *  
     * @param arg1 The arg1.
     * @throws IOException if an i/o error occurs
     */
    public void sendRtpPacket(org.gsma.rcs.core.ims.protocol.rtp.util.Buffer arg1) throws java.io.IOException {

    }

} // end RtpPacketTransmitter
