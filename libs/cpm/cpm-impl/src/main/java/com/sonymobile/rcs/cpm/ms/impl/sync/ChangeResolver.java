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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChangeResolver {

    private Map<String, Integer> mRemoteGroupVersionMap = new HashMap<String, Integer>();

    private Map<String, Integer> mLocalGroupVersionMap = new HashMap<String, Integer>();

    private Map<String, StatePair> mStatePairs = new HashMap<String, StatePair>();

    private boolean mLocalGroupIdsResolved = false;

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("SYNCHELPER[");
        sb.append("\n");
        for (StatePair si : mStatePairs.values()) {
            sb.append(si.toString());
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    public synchronized void purge() {
        // newItems.clear();
        for (StatePair si : mStatePairs.values()) {
            if (si.getLocalState().isDeleted()) {
                mStatePairs.remove(si.getId());
            }
        }
        mLocalGroupIdsResolved = false;
    }

    public int getRemoteGroupVersion(String groupId) {
        if (mRemoteGroupVersionMap.containsKey(groupId.toString())) {
            return mRemoteGroupVersionMap.get(groupId);
        }
        return 0;
    }

    public void setRemoteGroupVersion(String groupId, int version) {
        mRemoteGroupVersionMap.put(groupId.toString(), version);
    }

    public int getLocalGroupVersion(String groupId) {
        if (mLocalGroupVersionMap.containsKey(groupId.toString())) {
            return mLocalGroupVersionMap.get(groupId);
        }
        return 0;
    }

    public void setLocalGroupVersion(String groupId, int version) {
        mLocalGroupVersionMap.put(groupId.toString(), version);
    }

    public String getLocalGroupIdForItemId(String id) {
        resolveLocalGroupIds();
        return mStatePairs.get(id).getLocalState().getGroupId();
    }

    public String getRemoteGroupIdForItemId(String id) {
        return mStatePairs.get(id).getRemoteState().getGroupId();
    }

    private synchronized void resolveLocalGroupIds() {
        if (mLocalGroupIdsResolved)
            return;

        Map<String, String> localRemoteMap = new HashMap<String, String>();
        for (StatePair si : mStatePairs.values()) {
            if (si.getLocalState().getGroupId() != null && si.getRemoteState().getGroupId() != null) {
                localRemoteMap.put(si.getRemoteState().getGroupId(), si.getLocalState()
                        .getGroupId());
            }
        }
        for (StatePair si : mStatePairs.values()) {
            if (si.getLocalState().getGroupId() == null && si.getRemoteState().getGroupId() != null) {
                si.getLocalState().setGroupId(localRemoteMap.get(si.getRemoteState().getGroupId()));
            }
        }

        mLocalGroupIdsResolved = true;
    }

    public void markAllAsDeletedLocally() {
        for (StatePair item : mStatePairs.values()) {
            item.getLocalState().setDeleted(true);
        }
    }

    public void markAsExistsLocally(CpmObjectMetadata localState) {
        StatePair it = mStatePairs.get(localState.getId());

        if (it == null) {

            it = new StatePair(localState, null);
            mStatePairs.put(it.getId(), it);

        } else {
            it.getLocalState().update(localState);
        }
    }

    public void markAsExistsRemotely(CpmObjectMetadata remoteState) {
        StatePair it = mStatePairs.get(remoteState.getId());
        if (it == null) {

            it = new StatePair(null, remoteState);
            mStatePairs.put(it.getId(), it);

        } else {
            it.getRemoteState().update(remoteState);
        }
    }

    public void markAsDeletedRemotely(String id) {
        mStatePairs.get(id).getRemoteState().setDeleted(true);
    }

    public boolean existsLocally(String id) {
        StatePair si = mStatePairs.get(id);
        return si != null && si.getLocalState().getGroupId() != null;
    }

    public Collection<CpmObjectMetadata> getRemoteChanges() {
        Collection<CpmObjectMetadata> li = new ArrayList<CpmObjectMetadata>();
        for (StatePair item : mStatePairs.values()) {
            if (item.hasRemoteChange()) {
                li.add(item.getRemoteChange());
            }
        }
        return li;
    }

    public void remoteChangesUpdated() {
        for (StatePair item : mStatePairs.values()) {
            item.applyRemoteChange();
        }
    }

    public Collection<CpmObjectMetadata> getLocalChanges() {
        Collection<CpmObjectMetadata> li = new ArrayList<CpmObjectMetadata>();
        for (StatePair item : mStatePairs.values()) {
            if (item.hasLocalChange()) {
                li.add(item.getLocalChange());
            }
        }
        return li;
    }

    public void localChangesUpdated() {
        for (StatePair item : mStatePairs.values()) {
            item.applyLocalChange();
        }
    }

    public Set<String> getItemIdsByRemoteGroupId(String id) {
        Set<String> li = new HashSet<String>();
        for (StatePair item : mStatePairs.values()) {
            if ((item.getRemoteState().getGroupId() != null)
                    && item.getRemoteState().getGroupId().equals(id.toString())) {
                li.add(item.getId());
            }
        }
        return li;
    }

}
