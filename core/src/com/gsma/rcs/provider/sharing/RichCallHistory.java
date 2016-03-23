/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.provider.sharing;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.content.VideoContent;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing;
import com.gsma.services.rcs.sharing.image.ImageSharing;
import com.gsma.services.rcs.sharing.video.VideoSharing;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * Rich call history
 *
 * @author Jean-Marc AUFFRET
 */
public class RichCallHistory {
    /**
     * Current instance
     */
    private static volatile RichCallHistory sInstance;

    private final LocalContentResolver mLocalContentResolver;

    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(RichCallHistory.class.getSimpleName());

    private static final int FIRST_COLUMN_IDX = 0;

    private static final String SELECTION_BY_INTERRUPTED_GEOLOC_SHARINGS = GeolocSharingData.KEY_STATE
            + " IN ('"
            + GeolocSharing.State.STARTED.toInt()
            + "','"
            + GeolocSharing.State.INVITED.toInt()
            + "','"
            + GeolocSharing.State.ACCEPTING.toInt()
            + "','" + GeolocSharing.State.INITIATING.toInt() + "')";

    private static final String SELECTION_BY_INTERRUPTED_IMAGE_SHARINGS = ImageSharingData.KEY_STATE
            + " IN ('"
            + ImageSharing.State.STARTED.toInt()
            + "','"
            + ImageSharing.State.INVITED.toInt()
            + "','"
            + ImageSharing.State.ACCEPTING.toInt()
            + "','" + ImageSharing.State.INITIATING.toInt() + "')";

    private static final String SELECTION_BY_INTERRUPTED_VIDEO_SHARINGS = VideoSharingData.KEY_STATE
            + " IN ('"
            + VideoSharing.State.STARTED.toInt()
            + "','"
            + VideoSharing.State.INVITED.toInt()
            + "','"
            + VideoSharing.State.ACCEPTING.toInt()
            + "','" + VideoSharing.State.INITIATING.toInt() + "')";

    private static final String ORDER_BY_TIMESTAMP_ASC = GeolocSharingData.KEY_TIMESTAMP
            .concat(" ASC");

    /**
     * Get image transfer info from its unique Id
     *
     * @param columnName the column name
     * @param sharingId the sharing ID
     * @return Cursor the caller of this method has to close the cursor if a cursor is returned
     */
    private Cursor getImageTransferData(String columnName, String sharingId) {
        String[] projection = new String[] {
            columnName
        };
        Uri contentUri = Uri.withAppendedPath(ImageSharingData.CONTENT_URI, sharingId);
        Cursor cursor = mLocalContentResolver.query(contentUri, projection, null, null, null);
        CursorUtil.assertCursorIsNotNull(cursor, contentUri);
        if (!cursor.moveToNext()) {
            CursorUtil.close(cursor);
            return null;
        }
        return cursor;
    }

    /**
     * Get video sharing info from its unique Id
     *
     * @param columnName the column name
     * @param sharingId the sharing ID
     * @return Cursor the caller of this method has to close the cursor if a cursor is returned
     */
    private Cursor getVideoSharingData(String columnName, String sharingId) {
        String[] projection = new String[] {
            columnName
        };
        Uri contentUri = Uri.withAppendedPath(VideoSharingData.CONTENT_URI, sharingId);
        Cursor cursor = mLocalContentResolver.query(contentUri, projection, null, null, null);
        CursorUtil.assertCursorIsNotNull(cursor, contentUri);
        if (!cursor.moveToNext()) {
            CursorUtil.close(cursor);
            return null;
        }
        return cursor;
    }

    /**
     * Get geoloc sharing data
     *
     * @param columnName Column name
     * @param sharingId Sharing ID
     * @return Cursor
     */
    private Cursor getGeolocSharingData(String columnName, String sharingId) {
        String[] projection = new String[] {
            columnName
        };
        Uri contentUri = Uri.withAppendedPath(GeolocSharingData.CONTENT_URI, sharingId);
        Cursor cursor = mLocalContentResolver.query(contentUri, projection, null, null, null);
        CursorUtil.assertCursorIsNotNull(cursor, contentUri);
        if (!cursor.moveToNext()) {
            CursorUtil.close(cursor);
            return null;
        }
        return cursor;
    }

    private Integer getDataAsInteger(Cursor cursor) {
        try {
            if (cursor.isNull(FIRST_COLUMN_IDX)) {
                return null;
            }
            return cursor.getInt(FIRST_COLUMN_IDX);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    private Long getDataAsLong(Cursor cursor) {
        try {
            if (cursor.isNull(FIRST_COLUMN_IDX)) {
                return null;
            }
            return cursor.getLong(FIRST_COLUMN_IDX);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    private String getDataAsString(Cursor cursor) {
        try {
            return cursor.getString(FIRST_COLUMN_IDX);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get or Create Singleton instance of RichCallHistory
     *
     * @param localContentResolver Local content resolver
     */
    public static RichCallHistory getInstance(LocalContentResolver localContentResolver) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (RichCallHistory.class) {
            if (sInstance == null) {
                sInstance = new RichCallHistory(localContentResolver);
            }
            return sInstance;
        }
    }

    /**
     * Returns instance
     *
     * @return Instance
     */
    public static RichCallHistory getInstance() {
        synchronized (RichCallHistory.class) {
            return sInstance;
        }
    }

    /**
     * Constructor
     *
     * @param localContentResolver Local content resolver
     */
    private RichCallHistory(LocalContentResolver localContentResolver) {
        super();
        mLocalContentResolver = localContentResolver;
    }

    /**
     * Read the total size of transferred image
     *
     * @param sharingId the sharing ID
     * @return the total size
     */
    private Long getImageSharingTotalSize(String sharingId) {
        Cursor cursor = getImageTransferData(ImageSharingData.KEY_FILESIZE, sharingId);
        if (cursor == null) {
            return null;
        }
        return getDataAsLong(cursor);
    }

    /*--------------------- Video sharing update/add methods ----------------------*/

    /**
     * Add a new video sharing in the call history
     *
     * @param sharingId Session ID
     * @param contact Remote contact ID
     * @param direction Call event direction
     * @param content Shared content
     * @param state Call state
     * @param reasonCode Reason Code
     * @param timestamp Local timestamp for both incoming and outgoing video sharing
     * @return image URI
     */
    public Uri addVideoSharing(String sharingId, ContactId contact, Direction direction,
            VideoContent content, VideoSharing.State state, VideoSharing.ReasonCode reasonCode,
            long timestamp) {
        if (logger.isActivated()) {
            logger.debug("Add new video sharing for contact " + contact + ": sharingId="
                    + sharingId + ", state=" + state + ", reasonCode=" + reasonCode);
        }

        ContentValues values = new ContentValues();
        values.put(VideoSharingData.KEY_SHARING_ID, sharingId);
        values.put(VideoSharingData.KEY_CONTACT, contact.toString());
        values.put(VideoSharingData.KEY_DIRECTION, direction.toInt());
        values.put(VideoSharingData.KEY_STATE, state.toInt());
        values.put(VideoSharingData.KEY_REASON_CODE, reasonCode.toInt());
        values.put(VideoSharingData.KEY_TIMESTAMP, timestamp);
        values.put(VideoSharingData.KEY_DURATION, 0);
        values.put(VideoSharingData.KEY_VIDEO_ENCODING, content.getEncoding());
        values.put(VideoSharingData.KEY_WIDTH, content.getWidth());
        values.put(VideoSharingData.KEY_HEIGHT, content.getHeight());
        return mLocalContentResolver.insert(VideoSharingData.CONTENT_URI, values);
    }

    /**
     * Set the video sharing state, reason code and duration
     *
     * @param sharingId sharing ID of the entry
     * @param state New state
     * @param reasonCode Reason Code
     * @param duration Duration
     * @return true if updated
     */
    public boolean setVideoSharingStateReasonCodeAndDuration(String sharingId,
            VideoSharing.State state, VideoSharing.ReasonCode reasonCode, long duration) {
        if (logger.isActivated()) {
            logger.debug("Update video sharing state of sharing " + sharingId + " state=" + state
                    + ", reasonCode=" + reasonCode + " duration=" + duration);
        }
        ContentValues values = new ContentValues();
        values.put(VideoSharingData.KEY_STATE, state.toInt());
        values.put(VideoSharingData.KEY_REASON_CODE, reasonCode.toInt());
        if (duration > 0) {
            values.put(VideoSharingData.KEY_DURATION, duration);
        }
        return mLocalContentResolver.update(
                Uri.withAppendedPath(VideoSharingData.CONTENT_URI, sharingId), values, null, null) > 0;
    }

    /**
     * Set the video sharing state, reason code
     *
     * @param sharingId sharing ID of the entry
     * @param state New state
     * @param reasonCode Reason Code
     * @return true if updated
     */
    public boolean setVideoSharingStateReasonCode(String sharingId, VideoSharing.State state,
            VideoSharing.ReasonCode reasonCode) {
        if (logger.isActivated()) {
            logger.debug("Update video sharing state of sharing " + sharingId + " state=" + state
                    + ", reasonCode=" + reasonCode);
        }
        ContentValues values = new ContentValues();
        values.put(VideoSharingData.KEY_STATE, state.toInt());
        values.put(VideoSharingData.KEY_REASON_CODE, reasonCode.toInt());
        return mLocalContentResolver.update(
                Uri.withAppendedPath(VideoSharingData.CONTENT_URI, sharingId), values, null, null) > 0;
    }

    /*--------------------- Image sharing update / add methods ----------------------*/

    /**
     * Add a new image sharing in the call history
     *
     * @param sharingId Session ID
     * @param contact Remote contact ID
     * @param direction Call event direction
     * @param content Shared content
     * @param state Call state
     * @param reasonCode Reason Code
     * @param timestamp Local timestamp for both incoming and outgoing image sharing
     * @return uri
     */
    public Uri addImageSharing(String sharingId, ContactId contact, Direction direction,
            MmContent content, ImageSharing.State state, ImageSharing.ReasonCode reasonCode,
            long timestamp) {
        if (logger.isActivated()) {
            logger.debug("Add new image sharing for contact " + contact + ": sharing =" + sharingId
                    + ", state=" + state);
        }

        ContentValues values = new ContentValues();
        values.put(ImageSharingData.KEY_SHARING_ID, sharingId);
        values.put(ImageSharingData.KEY_CONTACT, contact.toString());
        values.put(ImageSharingData.KEY_DIRECTION, direction.toInt());
        values.put(ImageSharingData.KEY_FILE, content.getUri().toString());
        values.put(ImageSharingData.KEY_FILENAME, content.getName());
        values.put(ImageSharingData.KEY_MIME_TYPE, content.getEncoding());
        values.put(ImageSharingData.KEY_TRANSFERRED, 0);
        values.put(ImageSharingData.KEY_FILESIZE, content.getSize());
        values.put(ImageSharingData.KEY_STATE, state.toInt());
        values.put(ImageSharingData.KEY_REASON_CODE, reasonCode.toInt());
        values.put(ImageSharingData.KEY_TIMESTAMP, timestamp);
        return mLocalContentResolver.insert(ImageSharingData.CONTENT_URI, values);
    }

    /**
     * Set the image sharing state and reason code
     *
     * @param sharingId the sharing ID
     * @param state New state
     * @param reasonCode Reason Code
     * @return true if updated
     */
    public boolean setImageSharingStateAndReasonCode(String sharingId, ImageSharing.State state,
            ImageSharing.ReasonCode reasonCode) {
        if (logger.isActivated()) {
            logger.debug("Update status of image sharing " + sharingId + " to " + state);
        }
        ContentValues values = new ContentValues();
        values.put(ImageSharingData.KEY_STATE, state.toInt());
        values.put(ImageSharingData.KEY_REASON_CODE, reasonCode.toInt());
        if (state == ImageSharing.State.TRANSFERRED) {
            // Update the size of bytes if fully transferred
            Long total = getImageSharingTotalSize(sharingId);
            if (total != null && total != 0) {
                values.put(ImageSharingData.KEY_TRANSFERRED, total);
            }
        }
        return mLocalContentResolver.update(
                Uri.withAppendedPath(ImageSharingData.CONTENT_URI, sharingId), values, null, null) > 0;
    }

    /**
     * Update the image sharing progress
     *
     * @param sharingId Session ID of the entry
     * @param currentSize Current size
     * @return true if updated
     */
    public boolean setImageSharingProgress(String sharingId, long currentSize) {
        ContentValues values = new ContentValues();
        values.put(ImageSharingData.KEY_TRANSFERRED, currentSize);
        return mLocalContentResolver.update(
                Uri.withAppendedPath(ImageSharingData.CONTENT_URI, sharingId), values, null, null) > 0;
    }

    /*--------------------- Geoloc sharing update / add methods ----------------------*/

    /**
     * Add an incoming geoloc sharing
     *
     * @param contact Remote contact ID
     * @param sharingId Sharing ID
     * @param state Geoloc sharing state
     * @param reasonCode Reason code of the state
     * @param timestamp Local timestamp for incoming geoloc sharing
     * @return Uri
     */
    public Uri addIncomingGeolocSharing(ContactId contact, String sharingId,
            GeolocSharing.State state, GeolocSharing.ReasonCode reasonCode, long timestamp) {
        ContentValues values = new ContentValues();
        values.put(GeolocSharingData.KEY_SHARING_ID, sharingId);
        values.put(GeolocSharingData.KEY_CONTACT, contact.toString());
        values.put(GeolocSharingData.KEY_MIME_TYPE, MimeType.GEOLOC_MESSAGE);
        values.put(GeolocSharingData.KEY_DIRECTION, Direction.INCOMING.toInt());
        values.put(GeolocSharingData.KEY_STATE, state.toInt());
        values.put(GeolocSharingData.KEY_REASON_CODE, reasonCode.toInt());
        values.put(GeolocSharingData.KEY_TIMESTAMP, timestamp);
        return mLocalContentResolver.insert(GeolocSharingData.CONTENT_URI, values);
    }

    /**
     * Add an outgoing geoloc sharing
     *
     * @param contact Remote contact ID
     * @param sharingId Sharing ID
     * @param geoloc Geolocation
     * @param state Geoloc sharing state
     * @param reasonCode Reason code of the state
     * @param timestamp Local timestamp for outgoing geoloc sharing
     * @return Uri
     */
    public Uri addOutgoingGeolocSharing(ContactId contact, String sharingId, Geoloc geoloc,
            GeolocSharing.State state, GeolocSharing.ReasonCode reasonCode, long timestamp) {
        ContentValues values = new ContentValues();
        values.put(GeolocSharingData.KEY_SHARING_ID, sharingId);
        values.put(GeolocSharingData.KEY_CONTACT, contact.toString());
        values.put(GeolocSharingData.KEY_MIME_TYPE, MimeType.GEOLOC_MESSAGE);
        values.put(GeolocSharingData.KEY_CONTENT, geoloc.toString());
        values.put(GeolocSharingData.KEY_DIRECTION, Direction.OUTGOING.toInt());
        values.put(GeolocSharingData.KEY_STATE, state.toInt());
        values.put(GeolocSharingData.KEY_REASON_CODE, reasonCode.toInt());
        values.put(GeolocSharingData.KEY_TIMESTAMP, timestamp);
        return mLocalContentResolver.insert(GeolocSharingData.CONTENT_URI, values);
    }

    /**
     * Sets the data of a geoloc sharing and updates state to transferred
     *
     * @param sharingId Sharing ID
     * @param geoloc Geolococation
     * @return true if updated
     */
    public boolean setGeolocSharingTransferred(String sharingId, Geoloc geoloc) {
        ContentValues values = new ContentValues();
        values.put(GeolocSharingData.KEY_CONTENT, geoloc.toString());
        values.put(GeolocSharingData.KEY_STATE, GeolocSharing.State.TRANSFERRED.toInt());
        values.put(GeolocSharingData.KEY_REASON_CODE, GeolocSharing.ReasonCode.UNSPECIFIED.toInt());
        return mLocalContentResolver.update(
                Uri.withAppendedPath(GeolocSharingData.CONTENT_URI, sharingId), values, null, null) > 0;
    }

    /**
     * Update the geoloc sharing state and reason code
     *
     * @param sharingId Sharing ID
     * @param state Geoloc sharing state
     * @param reasonCode Reason code of the state
     * @return true if updated
     */
    public boolean setGeolocSharingStateAndReasonCode(String sharingId, GeolocSharing.State state,
            GeolocSharing.ReasonCode reasonCode) {
        ContentValues values = new ContentValues();
        values.put(GeolocSharingData.KEY_STATE, state.toInt());
        values.put(GeolocSharingData.KEY_REASON_CODE, reasonCode.toInt());
        return mLocalContentResolver.update(
                Uri.withAppendedPath(GeolocSharingData.CONTENT_URI, sharingId), values, null, null) > 0;
    }

    /**
     * Get the geoloc sharing state
     *
     * @param sharingId Sharing ID
     * @return state
     */
    public GeolocSharing.State getGeolocSharingState(String sharingId) {
        Cursor cursor = getGeolocSharingData(GeolocSharingData.KEY_STATE, sharingId);
        if (cursor == null) {
            return null;
        }
        return GeolocSharing.State.valueOf(getDataAsInteger(cursor));
    }

    /**
     * Get the geoloc sharing reason code
     *
     * @param sharingId Sharing ID
     * @return reason code
     */
    public GeolocSharing.ReasonCode getGeolocSharingReasonCode(String sharingId) {
        Cursor cursor = getGeolocSharingData(GeolocSharingData.KEY_REASON_CODE, sharingId);
        if (cursor == null) {
            return null;
        }
        return GeolocSharing.ReasonCode.valueOf(getDataAsInteger(cursor));
    }

    public Cursor getInterruptedGeolocSharings() {
        Cursor cursor = mLocalContentResolver.query(GeolocSharingData.CONTENT_URI, null,
                SELECTION_BY_INTERRUPTED_GEOLOC_SHARINGS, null, ORDER_BY_TIMESTAMP_ASC);
        CursorUtil.assertCursorIsNotNull(cursor, GeolocSharingData.CONTENT_URI);
        return cursor;
    }

    public Cursor getInterruptedImageSharings() {
        Cursor cursor = mLocalContentResolver.query(ImageSharingData.CONTENT_URI, null,
                SELECTION_BY_INTERRUPTED_IMAGE_SHARINGS, null, ORDER_BY_TIMESTAMP_ASC);
        CursorUtil.assertCursorIsNotNull(cursor, GeolocSharingData.CONTENT_URI);
        return cursor;
    }

    public Cursor getInterruptedVideoSharings() {
        Cursor cursor = mLocalContentResolver.query(VideoSharingData.CONTENT_URI, null,
                SELECTION_BY_INTERRUPTED_VIDEO_SHARINGS, null, ORDER_BY_TIMESTAMP_ASC);
        CursorUtil.assertCursorIsNotNull(cursor, GeolocSharingData.CONTENT_URI);
        return cursor;
    }

    /**
     * Delete all entries in Rich Call history
     */
    public void deleteAllEntries() {
        mLocalContentResolver.delete(ImageSharingData.CONTENT_URI, null, null);
        mLocalContentResolver.delete(VideoSharingData.CONTENT_URI, null, null);
        mLocalContentResolver.delete(GeolocSharingData.CONTENT_URI, null, null);
    }

    /**
     * Get Image sharing state from unique Id
     *
     * @param sharingId the sharing ID
     * @return State
     */
    public ImageSharing.State getImageSharingState(String sharingId) {
        Cursor cursor = getImageTransferData(ImageSharingData.KEY_STATE, sharingId);
        if (cursor == null) {
            return null;
        }
        return ImageSharing.State.valueOf(getDataAsInteger(cursor));
    }

    /**
     * Get Image sharing reason code from unique Id
     *
     * @param sharingId the sharing ID
     * @return Reason code
     */
    public ImageSharing.ReasonCode getImageSharingReasonCode(String sharingId) {
        Cursor cursor = getImageTransferData(ImageSharingData.KEY_REASON_CODE, sharingId);
        if (cursor == null) {
            return null;
        }
        return ImageSharing.ReasonCode.valueOf(getDataAsInteger(cursor));
    }

    /**
     * Get Video sharing state from unique Id
     *
     * @param sharingId the sharing ID
     * @return State
     */
    public VideoSharing.State getVideoSharingState(String sharingId) {
        Cursor cursor = getVideoSharingData(VideoSharingData.KEY_STATE, sharingId);
        if (cursor == null) {
            return null;
        }
        return VideoSharing.State.valueOf(getDataAsInteger(cursor));
    }

    /**
     * Get Video sharing reason code from unique Id
     *
     * @param sharingId the sharing ID
     * @return Reason code
     */
    public VideoSharing.ReasonCode getVideoSharingReasonCode(String sharingId) {
        Cursor cursor = getVideoSharingData(VideoSharingData.KEY_REASON_CODE, sharingId);
        if (cursor == null) {
            return null;
        }
        return VideoSharing.ReasonCode.valueOf(getDataAsInteger(cursor));
    }

    /**
     * Returns video sharing duration
     *
     * @param sharingId the sharing ID
     * @return duration
     */
    public Long getVideoSharingDuration(String sharingId) {
        Cursor cursor = getVideoSharingData(VideoSharingData.KEY_DURATION, sharingId);
        if (cursor == null) {
            return null;
        }
        return getDataAsLong(cursor);
    }

    /**
     * Get geolocation sharing info from its unique Id
     *
     * @param sharingId the sharing ID
     * @return Cursor the caller of this method has to close the cursor if a cursor is returned
     */
    public Cursor getGeolocSharingData(String sharingId) {
        Uri contentUri = Uri.withAppendedPath(GeolocSharingData.CONTENT_URI, sharingId);
        Cursor cursor = mLocalContentResolver.query(contentUri, null, null, null, null);
        CursorUtil.assertCursorIsNotNull(cursor, contentUri);
        return cursor;
    }

    /**
     * Get image transfer info from its unique Id
     *
     * @param sharingId the sharing ID
     * @return Cursor the caller of this method has to close the cursor if a cursor is returned
     */
    public Cursor getImageTransferData(String sharingId) {
        Uri contentUri = Uri.withAppendedPath(ImageSharingData.CONTENT_URI, sharingId);
        Cursor cursor = mLocalContentResolver.query(contentUri, null, null, null, null);
        CursorUtil.assertCursorIsNotNull(cursor, contentUri);
        return cursor;
    }

    /**
     * Get video sharing info from its unique Id
     *
     * @param sharingId the sharing ID
     * @return Cursor the caller of this method has to close the cursor if a cursor is returned
     */
    public Cursor getVideoSharingData(String sharingId) {
        Uri contentUri = Uri.withAppendedPath(VideoSharingData.CONTENT_URI, sharingId);
        Cursor cursor = mLocalContentResolver.query(contentUri, null, null, null, null);
        CursorUtil.assertCursorIsNotNull(cursor, contentUri);
        return cursor;
    }

    /**
     * @param sharingId the sharing ID
     * @return Cursor direction (INCOMING, ...)
     */
    public Direction getImageSharingDirection(String sharingId) {
        Cursor cursor = getImageTransferData(ImageSharingData.KEY_DIRECTION, sharingId);
        if (cursor == null) {
            return null;
        }
        return Direction.valueOf(getDataAsInteger(cursor));
    }

    /**
     * @param sharingId the sharing ID
     * @return uri of file
     */
    public Uri getFile(String sharingId) {
        Cursor cursor = getImageTransferData(ImageSharingData.KEY_FILE, sharingId);
        if (cursor == null) {
            return null;
        }
        return Uri.parse(getDataAsString(cursor));
    }
}
