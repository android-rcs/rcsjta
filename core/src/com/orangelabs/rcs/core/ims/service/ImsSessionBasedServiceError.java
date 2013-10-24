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
package com.orangelabs.rcs.core.ims.service;

/**
 * IMS session based service error
 * 
 * @author jexa7410
 */
public class ImsSessionBasedServiceError extends ImsServiceError {
	static final long serialVersionUID = 1L;
	
	/**
	 * Error code base for chat service
	 */
	protected static final int CHAT_ERROR_CODES = SESSION_ERROR_CODES + 10;
	
	/**
	 * Error code base for file transfer service
	 */
	protected static final int FT_ERROR_CODES = SESSION_ERROR_CODES + 20;

	/**
	 * Error code base for rich call service
	 */
	protected static final int RICHCALL_ERROR_CODES = SESSION_ERROR_CODES + 30;
	
	/**
	 * Error code base for SIP generic service
	 */
	protected static final int SIP_ERROR_CODES = SESSION_ERROR_CODES + 40;
	
	/**
	 * Error code base for IP call service
	 */
	protected static final int IPCALL_ERROR_CODES = SESSION_ERROR_CODES + 50;

	/**
	 * Session initiation has failed (e.g. 408 timeout)
	 */
	public final static int SESSION_INITIATION_FAILED = SESSION_ERROR_CODES + 1;
	
	/**
	 * Session initiation has been declined (e.g. 603 Decline or 486 Busy)
	 */
	public final static int SESSION_INITIATION_DECLINED = SESSION_ERROR_CODES + 2;

	/**
	 * Session initiation has been cancelled (e.g. 487 Session terminated)
	 */
	public final static int SESSION_INITIATION_CANCELLED = SESSION_ERROR_CODES + 3;
	
	/**
     * Constructor
     *
     * @param error Error
     */
    public ImsSessionBasedServiceError(ImsServiceError error) {
        super(error.getErrorCode(), error.getMessage());
    }

	/**
	 * Constructor
	 * 
	 * @param code Error code
	 */
	public ImsSessionBasedServiceError(int code) {
		super(code);
	}
	
	/**
	 * Constructor
	 * 
	 * @param code Error code
	 * @param msg Detail message 
	 */
	public ImsSessionBasedServiceError(int code, String msg) {
		super(code, msg);
	}
}
