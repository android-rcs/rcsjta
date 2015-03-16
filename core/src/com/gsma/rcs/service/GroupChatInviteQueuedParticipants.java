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

package com.gsma.rcs.service;

import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSession;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.GroupChatImpl;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Set;

/* package private */class GroupChatInviteQueuedParticipants implements Runnable {

    private final String mChatId;

    private final ChatServiceImpl mChatService;

    private final MessagingLog mMessagingLog;

    private final InstantMessagingService mImService;

    /* package private */GroupChatInviteQueuedParticipants(String chatId,
            ChatServiceImpl chatService, MessagingLog messagingLog,
            InstantMessagingService imService) {
        mChatId = chatId;
        mChatService = chatService;
        mMessagingLog = messagingLog;
        mImService = imService;
    }

    @Override
    public void run() {
        GroupChatImpl groupChat = mChatService.getOrCreateGroupChat(mChatId);
        Set<ContactId> participants = mMessagingLog
                .getGroupChatParticipantsToBeInvited(mChatId);
        GroupChatSession session = mImService.getGroupChatSession(mChatId);
        if (session != null && session.isMediaEstablished()) {
            groupChat.inviteParticipants(session, participants);
        }
    }
}
