/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.services.rcs;

/**
 * Rcs Max Allowed Session Limit Reached Exception.
 * <p>
 * Thrown if the SERVICE TYPE (message/filetransfer/imageshare/geolocationshare etc) cannot be
 * sent/transfered/resent or a new groupchat invitation cannot be sent right now since the limit of
 * allowed ongoing sessions has already been reached and the client needs to wait for at least one
 * session to be released back to the stack first.
 * </p>
 */
public class RcsMaxAllowedSessionLimitReachedException extends RcsServiceException {

    static final long serialVersionUID = 1L;

    /**
     * Constructor
     * 
     * @param message Error message obtained either from a constant string or through e.getMessage()
     */
    public RcsMaxAllowedSessionLimitReachedException(String message) {
        super(message);
    }

    /**
     * Constructor
     * 
     * @param message Error message obtained either from a constant string or through e.getMessage()
     * @param cause the cause (which is saved for later retrieval by the Throwable.getCause()
     *            method). (A null value is permitted, and indicates that the cause is nonexistent
     *            or unknown.)
     */
    public RcsMaxAllowedSessionLimitReachedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Asserts {@link RcsMaxAllowedSessionLimitReachedException}
     * <p>
     * An utility method that will translate the Server side exception to client specific exception
     * by parsing exception message which will have a special formatted exception message with a
     * pre-defined delimiter.
     * </p>
     * 
     * @param e Exception
     * @throws RcsMaxAllowedSessionLimitReachedException
     */
    public static void assertException(Exception e)
            throws RcsMaxAllowedSessionLimitReachedException {
        if (isIntendedException(e, RcsMaxAllowedSessionLimitReachedException.class)) {
            throw new RcsMaxAllowedSessionLimitReachedException(extractServerException(e), e);
        }
    }
}
