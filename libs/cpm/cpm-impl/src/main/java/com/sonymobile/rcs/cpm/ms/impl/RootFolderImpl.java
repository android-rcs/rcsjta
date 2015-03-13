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

package com.sonymobile.rcs.cpm.ms.impl;

import com.sonymobile.rcs.cpm.ms.ConversationHistoryFolder;
import com.sonymobile.rcs.cpm.ms.CpmMessageStore;
import com.sonymobile.rcs.cpm.ms.CpmMessageStoreException;
import com.sonymobile.rcs.cpm.ms.RootFolder;
import com.sonymobile.rcs.imap.ImapFolder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RootFolderImpl extends BasicFolderNode implements RootFolder {

    private final CommonMessageStoreImpl mMessageStore;

    public RootFolderImpl(String path, CommonMessageStoreImpl ms) {
        super(null, ms.getImapService().getRootFolder(path));
        this.mMessageStore = ms;
    }

    @Override
    public CpmMessageStore getMessageStore() {
        return mMessageStore;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<? extends ConversationHistoryFolder> getConversationHistoryFolders()
            throws CpmMessageStoreException {
        if (getSubFolders() != null) {
            return (Set<? extends ConversationHistoryFolder>) getSubFolders();
        }

        try {
            Set<ConversationHistoryFolderImpl> conversations = new HashSet<ConversationHistoryFolderImpl>();

            List<ImapFolder> li = getImapService().getFolders(getPath(), false);

            for (ImapFolder f : li) {
                conversations.add(new ConversationHistoryFolderImpl(this, f));
            }

            setSubFolders(conversations);
            return conversations;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CpmMessageStoreException("Reading conversation failed i root path: "
                    + getPath(), e);
        }

    }

    @Override
    public ConversationHistoryFolder getConversationHistoryFolder(String conversationId)
            throws CpmMessageStoreException {
        try {
            return new ConversationHistoryFolderImpl(this, getImapFolder().getSubFolder(
                    conversationId));
        } catch (Exception e) {
            throw new CpmMessageStoreException("Cannot create conversation (" + conversationId
                    + ") in root path: " + getPath(), e);
        }
    }

}
