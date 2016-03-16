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
 * RTCP APP packet
 * 
 * @author jexa7410
 */
public class RtcpAppPacket extends RtcpPacket {
    public int mSsrc;
    public int mName;
    public int mSubtype;

    public RtcpAppPacket(RtcpPacket parent) {
        super(parent);
        mType = 204;
    }

    public RtcpAppPacket(int ssrc, int name, int subtype, byte data[]) {
        mSsrc = ssrc;
        mName = name;
        mSubtype = subtype;
        mData = data;
        mType = 204;

        if ((data.length & 3) != 0) {
            throw new IllegalArgumentException("Bad data length");
        }
        if (subtype < 0 || subtype > 31) {
            throw new IllegalArgumentException("Bad subtype");
        }
    }

    public int calcLength() {
        return 12 + mData.length;
    }

    public void assemble(DataOutputStream out) throws IOException {
        out.writeByte(128 + mSubtype);
        out.writeByte(204);
        out.writeShort(2 + (mData.length >> 2));
        out.writeInt(mSsrc);
        out.writeInt(mName);
        out.write(mData);
    }
}
