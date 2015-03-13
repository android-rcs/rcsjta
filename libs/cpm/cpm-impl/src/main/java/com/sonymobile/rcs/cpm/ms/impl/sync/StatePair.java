/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.sonymobile.rcs.cpm.ms.impl.sync;

import com.sonymobile.rcs.cpm.ms.CpmObjectMetadata;
import com.sonymobile.rcs.cpm.ms.CpmObjectType;

/**
 * Holds the local and remote states in order to compare.
 */
public class StatePair {

    private final CpmObjectMetadata mLocalState;

    private final CpmObjectMetadata mRemoteState;

    public StatePair(String itemId, CpmObjectType type, String localGroupId, String remoteGroupId) {
        mLocalState = new CpmObjectMetadata(itemId, type);
        mLocalState.setGroupId(localGroupId);
        mRemoteState = new CpmObjectMetadata(itemId, type);
        mRemoteState.setGroupId(remoteGroupId);
    }

    public StatePair(CpmObjectMetadata local, CpmObjectMetadata remote) {
        super();
        if (local != null) {
            mLocalState = local.copy();
        } else {
            mLocalState = remote.copy();
            mLocalState.reset();
        }

        if (remote != null) {
            mRemoteState = remote.copy();
        } else {
            mRemoteState = local.copy();
            mRemoteState.reset();
        }
    }

    public String getId() {
        return mLocalState.getId();
    }

    public CpmObjectMetadata getLocalState() {
        return mLocalState;
    }

    public CpmObjectMetadata getRemoteState() {
        return mRemoteState;
    }

    @Override
    public String toString() {
        return "Item[ local(" + mLocalState + "), remote(" + mRemoteState + ")]";
    }

    public void applyLocalChange() {
        if (mRemoteState.isDeleted()) {
            mLocalState.setDeleted(true);
        }
        if (mRemoteState.isRead()) {
            mLocalState.setRead(true);
        }
    }

    public void applyRemoteChange() {
        if (mLocalState.isDeleted()) {
            mRemoteState.setDeleted(true);
        }
        if (mLocalState.isRead()) {
            mRemoteState.setRead(true);
        }
    }

    public boolean hasLocalChange() {
        boolean deleted = mRemoteState.isDeleted() && !mLocalState.isDeleted();
        boolean read = mRemoteState.isRead() && !mLocalState.isRead();

        return (deleted || read);
    }

    public boolean hasRemoteChange() {
        boolean deleted = mLocalState.isDeleted() && !mRemoteState.isDeleted();
        boolean read = mLocalState.isRead() && !mRemoteState.isRead();

        return (deleted || read);
    }

    public CpmObjectMetadata getLocalChange() {
        boolean deleted = mRemoteState.isDeleted() && !mLocalState.isDeleted();
        boolean read = mRemoteState.isRead() && !mLocalState.isRead();

        if (deleted || read) {
            CpmObjectMetadata hi = mLocalState.copy();
            hi.setDeleted(deleted);
            hi.setRead(read);
            return hi;
        }

        return null;
    }

    public CpmObjectMetadata getRemoteChange() {
        boolean deleted = mLocalState.isDeleted() && !mRemoteState.isDeleted();
        boolean read = mLocalState.isRead() && !mRemoteState.isRead();

        if (deleted || read) {
            CpmObjectMetadata hi = mRemoteState.copy();
            hi.setDeleted(deleted);
            hi.setRead(read);
            return hi;
        }

        return null;
    }

}
