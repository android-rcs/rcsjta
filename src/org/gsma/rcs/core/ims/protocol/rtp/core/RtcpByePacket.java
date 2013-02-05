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
 * Class RtcpByePacket.
 */
public class RtcpByePacket extends RtcpPacket {
    /**
     * The ssrc array.
     */
    public int[] ssrc;

    /**
     * The reason array.
     */
    public byte[] reason;

    /**
     * Creates a new instance of RtcpByePacket.
     *  
     * @param arg1 The arg1.
     */
    public RtcpByePacket(RtcpPacket arg1) {
        super();
    }

    /**
     * Creates a new instance of RtcpByePacket.
     *  
     * @param arg1 The arg1 array.
     * @param arg2 The arg2 array.
     */
    public RtcpByePacket(int[] arg1, byte[] arg2) {
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

} // end RtcpByePacket
