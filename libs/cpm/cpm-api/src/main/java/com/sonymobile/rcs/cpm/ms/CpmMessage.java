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

package com.sonymobile.rcs.cpm.ms;

/**
 * Represents an abstract CPM message
 */
public interface CpmMessage extends CpmObject {

    public long getDate();

    public String getMessageId();

    public String getContentType();

    public String getContent();

    /**
     * Returns the From header
     * 
     * @return from
     */
    public Participant getFrom();

    /*
     * NEVER USED The definition and identity specification of the message object SHALL be
     * applicable to the file transfer history object, which consists of a UID, UIDVALIDITY and
     * their values.
     * @return
     */
    // public String getCPMId();

    // SYSTEM FLAGS

    /**
     * Seen: Message has been read.
     * 
     * @return true if the message is Seen
     */
    public boolean isSeen();

    /**
     * Sets the message as Seen
     * 
     * @param seen
     * @throws MessageStoreException
     */
    public void setSeen(boolean seen) throws CpmMessageStoreException;

    /**
     * Deleted: Message is "deleted" for later removal with IMAP_Delete, IMAP_CloseCurrentMB,
     * IMAP_SetCurrentMB or IMAP_Logout.
     * 
     * @return true if deleted
     */
    public boolean isDeleted();

    /**
     * TODO : use remove instead ?
     * 
     * @param deleted
     */
    public void setDeleted(boolean deleted) throws CpmMessageStoreException;

    /**
     * Recent: Message "recently" arrived in this mailbox. This session is the first session
     * notified about this message; subsequent sessions will not see the \Recent flag set for this
     * message. This permanent flag is managed by the IMAP server and cannot be modified by an IMAP
     * client using IMAP_SetFlags, for instance.
     * 
     * @return true if recent
     */
    public boolean isRecent();

    /**
     * Draft: Message is in draft format; in other words, not complete.
     * 
     * @return true if the message is Draft
     */
    public boolean isDraft();

    /**
     * Sets the message as draft
     * 
     * @param draft
     * @throws MessageStoreException
     */
    public void setDraft(boolean draft) throws CpmMessageStoreException;

    /**
     * Answered: Message has been answered.
     * 
     * @return
     */
    public boolean isAnswered();

    /**
     * @param answered
     * @throws MessageStoreException
     */
    public void setAnswered(boolean answered) throws CpmMessageStoreException;

    /**
     * Flagged: Message is "flagged" for urgent/special attention.
     * 
     * @return true if the message is flagged
     */
    public boolean isFlagged();

    /**
     * Sets the message as Flagged
     * 
     * @param flagged
     * @throws MessageStoreException
     */
    public void setFlagged(boolean flagged) throws CpmMessageStoreException;

    // CPM FLag
    /**
     * Appendix C. CPM-defined IMAP Flag Extensions C.1 \read-report-sent \ read-report-sent is a
     * CPM-defined IMAP flag extension associated with a Message Object stored in Message Storage
     * Server. The definition of the flag is: \read-report-sent Read report is sent Formal syntax of
     * \read-report-sent is: flag-extension = "\read-report-sent" Example: C: A003 STORE 2:4 +FLAGS
     * (\read-report-sent) \ read-report-sent status flag is visible to the Message Storage Client
     * but is masked to the CPM User. This flag is used to record whether a read report has been
     * sent or not for a message object in the Message Storage Server.
     */
    public boolean isReadReportSent();

    /**
     * Returns the MDNSent flag
     * 
     * @return true if MDN is sent
     */
    public boolean isMDNSent();

    /**
     * Sets the MDNSent metadata
     * 
     * @param MDNSent
     */
    public void setMDNSent(boolean MDNSent);

    /**
     * Returns the Forwarded flag
     * 
     * @return true id the message has been forwarded
     */
    public boolean isForwarded();

    /**
     * @param forwarded
     */
    public void setForwarded(boolean forwarded);

}
