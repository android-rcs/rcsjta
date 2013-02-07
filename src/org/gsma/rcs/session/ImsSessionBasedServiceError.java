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

package org.gsma.rcs.session;

/**
 * Class ImsSessionBasedServiceError.
 */
public class ImsSessionBasedServiceError extends ImsServiceError {
	
	static final long serialVersionUID = 1L;

    /**
     * Constant SESSION_INITIATION_FAILED.
     */
    public static final int SESSION_INITIATION_FAILED = 101;

    /**
     * Constant SESSION_INITIATION_DECLINED.
     */
    public static final int SESSION_INITIATION_DECLINED = 102;

    /**
     * Constant SESSION_INITIATION_CANCELLED.
     */
    public static final int SESSION_INITIATION_CANCELLED = 103;

    /**
     * Constant CHAT_ERROR_CODES.
     */
    protected static final int CHAT_ERROR_CODES = 0;

    /**
     * Constant FT_ERROR_CODES.
     */
    protected static final int FT_ERROR_CODES = 0;

    /**
     * Constant RICHCALL_ERROR_CODES.
     */
    protected static final int RICHCALL_ERROR_CODES = 0;

    /**
     * Constant SIP_ERROR_CODES.
     */
    protected static final int SIP_ERROR_CODES = 0;

    /**
     * Creates a new instance of ImsSessionBasedServiceError.
     *
     * @param arg1 The arg1.
     */
    public ImsSessionBasedServiceError(ImsServiceError arg1) {
        super(0);
    }

    /**
     * Creates a new instance of ImsSessionBasedServiceError.
     *
     * @param arg1 The arg1.
     */
    public ImsSessionBasedServiceError(int arg1) {
        super(0);
    }

    /**
     * Creates a new instance of ImsSessionBasedServiceError.
     *
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public ImsSessionBasedServiceError(int arg1, String arg2) {
        super(0);
    }

} // end ImsSessionBasedServiceError
