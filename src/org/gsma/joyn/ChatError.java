/*
 * Copyright 2013, France Telecom
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *    http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.joyn;

import java.lang.String;

/**
 * Class ChatError.
 */
public class ChatError extends org.gsma.joyn.session.ImsSessionBasedServiceError {
	
	static final long serialVersionUID = 1L;

    /**
     * Constant MEDIA_SESSION_FAILED.
     */
    public static final int MEDIA_SESSION_FAILED = 111;

    /**
     * Constant SUBSCRIBE_CONFERENCE_FAILED.
     */
    public static final int SUBSCRIBE_CONFERENCE_FAILED = 112;

    /**
     * Constant SESSION_NOT_FOUND.
     */
    public static final int SESSION_NOT_FOUND = 113;

    /**
     * Constant SESSION_RESTART_FAILED.
     */
    public static final int SESSION_RESTART_FAILED = 114;

    /**
     * Creates a new instance of ChatError.
     *
     * @param arg1 The arg1.
     */
    public ChatError(org.gsma.joyn.session.ImsServiceError arg1) {
        super((org.gsma.joyn.session.ImsServiceError) null);
    }

    /**
     * Creates a new instance of ChatError.
     *
     * @param arg1 The arg1.
     */
    public ChatError(int arg1) {
        super((org.gsma.joyn.session.ImsServiceError) null);
    }

    /**
     * Creates a new instance of ChatError.
     *
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public ChatError(int arg1, String arg2) {
        super((org.gsma.joyn.session.ImsServiceError) null);
    }

} // end ChatError
