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
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Arrays;
import java.util.Set;

/* package private */class GroupChatInviteQueuedParticipants implements Runnable {

    private final String mChatId;

    private final ChatServiceImpl mChatService;

    private final MessagingLog mMessagingLog;

    private final InstantMessagingService mImService;

    private final Logger mLogger = Logger.getLogger(getClass().getName());

    /* package private */GroupChatInviteQueuedParticipants(String chatId,
            ChatServiceImpl chatService, MessagingLog messagingLog,
            InstantMessagingService imService) {
        mChatId = chatId;
        mChatService = chatService;
        mMessagingLog = messagingLog;
        mImService = imService;
    }

    private void inviteQueuedParticipants(final GroupChatSession session,
            final Set<ContactId> participants) {
        if (mLogger.isActivated()) {
            mLogger.debug(new StringBuilder("Adding ")
                    .append(Arrays.toString(participants.toArray())).append(" to the session.")
                    .toString());
        }

        if (session.getMaxNumberOfAdditionalParticipants() < participants.size()) {
            GroupChatImpl groupChat = mChatService.getOrCreateGroupChat(mChatId);

            for (ContactId contact : participants) {
                groupChat.handleAddParticipantFailed(contact,
                        "Maximum number of participants reached");
            }
            return;
        }
        new Thread() {
            public void run() {
                session.inviteParticipants(participants);
            }
        }.start();
    }

    @Override
    public void run() {
        try {
            Set<ContactId> participants = mMessagingLog
                    .getGroupChatParticipantsToBeInvited(mChatId);

            if (participants.size() == 0) {
                return;
            }

            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session != null && session.isMediaEstablished()) {
                inviteQueuedParticipants(session, participants);
            }
        } catch (Exception e) {
            /*
             * Exception will be handled better in CR037.
             */
            if (mLogger.isActivated()) {
                mLogger.error(
                        "Exception occured while trying to invite queued participants to group chat with chatId "
                                .concat(mChatId), e);
            }
        }
    }
}
