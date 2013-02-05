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

package org.gsma.rcs.core.ims.protocol.rtp.codec.video.h264;

/**
 * Class H264RtpHeaders.
 */
public class H264RtpHeaders {
    /**
     * Constant AVC_NALTYPE_FUA.
     */
    public static final int AVC_NALTYPE_FUA = 28;

    /**
     * Creates a new instance of H264RtpHeaders.
     *  
     * @param arg1 The arg1 array.
     */
    public H264RtpHeaders(byte[] arg1) {

    }

    /**
     *  
     * @return  The string.
     */
    public String toString() {
        return (java.lang.String) null;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isFrameNonInterleaved() {
        return false;
    }

    /**
     * Returns the header size.
     *  
     * @return  The header size.
     */
    public int getHeaderSize() {
        return 0;
    }

    /**
     * Returns the n a l header.
     *  
     * @return  The n a l header.
     */
    public byte getNALHeader() {
        return (byte) 0;
    }

    /**
     * Returns the f u i_ f.
     *  
     * @return  The f u i_ f.
     */
    public boolean getFUI_F() {
        return false;
    }

    /**
     * Returns the f u i_ n r i.
     *  
     * @return  The f u i_ n r i.
     */
    public int getFUI_NRI() {
        return 0;
    }

    /**
     * Returns the f u i_ t y p e.
     *  
     * @return  The f u i_ t y p e.
     */
    public byte getFUI_TYPE() {
        return (byte) 0;
    }

    /**
     * Returns the f u h_ s.
     *  
     * @return  The f u h_ s.
     */
    public boolean getFUH_S() {
        return false;
    }

    /**
     * Returns the f u h_ e.
     *  
     * @return  The f u h_ e.
     */
    public boolean getFUH_E() {
        return false;
    }

    /**
     * Returns the f u h_ r.
     *  
     * @return  The f u h_ r.
     */
    public boolean getFUH_R() {
        return false;
    }

    /**
     * Returns the f u h_ t y p e.
     *  
     * @return  The f u h_ t y p e.
     */
    public byte getFUH_TYPE() {
        return (byte) 0;
    }

} // end H264RtpHeaders
