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
 * Class RtpSource.
 */
public class RtpSource {
    /**
     * The c n a m e.
     */
    public static String CNAME;

    /**
     * The s s r c.
     */
    public int SSRC;

    /**
     * The fraction.
     */
    public double fraction;

    /**
     * The lost.
     */
    public long lost;

    /**
     * The last_seq.
     */
    public long last_seq;

    /**
     * The jitter.
     */
    public long jitter;

    /**
     * The lst.
     */
    public long lst;

    /**
     * The dlsr.
     */
    public double dlsr;

    /**
     * The active sender.
     */
    public boolean activeSender;

    /**
     * The time of last r t c p arrival.
     */
    public double timeOfLastRTCPArrival;

    /**
     * The time of last r t p arrival.
     */
    public double timeOfLastRTPArrival;

    /**
     * The timeof last s r rcvd.
     */
    public double timeofLastSRRcvd;

    /**
     * The no of r t p packets rcvd.
     */
    public int noOfRTPPacketsRcvd;

    /**
     * The base_seq.
     */
    public long base_seq;

    /**
     * The expected.
     */
    public long expected;

    /**
     * The expected_prior.
     */
    public long expected_prior;

    /**
     * The received_prior.
     */
    public long received_prior;

    /**
     * The max_seq.
     */
    public long max_seq;

    /**
     * The cycles.
     */
    public long cycles;

    /**
     * The w r a p m a x.
     */
    public long WRAPMAX;

    /**
     * Creates a new instance of RtpSource.
     *  
     * @param arg1 The arg1.
     */
    RtpSource(int arg1) {

    }

    /**
     *  
     * @return  The int.
     */
    public int updateStatistics() {
        return 0;
    }

    /**
     * Returns the extended max.
     *  
     * @return  The extended max.
     */
    public long getExtendedMax() {
        return 0l;
    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void updateSeq(long arg1) {

    }

} // end RtpSource
