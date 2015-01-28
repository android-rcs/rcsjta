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
 * RTP source
 *
 * @author jexa7410
 * @author Deutsche Telekom
 */
public class RtpSource {
    /**
     * RFC 3550: The dropout parameter MAX_DROPOUT should be a small fraction of
     * the 16-bit sequence number space to give a reasonable probability that
     * new sequence numbers after a restart will not fall in the acceptable
     * range for sequence numbers from before the restart.
     */
    private static final int MAX_DROPOUT = 3000;

    /**
     * RFC 3550: the sequence number is considered valid if it is no more than
     * MAX_DROPOUT ahead of maxSeq nor more than MAX_MISORDER behind
     */
    private static final int MAX_MISORDER = 100;

    /**
     * RFC 3550: RTP sequence number module
     */
    private static final int RTP_SEQ_MOD = (1 << 16);

	/**
	 * CNAME value
	 */
    public static String CNAME = "anonymous@127.0.0.1";

    /**
     * Source is not valid until MIN_SEQUENTIAL packets with
     * sequential sequence numbers have been received.
     */
    private static int MIN_SEQUENCIAL = 0;

    /**
     * Is this source and ActiveSender.
     */
    public boolean activeSender;

    /**
     * Source description
     */
    public int ssrc;

    /**
     * Highest Sequence number received from this source
     */
    private int maxSeq;

    /**
     * Keep track of the wrapping around of RTP sequence numbers, since RTP Seq No. are only 16 bits
     */
    private int cycles;

    /**
     * Sequence Number of the first RTP packet received from this source
     */
    private int baseSeq;

    /**
     * Last 'bad' sequence number + 1
     */
    private int badSeq;

    /**
     * Sequence packets till source is valid
     */
    private int probation;

    /**
     * Packets received
     */
    private int received;

    /**
     * Packet expected at last interval
     */
    private int expectedPrior;

    /**
     * Packet received at last interval
     */
    private int receivedPrior;

    /**
     * Estimated jitter.
     */
    public long jitter;
    
    /**
     * Last SR Packet timestamp
     */
    private long lastSenderReport;


    /**
     * Constructor requires an SSRC for it to be a valid source. The constructor initializes
     * all the source class members to a default value
     *
     * @param   sourceSSRC SSRC of the new source
     */
    RtpSource(int sourceSSRC) {
        ssrc = sourceSSRC;
        lastSenderReport = 0;
        probation = MIN_SEQUENCIAL;
        jitter = 0;
        initSeq(-1);
    }

    /**
     * Generates the extended sequence number.
     *
     * @param seq Original sequence number
     * @return Extended sequence number
     */
    public int generateExtendedSequenceNumber(int seq) {
        return seq + (RTP_SEQ_MOD * cycles);
    }

    /**
     * Updates the statistics related to Sender Reports. Should be invoked when
     * a RTCP Sender Report is received.
     *
     * @param srp Sender Report
     */
    public void receivedSenderReport(RtcpSenderReportPacket srp) {
        // RFC 3550: last SR timestamp (LSR): 32 bits - The middle 32 bits out
        // of 64 in the NTP timestamp received as part of the most recent RTCP
        // sender report
        lastSenderReport = (((srp.ntptimestampmsw << 32) | srp.ntptimestamplsw) & 0x0000ffffffff0000L) >>> 16;
    }

    /**
     * Updates the statistics related to RTP packets Should be invoked every
     * time this source receive an RTP Packet .
     *
     * @param packet
     */
    public void receiveRtpPacket(RtpPacket packet) {
        if (baseSeq == -1) {
            // First packet received
            initSeq(packet.seqnum);
        }
        updateSeq(packet.seqnum);
    }

    /**
     * Generate the Reception Report
     *
     * @return ReceptionReport
     */
    public ReceptionReport generateReceptionReport() {
        ReceptionReport report = new ReceptionReport(ssrc);
        updateReceptionReport(report);
        return report;
    }

    /**
     * Updates the reception report with latest data. The statistics calculation
     * is based on the algorithms present in RFC 3550
     *
     * @param report Reception report to update
     */
    public void updateReceptionReport(ReceptionReport report) {
        // Calculate the number of packets lost
        int extendedMax = getExtendedSequenceNumber();
        int expected = extendedMax - baseSeq + 1;
        report.setCumulativeNumberOfPacketsLost(expected - received);

        // TODO : Calculate the delay after last sender report received
        report.setDelaySinceLastSenderReport(0);
        report.setExtendedHighestSequenceNumberReceived(getExtendedSequenceNumber());

        // Calculate the fraction lost
        long expectedInterval = expected - expectedPrior;
        expectedPrior = expected;
        int receivedInterval = received - receivedPrior;
        receivedPrior = received;
        long lostInterval = expectedInterval - receivedInterval;
        if (expectedInterval == 0 || lostInterval <= 0) {
            report.setFractionLost(0);
        } else {
            report.setFractionLost((lostInterval << 8) / (double) expectedInterval);
        }

        // TODO : Calculate jitter
        report.setInterarrivalJitter(0);

        report.setLastSenderReport(lastSenderReport);
        report.setSsrc(ssrc);
    }

    /**
     * Set the Source description
     *
     * @param ssrc
     */
    public void setSsrc(int ssrc) {
        this.ssrc = ssrc;
    }

    /**
     * Initiate sequence. RFC 3550
     *
     * @param sequenceNumber
     */
    private void initSeq(int sequenceNumber) {
        baseSeq = sequenceNumber;
        maxSeq = sequenceNumber;
        badSeq = RTP_SEQ_MOD + 1; // so seq == bad_seq is false
        cycles = 0;
        received = 0;
        receivedPrior = 0;
        expectedPrior = 0;
    }

    /**
     * Ensures that a source is declared valid only after MIN_SEQUENTIAL packets
     * have been received in sequence.It also validates the sequence number seq
     * of a newly received packet and updates the sequence state.
     *
     * Algorithm in the RFC 3550 (Appendix A.1)
     *
     * @param seq Sequence Number
     */
    private int updateSeq(int seq) {
        long udelta = seq - maxSeq;

        // Source is not valid until MIN_SEQUENTIAL packets with sequential
        // sequence numbers have been received.
        if (probation > 0) {
            if (seq == maxSeq + 1) {
                probation--;
                maxSeq = seq;
                if (probation == 0) {
                    initSeq(seq);
                    received++;
                    return 1;
                }
            } else {
                probation = MIN_SEQUENCIAL - 1;
                maxSeq = seq;
                return 1;
            }
            return 0;
        } else if (udelta < MAX_DROPOUT) {
            // in order, with permissible gap
            if (seq < maxSeq && (udelta >= (MAX_MISORDER * -1))) {
                // late packet within interval
                received++;
                return 1;
            }

            if (seq < maxSeq) {
                // Sequence number wrapped - count another 64K cycle.
                cycles++;
            }
            maxSeq = seq;
        } else if (udelta <= RTP_SEQ_MOD - MAX_MISORDER) {
            // the sequence number made a very large jump
            if (seq == badSeq) {
                // Two sequential packets -- assume that the other side
                // restarted without telling us so just re-sync
                // (i.e., pretend this was the first packet).
                initSeq(seq);
            } else {
                badSeq = (seq + 1) & (RTP_SEQ_MOD - 1);
                return 0;
            }
        } else {
            // duplicate or reordered packet
        }
        received++;
        return 1;
    }

    /**
     * Return the extended sequence number for a source considering that
     * sequences cycle.
     *
     * @return Extended sequence number
     */
    private int getExtendedSequenceNumber() {
        return generateExtendedSequenceNumber(maxSeq);
    }
}