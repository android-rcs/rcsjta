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

package com.gsma.rcs.core.ims.protocol.rtp.core;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.gsma.rcs.core.ims.protocol.rtp.util.Packet;

/**
 * RTCP compound packet
 * 
 * @author jexa7410
 */
public class RtcpCompoundPacket extends RtcpPacket {
    public RtcpPacket[] mPackets;

    public RtcpCompoundPacket(Packet packet) {
        super(packet);
        mType = -1;
    }

    public RtcpCompoundPacket(RtcpPacket[] rtcppackets) {
        mPackets = rtcppackets;
        mType = -1;
    }

    public void assemble(int i, boolean bool) throws IOException {
        mLength = i;
        mOffset = 0;
        ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream(i);
        DataOutputStream dataoutputstream = new DataOutputStream(bytearrayoutputstream);
        int i_0_;
        if (bool) {
            mOffset += 4;
        }
        i_0_ = mOffset;
        for (int i_1_ = 0; i_1_ < mPackets.length; i_1_++) {
            i_0_ = bytearrayoutputstream.size();
            mPackets[i_1_].assemble(dataoutputstream);
        }
        int i_2_ = bytearrayoutputstream.size();
        mData = bytearrayoutputstream.toByteArray();
        if (i_2_ > i) {
            throw new IOException("RTCP Packet overflow");
        }
        if (i_2_ < i) {
            if (mData.length < i)
                System.arraycopy(mData, 0, mData = new byte[i], 0, i_2_);
            mData[i_0_] |= 0x20;
            mData[i - 1] = (byte) (i - i_2_);
            int i_3_ = (mData[i_0_ + 3] & 0xff) + (i - i_2_ >> 2);
            if (i_3_ >= 256)
                mData[i_0_ + 2] += i - i_2_ >> 10;
            mData[i_0_ + 3] = (byte) i_3_;
        }
    }

    public void assemble(DataOutputStream dataoutputstream) throws IOException {
        throw new IllegalArgumentException("Recursive Compound Packet");
    }

    public int calcLength() {
        int i = 0;
        if (mPackets == null || mPackets.length < 1)
            throw new IllegalArgumentException("Bad RTCP Compound Packet");
        for (int i_4_ = 0; i_4_ < mPackets.length; i_4_++)
            i += mPackets[i_4_].calcLength();
        return i;
    }
}
