/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.orangelabs.rcs.core.ims.protocol.rtp.core;

/**
 * ReceptionReport based on RFC 3550 specification
 *
 * @author Deutsche Telekom
 */
public class ReceptionReport {

    /**
     * The SSRC identifier of the source to which the information in this
     * reception report block pertains
     */
    private int ssrc;

    /**
     * The fraction of RTP data packets from source SSRC lost since the previous
     * SR or RR packet was sent
     */
    private double fractionLost;

    /**
     * The total number of RTP data packets from source SSRC_n that have been
     * lost since the beginning of reception
     */
    private int cumulativeNumberOfPacketsLost;

    /**
     * The low 16 bits contain the highest sequence number received in an RTP
     * data packet from source SSRC, and the most significant 16 bits extend
     * that sequence number with the corresponding count of sequence number
     * cycles
     */
    private long extendedHighestSequenceNumberReceived;

    /**
     * An estimate of the statistical variance of the RTP data packet
     * interarrival time, measured in timestamp units and expressed as
     * anunsigned integer
     */
    private long interarrivalJitter;

    /**
     * The middle 32 bits out of 64 in the NTP timestamp received as part of the
     * most recent RTCP sender report (SR) packet from source SSRC. If no SR has
     * been received yet, the field is set to zero
     */
    private long lastSenderReport;

    /**
     * The delay, expressed in units of 1/65536 seconds, between receiving the
     * last SR packet from source SSRC and sending this reception report block
     */
    private long delaySinceLastSenderReport;

    /**
     * Default constructor
     *
     * @param ssrc Source identifier
     */
    public ReceptionReport(int ssrc) {
        this.ssrc = ssrc;
    }

    public long getSsrc() {
        return ssrc;
    }

    public int setSsrc(int ssrc) {
        return ssrc;
    }

    public double getFractionLost() {
        return fractionLost;
    }

    public void setFractionLost(double fractionLost) {
        this.fractionLost = fractionLost;
    }

    public int getCumulativeNumberOfPacketsLost() {
        return cumulativeNumberOfPacketsLost;
    }

    public void setCumulativeNumberOfPacketsLost(int cumulativeNumberOfPacketsLost) {
        this.cumulativeNumberOfPacketsLost = cumulativeNumberOfPacketsLost;
    }

    public long getExtendedHighestSequenceNumberReceived() {
        return extendedHighestSequenceNumberReceived;
    }

    public void setExtendedHighestSequenceNumberReceived(long extendedHighestSequenceNumberReceived) {
        this.extendedHighestSequenceNumberReceived = extendedHighestSequenceNumberReceived;
    }

    public long getInterarrivalJitter() {
        return interarrivalJitter;
    }

    public void setInterarrivalJitter(long interarrivalJitter) {
        this.interarrivalJitter = interarrivalJitter;
    }

    public long getLastSenderReport() {
        return lastSenderReport;
    }

    public void setLastSenderReport(long lastSenderReport) {
        this.lastSenderReport = lastSenderReport;
    }

    public long getDelaySinceLastSenderReport() {
        return delaySinceLastSenderReport;
    }

    public void setDelaySinceLastSenderReport(long delaySinceLastSenderReport) {
        this.delaySinceLastSenderReport = delaySinceLastSenderReport;
    }
}
