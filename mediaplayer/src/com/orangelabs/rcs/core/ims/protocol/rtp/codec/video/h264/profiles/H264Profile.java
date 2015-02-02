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

package com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.profiles;

import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264TypeLevel.*;
import com.orangelabs.rcs.core.ims.protocol.rtp.util.HexadecimalUtils;

/**
 * Represent H264 base Profile
 *
 * @author Deutsche Telekom AG
 */
public abstract class H264Profile {

    /**
     * Video width
     */
    private int videoWidth;

    /**
     * Video height
     */
    private int videoHeight;

    /**
     * Video frame rate
     */
    private float frameRate;

    /**
     * Video bit rate
     */
    private int bitRate;

    /**
     * Packet size
     */
    private int packetSize;

    /**
     * Level type (1, 1b, 1.1...)
     */
    private H264TypeLevel level;

    /**
     * Profile type (BASELINE, MAIN...)
     */
    private H264TypeProfile type;

    /**
     * Codec parameters
     */
    private String codeParams;

    /**
     * Profile level id
     */
    private String levelId;

    /**
     * Profile name
     */
    private String profileName;

    /**
     * Profile IOP
     */
    private Byte profileIOP;

    /**
     * Base constructor for H264 Profiles
     *
     * @param profileName Profile name
     * @param level Profile level
     * @param type Profile type
     * @param levelId Profile level id
     * @param videoWidth Video with
     * @param videoHeight Video height
     * @param frameRate Frame rate
     * @param bitRate Bit rate
     * @param packetSize Packet size
     * @param codeParams Codec parameters
     */
    public H264Profile(String profileName,
            H264TypeLevel level,
            H264TypeProfile type,
            String levelId,
            int videoWidth,
            int videoHeight,
            float frameRate,
            int bitRate,
            int packetSize,
            String codeParams) {

        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.frameRate = frameRate;
        this.bitRate = bitRate;
        this.packetSize = packetSize;
        this.level = level;
        this.type = type;
        this.codeParams = codeParams;
        this.levelId = levelId;
        this.profileIOP = getProfileIOPFromLevelId(levelId);
    }

    public String getCodeParams() {
        return codeParams;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public float getFrameRate() {
        return frameRate;
    }

    public int getBitRate() {
        return bitRate;
    }

    public int getPacketSize() {
        return packetSize;
    }

    public H264TypeLevel getLevel() {
        return level;
    }

    public H264TypeProfile getType() {
        return type;
    }

    public String getLevelId() {
        return levelId;
    }

    public String getProfileName() {
        return profileName;
    }

    public Byte getProfileIOP() {
        return profileIOP;
    }

    /**
     * Get the byte that represents the profile IDC
     *
     * @param profileId H264 profile-id
     * @return profile IDC
     */
    public static Byte getProfileIDCFromLevelId(final String profileLevelId) {
        return getProfileInformationFromLevelId(profileLevelId, 0);
    }

    /**
     * Get the byte that represents the profile IOP
     *
     * @param profileId H264 profile-id
     * @return profile IOP
     */
    public static Byte getProfileIOPFromLevelId(final String profileLevelId) {
        return getProfileInformationFromLevelId(profileLevelId, 1);
    }

    /**
     * Get the byte that represents the level IDC
     * 
     * @param profileId H264 profile-id
     * @return level IDC
     */
    public static Byte getLevelIDCFromLevelId(final String profileLevelId) {
        return getProfileInformationFromLevelId(profileLevelId, 2);
    }

    /**
     * Get informations after parse H264 profile-id
     * 
     * @param profileId H264 profile-id
     * @param what 0: Profile IDC, 1: Profile IOP, 2: Level IDC
     * @return
     */
    private static Byte getProfileInformationFromLevelId(final String profileLevelId, int what) {
        byte[] arrProfileId = HexadecimalUtils.hexStringToByteArray(profileLevelId);
        if (arrProfileId == null || arrProfileId.length != 3 || what < 0 || what > 2) {
            return null;
        } else {
            return arrProfileId[what];
        }
    }

    /**
     * Get instance of H264 profile, using profile-id
     * 
     * @param profileId
     * @return {@link H264Profile} if supported, otherwise <code>null</code>
     */
    public static H264Profile getProfile(String profileId) {
        H264Profile profile = null;
        try {

            final Byte profileIDC = getProfileIDCFromLevelId(profileId);
            final Byte profileIOP = getProfileIOPFromLevelId(profileId);
            final Byte levelIDC = getLevelIDCFromLevelId(profileId);

            if (profileIDC == null || profileIOP == null || levelIDC == null) {
                return null;
            }

            // constraintSet3Flag is the X bit on YYYX YYYY
            int constraintSet3FlagValue = ((profileIOP >> 4) & 0x01);
            H264ConstraintSetFlagType constraintSet3Flag = ((constraintSet3FlagValue == 1) ? H264ConstraintSetFlagType.TRUE
                    : H264ConstraintSetFlagType.FALSE);

            H264TypeLevel level = H264TypeLevel.getH264LevelType(levelIDC,
                    constraintSet3Flag);

            if (H264TypeLevel.LEVEL_1 == level) {
                profile = new H264Profile1();
            } else if (H264TypeLevel.LEVEL_1B == level) {
                profile = new H264Profile1b();
            } else if (H264TypeLevel.LEVEL_1_1 == level) {
                profile = new H264Profile1_1();
            } else if (H264TypeLevel.LEVEL_1_2 == level) {
                profile = new H264Profile1_2();
            } else if (H264TypeLevel.LEVEL_1_3 == level) {
                profile = new H264Profile1_3();
            } else {
                profile = null;
            }

        } catch (Exception e) {
        }

        return profile;
    }
}
