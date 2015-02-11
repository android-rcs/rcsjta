/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.upload;

import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;

/**
 * File upload session listener
 * 
 * @author Jean-Marc AUFFRET
 */
public interface FileUploadSessionListener {
    /**
     * Upload started
     */
    public void handleUploadStarted();

    /**
     * Upload progress
     * 
     * @param currentSize Data size transfered
     * @param totalSize Total size to be transfered
     */
    public void handleUploadProgress(long currentSize, long totalSize);

    /**
     * Upload terminated with success
     * 
     * @param info File info document
     */
    public void handleUploadTerminated(FileTransferHttpInfoDocument info);

    /**
     * Upload error
     * 
     * @param error Error
     */
    public void handleUploadError(int error);

    /**
     * Upload aborted
     */
    public void handleUploadAborted();

    /**
     * Not allowed to send
     */
    public void handleUploadNotAllowedToSend();
}
