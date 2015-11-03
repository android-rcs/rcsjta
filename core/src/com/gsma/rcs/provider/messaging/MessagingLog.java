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

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.fthttp.FtHttpResume;
import com.gsma.rcs.provider.fthttp.FtHttpResumeUpload;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog.Message.Content;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.ChatLog.Message.GroupChatEvent;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.chat.GroupChat.ReasonCode;
import com.gsma.services.rcs.chat.GroupChat.State;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

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
     * @param localContentResolver Local content resolver
     * @param rcsSettings the RCS settings accessor
     * @return singleton instance
     */
    public static MessagingLog createInstance(LocalContentResolver localContentResolver,
            RcsSettings rcsSettings) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (MessagingLog.class) {
            if (sInstance == null) {
                sInstance = new MessagingLog(localContentResolver, rcsSettings);
            }
            return sInstance;
        }
    }

    /**
     * Constructor
     * 
     * @param localContentResolver Local content provider
     * @param rcsSettings the RCS settings accessor
     */
    private MessagingLog(LocalContentResolver localContentResolver, RcsSettings rcsSettings) {
        mLocalContentResolver = localContentResolver;
        mGroupChatLog = new GroupChatLog(localContentResolver);
        mGroupChatDeliveryInfoLog = new GroupDeliveryInfoLog(localContentResolver);
        mMessageLog = new MessageLog(mLocalContentResolver, mGroupChatDeliveryInfoLog, rcsSettings);
        mFileTransferLog = new FileTransferLog(localContentResolver, mGroupChatDeliveryInfoLog,
                rcsSettings);
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
    public boolean setGroupChatStateAndReasonCode(String chatId, State state, ReasonCode reasonCode) {
        return mGroupChatLog.setGroupChatStateAndReasonCode(chatId, state, reasonCode);
    }

    @Override
    public boolean setGroupChatParticipants(String chatId,
            Map<ContactId, ParticipantStatus> participants) {
        return mGroupChatLog.setGroupChatParticipants(chatId, participants);
    }

    @Override
    public boolean setGroupChatRejoinId(String chatId, String rejoinId, boolean updateStateToStarted) {
        return mGroupChatLog.setGroupChatRejoinId(chatId, rejoinId, updateStateToStarted);
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
            Content.ReasonCode reasonCode, long deliveryExpiration) {
        mMessageLog.addOutgoingOneToOneChatMessage(msg, status, reasonCode, deliveryExpiration);
    }

    @Override
    public void addIncomingGroupChatMessage(String chatId, ChatMessage msg,
            boolean imdnDisplayedRequested) {
        mMessageLog.addIncomingGroupChatMessage(chatId, msg, imdnDisplayedRequested);
    }

    @Override
    public void addOutgoingGroupChatMessage(String chatId, ChatMessage msg,
            Set<ContactId> recipients, Status status, Content.ReasonCode reasonCode) {
        mMessageLog.addOutgoingGroupChatMessage(chatId, msg, recipients, status, reasonCode);
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

    public boolean setChatMessageStatusAndReasonCode(String msgId, Status status,
            Content.ReasonCode reasonCode) {
        return mMessageLog.setChatMessageStatusAndReasonCode(msgId, status, reasonCode);
    }

    @Override
    public boolean isMessagePersisted(String msgId) {
        return mMessageLog.isMessagePersisted(msgId);
    }

    @Override
    public void addOneToOneFileTransfer(String fileTransferId, ContactId contact,
            Direction direction, MmContent content, MmContent fileIcon, FileTransfer.State state,
            FileTransfer.ReasonCode reasonCode, long timestamp, long timestampSent,
            long fileExpiration, long fileIconExpiration) {
        mFileTransferLog.addOneToOneFileTransfer(fileTransferId, contact, direction, content,
                fileIcon, state, reasonCode, timestamp, timestampSent, fileExpiration,
                fileIconExpiration);
    }

    @Override
    public void addOutgoingGroupFileTransfer(String fileTransferId, String chatId,
            MmContent content, MmContent thumbnail, Set<ContactId> recipients,
            FileTransfer.State state, FileTransfer.ReasonCode reasonCode, long timestamp,
            long timestampSent) {
        mFileTransferLog.addOutgoingGroupFileTransfer(fileTransferId, chatId, content, thumbnail,
                recipients, state, reasonCode, timestamp, timestampSent);
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
    public boolean setFileTransferStateAndReasonCode(String fileTransferId,
            FileTransfer.State state, FileTransfer.ReasonCode reasonCode) {
        return mFileTransferLog
                .setFileTransferStateAndReasonCode(fileTransferId, state, reasonCode);
    }

    @Override
    public boolean setFileTransferDelivered(String fileTransferId, long timestampDelivered) {
        return mFileTransferLog.setFileTransferDelivered(fileTransferId, timestampDelivered);
    }

    @Override
    public boolean setFileTransferDisplayed(String fileTransferId, long timestampDisplayed) {
        return mFileTransferLog.setFileTransferDisplayed(fileTransferId, timestampDisplayed);
    }

    @Override
    public boolean setFileTransferStateAndTimestamp(String fileTransferId,
            FileTransfer.State state, FileTransfer.ReasonCode reasonCode, long timestamp,
            long timestampSent) {
        return mFileTransferLog.setFileTransferStateAndTimestamp(fileTransferId, state, reasonCode,
                timestamp, timestampSent);
    }

    @Override
    public void markFileTransferAsRead(String fileTransferId) {
        mFileTransferLog.markFileTransferAsRead(fileTransferId);
    }

    @Override
    public boolean setFileTransferProgress(String fileTransferId, long currentSize) {
        return mFileTransferLog.setFileTransferProgress(fileTransferId, currentSize);
    }

    @Override
    public boolean setFileTransferred(String fileTransferId, MmContent content,
            long fileExpiration, long fileIconExpiration, long deliveryExpiration) {
        return mFileTransferLog.setFileTransferred(fileTransferId, content, fileExpiration,
                fileIconExpiration, deliveryExpiration);
    }

    @Override
    public String getFileTransferIcon(String fileTransferId) {
        return mFileTransferLog.getFileTransferIcon(fileTransferId);
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
        mLocalContentResolver.delete(GroupChatData.CONTENT_URI, null, null);
        mLocalContentResolver.delete(MessageData.CONTENT_URI, null, null);
        mLocalContentResolver.delete(FileTransferData.CONTENT_URI, null, null);
        mLocalContentResolver.delete(GroupDeliveryInfoData.CONTENT_URI, null, null);
    }

    @Override
    public Uri addGroupChatDeliveryInfoEntry(String chatId, ContactId contact, String msgId,
            GroupDeliveryInfo.Status status, GroupDeliveryInfo.ReasonCode reasonCode,
            long timestampDelivered, long timestampDisplayed) {
        return mGroupChatDeliveryInfoLog.addGroupChatDeliveryInfoEntry(chatId, contact, msgId,
                status, reasonCode, timestampDelivered, timestampDisplayed);
    }

    @Override
    public boolean setGroupChatDeliveryInfoStatusAndReasonCode(String chatId, ContactId contact,
            String msgId, GroupDeliveryInfo.Status status, GroupDeliveryInfo.ReasonCode reasonCode) {
        return mGroupChatDeliveryInfoLog.setGroupChatDeliveryInfoStatusAndReasonCode(chatId,
                contact, msgId, status, reasonCode);
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
    public boolean setFileUploadTId(String fileTransferId, String tId) {
        return mFileTransferLog.setFileUploadTId(fileTransferId, tId);
    }

    @Override
    public boolean setFileDownloadAddress(String fileTransferId, Uri downloadAddress) {
        return mFileTransferLog.setFileDownloadAddress(fileTransferId, downloadAddress);
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
    public String getFileTransferChatId(String fileTransferId) {
        return mFileTransferLog.getFileTransferChatId(fileTransferId);
    }

    @Override
    public FileTransfer.State getFileTransferState(String fileTransferId) {
        return mFileTransferLog.getFileTransferState(fileTransferId);
    }

    @Override
    public FileTransfer.ReasonCode getFileTransferReasonCode(String fileTransferId) {
        return mFileTransferLog.getFileTransferReasonCode(fileTransferId);
    }

    @Override
    public Long getFileTransferSentTimestamp(String fileTransferId) {
        return mFileTransferLog.getFileTransferSentTimestamp(fileTransferId);
    }

    @Override
    public Long getFileTransferTimestamp(String fileTransferId) {
        return mFileTransferLog.getFileTransferTimestamp(fileTransferId);
    }

    @Override
    public Map<ContactId, ParticipantStatus> getParticipants(String chatId) {
        return mGroupChatLog.getParticipants(chatId);
    }

    @Override
    public Map<ContactId, ParticipantStatus> getParticipants(String chatId,
            Set<ParticipantStatus> statuses) {
        return mGroupChatLog.getParticipants(chatId, statuses);
    }

    @Override
    public boolean isGroupFileTransfer(String fileTransferId) {
        return mFileTransferLog.isGroupFileTransfer(fileTransferId);
    }

    @Override
    public boolean setRejectNextGroupChatNextInvitation(String chatId) {
        return mGroupChatLog.setRejectNextGroupChatNextInvitation(chatId);
    }

    @Override
    public Long getMessageSentTimestamp(String msgId) {
        return mMessageLog.getMessageSentTimestamp(msgId);
    }

    @Override
    public Boolean isMessageRead(String msgId) {
        return mMessageLog.isMessageRead(msgId);
    }

    @Override
    public Long getMessageTimestamp(String msgId) {
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
    public Cursor getFileTransferData(String fileTransferId) {
        return mFileTransferLog.getFileTransferData(fileTransferId);
    }

    @Override
    public Cursor getGroupChatData(String chatId) {
        return mGroupChatLog.getGroupChatData(chatId);
    }

    @Override
    public Cursor getChatMessageData(String msgId) {
        return mMessageLog.getChatMessageData(msgId);
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
    public Cursor getAllQueuedOneToOneChatMessages() {
        return mMessageLog.getAllQueuedOneToOneChatMessages();
    }

    @Override
    public Cursor getQueuedAndUploadedButNotTransferredFileTransfers() {
        return mFileTransferLog.getQueuedAndUploadedButNotTransferredFileTransfers();
    }

    @Override
    public Cursor getInterruptedFileTransfers() {
        return mFileTransferLog.getInterruptedFileTransfers();
    }

    public boolean setChatMessageTimestamp(String msgId, long timestamp, long timestampSent) {
        return mMessageLog.setChatMessageTimestamp(msgId, timestamp, timestampSent);
    }

    @Override
    public boolean setRemoteSipId(String fileTransferId, String remoteInstanceId) {
        return mFileTransferLog.setRemoteSipId(fileTransferId, remoteInstanceId);
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
    public boolean setGroupChatParticipantsStateAndReasonCode(String chatId,
            Map<ContactId, ParticipantStatus> participants, State state, ReasonCode reasonCode) {
        return mGroupChatLog.setGroupChatParticipantsStateAndReasonCode(chatId, participants,
                state, reasonCode);
    }

    @Override
    public boolean isOneToOneChatMessage(String msgId) {
        return mMessageLog.isOneToOneChatMessage(msgId);
    }

    @Override
    public boolean setGroupChatDeliveryInfoDelivered(String chatId, ContactId contact,
            String fileTransferId, long timestampDelivered) {
        return mGroupChatDeliveryInfoLog.setGroupChatDeliveryInfoDelivered(chatId, contact,
                fileTransferId, timestampDelivered);
    }

    @Override
    public boolean setGroupChatDeliveryInfoDisplayed(String chatId, ContactId contact,
            String fileTransferId, long timestampDisplayed) {
        return mGroupChatDeliveryInfoLog.setGroupChatDeliveryInfoDisplayed(chatId, contact,
                fileTransferId, timestampDisplayed);
    }

    @Override
    public boolean setChatMessageStatusDelivered(String msgId, long timestampDelivered) {
        return mMessageLog.setChatMessageStatusDelivered(msgId, timestampDelivered);
    }

    @Override
    public boolean setChatMessageStatusDisplayed(String msgId, long timestampDisplayed) {
        return mMessageLog.setChatMessageStatusDisplayed(msgId, timestampDisplayed);
    }

    @Override
    public void clearMessageDeliveryExpiration(List<String> msgIds) {
        mMessageLog.clearMessageDeliveryExpiration(msgIds);
    }

    @Override
    public void clearFileTransferDeliveryExpiration(List<String> fileTransferIds) {
        mFileTransferLog.clearFileTransferDeliveryExpiration(fileTransferIds);
    }

    @Override
    public boolean setFileTransferDeliveryExpired(String fileTransferId) {
        return mFileTransferLog.setFileTransferDeliveryExpired(fileTransferId);
    }

    @Override
    public boolean setChatMessageDeliveryExpired(String msgId) {
        return mMessageLog.setChatMessageDeliveryExpired(msgId);
    }

    @Override
    public Cursor getUndeliveredOneToOneChatMessages() {
        return mMessageLog.getUndeliveredOneToOneChatMessages();
    }

    @Override
    public Cursor getUnDeliveredOneToOneFileTransfers() {
        return mFileTransferLog.getUnDeliveredOneToOneFileTransfers();
    }

    @Override
    public Boolean isChatMessageExpiredDelivery(String msgId) {
        return mMessageLog.isChatMessageExpiredDelivery(msgId);
    }

    @Override
    public Boolean isFileTransferExpiredDelivery(String fileTransferId) {
        return mFileTransferLog.isFileTransferExpiredDelivery(fileTransferId);
    }

    @Override
    public String getMessageChatId(String msgId) {
        return mMessageLog.getMessageChatId(msgId);
    }

    @Override
    public void setFileTransferDownloadInfo(String fileTransferId,
            FileTransferHttpInfoDocument ftHttpInfo) {
        mFileTransferLog.setFileTransferDownloadInfo(fileTransferId, ftHttpInfo);
    }

    @Override
    public FileTransferHttpInfoDocument getFileDownloadInfo(String fileTransferId)
            throws FileAccessException {
        return mFileTransferLog.getFileDownloadInfo(fileTransferId);
    }

    @Override
    public FileTransferHttpInfoDocument getFileDownloadInfo(Cursor cursor)
            throws FileAccessException {
        return mFileTransferLog.getFileDownloadInfo(cursor);
    }

    @Override
    public void setFileTransferTimestamps(String fileTransferId, long timestamp, long timestampSent) {
        mFileTransferLog.setFileTransferTimestamps(fileTransferId, timestamp, timestampSent);
    }

    @Override
    public boolean setFileInfoDequeued(String fileTransferId, long deliveryExpiration) {
        return mFileTransferLog.setFileInfoDequeued(fileTransferId, deliveryExpiration);
    }

    @Override
    public boolean setChatMessageStatusAndTimestamp(String msgId, Status status,
            Content.ReasonCode reasonCode, long timestamp, long timestampSent) {
        return mMessageLog.setChatMessageStatusAndTimestamp(msgId, status, reasonCode, timestamp,
                timestampSent);
    }

    @Override
    public Long getFileTransferProgress(String fileTransferId) {
        return mFileTransferLog.getFileTransferProgress(fileTransferId);
    }

    @Override
    public void addOneToOneFailedDeliveryMessage(ChatMessage msg) {
        mMessageLog.addOneToOneFailedDeliveryMessage(msg);
    }

    @Override
    public void addGroupChatFailedDeliveryMessage(String chatId, ChatMessage msg) {
        mMessageLog.addGroupChatFailedDeliveryMessage(chatId, msg);
    }
}
