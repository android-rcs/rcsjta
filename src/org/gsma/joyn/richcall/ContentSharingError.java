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

package org.gsma.joyn.richcall;

/**
 * Class ContentSharingError.
 */
public class ContentSharingError extends org.gsma.joyn.session.ImsSessionBasedServiceError {
	
	static final long serialVersionUID = 1L;

    /**
     * Constant MEDIA_RENDERER_NOT_INITIALIZED.
     */
    public static final int MEDIA_RENDERER_NOT_INITIALIZED = 131;

    /**
     * Constant MEDIA_TRANSFER_FAILED.
     */
    public static final int MEDIA_TRANSFER_FAILED = 132;

    /**
     * Constant MEDIA_STREAMING_FAILED.
     */
    public static final int MEDIA_STREAMING_FAILED = 133;

    /**
     * Constant UNSUPPORTED_MEDIA_TYPE.
     */
    public static final int UNSUPPORTED_MEDIA_TYPE = 134;

    /**
     * Constant MEDIA_SAVING_FAILED.
     */
    public static final int MEDIA_SAVING_FAILED = 135;

    /**
     * Creates a new instance of ContentSharingError.
     *
     * @param arg1 The arg1.
     */
    public ContentSharingError(org.gsma.joyn.session.ImsServiceError arg1) {
        super((org.gsma.joyn.session.ImsServiceError) null);
    }

    /**
     * Creates a new instance of ContentSharingError.
     *
     * @param arg1 The arg1.
     */
    public ContentSharingError(int arg1) {
        super((org.gsma.joyn.session.ImsServiceError) null);
    }

    /**
     * Creates a new instance of ContentSharingError.
     *
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public ContentSharingError(int arg1, String arg2) {
        super((org.gsma.joyn.session.ImsServiceError) null);
    }

} // end ContentSharingError
