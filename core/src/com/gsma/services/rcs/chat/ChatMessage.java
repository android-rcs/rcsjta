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

package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.contact.ContactId;

/**
 * Chat message
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
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
     * @return ID
     * @throws RcsServiceException
     */
    public String getId() throws RcsServiceException {
        try {
            return mChatMessageInf.getId();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the contact
     * 
     * @return ContactId
     * @throws RcsServiceException
     */
    public ContactId getRemoteContact() throws RcsServiceException {
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
     * @throws RcsServiceException
     */
    public String getContent() throws RcsServiceException {
        try {
            return mChatMessageInf.getContent();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the mime type of the chat message.
     * 
     * @return ContactId
     * @throws RcsServiceException
     */
    public String getMimeType() throws RcsServiceException {
        try {
            return mChatMessageInf.getMimeType();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the direction of message
     * 
     * @return Direction
     * @see Direction
     * @throws RcsServiceException
     */
    public Direction getDirection() throws RcsServiceException {
        try {
            return Direction.valueOf(mChatMessageInf.getDirection());
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the local time-stamp of when the chat message was sent and/or queued for outgoing
     * messages or the local time-stamp of when the chat message was received for incoming messages.
     * 
     * @return long
     * @throws RcsServiceException
     */
    public long getTimestamp() throws RcsServiceException {
        try {
            return mChatMessageInf.getTimestamp();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the local time-stamp of when the chat message was sent and/or queued for outgoing
     * messages or the remote time-stamp of when the chat message was sent for incoming messages.
     * 
     * @return long
     * @throws RcsServiceException
     */
    public long getTimestampSent() throws RcsServiceException {
        try {
            return mChatMessageInf.getTimestampSent();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the local timestamp of when the chat message was delivered for outgoing messages or 0
     * for incoming messages or it was not yet delivered.
     * 
     * @return long
     * @throws RcsServiceException
     */
    public long getTimestampDelivered() throws RcsServiceException {
        try {
            return mChatMessageInf.getTimestampDelivered();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the local timestamp of when the chat message was displayed for outgoing messages or 0
     * for incoming messages or it was not yes displayed.
     * 
     * @return long
     * @throws RcsServiceException
     */
    public long getTimestampDisplayed() throws RcsServiceException {
        try {
            return mChatMessageInf.getTimestampDisplayed();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the status of the chat message.
     * 
     * @return Status
     * @throws RcsServiceException
     */
    public Status getStatus() throws RcsServiceException {
        try {
            return Status.valueOf(mChatMessageInf.getStatus());
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the reason code of the chat message.
     * 
     * @return ReasonCode
     * @throws RcsServiceException
     */
    public ReasonCode getReasonCode() throws RcsServiceException {
        try {
            return ReasonCode.valueOf(mChatMessageInf.getReasonCode());
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the chat ID of this chat message.
     * 
     * @return String
     * @throws RcsServiceException
     */
    public String getChatId() throws RcsServiceException {
        try {
            return mChatMessageInf.getChatId();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns true is this chat message has been marked as read.
     * 
     * @return boolean
     * @throws RcsServiceException
     */
    public boolean isRead() throws RcsServiceException {
        try {
            return mChatMessageInf.isRead();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }
}
