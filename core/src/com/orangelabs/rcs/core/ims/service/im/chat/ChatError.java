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

package com.orangelabs.rcs.core.ims.service.im.chat;

import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.ImsSessionBasedServiceError;

/**
 * Chat error
 * 
 * @author jexa7410
 */
public class ChatError extends ImsSessionBasedServiceError {
	static final long serialVersionUID = 1L;

	/**
	 * Media session has failed (e.g. MSRP failure)
	 */
	public final static int MEDIA_SESSION_FAILED = CHAT_ERROR_CODES + 1;
	
	/**
	 * Subscription to conference package has failed
	 */
	public final static int SUBSCRIBE_CONFERENCE_FAILED = CHAT_ERROR_CODES + 2;
	
	/**
	 * Group chat session not found in the server 
	 */
	public final static int SESSION_NOT_FOUND = CHAT_ERROR_CODES + 3;
	
	/**
	 * Group chat restart has failed
	 */
	public final static int SESSION_RESTART_FAILED = CHAT_ERROR_CODES + 4;

    /**
     * Media session can't be used for sending any longer (e.g. MSRP error report received)
     */
    public final static int MEDIA_SESSION_BROKEN = CHAT_ERROR_CODES + 5;
    
    /**
     * Constructor
     *
     * @param error Error code
     */
    public ChatError(ImsServiceError error) {
        super(error.getErrorCode(), error.getMessage());
    }

	/**
	 * Constructor
	 * 
	 * @param code Error code
	 */
	public ChatError(int code) {
		super(code);
	}
	
	/**
	 * Constructor
	 * 
	 * @param code Error code
	 * @param msg Detail message 
	 */
	public ChatError(int code, String msg) {
		super(code, msg);
	}
}
