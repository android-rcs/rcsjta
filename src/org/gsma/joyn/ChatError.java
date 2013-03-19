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
import org.gsma.joyn.session.ImsServiceError;
import org.gsma.joyn.session.ImsSessionBasedServiceError;

/**
 * Class ChatError.
 *
 * @author Jean-Marc AUFFRET (Orange) Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public class ChatError extends ImsSessionBasedServiceError {
	
    static final long serialVersionUID = 1L;

    /**
     * Media session has failed (e.g. MSRP failure)
     */
    public static final int MEDIA_SESSION_FAILED = 111;

    /**
     * Subscription to conference package has failed
     */
    public static final int SUBSCRIBE_CONFERENCE_FAILED = 112;

    /**
     * Group chat session not found in the server 
     */
    public static final int SESSION_NOT_FOUND = 113;

    /**
     * Group chat restart has failed
     */
    public static final int SESSION_RESTART_FAILED = 114;

    /**
     * Constructor, creates a new instance of ChatError.
     *
     * @param error
     */
    public ChatError(ImsServiceError error) {
        super((ImsServiceError) null);
    }

    /**
     * Constructor, creates a new instance of ChatError.
     *
     * @param code
     */
    public ChatError(int code) {
        super((ImsServiceError) null);
    }

    /**
     * Constructor, creates a new instance of ChatError.
     *
     * @param code
     * @param msg
     */
    public ChatError(int code, String msg) {
        super((ImsServiceError) null);
    }

}
