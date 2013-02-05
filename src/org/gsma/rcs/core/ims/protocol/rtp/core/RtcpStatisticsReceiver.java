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
 * Class RtcpStatisticsReceiver.
 */
public class RtcpStatisticsReceiver {
    /**
     * The num rtcp pkts.
     */
    public int numRtcpPkts;

    /**
     * The num rtcp bytes.
     */
    public int numRtcpBytes;

    /**
     * The num sr pkts.
     */
    public int numSrPkts;

    /**
     * The num bad rtcp pkts.
     */
    public int numBadRtcpPkts;

    /**
     * The num unknown types.
     */
    public int numUnknownTypes;

    /**
     * The num malformed rtcp pkts.
     */
    public int numMalformedRtcpPkts;

    /**
     * Creates a new instance of RtcpStatisticsReceiver.
     */
    public RtcpStatisticsReceiver() {

    }

} // end RtcpStatisticsReceiver
