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

import com.sonymobile.rcs.cpm.ms.ConversationHistoryFolder;
import com.sonymobile.rcs.cpm.ms.CpmMessageStoreException;
import com.sonymobile.rcs.cpm.ms.CpmObjectMetadata;
import com.sonymobile.rcs.cpm.ms.SessionHistoryFolder;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO : test when the folder has not changed and flags have changed

public class DefaultSyncStrategy extends AbstractSyncStrategy {

    private static final long serialVersionUID = 1L;

    private ChangeResolver mSyncHelper = new ChangeResolver();

    // private List<ItemState> newItems = new ArrayList<ItemState>();

    // private Map<String, Set<String>> mRemoteItemIdsByGroupId = new HashMap<String,
    // Set<String>>();

    private static Logger LOG = Logger.getLogger(DefaultSyncStrategy.class.getName());

    @Override
    public void execute() throws CpmMessageStoreException {
        debug("** Analyze local changes **");
        analyzeLocalFlagsChanges();
        report.setProgress(15);

        debug("** Analyze remote changes **");
        analyzeRemoteChanges();
        report.setProgress(30);

        debug("** Update remote flags **");
        applyRemoteChanges();
        report.setProgress(45);

        debug("** Insert new items in local db **");
        insertNewItemsAndGroups();
        report.setProgress(60);

        debug("** Update local flags **");
        applyLocalChanges();
        report.setProgress(75);

        mSyncHelper.purge();

    }

    private void analyzeLocalFlagsChanges() throws CpmMessageStoreException {
        mSyncHelper.markAllAsDeletedLocally();

        Set<? extends ConversationHistoryFolder> localConvs = localStore.getDefaultFolder()
                .getConversationHistoryFolders();

        for (ConversationHistoryFolder localConv : localConvs) {
            // TODO standalone
            Set<? extends SessionHistoryFolder> localSessions = localConv
                    .getSessionHistoryFolders();
            for (SessionHistoryFolder sessionHistoryFolder : localSessions) {
                Iterable<? extends CpmObjectMetadata> allFlags = sessionHistoryFolder
                        .getObjectMetadata();
                for (CpmObjectMetadata flags : allFlags) {
                    mSyncHelper.markAsExistsLocally(flags);
                }
            }
        }
    }

    private void analyzeRemoteChanges() throws CpmMessageStoreException {
        // Browse remote
        Collection<SessionHistoryFolder> remoteGroups = new HashSet<SessionHistoryFolder>();
        for (ConversationHistoryFolder remoteConv : remoteStore.getDefaultFolder()
                .getConversationHistoryFolders()) {
            remoteGroups.addAll(remoteConv.getSessionHistoryFolders());
        }

        for (SessionHistoryFolder remoteGroup : remoteGroups) {

            String remoteGroupId = remoteGroup.getId().toString();

            debug("Analyzing remote group : " + remoteGroupId);

            int remoteVersion = remoteGroup.getVersion();

            int savedVersion = mSyncHelper.getRemoteGroupVersion(remoteGroupId);

            if (remoteVersion == 0 || savedVersion != remoteVersion) {
                if (savedVersion == 0) { // that means doesnt exist yet
                    // TODO careful might break here
                    mSyncHelper.setRemoteGroupVersion(remoteGroupId, remoteVersion);
                }

                Iterable<? extends CpmObjectMetadata> remoteFlagsInfos = remoteGroup
                        .getObjectMetadata();

                // Detect remote new
                for (CpmObjectMetadata remoteState : remoteFlagsInfos) {

                    mSyncHelper.markAsExistsRemotely(remoteState);

                    if (!remoteState.isDeleted()) {
                        // will be useful later
                        // saveExistingRemoteId(remoteState.getId(), remoteGroupId);
                    }

                    if (!mSyncHelper.existsLocally(remoteState.getId())) {

                        debug("found a new remote item with id = " + remoteState.getId());

                        // newItems.add(newItem);
                    }
                }

            } else { // if version are same do nothing
                debug("remoteGroup " + remoteGroupId + " version (" + savedVersion
                        + ") hasnt changed");
            }

        } // end for loop on sessions
    }

    public void insertNewItemsAndGroups() throws CpmMessageStoreException {
        List<CpmObjectMetadata> newItems = null;
        debug("There are " + newItems.size() + " new items");

        for (CpmObjectMetadata item : newItems) {
            String itemId = item.getId();
            String localGroupId = mSyncHelper.getLocalGroupIdForItemId(itemId);
            if (localGroupId == null) {
                // TODO !!!! hard coded rule !!!!!! we create same group locally !!!!
                String remoteGroupId = mSyncHelper.getRemoteGroupIdForItemId(itemId);

                localGroupId = remoteGroupId;
                debug("getting group : " + remoteGroupId);
                // ItemGroup remoteGroup = remoteStore.getGroupById(remoteGroupId);
                // GroupInfo remoteGroupInfo = remoteGroup.getInfo();
                // debug("Creating new group : "+remoteGroupInfo);
                // localStore.createGroup(remoteGroupInfo);

            }
            // ItemGroup localGroup = localStore.getGroupById(localGroupId);
            // debug("adding item "+itemId+" to group "+localGroupId);
            // localGroup.addItem(item);
            item.setDeleted(false);
            mSyncHelper.markAsExistsLocally(item);
        }

    }

    public void applyLocalChanges() throws CpmMessageStoreException {
        // Set<String> synchronizedGroupIds = mRemoteItemIdsByGroupId.keySet();
        // for (String remoteGroupId : synchronizedGroupIds) {
        // Set<String> maybeExistIds = mSyncHelper.getItemIdsByRemoteGroupId(remoteGroupId);
        // Set<String> existIds = mRemoteItemIdsByGroupId.get(remoteGroupId);
        // for (String id : maybeExistIds) {
        // if (existIds.contains(id)) {
        // mSyncHelper.markAsDeletedRemotely(id);
        // }
        // }
        // }

        Collection<CpmObjectMetadata> localChanges = mSyncHelper.getLocalChanges();

        for (CpmObjectMetadata changedItem : localChanges) {
            // ItemGroup group = localStore.getGroupById(changedItem.getGroupId());
            // group.updateItem(changedItem);
        }

        mSyncHelper.localChangesUpdated();
    }

    public void applyRemoteChanges() throws CpmMessageStoreException {

        // apply changes
        Collection<CpmObjectMetadata> remoteChanges = mSyncHelper.getRemoteChanges();

        for (CpmObjectMetadata changedItem : remoteChanges) {
            // ItemGroup group = remoteStore.getGroupById(changedItem.getGroupId());
            // group.updateItem(changedItem);
        }

        mSyncHelper.remoteChangesUpdated();

        // it s ok now to release remote connection
        try {
            remoteStore.close();
        } catch (IOException e) {
            LOG.warning("Problem while closing remote resource");
        }
    }

    protected void debug(String message) {
        report.setMessage(message);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(message);
        }
    }

    // private void saveExistingRemoteId(String itemId, String remoteGroupId){
    // Set<String> ids = mRemoteItemIdsByGroupId.get(remoteGroupId);
    // if (ids == null) {
    // ids = new HashSet<String>();
    // mRemoteItemIdsByGroupId.put(remoteGroupId, ids);
    // }
    // ids.add(itemId);
    // }

}
