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

import com.sonymobile.rcs.cpm.ms.CpmMessage;
import com.sonymobile.rcs.cpm.ms.CpmMessageStoreException;
import com.sonymobile.rcs.cpm.ms.Participant;
import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapMessage;
import com.sonymobile.rcs.imap.ImapMessageMetadata;
import com.sonymobile.rcs.imap.ImapMessageStatus;

public abstract class AbstractCpmMessage extends AbstractCpmObject implements CpmMessage {

    private ImapMessageStatus status = ImapMessageStatus.EMPTY;

    public AbstractCpmMessage(ImapMessage msg, MessageFolderImpl folder) {
        super(msg, folder);
        if (msg.getMetadata() != null) {
            ImapMessageMetadata md = msg.getMetadata();
            status = md.asStatus();
        }
    }

    @Override
    public String getContent() {
        return getImapMessage().getBody().getContent();
    }

    @Override
    public String getMessageId() {
        return getImapMessage().getMessageId();
    }

    @Override
    public int getStorageId() {
        return getImapMessage().getUid();
    }

    @Override
    public String getContentType() {
        return getImapMessage().getContentType();
    }

    @Override
    public long getDate() {
        return getImapMessage().getDateAsDate();
    }

    /*
     * protected String retrievePayload() throws IOException, IMAPException{ if
     * (imapMessage.getPayload() == null){ selectFolder(); Part p =
     * getIMAPService().fetchMessageBody(imapMessage.getUid()); imapMessage.getBody().replace(p); }
     * return imapMessage.getPayload(); }
     */

    /*
     * protected synchronized void refreshStatus() throws CPMMessageStoreException { try {
     * selectFolder(); IMAPMessageMetadata m =
     * getIMAPService().fetchMessageMetadata(imapMessage.getUid(), false, false); status =
     * m.asStatus(); } catch (Exception e) { e.printStackTrace(); throw new
     * CPMMessageStoreException(e); } }
     */

    @Override
    public Participant getFrom() {
        return Participant.parseString(getImapMessage().getFrom());
    }

    private synchronized void setFlag(boolean val, Flag f) throws CpmMessageStoreException {
        try {
            if (val) {
                getImapService().addFlags(getImapMessage().getUid(), f);
            } else {
                getImapService().removeFlags(getImapMessage().getUid(), f);
            }
            // refreshStatus();
        } catch (Exception e) {
            e.printStackTrace();
            throw new CpmMessageStoreException("", e);
        }
    }

    @Override
    public boolean isSeen() {
        return status.isSeen();
    }

    @Override
    public void setSeen(boolean seen) throws CpmMessageStoreException {
        setFlag(seen, Flag.Seen);
    }

    @Override
    public boolean isDeleted() {
        return status.isDeleted();
    }

    @Override
    public void setDeleted(boolean deleted) throws CpmMessageStoreException {
        setFlag(deleted, Flag.Deleted);
    }

    @Override
    public boolean isRecent() {
        return status.isRecent();
    }

    @Override
    public boolean isDraft() {
        return status.isDraft();
    }

    @Override
    public void setDraft(boolean draft) throws CpmMessageStoreException {
        setFlag(draft, Flag.Draft);
    }

    @Override
    public boolean isAnswered() {
        return status.isAnswered();
    }

    @Override
    public void setAnswered(boolean answered) throws CpmMessageStoreException {
        setFlag(answered, Flag.Answered);
    }

    @Override
    public boolean isFlagged() {
        return status.isFlagged();
    }

    @Override
    public void setFlagged(boolean flagged) throws CpmMessageStoreException {
        setFlag(flagged, Flag.Flagged);
    }

    @Override
    public boolean isReadReportSent() {
        return status.isReadReportSent();
    }

    // TODO Unused flags
    @Override
    public boolean isForwarded() {
        return false;
    }

    @Override
    public boolean isMDNSent() {
        return false;
    }

    @Override
    public void setForwarded(boolean forwarded) {
    }

    @Override
    public void setMDNSent(boolean MDNSent) {
    }

}
