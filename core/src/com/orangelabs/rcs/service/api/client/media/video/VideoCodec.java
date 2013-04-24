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

package com.orangelabs.rcs.service.api.client.media.video;

import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.service.api.client.media.MediaCodec;

/**
 * Video codec
 * 
 * @author hlxn7157
 */
public class VideoCodec {

    /**
     * Media codec
     */
    private MediaCodec mediaCodec;

    /**
     * Payload key
     */
    private static final String PAYLOAD = "payload";

    /**
     * Clock rate key
     */
    private static final String CLOCKRATE = "clockRate";

    /**
     * Codec param key
     */
    private static final String CODECPARAMS = "codecParams";

    /**
     * Frame rate key
     */
    private static final String FRAMERATE = "framerate";

    /**
     * Bit rate key
     */
    private static final String BITRATE = "bitrate";

    /**
     * Codec width key
     */
    private static final String CODECWIDTH = "codecWidth";

    /**
     * Codec height key
     */
    private static final String CODECHEIGHT = "codecHeight";

    /**
     * Constructor
     *
     * @param codecName Codec name
     * @param clockRate Clock rate
     * @param codecParams Codec parameters
     * @param framerate Frame rate
     * @param bitrate Bit rate
     * @param width Video width
     * @param height Video height
     */
    public VideoCodec(String codecName, int payload, int clockRate, String codecParams, int framerate,
            int bitrate, int width, int height) {
        mediaCodec = new MediaCodec(codecName);
        mediaCodec.setIntParam(PAYLOAD, payload);
        mediaCodec.setIntParam(CLOCKRATE, clockRate);
        mediaCodec.setStringParam(CODECPARAMS, codecParams);
        mediaCodec.setIntParam(FRAMERATE, framerate);
        mediaCodec.setIntParam(BITRATE, bitrate);
        mediaCodec.setIntParam(CODECWIDTH, width);
        mediaCodec.setIntParam(CODECHEIGHT, height);
    }

    /**
     * Constructor
     * 
     * @param mediaCodec Media codec
     */
    public VideoCodec(MediaCodec mediaCodec) {
        this.mediaCodec = mediaCodec;
    }

    /**
     * Get media codec
     * 
     * @return Media codec
     */
    public MediaCodec getMediaCodec() {
        return mediaCodec;
    }

    /**
     * Get codec name
     * 
     * @return Codec name
     */
    public String getCodecName() {
        return mediaCodec.getCodecName();
    }

    /**
     * Get payload
     * 
     * @return Payload
     */
    public int getPayload() {
        return mediaCodec.getIntParam(PAYLOAD, 96);
    }

    /**
     * Get video clock rate
     * 
     * @return Video clock rate
     */
    public int getClockRate() {
        return mediaCodec.getIntParam(CLOCKRATE, 90000);
    }

    /**
     * Get video codec parameters
     * 
     * @return Video codec parameters
     */
    public String getCodecParams() {
        return mediaCodec.getStringParam(CODECPARAMS);
    }

    /**
     * Get video frame rate
     * 
     * @return Video frame rate
     */
    public int getFramerate() {
        return mediaCodec.getIntParam(FRAMERATE, 15);
    }

    /**
     * Get video bitrate
     * 
     * @return Video bitrate
     */
    public int getBitrate() {
        return mediaCodec.getIntParam(BITRATE, 0);
    }

    /**
     * Get video width
     * 
     * @return Video width
     */
    public int getWidth() {
        return mediaCodec.getIntParam(CODECWIDTH, 176);
    }

    /**
     * Get video height
     * 
     * @return Video height
     */
    public int getHeight() {
        return mediaCodec.getIntParam(CODECHEIGHT, 144);
    }

    /**
     * Compare codec encodings and resolutions
     *
     * @param codec Codec to compare
     * @return True if codecs are equals
     */
    public boolean compare(VideoCodec codec) {
        boolean ret = false;
        if (getCodecName().equalsIgnoreCase(codec.getCodecName()) 
                && (getWidth() == codec.getWidth() || getWidth() == 0 || codec.getWidth() == 0)
                && (getHeight() == codec.getHeight() || getHeight() == 0 || codec.getHeight() == 0)) {
            if (getCodecName().equalsIgnoreCase(H264Config.CODEC_NAME)) {
                if (H264Config.getCodecProfileLevelId(getCodecParams()).compareToIgnoreCase(H264Config.getCodecProfileLevelId(codec.getCodecParams())) == 0) {
                    ret =  true;
                }
            } else {
                if (getCodecParams().equalsIgnoreCase(codec.getCodecParams())) {
                    ret = true;
                }
            }
        }
        return ret;
    }

    /**
     * Check if a codec is in a list
     *
     * @param supportedCodecs List of supported codec
     * @param codec Selected codec
     * @return True if the codec is in the list
     */
    public static boolean checkVideoCodec(MediaCodec[] supportedCodecs, VideoCodec codec) {
        for (int i = 0; i < supportedCodecs.length; i++) {
            if (codec.compare(new VideoCodec(supportedCodecs[i]))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns string representation of the video codec
     * 
     * @return String
     */
    public String toString() {
        return "Codec " + getCodecName() + " " + getPayload() + " " +
                getClockRate() + " " + getCodecParams() + " " +
                getFramerate() + " " + getBitrate() + " " +
                getWidth() + "x" + getHeight();
    }
}
