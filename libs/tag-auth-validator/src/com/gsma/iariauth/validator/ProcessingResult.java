/*
 * Copyright (C) 2014 GSM Association
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.gsma.iariauth.validator;

/**
 * An interface containing the results of processing a package, or of any intermediate step.
 */
public interface ProcessingResult {

    /**
     * Processing results
     */

    /* general - processing failures */
    public static final int STATUS_IO_ERROR = -4;
    public static final int STATUS_CAPABILITY_ERROR = -3;
    public static final int STATUS_INTERNAL_ERROR = -2;
    public static final int STATUS_NOT_PROCESSED = -1;
    /* general */
    public static final int STATUS_OK = 0;
    public static final int STATUS_INVALID = 1;
    public static final int STATUS_DENIED = 2;
    /* signature-related */
    public static final int STATUS_REVOKED = 3;
    public static final int STATUS_VALID_NO_ANCHOR = 4;
    public static final int STATUS_VALID_HAS_ANCHOR = 5;

    /**
     * Retrieve the processing/validation status
     */
    public int getStatus();

    /**
     * Retrieve the processing error Artifact. This will reference the specific failure if document
     * processing or signature validation or verification fail. Null if no error
     */
    public Artifact getError();

    /**
     * Retrieve the processed document
     */
    public IARIAuthDocument getAuthDocument();
}
