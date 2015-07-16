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

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSession;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.service.api.ServerApiUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.ChatLog.Message.Content;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Process;
import android.text.TextUtils;

public abstract class DequeueTask implements Runnable {

    protected final Object mLock;

    protected final Context mCtx;

    protected final Core mCore;

    protected final InstantMessagingService mImService;

    protected final ContactManager mContactManager;

    protected final MessagingLog mMessagingLog;

    protected final RcsSettings mRcsSettings;

    protected final ChatServiceImpl mChatService;

    protected final FileTransferServiceImpl mFileTransferService;

    protected final Logger mLogger = Logger.getLogger(getClass().getName());
    
    protected final ServerApiUtils mServerApiUtils;

    public DequeueTask(Object lock, Context ctx, Core core, ContactManager contactManager,
            MessagingLog messagingLog, RcsSettings rcsSettings, ChatServiceImpl chatService,
            FileTransferServiceImpl fileTransferService, ServerApiUtils serverApiUtils) {
        mLock = lock;
        mCtx = ctx;
        mCore = core;
        mImService = mCore.getImService();
        mContactManager = contactManager;
        mMessagingLog = messagingLog;
        mRcsSettings = rcsSettings;
        mChatService = chatService;
        mFileTransferService = fileTransferService;
        mServerApiUtils = serverApiUtils;
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
     * Check if it is possible to dequeue and transfer one-one file
     * 
     * @param contact
     * @param fileTransferService
     * @return boolean
     */
    protected boolean isAllowedToDequeueOneToOneFileTransfer(ContactId contact,
            FileTransferServiceImpl fileTransferService) {
        if (fileTransferService.getFileTransferProtocolForOneToOneFileTransfer(contact) == null) {
            if (mLogger.isActivated()) {
                mLogger.debug(new StringBuilder(
                        "Cannot dequeue one-to-one file transfer right now as there are no enough capabilities for remote contact '")
                        .append(contact).append("'").toString());
            }
            return false;
        }
        return isAllowedToDequeueFileTransfer();
    }

    /**
     * Check if it is possible to dequeue and transfer one-one file
     * 
     * @return boolean
     */
    protected boolean isAllowedToDequeueGroupFileTransfer() {
        return isAllowedToDequeueFileTransfer();
    }

    /**
     * Check if dequeueing and sending of 1-1 chat messages to specified contact is allowed
     * 
     * @param contact
     * @return
     */
    protected boolean isAllowedToDequeueOneToOneChatMessage(ContactId contact) {
        Capabilities remoteCapabilities = mContactManager.getContactCapabilities(contact);
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
     * Check if dequeueing and sending of 1-1 chat messages to specified contact is possible
     * 
     * @param contact
     * @return boolean
     */
    protected boolean isPossibleToDequeueOneToOneChatMessage(ContactId contact) {
        Capabilities remoteCapabilities = mContactManager.getContactCapabilities(contact);
        if (remoteCapabilities == null) {
            if (mLogger.isActivated()) {
                mLogger.debug(new StringBuilder(
                        "Cannot dequeue one-to-one chat messages as the capabilities are not known for remote contact ")
                        .append(contact).toString());
            }
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
    private boolean isPossibleToDequeueGroupChatMessagesAndGroupFileTransfers(String chatId) {
        if (!mRcsSettings.isGroupChatActivated()) {
            if (mLogger.isActivated()) {
                mLogger.debug("Cannot dequeue group chat messages and file transfers right now as group chat feature is not activated!");
            }
            return false;
        }
        if (mChatService.isGroupChatAbandoned(chatId)) {
            if (mLogger.isActivated()) {
                mLogger.debug(new StringBuilder(
                        "Cannot dequeue group chat messages and group file transfers right now as the group chat with chatId '")
                        .append(chatId)
                        .append("' is abandoned and can be no more used to send or receive messages.")
                        .toString());
            }
            return false;
        }
        final GroupChatSession groupChatSession = mImService.getGroupChatSession(chatId);
        if (groupChatSession == null) {
            GroupChatInfo groupChat = mMessagingLog.getGroupChatInfo(chatId);
            if (groupChat == null) {
                if (mLogger.isActivated()) {
                    mLogger.debug(new StringBuilder(
                            "Cannot dequeue group chat messages and group file transfers as the group chat with group chat Id '")
                            .append(chatId)
                            .append("' is not rejoinable as the group chat does not exist in DB.")
                            .toString());
                }
                return false;
            }
            if (TextUtils.isEmpty(groupChat.getRejoinId())) {
                if (mLogger.isActivated()) {
                    mLogger.debug(new StringBuilder(
                            "Cannot dequeue group chat messages and group file transfers as thr group chat with group chat Id '")
                            .append(chatId)
                            .append("' is not rejoinable as there is no ongoing session with "
                                    + "corresponding chatId and there exists no rejoinId to "
                                    + "rejoin the group chat.").toString());
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Check if dequeuing and sending of group chat messages to specified chatId is possible
     * 
     * @param chatId
     * @return boolean
     */
    protected boolean isPossibleToDequeueGroupChatMessage(String chatId) {
        return isPossibleToDequeueGroupChatMessagesAndGroupFileTransfers(chatId);
    }

    /**
     * Does the stack process have permissions to read the Uri
     * 
     * @param file
     * @return
     */
    private boolean isReadFromUriAllowed(Uri file) {
        return PackageManager.PERMISSION_GRANTED == mCtx.checkUriPermission(file, Process.myPid(),
                Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    /**
     * Check if it is possible to dequeue file transfer
     * 
     * @return boolean
     */
    private boolean isPossibleToDequeueFileTransfer(Uri file, long size) {
        if (ContentResolver.SCHEME_CONTENT.equals(file.getScheme()) && !isReadFromUriAllowed(file)) {
            if (mLogger.isActivated()) {
                mLogger.debug("Cannot dequeue file as there is no Uri permission available for file "
                        .concat(file.toString()));
            }
            return false;
        }
        if (mImService.isFileSizeExceeded(size)) {
            if (mLogger.isActivated()) {
                mLogger.debug(new StringBuilder(
                        "Cannot dequeue file as there the maximum allowed size is exceeded by the file ")
                        .append(file).append(" size: ").append(size).toString());
            }
            return false;
        }
        return true;
    }

    /**
     * Check if it is possible to dequeue one-one file transfer
     * 
     * @param contact
     * @param file
     * @param size
     * @return boolean
     */
    protected boolean isPossibleToDequeueOneToOneFileTransfer(ContactId contact, Uri file, long size) {
        Capabilities remoteCapabilities = mContactManager.getContactCapabilities(contact);
        if (remoteCapabilities == null) {
            if (mLogger.isActivated()) {
                mLogger.debug(new StringBuilder(
                        "Cannot dequeue one-to-one file transfer as the capabilities are not known for remote contact ")
                        .append(contact).toString());
            }
            return false;
        }
        if (!isPossibleToDequeueFileTransfer(file, size)) {
            return false;
        }
        return true;
    }

    /**
     * Check if it is possible to dequeue group file transfer
     * 
     * @param chatId
     * @param file
     * @param size
     * @return boolean
     */
    protected boolean isPossibleToDequeueGroupFileTransfer(String chatId, Uri file, long size) {
        if (!isPossibleToDequeueFileTransfer(file, size)) {
            return false;
        }
        if (!isPossibleToDequeueGroupChatMessagesAndGroupFileTransfers(chatId)) {
            return false;
        }
        if (!mRcsSettings.getMyCapabilities().isFileTransferHttpSupported()) {
            if (mLogger.isActivated()) {
                mLogger.debug("Cannot transfer file to group chat as FT over HTTP capabilities are not supported for self.");
            }
            return false;
        }
        return true;
    }

    /**
     * Set one-one chat message as failed
     * 
     * @param contact
     * @param msgId
     * @param mimeType
     */
    protected void setOneToOneChatMessageAsFailed(ContactId contact, String msgId, String mimeType) {
        mChatService.setOneToOneChatMessageStatusAndReasonCode(msgId, mimeType, contact,
                Status.FAILED, Content.ReasonCode.FAILED_SEND);
    }

    /**
     * Set group chat message as failed
     * 
     * @param chatId
     * @param msgId
     * @param mimeType
     */
    protected void setGroupChatMessageAsFailed(String chatId, String msgId, String mimeType) {
        mChatService.setGroupChatMessageStatusAndReasonCode(msgId, mimeType, chatId,
                Status.FAILED, Content.ReasonCode.FAILED_SEND);
    }

    /**
     * Set one-one file transfer as failed
     * 
     * @param contact
     * @param fileTransferId
     */
    protected void setOneToOneFileTransferAsFailed(ContactId contact, String fileTransferId) {
        mFileTransferService.setOneToOneFileTransferStateAndReasonCode(fileTransferId, contact,
                State.FAILED, FileTransfer.ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);
    }

    /**
     * Set group file transfer as failed
     * 
     * @param chatId
     * @param fileTransferId
     */
    protected void setGroupFileTransferAsFailed(String chatId, String fileTransferId) {
        mFileTransferService.setGroupFileTransferStateAndReasonCode(fileTransferId, chatId,
                State.FAILED, FileTransfer.ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);
    }

    /**
     * Is IMS connected
     * 
     * @return boolean
     */
    protected boolean isImsConnected() {
        return mServerApiUtils.isImsConnected();
    }
}
