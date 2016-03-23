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

import com.gsma.services.rcs.upload.FileUpload.State;

/**
 * File upload event listener
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class FileUploadListener {
    /**
     * Callback called when the file upload state has been changed.
     * 
     * @param uploadId ID of upload
     * @param state State of upload
     */
    public abstract void onStateChanged(String uploadId, State state);

    /**
     * Callback called during the upload progress.
     * 
     * @param uploadId ID of upload
     * @param currentSize Current transferred size in bytes
     * @param totalSize Total size to transfer in bytes
     */
    public abstract void onProgressUpdate(String uploadId, long currentSize, long totalSize);

    /**
     * Callback called when the file has been uploaded.
     * 
     * @param uploadId ID of upload
     * @param info Info about the file upload
     */
    public abstract void onUploaded(String uploadId, FileUploadInfo info);
}
