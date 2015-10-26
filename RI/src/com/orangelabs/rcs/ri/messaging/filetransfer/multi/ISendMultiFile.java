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

package com.orangelabs.rcs.ri.messaging.filetransfer.multi;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.filetransfer.FileTransferService;

import java.util.List;

/**
 * @author Philippe LEMORDANT
 */
public interface ISendMultiFile {

    /**
     * Initialize
     */
    void initialize();

    /**
     * Transfers list of files
     * 
     * @param files to transfer
     * @return boolean
     */
    boolean transferFiles(List<FileTransferProperties> files);

    /**
     * Adds file transfer event listener
     * 
     * @param fileTransferService file transfer service
     * @throws RcsServiceException
     */
    void addFileTransferEventListener(FileTransferService fileTransferService)
            throws RcsServiceException;

    /**
     * Removes file transfer event listener
     * 
     * @param fileTransferService file transfer service
     * @throws RcsServiceException
     */
    void removeFileTransferEventListener(FileTransferService fileTransferService)
            throws RcsServiceException;

    /**
     * Checks if it is possible to initiate file transfer to group chat or single contact.
     * 
     * @param chatId the chat ID or contact ID in single chat
     * @return True if it is possible to initiate file transfer to group chat or single contact.
     * @throws RcsServiceException
     */
    boolean checkPermissionToSendFile(String chatId) throws RcsServiceException;
}
