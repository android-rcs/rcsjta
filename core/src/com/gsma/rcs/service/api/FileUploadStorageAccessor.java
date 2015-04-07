/*
 * Copyright (C) 2010 France Telecom S.A.
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

package com.gsma.rcs.service.api;

import com.gsma.services.rcs.upload.FileUpload.State;
import com.gsma.services.rcs.upload.FileUploadInfo;

import android.net.Uri;

/**
 * FileUploadStorageAccessor helps in retrieving data related to a File upload.
 */
public class FileUploadStorageAccessor {

    private final Uri mFile;

    private FileUploadInfo mInfo;

    private State mState;

    /**
     * Constructor
     * 
     * @param file the file URI
     * @param state State of the file upload
     */
    public FileUploadStorageAccessor(Uri file, State state) {
        mFile = file;
        mState = state;
    }

    /**
     * Gets the file URI
     * 
     * @return the file URI
     */
    public Uri getFile() {
        return mFile;
    }

    /**
     * Gets the information on the uploaded file
     * 
     * @return the information on the uploaded file
     */
    public FileUploadInfo getInfo() {
        return mInfo;
    }

    /**
     * Sets the information of the uploaded file
     * 
     * @param info the information of the uploaded file
     */
    public void setInfo(FileUploadInfo info) {
        mInfo = info;
    }

    /**
     * Gets the state of the file upload
     * 
     * @return the state of the file upload
     */
    public State getState() {
        return mState;
    }

    /**
     * Sets the state of the file upload
     * 
     * @param state the state of the file upload
     */
    public void setState(State state) {
        mState = state;
    }

}
