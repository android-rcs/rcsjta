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
 * Class RtcpSession.
 */
public class RtcpSession {
    /**
     * The is bye requested.
     */
    public boolean isByeRequested;

    /**
     * The time of last r t p sent.
     */
    public double timeOfLastRTPSent;

    /**
     * The time of last r t c p sent.
     */
    public double timeOfLastRTCPSent;

    /**
     * The app startup time.
     */
    public long appStartupTime;

    /**
     * The t.
     */
    public double T;

    /**
     * The s s r c.
     */
    public int SSRC;

    /**
     * The tc.
     */
    public double tc;

    /**
     * The packet count.
     */
    public long packetCount;

    /**
     * The octet count.
     */
    public long octetCount;

    /**
     * Creates a new instance of RtcpSession.
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public RtcpSession(boolean arg1, double arg2) {

    }

    /**
     *  
     * @return  The long.
     */
    public long currentTime() {
        return 0l;
    }

    /**
     * Sets the members.
     *  
     * @param arg1 The members.
     */
    public void setMembers(int arg1) {

    }

    /**
     * Sets the senders.
     *  
     * @param arg1 The senders.
     */
    public void setSenders(int arg1) {

    }

    /**
     * Returns the report interval.
     *  
     * @return  The report interval.
     */
    public double getReportInterval() {
        return 0.0d;
    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void updateavgrtcpsize(int arg1) {

    }

    /**
     * Returns the my source.
     *  
     * @return  The my source.
     */
    public RtpSource getMySource() {
        return (RtpSource) null;
    }

} // end RtcpSession
