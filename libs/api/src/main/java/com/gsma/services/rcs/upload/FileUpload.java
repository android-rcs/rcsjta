/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

import com.gsma.services.rcs.RcsGenericException;

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
         * Initiating state
         */
        INITIATING(0),

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

        private static SparseArray<State> mValueToEnum = new SparseArray<>();
        static {
            for (State entry : State.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        State(int value) {
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
        public static State valueOf(int value) {
            State entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class " + State.class.getName() + ""
                    + value + "!");
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
     * @return String Upload ID
     * @throws RcsGenericException
     */
    public String getUploadId() throws RcsGenericException {
        try {
            return mUploadInf.getUploadId();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the URI of the file to be uploaded
     * 
     * @return Uri
     * @throws RcsGenericException
     */
    public Uri getFile() throws RcsGenericException {
        try {
            return mUploadInf.getFile();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns info related to the uploaded file on the content server
     * 
     * @return FileUploadInfo info or null if not yet upload or in case of error
     * @throws RcsGenericException
     * @see FileUploadInfo
     */
    public FileUploadInfo getUploadInfo() throws RcsGenericException {
        try {
            return mUploadInf.getUploadInfo();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the state of the upload
     * 
     * @return State
     * @throws RcsGenericException
     * @see FileUpload.State
     */
    public State getState() throws RcsGenericException {
        try {
            return State.valueOf(mUploadInf.getState());

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Aborts the upload
     * 
     * @throws RcsGenericException
     */
    public void abortUpload() throws RcsGenericException {
        try {
            mUploadInf.abortUpload();
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }
}
