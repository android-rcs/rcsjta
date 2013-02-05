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

package org.gsma.rcs.core.ims.protocol.rtp.codec.video.h263.encoder;

/**
 * Class NativeH263EncoderParams.
 */
public class NativeH263EncoderParams {
    /**
     * Constant SIMPLE_PROFILE_LEVEL0.
     */
    public static final int SIMPLE_PROFILE_LEVEL0 = 0;

    /**
     * Constant SIMPLE_PROFILE_LEVEL1.
     */
    public static final int SIMPLE_PROFILE_LEVEL1 = 1;

    /**
     * Constant SIMPLE_PROFILE_LEVEL2.
     */
    public static final int SIMPLE_PROFILE_LEVEL2 = 2;

    /**
     * Constant SIMPLE_PROFILE_LEVEL3.
     */
    public static final int SIMPLE_PROFILE_LEVEL3 = 3;

    /**
     * Constant CORE_PROFILE_LEVEL1.
     */
    public static final int CORE_PROFILE_LEVEL1 = 4;

    /**
     * Constant CORE_PROFILE_LEVEL2.
     */
    public static final int CORE_PROFILE_LEVEL2 = 5;

    /**
     * Constant SIMPLE_SCALABLE_PROFILE_LEVEL0.
     */
    public static final int SIMPLE_SCALABLE_PROFILE_LEVEL0 = 6;

    /**
     * Constant SIMPLE_SCALABLE_PROFILE_LEVEL1.
     */
    public static final int SIMPLE_SCALABLE_PROFILE_LEVEL1 = 7;

    /**
     * Constant SIMPLE_SCALABLE_PROFILE_LEVEL2.
     */
    public static final int SIMPLE_SCALABLE_PROFILE_LEVEL2 = 8;

    /**
     * Constant CORE_SCALABLE_PROFILE_LEVEL1.
     */
    public static final int CORE_SCALABLE_PROFILE_LEVEL1 = 10;

    /**
     * Constant CORE_SCALABLE_PROFILE_LEVEL2.
     */
    public static final int CORE_SCALABLE_PROFILE_LEVEL2 = 11;

    /**
     * Constant CORE_SCALABLE_PROFILE_LEVEL3.
     */
    public static final int CORE_SCALABLE_PROFILE_LEVEL3 = 12;

    /**
     * Constant SHORT_HEADER.
     */
    public static final int SHORT_HEADER = 0;

    /**
     * Constant SHORT_HEADER_WITH_ERR_RES.
     */
    public static final int SHORT_HEADER_WITH_ERR_RES = 1;

    /**
     * Constant H263_MODE.
     */
    public static final int H263_MODE = 2;

    /**
     * Constant H263_MODE_WITH_ERR_RES.
     */
    public static final int H263_MODE_WITH_ERR_RES = 3;

    /**
     * Constant DATA_PARTITIONING_MODE.
     */
    public static final int DATA_PARTITIONING_MODE = 4;

    /**
     * Constant COMBINE_MODE_NO_ERR_RES.
     */
    public static final int COMBINE_MODE_NO_ERR_RES = 5;

    /**
     * Constant COMBINE_MODE_WITH_ERR_RES.
     */
    public static final int COMBINE_MODE_WITH_ERR_RES = 6;

    /**
     * Constant CONSTANT_Q.
     */
    public static final int CONSTANT_Q = 0;

    /**
     * Constant CBR_1.
     */
    public static final int CBR_1 = 1;

    /**
     * Constant VBR_1.
     */
    public static final int VBR_1 = 2;

    /**
     * Constant CBR_2.
     */
    public static final int CBR_2 = 3;

    /**
     * Constant VBR_2.
     */
    public static final int VBR_2 = 4;

    /**
     * Constant CBR_LOWDELAY.
     */
    public static final int CBR_LOWDELAY = 5;

    /**
     * Creates a new instance of NativeH263EncoderParams.
     */
    public NativeH263EncoderParams() {

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
     * Sets the bit rate.
     *  
     * @param arg1 The bit rate.
     */
    public void setBitRate(int arg1) {

    }

    /**
     * Sets the enc mode.
     *  
     * @param arg1 The enc mode.
     */
    public void setEncMode(int arg1) {

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
     * Sets the packet size.
     *  
     * @param arg1 The packet size.
     */
    public void setPacketSize(int arg1) {

    }

    /**
     * Returns the profile_level.
     *  
     * @return  The profile_level.
     */
    public int getProfile_level() {
        return 0;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isRvlcEnable() {
        return false;
    }

    /**
     * Returns the gob header interval.
     *  
     * @return  The gob header interval.
     */
    public int getGobHeaderInterval() {
        return 0;
    }

    /**
     * Returns the num layers.
     *  
     * @return  The num layers.
     */
    public int getNumLayers() {
        return 0;
    }

    /**
     * Returns the time inc res.
     *  
     * @return  The time inc res.
     */
    public int getTimeIncRes() {
        return 0;
    }

    /**
     * Returns the tick per src.
     *  
     * @return  The tick per src.
     */
    public int getTickPerSrc() {
        return 0;
    }

    /**
     * Returns the enc height.
     *  
     * @return  The enc height.
     */
    public int getEncHeight() {
        return 0;
    }

    /**
     * Returns the enc width.
     *  
     * @return  The enc width.
     */
    public int getEncWidth() {
        return 0;
    }

    /**
     * Returns the enc frame rate.
     *  
     * @return  The enc frame rate.
     */
    public float getEncFrameRate() {
        return 0.0f;
    }

    /**
     * Returns the i quant.
     *  
     * @return  The i quant.
     */
    public int getIQuant() {
        return 0;
    }

    /**
     * Returns the p quant.
     *  
     * @return  The p quant.
     */
    public int getPQuant() {
        return 0;
    }

    /**
     * Returns the quant type.
     *  
     * @return  The quant type.
     */
    public int getQuantType() {
        return 0;
    }

    /**
     * Returns the rc type.
     *  
     * @return  The rc type.
     */
    public int getRcType() {
        return 0;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isNoFrameSkipped() {
        return false;
    }

    /**
     * Returns the intra period.
     *  
     * @return  The intra period.
     */
    public int getIntraPeriod() {
        return 0;
    }

    /**
     * Returns the num intra m b.
     *  
     * @return  The num intra m b.
     */
    public int getNumIntraMB() {
        return 0;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isSceneDetect() {
        return false;
    }

    /**
     * Returns the search range.
     *  
     * @return  The search range.
     */
    public int getSearchRange() {
        return 0;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isMv8x8Enable() {
        return false;
    }

    /**
     * Returns the intra d c vlc th.
     *  
     * @return  The intra d c vlc th.
     */
    public int getIntraDCVlcTh() {
        return 0;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isUseACPred() {
        return false;
    }

    /**
     * Sets the profile_level.
     *  
     * @param arg1 The profile_level.
     */
    public void setProfile_level(int arg1) {

    }

    /**
     * Sets the rvlc enable.
     *  
     * @param arg1 The rvlc enable.
     */
    public void setRvlcEnable(boolean arg1) {

    }

    /**
     * Sets the gob header interval.
     *  
     * @param arg1 The gob header interval.
     */
    public void setGobHeaderInterval(int arg1) {

    }

    /**
     * Sets the num layers.
     *  
     * @param arg1 The num layers.
     */
    public void setNumLayers(int arg1) {

    }

    /**
     * Sets the time inc res.
     *  
     * @param arg1 The time inc res.
     */
    public void setTimeIncRes(int arg1) {

    }

    /**
     * Sets the tick per src.
     *  
     * @param arg1 The tick per src.
     */
    public void setTickPerSrc(int arg1) {

    }

    /**
     * Sets the enc height.
     *  
     * @param arg1 The enc height.
     */
    public void setEncHeight(int arg1) {

    }

    /**
     * Sets the enc width.
     *  
     * @param arg1 The enc width.
     */
    public void setEncWidth(int arg1) {

    }

    /**
     * Sets the enc frame rate.
     *  
     * @param arg1 The enc frame rate.
     */
    public void setEncFrameRate(float arg1) {

    }

    /**
     * Sets the i quant.
     *  
     * @param arg1 The i quant.
     */
    public void setIQuant(int arg1) {

    }

    /**
     * Sets the p quant.
     *  
     * @param arg1 The p quant.
     */
    public void setPQuant(int arg1) {

    }

    /**
     * Sets the quant type.
     *  
     * @param arg1 The quant type.
     */
    public void setQuantType(int arg1) {

    }

    /**
     * Sets the rc type.
     *  
     * @param arg1 The rc type.
     */
    public void setRcType(int arg1) {

    }

    /**
     * Sets the no frame skipped.
     *  
     * @param arg1 The no frame skipped.
     */
    public void setNoFrameSkipped(boolean arg1) {

    }

    /**
     * Sets the intra period.
     *  
     * @param arg1 The intra period.
     */
    public void setIntraPeriod(int arg1) {

    }

    /**
     * Sets the num intra m b.
     *  
     * @param arg1 The num intra m b.
     */
    public void setNumIntraMB(int arg1) {

    }

    /**
     * Sets the scene detect.
     *  
     * @param arg1 The scene detect.
     */
    public void setSceneDetect(boolean arg1) {

    }

    /**
     * Sets the search range.
     *  
     * @param arg1 The search range.
     */
    public void setSearchRange(int arg1) {

    }

    /**
     * Sets the mv8x8 enable.
     *  
     * @param arg1 The mv8x8 enable.
     */
    public void setMv8x8Enable(boolean arg1) {

    }

    /**
     * Sets the intra d c vlc th.
     *  
     * @param arg1 The intra d c vlc th.
     */
    public void setIntraDCVlcTh(int arg1) {

    }

    /**
     * Sets the use a c pred.
     *  
     * @param arg1 The use a c pred.
     */
    public void setUseACPred(boolean arg1) {

    }

    /**
     * Returns the vbv delay.
     *  
     * @return  The vbv delay.
     */
    public float getVbvDelay() {
        return 0.0f;
    }

    /**
     * Sets the vbv delay.
     *  
     * @param arg1 The vbv delay.
     */
    public void setVbvDelay(float arg1) {

    }

} // end NativeH263EncoderParams
