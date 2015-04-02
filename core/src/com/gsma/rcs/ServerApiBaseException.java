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

package com.gsma.rcs;

import android.text.TextUtils;

/**
 * Parent for those exception subclasses which are intended to be thrown over AIDL layer.
 * <p>
 * <b>Un-Checked Exception. </b>
 * </p>
 * <p>
 * To avoid over-logging this class doesn't intend to log exceptions for each of the sub-classes
 * that extend this class, as some of them might have occurred due to improper usage of terminal api
 * and hence should be corrected on that layer.
 * </p>
 * <p>
 * The decision for sub-classes that whether they want to be get logged or not is based on the
 * implementation of <i>shouldBeLogged()</i>
 * </p>
 */
public abstract class ServerApiBaseException extends IllegalStateException {

    /**
     * Special Delimiter that will be appended while propagating server exceptions over AIDL layer.
     */
    private static final char DELIMITER_PIPE = '|';

    static final long serialVersionUID = 1L;

    /**
     * Helper method to validate exception message.
     * <p>
     * Exception message should never be NULL or EMPTY, this is strictly mandated for RCS customized
     * exceptions as there will never be an instance where we should not be able to specify the
     * reason for the thrown exception
     * </p>
     * 
     * @param message
     * @return
     */
    private static String validateExceptionMessage(String message) {
        if (TextUtils.isEmpty(message)) {
            throw new IllegalArgumentException("Exception message must never be NULL or EMPTY!");
        }

        return message;
    }

    /**
     * Constructor
     * 
     * @param clazz <i>Class.class</i> of the SubClasses
     * @param message Error message obtained either from a constant string or through e.getMessage()
     */
    protected ServerApiBaseException(Class clazz, String message) {
        super(new StringBuilder(clazz.getName()).append(DELIMITER_PIPE)
                .append(validateExceptionMessage(message)).toString());

    }

    /**
     * Constructor
     * 
     * @param clazz <i>Class.class</i> of the SubClasses
     * @param message Error message obtained either from a constant string or through e.getMessage()
     * @param cause the cause (which is saved for later retrieval by the Throwable.getCause()
     *            method). (A null value is permitted, and indicates that the cause is nonexistent
     *            or unknown.)
     */
    protected ServerApiBaseException(Class clazz, String message, Throwable cause) {
        super(new StringBuilder(clazz.getName()).append(DELIMITER_PIPE)
                .append(validateExceptionMessage(message)).toString(), cause);
    }

    /**
     * Api for the subclasses to decide if this exception should be treated as a bug and hence to be
     * get logged or not in the service layer just before AIDL connection to client.
     * 
     * @return boolean TRUE if exception should not be logged.
     */
    public abstract boolean shouldNotBeLogged();
}
