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

package org.gsma.rcs.core.ims.protocol.rtp.codec.video.h263;

/**
 * Class H263RtpHeader.
 */
public class H263RtpHeader {
    /**
     * The h e a d e r_ s i z e.
     */
    public int HEADER_SIZE;

    /**
     * The r r.
     */
    public byte RR;

    /**
     * The p.
     */
    public boolean P;

    /**
     * The v.
     */
    public boolean V;

    /**
     * The p l e n.
     */
    public int PLEN;

    /**
     * The p e b i t.
     */
    public int PEBIT;

    /**
     * Creates a new instance of H263RtpHeader.
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @param arg3 The arg3.
     * @param arg4 The arg4.
     * @param arg5 The arg5.
     */
    public H263RtpHeader(byte arg1, boolean arg2, boolean arg3, int arg4, int arg5) {

    }

    /**
     * Creates a new instance of H263RtpHeader.
     *  
     * @param arg1 The arg1 array.
     */
    public H263RtpHeader(byte[] arg1) {

    }

} // end H263RtpHeader
