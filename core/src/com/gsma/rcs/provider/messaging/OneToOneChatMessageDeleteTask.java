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

package com.gsma.rcs.provider.messaging;

import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatSession;
import com.gsma.rcs.provider.DeleteTask;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Set;

public class OneToOneChatMessageDeleteTask extends DeleteTask.GroupedByContactId {

    private static final String SELECTION_ONETOONE_CHATMESSAGES = new StringBuilder(
            ChatLog.Message.CHAT_ID).append("=").append(ChatLog.Message.CONTACT).toString();

    private final ChatServiceImpl mChatService;

    private final InstantMessagingService mImService;

    /**
     * Deletion of all one to one chat messages.
     * 
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param imsLock the ims operation lock
     */
    public OneToOneChatMessageDeleteTask(ChatServiceImpl chatService,
            InstantMessagingService imService, LocalContentResolver contentResolver, Object imsLock) {
        super(contentResolver, imsLock, ChatLog.Message.CONTENT_URI, ChatLog.Message.MESSAGE_ID,
                ChatLog.Message.CONTACT, SELECTION_ONETOONE_CHATMESSAGES);
        mChatService = chatService;
        mImService = imService;
        setAllAtOnce(true);
    }

    /**
     * Deletion of a specific chat message.
     * 
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param imsLock the ims operation lock
     * @param messageId the message id
     */
    public OneToOneChatMessageDeleteTask(ChatServiceImpl chatService,
            InstantMessagingService imService, LocalContentResolver contentResolver,
            Object imsLock, String messageId) {
        super(contentResolver, imsLock, ChatLog.Message.CONTENT_URI, ChatLog.Message.MESSAGE_ID,
                ChatLog.Message.CONTACT, null, messageId);
        mChatService = chatService;
        mImService = imService;
    }

    /**
     * Deletion of a specific one to one conversation.
     * 
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param imsLock the ims operation lock
     * @param contact the contact
     */
    public OneToOneChatMessageDeleteTask(ChatServiceImpl chatService,
            InstantMessagingService imService, LocalContentResolver contentResolver,
            Object imsLock, ContactId contact) {
        super(contentResolver, imsLock, ChatLog.Message.CONTENT_URI, ChatLog.Message.MESSAGE_ID,
                ChatLog.Message.CONTACT, contact);
        mChatService = chatService;
        mImService = imService;
        setAllAtOnce(true);
    }

    @Override
    protected void onRowDelete(ContactId contact, String msgId) {
        if (isSingleRowDelete()) {
            return;

        }
        ChatSession session = mImService.getOneToOneChatSession(contact);
        if (session == null) {
            mChatService.removeOneToOneChat(contact);
            return;

        }
        session.deleteSession();
        mChatService.removeOneToOneChat(contact);
    }

    @Override
    protected void onCompleted(ContactId contact, Set<String> msgIds) {
        mChatService.broadcastOneToOneMessagesDeleted(contact, msgIds);
    }

}
