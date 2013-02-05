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
 * Class RtpStatisticsReceiver.
 */
public class RtpStatisticsReceiver {
    /**
     * The num packets.
     */
    public int numPackets;

    /**
     * The num bytes.
     */
    public int numBytes;

    /**
     * The num bad rtp pkts.
     */
    public int numBadRtpPkts;

    /**
     * Creates a new instance of RtpStatisticsReceiver.
     */
    public RtpStatisticsReceiver() {

    }

} // end RtpStatisticsReceiver
