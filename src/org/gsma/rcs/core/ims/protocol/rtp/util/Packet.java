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

package org.gsma.rcs.core.ims.protocol.rtp.util;

/**
 * Class Packet.
 */
public class Packet {
    /**
     * The data array.
     */
    public byte[] data;

    /**
     * The length.
     */
    public int length;

    /**
     * The offset.
     */
    public int offset;

    /**
     * The received at.
     */
    public long receivedAt;

    /**
     * Creates a new instance of Packet.
     */
    public Packet() {

    }

    /**
     * Creates a new instance of Packet.
     *  
     * @param arg1 The arg1.
     */
    public Packet(Packet arg1) {

    }

} // end Packet
