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

import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSession;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.GroupChat.ReasonCode;
import com.gsma.services.rcs.contact.ContactId;

public abstract class DequeueTask implements Runnable {

    protected final Object mLock;

    protected final InstantMessagingService mImService;

    protected final ContactsManager mContactManager;

    protected final MessagingLog mMessagingLog;

    protected final RcsSettings mRcsSettings;

    protected final Logger mLogger = Logger.getLogger(getClass().getName());

    public DequeueTask(Object lock, InstantMessagingService imService,
            ContactsManager contactManager, MessagingLog messagingLog, RcsSettings rcsSettings) {
        mLock = lock;
        mImService = imService;
        mContactManager = contactManager;
        mMessagingLog = messagingLog;
        mRcsSettings = rcsSettings;
    }

    /**
     * Check if it is possible to dequeue and transfer one-one/ group file
     * 
     * @return boolean
     */
    private boolean isAllowedToDequeueFileTransfer() {
        if (mImService.isMaxConcurrentOutgoingFileTransfersReached()) {
            if (mLogger.isActivated()) {
                mLogger.debug("Cannot dequeue file transfer as the limit of allowed concurrent outgoing file transfers is reached.");
            }
            return false;
        }
        if (!mImService.isFileTransferSessionAvailable()) {
            if (mLogger.isActivated()) {
                mLogger.debug("Cannot dequeue file transfer as there are no available file transfer sessions.");
            }
            return false;
        }
        return true;
    }

    /**
     * Check if dequeueing and sending of 1-1 chat messages to specified contact is possible
     * 
     * @param contact
     * @return boolean
     */
    protected boolean isAllowedToDequeueOneToOneChatMessage(ContactId contact) {
        Capabilities remoteCapabilities = mContactManager.getContactCapabilities(contact);
        if (remoteCapabilities == null) {
            if (mLogger.isActivated()) {
                mLogger.debug(new StringBuilder(
                        "Cannot dequeue one-to-one chat messages right now as the capabilities are not known for remote contact ")
                        .append(contact).toString());
            }
            return false;
        }
        if (!remoteCapabilities.isImSessionSupported()) {
            if (mLogger.isActivated()) {
                mLogger.debug(new StringBuilder(
                        "Cannot dequeue one-to-one chat messages right now as IM session capabilities are not supported for remote contact ")
                        .append(contact).append(" and IM_CAP_ALWAYS_ON is false!").toString());
            }
            return false;
        }
        return true;
    }

    /**
     * Check if it is allowed to dequeue one-one file transfer
     * 
     * @return boolean
     */
    protected boolean isAllowedToDequeueOneToOneFileTransfer() {
        return isAllowedToDequeueFileTransfer();
    }

    /**
     * Check if it is allowed to dequeue group file transfer
     * 
     * @param chatId
     * @return boolean
     */
    protected boolean isAllowedToDequeueGroupFileTransfer(String chatId) {
        if (!mRcsSettings.getMyCapabilities().isFileTransferHttpSupported()) {
            if (mLogger.isActivated()) {
                mLogger.debug("Cannot transfer file to group chat as FT over HTTP capabilities are not supported for self.");
            }
            return false;
        }
        final GroupChatSession groupChatSession = mImService.getGroupChatSession(chatId);
        if (groupChatSession == null || !groupChatSession.isMediaEstablished()) {
            if (mLogger.isActivated()) {
                mLogger.debug(new StringBuilder(
                        "Cannot transfer file to group chat as there is no corresponding group chat session with chatId ")
                        .append(chatId).append(" in established state.").toString());
            }
            return false;
        }
        if (!isAllowedToDequeueFileTransfer()) {
            return false;
        }
        return true;
    }

    /**
     * Check if it is possible to dequeue group chat messages and group file transfers
     * 
     * @param chatId
     * @return boolean
     */
    protected boolean isAllowedToDequeueGroupChatMessagesAndGroupFileTransfers(String chatId) {
        if (!mRcsSettings.isGroupChatActivated()) {
            if (mLogger.isActivated()) {
                mLogger.debug("Cannot dequeue group chat messages and file transfers right now as group chat feature is not activated!");
            }
            return false;
        }
        ReasonCode reasonCode = mMessagingLog.getGroupChatReasonCode(chatId);
        switch (reasonCode) {
            case ABORTED_BY_USER:
            case FAILED_INITIATION:
            case REJECTED_BY_REMOTE:
            case REJECTED_MAX_CHATS:
            case REJECTED_SPAM:
            case REJECTED_BY_TIMEOUT:
                if (mLogger.isActivated()) {
                    mLogger.debug(new StringBuilder(
                            "Cannot dequeue group chat messages and group file transfers right now as it is ")
                            .append(reasonCode).toString());
                }
                return false;
            default:
                break;
        }
        return true;
    }
}
