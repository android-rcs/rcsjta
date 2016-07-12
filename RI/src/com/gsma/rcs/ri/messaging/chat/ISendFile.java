/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.ri.messaging.chat;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferService;

import android.net.Uri;

/**
 * @author Philippe LEMORDANT
 */
public interface ISendFile {

    /**
     * Initialize
     */
    void initialize();

    /**
     * Transfer file
     * 
     * @param file Uri of file to transfer
     * @param disposition the file disposition
     * @param fileIcon File icon option. If true, the stack tries to attach fileicon.
     * @return True if file transfer is successful
     */
    boolean transferFile(Uri file, FileTransfer.Disposition disposition, boolean fileIcon);

    /**
     * Add file transfer event listener
     * 
     * @param fileTransferService the file transfer service
     */
    void addFileTransferEventListener(FileTransferService fileTransferService)
            throws RcsServiceException;

    /**
     * Remove file transfer event listener
     * 
     * @param fileTransferService the file transfer service
     */
    void removeFileTransferEventListener(FileTransferService fileTransferService)
            throws RcsServiceException;

}
