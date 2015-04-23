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

import java.util.Set;

public class GroupChatMessageDeleteTask extends DeleteTask.GroupedByChatId {

    private static final String SELECTION_GROUP_CHATMESSAGES = new StringBuilder(
            MessageData.KEY_CHAT_ID).append("<>").append(MessageData.KEY_CONTACT).toString();
    private static final String SELECTION_CHATMESSAGES_BY_CHATID = new StringBuilder(
            MessageData.KEY_CHAT_ID).append("=?").toString();

    private final ChatServiceImpl mChatService;

    private final InstantMessagingService mImService;

    /**
     * Deletion of all group chat messages.
     * 
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param imsLock the ims operation lock
     */
    public GroupChatMessageDeleteTask(ChatServiceImpl chatService,
            InstantMessagingService imService, LocalContentResolver contentResolver, Object imsLock) {
        super(contentResolver, imsLock, MessageData.CONTENT_URI, MessageData.KEY_MESSAGE_ID,
                MessageData.KEY_CHAT_ID, SELECTION_GROUP_CHATMESSAGES);
        mChatService = chatService;
        mImService = imService;
    }

    /**
     * Deletion of all chat messages from the specified group chat id.
     * 
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param imsLock the ims operation lock
     * @param chatId the chat id
     */
    public GroupChatMessageDeleteTask(ChatServiceImpl chatService,
            InstantMessagingService imService, LocalContentResolver contentResolver,
            Object imsLock, String chatId) {
        super(contentResolver, imsLock, MessageData.CONTENT_URI, MessageData.KEY_MESSAGE_ID,
                GroupChatData.KEY_CHAT_ID, SELECTION_CHATMESSAGES_BY_CHATID, chatId);
        mChatService = chatService;
        mImService = imService;
    }

    /**
     * Deletion of a specific message.
     * 
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param imsLock the ims operation lock
     * @param chatId the chat id (optional, can be null)
     * @param messageId the message id
     */
    public GroupChatMessageDeleteTask(ChatServiceImpl chatService,
            InstantMessagingService imService, LocalContentResolver contentResolver,
            Object imsLock, String chatId, String messageId) {
        super(contentResolver, imsLock, GroupChatData.CONTENT_URI, MessageData.KEY_MESSAGE_ID,
                GroupChatData.KEY_CHAT_ID, null, messageId);
        mChatService = chatService;
        mImService = imService;
    }

    @Override
    protected void onRowDelete(String chatId, String msgId) {
        if (isSingleRowDelete()) {
            return;

        }
        ChatSession session = mImService.getGroupChatSession(chatId);
        if (session == null) {
            mChatService.removeGroupChat(chatId);
            return;

        }
        session.deleteSession();
        mChatService.removeGroupChat(chatId);
    }

    @Override
    protected void onCompleted(String chatId, Set<String> msgIds) {
        mChatService.broadcastGroupChatMessagesDeleted(chatId, msgIds);
    }

}
