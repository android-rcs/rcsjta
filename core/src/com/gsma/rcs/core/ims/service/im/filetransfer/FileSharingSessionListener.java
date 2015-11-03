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
import com.gsma.rcs.provider.settings.RcsSettingsData.FileTransferProtocol;
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
    void onTransferProgress(ContactId contact, long currentSize, long totalSize);

    /**
     * File transfer not allowed to send
     * 
     * @param contact Remote contact
     */
    void onTransferNotAllowedToSend(ContactId contact);

    /**
     * File transfer error
     * 
     * @param error Error
     * @param contact Remote contact
     */
    void onTransferError(FileSharingError error, ContactId contact);

    /**
     * File has been transferred In case of file transfer over MSRP, the terminating side has
     * received the file, but in case of file transfer over HTTP, only the content server has
     * received the file.
     * 
     * @param content MmContent associated to the received file
     * @param contact Remote contact
     * @param fileExpiration the time when file on the content server is no longer valid to download
     * @param fileIconExpiration the time when file icon on the content server is no longer valid to
     *            download
     * @param ftProtocol FileTransferProtocol
     */
    void onFileTransferred(MmContent content, ContactId contact, long fileExpiration,
            long fileIconExpiration, FileTransferProtocol ftProtocol);

    /**
     * File transfer has been paused by user
     * 
     * @param contact Remote contact
     */
    void onFileTransferPausedByUser(ContactId contact);

    /**
     * File transfer has been paused by system
     * 
     * @param contact Remote contact
     */
    void onFileTransferPausedBySystem(ContactId contact);

    /**
     * File transfer has been resumed
     * 
     * @param contact Remote contact
     */
    void onFileTransferResumed(ContactId contact);

    /**
     * A session invitation has been received
     * 
     * @param contact Remote contact
     * @param file the file content
     * @param fileIcon the file icon
     * @param timestamp Local timestamp when got file sharing
     * @param timestampSent Remote timestamp sent in payload for the file sharing
     * @param fileExpiration the file expiration
     * @param fileIconExpiration the file icon expiration
     */
    void onSessionInvited(ContactId contact, MmContent file, MmContent fileIcon, long timestamp,
            long timestampSent, long fileExpiration, long fileIconExpiration);

    /**
     * Session is auto-accepted and the session is in the process of being started
     * 
     * @param contact Remote contact
     * @param file the file content
     * @param fileIcon the file icon
     * @param timestamp Local timestamp when got file sharing
     * @param timestampSent Remote timestamp sent in payload for the file sharing
     * @param fileExpiration the file expiration
     * @param fileIconExpiration the file icon expiration
     */
    void onSessionAutoAccepted(ContactId contact, MmContent file, MmContent fileIcon,
            long timestamp, long timestampSent, long fileExpiration, long fileIconExpiration);

    /**
     * HTTP download information is available
     */
    void onHttpDownloadInfoAvailable();

}
