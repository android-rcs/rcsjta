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

package com.gsma.rcs.core.ims.service.ipcall;

import com.gsma.rcs.core.content.AudioContent;
import com.gsma.rcs.core.content.VideoContent;
import com.gsma.rcs.provider.ipcall.IPCallHistory;
import com.gsma.rcs.service.ipcalldraft.IPCallLog;
import com.gsma.rcs.service.ipcalldraft.IPCall.ReasonCode;
import com.gsma.rcs.service.ipcalldraft.IPCall.State;
import com.gsma.rcs.utils.ContactUtils;
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

    public IPCallPersistedStorageAccessor(String callId, IPCallHistory ipCallLog) {
        mCallId = callId;
        mIPCallLog = ipCallLog;
    }

    public IPCallPersistedStorageAccessor(String callId, ContactId contact, Direction direction,
            IPCallHistory ipCallLog) {
        mCallId = callId;
        mContact = contact;
        mDirection = direction;
        mIPCallLog = ipCallLog;
    }

    private void cacheData() {
        Cursor cursor = null;
        try {
            cursor = mIPCallLog.getCacheableIPCallData(mCallId);
            String contact = cursor.getString(cursor.getColumnIndexOrThrow(IPCallLog.CONTACT));
            if (contact != null) {
                mContact = ContactUtils.createContactId(contact);
            }
            mDirection = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(IPCallLog.DIRECTION)));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
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
        return mIPCallLog.getState(mCallId);
    }

    public ReasonCode getReasonCode() {
        return mIPCallLog.getReasonCode(mCallId);
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

    public void setStateAndReasonCode(State state, ReasonCode reasonCode) {
        mIPCallLog.setCallStateAndReasonCode(mCallId, state, reasonCode);
    }

    public Uri addCall(ContactId contact, Direction direction, AudioContent audiocontent,
            VideoContent videocontent, State state, ReasonCode reasonCode) {
        return mIPCallLog.addCall(mCallId, contact, direction, audiocontent, videocontent, state,
                reasonCode);
    }
}
