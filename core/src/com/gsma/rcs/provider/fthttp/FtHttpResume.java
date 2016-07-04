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

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

/**
 * @author YPLO6403 FtHttpResume is the abstract base class for all FT HTTP resume classes
 */
public abstract class FtHttpResume {

    /**
     * The direction of transfer
     */
    final private Direction mDirection;

    /**
     * Uri of file
     */
    final private Uri mFile;

    /**
     * The file name
     */
    final private String mFileName;

    /**
     * The file mime type
     */
    final private String mMimeType;

    /**
     * The size of the file to download
     */
    final private long mSize;

    /**
     * The fileIcon URI
     */
    final private Uri mFileIcon;

    /**
     * The remote contact identifier
     */
    final private ContactId mContact;

    /**
     * The Chat Id
     */
    final private String mChatId;

    /**
     * The file transfer Id
     */
    final private String mFileTransferId;

    /**
     * Is FT initiated from Group Chat
     */
    final private boolean mGroupTransfer;

    /**
     * The local timestamp
     */
    final private long mTimestamp;

    /**
     * The timestamp sent in payload
     */
    final private long mTimestampSent;

    /**
     * Creates an instance of FtHttpResume Data Object
     * 
     * @param direction the {@code direction} value.
     * @param file the {@code Uri of file} value.
     * @param fileName the {@code fileName} value.
     * @param mimeType the {@code mimeType} value.
     * @param size the {@code size} value.
     * @param fileIcon the {@code fileIcon} value.
     * @param contact the {@code contactId} value.
     * @param chatId the {@code chatId} value.
     * @param fileTransferId the {@code fileTransferId} value.
     * @param groupTransfer the {@code groupTransfer} value.
     * @param timestamp the {@code timestamp} value
     * @param timestampSent the {@code timestampSent} value
     */
    public FtHttpResume(Direction direction, Uri file, String fileName, String mimeType, long size,
            Uri fileIcon, ContactId contact, String chatId, String fileTransferId,
            boolean groupTransfer, long timestamp, long timestampSent) {
        if (size <= 0 || file == null || fileName == null)
            throw new IllegalArgumentException("size invalid arguments (size=" + size + ") (file="
                    + file + ") (fileName=" + fileName + ")");
        mDirection = direction;
        mFile = file;
        mFileName = fileName;
        mSize = size;
        mFileIcon = fileIcon;
        mContact = contact;
        mChatId = chatId;
        mFileTransferId = fileTransferId;
        mGroupTransfer = groupTransfer;
        mTimestamp = timestamp;
        mTimestampSent = timestampSent;
        mMimeType = mimeType;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public long getTimestampSent() {
        return mTimestampSent;
    }

    /**
     * Gets direction
     * 
     * @return direction
     */
    public Direction getDirection() {
        return mDirection;
    }

    /**
     * Gets file URI
     * 
     * @return file URI
     */
    public Uri getFile() {
        return mFile;
    }

    /**
     * Get file name
     * 
     * @return file name
     */
    public String getFileName() {
        return mFileName;
    }

    /**
     * Gets file size
     * 
     * @return file size
     */
    public long getSize() {
        return mSize;
    }

    /**
     * Gets file icon URI
     * 
     * @return file icon URI
     */
    public Uri getFileicon() {
        return mFileIcon;
    }

    /**
     * Gets remote contact
     * 
     * @return remote contact
     */
    public ContactId getContact() {
        return mContact;
    }

    /**
     * Gets chat ID
     * 
     * @return chat ID
     */
    public String getChatId() {
        return mChatId;
    }

    /**
     * Gets file transfer ID
     * 
     * @return file transfer ID
     */
    public String getFileTransferId() {
        return mFileTransferId;
    }

    /**
     * Checks if group transfer
     * 
     * @return True if group transfer
     */
    public boolean isGroupTransfer() {
        return mGroupTransfer;
    }

    /**
     * Gets the file mime type
     * 
     * @return the file mime type
     */
    public String getMimeType() {
        return mMimeType;
    }

    @Override
    public String toString() {
        return "FtHttpResume [timestamp=" + mTimestamp + ", timestampSent=" + mTimestampSent
                + ", dir=" + mDirection + ", file=" + mFile + ", fileName=" + mFileName
                + ",fileIcon=" + mFileIcon + "]";
    }

}
