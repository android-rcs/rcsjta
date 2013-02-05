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

package org.gsma.rcs.core.ims.protocol.rtp.codec.video.h264.decoder;

/**
 * Class NativeH264Decoder.
 */
public class NativeH264Decoder {
    /**
     * Creates a new instance of NativeH264Decoder.
     */
    public NativeH264Decoder() {

    }

    /**
     *  
     * @return  The int.
     */
    public static int InitDecoder() {
        return 0;
    }

    /**
     *  
     * @return  The int.
     */
    public static int DeinitDecoder() {
        return 0;
    }

    /**
     *  
     * @param arg1 The arg1 array.
     * @param arg2 The arg2 array.
     * @return  The int.
     */
    public static synchronized int DecodeAndConvert(byte[] arg1, int[] arg2) {
        return 0;
    }

} // end NativeH264Decoder
