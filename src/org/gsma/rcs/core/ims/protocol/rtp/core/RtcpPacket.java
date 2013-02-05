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
 * Class RtcpPacket.
 */
public abstract class RtcpPacket extends org.gsma.rcs.core.ims.protocol.rtp.util.Packet {
    /**
     * Constant VERSION.
     */
    public static final byte VERSION = 2;

    /**
     * Constant PADDING.
     */
    public static final byte PADDING = 0;

    /**
     * Constant RTCP_SR.
     */
    public static final int RTCP_SR = 200;

    /**
     * Constant RTCP_RR.
     */
    public static final int RTCP_RR = 201;

    /**
     * Constant RTCP_SDES.
     */
    public static final int RTCP_SDES = 202;

    /**
     * Constant RTCP_BYE.
     */
    public static final int RTCP_BYE = 203;

    /**
     * Constant RTCP_APP.
     */
    public static final int RTCP_APP = 204;

    /**
     * Constant RTCP_COMPOUND.
     */
    public static final int RTCP_COMPOUND = -1;

    /**
     * The base.
     */
    public org.gsma.rcs.core.ims.protocol.rtp.util.Packet base;

    /**
     * The type.
     */
    public int type;

    /**
     * Creates a new instance of RtcpPacket.
     */
    public RtcpPacket() {
        super();
    }

    /**
     * Creates a new instance of RtcpPacket.
     *  
     * @param arg1 The arg1.
     */
    public RtcpPacket(RtcpPacket arg1) {
        super();
    }

    /**
     * Creates a new instance of RtcpPacket.
     *  
     * @param arg1 The arg1.
     */
    public RtcpPacket(org.gsma.rcs.core.ims.protocol.rtp.util.Packet arg1) {
        super();
    }

    /**
     *  
     * @param arg1 The arg1.
     * @throws IOException if an i/o error occurs
     */
    public abstract void assemble(java.io.DataOutputStream arg1) throws java.io.IOException;

    /**
     *  
     * @return  The int.
     */
    public abstract int calcLength();

} // end RtcpPacket
