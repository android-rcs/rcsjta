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
 * Class RtcpReceiverReportPacket.
 */
public class RtcpReceiverReportPacket extends RtcpPacket {
    /**
     * The ssrc.
     */
    public int ssrc;

    /**
     * The reports array.
     */
    public RtcpReport[] reports;

    /**
     * Creates a new instance of RtcpReceiverReportPacket.
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2 array.
     */
    public RtcpReceiverReportPacket(int arg1, RtcpReport[] arg2) {
        super();
    }

    /**
     * Creates a new instance of RtcpReceiverReportPacket.
     *  
     * @param arg1 The arg1.
     */
    public RtcpReceiverReportPacket(RtcpPacket arg1) {
        super();
    }

    /**
     *  
     * @param arg1 The arg1.
     * @throws IOException if an i/o error occurs
     */
    public void assemble(java.io.DataOutputStream arg1) throws java.io.IOException {

    }

    /**
     *  
     * @return  The int.
     */
    public int calcLength() {
        return 0;
    }

} // end RtcpReceiverReportPacket
