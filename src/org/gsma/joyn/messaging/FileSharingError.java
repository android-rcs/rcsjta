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

package org.gsma.joyn.messaging;

/**
 * Class FileSharingError.
 */
public class FileSharingError extends org.gsma.joyn.session.ImsSessionBasedServiceError {
	
	static final long serialVersionUID = 1L;

    /**
     * Constant MEDIA_TRANSFER_FAILED.
     */
    public static final int MEDIA_TRANSFER_FAILED = 121;

    /**
     * Constant UNSUPPORTED_MEDIA_TYPE.
     */
    public static final int UNSUPPORTED_MEDIA_TYPE = 122;

    /**
     * Constant MEDIA_SAVING_FAILED.
     */
    public static final int MEDIA_SAVING_FAILED = 123;

    /**
     * Creates a new instance of FileSharingError.
     *
     * @param arg1 The arg1.
     */
    public FileSharingError(org.gsma.joyn.session.ImsServiceError arg1) {
        super((org.gsma.joyn.session.ImsServiceError) null);
    }

    /**
     * Creates a new instance of FileSharingError.
     *
     * @param arg1 The arg1.
     */
    public FileSharingError(int arg1) {
        super((org.gsma.joyn.session.ImsServiceError) null);
    }

    /**
     * Creates a new instance of FileSharingError.
     *
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public FileSharingError(int arg1, String arg2) {
        super((org.gsma.joyn.session.ImsServiceError) null);
    }

} // end FileSharingError
