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
 * Class NativeH264EncoderParams.
 */
public class NativeH264EncoderParams {
    /**
     * Constant PROFILE_DEFAULT.
     */
    public static final int PROFILE_DEFAULT = 0;

    /**
     * Constant PROFILE_BASELINE.
     */
    public static final int PROFILE_BASELINE = 1;

    /**
     * Constant PROFILE_MAIN.
     */
    public static final int PROFILE_MAIN = 2;

    /**
     * Constant PROFILE_EXTENDED.
     */
    public static final int PROFILE_EXTENDED = 3;

    /**
     * Constant PROFILE_HIGH.
     */
    public static final int PROFILE_HIGH = 4;

    /**
     * Constant PROFILE_HIGH10.
     */
    public static final int PROFILE_HIGH10 = 5;

    /**
     * Constant PROFILE_HIGH422.
     */
    public static final int PROFILE_HIGH422 = 6;

    /**
     * Constant PROFILE_HIGH444.
     */
    public static final int PROFILE_HIGH444 = 7;

    /**
     * Constant LEVEL_AUTODETECT.
     */
    public static final int LEVEL_AUTODETECT = 0;

    /**
     * Constant LEVEL_1.
     */
    public static final int LEVEL_1 = 1;

    /**
     * Constant LEVEL_1B.
     */
    public static final int LEVEL_1B = 2;

    /**
     * Constant LEVEL_11.
     */
    public static final int LEVEL_11 = 3;

    /**
     * Constant LEVEL_12.
     */
    public static final int LEVEL_12 = 4;

    /**
     * Constant LEVEL_13.
     */
    public static final int LEVEL_13 = 5;

    /**
     * Constant LEVEL_2.
     */
    public static final int LEVEL_2 = 6;

    /**
     * Constant LEVEL_21.
     */
    public static final int LEVEL_21 = 7;

    /**
     * Constant LEVEL_22.
     */
    public static final int LEVEL_22 = 8;

    /**
     * Constant LEVEL_3.
     */
    public static final int LEVEL_3 = 9;

    /**
     * Constant LEVEL_31.
     */
    public static final int LEVEL_31 = 10;

    /**
     * Constant LEVEL_32.
     */
    public static final int LEVEL_32 = 11;

    /**
     * Constant LEVEL_4.
     */
    public static final int LEVEL_4 = 12;

    /**
     * Constant LEVEL_41.
     */
    public static final int LEVEL_41 = 13;

    /**
     * Constant LEVEL_42.
     */
    public static final int LEVEL_42 = 14;

    /**
     * Constant LEVEL_5.
     */
    public static final int LEVEL_5 = 15;

    /**
     * Constant LEVEL_51.
     */
    public static final int LEVEL_51 = 16;

    /**
     * Constant VIDEO_FORMAT_RGB24.
     */
    public static final int VIDEO_FORMAT_RGB24 = 0;

    /**
     * Constant VIDEO_FORMAT_RGB12.
     */
    public static final int VIDEO_FORMAT_RGB12 = 1;

    /**
     * Constant VIDEO_FORMAT_YUV420.
     */
    public static final int VIDEO_FORMAT_YUV420 = 2;

    /**
     * Constant VIDEO_FORMAT_UYVY.
     */
    public static final int VIDEO_FORMAT_UYVY = 3;

    /**
     * Constant VIDEO_FORMAT_YUV420SEMIPLANAR.
     */
    public static final int VIDEO_FORMAT_YUV420SEMIPLANAR = 4;

    /**
     * Constant ENCODING_MODE_TWOWAY.
     */
    public static final int ENCODING_MODE_TWOWAY = 0;

    /**
     * Constant ENCODING_MODE_RECORDER.
     */
    public static final int ENCODING_MODE_RECORDER = 1;

    /**
     * Constant ENCODING_MODE_STREAMING.
     */
    public static final int ENCODING_MODE_STREAMING = 2;

    /**
     * Constant ENCODING_MODE_DOWNLOAD.
     */
    public static final int ENCODING_MODE_DOWNLOAD = 3;

    /**
     * Constant OUTPUT_FORMAT_ANNEXB.
     */
    public static final int OUTPUT_FORMAT_ANNEXB = 0;

    /**
     * Constant OUTPUT_FORMAT_MP4.
     */
    public static final int OUTPUT_FORMAT_MP4 = 1;

    /**
     * Constant OUTPUT_FORMAT_RTP.
     */
    public static final int OUTPUT_FORMAT_RTP = 2;

    /**
     * Constant RATE_CONTROL_TYPE_CONSTANT_Q.
     */
    public static final int RATE_CONTROL_TYPE_CONSTANT_Q = 0;

    /**
     * Constant RATE_CONTROL_TYPE_CBR_1.
     */
    public static final int RATE_CONTROL_TYPE_CBR_1 = 1;

    /**
     * Constant RATE_CONTROL_TYPE_VBR_1.
     */
    public static final int RATE_CONTROL_TYPE_VBR_1 = 2;

    /**
     * Creates a new instance of NativeH264EncoderParams.
     */
    public NativeH264EncoderParams() {

    }

    /**
     * Creates a new instance of NativeH264EncoderParams.
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @param arg3 The arg3.
     * @param arg4 The arg4.
     * @param arg5 The arg5.
     * @param arg6 The arg6.
     * @param arg7 The arg7.
     * @param arg8 The arg8.
     */
    public NativeH264EncoderParams(org.gsma.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264TypeProfile arg1, byte arg2, org.gsma.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264TypeLevel arg3, int arg4, int arg5, int arg6, float arg7, int arg8) {

    }

    /**
     * Sets the level.
     *  
     * @param arg1 The level.
     */
    public void setLevel(org.gsma.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264TypeLevel arg1) {

    }

    /**
     * Returns the level.
     *  
     * @return  The level.
     */
    public int getLevel() {
        return 0;
    }

    /**
     * Returns the frame rate.
     *  
     * @return  The frame rate.
     */
    public float getFrameRate() {
        return 0.0f;
    }

    /**
     * Returns the bit rate.
     *  
     * @return  The bit rate.
     */
    public int getBitRate() {
        return 0;
    }

    /**
     * Returns the packet size.
     *  
     * @return  The packet size.
     */
    public int getPacketSize() {
        return 0;
    }

    /**
     * Returns the profile i o p.
     *  
     * @return  The profile i o p.
     */
    public byte getProfileIOP() {
        return (byte) 0;
    }

    /**
     * Returns the profile.
     *  
     * @return  The profile.
     */
    public int getProfile() {
        return 0;
    }

    /**
     * Sets the output format.
     *  
     * @param arg1 The output format.
     */
    public void setOutputFormat(int arg1) {

    }

    /**
     * Returns the output format.
     *  
     * @return  The output format.
     */
    public int getOutputFormat() {
        return 0;
    }

    /**
     * Sets the frame width.
     *  
     * @param arg1 The frame width.
     */
    public void setFrameWidth(int arg1) {

    }

    /**
     * Sets the frame height.
     *  
     * @param arg1 The frame height.
     */
    public void setFrameHeight(int arg1) {

    }

    /**
     * Sets the frame rate.
     *  
     * @param arg1 The frame rate.
     */
    public void setFrameRate(float arg1) {

    }

    /**
     * Sets the bit rate.
     *  
     * @param arg1 The bit rate.
     */
    public void setBitRate(int arg1) {

    }

    /**
     * Sets the profiles and level.
     *  
     * @param arg1 The profiles and level.
     */
    public void setProfilesAndLevel(String arg1) {

    }

    /**
     * Sets the enc mode.
     *  
     * @param arg1 The enc mode.
     */
    public void setEncMode(int arg1) {

    }

    /**
     * Sets the scene detection.
     *  
     * @param arg1 The scene detection.
     */
    public void setSceneDetection(boolean arg1) {

    }

    /**
     * Sets the i frame interval.
     *  
     * @param arg1 The i frame interval.
     */
    public void setIFrameInterval(int arg1) {

    }

    /**
     * Sets the frame orientation.
     *  
     * @param arg1 The frame orientation.
     */
    public void setFrameOrientation(int arg1) {

    }

    /**
     * Sets the profile.
     *  
     * @param arg1 The profile.
     */
    public void setProfile(org.gsma.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264TypeProfile arg1) {

    }

    /**
     * Sets the profile i o p.
     *  
     * @param arg1 The profile i o p.
     */
    public void setProfileIOP(byte arg1) {

    }

    /**
     * Returns the frame width.
     *  
     * @return  The frame width.
     */
    public int getFrameWidth() {
        return 0;
    }

    /**
     * Returns the frame height.
     *  
     * @return  The frame height.
     */
    public int getFrameHeight() {
        return 0;
    }

    /**
     * Returns the frame orientation.
     *  
     * @return  The frame orientation.
     */
    public int getFrameOrientation() {
        return 0;
    }

    /**
     * Returns the video format.
     *  
     * @return  The video format.
     */
    public int getVideoFormat() {
        return 0;
    }

    /**
     * Sets the video format.
     *  
     * @param arg1 The video format.
     */
    public void setVideoFormat(int arg1) {

    }

    /**
     * Returns the encode i d.
     *  
     * @return  The encode i d.
     */
    public int getEncodeID() {
        return 0;
    }

    /**
     * Sets the encode i d.
     *  
     * @param arg1 The encode i d.
     */
    public void setEncodeID(int arg1) {

    }

    /**
     * Returns the num layer.
     *  
     * @return  The num layer.
     */
    public int getNumLayer() {
        return 0;
    }

    /**
     * Sets the num layer.
     *  
     * @param arg1 The num layer.
     */
    public void setNumLayer(int arg1) {

    }

    /**
     * Returns the enc mode.
     *  
     * @return  The enc mode.
     */
    public int getEncMode() {
        return 0;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isOutOfBandParamSet() {
        return false;
    }

    /**
     * Sets the out of band param set.
     *  
     * @param arg1 The out of band param set.
     */
    public void setOutOfBandParamSet(boolean arg1) {

    }

    /**
     * Sets the packet size.
     *  
     * @param arg1 The packet size.
     */
    public void setPacketSize(int arg1) {

    }

    /**
     * Returns the rate control type.
     *  
     * @return  The rate control type.
     */
    public int getRateControlType() {
        return 0;
    }

    /**
     * Sets the rate control type.
     *  
     * @param arg1 The rate control type.
     */
    public void setRateControlType(int arg1) {

    }

    /**
     * Returns the buffer delay.
     *  
     * @return  The buffer delay.
     */
    public float getBufferDelay() {
        return 0.0f;
    }

    /**
     * Sets the buffer delay.
     *  
     * @param arg1 The buffer delay.
     */
    public void setBufferDelay(float arg1) {

    }

    /**
     * Returns the iquant.
     *  
     * @return  The iquant.
     */
    public int getIquant() {
        return 0;
    }

    /**
     * Sets the iquant.
     *  
     * @param arg1 The iquant.
     */
    public void setIquant(int arg1) {

    }

    /**
     * Returns the pquant.
     *  
     * @return  The pquant.
     */
    public int getPquant() {
        return 0;
    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void setiPquant(int arg1) {

    }

    /**
     * Returns the bquant.
     *  
     * @return  The bquant.
     */
    public int getBquant() {
        return 0;
    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void setiBquant(int arg1) {

    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isSceneDetection() {
        return false;
    }

    /**
     * Returns the i frame interval.
     *  
     * @return  The i frame interval.
     */
    public int getIFrameInterval() {
        return 0;
    }

    /**
     * Returns the num intra m b refresh.
     *  
     * @return  The num intra m b refresh.
     */
    public int getNumIntraMBRefresh() {
        return 0;
    }

    /**
     * Sets the num intra m b refresh.
     *  
     * @param arg1 The num intra m b refresh.
     */
    public void setNumIntraMBRefresh(int arg1) {

    }

    /**
     * Returns the clip duration.
     *  
     * @return  The clip duration.
     */
    public int getClipDuration() {
        return 0;
    }

    /**
     * Sets the clip duration.
     *  
     * @param arg1 The clip duration.
     */
    public void setClipDuration(int arg1) {

    }

    /**
     * Returns the f s i buff.
     *  
     * @return  The f s i buff array.
     */
    public byte[] getFSIBuff() {
        return (byte []) null;
    }

    /**
     * Sets the f s i buff.
     *  
     * @param arg1 The f s i buff array.
     */
    public void setFSIBuff(byte[] arg1) {

    }

    /**
     * Returns the f s i buff length.
     *  
     * @return  The f s i buff length.
     */
    public int getFSIBuffLength() {
        return 0;
    }

    /**
     * Sets the f s i buff length.
     *  
     * @param arg1 The f s i buff length.
     */
    public void setFSIBuffLength(int arg1) {

    }

    /**
     * Parses the h264 type profile.
     *  
     * @param arg1 The arg1.
     * @return  The int.
     */
    public static int parseH264TypeProfile(org.gsma.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264TypeProfile arg1) {
        return 0;
    }

    /**
     * Parses the h264 type level.
     *  
     * @param arg1 The arg1.
     * @return  The int.
     */
    public static int parseH264TypeLevel(org.gsma.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264TypeLevel arg1) {
        return 0;
    }

} // end NativeH264EncoderParams
