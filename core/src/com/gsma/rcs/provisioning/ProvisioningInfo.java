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

package com.gsma.rcs.provisioning;

/**
 * Provisioning info
 * 
 * @author jexa7410
 */
public class ProvisioningInfo {
    /**
     * Version of the provisioning document
     */
    private int mVersion;

    /**
     * Validity of the provisioning document
     */
    private long mValidity = 0L;

    /**
     * Token of the provisioning document
     */
    private String mToken;

    /**
     * Validity of the token of the provisioning document
     */
    private long mTokenValidity = 0L;

    /**
     * Title for terms and conditions
     */
    private String mTitle;

    /**
     * Message for terms and conditions
     */
    private String mMessage;

    /**
     * Accept button for terms and conditions
     */
    private boolean mAcceptBtn = false;

    /**
     * Reject button for terms and conditions
     */
    private boolean mRejectBtn = false;

    /**
     * Enumerated for the provisioning version
     */
    public enum Version {
        /**
         * The configuration is reseted : RCS client is temporary disabled.
         */
        RESETED(0),
        /**
         * The configuration is reseted : RCS client is forbidden.
         */
        RESETED_NOQUERY(-1),
        /**
         * The RCS client is disabled and configuration query stopped.
         */
        DISABLED_NOQUERY(-2),
        /**
         * The RCS client is in dormant state: RCS is disabled but provisioning is still running.
         */
        DISABLED_DORMANT(-3);

        private int mVers;

        private Version(int vers) {
            mVers = vers;
        }

        public int toInt() {
            return mVers;
        }

    }

    /**
     * Set version
     * 
     * @param version
     */
    public void setVersion(int version) {
        mVersion = version;
    }

    /**
     * Set validity in milliseconds
     * 
     * @param validity
     */
    public void setValidity(long validity) {
        mValidity = validity;
    }

    /**
     * Set title
     * 
     * @param title
     */
    public void setTitle(String title) {
        mTitle = title;
    }

    /**
     * Set message
     * 
     * @param message
     */
    public void setMessage(String message) {
        mMessage = message;
    }

    /**
     * Set AcceptBtn
     * 
     * @param acceptBtn
     */
    public void setAcceptBtn(boolean acceptBtn) {
        mAcceptBtn = acceptBtn;
    }

    /**
     * Set RejectBtn
     * 
     * @param rejectBtn
     */
    public void setRejectBtn(boolean rejectBtn) {
        mRejectBtn = rejectBtn;
    }

    /**
     * Get version
     * 
     * @return version
     */
    public int getVersion() {
        return mVersion;
    }

    /**
     * Get validity in milliseconds
     * 
     * @return validity
     */
    public long getValidity() {
        return mValidity;
    }

    /**
     * Get title
     * 
     * @return title
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Get message
     * 
     * @return message
     */
    public String getMessage() {
        return mMessage;
    }

    /**
     * Get acceptBtn
     * 
     * @return acceptBtn
     */
    public boolean getAcceptBtn() {
        return mAcceptBtn;
    }

    /**
     * Get rejectBtn
     * 
     * @return rejectBtn
     */
    public boolean getRejectBtn() {
        return mRejectBtn;
    }

    /**
     * Get token
     * 
     * @return token
     */
    public String getToken() {
        return mToken;
    }

    /**
     * Set token
     * 
     * @param token
     */
    public void setToken(String token) {
        mToken = token;
    }

    /**
     * Get token validity
     * 
     * @return token validity
     */
    public long getTokenValidity() {
        return mTokenValidity;
    }

    /**
     * Set token validity
     * 
     * @param tokenValidity
     */
    public void setTokenValidity(long tokenValidity) {
        mTokenValidity = tokenValidity;
    }
}
