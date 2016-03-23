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

package com.gsma.rcs.core.ims.protocol.rtp.util;

/**
 * Generic packet
 * 
 * @author jexa7410
 */
public class Packet {
    /**
     * Data
     */
    public byte[] mData;

    /**
     * Packet length
     */
    public int mLength;

    /**
     * Offset
     */
    public int mOffset;

    /**
     * Received at
     */
    public long mReceivedAt;

    /**
     * Constructor
     */
    public Packet() {
    }

    /**
     * Constructor
     * 
     * @param packet Packet
     */
    public Packet(Packet packet) {
        mData = packet.mData;
        mLength = packet.mLength;
        mOffset = packet.mOffset;
        mReceivedAt = packet.mReceivedAt;
    }
}
