/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.core.ims.protocol.rtp;

import com.gsma.rcs.core.ims.protocol.rtp.codec.Codec;
import com.gsma.rcs.core.ims.protocol.rtp.format.Format;
import com.gsma.rcs.core.ims.protocol.rtp.format.audio.AmrWbAudioFormat;
import com.gsma.rcs.core.ims.protocol.rtp.format.audio.AudioFormat;
import com.gsma.rcs.core.ims.protocol.rtp.format.video.H264VideoFormat;
import com.gsma.rcs.core.ims.protocol.rtp.format.video.VideoFormat;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Media registry that handles the supported codecs
 * 
 * @author jexa7410
 */
public class MediaRegistry {

    /**
     * Supported codecs
     */
    private static Hashtable<String, Format> sSupportedCodexs = new Hashtable<String, Format>();
    static {
        sSupportedCodexs.put(H264VideoFormat.ENCODING.toLowerCase(), new H264VideoFormat());
        sSupportedCodexs.put(AmrWbAudioFormat.ENCODING.toLowerCase(), new AmrWbAudioFormat());
    }

    /**
     * Returns the list of the supported video format
     * 
     * @return List of video formats
     */
    public static Vector<VideoFormat> getSupportedVideoFormats() {
        Vector<VideoFormat> list = new Vector<VideoFormat>();
        for (Enumeration<Format> e = sSupportedCodexs.elements(); e.hasMoreElements();) {
            Format fmt = e.nextElement();
            if (fmt instanceof VideoFormat) {
                list.addElement((VideoFormat) fmt);
            }
        }
        return list;
    }

    /**
     * Returns the list of the supported audio format
     * 
     * @return List of audio formats
     */
    public static Vector<AudioFormat> getSupportedAudioFormats() {
        Vector<AudioFormat> list = new Vector<AudioFormat>();
        for (Enumeration<Format> e = sSupportedCodexs.elements(); e.hasMoreElements();) {
            Format fmt = e.nextElement();
            if (fmt instanceof AudioFormat) {
                list.addElement((AudioFormat) fmt);
            }
        }
        return list;
    }

    /**
     * Generate the format associated to the codec name
     * 
     * @param codec Codec name
     * @return Format
     */
    public static Format generateFormat(String codec) {
        return sSupportedCodexs.get(codec.toLowerCase());
    }

    /**
     * Is codec supported
     * 
     * @param codec Codec name
     * @return Boolean
     */
    public static boolean isCodecSupported(String codec) {
        Format format = sSupportedCodexs.get(codec.toLowerCase());
        return (format != null);
    }

    /**
     * Generate the codec encoding chain
     * 
     * @param encoding Encoding name
     * @return Codec chain
     */
    public static Codec[] generateEncodingCodecChain(String encoding) {
        if (encoding.equalsIgnoreCase(H264VideoFormat.ENCODING)) {
            // Java H264 packetizer
            Codec[] chain = {
                new com.gsma.rcs.core.ims.protocol.rtp.codec.video.h264.JavaPacketizer()
            };
            return chain;
        }
        // Codec implemented in the native part
        return new Codec[0];
    }

    /**
     * Generate the decoding codec chain
     * 
     * @param encoding Encoding name
     * @return Codec chain
     */
    public static Codec[] generateDecodingCodecChain(String encoding) {
        if (encoding.equalsIgnoreCase(H264VideoFormat.ENCODING)) {
            // Java H264 depacketizer
            Codec[] chain = {
                new com.gsma.rcs.core.ims.protocol.rtp.codec.video.h264.JavaDepacketizer()
            };
            return chain;
        }
        // Codec implemented in the native part
        return new Codec[0];
    }
}
