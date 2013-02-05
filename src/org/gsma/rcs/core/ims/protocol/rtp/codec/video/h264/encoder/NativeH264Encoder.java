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

package org.gsma.rcs.core.ims.protocol.rtp.codec.video.h264.encoder;

/**
 * Class NativeH264Encoder.
 */
public class NativeH264Encoder {
    /**
     * Creates a new instance of NativeH264Encoder.
     */
    public NativeH264Encoder() {

    }

    /**
     *  
     * @param arg1 The arg1.
     * @return  The int.
     */
    public static int InitEncoder(NativeH264EncoderParams arg1) {
        return 0;
    }

    /**
     *  
     * @param arg1 The arg1 array.
     * @param arg2 The arg2.
     * @param arg3 The arg3.
     * @param arg4 The arg4.
     * @return  The byte array.
     */
    public static byte[] EncodeFrame(byte[] arg1, long arg2, boolean arg3, float arg4) {
        return (byte []) null;
    }

    /**
     * Returns the n a l.
     *  
     * @return  The n a l array.
     */
    public static byte[] getNAL() {
        return (byte []) null;
    }

    /**
     *  
     * @return  The int.
     */
    public static int DeinitEncoder() {
        return 0;
    }

    /**
     * Returns the last encode status.
     *  
     * @return  The last encode status.
     */
    public static int getLastEncodeStatus() {
        return 0;
    }

} // end NativeH264Encoder
