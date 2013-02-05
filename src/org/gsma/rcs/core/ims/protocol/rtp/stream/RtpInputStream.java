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

package org.gsma.rcs.core.ims.protocol.rtp.stream;

/**
 * Class RtpInputStream.
 */
public class RtpInputStream implements ProcessorInputStream {
    /**
     * Creates a new instance of RtpInputStream.
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @param arg3 The arg3.
     * @param arg4 The arg4.
     */
    public RtpInputStream(String arg1, int arg2, int arg3, org.gsma.rcs.core.ims.protocol.rtp.format.Format arg4) {

    }

    public void close() {

    }

    /**
     *  
     * @return  The buffer.
     */
    public org.gsma.rcs.core.ims.protocol.rtp.util.Buffer read() throws Exception {
        return (org.gsma.rcs.core.ims.protocol.rtp.util.Buffer) null;
    }

    public void open() throws Exception {

    }

    /**
     * Returns the rtp receiver.
     *  
     * @return  The rtp receiver.
     */
    public org.gsma.rcs.core.ims.protocol.rtp.core.RtpPacketReceiver getRtpReceiver() {
        return (org.gsma.rcs.core.ims.protocol.rtp.core.RtpPacketReceiver) null;
    }

    /**
     * Returns the rtcp receiver.
     *  
     * @return  The rtcp receiver.
     */
    public org.gsma.rcs.core.ims.protocol.rtp.core.RtcpPacketReceiver getRtcpReceiver() {
        return (org.gsma.rcs.core.ims.protocol.rtp.core.RtcpPacketReceiver) null;
    }

} // end RtpInputStream
