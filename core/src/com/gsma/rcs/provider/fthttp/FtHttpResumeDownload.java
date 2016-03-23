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
 * @author YPLO6403 Class to handle FtHttpResumeDownload data objects
 */
public final class FtHttpResumeDownload extends FtHttpResume {

    /**
     * The URI to download file from
     */
    private final Uri mServerAddress;

    private final long mFileExpiration;

    private final long mIconExpiration;

    private final boolean mAccepted;

    private final String mRemoteSipInstance;

    /**
     * Creates a FT HTTP resume download data object
     * 
     * @param downloadServerAddress the {@code downloadServerAddress} instance.
     * @param file the {@code file} value.
     * @param fileIcon the {@code fileIcon} value.
     * @param content the {@code content} content.
     * @param contact the {@code contactId} value.
     * @param chatId the {@code chatId} value.
     * @param filetransferId the {@code filetransferId} value.
     * @param isGroup the {@code isGroup} value.
     * @param timestamp the {@code timestamp} value.
     * @param timestampSent the {@code timestampSent} value.
     * @param fileExpiration the {@code fileExpiration} value.
     * @param iconExpiration the {@code iconExpiration} value.
     * @param accepted the {@code accepted} value.
     * @param remoteSipInstance the {@code remoteSipInstance} value.
     */
    public FtHttpResumeDownload(Uri downloadServerAddress, Uri file, Uri fileIcon,
            MmContent content, ContactId contact, String chatId, String filetransferId,
            boolean isGroup, long timestamp, long timestampSent, long fileExpiration,
            long iconExpiration, boolean accepted, String remoteSipInstance) {
        super(Direction.INCOMING, file, content.getName(), content.getEncoding(),
                content.getSize(), fileIcon, contact, chatId, filetransferId, isGroup, timestamp,
                timestampSent);
        mServerAddress = downloadServerAddress;
        mFileExpiration = fileExpiration;
        mIconExpiration = iconExpiration;
        mAccepted = accepted;
        mRemoteSipInstance = remoteSipInstance;
        if (downloadServerAddress == null || filetransferId == null)
            throw new IllegalArgumentException("Invalid argument");
    }

    /**
     * Returns the download server URI
     * 
     * @return the download server URI
     */
    public Uri getServerAddress() {
        return mServerAddress;
    }

    /**
     * Returns the time when the file on the content server is no longer valid to download.
     * 
     * @return time
     */
    public long getFileExpiration() {
        return mFileExpiration;
    }

    /**
     * Returns the time when the file icon on the content server is no longer valid to download.
     * 
     * @return time
     */
    public long getIconExpiration() {
        return mIconExpiration;
    }

    /**
     * Checks if download is accepted
     * 
     * @return True if accepted
     */
    public boolean isAccepted() {
        return mAccepted;
    }

    /**
     * Gets remote SIP instance
     * 
     * @return remote SIP instance
     */
    public String getRemoteSipInstance() {
        return mRemoteSipInstance;
    }

    @Override
    public String toString() {
        return "FtHttpResumeDownload [serverAddress=" + mServerAddress + ", file=" + getFile()
                + ",fileName=" + getFileName() + ", size=" + getSize() + ", fileicon="
                + getFileicon() + ", contact=" + getContact() + ", chatId=" + getChatId()
                + ", fileTransferId=" + getFileTransferId() + ", isGroup=" + isGroupTransfer()
                + "]";
    }

}
