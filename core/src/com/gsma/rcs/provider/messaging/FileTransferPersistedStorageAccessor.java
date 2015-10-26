/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.provider.messaging;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.fthttp.FtHttpResume;
import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;

import android.database.Cursor;
import android.net.Uri;

/**
 * FileTransferPersistedStorageAccessor helps in retrieving persisted data related to a file
 * transfer from the persisted storage. It can utilize caching for such data that will not be
 * changed after creation of the File transfer to speed up consecutive access.
 */
public class FileTransferPersistedStorageAccessor {

    private final String mFileTransferId;

    private final MessagingLog mMessagingLog;

    private ContactId mContact;

    private Boolean mRead;

    private Direction mDirection;

    private String mChatId;

    private String mFileName;

    private Long mFileSize;

    private String mMimeType;

    private Uri mFile;

    private Uri mFileIcon;

    private String mFileIconMimeType;

    private Long mTimestampDelivered;

    private Long mTimestampDisplayed;

    private Long mFileExpiration = FileTransferData.UNKNOWN_EXPIRATION;

    private Long mFileIconExpiration = FileTransferData.UNKNOWN_EXPIRATION;

    /**
     * Constructor
     * 
     * @param fileTransferId
     * @param messagingLog
     */
    public FileTransferPersistedStorageAccessor(String fileTransferId, MessagingLog messagingLog) {
        mFileTransferId = fileTransferId;
        mMessagingLog = messagingLog;
    }

    /**
     * Constructor
     * 
     * @param fileTransferId
     * @param contact
     * @param direction
     * @param chatId
     * @param file
     * @param fileIcon
     * @param messagingLog
     */
    public FileTransferPersistedStorageAccessor(String fileTransferId, ContactId contact,
            Direction direction, String chatId, MmContent file, MmContent fileIcon,
            MessagingLog messagingLog) {
        mFileTransferId = fileTransferId;
        mContact = contact;
        mDirection = direction;
        mChatId = chatId;
        mFile = file.getUri();
        mFileIcon = fileIcon != null ? fileIcon.getUri() : null;
        mFileIconMimeType = fileIcon != null ? fileIcon.getEncoding() : null;
        mFileName = file.getName();
        mMimeType = file.getEncoding();
        mFileSize = file.getSize();
        mMessagingLog = messagingLog;
    }

    private void cacheData() {
        Cursor cursor = null;
        try {
            cursor = mMessagingLog.getFileTransferData(mFileTransferId);
            if (!cursor.moveToNext()) {
                throw new ServerApiPersistentStorageException(new StringBuilder(
                        "Data not found for file transfer ").append(mFileTransferId).toString());
            }

            String contact = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_CONTACT));
            if (contact != null) {
                mContact = ContactUtil.createContactIdFromTrustedData(contact);
            }
            mDirection = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_DIRECTION)));
            mChatId = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferData.KEY_CHAT_ID));
            mFileName = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_FILENAME));
            mMimeType = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_MIME_TYPE));
            mFile = Uri.parse(cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_FILE)));
            String fileIcon = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_FILEICON));
            if (fileIcon != null) {
                mFileIcon = Uri.parse(fileIcon);
            }
            if (!Boolean.TRUE.equals(mRead)) {
                mRead = ReadStatus.READ.toInt() == cursor.getInt(cursor
                        .getColumnIndexOrThrow(FileTransferData.KEY_READ_STATUS));
            }
            mFileSize = cursor.getLong(cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILESIZE));
            mFileIconMimeType = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_FILEICON_MIME_TYPE));
            if (mTimestampDelivered == null || mTimestampDelivered == 0) {
                mTimestampDelivered = cursor.getLong(cursor
                        .getColumnIndexOrThrow(FileTransferData.KEY_TIMESTAMP_DELIVERED));
            }
            if (mTimestampDisplayed == null || mTimestampDisplayed == 0) {
                mTimestampDisplayed = cursor.getLong(cursor
                        .getColumnIndexOrThrow(FileTransferData.KEY_TIMESTAMP_DISPLAYED));
            }
            if (mFileExpiration == FileTransferData.UNKNOWN_EXPIRATION) {
                mFileExpiration = cursor.getLong(cursor
                        .getColumnIndexOrThrow(FileTransferData.KEY_FILE_EXPIRATION));
            }
            if (mFileIconExpiration == FileTransferData.UNKNOWN_EXPIRATION) {
                mFileIconExpiration = cursor.getLong(cursor
                        .getColumnIndexOrThrow(FileTransferData.KEY_FILEICON_EXPIRATION));
            }
        } finally {
            CursorUtil.close(cursor);
        }
    }

    public String getChatId() {
        /*
         * Utilizing cache here as chatId can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mChatId == null) {
            cacheData();
        }
        return mChatId;
    }

    public ContactId getRemoteContact() {
        /*
         * Utilizing cache here as contact can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mContact == null) {
            cacheData();
        }
        return mContact;
    }

    public Uri getFile() {
        /*
         * Utilizing cache here as file can't be changed in persistent storage after entry insertion
         * anyway so no need to query for it multiple times.
         */
        if (mFile == null) {
            cacheData();
        }
        return mFile;
    }

    public String getFileName() {
        /*
         * Utilizing cache here as file name can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mFileName == null) {
            cacheData();
        }
        return mFileName;
    }

    public Long getFileSize() {
        /*
         * Utilizing cache here as file size can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mFileSize == null) {
            cacheData();
        }
        return mFileSize;
    }

    public String getMimeType() {
        /*
         * Utilizing cache here as mime type can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mMimeType == null) {
            cacheData();
        }
        return mMimeType;
    }

    public Uri getFileIcon() {
        /*
         * Utilizing cache here as file icon can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mFileIcon == null) {
            cacheData();
        }
        return mFileIcon;
    }

    public String getFileIconMimeType() {
        /*
         * Utilizing cache here as file icon mime type can't be changed in persistent storage after
         * entry insertion anyway so no need to query for it multiple times.
         */
        if (mFileIconMimeType == null) {
            cacheData();
        }
        return mFileIconMimeType;
    }

    public long getTimestamp() {
        Long timestamp = mMessagingLog.getFileTransferTimestamp(mFileTransferId);
        if (timestamp == null) {
            throw new ServerApiPersistentStorageException(new StringBuilder(
                    "Timestamp not found for file transfer ").append(mFileTransferId).toString());
        }
        return timestamp;
    }

    public long getTimestampSent() {
        Long timestamp = mMessagingLog.getFileTransferSentTimestamp(mFileTransferId);
        if (timestamp == null) {
            throw new ServerApiPersistentStorageException(new StringBuilder(
                    "TimestampSent not found for file transfer ").append(mFileTransferId)
                    .toString());
        }
        return timestamp;
    }

    public long getTimestampDelivered() {
        /*
         * Utilizing cache here as Timestamp delivered can't be changed in persistent storage after
         * it has been set to some value bigger than zero, so no need to query for it multiple
         * times.
         */
        if (mTimestampDelivered == null || mTimestampDelivered == 0) {
            cacheData();
        }
        return mTimestampDelivered;
    }

    public Long getTimestampDisplayed() {
        /*
         * Utilizing cache here as Timestamp displayed can't be changed in persistent storage after
         * it has been set to some value bigger than zero, so no need to query for it multiple
         * times.
         */
        if (mTimestampDisplayed == null || mTimestampDisplayed == 0) {
            cacheData();
        }
        return mTimestampDisplayed;
    }

    public State getState() {
        State state = mMessagingLog.getFileTransferState(mFileTransferId);
        if (state == null) {
            throw new ServerApiPersistentStorageException(new StringBuilder(
                    "State not found for file transfer ").append(mFileTransferId).toString());
        }
        return state;
    }

    public ReasonCode getReasonCode() {
        ReasonCode reasonCode = mMessagingLog.getFileTransferReasonCode(mFileTransferId);
        if (reasonCode == null) {
            throw new ServerApiPersistentStorageException(new StringBuilder(
                    "Reason code not found for file transfer ").append(mFileTransferId).toString());
        }
        return reasonCode;
    }

    public Direction getDirection() {
        /*
         * Utilizing cache here as direction can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mDirection == null) {
            cacheData();
        }
        return mDirection;
    }

    public Boolean isRead() {
        /*
         * No need to read from provider unless incoming and not already marked as read.
         */
        if (Direction.INCOMING == mDirection && !Boolean.TRUE.equals(mRead)) {
            cacheData();
        }
        return mRead;
    }

    public boolean setStateAndReasonCode(State state, ReasonCode reasonCode) {
        return mMessagingLog.setFileTransferStateAndReasonCode(mFileTransferId, state, reasonCode);
    }

    public boolean setProgress(long currentSize) {
        return mMessagingLog.setFileTransferProgress(mFileTransferId, currentSize);
    }

    public boolean setTransferred(MmContent content, long fileExpiration, long fileIconExpiration,
            long deliveryExpiration) {
        return mMessagingLog.setFileTransferred(mFileTransferId, content, fileExpiration,
                fileIconExpiration, deliveryExpiration);
    }

    public void addOneToOneFileTransfer(ContactId contact, Direction direction, MmContent content,
            MmContent fileIcon, State status, ReasonCode reasonCode, long timestamp,
            long timestampSent, long fileExpiration, long fileIconExpiration) {
        mContact = contact;
        mDirection = direction;
        mMessagingLog.addOneToOneFileTransfer(mFileTransferId, contact, direction, content,
                fileIcon, status, reasonCode, timestamp, timestampSent, fileExpiration,
                fileIconExpiration);
    }

    public void addIncomingGroupFileTransfer(String chatId, ContactId contact, MmContent content,
            MmContent fileicon, State state, ReasonCode reasonCode, long timestamp,
            long timestampSent, long fileExpiration, long fileIconExpiration) {
        mChatId = chatId;
        mContact = contact;
        mMessagingLog.addIncomingGroupFileTransfer(mFileTransferId, chatId, contact, content,
                fileicon, state, reasonCode, timestamp, timestampSent, fileExpiration,
                fileIconExpiration);
    }

    public FtHttpResume getFileTransferResumeInfo() {
        return mMessagingLog.getFileTransferResumeInfo(mFileTransferId);
    }

    /**
     * Returns the time for when file on the content server is no longer valid to download.
     * 
     * @return time
     */
    public long getFileExpiration() {
        /* No need to read from provider unless outgoing and expiration is unknown. */
        if (Direction.OUTGOING == mDirection
                && FileTransferData.UNKNOWN_EXPIRATION == mFileExpiration) {
            cacheData();
        }
        return mFileExpiration;
    }

    /**
     * Returns the time for when file icon on the content server is no longer valid to download.
     * 
     * @return time
     */
    public long getFileIconExpiration() {
        /* No need to read from provider unless outgoing and expiration is unknown. */
        if (Direction.OUTGOING == mDirection
                && FileTransferData.UNKNOWN_EXPIRATION == mFileIconExpiration) {
            cacheData();
        }
        return mFileIconExpiration;
    }

    /**
     * Returns true if delivery for this file has expired or false otherwise. Note: false means
     * either that delivery for this file has not yet expired, delivery has been successful,
     * delivery expiration has been cleared (see clearFileTransferDeliveryExpiration) or that this
     * particular file is not eligible for delivery expiration in the first place.
     * 
     * @return deliveryExpiration
     */
    public boolean isExpiredDelivery() {
        Boolean expiredDelivery = mMessagingLog.isFileTransferExpiredDelivery(mFileTransferId);
        if (expiredDelivery == null) {
            throw new ServerApiPersistentStorageException(new StringBuilder(
                    "Expired Delivery not found for file transfer ").append(mFileTransferId)
                    .toString());
        }
        return expiredDelivery;
    }

    public boolean setStateAndTimestamp(State state, ReasonCode reasonCode, long timestamp,
            long timestampSent) {
        return mMessagingLog.setFileTransferStateAndTimestamp(mFileTransferId, state, reasonCode,
                timestamp, timestampSent);
    }

    public boolean setFileInfoDequeued(long deliveryExpiration) {
        return mMessagingLog.setFileInfoDequeued(mFileTransferId, deliveryExpiration);
    }

    /**
     * Returns the number of transferred bytes.
     * 
     * @return the number of transferred bytes.
     */
    public long getFileTransferProgress() {
        Long transferred = mMessagingLog.getFileTransferProgress(mFileTransferId);
        if (transferred == null) {
            throw new ServerApiPersistentStorageException(new StringBuilder(
                    "Transferred not found for file transfer ").append(mFileTransferId).toString());
        }
        return transferred;
    }
}
