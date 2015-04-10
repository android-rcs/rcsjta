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
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSession;
import com.gsma.rcs.provider.DeleteTask;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.services.rcs.chat.ChatLog;

import java.util.Set;

/**
 * Deletion task for group chats.
 */
public class GroupChatDeleteTask extends DeleteTask.NotGrouped {

    private static final String SELECTION_GROUPDELIVERY_BY_CHATID = new StringBuilder(
            GroupDeliveryInfoData.KEY_CHAT_ID).append("=?").toString();

    private final ChatServiceImpl mChatService;

    private final InstantMessagingService mImService;

    /**
     * Deletion of all group chats.
     * 
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param imsLock the ims operation lock
     */
    public GroupChatDeleteTask(ChatServiceImpl chatService, InstantMessagingService imService,
            LocalContentResolver contentResolver, Object imsLock) {
        super(contentResolver, imsLock, ChatData.CONTENT_URI, ChatLog.GroupChat.CHAT_ID, null);
        mChatService = chatService;
        mImService = imService;
    }

    /**
     * Deletion of a specific group chat.
     * 
     * @param chatService the chat service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param imsLock the ims operation lock
     * @param chatId the group chat id
     */
    public GroupChatDeleteTask(ChatServiceImpl chatService, InstantMessagingService imService,
            LocalContentResolver contentResolver, Object imsLock, String chatId) {
        super(contentResolver, imsLock, ChatData.CONTENT_URI, ChatLog.GroupChat.CHAT_ID, null,
                chatId);
        mChatService = chatService;
        mImService = imService;
    }

    @Override
    protected void onRowDelete(String chatId) {
        GroupChatSession session = mImService.getGroupChatSession(chatId);
        if (session == null) {
            mLocalContentResolver.delete(GroupDeliveryInfoData.CONTENT_URI,
                    SELECTION_GROUPDELIVERY_BY_CHATID, new String[] {
                        chatId
                    });
            return;

        }
        session.deleteSession();
        mLocalContentResolver.delete(GroupDeliveryInfoData.CONTENT_URI,
                SELECTION_GROUPDELIVERY_BY_CHATID, new String[] {
                    chatId
                });
    }

    @Override
    protected void onCompleted(Set<String> deletedIds) {
        mChatService.broadcastGroupChatsDeleted(deletedIds);
    }

}
