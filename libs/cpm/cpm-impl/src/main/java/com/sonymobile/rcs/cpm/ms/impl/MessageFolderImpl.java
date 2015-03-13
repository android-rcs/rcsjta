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
import com.sonymobile.rcs.cpm.ms.CpmFolderStatus;
import com.sonymobile.rcs.cpm.ms.CpmGroupState;
import com.sonymobile.rcs.cpm.ms.CpmMessageStoreException;
import com.sonymobile.rcs.cpm.ms.CpmObject;
import com.sonymobile.rcs.cpm.ms.CpmObjectFolder;
import com.sonymobile.rcs.cpm.ms.CpmObjectMetadata;
import com.sonymobile.rcs.cpm.ms.SessionHistoryFolder;
import com.sonymobile.rcs.imap.ImapException;
import com.sonymobile.rcs.imap.ImapFolder;
import com.sonymobile.rcs.imap.ImapFolderStatus;
import com.sonymobile.rcs.imap.ImapMessage;
import com.sonymobile.rcs.imap.ImapMessageMetadata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class MessageFolderImpl extends BasicFolderNode implements CpmObjectFolder {

    private ImapFolderStatus mLastStatus;

    private List<CpmObject> mCpmObjectsCache = null;

    protected MessageFolderImpl(FolderNode parent, ImapFolder folder)
            throws CpmMessageStoreException {
        super(parent, folder);
    }

    @Override
    public int getVersion() {
        // TODO !!! Auto-generated method stub
        return 0;
    }

    public void clearCache() {
        this.mCpmObjectsCache = null;
    }

    @Override
    public void setListening(boolean listening) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public boolean isListening() {
        throw new RuntimeException("Not Implemented");
    }

    protected void create() throws IOException, ImapException {
        getImapService().create(getPath());
    }

    protected void select() throws IOException, ImapException {
        mLastStatus = getImapService().select(getPath());
    }

    @Override
    public String getMetadata() throws CpmMessageStoreException {
        try {
            return getImapService().getFolderMetadata(getPath());
        } catch (Exception e) {
            e.printStackTrace();
            throw new CpmMessageStoreException("Read metadata failed on folder:" + getPath(), e);
        }
    }

    @Override
    public void setMetadata(String comment) throws CpmMessageStoreException {
        try {
            getImapService().setFolderMetadata(getPath(), comment);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CpmMessageStoreException("Write metadata failed on folder:" + getPath(), e);
        }
    }

    @Override
    public Set<CpmObjectMetadata> getObjectMetadata() throws CpmMessageStoreException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CpmFolderStatus getStatus() throws CpmMessageStoreException {
        if (mLastStatus == null) {
            refreshStatus();
        }
        return new FolderStatusImpl(this);
    }

    public int getUidvalidity() {
        if (mLastStatus != null) {
            return mLastStatus.getUidValidity();
        }
        return -1;
    }

    public void refreshStatus() throws CpmMessageStoreException {
        try {
            mLastStatus = getImapService().getFolderStatus(getPath());
        } catch (Exception e) {
            e.printStackTrace();
            throw new CpmMessageStoreException("Read status failed on folder:" + getPath(), e);
        }
    }

    @Override
    public synchronized Set<CpmObject> getCpmObjects() throws CpmMessageStoreException {

        if (mCpmObjectsCache != null) {
            return new HashSet<CpmObject>(mCpmObjectsCache);
        }

        try {

            mCpmObjectsCache = new ArrayList<CpmObject>();

            select();

            if (this.mLastStatus.getExists() == 0) {
                return new HashSet<CpmObject>(mCpmObjectsCache);
            }

            List<ImapMessage> imapMsgList = getImapService().fetchMessages("1:*");
            // get metadata

            List<ImapMessageMetadata> allMetadata = getImapService()
                    .fetchMessageMetadataList("1:*");

            // for metadata remap
            Map<Integer, ImapMessageMetadata> metadataMap = new HashMap<Integer, ImapMessageMetadata>();
            for (ImapMessageMetadata imapMessageMetadata : allMetadata) {
                metadataMap.put(imapMessageMetadata.getUid(), imapMessageMetadata);
            }

            for (ImapMessage imapMessage : imapMsgList) {
                imapMessage.setMetadata(metadataMap.get(imapMessage.getUid()));

                CpmObject cpmObj = null;

                try {
                    cpmObj = asCpmObject(imapMessage);
                } catch (Exception e) {
                    throw new CpmMessageStoreException("Parsing exception", e);
                }

                if (cpmObj != null) {
                    mCpmObjectsCache.add(cpmObj);
                }
            }

            return new HashSet<CpmObject>(mCpmObjectsCache);

        } catch (Exception e) {
            throw new CpmMessageStoreException("Reading objects failed on folder:" + getPath(), e);
        }
    }

    private CpmObject asCpmObject(ImapMessage msg) throws Exception {
        if (msg.getBody() == null)
            return null;
        String contentType = msg.getBody().getContentType();

        if (contentType == null) {
            System.err.println("Payload invalid : " + msg.getBody().toPayload());
            return null;
        } else if (contentType.contains(CpmGroupState.CONTENT_TYPE) && isInSession()) {
            return GroupStateImpl.fromXml(msg.getUid(), (SessionHistoryFolder) this, msg.getBody()
                    .getContent());
        } else if (contentType.contains("Application/X-CPM-Session") && isInSession()) {
            return new SessionInfoImpl(msg, this);
        } else if (contentType.contains("Application/X-CPM-File-Transfer")) {
            return new CpmFileTransferImpl(msg, this);
        } else if (contentType.startsWith("text/plain")) {
            return new CpmChatMessageImpl(msg, this);
        } else {
            return new RemoteMediaObject(msg, this);
        }
    }

    @Override
    public CpmObject getObjectByStorageId(int id) throws CpmMessageStoreException {
        Set<CpmObject> objects = getCpmObjects();
        for (CpmObject cpmObject : objects) {
            if (cpmObject.getStorageId() == id) {
                return cpmObject;
            }
        }
        return null;
    }

    protected ImapMessage createIMAPMessage() throws CpmMessageStoreException {
        ImapMessage msg = new ImapMessage();
        if (isInSession()) {
            msg.setContributionId(((SessionHistoryFolder) this).getId());
            msg.setConversationId(getParent().getName());
            msg.setSubject(((SessionHistoryFolder) this).getSubject());
        } else if (isStandalone()) {
            msg.setConversationId(getName());
        }

        return msg;
    }

    protected boolean isInSession() {
        return this instanceof SessionHistoryFolder;
    }

    protected boolean isStandalone() {
        return this instanceof ConversationHistoryFolder;
    }

    class FolderStatusImpl implements CpmFolderStatus {

        private long time;

        public FolderStatusImpl(MessageFolderImpl messageFolderImpl) {
            this.time = System.currentTimeMillis();
        }

        @Override
        public int getId() {
            return getUidvalidity();
        }

        @Override
        public long getTime() {
            return time;
        }

        @Override
        public int getMessageCount() {
            return mLastStatus.getExists();
        }

        @Override
        public int getRecent() {
            return mLastStatus.getRecent();
        }

        @Override
        public int getUnseen() {
            return mLastStatus.getUnseen();
        }
    }
}
