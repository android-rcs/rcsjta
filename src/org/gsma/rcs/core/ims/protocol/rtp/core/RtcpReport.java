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
 * Class RtcpReport.
 */
public class RtcpReport {
    /**
     * The ssrc.
     */
    public int ssrc;

    /**
     * The fractionlost.
     */
    public int fractionlost;

    /**
     * The packetslost.
     */
    public int packetslost;

    /**
     * The lastseq.
     */
    public long lastseq;

    /**
     * The jitter.
     */
    public int jitter;

    /**
     * The lsr.
     */
    public long lsr;

    /**
     * The dlsr.
     */
    public long dlsr;

    /**
     * The receipt time.
     */
    public long receiptTime;

    /**
     * Creates a new instance of RtcpReport.
     */
    public RtcpReport() {

    }

    /**
     * Returns the d l s r.
     *  
     * @return  The d l s r.
     */
    public long getDLSR() {
        return 0l;
    }

    /**
     * Returns the fraction lost.
     *  
     * @return  The fraction lost.
     */
    public int getFractionLost() {
        return 0;
    }

    /**
     * Returns the jitter.
     *  
     * @return  The jitter.
     */
    public long getJitter() {
        return 0l;
    }

    /**
     * Returns the l s r.
     *  
     * @return  The l s r.
     */
    public long getLSR() {
        return 0l;
    }

    /**
     * Returns the num lost.
     *  
     * @return  The num lost.
     */
    public long getNumLost() {
        return 0l;
    }

    /**
     * Returns the s s r c.
     *  
     * @return  The s s r c.
     */
    public long getSSRC() {
        return 0l;
    }

    /**
     * Returns the xtnd seq num.
     *  
     * @return  The xtnd seq num.
     */
    public long getXtndSeqNum() {
        return 0l;
    }

} // end RtcpReport
