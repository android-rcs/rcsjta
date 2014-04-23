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
package com.orangelabs.rcs.core.ims.service.im.filetransfer;

import com.orangelabs.rcs.core.ims.service.ImsSessionListener;


/**
 * File transfer session listener
 * 
 * @author jexa7410
 */
public interface FileSharingSessionListener extends ImsSessionListener  {
	/**
	 * File transfer progress
	 * 
	 * @param currentSize Data size transfered 
	 * @param totalSize Total size to be transfered
	 */
    public void handleTransferProgress(long currentSize, long totalSize);
    
    /**
     * File transfer error
     * 
     * @param error Error
     */
    public void handleTransferError(FileSharingError error);
    
    /**
     * File has been transfered
     * In case of file transfer over MSRP, the terminating side has received the file, 
     * but in case of file transfer over HTTP, only the content server has received the
     * file.
     *
     * @param filename Filename associated to the received file
     */
    public void handleFileTransfered(String filename);
    
    /**
     * File transfer has been paused
     */
    public void handleFileTransferPaused();
    
    /**
     * File transfer has been resumed
     */
    public void handleFileTransferResumed();
}
