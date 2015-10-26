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

import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing.ReasonCode;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing.State;

import android.database.Cursor;

/**
 * GeolocSharingPersistedStorageAccessor helps in retrieving persisted data related to a geoloc
 * sharing from the persisted storage. It can utilize caching for such data that will not be changed
 * after creation of the geoloc sharing to speed up consecutive access.
 */
public class GeolocSharingPersistedStorageAccessor {

    private final String mSharingId;

    private final RichCallHistory mRichCallLog;

    private ContactId mContact;

    private Geoloc mGeoloc;

    private Direction mDirection;

    private long mTimestamp;

    public GeolocSharingPersistedStorageAccessor(String sharingId, RichCallHistory richCallHistory) {
        mSharingId = sharingId;
        mRichCallLog = richCallHistory;
    }

    public GeolocSharingPersistedStorageAccessor(String sharingId, ContactId contact,
            Geoloc geoloc, Direction direction, RichCallHistory richCallHistory, long timestamp) {
        mSharingId = sharingId;
        mContact = contact;
        mGeoloc = geoloc;
        mDirection = direction;
        mRichCallLog = richCallHistory;
        mTimestamp = timestamp;
    }

    private void cacheData() {
        Cursor cursor = null;
        try {
            cursor = mRichCallLog.getGeolocSharingData(mSharingId);
            if (!cursor.moveToNext()) {
                throw new ServerApiPersistentStorageException(new StringBuilder(
                        "Data not found for video sharing ").append(mSharingId).toString());
            }
            String contact = cursor.getString(cursor
                    .getColumnIndexOrThrow(GeolocSharingData.KEY_CONTACT));
            if (contact != null) {
                mContact = ContactUtil.createContactIdFromTrustedData(contact);
            }
            mDirection = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(GeolocSharingData.KEY_DIRECTION)));
            String geoloc = cursor.getString(cursor
                    .getColumnIndexOrThrow(GeolocSharingData.KEY_CONTENT));
            if (geoloc != null) {
                mGeoloc = new Geoloc(geoloc);
            }
            mTimestamp = cursor.getLong(cursor
                    .getColumnIndexOrThrow(GeolocSharingData.KEY_TIMESTAMP));
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

    public Geoloc getGeoloc() {
        /*
         * Utilizing cache here as geoloc can't be changed in persistent storage after geoloc has
         * been set anyway so no need to query for it multiple times.
         */
        if (mGeoloc == null) {
            cacheData();
        }
        return mGeoloc;
    }

    public String getMimeType() {
        return MimeType.GEOLOC_MESSAGE;
    }

    public State getState() {
        State state = mRichCallLog.getGeolocSharingState(mSharingId);
        if (state == null) {
            throw new ServerApiPersistentStorageException(new StringBuilder(
                    "State not found for geoloc sharing ").append(mSharingId).toString());
        }
        return state;
    }

    public ReasonCode getReasonCode() {
        ReasonCode reasonCode = mRichCallLog.getGeolocSharingReasonCode(mSharingId);
        if (reasonCode == null) {
            throw new ServerApiPersistentStorageException(new StringBuilder(
                    "Reason code not found for geoloc sharing ").append(mSharingId).toString());
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
        return mRichCallLog.setGeolocSharingStateAndReasonCode(mSharingId, state, reasonCode);
    }

    public void addIncomingGeolocSharing(ContactId contact, State state, ReasonCode reasonCode,
            long timestamp) {
        mContact = contact;
        mTimestamp = timestamp;
        mRichCallLog.addIncomingGeolocSharing(mContact, mSharingId, state, reasonCode, timestamp);
    }
}
