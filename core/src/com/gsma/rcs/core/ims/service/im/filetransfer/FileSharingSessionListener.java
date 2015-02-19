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

package com.gsma.rcs.core.ims.service.im.filetransfer;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.services.rcs.contact.ContactId;

/**
 * File transfer session listener
 * 
 * @author jexa7410
 */
public interface FileSharingSessionListener extends ImsSessionListener {
    /**
     * File transfer progress
     * 
     * @param contact Remote contact
     * @param currentSize Data size transfered
     * @param totalSize Total size to be transfered
     */
    public void handleTransferProgress(ContactId contact, long currentSize, long totalSize);

    /**
     * File transfer not allowed to send
     * 
     * @param contact Remote contact
     */
    public void handleTransferNotAllowedToSend(ContactId contact);

    /**
     * File transfer error
     * 
     * @param error Error
     * @param contact Remote contact
     */
    public void handleTransferError(FileSharingError error, ContactId contact);

    /**
     * File has been transfered In case of file transfer over MSRP, the terminating side has
     * received the file, but in case of file transfer over HTTP, only the content server has
     * received the file.
     * 
     * @param content MmContent associated to the received file
     * @param contact Remote contact
     */
    public void handleFileTransfered(MmContent content, ContactId contact);

    /**
     * File transfer has been paused by user
     * 
     * @param contact Remote contact
     */
    public void handleFileTransferPausedByUser(ContactId contact);

    /**
     * File transfer has been paused by system
     * 
     * @param contact Remote contact
     */
    public void handleFileTransferPausedBySystem(ContactId contact);

    /**
     * File transfer has been resumed
     * 
     * @param contact Remote contact
     */
    public void handleFileTransferResumed(ContactId contact);

    /**
     * A session invitation has been received
     * 
     * @param contact Remote contact
     * @param file
     * @param fileIcon
     */
    public void handleSessionInvited(ContactId contact, MmContent file, MmContent fileIcon);

    /**
     * Session is auto-accepted and the session is in the process of being started
     * 
     * @param contact Remote contact
     * @param file
     * @param fileIcon
     */
    public void handleSessionAutoAccepted(ContactId contact, MmContent file, MmContent fileIcon);
}
