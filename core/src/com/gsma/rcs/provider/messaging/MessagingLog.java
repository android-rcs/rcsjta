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
import com.gsma.services.rcs.GroupDeliveryInfo;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.GroupChatEvent;
import com.gsma.services.rcs.chat.ChatLog.Message.Status;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChat.ReasonCode;
import com.gsma.services.rcs.chat.GroupChat.State;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.List;
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
    private static MessagingLog instance;

    private LocalContentResolver mLocalContentResolver;

    private GroupChatLog groupChatLog;

    private MessageLog messageLog;

    private FileTransferLog fileTransferLog;

    private GroupDeliveryInfoLog groupChatDeliveryInfoLog;

    /**
     * Create instance
     * 
     * @param context Context
     * @param localContentResolver Local content resolver
     */
    public static synchronized void createInstance(Context context,
            LocalContentResolver localContentResolver) {
        if (instance == null) {
            instance = new MessagingLog(context, localContentResolver);
        }
    }

    /**
     * Returns instance
     * 
     * @return Instance
     */
    public static MessagingLog getInstance() {
        return instance;
    }

    /**
     * Constructor
     * 
     * @param context Application context
     * @param localContentResolver Local content provider
     */
    private MessagingLog(Context context, LocalContentResolver localContentResolver) {
        mLocalContentResolver = localContentResolver;
        groupChatLog = new GroupChatLog(context, localContentResolver);
        groupChatDeliveryInfoLog = new GroupDeliveryInfoLog(localContentResolver);
        messageLog = new MessageLog(mLocalContentResolver, groupChatLog, groupChatDeliveryInfoLog);
        fileTransferLog = new FileTransferLog(localContentResolver, groupChatLog,
                groupChatDeliveryInfoLog);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatLog#addGroupChat(java .lang.String,
     * com.gsma.services.rcs.contact.ContactId, java.lang.String, java.util.Set, int, int, int)
     */
    @Override
    public void addGroupChat(String chatId, ContactId contact, String subject,
            Set<ParticipantInfo> participants, State state, ReasonCode reasonCode,
            Direction direction) {
        groupChatLog.addGroupChat(chatId, contact, subject, participants, state, reasonCode,
                direction);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatLog#acceptGroupChatNextInvitation(java.lang
     * .String)
     */
    @Override
    public void acceptGroupChatNextInvitation(String chatId) {
        groupChatLog.acceptGroupChatNextInvitation(chatId);
    }

    /*
     * (non-Javadoc)
     * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog# setGroupChatStateAndReasonCode
     * setGroupChatStateAndReasonCode (java.lang.String, int, int, GroupChatLog.ActiveStatus)
     */
    @Override
    public void setGroupChatStateAndReasonCode(String chatId, State state, ReasonCode reasonCode) {
        groupChatLog.setGroupChatStateAndReasonCode(chatId, state, reasonCode);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatLog#updateGroupChatParticipant(java.lang.
     * String, java.util.Set)
     */
    @Override
    public void updateGroupChatParticipant(String chatId, Set<ParticipantInfo> participants) {
        groupChatLog.updateGroupChatParticipant(chatId, participants);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatLog#setGroupChatRejoinId(java.lang.String,
     * java.lang.String)
     */
    @Override
    public void setGroupChatRejoinId(String chatId, String rejoinId) {
        groupChatLog.setGroupChatRejoinId(chatId, rejoinId);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatLog#getGroupChatInfo(java.lang.String)
     */
    @Override
    public GroupChatInfo getGroupChatInfo(String chatId) {
        return groupChatLog.getGroupChatInfo(chatId);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatLog#getGroupChatConnectedParticipants(java
     * .lang.String)
     */
    @Override
    public Set<ParticipantInfo> getGroupChatConnectedParticipants(String chatId) {
        return groupChatLog.getGroupChatConnectedParticipants(chatId);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IMessageLog#addOneToOneSpamMessage(
     * orangelabs.rcs.core.ims.service.im.chat.ChatMessage))
     */
    @Override
    public void addOneToOneSpamMessage(ChatMessage msg) {
        messageLog.addOneToOneSpamMessage(msg);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IMessageLog# addIncomingOneToOneChatMessage
     * (com.gsma.rcs.core.ims.service.im.chat.ChatMessage, boolean)
     */
    @Override
    public void addIncomingOneToOneChatMessage(ChatMessage msg, boolean imdnDisplayedRequested) {
        messageLog.addIncomingOneToOneChatMessage(msg, imdnDisplayedRequested);
    }

    /*
     * (non-Javadoc)
     * @see com.orangelabs.rcs.provider.messaging.IMessageLog# addOutgoingOneToOneChatMessage
     * addOutgoingOneToOneChatMessage (com.orangelabs .rcs.core.ims.service.im.chat.ChatMessage,
     * int, int)
     */
    @Override
    public void addOutgoingOneToOneChatMessage(ChatMessage msg, Status status,
            Message.ReasonCode reasonCode) {
        messageLog.addOutgoingOneToOneChatMessage(msg, status, reasonCode);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IMessageLog#addGroupChatMessage (java.lang.String,
     * com.gsma.rcs.core.ims.service.im.chat.ChatMessage, int, int, int)
     */
    @Override
    public void addGroupChatMessage(String chatId, ChatMessage msg, Direction direction,
            Status status, Message.ReasonCode reasonCode) {
        messageLog.addGroupChatMessage(chatId, msg, direction, status, reasonCode);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IMessageLog#addGroupChatEvent(java.lang.String,
     * java.lang.String, int)
     */
    @Override
    public void addGroupChatEvent(String chatId, ContactId contact, GroupChatEvent event) {
        messageLog.addGroupChatEvent(chatId, contact, event);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IMessageLog#markMessageAsRead(java.lang.String)
     */
    @Override
    public void markMessageAsRead(String msgId) {
        messageLog.markMessageAsRead(msgId);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IMessageLog# updateChatMessageStatusAndReasonCode
     * (java.lang.String, int, int)
     */
    @Override
    public void setChatMessageStatusAndReasonCode(String msgId, Status status,
            Message.ReasonCode reasonCode) {
        messageLog.setChatMessageStatusAndReasonCode(msgId, status, reasonCode);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IMessageLog#markIncomingChatMessageAsReceived(java.
     * lang.String)
     */
    @Override
    public void markIncomingChatMessageAsReceived(String msgId) {
        messageLog.markIncomingChatMessageAsReceived(msgId);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IMessageLog#isMessagePersisted(java.lang.String)
     */
    @Override
    public boolean isMessagePersisted(String msgId) {
        return messageLog.isMessagePersisted(msgId);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IFileTransferLog#addFileTransfer(java.lang.String,
     * java.lang.String, int, com.gsma.rcs.core.content.MmContent,
     * com.gsma.rcs.core.content.MmContent, int, int)
     */
    @Override
    public void addFileTransfer(String fileTransferId, ContactId contact, Direction direction,
            MmContent content, MmContent fileIcon, FileTransfer.State state,
            FileTransfer.ReasonCode reasonCode) {
        fileTransferLog.addFileTransfer(fileTransferId, contact, direction, content, fileIcon,
                state, reasonCode);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IFileTransferLog#addOutgoingGroupFileTransfer(java.
     * lang.String, java.lang.String, com.gsma.rcs.core.content.MmContent,
     * com.gsma.rcs.core.content.MmContent, int, int)
     */
    @Override
    public void addOutgoingGroupFileTransfer(String fileTransferId, String chatId,
            MmContent content, MmContent thumbnail, FileTransfer.State state,
            FileTransfer.ReasonCode reasonCode) {
        fileTransferLog.addOutgoingGroupFileTransfer(fileTransferId, chatId, content, thumbnail,
                state, reasonCode);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IFileTransferLog#addIncomingGroupFileTransfer(java.
     * lang.String, java.lang.String, java.lang.String, com.gsma.rcs.core.content.MmContent,
     * com.gsma.rcs.core.content.MmContent, int, int)
     */
    @Override
    public void addIncomingGroupFileTransfer(String fileTransferId, String chatId,
            ContactId contact, MmContent content, MmContent fileIcon, FileTransfer.State state,
            FileTransfer.ReasonCode reasonCode) {
        fileTransferLog.addIncomingGroupFileTransfer(fileTransferId, chatId, contact, content,
                fileIcon, state, reasonCode);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IFileTransferLog#
     * updateFileTransferStateAndReasonCode(java.lang.String, int, int
     */
    @Override
    public void setFileTransferStateAndReasonCode(String fileTransferId, FileTransfer.State state,
            FileTransfer.ReasonCode reasonCode) {
        fileTransferLog.setFileTransferStateAndReasonCode(fileTransferId, state, reasonCode);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IFileTransferLog#markFileTransferAsRead(java.lang.String
     * )
     */
    @Override
    public void markFileTransferAsRead(String fileTransferId) {
        fileTransferLog.markFileTransferAsRead(fileTransferId);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IFileTransferLog#updateFileTransferProgress(java.lang
     * .String, long)
     */
    @Override
    public void setFileTransferProgress(String fileTransferId, long currentSize) {
        fileTransferLog.setFileTransferProgress(fileTransferId, currentSize);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IFileTransferLog#updateFileTransferred(java.lang.String
     * , com.gsma.rcs.core.content.MmContent)
     */
    @Override
    public void setFileTransferred(String fileTransferId, MmContent content) {
        fileTransferLog.setFileTransferred(fileTransferId, content);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IFileTransferLog#isFileTransfer(java.lang.String)
     */
    @Override
    public boolean isFileTransfer(String fileTransferId) {
        return fileTransferLog.isFileTransfer(fileTransferId);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatLog#isGroupChatNextInviteRejected(java.lang
     * .String)
     */
    @Override
    public boolean isGroupChatNextInviteRejected(String chatId) {
        return groupChatLog.isGroupChatNextInviteRejected(chatId);
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

    /*
     * (non-Javadoc)
     * @see
     * com.orangelabs.rcs.provider.messaging.IGroupChatDeliveryInfoLog#addGroupChatDeliveryInfoEntry
     * (java.lang.String, com.gsma.services.rcs.contact.ContactId, java.lang.String,
     * GroupDeliveryInfoLog.Status, GroupDeliveryInfoLog.ReasonCode)
     */
    @Override
    public Uri addGroupChatDeliveryInfoEntry(String chatId, ContactId contact, String msgId,
            GroupDeliveryInfo.Status status, GroupDeliveryInfo.ReasonCode reasonCode) {
        return groupChatDeliveryInfoLog.addGroupChatDeliveryInfoEntry(chatId, contact, msgId,
                status, reasonCode);
    }

    /*
     * (non-Javadoc)
     * @see com.orangelabs.rcs.provider.messaging.IGroupChatDeliveryInfoLog#
     * setGroupChatDeliveryInfoStatusAndReasonCode(java.lang.String,
     * com.gsma.services.rcs.contact.ContactId, GroupDeliveryInfoLog.Status,
     * GroupDeliveryInfoLog.ReasonCode)
     */
    @Override
    public void setGroupChatDeliveryInfoStatusAndReasonCode(String msgId, ContactId contact,
            GroupDeliveryInfo.Status status, GroupDeliveryInfo.ReasonCode reasonCode) {
        groupChatDeliveryInfoLog.setGroupChatDeliveryInfoStatusAndReasonCode(msgId, contact,
                status, reasonCode);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatDeliveryInfoLog#isDeliveredToAllRecipients
     * (java.lang.String)
     */
    @Override
    public boolean isDeliveredToAllRecipients(String msgId) {
        return groupChatDeliveryInfoLog.isDeliveredToAllRecipients(msgId);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatDeliveryInfoLog#isDisplayedByAllRecipients
     * (java.lang.String)
     */
    @Override
    public boolean isDisplayedByAllRecipients(String msgId) {
        return groupChatDeliveryInfoLog.isDisplayedByAllRecipients(msgId);
    }

    @Override
    public void setFileUploadTId(String fileTransferId, String tId) {
        fileTransferLog.setFileUploadTId(fileTransferId, tId);
    }

    @Override
    public void setFileDownloadAddress(String fileTransferId, Uri downloadAddress) {
        fileTransferLog.setFileDownloadAddress(fileTransferId, downloadAddress);
    }

    @Override
    public List<FtHttpResume> retrieveFileTransfersPausedBySystem() {
        return fileTransferLog.retrieveFileTransfersPausedBySystem();
    }

    @Override
    public FtHttpResumeUpload retrieveFtHttpResumeUpload(String tId) {
        return fileTransferLog.retrieveFtHttpResumeUpload(tId);
    }

    @Override
    public Set<ParticipantInfo> getParticipants(String participants) {
        return groupChatLog.getParticipants(participants);
    }

    @Override
    public GroupChat.State getGroupChatState(String chatId) {
        return groupChatLog.getGroupChatState(chatId);
    }

    @Override
    public GroupChat.ReasonCode getGroupChatReasonCode(String chatId) {
        return groupChatLog.getGroupChatReasonCode(chatId);
    }

    @Override
    public FileTransfer.State getFileTransferState(String fileTransferId) {
        return fileTransferLog.getFileTransferState(fileTransferId);
    }

    @Override
    public FileTransfer.ReasonCode getFileTransferStateReasonCode(String fileTransferId) {
        return fileTransferLog.getFileTransferStateReasonCode(fileTransferId);
    }

    @Override
    public long getFileTransferSentTimestamp(String fileTransferId) {
        return fileTransferLog.getFileTransferSentTimestamp(fileTransferId);
    }

    @Override
    public long getFileTransferTimestamp(String fileTransferId) {
        return fileTransferLog.getFileTransferTimestamp(fileTransferId);
    }

    @Override
    public Set<ParticipantInfo> getGroupChatParticipants(String chatId) {
        return groupChatLog.getGroupChatParticipants(chatId);
    }

    @Override
    public boolean isGroupFileTransfer(String fileTransferId) {
        return fileTransferLog.isGroupFileTransfer(fileTransferId);
    }

    @Override
    public void setRejectNextGroupChatNextInvitation(String chatId) {
        groupChatLog.setRejectNextGroupChatNextInvitation(chatId);
    }

    @Override
    public long getMessageSentTimestamp(String msgId) {
        return messageLog.getMessageSentTimestamp(msgId);
    }

    @Override
    public boolean isMessageRead(String msgId) {
        return messageLog.isMessageRead(msgId);
    }

    @Override
    public long getMessageTimestamp(String msgId) {
        return messageLog.getMessageTimestamp(msgId);
    }

    @Override
    public Status getMessageStatus(String msgId) {
        return messageLog.getMessageStatus(msgId);
    }

    @Override
    public Message.ReasonCode getMessageReasonCode(String msgId) {
        return messageLog.getMessageReasonCode(msgId);
    }

    @Override
    public String getMessageMimeType(String msgId) {
        return messageLog.getMessageMimeType(msgId);
    }

    @Override
    public Set<String> getChatIdsOfActiveGroupChatsForAutoRejoin() {
        return groupChatLog.getChatIdsOfActiveGroupChatsForAutoRejoin();
    }

    @Override
    public Cursor getCacheableFileTransferData(String fileTransferId) {
        return fileTransferLog.getCacheableFileTransferData(fileTransferId);
    }

    @Override
    public Cursor getCacheableGroupChatData(String chatId) {
        return groupChatLog.getCacheableGroupChatData(chatId);
    }

    @Override
    public Cursor getCacheableChatMessageData(String msgId) {
        return messageLog.getCacheableChatMessageData(msgId);
    }

    @Override
    public String getChatMessageContent(String msgId) {
        return messageLog.getChatMessageContent(msgId);
    }
}
