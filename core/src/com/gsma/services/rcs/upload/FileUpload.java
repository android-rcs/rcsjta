/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.services.rcs.upload;

import com.gsma.services.rcs.RcsServiceException;

import android.net.Uri;
import android.util.SparseArray;

/**
 * File upload
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileUpload {

    /**
     * File upload state
     */
    public enum State {

        /**
         * Inactive state
         */
        INACTIVE(0),

        /**
         * Upload is started
         */
        STARTED(1),

        /**
         * Upload has been aborted
         */
        ABORTED(2),

        /**
         * Upload has failed
         */
        FAILED(3),

        /**
         * File has been transferred with success
         */
        TRANSFERRED(4);

        private final int mValue;

        private static SparseArray<State> mValueToEnum = new SparseArray<State>();
        static {
            for (State entry : State.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private State(int value) {
            mValue = value;
        }

        /**
         * Returns the value of this State as an integer.
         * 
         * @return integer value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * Returns a State instance representing the specified integer value.
         * 
         * @param value the integer value
         * @return State instance
         */
        public static final State valueOf(int value) {
            State entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException(new StringBuilder("No enum const class ")
                    .append(State.class.getName()).append(".").append(value).append("!").toString());
        }
    }

    /**
     * File upload interface
     */
    private IFileUpload mUploadInf;

    /**
     * Constructor
     * 
     * @param uploadInf Upload interface
     */
    /* package private */FileUpload(IFileUpload uploadInf) {
        mUploadInf = uploadInf;
    }

    /**
     * Returns the upload ID of the upload
     * 
     * @return Upload ID
     * @throws RcsServiceException
     */
    public String getUploadId() throws RcsServiceException {
        try {
            return mUploadInf.getUploadId();
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Returns the URI of the file to be uploaded
     * 
     * @return Uri
     * @throws RcsServiceException
     */
    public Uri getFile() throws RcsServiceException {
        try {
            return mUploadInf.getFile();
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Returns info related to the uploaded file on the content server
     * 
     * @return Upload info or null if not yet upload or in case of error
     * @see FileUploadInfo
     * @throws RcsServiceException
     */
    public FileUploadInfo getUploadInfo() throws RcsServiceException {
        try {
            return mUploadInf.getUploadInfo();
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Returns the state of the upload
     * 
     * @return State
     * @see FileUpload.State
     * @throws RcsServiceException
     */
    public State getState() throws RcsServiceException {
        try {
            return State.valueOf(mUploadInf.getState());
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Aborts the upload
     * 
     * @throws RcsServiceException
     */
    public void abortUpload() throws RcsServiceException {
        try {
            mUploadInf.abortUpload();
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }
}
