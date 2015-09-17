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

package com.gsma.services.rcs;

/**
 * Rcs Persistent Storage Exception
 * <p>
 * Thrown when a method of the service API is called to persist data or read back persisted data
 * failed. This can be because the underlying persistent storage database (or possibly further on a
 * CPM cloud) reported an error such as no more entries can be added perhaps because disk is full,
 * or just that a SQL operation failed or even a unsuccessful read operation from persistent
 * storage.
 * </p>
 */
public class RcsPersistentStorageException extends RcsServiceException {

    static final long serialVersionUID = 1L;

    /**
     * Constructor
     * 
     * @param message Error message obtained either from a constant string or through e.getMessage()
     */
    public RcsPersistentStorageException(String message) {
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
    public RcsPersistentStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Asserts {@link RcsPersistentStorageException}
     * <p>
     * An utility method that will translate the Server side exception to client specific exception
     * by parsing exception message which will have a special formatted exception message with a
     * pre-defined delimiter.
     * </p>
     * 
     * @param e Exception
     * @throws RcsPersistentStorageException
     */
    public static void assertException(Exception e) throws RcsPersistentStorageException {
        if (isIntendedException(e, RcsPersistentStorageException.class)) {
            throw new RcsPersistentStorageException(extractServerException(e), e);
        }
    }
}
