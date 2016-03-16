/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * RTCP BYE packet
 * 
 * @author jexa7410
 */
public class RtcpByePacket extends RtcpPacket {

    public int mSsrc[];
    public byte mReason[];

    public RtcpByePacket(RtcpPacket parent) {
        super(parent);
        mType = 203;
    }

    public RtcpByePacket(int ssrc[], byte reason[]) {
        this.mSsrc = ssrc;
        if (reason != null) {
            this.mReason = reason;
        } else {
            this.mReason = new byte[0];
        }
        if (ssrc.length > 31) {
            throw new IllegalArgumentException("Too many SSRCs");
        }
    }

    public int calcLength() {
        return 4 + (mSsrc.length << 2) + (mReason.length <= 0 ? 0 : mReason.length + 4 & -4);
    }

    public void assemble(DataOutputStream out) throws IOException {
        out.writeByte(128 + mSsrc.length);
        out.writeByte(203);
        out.writeShort(mSsrc.length + (mReason.length <= 0 ? 0 : mReason.length + 4 >> 2));
        for (int i = 0; i < mSsrc.length; i++) {
            out.writeInt(mSsrc[i]);
        }

        if (mReason.length > 0) {
            out.writeByte(mReason.length);
            out.write(mReason);
            for (int i = (mReason.length + 4 & -4) - mReason.length - 1; i > 0; i--) {
                out.writeByte(0);
            }
        }
    }
}
