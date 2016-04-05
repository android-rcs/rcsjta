/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.ri.sharing.geoloc;

import com.gsma.rcs.ri.utils.ContactUtil;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing.ReasonCode;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing.State;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingLog;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;

/**
 * Geolocation sharing Data Object
 * 
 * @author Philippe LEMORDANT
 */
public class GeolocSharingDAO {

    private final String mSharingId;

    private final ContactId mContact;

    private final State mState;

    private final ReasonCode mReasonCode;

    private final Direction mDirection;

    private final String mMimeType;

    private final String mContent;

    private final long mTimestamp;

    private static ContentResolver sContentResolver;

    private GeolocSharingDAO(String sharingId, ContactId contact, State state,
            ReasonCode reasonCode, Direction dir, String mimeType, String content, long timestamp) {
        mSharingId = sharingId;
        mContact = contact;
        mState = state;
        mReasonCode = reasonCode;
        mDirection = dir;
        mMimeType = mimeType;
        mContent = content;
        mTimestamp = timestamp;
    }

    public String getSharingId() {
        return mSharingId;
    }

    public GeolocSharing.State getState() {
        return mState;
    }

    public String getContent() {
        return mContent;
    }

    public ContactId getContact() {
        return mContact;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public Direction getDirection() {
        return mDirection;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public ReasonCode getReasonCode() {
        return mReasonCode;
    }

    /**
     * Gets instance of chat message from RCS provider
     * 
     * @param ctx the context
     * @param sharingId the sharing ID
     * @return instance or null if entry not found
     */
    public static GeolocSharingDAO getGeolocSharing(Context ctx, String sharingId) {
        if (sContentResolver == null) {
            sContentResolver = ctx.getContentResolver();
        }
        Cursor cursor = null;
        try {
            cursor = sContentResolver.query(
                    Uri.withAppendedPath(GeolocSharingLog.CONTENT_URI, sharingId), null, null,
                    null, null);
            if (cursor == null) {
                throw new SQLException("Query failed!");
            }
            if (!cursor.moveToFirst()) {
                return null;
            }
            String contact = cursor.getString(cursor
                    .getColumnIndexOrThrow(GeolocSharingLog.CONTACT));
            ContactId contactId = null;
            if (contact != null) {
                contactId = ContactUtil.formatContact(contact);
            }
            String mimeType = cursor.getString(cursor
                    .getColumnIndexOrThrow(GeolocSharingLog.MIME_TYPE));
            String content = cursor.getString(cursor
                    .getColumnIndexOrThrow(GeolocSharingLog.CONTENT));
            State state = State.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(GeolocSharingLog.STATE)));
            Direction dir = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(GeolocSharingLog.DIRECTION)));
            long timestamp = cursor.getLong(cursor
                    .getColumnIndexOrThrow(GeolocSharingLog.TIMESTAMP));
            ReasonCode reasonCode = ReasonCode.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(GeolocSharingLog.REASON_CODE)));
            return new GeolocSharingDAO(sharingId, contactId, state, reasonCode, dir, mimeType,
                    content, timestamp);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

}
