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

package com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.encoder;

import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264Profile;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264TypeLevel;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264TypeLevel.H264ConstraintSetFlagType;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264TypeProfile;

/**
 * H264 Encoder settings
 *
 * @author Deutsche Telekom AG
 */
public class NativeH264EncoderParams {

    // ----- Contants -----
    // This constants values must be updated with those that were in the encoder
    // codec

    // - Targeted profile to encode -
    public static final int PROFILE_DEFAULT = 0;
    public static final int PROFILE_BASELINE = 1;
    public static final int PROFILE_MAIN = 2;
    public static final int PROFILE_EXTENDED = 3;
    public static final int PROFILE_HIGH = 4;
    public static final int PROFILE_HIGH10 = 5;
    public static final int PROFILE_HIGH422 = 6;
    public static final int PROFILE_HIGH444 = 7;

    // - Targeted level to encode -
    public static final int LEVEL_AUTODETECT = 0;
    public static final int LEVEL_1 = 1;
    public static final int LEVEL_1B = 2;
    public static final int LEVEL_11 = 3;
    public static final int LEVEL_12 = 4;
    public static final int LEVEL_13 = 5;
    public static final int LEVEL_2 = 6;
    public static final int LEVEL_21 = 7;
    public static final int LEVEL_22 = 8;
    public static final int LEVEL_3 = 9;
    public static final int LEVEL_31 = 10;
    public static final int LEVEL_32 = 11;
    public static final int LEVEL_4 = 12;
    public static final int LEVEL_41 = 13;
    public static final int LEVEL_42 = 14;
    public static final int LEVEL_5 = 15;
    public static final int LEVEL_51 = 16;

    // - Contains supported video input format -
    public static final int VIDEO_FORMAT_RGB24 = 0;
    public static final int VIDEO_FORMAT_RGB12 = 1;
    public static final int VIDEO_FORMAT_YUV420 = 2;
    public static final int VIDEO_FORMAT_UYVY = 3;
    public static final int VIDEO_FORMAT_YUV420SEMIPLANAR = 4;

    // - Type of contents for optimal encoding mode -
    public static final int ENCODING_MODE_TWOWAY = 0;
    public static final int ENCODING_MODE_RECORDER = 1;
    public static final int ENCODING_MODE_STREAMING = 2;
    public static final int ENCODING_MODE_DOWNLOAD = 3;

    // - Output format -
    public static final int OUTPUT_FORMAT_ANNEXB = 0;
    public static final int OUTPUT_FORMAT_MP4 = 1;
    public static final int OUTPUT_FORMAT_RTP = 2;

    // - Rate control type -
    public static final int RATE_CONTROL_TYPE_CONSTANT_Q = 0;
    public static final int RATE_CONTROL_TYPE_CBR_1 = 1;
    public static final int RATE_CONTROL_TYPE_VBR_1 = 2;

    // ----- Properties -----

    /**
     * Contains the width in pixels of the input frame.
     */
    private int frameWidth;

    /**
     * Contains the height in pixels of the input frame.
     */
    private int frameHeight;

    /**
     * Contains the input frame rate in the unit of frame per second.
     */
    private float frameRate;

    /**
     * Contains Frame Orientation. Used for RGB input. 1 means Bottom_UP RGB, 0
     * means Top_Down RGB, -1 for video formats other than RGB
     */
    private int frameOrientation; // TODO not implemented yet on the codec side

    /**
     * Contains the format of the input video, e.g., YUV 4:2:0, UYVY, RGB24,
     * etc.
     */
    private int videoFormat;

    /**
     * Specifies an ID that will be used to specify this encoder while returning
     * the bitstream in asynchronous mode.
     */

    private int encodeID;
    /**
     * Specifies the targeted profile, and will also specifies available tools
     * for iEncMode. If default is used, encoder will choose its own preferred
     * profile. If autodetect is used, encoder will check other settings and
     * choose the right profile that doesn't have any conflicts.
     */
    private int profile;

    /**
     * Specifies the targeted profile IOP, composed of the values of constraint
     * flags
     */
    private byte profileIOP;

    /**
     * Specifies the target level When present, other settings will be checked
     * against the range allowable by this target level. Fail will returned upon
     * Initialize call. If not known, users must set it to autodetect. Encoder
     * will calculate the right level that doesn't conflict with other settings.
     */
    private int level;

    /**
     * Specifies whether base only (numLayer = 1) or base + enhancement layer
     * (numLayer =2 ) is to be used.
     */
    private int numLayer;

    /**
     * Specifies the bit rate in bit per second.
     */
    private int bitRate;

    /**
     * Specifies the encoding mode. This translates to the complexity of
     * encoding modes and error resilient tools.
     */
    private int encMode;

    /**
     * Specifies that SPS and PPS are retrieved first and sent out-of-band
     */
    private boolean outOfBandParamSet;

    /**
     * Specifies the desired output format.
     */
    private int outputFormat;

    /**
     * Specifies the packet size in bytes which represents the desired number of
     * bytes per NAL. If this number is set to 0, the encoder will encode the
     * entire slice group as one NAL.
     */
    private int packetSize;

    /**
     * Specifies the rate control algorithm among one of the following constant
     * Q, CBR and VBR.
     */
    private int rateControlType;

    /**
     * Specifies the VBV buffer size which determines the end-to-end delay
     * between the encoder and the decoder. The size is in unit of seconds. For
     * download application, the buffer size can be larger than the streaming
     * application. For 2-way application, this buffer shall be kept minimal.
     * For a special case, in VBR mode, iBufferDelay will be set to -1 to allow
     * buffer underflow.
     */
    private float bufferDelay;

    /**
     * Specifies the initial quantization parameter for the first I-frame. If
     * constant Q rate control is used, this QP will be used for all the
     * I-frames. This number must be set between 1 and 31, otherwise,
     * Initialize() will fail.
     */
    private int iquant;

    /**
     * Specifies the initial quantization parameter for the first P-frame. If
     * constant Q rate control is used, this QP will be used for all the
     * P-frames. This number must be set between 1 and 31, otherwise,
     * Initialize() will fail.
     */
    private int pquant;

    /**
     * Specifies the initial quantization parameter for the first B-frame. If
     * constant Q rate control is used, this QP will be used for all the
     * B-frames. This number must be set between 1 and 31, otherwise,
     * Initialize() will fail.
     */
    private int bquant;

    /**
     * Specifies automatic scene detection where I-frame will be used the the
     * first frame in a new scene.
     */
    private boolean sceneDetection;

    /**
     * Specifies the maximum period in seconds between 2 INTRA frames. An INTRA
     * mode is forced to a frame once this interval is reached. When there is
     * only one I-frame is present at the beginning of the clip, iFrameInterval
     * should be set to -1. For all I-frames coding this number should be set to
     * 0.
     */
    private int iFrameInterval;

    /**
     * According to iIFrameInterval setting, the minimum number of intra MB per
     * frame is optimally calculated for error resiliency. However, when
     * iIFrameInterval is set to -1, numIntraMBRefresh must be specified to
     * guarantee the minimum number of intra macroblocks per frame.
     */
    private int numIntraMBRefresh;

    /**
     * Specifies the duration of the clip in millisecond, needed for VBR encode.
     * Set to 0 if unknown.
     */
    private int clipDuration;

    /**
     * Specify FSI Buffer input
     */
    private byte[] fSIBuff;

    /**
     * Specify FSI Buffer Length
     */
    private int fSIBuffLength;


// ----- Constructors -----

    /**
     * Constructor for native H264Encoder parameters
     */
    public NativeH264EncoderParams() {
        // Default parameter that were being used in the codec, some of them
        // hard coded
        this.frameWidth = 176;
        this.frameHeight = 144;
        this.frameRate = 15;
        this.frameOrientation = 0;
        this.videoFormat = VIDEO_FORMAT_YUV420SEMIPLANAR;
        this.encodeID = 0;
        this.profile = PROFILE_BASELINE;
        this.profileIOP = 0;
        this.level = LEVEL_1B;
        this.numLayer = 1;
        this.bitRate = 64000;
        this.encMode = ENCODING_MODE_TWOWAY;
        this.outOfBandParamSet = true;
        this.outputFormat = OUTPUT_FORMAT_RTP;
        this.packetSize = 8192;
        this.rateControlType = RATE_CONTROL_TYPE_CBR_1;
        this.bufferDelay = 2;
        this.iquant = 15;
        this.pquant = 12;
        this.bquant = 0;
        this.sceneDetection = false;
        this.iFrameInterval = 1;
        this.numIntraMBRefresh = 50;
        this.clipDuration = 0;
        this.fSIBuff = null;
        this.fSIBuffLength = 0;
    }

    /**
     * Constructor for native H264Encoder parameters
     * 
     * @param profileType Profile type
     * @param profileIOP Profile IOP
     * @param levelType Profile level type
     * @param frameWidth Width in pixels of the input frame
     * @param frameHeight Height in pixels of the input frame
     * @param bitRate Bit rate in bit per second
     * @param frameRate Frame rate in the unit of frame per second
     * @param packetSize Packet size in bytes which represents the desired
     *            number of bytes per NAL
     */
    public NativeH264EncoderParams(H264TypeProfile profileType, byte profileIOP, H264TypeLevel levelType,
            int frameWidth, int frameHeight,
            int bitRate, float frameRate, int packetSize) {
        this(); // to fill the default parameters

        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.bitRate = bitRate;
        this.frameRate = frameRate;
        this.packetSize = packetSize;
        setProfile(profileType);
        setLevel(levelType);
        this.profileIOP = profileIOP;
    }

    // ----- Getters and Setters -----

    /**
     * Method to set profiles and level from a given codec parameter string
     *
     * @param codecParams Codec parameters
     */
    public void setProfilesAndLevel(String codecParams) {
        String profile_level_id = H264Config.getCodecProfileLevelId(codecParams);
        Byte profile_idc = H264Profile.getProfileIDCFromLevelId(profile_level_id);
        Byte profile_iop = H264Profile.getProfileIOPFromLevelId(profile_level_id);
        Byte level_idc = H264Profile.getLevelIDCFromLevelId(profile_level_id);

        if (profile_idc != null && profile_iop != null && level_idc != null) {
            // constraintSet3Flag is the X bit on YYYX YYYY
            int constraintSet3FlagValue = ((profile_iop >> 4) & 0x01);
            H264ConstraintSetFlagType constraintSet3Flag = ((constraintSet3FlagValue == 1) ? H264ConstraintSetFlagType.TRUE
                    : H264ConstraintSetFlagType.FALSE);

            setProfileIOP(profile_iop < 0 ? 0 : profile_iop);
            setProfile(H264TypeProfile.getH264ProfileType(profile_idc));
            setLevel(H264TypeLevel.getH264LevelType(level_idc, constraintSet3Flag));
        }
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public void setFrameWidth(int frameWidth) {
        this.frameWidth = frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public void setFrameHeight(int frameHeight) {
        this.frameHeight = frameHeight;
    }

    public float getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(float frameRate) {
        this.frameRate = frameRate;
    }

    public int getFrameOrientation() {
        return frameOrientation;
    }

    public void setFrameOrientation(int frameOrientation) {
        this.frameOrientation = frameOrientation;
    }

    public int getVideoFormat() {
        return videoFormat;
    }

    public void setVideoFormat(int videoFormat) {
        this.videoFormat = videoFormat;
    }

    public int getEncodeID() {
        return encodeID;
    }

    public void setEncodeID(int encodeID) {
        this.encodeID = encodeID;
    }

    public int getProfile() {
        return profile;
    }

    public void setProfile(H264TypeProfile profile) {
        this.profile = parseH264TypeProfile(profile);
    }

    public byte getProfileIOP() {
        return profileIOP;
    }

    public void setProfileIOP(byte profileIOP) {
        this.profileIOP = profileIOP;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(H264TypeLevel level) {
        this.level = parseH264TypeLevel(level);
    }

    public int getNumLayer() {
        return numLayer;
    }

    public void setNumLayer(int numLayer) {
        this.numLayer = numLayer;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public int getEncMode() {
        return encMode;
    }

    public void setEncMode(int encMode) {
        this.encMode = encMode;
    }

    public boolean isOutOfBandParamSet() {
        return outOfBandParamSet;
    }

    public void setOutOfBandParamSet(boolean outOfBandParamSet) {
        this.outOfBandParamSet = outOfBandParamSet;
    }

    public int getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(int outputFormat) {
        this.outputFormat = outputFormat;
    }

    public int getPacketSize() {
        return packetSize;
    }

    public void setPacketSize(int packetSize) {
        this.packetSize = packetSize;
    }

    public int getRateControlType() {
        return rateControlType;
    }

    public void setRateControlType(int rateControlType) {
        this.rateControlType = rateControlType;
    }

    public float getBufferDelay() {
        return bufferDelay;
    }

    public void setBufferDelay(float bufferDelay) {
        this.bufferDelay = bufferDelay;
    }

    public int getIquant() {
        return iquant;
    }

    public void setIquant(int iquant) {
        this.iquant = iquant;
    }

    public int getPquant() {
        return pquant;
    }

    public void setiPquant(int pquant) {
        this.pquant = pquant;
    }

    public int getBquant() {
        return bquant;
    }

    public void setiBquant(int bquant) {
        this.bquant = bquant;
    }

    public boolean isSceneDetection() {
        return sceneDetection;
    }

    public void setSceneDetection(boolean sceneDetection) {
        this.sceneDetection = sceneDetection;
    }

    public int getIFrameInterval() {
        return iFrameInterval;
    }

    public void setIFrameInterval(int iFrameInterval) {
        this.iFrameInterval = iFrameInterval;
    }

    public int getNumIntraMBRefresh() {
        return numIntraMBRefresh;
    }

    public void setNumIntraMBRefresh(int numIntraMBRefresh) {
        this.numIntraMBRefresh = numIntraMBRefresh;
    }

    public int getClipDuration() {
        return clipDuration;
    }

    public void setClipDuration(int clipDuration) {
        this.clipDuration = clipDuration;
    }

    public byte[] getFSIBuff() {
        return fSIBuff;
    }

    public void setFSIBuff(byte[] fSIBuff) {
        this.fSIBuff = fSIBuff;
    }

    public int getFSIBuffLength() {
        return fSIBuffLength;
    }

    public void setFSIBuffLength(int fSIBuffLength) {
        this.fSIBuffLength = fSIBuffLength;
    }

    /**
     * Parse {@link H264TypeLevel} to map encode parameter 'Level'
     *
     * @param level
     * @return map value if valid type, otherwise return <code>-1</code>
     */
    public static int parseH264TypeLevel(H264TypeLevel level) {
        if (level == H264TypeLevel.LEVEL_1) {
            return LEVEL_1;
        } else if (level == H264TypeLevel.LEVEL_1B) {
            return LEVEL_1B;
        } else if (level == H264TypeLevel.LEVEL_1_1) {
            return LEVEL_11;
        } else if (level == H264TypeLevel.LEVEL_1_2) {
            return LEVEL_12;
        } else if (level == H264TypeLevel.LEVEL_1_3) {
            return LEVEL_13;
        } else if (level == H264TypeLevel.LEVEL_2) {
            return LEVEL_2;
        } else if (level == H264TypeLevel.LEVEL_2_1) {
            return LEVEL_21;
        } else if (level == H264TypeLevel.LEVEL_2_2) {
            return LEVEL_22;
        } else if (level == H264TypeLevel.LEVEL_3) {
            return LEVEL_3;
        } else if (level == H264TypeLevel.LEVEL_3_1) {
            return LEVEL_31;
        } else if (level == H264TypeLevel.LEVEL_3_2) {
            return LEVEL_32;
        } else if (level == H264TypeLevel.LEVEL_4) {
            return LEVEL_4;
        } else if (level == H264TypeLevel.LEVEL_4_1) {
            return LEVEL_41;
        } else if (level == H264TypeLevel.LEVEL_4_2) {
            return LEVEL_42;
        } else if (level == H264TypeLevel.LEVEL_5) {
            return LEVEL_5;
        } else if (level == H264TypeLevel.LEVEL_5_1) {
            return LEVEL_51;
        } else {
            return -1;
        }
    }

    /**
     * Parse {@link H264TypeProfile} to map encode parameter 'Profile'
     *
     * @param profile
     * @return map value if valid type, otherwise <code>PROFILE_DEFAULT<code>
     */
    public static int parseH264TypeProfile(H264TypeProfile profile) {
        if (profile == H264TypeProfile.PROFILE_BASELINE) {
            return PROFILE_BASELINE;
        } else if (profile == H264TypeProfile.PROFILE_MAIN) {
            return PROFILE_MAIN;
        } else if (profile == H264TypeProfile.PROFILE_EXTENDED) {
            return PROFILE_EXTENDED;
        } else if (profile == H264TypeProfile.PROFILE_HIGH) {
            return PROFILE_HIGH;
        } else if (profile == H264TypeProfile.PROFILE_HIGH10) {
            return PROFILE_HIGH10;
        } else if (profile == H264TypeProfile.PROFILE_HIGH422) {
            return PROFILE_HIGH422;
        } else if (profile == H264TypeProfile.PROFILE_HIGH444) {
            return PROFILE_HIGH444;
        } else {
            return PROFILE_DEFAULT;
        }
    }

}
