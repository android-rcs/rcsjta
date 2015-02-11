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

package com.gsma.rcs.core.ims.service.presence;

import com.gsma.rcs.core.ims.service.ImsServiceError;

/**
 * Presence error
 * 
 * @author jexa7410
 */
public class PresenceError extends ImsServiceError {
    static final long serialVersionUID = 1L;

    /**
     * Subscription has failed (e.g. 404 not found)
     */
    public final static int SUBSCRIBE_FAILED = PRESENCE_ERROR_CODES + 1;

    /**
     * Publish has failed (e.g. 408 timeout)
     */
    public final static int PUBLISH_FAILED = PRESENCE_ERROR_CODES + 2;

    /**
     * Constructor
     * 
     * @param code Error code
     */
    public PresenceError(int code) {
        super(code);
    }

    /**
     * Constructor
     * 
     * @param code Error code
     * @param msg Detail message
     */
    public PresenceError(int code, String msg) {
        super(code, msg);
    }
}
