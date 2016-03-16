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

/**
 * RTCP report
 * 
 * @author jexa7410
 */
public class RtcpReport {
    private final int mSsrc;
    private final int mFractionLost;
    private final int mPacketsLost;
    private final long mLastSeq;
    private final int mJitter;
    private final long mLsr;
    private final long mDlsr;

    public RtcpReport(int ssrc, int fractionLost, int packetsLost, long lastSeq, int jitter,
            long lsr, long dlsr) {
        mSsrc = ssrc;
        mFractionLost = fractionLost;
        mPacketsLost = packetsLost;
        mLastSeq = lastSeq;
        mJitter = jitter;
        mLsr = lsr;
        mDlsr = dlsr;
    }

    public long getDlsr() {
        return mDlsr;
    }

    public int getFractionLost() {
        return mFractionLost;
    }

    public int getJitter() {
        return mJitter;
    }

    public long getLsr() {
        return mLsr;
    }

    public int getPacketsLost() {
        return mPacketsLost;
    }

    public int getSsrc() {
        return mSsrc;
    }

    public long getLastSeq() {
        return mLastSeq;
    }

}
