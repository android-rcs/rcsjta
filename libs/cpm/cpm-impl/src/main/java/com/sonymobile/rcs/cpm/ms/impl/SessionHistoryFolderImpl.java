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
import com.sonymobile.rcs.cpm.ms.CpmGroupState;
import com.sonymobile.rcs.cpm.ms.CpmMessageStoreException;
import com.sonymobile.rcs.cpm.ms.CpmObject;
import com.sonymobile.rcs.cpm.ms.SessionHistoryFolder;
import com.sonymobile.rcs.cpm.ms.SessionInfo;
import com.sonymobile.rcs.imap.ImapException;
import com.sonymobile.rcs.imap.ImapFolder;
import com.sonymobile.rcs.imap.ImapMessage;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class SessionHistoryFolderImpl extends MessageFolderImpl implements SessionHistoryFolder {

    private SessionInfo mSessionInfo;

    protected SessionHistoryFolderImpl(ConversationHistoryFolderImpl parent, ImapFolder folder)
            throws CpmMessageStoreException {
        super(parent, folder);
        // info = findSessionInfoObject();
    }

    @Override
    protected void create() throws IOException, ImapException {
        super.create();
        select();
        // int uid = getImapService().append(getPath(), null, info.getBody());
        // getImapService().addFlags(uid, Flag.Seen);
    }

    /*
     * protected SessionHistoryFolderImpl(ConversationHistoryFolder parent, Date date, String from,
     * String contributionId, String subject, String inReplyToContributionId) throws
     * CPMMessageStoreException { super(parent, contributionId); info =
     * createSessionInfoObject(date, from, contributionId, subject, inReplyToContributionId); }
     */

    @SuppressWarnings("unused")
    private static ImapMessage createSessionInfoObject(Date date, String from,
            String contributionId, String subject, String inReplyToContributionId) {
        ImapMessage sessionInfoObject = new ImapMessage();
        sessionInfoObject.setContributionId(contributionId);
        sessionInfoObject.setSubject(subject);
        sessionInfoObject.setFrom(from);
        sessionInfoObject.setInReplyToContributionId(inReplyToContributionId);
        sessionInfoObject.setDate(date);

        sessionInfoObject.getBody().setContentType("Application/X-CPM-Session");
        // TODO SPECS!!!
        return sessionInfoObject;
    }

    @Override
    public String getFrom() throws CpmMessageStoreException {
        return getSessionInfoObject().getFrom();
    }

    @Override
    public String getSubject() throws CpmMessageStoreException {
        return getSessionInfoObject().getSubject();
    }

    @Override
    public String getId() {
        return getName();
    }

    @Override
    public String getInReplyToContributionId() {
        try {
            return getSessionInfoObject().getInReplyToContributionId();
        } catch (CpmMessageStoreException e) {
            return null;
        }
    }

    @Override
    public ConversationHistoryFolder getConversation() {
        return (ConversationHistoryFolder) getParent();
    }

    @Override
    public Set<CpmGroupState> getGroupStates() throws CpmMessageStoreException {
        try {
            Set<CpmGroupState> gsList = new HashSet<CpmGroupState>();

            Set<CpmObject> objects = getCpmObjects();
            for (CpmObject cpmObject : objects) {
                if (cpmObject instanceof CpmGroupState) {
                    gsList.add((CpmGroupState) cpmObject);
                }
            }

            return gsList;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CpmMessageStoreException("Cannot get group states in path: " + getPath(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<? extends FolderNode> getSubFolders() throws CpmMessageStoreException {
        return Collections.EMPTY_LIST;
    }

    @Override
    public SessionInfo getSessionInfoObject() throws CpmMessageStoreException {
        if (mSessionInfo != null)
            return mSessionInfo;

        try {
            Set<CpmObject> objects = getCpmObjects();
            for (CpmObject cpmObject : objects) {
                if (cpmObject instanceof SessionInfo) {
                    mSessionInfo = (SessionInfo) cpmObject;
                    break;
                }
            }
        } catch (Exception e) {
            throw new CpmMessageStoreException("Cannot get session info object in path: "
                    + getPath(), e);
        }
        if (mSessionInfo == null) {
            throw new CpmMessageStoreException("No session info object found for folder :"
                    + getPath());
        }
        return mSessionInfo;
    }

    // @Override
    // public CPMGroupState addGroupState(int type, Date timestamp,
    // String lastFocusSessionId, Participant... participants)
    // throws CPMMessageStoreException {
    // GroupStateImpl g = new GroupStateImpl(this, timestamp, lastFocusSessionId,
    // Arrays.asList(participants));
    // IMAPMessage msg = createIMAPMessage();
    // msg.getBody().setContentType(CPMGroupState.CONTENT_TYPE);
    // msg.setTextBody(g.toXml());
    // try {
    //
    // int uid = getImapService().append(getPath(), null, msg.getBody());
    // getImapService().noop();
    // msg.setUid(uid);
    //
    // return g;
    // } catch (Exception e) {
    // e.printStackTrace();
    // throw new CPMMessageStoreException(e);
    // }
    // }

}
