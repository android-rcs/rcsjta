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

package com.gsma.rcs.provider.sharing;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.content.VideoContent;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing.ReasonCode;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing.State;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingLog;
import com.gsma.services.rcs.sharing.image.ImageSharing;
import com.gsma.services.rcs.sharing.image.ImageSharingLog;
import com.gsma.services.rcs.sharing.video.VideoSharing;
import com.gsma.services.rcs.sharing.video.VideoSharingLog;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
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
    private static RichCallHistory mInstance;

    private final LocalContentResolver mLocalContentResolver;

    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(RichCallHistory.class.getSimpleName());

    private static final int FIRST_COLUMN_IDX = 0;

    private static final String SELECTION_BY_INTERRUPTED_GEOLOC_SHARINGS = new StringBuilder(
            GeolocSharingData.KEY_STATE).append(" IN ('")
            .append(GeolocSharing.State.STARTED.toInt()).append("','")
            .append(GeolocSharing.State.INITIATING.toInt()).append("','")
            .append(GeolocSharing.State.INVITED.toInt()).append("')").toString();

    private static final String SELECTION_BY_INTERRUPTED_IMAGE_SHARINGS = new StringBuilder(
            ImageSharingData.KEY_STATE).append(" IN ('").append(ImageSharing.State.STARTED.toInt())
            .append("','").append(ImageSharing.State.INITIATING.toInt()).append("','")
            .append(ImageSharing.State.INVITED.toInt()).append("')").toString();

    private static final String SELECTION_BY_INTERRUPTED_VIDEO_SHARINGS = new StringBuilder(
            VideoSharingData.KEY_STATE).append(" IN ('").append(VideoSharing.State.STARTED.toInt())
            .append("','").append(VideoSharing.State.INITIATING.toInt()).append("','")
            .append(VideoSharing.State.INVITED.toInt()).append("')").toString();

    private static final String ORDER_BY_TIMESTAMP_ASC = GeolocSharingData.KEY_TIMESTAMP
            .concat(" ASC");

    /**
     * Get image transfer info from its unique Id
     * 
     * @param columnName
     * @param sharingId
     * @return Cursor the caller of this method has to close the cursor if a cursor is returned
     */
    private Cursor getImageTransferData(String columnName, String sharingId) {
        String[] projection = new String[] {
            columnName
        };
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(
                    Uri.withAppendedPath(ImageSharingLog.CONTENT_URI, sharingId), projection, null,
                    null, null);
            if (cursor.moveToFirst()) {
                return cursor;
            }
            throw new SQLException(
                    "No row returned while querying for image transfer data with sharingId : "
                            .concat(sharingId));

        } catch (RuntimeException e) {
            if (cursor != null) {
                cursor.close();
            }
            throw e;
        }
    }

    /**
     * Get video sharing info from its unique Id
     * 
     * @param columnName
     * @param sharingId
     * @return Cursor the caller of this method has to close the cursor if a cursor is returned
     */
    private Cursor getVideoSharingData(String columnName, String sharingId) {
        String[] projection = new String[] {
            columnName
        };
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(
                    Uri.withAppendedPath(VideoSharingLog.CONTENT_URI, sharingId), projection, null,
                    null, null);
            if (cursor.moveToFirst()) {
                return cursor;
            }
            throw new SQLException(
                    "No row returned while querying for video sharing data with sharingId : "
                            .concat(sharingId));

        } catch (RuntimeException e) {
            if (cursor != null) {
                cursor.close();
            }
            throw e;
        }
    }

    private int getDataAsInt(Cursor cursor) {
        try {
            return cursor.getInt(FIRST_COLUMN_IDX);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private long getDataAsLong(Cursor cursor) {
        try {
            return cursor.getLong(FIRST_COLUMN_IDX);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Create instance
     * 
     * @param localContentResolver Local content resolver
     */
    public static synchronized void createInstance(LocalContentResolver localContentResolver) {
        if (mInstance == null) {
            mInstance = new RichCallHistory(localContentResolver);
        }
    }

    /**
     * Returns instance
     * 
     * @return Instance
     */
    public static RichCallHistory getInstance() {
        return mInstance;
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
     * Get geoloc sharing data
     * 
     * @param columnName Column name
     * @param sharingId Sharing ID
     */
    private Cursor getGeolocSharingData(String columnName, String sharingId) throws SQLException {
        String[] projection = new String[] {
            columnName
        };
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(
                    Uri.withAppendedPath(GeolocSharingData.CONTENT_URI, sharingId), projection,
                    null, null, null);
            if (cursor.moveToFirst()) {
                return cursor;
            }

            throw new SQLException(
                    "No row returned while querying for geoloc sharing data with sharingId : "
                            + sharingId);

        } catch (RuntimeException e) {
            if (logger.isActivated()) {
                logger.error(
                        "Exception occured while retrieving geoloc sharing data of sharingId = '"
                                + sharingId + "' ! ", e);
            }
            if (cursor != null) {
                cursor.close();
            }
            throw e;
        }
    }

    /*--------------------- Video sharing methods ----------------------*/

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
            logger.debug(new StringBuilder("Add new video sharing for contact ").append(contact)
                    .append(": sharingId=").append(sharingId).append(", state=").append(state)
                    .append(", reasonCode=").append(reasonCode).toString());
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
        return mLocalContentResolver.insert(VideoSharingLog.CONTENT_URI, values);
    }

    /**
     * Set the video sharing state, reason code and duration
     * 
     * @param sharingId sharing ID of the entry
     * @param state New state
     * @param reasonCode Reason Code
     * @param duration
     */
    public void setVideoSharingStateReasonCodeAndDuration(String sharingId,
            VideoSharing.State state, VideoSharing.ReasonCode reasonCode, long duration) {
        if (logger.isActivated()) {
            logger.debug(new StringBuilder("Update video sharing state of sharing ")
                    .append(sharingId).append(" state=").append(state).append(", reasonCode=")
                    .append(reasonCode).append(" duration=").append(duration).toString());
        }
        ContentValues values = new ContentValues();
        values.put(VideoSharingData.KEY_STATE, state.toInt());
        values.put(VideoSharingData.KEY_REASON_CODE, reasonCode.toInt());
        values.put(VideoSharingData.KEY_DURATION, duration);

        mLocalContentResolver.update(Uri.withAppendedPath(VideoSharingLog.CONTENT_URI, sharingId),
                values, null, null);
    }

    /**
     * Update the video sharing duration at the end of the call
     * 
     * @param sharingId
     * @param duration Duration
     */
    public void setVideoSharingDuration(String sharingId, long duration) {
        if (logger.isActivated()) {
            logger.debug(new StringBuilder("Update duration of sharing ").append(sharingId)
                    .append(" to ").append(duration).toString());
        }
        ContentValues values = new ContentValues();
        values.put(VideoSharingData.KEY_DURATION, duration);
        mLocalContentResolver.update(Uri.withAppendedPath(VideoSharingLog.CONTENT_URI, sharingId),
                values, null, null);
    }

    /*--------------------- Image sharing methods ----------------------*/

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
        return mLocalContentResolver.insert(ImageSharingLog.CONTENT_URI, values);
    }

    /**
     * Set the image sharing state and reason code
     * 
     * @param sharingId
     * @param state New state
     * @param reasonCode Reason Code
     */
    public void setImageSharingStateAndReasonCode(String sharingId, ImageSharing.State state,
            ImageSharing.ReasonCode reasonCode) {
        if (logger.isActivated()) {
            logger.debug("Update status of image sharing " + sharingId + " to " + state);
        }
        ContentValues values = new ContentValues();
        values.put(ImageSharingData.KEY_STATE, state.toInt());
        values.put(ImageSharingData.KEY_REASON_CODE, reasonCode.toInt());
        if (state == ImageSharing.State.TRANSFERRED) {
            // Update the size of bytes if fully transferred
            long total = getImageSharingTotalSize(sharingId);
            if (total != 0) {
                values.put(ImageSharingData.KEY_TRANSFERRED, total);
            }
        }
        mLocalContentResolver.update(Uri.withAppendedPath(ImageSharingLog.CONTENT_URI, sharingId),
                values, null, null);
    }

    /**
     * Read the total size of transferred image
     * 
     * @param sharingId
     * @return the total size (or 0 if failed)
     */
    public long getImageSharingTotalSize(String sharingId) {
        Cursor c = null;
        try {
            String[] projection = new String[] {
                ImageSharingData.KEY_FILESIZE
            };
            c = mLocalContentResolver.query(
                    Uri.withAppendedPath(ImageSharingLog.CONTENT_URI, sharingId), projection, null,
                    null, null);
            if (c.moveToFirst()) {
                return c.getLong(0);
            }
        } catch (Exception e) {
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return 0L;
    }

    /**
     * Update the image sharing progress
     * 
     * @param sharingId Session ID of the entry
     * @param currentSize Current size
     */
    public void setImageSharingProgress(String sharingId, long currentSize) {
        ContentValues values = new ContentValues();
        values.put(ImageSharingData.KEY_TRANSFERRED, currentSize);
        mLocalContentResolver.update(Uri.withAppendedPath(ImageSharingLog.CONTENT_URI, sharingId),
                values, null, null);
    }

    /*--------------------- Geoloc sharing methods ----------------------*/

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
    public Uri addIncomingGeolocSharing(ContactId contact, String sharingId, State state,
            ReasonCode reasonCode, long timestamp) {
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
            State state, ReasonCode reasonCode, long timestamp) {
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
     */
    public void setGeolocSharingTransferred(String sharingId, Geoloc geoloc) {
        ContentValues values = new ContentValues();
        values.put(GeolocSharingData.KEY_CONTENT, geoloc.toString());
        values.put(GeolocSharingData.KEY_STATE, GeolocSharing.State.TRANSFERRED.toInt());
        values.put(GeolocSharingData.KEY_REASON_CODE, GeolocSharing.ReasonCode.UNSPECIFIED.toInt());
        if (mLocalContentResolver.update(
                Uri.withAppendedPath(GeolocSharingData.CONTENT_URI, sharingId), values, null, null) < 1) {
            /* TODO: Exception throwing should be implemented here in CR037 */
            if (logger.isActivated()) {
                logger.warn(new StringBuilder("There was no geoloc sharing for sharingId '")
                        .append(sharingId).append("' to update!").toString());
            }
        }
    }

    /**
     * Update the geoloc sharing state and reason code
     * 
     * @param sharingId Sharing ID
     * @param state Geoloc sharing state
     * @param reasonCode Reason code of the state
     */
    public void setGeolocSharingStateAndReasonCode(String sharingId, State state,
            ReasonCode reasonCode) {
        ContentValues values = new ContentValues();
        values.put(GeolocSharingData.KEY_STATE, state.toInt());
        values.put(GeolocSharingData.KEY_REASON_CODE, reasonCode.toInt());
        if (mLocalContentResolver.update(
                Uri.withAppendedPath(GeolocSharingData.CONTENT_URI, sharingId), values, null, null) < 1) {
            /* TODO: Exception throwing should be implemented here in CR037 */
            if (logger.isActivated()) {
                logger.warn(new StringBuilder("There was no geoloc sharing for sharingId '")
                        .append(sharingId).append("' to update!").toString());
            }
        }
    }

    /**
     * Get the geoloc sharing state
     * 
     * @param sharingId Sharing ID
     * @return state
     */
    public GeolocSharing.State getGeolocSharingState(String sharingId) {
        if (logger.isActivated()) {
            logger.debug(new StringBuilder("Get geoloc sharing state for ").append(sharingId)
                    .append(".").toString());
        }
        return State.valueOf(getDataAsInt(getGeolocSharingData(GeolocSharingData.KEY_STATE,
                sharingId)));
    }

    /**
     * Get the geoloc sharing state reason code
     * 
     * @param sharingId Sharing ID
     * @return reason code
     */
    public GeolocSharing.ReasonCode getGeolocSharingStateReasonCode(String sharingId) {
        if (logger.isActivated()) {
            logger.debug(new StringBuilder("Get geoloc sharing state reason code for ")
                    .append(sharingId).append(".").toString());
        }
        return ReasonCode.valueOf(getDataAsInt(getGeolocSharingData(
                GeolocSharingData.KEY_REASON_CODE, sharingId)));
    }

    public Cursor getInterruptedGeolocSharings() {
        return mLocalContentResolver.query(GeolocSharingData.CONTENT_URI, null,
                SELECTION_BY_INTERRUPTED_GEOLOC_SHARINGS, null, ORDER_BY_TIMESTAMP_ASC);
    }

    public Cursor getInterruptedImageSharings() {
        return mLocalContentResolver.query(ImageSharingLog.CONTENT_URI, null,
                SELECTION_BY_INTERRUPTED_IMAGE_SHARINGS, null, ORDER_BY_TIMESTAMP_ASC);
    }

    public Cursor getInterruptedVideoSharings() {
        return mLocalContentResolver.query(VideoSharingLog.CONTENT_URI, null,
                SELECTION_BY_INTERRUPTED_VIDEO_SHARINGS, null, ORDER_BY_TIMESTAMP_ASC);
    }

    /**
     * Get cache-able geolocation sharing info from its unique Id
     * 
     * @param sharingId
     * @return Cursor the caller of this method has to close the cursor if a cursor is returned
     */
    public Cursor getCacheableGeolocSharingData(String sharingId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(
                    Uri.withAppendedPath(GeolocSharingLog.CONTENT_URI, sharingId), null, null,
                    null, null);
            if (cursor.moveToFirst()) {
                return cursor;
            }
            throw new SQLException(
                    "No row returned while querying for geoloc sharing data with sharingId : "
                            + sharingId);

        } catch (RuntimeException e) {
            if (cursor != null) {
                cursor.close();
            }
            throw e;
        }
    }

    /**
     * Delete all entries in Rich Call history
     */
    public void deleteAllEntries() {
        mLocalContentResolver.delete(ImageSharingLog.CONTENT_URI, null, null);
        mLocalContentResolver.delete(VideoSharingLog.CONTENT_URI, null, null);
        mLocalContentResolver.delete(GeolocSharingData.CONTENT_URI, null, null);
    }

    /**
     * Get Image sharing state from unique Id
     * 
     * @param sharingId
     * @return State
     */
    public ImageSharing.State getImageSharingState(String sharingId) {
        if (logger.isActivated()) {
            logger.debug("Get image transfer state for sharingId ".concat(sharingId));
        }
        return ImageSharing.State.valueOf(getDataAsInt(getImageTransferData(
                ImageSharingData.KEY_STATE, sharingId)));
    }

    /**
     * Get Image sharing reason code from unique Id
     * 
     * @param sharingId
     * @return Reason code
     */
    public ImageSharing.ReasonCode getImageSharingReasonCode(String sharingId) {
        if (logger.isActivated()) {
            logger.debug("Get image transfer reason code for sharingId ".concat(sharingId));
        }
        return ImageSharing.ReasonCode.valueOf(getDataAsInt(getImageTransferData(
                ImageSharingData.KEY_REASON_CODE, sharingId)));
    }

    /**
     * Get Video sharing state from unique Id
     * 
     * @param sharingId
     * @return State
     */
    public VideoSharing.State getVideoSharingState(String sharingId) {
        if (logger.isActivated()) {
            logger.debug("Get video share state for sharingId ".concat(sharingId));
        }
        return VideoSharing.State.valueOf(getDataAsInt(getVideoSharingData(
                VideoSharingData.KEY_STATE, sharingId)));
    }

    /**
     * Get Video sharing reason code from unique Id
     * 
     * @param sharingId
     * @return Reason code
     */
    public VideoSharing.ReasonCode getVideoSharingReasonCode(String sharingId) {
        if (logger.isActivated()) {
            logger.debug("Get video share reason code for sharingId ".concat(sharingId));
        }
        return VideoSharing.ReasonCode.valueOf(getDataAsInt(getVideoSharingData(
                VideoSharingData.KEY_REASON_CODE, sharingId)));
    }

    /**
     * Get cacheable image transfer info from its unique Id
     * 
     * @param sharingId
     * @return Cursor the caller of this method has to close the cursor if a cursor is returned
     */
    public Cursor getCacheableImageTransferData(String sharingId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(
                    Uri.withAppendedPath(ImageSharingLog.CONTENT_URI, sharingId), null, null, null,
                    null);
            if (cursor.moveToFirst()) {
                return cursor;
            }
            throw new SQLException(
                    "No row returned while querying for image transfer data with sharingId : "
                            .concat(sharingId));

        } catch (RuntimeException e) {
            if (cursor != null) {
                cursor.close();
            }
            throw e;
        }
    }

    /**
     * Get cacheable video sharing info from its unique Id
     * 
     * @param sharingId
     * @return Cursor the caller of this method has to close the cursor if a cursor is returned
     */
    public Cursor getCacheableVideoSharingData(String sharingId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(
                    Uri.withAppendedPath(VideoSharingLog.CONTENT_URI, sharingId), null, null, null,
                    null);
            if (cursor.moveToFirst()) {
                return cursor;
            }
            throw new SQLException(
                    "No row returned while querying for video sharing data with sharingId : "
                            .concat(sharingId));

        } catch (RuntimeException e) {
            if (cursor != null) {
                cursor.close();
            }
            throw e;
        }
    }

    /**
     * Returns video sharing duration
     * 
     * @param sharingId
     * @return duration
     */
    public long getVideoSharingDuration(String sharingId) {
        return getDataAsLong(getVideoSharingData(VideoSharingData.KEY_DURATION, sharingId));
    }
}
