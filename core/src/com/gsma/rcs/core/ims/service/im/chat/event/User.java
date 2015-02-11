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

package com.gsma.rcs.core.ims.service.im.chat.event;

/**
 * @author YPLO6403
 */
public class User {
    public final static String STATE_UNKNOWN = "unknown";
    public final static String STATE_CONNECTED = "connected";
    public final static String STATE_DISCONNECTED = "disconnected";
    public final static String STATE_DEPARTED = "departed";
    public final static String STATE_BOOTED = "booted";
    public final static String STATE_FAILED = "failed";
    public final static String STATE_BUSY = "busy";
    public final static String STATE_DECLINED = "declined";
    public final static String STATE_PENDING = "pending";

    private String entity;

    private boolean me;

    private String state = STATE_UNKNOWN;

    private String displayName = null;

    private String disconnectionMethod = null;

    private String failureReason = null;

    public User(String entity, boolean me) {
        this.entity = entity;
        this.me = me;
    }

    public String getEntity() {
        return entity;
    }

    public boolean isMe() {
        return me;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDisconnectionMethod(String method) {
        this.disconnectionMethod = method;
    }

    public String getDisconnectionMethod() {
        return disconnectionMethod;
    }

    public void setFailureReason(String reason) {
        this.failureReason = reason;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String toString() {
        String result = "user=" + entity + ", state=" + state;
        if (disconnectionMethod != null) {
            result += ", method=" + disconnectionMethod;
        }
        if (failureReason != null) {
            result += ", reason=" + failureReason;
        }
        return result;
    }

}
