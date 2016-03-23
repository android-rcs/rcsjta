/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.contact.ContactId;

/**
 * Chat message
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class ChatMessage {

    private final IChatMessage mChatMessageInf;

    /**
     * Constructor
     * 
     * @param chatMessageInf IChatMessage
     */
    /* package private */ChatMessage(IChatMessage chatMessageInf) {
        mChatMessageInf = chatMessageInf;
    }

    /**
     * Returns the message ID
     * 
     * @return String
     * @throws RcsGenericException
     */
    public String getId() throws RcsGenericException {
        try {
            return mChatMessageInf.getId();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the contact
     * 
     * @return ContactId
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public ContactId getRemoteContact() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mChatMessageInf.getContact();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the message content
     * 
     * @return String
     * @throws RcsPersistentStorageException
     */
    public String getContent() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mChatMessageInf.getContent();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the mime type of the chat message.
     * 
     * @return String
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public String getMimeType() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mChatMessageInf.getMimeType();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the direction of message
     * 
     * @return Direction
     * @see Direction
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public Direction getDirection() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return Direction.valueOf(mChatMessageInf.getDirection());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the local time-stamp of when the chat message was sent and/or queued for outgoing
     * messages or the local time-stamp of when the chat message was received for incoming messages.
     * 
     * @return long
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public long getTimestamp() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mChatMessageInf.getTimestamp();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the local time-stamp of when the chat message was sent and/or queued for outgoing
     * messages or the remote time-stamp of when the chat message was sent for incoming messages.
     * 
     * @return long
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public long getTimestampSent() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mChatMessageInf.getTimestampSent();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the local timestamp of when the chat message was delivered for outgoing messages or 0
     * for incoming messages or it was not yet delivered.
     * 
     * @return long
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public long getTimestampDelivered() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mChatMessageInf.getTimestampDelivered();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the local timestamp of when the chat message was displayed for outgoing messages or 0
     * for incoming messages or it was not yes displayed.
     * 
     * @return long
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public long getTimestampDisplayed() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mChatMessageInf.getTimestampDisplayed();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the status of the chat message.
     * 
     * @return Status
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public Status getStatus() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return Status.valueOf(mChatMessageInf.getStatus());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the reason code of the chat message.
     * 
     * @return ReasonCode
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public ReasonCode getReasonCode() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return ReasonCode.valueOf(mChatMessageInf.getReasonCode());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the chat ID of this chat message.
     * 
     * @return String
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public String getChatId() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mChatMessageInf.getChatId();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true is this chat message has been marked as read.
     * 
     * @return boolean
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public boolean isRead() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mChatMessageInf.isRead();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if delivery for this chat message has expired or false otherwise. Note: false
     * means either that delivery for this chat message has not yet expired, delivery has been
     * successful, delivery expiration has been cleared (see clearMessageDeliveryExpiration) or that
     * this particular chat message is not eligible for delivery expiration in the first place.
     * 
     * @return boolean
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public boolean isExpiredDelivery() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mChatMessageInf.isExpiredDelivery();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }
}
