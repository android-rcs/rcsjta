/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.im.chat.event;

/**
 * @author YPLO6403
 */
public class User {

    public final static String STATE_CONNECTED = "connected";

    public final static String STATE_DISCONNECTED = "disconnected";

    public final static String STATE_DEPARTED = "departed";

    public final static String STATE_BOOTED = "booted";

    public final static String STATE_FAILED = "failed";

    public final static String STATE_BUSY = "busy";

    public final static String STATE_DECLINED = "declined";

    public final static String STATE_PENDING = "pending";

    private final String mEntity;

    private final boolean mMe;

    private final String mState;

    private final String mDisplayName;

    private final String mDisconnectionMethod;

    private final String mFailureReason;

    public User(String entity, boolean me, String state, String displayName,
            String disconnectionMethod, String failureReason) {
        mEntity = entity;
        mMe = me;
        mState = state;
        mDisplayName = displayName;
        mDisconnectionMethod = disconnectionMethod;
        mFailureReason = failureReason;
    }

    public String getEntity() {
        return mEntity;
    }

    public boolean isMe() {
        return mMe;
    }

    public String getState() {
        return mState;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public String getDisconnectionMethod() {
        return mDisconnectionMethod;
    }

    public String getFailureReason() {
        return mFailureReason;
    }

    public String toString() {
        StringBuilder result = new StringBuilder("user=").append(mEntity).append(", state=")
                .append(mState);
        if (mDisconnectionMethod != null) {
            result.append(", method=").append(mDisconnectionMethod);
        }
        if (mFailureReason != null) {
            result.append(", reason=").append(mFailureReason);
        }
        return result.toString();
    }

}
