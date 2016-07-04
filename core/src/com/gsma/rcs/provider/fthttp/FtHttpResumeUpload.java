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

package com.gsma.rcs.provider.fthttp;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

/**
 * @author YPLO6403 Class to handle FtHttpResumeUpload data objects
 */
public class FtHttpResumeUpload extends FtHttpResume {

    /**
     * The FT HTTP Transfer Id
     */
    final private String mTId;

    /**
     * Creates a FT HTTP resume upload data object
     * 
     * @param file the {@code file} value.
     * @param fileIcon the {@code fileIcon} value.
     * @param tId the {@code tId} value.
     * @param contact the {@code contactId} value.
     * @param chatId the {@code chatId} value.
     * @param fileTransferId the {@code fileTransferId} value.
     * @param isGroup the {@code isGroup} value.
     * @param timestamp the {@code timestamp} value.
     * @param timestampSent the {@code timestampSent} value.
     */
    public FtHttpResumeUpload(MmContent file, Uri fileIcon, String tId, ContactId contact,
            String chatId, String fileTransferId, boolean isGroup, long timestamp,
            long timestampSent) {
        super(Direction.OUTGOING, file.getUri(), file.getName(), file.getEncoding(),
                file.getSize(), fileIcon, contact, chatId, fileTransferId, isGroup, timestamp,
                timestampSent);
        mTId = tId;
    }

    /**
     * Gets the HTTP File Transfer ID
     * 
     * @return the HTTP File Transfer ID
     */
    public String getTId() {
        return mTId;
    }

    @Override
    public String toString() {
        return "FtHttpResumeUpload [tId=" + mTId + ", file=" + getFile() + ",getFileName()="
                + getFileName() + ", getSize()=" + getSize() + ", getFileicon()=" + getFileicon()
                + ", getContact()=" + getContact() + ", getChatId()=" + getChatId()
                + ", getFileTransferId()=" + getFileTransferId() + ", isGroup()="
                + isGroupTransfer() + "]";
    }

}
