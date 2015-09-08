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

package com.gsma.rcs.provider.ipcall;

import com.gsma.rcs.core.content.AudioContent;
import com.gsma.rcs.core.content.VideoContent;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
import com.gsma.rcs.service.ipcalldraft.IPCall.ReasonCode;
import com.gsma.rcs.service.ipcalldraft.IPCall.State;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;

import android.database.Cursor;
import android.net.Uri;

/**
 * IPCallPersistedStorageAccessor helps in retrieving persisted data related to a IP call from the
 * persisted storage. It can utilize caching for such data that will not be changed after creation
 * of the IP call to speed up consecutive access.
 */
public class IPCallPersistedStorageAccessor {

    private final String mCallId;

    private final IPCallHistory mIPCallLog;

    private ContactId mContact;

    private Direction mDirection;

    private long mTimestamp;

    public IPCallPersistedStorageAccessor(String callId, IPCallHistory ipCallLog) {
        mCallId = callId;
        mIPCallLog = ipCallLog;
    }

    public IPCallPersistedStorageAccessor(String callId, ContactId contact, Direction direction,
            IPCallHistory ipCallLog, long timestamp) {
        mCallId = callId;
        mContact = contact;
        mDirection = direction;
        mIPCallLog = ipCallLog;
        mTimestamp = timestamp;
    }

    private void cacheData() {
        Cursor cursor = null;
        try {
            cursor = mIPCallLog.getIPCallData(mCallId);
            if (!cursor.moveToNext()) {
                throw new ServerApiPersistentStorageException(
                        "Data not found for ip call ".concat(mCallId));
            }
            String contact = cursor.getString(cursor.getColumnIndexOrThrow(IPCallData.KEY_CONTACT));
            if (contact != null) {
                mContact = ContactUtil.createContactIdFromTrustedData(contact);
            }
            mDirection = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(IPCallData.KEY_DIRECTION)));
            mTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(IPCallData.KEY_TIMESTAMP));
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

    public State getState() {
        State state = mIPCallLog.getState(mCallId);
        if (state == null) {
            throw new ServerApiPersistentStorageException(
                    "State not found for ip call ".concat(mCallId));
        }
        return state;
    }

    public ReasonCode getReasonCode() {
        ReasonCode reasonCode = mIPCallLog.getReasonCode(mCallId);
        if (reasonCode == null) {
            throw new ServerApiPersistentStorageException(
                    "Reason code not found for ip call ".concat(mCallId));
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
        return mIPCallLog.setCallStateAndReasonCode(mCallId, state, reasonCode);
    }

    public Uri addCall(ContactId contact, Direction direction, AudioContent audiocontent,
            VideoContent videocontent, State state, ReasonCode reasonCode, long timestamp) {
        mContact = contact;
        mDirection = direction;
        mTimestamp = timestamp;
        return mIPCallLog.addCall(mCallId, contact, direction, audiocontent, videocontent, state,
                reasonCode, timestamp);
    }
}
