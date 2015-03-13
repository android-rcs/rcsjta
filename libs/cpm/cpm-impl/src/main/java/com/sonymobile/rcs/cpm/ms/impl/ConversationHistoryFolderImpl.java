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
import com.sonymobile.rcs.cpm.ms.CpmMessageStoreException;
import com.sonymobile.rcs.cpm.ms.SessionHistoryFolder;
import com.sonymobile.rcs.imap.ImapFolder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConversationHistoryFolderImpl extends MessageFolderImpl implements
        ConversationHistoryFolder {

    public ConversationHistoryFolderImpl(FolderNode parent, ImapFolder folder)
            throws CpmMessageStoreException {
        super(parent, folder);
    }

    @Override
    public String getId() {
        return getName();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<? extends SessionHistoryFolder> getSessionHistoryFolders()
            throws CpmMessageStoreException {
        if (getSubFolders() != null) {
            return (Set<? extends SessionHistoryFolder>) getSubFolders();
        }

        try {
            Set<SessionHistoryFolderImpl> sessions = new HashSet<SessionHistoryFolderImpl>();

            List<ImapFolder> li = getImapService().getFolders(getPath(), false);
            for (ImapFolder f : li) {
                sessions.add(new SessionHistoryFolderImpl(this, f));
            }

            setSubFolders(sessions);
            return sessions;
        } catch (Exception e) {
            throw new CpmMessageStoreException("Couldnt retrieve sessions for conversation "
                    + getName(), e);
        }
    }

    @Override
    public SessionHistoryFolder getSessionHistoryFolder(String contributionId)
            throws CpmMessageStoreException {
        ImapFolder childFolder = getImapFolder().getSubFolder(contributionId);
        return new SessionHistoryFolderImpl(this, childFolder);
    }

    /*
     * @Override public void removeSession(SessionHistoryFolder session) { try {
     * getIMAPService().delete(session.getFullName(true)); } catch (Exception e) { // TODO
     * Auto-generated catch block e.printStackTrace(); } }
     */

    /*
     * @Override public boolean hasTemporaryMessages() throws MessageStoreException { try { String
     * temp = getFullName(false); IMAPFolderStatus st = getIMAPService().examine(temp); return (st
     * != null && st.getExists() > 0); } catch (Exception e) { throw new MessageStoreException(e); }
     * }
     */

}
