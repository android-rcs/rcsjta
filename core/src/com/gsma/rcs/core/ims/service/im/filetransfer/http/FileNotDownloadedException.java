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

package com.gsma.rcs.core.ims.service.im.filetransfer.http;


/**
 * File Not Downloaded Exception
 */
public class FileNotDownloadedException extends Exception {

    static final long serialVersionUID = 1L;

    /**
     * Constructor
     * 
     * @param message Error message obtained either from a constant string or through e.getMessage()
     */
    public FileNotDownloadedException(String message) {
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
    public FileNotDownloadedException(String message, Throwable cause) {
        super(message, cause);
    }
}
