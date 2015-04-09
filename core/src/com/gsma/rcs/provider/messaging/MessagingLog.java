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

package com.gsma.rcs.provider.messaging;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.fthttp.FtHttpResume;
import com.gsma.rcs.provider.fthttp.FtHttpResumeUpload;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.services.rcs.GroupDeliveryInfo;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.Message.Content;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.ChatLog.Message.GroupChatEvent;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.chat.GroupChat.ReasonCode;
import com.gsma.services.rcs.chat.GroupChat.State;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to interface the Instant Messaging tables
 * 
 * @author LEMORDANT Philippe
 */
public class MessagingLog implements IGroupChatLog, IMessageLog, IFileTransferLog,
        IGroupDeliveryInfoLog {
    /**
     * Current instance
     */
    private static volatile MessagingLog sInstance;

    private final LocalContentResolver mLocalContentResolver;

    private final GroupChatLog mGroupChatLog;

    private final MessageLog mMessageLog;

    private final FileTransferLog mFileTransferLog;

    private final GroupDeliveryInfoLog mGroupChatDeliveryInfoLog;

    /**
     * Create instance
     * 
     * @param context Context
     * @param localContentResolver Local content resolver
     * @param rcsSettings
     * @return singleton instance
     */
    public static MessagingLog createInstance(Context context,
            LocalContentResolver localContentResolver, RcsSettings rcsSettings) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (MessagingLog.class) {
            if (sInstance == null) {
                sInstance = new MessagingLog(context, localContentResolver, rcsSettings);
            }
            return sInstance;
        }
    }

    /**
     * Constructor
     * 
     * @param context Application context
     * @param localContentResolver Local content provider
     * @param rcsSettings
     */
    private MessagingLog(Context context, LocalContentResolver localContentResolver,
            RcsSettings rcsSettings) {
        mLocalContentResolver = localContentResolver;
        mGroupChatLog = new GroupChatLog(context, localContentResolver);
        mGroupChatDeliveryInfoLog = new GroupDeliveryInfoLog(localContentResolver);
        mMessageLog = new MessageLog(mLocalContentResolver, mGroupChatLog,
                mGroupChatDeliveryInfoLog, rcsSettings);
        mFileTransferLog = new FileTransferLog(localContentResolver, mGroupChatLog,
                mGroupChatDeliveryInfoLog);
    }

    @Override
    public void addGroupChat(String chatId, ContactId contact, String subject,
            Map<ContactId, ParticipantStatus> participants, State state, ReasonCode reasonCode,
            Direction direction, long timestamp) {
        mGroupChatLog.addGroupChat(chatId, contact, subject, participants, state, reasonCode,
                direction, timestamp);
    }

    @Override
    public void acceptGroupChatNextInvitation(String chatId) {
        mGroupChatLog.acceptGroupChatNextInvitation(chatId);
    }

    @Override
    public void setGroupChatStateAndReasonCode(String chatId, State state, ReasonCode reasonCode) {
        mGroupChatLog.setGroupChatStateAndReasonCode(chatId, state, reasonCode);
    }

    @Override
    public void updateGroupChatParticipants(String chatId,
            Map<ContactId, ParticipantStatus> participants) {
        mGroupChatLog.updateGroupChatParticipants(chatId, participants);
    }

    @Override
    public void setGroupChatRejoinId(String chatId, String rejoinId) {
        mGroupChatLog.setGroupChatRejoinId(chatId, rejoinId);
    }

    @Override
    public GroupChatInfo getGroupChatInfo(String chatId) {
        return mGroupChatLog.getGroupChatInfo(chatId);
    }

    @Override
    public void addOneToOneSpamMessage(ChatMessage msg) {
        mMessageLog.addOneToOneSpamMessage(msg);
    }

    @Override
    public void addIncomingOneToOneChatMessage(ChatMessage msg, boolean imdnDisplayedRequested) {
        mMessageLog.addIncomingOneToOneChatMessage(msg, imdnDisplayedRequested);
    }

    @Override
    public void addOutgoingOneToOneChatMessage(ChatMessage msg, Status status,
            Content.ReasonCode reasonCode) {
        mMessageLog.addOutgoingOneToOneChatMessage(msg, status, reasonCode);
    }

    @Override
    public void addGroupChatMessage(String chatId, ChatMessage msg, Direction direction,
            Status status, Content.ReasonCode reasonCode) {
        mMessageLog.addGroupChatMessage(chatId, msg, direction, status, reasonCode);
    }

    @Override
    public String addGroupChatEvent(String chatId, ContactId contact, GroupChatEvent.Status status,
            long timestamp) {
        return mMessageLog.addGroupChatEvent(chatId, contact, status, timestamp);
    }

    @Override
    public void markMessageAsRead(String msgId) {
        mMessageLog.markMessageAsRead(msgId);
    }

    @Override
    public void setChatMessageStatusAndReasonCode(String msgId, Status status,
            Content.ReasonCode reasonCode) {
        mMessageLog.setChatMessageStatusAndReasonCode(msgId, status, reasonCode);
    }

    @Override
    public void markIncomingChatMessageAsReceived(String msgId) {
        mMessageLog.markIncomingChatMessageAsReceived(msgId);
    }

    @Override
    public boolean isMessagePersisted(String msgId) {
        return mMessageLog.isMessagePersisted(msgId);
    }

    @Override
    public void addFileTransfer(String fileTransferId, ContactId contact, Direction direction,
            MmContent content, MmContent fileIcon, FileTransfer.State state,
            FileTransfer.ReasonCode reasonCode, long timestamp, long timestampSent,
            long fileExpiration, long fileIconExpiration) {
        mFileTransferLog.addFileTransfer(fileTransferId, contact, direction, content, fileIcon,
                state, reasonCode, timestamp, timestampSent, fileExpiration, fileIconExpiration);
    }

    @Override
    public void addOutgoingGroupFileTransfer(String fileTransferId, String chatId,
            MmContent content, MmContent thumbnail, FileTransfer.State state,
            FileTransfer.ReasonCode reasonCode, long timestamp, long timestampSent) {
        mFileTransferLog.addOutgoingGroupFileTransfer(fileTransferId, chatId, content, thumbnail,
                state, reasonCode, timestamp, timestampSent);
    }

    @Override
    public void addIncomingGroupFileTransfer(String fileTransferId, String chatId,
            ContactId contact, MmContent content, MmContent fileIcon, FileTransfer.State state,
            FileTransfer.ReasonCode reasonCode, long timestamp, long timestampSent,
            long fileExpiration, long fileIconExpiration) {
        mFileTransferLog.addIncomingGroupFileTransfer(fileTransferId, chatId, contact, content,
                fileIcon, state, reasonCode, timestamp, timestampSent, fileExpiration,
                fileIconExpiration);
    }

    @Override
    public void setFileTransferStateAndReasonCode(String fileTransferId, FileTransfer.State state,
            FileTransfer.ReasonCode reasonCode) {
        mFileTransferLog.setFileTransferStateAndReasonCode(fileTransferId, state, reasonCode);
    }

    @Override
    public void setFileTransferStateAndTimestamps(String fileTransferId, FileTransfer.State state,
            FileTransfer.ReasonCode reasonCode, long timestamp, long timestampSent) {
        mFileTransferLog.setFileTransferStateAndTimestamps(fileTransferId, state, reasonCode,
                timestamp, timestampSent);

    }

    @Override
    public void markFileTransferAsRead(String fileTransferId) {
        mFileTransferLog.markFileTransferAsRead(fileTransferId);
    }

    @Override
    public void setFileTransferProgress(String fileTransferId, long currentSize) {
        mFileTransferLog.setFileTransferProgress(fileTransferId, currentSize);
    }

    @Override
    public void setFileTransferred(String fileTransferId, MmContent content, long fileExpiration,
            long fileIconExpiration) {
        mFileTransferLog.setFileTransferred(fileTransferId, content, fileExpiration,
                fileIconExpiration);
    }

    @Override
    public boolean isFileTransfer(String fileTransferId) {
        return mFileTransferLog.isFileTransfer(fileTransferId);
    }

    @Override
    public boolean isGroupChatNextInviteRejected(String chatId) {
        return mGroupChatLog.isGroupChatNextInviteRejected(chatId);
    }

    /**
     * Delete all entries in Chat, Message and FileTransfer Logs
     */
    public void deleteAllEntries() {
        mLocalContentResolver.delete(ChatData.CONTENT_URI, null, null);
        mLocalContentResolver.delete(ChatLog.Message.CONTENT_URI, null, null);
        mLocalContentResolver.delete(FileTransferData.CONTENT_URI, null, null);
        mLocalContentResolver.delete(GroupDeliveryInfoData.CONTENT_URI, null, null);
    }

    @Override
    public Uri addGroupChatDeliveryInfoEntry(String chatId, ContactId contact, String msgId,
            GroupDeliveryInfo.Status status, GroupDeliveryInfo.ReasonCode reasonCode) {
        return mGroupChatDeliveryInfoLog.addGroupChatDeliveryInfoEntry(chatId, contact, msgId,
                status, reasonCode);
    }

    @Override
    public void setGroupChatDeliveryInfoStatusAndReasonCode(String msgId, ContactId contact,
            GroupDeliveryInfo.Status status, GroupDeliveryInfo.ReasonCode reasonCode) {
        mGroupChatDeliveryInfoLog.setGroupChatDeliveryInfoStatusAndReasonCode(msgId, contact,
                status, reasonCode);
    }

    @Override
    public boolean isDeliveredToAllRecipients(String msgId) {
        return mGroupChatDeliveryInfoLog.isDeliveredToAllRecipients(msgId);
    }

    @Override
    public boolean isDisplayedByAllRecipients(String msgId) {
        return mGroupChatDeliveryInfoLog.isDisplayedByAllRecipients(msgId);
    }

    @Override
    public void setFileUploadTId(String fileTransferId, String tId) {
        mFileTransferLog.setFileUploadTId(fileTransferId, tId);
    }

    @Override
    public void setFileDownloadAddress(String fileTransferId, Uri downloadAddress) {
        mFileTransferLog.setFileDownloadAddress(fileTransferId, downloadAddress);
    }

    @Override
    public List<FtHttpResume> retrieveFileTransfersPausedBySystem() {
        return mFileTransferLog.retrieveFileTransfersPausedBySystem();
    }

    @Override
    public FtHttpResumeUpload retrieveFtHttpResumeUpload(String tId) {
        return mFileTransferLog.retrieveFtHttpResumeUpload(tId);
    }

    @Override
    public GroupChat.State getGroupChatState(String chatId) {
        return mGroupChatLog.getGroupChatState(chatId);
    }

    @Override
    public GroupChat.ReasonCode getGroupChatReasonCode(String chatId) {
        return mGroupChatLog.getGroupChatReasonCode(chatId);
    }

    @Override
    public FileTransfer.State getFileTransferState(String fileTransferId) {
        return mFileTransferLog.getFileTransferState(fileTransferId);
    }

    @Override
    public FileTransfer.ReasonCode getFileTransferStateReasonCode(String fileTransferId) {
        return mFileTransferLog.getFileTransferStateReasonCode(fileTransferId);
    }

    @Override
    public long getFileTransferSentTimestamp(String fileTransferId) {
        return mFileTransferLog.getFileTransferSentTimestamp(fileTransferId);
    }

    @Override
    public long getFileTransferTimestamp(String fileTransferId) {
        return mFileTransferLog.getFileTransferTimestamp(fileTransferId);
    }

    @Override
    public Map<ContactId, ParticipantStatus> getParticipants(String chatId) {
        return mGroupChatLog.getParticipants(chatId);
    }

    @Override
    public boolean isGroupFileTransfer(String fileTransferId) {
        return mFileTransferLog.isGroupFileTransfer(fileTransferId);
    }

    @Override
    public void setRejectNextGroupChatNextInvitation(String chatId) {
        mGroupChatLog.setRejectNextGroupChatNextInvitation(chatId);
    }

    @Override
    public long getMessageSentTimestamp(String msgId) {
        return mMessageLog.getMessageSentTimestamp(msgId);
    }

    @Override
    public boolean isMessageRead(String msgId) {
        return mMessageLog.isMessageRead(msgId);
    }

    @Override
    public long getMessageTimestamp(String msgId) {
        return mMessageLog.getMessageTimestamp(msgId);
    }

    @Override
    public Status getMessageStatus(String msgId) {
        return mMessageLog.getMessageStatus(msgId);
    }

    @Override
    public Content.ReasonCode getMessageReasonCode(String msgId) {
        return mMessageLog.getMessageReasonCode(msgId);
    }

    @Override
    public String getMessageMimeType(String msgId) {
        return mMessageLog.getMessageMimeType(msgId);
    }

    @Override
    public Set<String> getChatIdsOfActiveGroupChatsForAutoRejoin() {
        return mGroupChatLog.getChatIdsOfActiveGroupChatsForAutoRejoin();
    }

    @Override
    public Cursor getCacheableFileTransferData(String fileTransferId) {
        return mFileTransferLog.getCacheableFileTransferData(fileTransferId);
    }

    @Override
    public Cursor getCacheableGroupChatData(String chatId) {
        return mGroupChatLog.getCacheableGroupChatData(chatId);
    }

    @Override
    public Cursor getCacheableChatMessageData(String msgId) {
        return mMessageLog.getCacheableChatMessageData(msgId);
    }

    @Override
    public String getChatMessageContent(String msgId) {
        return mMessageLog.getChatMessageContent(msgId);
    }

    @Override
    public FtHttpResume getFileTransferResumeInfo(String fileTransferId) {
        return mFileTransferLog.getFileTransferResumeInfo(fileTransferId);
    }

    @Override
    public Cursor getQueuedOneToOneChatMessages(ContactId contact) {
        return mMessageLog.getQueuedOneToOneChatMessages(contact);
    }

    @Override
    public Cursor getQueuedFileTransfers() {
        return mFileTransferLog.getQueuedFileTransfers();
    }

    @Override
    public void dequeueChatMessage(ChatMessage message) {
        mMessageLog.dequeueChatMessage(message);
    }

    @Override
    public void dequeueFileTransfer(String fileTransferId, long timestamp, long timestampSent) {
        mFileTransferLog.dequeueFileTransfer(fileTransferId, timestamp, timestampSent);
    }

    @Override
    public Set<ContactId> getGroupChatParticipantsToBeInvited(String chatId) {
        return mGroupChatLog.getGroupChatParticipantsToBeInvited(chatId);
    }

    @Override
    public Cursor getQueuedGroupChatMessages(String chatId) {
        return mMessageLog.getQueuedGroupChatMessages(chatId);
    }

    @Override
    public Cursor getQueuedGroupFileTransfers(String chatId) {
        return mFileTransferLog.getQueuedGroupFileTransfers(chatId);
    }

    public Cursor getQueuedOneToOneFileTransfers(ContactId contact) {
        return mFileTransferLog.getQueuedOneToOneFileTransfers(contact);
    }

    @Override
    public String getFileTransferUploadTid(String fileTransferId) {
        return mFileTransferLog.getFileTransferUploadTid(fileTransferId);
    }

    @Override
    public Cursor getInterruptedFileTransfers() {
        return mFileTransferLog.getInterruptedFileTransfers();
    }

    public void setChatMessageTimestamp(String msgId, long timestamp, long timestampSent) {
        mMessageLog.setChatMessageTimestamp(msgId, timestamp, timestampSent);
    }

    @Override
    public void setRemoteSipId(String fileTransferId, String remoteInstanceId) {
        mFileTransferLog.setRemoteSipId(fileTransferId, remoteInstanceId);
    }

    @Override
    public boolean isGroupChatPersisted(String chatId) {
        return mGroupChatLog.isGroupChatPersisted(chatId);
    }

    @Override
    public Map<ContactId, GroupChatEvent.Status> getGroupChatEvents(String chatId) {
        return mMessageLog.getGroupChatEvents(chatId);
    }

    @Override
    public void setGroupChatParticipantsStateAndReasonCode(
            Map<ContactId, ParticipantStatus> participants, String chatId, State state,
            ReasonCode reasonCode) {
        mGroupChatLog.setGroupChatParticipantsStateAndReasonCode(participants, chatId, state,
                reasonCode);
    }
}
