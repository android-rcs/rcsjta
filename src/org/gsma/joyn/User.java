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

package org.gsma.joyn;

import java.lang.String;

/**
 * Class User.
 */
public class User {
    /**
     * Constant STATE_UNKNOWN.
     */
    public static final String STATE_UNKNOWN = "unknown";

    /**
     * Constant STATE_CONNECTED.
     */
    public static final String STATE_CONNECTED = "connected";

    /**
     * Constant STATE_DISCONNECTED.
     */
    public static final String STATE_DISCONNECTED = "disconnected";

    /**
     * Constant STATE_DEPARTED.
     */
    public static final String STATE_DEPARTED = "departed";

    /**
     * Constant STATE_BOOTED.
     */
    public static final String STATE_BOOTED = "booted";

    /**
     * Constant STATE_FAILED.
     */
    public static final String STATE_FAILED = "failed";

    /**
     * Constant STATE_BUSY.
     */
    public static final String STATE_BUSY = "busy";

    /**
     * Constant STATE_DECLINED.
     */
    public static final String STATE_DECLINED = "declined";

    /**
     * Constant STATE_PENDING.
     */
    public static final String STATE_PENDING = "pending";

    /**
     * Creates a new instance of User.
     *
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public User(String arg1, boolean arg2) {

    }

    /**
     *
     * @return  The string.
     */
    public String toString() {
        return (String) null;
    }

    /**
     * Returns the state.
     *
     * @return  The state.
     */
    public String getState() {
        return (String) null;
    }

    /**
     * Sets the state.
     *
     * @param arg1 The state.
     */
    public void setState(String arg1) {

    }

    /**
     * Returns the display name.
     *
     * @return  The display name.
     */
    public String getDisplayName() {
        return (String) null;
    }

    /**
     * Returns the entity.
     *
     * @return  The entity.
     */
    public String getEntity() {
        return (String) null;
    }

    /**
     * Sets the display name.
     *
     * @param arg1 The display name.
     */
    public void setDisplayName(String arg1) {

    }

    /**
     *
     * @return  The boolean.
     */
    public boolean isMe() {
        return false;
    }

    /**
     * Sets the disconnection method.
     *
     * @param arg1 The disconnection method.
     */
    public void setDisconnectionMethod(String arg1) {

    }

    /**
     * Returns the disconnection method.
     *
     * @return  The disconnection method.
     */
    public String getDisconnectionMethod() {
        return (String) null;
    }

    /**
     * Sets the failure reason.
     *
     * @param arg1 The failure reason.
     */
    public void setFailureReason(String arg1) {

    }

    /**
     * Returns the failure reason.
     *
     * @return  The failure reason.
     */
    public String getFailureReason() {
        return (String) null;
    }

    /**
     *
     * @param arg1 The arg1.
     * @return  The boolean.
     */
    public static boolean isConnected(String arg1) {
        return false;
    }

    /**
     *
     * @param arg1 The arg1.
     * @return  The boolean.
     */
    public static boolean isDisconnected(String arg1) {
        return false;
    }

} // end User
