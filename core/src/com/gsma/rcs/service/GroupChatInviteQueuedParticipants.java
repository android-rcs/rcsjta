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
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.GroupChatImpl;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/* package private */class GroupChatInviteQueuedParticipants implements Runnable {

    private static final Set<ParticipantStatus> INVITE_QUEUED_STATUSES = new HashSet<ParticipantStatus>();
    static {
        INVITE_QUEUED_STATUSES.add(ParticipantStatus.INVITE_QUEUED);
    }

    private final String mChatId;

    private final ChatServiceImpl mChatService;

    private final InstantMessagingService mImService;

    private final Logger mLogger = Logger.getLogger(getClass().getName());

    /* package private */GroupChatInviteQueuedParticipants(String chatId,
            ChatServiceImpl chatService, InstantMessagingService imService) {
        mChatId = chatId;
        mChatService = chatService;
        mImService = imService;
    }

    @Override
    public void run() {
        try {
            GroupChatImpl groupChat = mChatService.getOrCreateGroupChat(mChatId);
            final Set<ContactId> participantsToBeInvited = groupChat.getParticipants(
                    INVITE_QUEUED_STATUSES).keySet();
            if (participantsToBeInvited == null || participantsToBeInvited.size() == 0) {
                return;
            }

            final GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session != null && session.isMediaEstablished()) {
                if (mLogger.isActivated()) {
                    mLogger.debug(new StringBuilder("Adding ")
                            .append(Arrays.toString(participantsToBeInvited.toArray()))
                            .append(" to the group chat session ").append(mChatId).append(".")
                            .toString());
                }

                if (session.getMaxNumberOfAdditionalParticipants() < participantsToBeInvited.size()) {
                    for (ContactId contact : participantsToBeInvited) {
                        groupChat.handleAddParticipantFailed(contact,
                                "Maximum number of participants reached");
                    }
                    return;
                }

                new Thread() {
                    public void run() {
                        session.inviteParticipants(participantsToBeInvited);
                    }
                }.start();
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
