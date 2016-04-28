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

package com.gsma.rcs.provider.sharing;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.image.ImageSharing.ReasonCode;
import com.gsma.services.rcs.sharing.image.ImageSharing.State;

import android.database.Cursor;
import android.net.Uri;

/**
 * ImageSharingPersistedStorageAccessor helps in retrieving persisted data related to a image share
 * from the persisted storage. It can utilize caching for such data that will not be changed after
 * creation of the Image sharing to speed up consecutive access.
 */
public class ImageSharingPersistedStorageAccessor {

    private final String mSharingId;

    private final RichCallHistory mRichCallLog;

    private ContactId mContact;

    private Direction mDirection;

    private String mFileName;

    private Long mFileSize;

    private String mMimeType;

    private Uri mFile;

    private long mTimestamp;

    public ImageSharingPersistedStorageAccessor(String sharingId, RichCallHistory richCallLog) {
        mSharingId = sharingId;
        mRichCallLog = richCallLog;
    }

    public ImageSharingPersistedStorageAccessor(String sharingId, ContactId contact,
            Direction direction, Uri file, String fileName, String mimeType, long fileSize,
            RichCallHistory richCallLog, long timestamp) {
        mSharingId = sharingId;
        mContact = contact;
        mDirection = direction;
        mFile = file;
        mFileName = fileName;
        mFileSize = fileSize;
        mMimeType = mimeType;
        mRichCallLog = richCallLog;
        mTimestamp = timestamp;
    }

    private void cacheData() {
        Cursor cursor = null;
        try {
            cursor = mRichCallLog.getImageTransferData(mSharingId);
            if (!cursor.moveToNext()) {
                throw new ServerApiPersistentStorageException("Data not found for image sharing "
                        + mSharingId);
            }
            String contact = cursor.getString(cursor
                    .getColumnIndexOrThrow(ImageSharingData.KEY_CONTACT));
            if (contact != null) {
                mContact = ContactUtil.createContactIdFromTrustedData(contact);
            }
            mDirection = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(ImageSharingData.KEY_DIRECTION)));
            mFileName = cursor.getString(cursor
                    .getColumnIndexOrThrow(ImageSharingData.KEY_FILENAME));
            mMimeType = cursor.getString(cursor
                    .getColumnIndexOrThrow(ImageSharingData.KEY_MIME_TYPE));
            mFileSize = cursor.getLong(cursor.getColumnIndexOrThrow(ImageSharingData.KEY_FILESIZE));
            mFile = Uri.parse(cursor.getString(cursor
                    .getColumnIndexOrThrow(ImageSharingData.KEY_FILE)));
            mTimestamp = cursor.getLong(cursor
                    .getColumnIndexOrThrow(ImageSharingData.KEY_TIMESTAMP));
        } finally {
            CursorUtil.close(cursor);
        }
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

    public long getFileSize() {
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

    public State getState() {
        State state = mRichCallLog.getImageSharingState(mSharingId);
        if (state == null) {
            throw new ServerApiPersistentStorageException("State not found for image sharing "
                    + mSharingId);
        }
        return state;
    }

    public ReasonCode getReasonCode() {
        ReasonCode reasonCode = mRichCallLog.getImageSharingReasonCode(mSharingId);
        if (reasonCode == null) {
            throw new ServerApiPersistentStorageException(
                    "Reason code not found for image sharing " + mSharingId);
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

    public long getTimestamp() {
        /*
         * Utilizing cache here as timestamp can't be changed in persistent storage after it has
         * been set to some value bigger than zero, so no need to query for it multiple times.
         */
        if (mTimestamp == 0) {
            cacheData();
        }
        return mTimestamp;
    }

    public boolean setStateAndReasonCode(State state, ReasonCode reasonCode) {
        return mRichCallLog.setImageSharingStateAndReasonCode(mSharingId, state, reasonCode);
    }

    public boolean setProgress(long currentSize) {
        return mRichCallLog.setImageSharingProgress(mSharingId, currentSize);
    }

    public Uri addImageSharing(ContactId contact, Direction direction, MmContent content,
            State state, ReasonCode reasonCode, long timestamp) {
        mContact = contact;
        mDirection = direction;
        mTimestamp = timestamp;
        return mRichCallLog.addImageSharing(mSharingId, contact, direction, content, state,
                reasonCode, timestamp);
    }
}
