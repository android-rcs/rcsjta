/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.orangelabs.rcs.core.ims.service.im.chat;

import com.gsma.services.rcs.contacts.ContactId;

import java.util.Date;

public class ChatMessage {

    private final ContactId mRemote;

    private final String mDisplayName;

    private final String mContent;

    private final String mMimeType;

    /**
     * Receipt date of the message
     */
    private final Date mReceiptAt;

    /**
     * Receipt date of the message on the server (i.e. CPIM date)
     */
    private final Date mServerReceiptAt;

    private final String mMsgId;

    /**
     * Constructor for incoming message
     * 
     * @param msgId Message ID
     * @param remote Remote contact
     * @param content Text message
     * @param mimeType MIME type
     * @param serverReceiptAt Receipt date of the message on the server
     * @param displayName the name to display
     */
    public ChatMessage(String msgId, ContactId remote, String content, String mimeType, Date serverReceiptAt, String displayName) {
        mMsgId = msgId;
        mRemote = remote;
        mContent = content;
        mMimeType = mimeType;
        mReceiptAt = new Date();
        mServerReceiptAt = (serverReceiptAt != null ? serverReceiptAt : mReceiptAt);
        mDisplayName = displayName;
    }

    /**
     * Gets the message MIME-type
     * 
     * @return MIME-type
     */
    public String getMimeType() {
        return mMimeType;
    }

    /**
     * Gets the message content
     * 
     * @return message content
     */
    public String getContent() {
        return mContent;
    }

    /**
     * Gets the message ID
     * 
     * @return message ID
     */
    public String getMessageId() {
        return mMsgId;
    }

    /**
     * Gets the remote user
     * 
     * @return the remote contact
     */
    public ContactId getRemoteContact() {
        return mRemote;
    }

    /**
     * Gets the receipt date of the message
     * 
     * @return receipt date
     */
    public Date getDate() {
        return mReceiptAt;
    }

    /**
     * Gets the receipt date of the message on the server
     * 
     * @return server receipt date
     */
    public Date getServerDate() {
        return mServerReceiptAt;
    }

    /**
     * Gets the remote display name
     * 
     * @return remote display name
     */
    public String getDisplayName() {
        return mDisplayName;
    }

    @Override
    public String toString() {
        if (mContent != null && mContent.length() < 30) {
            return new StringBuilder("IM [from=").append(mRemote).append(", pseudo='")
                    .append(mDisplayName).append("', msg='").append(mContent).append("', msgId=")
                    .append(mMsgId).append("', mimeType='").append(mMimeType).append("']")
                    .append("]").toString();
        } else {
            return new StringBuilder("IM [from=").append(mRemote).append(", pseudo='")
                    .append(mDisplayName).append("', msgId=").append(mMsgId)
                    .append("', mimeType='").append(mMimeType).append("']").toString();
        }
    }
}
