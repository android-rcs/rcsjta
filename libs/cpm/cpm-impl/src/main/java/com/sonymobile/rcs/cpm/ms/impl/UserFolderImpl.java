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
import com.sonymobile.rcs.cpm.ms.CpmChatMessage;
import com.sonymobile.rcs.cpm.ms.CpmFileTransfer;
import com.sonymobile.rcs.cpm.ms.CpmMediaObject;
import com.sonymobile.rcs.cpm.ms.CpmMessage;
import com.sonymobile.rcs.cpm.ms.CpmMessageStoreException;
import com.sonymobile.rcs.cpm.ms.FileItem;
import com.sonymobile.rcs.cpm.ms.Participant;
import com.sonymobile.rcs.cpm.ms.UserFolder;
import com.sonymobile.rcs.imap.ImapFolder;
import com.sonymobile.rcs.imap.ImapMessage;
import com.sonymobile.rcs.imap.Search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserFolderImpl extends MessageFolderImpl implements UserFolder {

    public UserFolderImpl(FolderNode parent, ImapFolder folder) throws CpmMessageStoreException {
        super(parent, folder);
    }

    @Override
    public UserFolder createSubFolder(String folderName) throws CpmMessageStoreException {
        UserFolderImpl uf = null;
        try {
            uf = new UserFolderImpl(this, getImapFolder().getSubFolder(folderName));
            uf.create();
        } catch (Exception e) {
            throw new CpmMessageStoreException("", e);
        }
        return uf;
    }

    @Override
    public Set<UserFolder> getUserFolders() throws CpmMessageStoreException {
        return null;
    }

    @Override
    public void copy(CpmMessage message) throws CpmMessageStoreException {
        // AbstractCpmMessage obj = (AbstractCpmMessage)message;
        try {
            // getImapService().select(message.getFolder().getPath());
            // getImapService().copy(obj.getUid(), getPath());
        } catch (Exception e) {
            throw new CpmMessageStoreException("", e);
        }
    }

    @Override
    public void copy(ConversationHistoryFolder conversation) throws CpmMessageStoreException {
        try {
            getImapService().select(getPath());
            int[] ids = getImapService().searchMessages(new Search().all());
            int first = ids[0];
            int last = ids[ids.length - 1];
            getImapService().copy(first + ":" + last, getPath());
        } catch (Exception e) {
            throw new CpmMessageStoreException("", e);
        }
    }

    @Override
    public void remove(CpmMessage message) throws CpmMessageStoreException {
        AbstractCpmMessage msg = (AbstractCpmMessage) message;
        msg.setDeleted(true);
    }

    @Override
    public CpmFileTransfer addFileTransfer(String messageId, int type,
            List<Participant> participants, FileItem... files) throws CpmMessageStoreException {
        ImapMessage msg = createIMAPMessage();
        msg.setFrom(Participant.asString(participants));
        msg.setContentType("Application/X-CPM-File-Transfer");
        msg.setContentDisposition("render");
        msg.setMessageId(messageId); // TODO verify it s there
        msg.getBody().setContent(
                CpmFileTransferImpl.toXml(type, participants, Arrays.asList(files)));
        try {
            int uid = getImapService().append(getPath(), null, msg.getBody());
            getImapService().noop();
            msg.setUid(uid);
            return new CpmFileTransferImpl(msg, this);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CpmMessageStoreException("", e);
        }
    }

    @Override
    public CpmMediaObject addMediaObject(String messageId, Participant from, String contentType,
            byte[] data) throws CpmMessageStoreException {
        ImapMessage msg = createIMAPMessage();
        msg.getBody().setContent(data);
        msg.getBody().setContentType(contentType);
        msg.setMessageId(messageId); // TODO check id meesage id is for media object
        msg.setFrom(from.toString());
        msg.setContentTransferEncoding("base64");
        msg.setContentDisposition("attachment");

        try {
            int uid = getImapService().append(getPath(), null, msg.getBody());
            getImapService().noop();
            msg.setUid(uid);
            return new RemoteMediaObject(msg, this);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CpmMessageStoreException("", e);
        }
    }

    @Override
    public CpmChatMessage addChatMessage(String messageId, Participant from, String text)
            throws CpmMessageStoreException {
        ImapMessage msg = createIMAPMessage();
        msg.setTextBody(text);
        msg.setMessageId(messageId);
        msg.setFrom(from.toString());

        try {
            int uid = getImapService().append(getPath(), null, msg.getBody());
            getImapService().noop();
            msg.setUid(uid);
            return new CpmChatMessageImpl(msg, this);
        } catch (Exception e) {
            throw new CpmMessageStoreException("", e);
        }
    }

    @Override
    public Set<ConversationHistoryFolder> getConversations() throws CpmMessageStoreException {
        try {

            List<ImapFolder> li = getImapService().getFolders(getPath(), false);

            Set<ConversationHistoryFolder> conversations = new HashSet<ConversationHistoryFolder>();

            for (ImapFolder f : li) {
                conversations.add(new ConversationHistoryFolderImpl(this, f));
            }

            return conversations;
        } catch (Exception e) {
            throw new CpmMessageStoreException("", e);
        }
    }

}
