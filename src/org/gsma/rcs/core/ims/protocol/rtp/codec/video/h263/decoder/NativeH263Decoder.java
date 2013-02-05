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

package org.gsma.rcs.core.ims.protocol.rtp.codec.video.h263.decoder;

/**
 * Class NativeH263Decoder.
 */
public class NativeH263Decoder {
    /**
     * Creates a new instance of NativeH263Decoder.
     */
    public NativeH263Decoder() {

    }

    /**
     * Returns the video width.
     *  
     * @return  The video width.
     */
    public static int getVideoWidth() {
        return 0;
    }

    /**
     * Returns the video height.
     *  
     * @return  The video height.
     */
    public static int getVideoHeight() {
        return 0;
    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @return  The int.
     */
    public static int InitDecoder(int arg1, int arg2) {
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
     * @param arg1 The arg1.
     * @return  The int.
     */
    public static int InitParser(String arg1) {
        return 0;
    }

    /**
     * Returns the video length.
     *  
     * @return  The video length.
     */
    public static int getVideoLength() {
        return 0;
    }

    /**
     *  
     * @return  The int.
     */
    public static int DeinitParser() {
        return 0;
    }

    /**
     *  
     * @param arg1 The arg1 array.
     * @param arg2 The arg2 array.
     * @param arg3 The arg3.
     * @return  The int.
     */
    public static int DecodeAndConvert(byte[] arg1, int[] arg2, long arg3) {
        return 0;
    }

    /**
     * Returns the video coding.
     *  
     * @return  The video coding.
     */
    public static String getVideoCoding() {
        return (java.lang.String) null;
    }

    /**
     * Returns the video sample.
     *  
     * @param arg1 The arg1 array.
     * @return  The video sample.
     */
    public static VideoSample getVideoSample(int[] arg1) {
        return (VideoSample) null;
    }

} // end NativeH263Decoder
