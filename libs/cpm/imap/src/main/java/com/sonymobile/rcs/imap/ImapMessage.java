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

package com.sonymobile.rcs.imap;

import java.util.Date;

/**
 * Represents a raw imap message
 */
public class ImapMessage {

    private int mUid = -1;

    private Part mBody = new Part();

    private ImapMessageMetadata mMetadata;

    private String mFolderPath;

    public ImapMessage() {
    }

    public ImapMessage(int uid, ImapMessageMetadata metadata, Part body) {
        super();
        mUid = uid;
        mMetadata = metadata;
        mBody = body;
    }

    public void setFolderPath(String folderPath) {
        mFolderPath = folderPath;
    }

    public String getFolderPath() {
        return mFolderPath;
    }

    public void setMetadata(ImapMessageMetadata metadata) {
        this.mMetadata = metadata;
    }

    public ImapMessageMetadata getMetadata() {
        return mMetadata;
    }

    public void setTextBody(String textBody) {
        this.mBody.setContent(textBody);
    }

    public String getTextBody() {
        return mBody.getContent();
    }

    private String getHeaderValue(String key) {
        return mBody.getHeader(key);
    }

    private void setHeaderValue(String key, String value) {
        mBody.setHeader(key, value);
    }

    public String getPayload() {
        return mBody.toPayload();
    }

    public void fromPayload(String payload) {
        mBody.fromPayload(payload);
    }

    public Part getBody() {
        return mBody;
    }

    @Override
    public String toString() {
        return "IMAPSimpleMessage[uid=" + mUid + "]";
    }

    public void setUid(int uid) {
        this.mUid = uid;
    }

    public int getUid() {
        return mUid;
    }

    public void setFrom(String from) {
        setHeaderValue("From", from);
    }

    public String getFrom() {
        String from = getHeaderValue("From");
        if (from.startsWith("<"))
            from = from.substring(1);
        if (from.endsWith(">"))
            from = from.substring(0, from.length() - 1);
        if (from.startsWith("sip:")) {
            from = from.substring(4);
        }

        return from;
    }

    public String getMessageId() {
        return getHeaderValue("Message-ID");
    }

    public void setMessageId(String id) {
        setHeaderValue("Message-ID", id);
    }

    public void setContentType(String string) {
        setHeaderValue("Content-Type", string);
    }

    public String getContentType() {
        return getHeaderValue("Content-Type");
    }

    public void setContentDisposition(String string) {
        setHeaderValue("Content-Disposition", string);
    }

    public String getContentDisposition() {
        return getHeaderValue("Content-Disposition");
    }

    public void setContentTransferEncoding(String string) {
        setHeaderValue("Content-Transfer-Encoding", string);
    }

    public void setContributionId(String sessid) {
        setHeaderValue("Contribution-ID", sessid);
    }

    public void setConversationId(String cid) {
        setHeaderValue("Conversation-ID", cid);
    }

    public void setSubject(String subject) {
        setHeaderValue("Subject", subject);
    }

    public String getSubject() {
        return getHeaderValue("Subject");
    }

    public void setInReplyToContributionId(String inReplyToContributionId) {
        setHeaderValue("In-Reply-To-Contribution-ID", inReplyToContributionId);
    }

    public String getInReplyToContributionId() {
        return getHeaderValue("In-Reply-To-Contribution-ID");
    }

    public void setDate(String d) {
        setHeaderValue("Date", d);
    }

    public String getDate() {
        return getHeaderValue("Date");
    }

    public void setDate(Date date) {
        mBody.setDate(date);
    }

    public long getDateAsDate() {
        return mBody.getDate().getTime();
    }

}
