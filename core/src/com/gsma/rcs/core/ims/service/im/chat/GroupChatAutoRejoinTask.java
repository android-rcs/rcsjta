/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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

package com.gsma.rcs.core.ims.service.im.chat;

import com.gsma.rcs.core.ims.protocol.sip.SipNetworkException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.utils.logger.Logger;

public class GroupChatAutoRejoinTask implements Runnable {

    private final MessagingLog mMessagingLog;

    private final InstantMessagingService mImService;

    private static final Logger sLogger = Logger.getLogger(GroupChatAutoRejoinTask.class.getName());

    public GroupChatAutoRejoinTask(InstantMessagingService imService, MessagingLog messagingLog) {
        mMessagingLog = messagingLog;
        mImService = imService;
    }

    @Override
    public void run() {
        for (String chatId : mMessagingLog.getChatIdsOfActiveGroupChatsForAutoRejoin()) {
            try {
                mImService.rejoinGroupChat(chatId);

            } catch (SipPayloadException e) {
                sLogger.error(new StringBuilder("Could not auto-rejoin group chat with chatID '")
                        .append(chatId).append("' due to: ").append(e.getMessage()).toString(), e);
            } catch (SipNetworkException e) {
                if (sLogger.isActivated()) {
                    sLogger.debug(e.getMessage());
                }
            } catch (RuntimeException e) {
                /*
                 * Intentionally catch runtime exceptions as else it will abruptly end the thread
                 * and eventually bring the whole system down, which is not intended.
                 */
                sLogger.error(new StringBuilder("Could not auto-rejoin group chat with chatID '")
                        .append(chatId).append("' due to: ").append(e.getMessage()).toString(), e);

            }
        }
    }
}
