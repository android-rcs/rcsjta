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

package com.orangelabs.rcs.provider.messaging;

import java.util.List;
import java.util.Set;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.provider.LocalContentResolver;
import com.orangelabs.rcs.provider.fthttp.FtHttpResume;
import com.orangelabs.rcs.provider.fthttp.FtHttpResumeUpload;

import android.content.Context;
import android.net.Uri;

/**
 * Class to interface the Instant Messaging tables
 * 
 * @author LEMORDANT Philippe
 * 
 */
public class MessagingLog implements IGroupChatLog, IMessageLog, IFileTransferLog, IGroupDeliveryInfoLog {
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
	 * Empty constructor : prevent caller from creating multiple instances
	 */
	private MessagingLog() {
	}

	/**
	 * Create instance
	 * 
	 * @param context
	 *            Context
	 * @param localContentResolver
	 *            Local content resolver
	 */
	public static synchronized void createInstance(Context context, LocalContentResolver localContentResolver) {
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
	 * @param context
	 *            Application context
	 * @param localContentResolver
	 *            Local content provider
	 */
	private MessagingLog(Context context, LocalContentResolver localContentResolver) {
		mLocalContentResolver = localContentResolver;
		groupChatLog = new GroupChatLog(context, localContentResolver);
		groupChatDeliveryInfoLog = new GroupDeliveryInfoLog(localContentResolver);
		messageLog = new MessageLog(mLocalContentResolver, groupChatLog, groupChatDeliveryInfoLog);
		fileTransferLog = new FileTransferLog(localContentResolver, groupChatLog, groupChatDeliveryInfoLog);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.orangelabs.rcs.provider.messaging.IGroupChatLog#addGroupChat(java
	 * .lang.String, com.gsma.services.rcs.contacts.ContactId, java.lang.String,
	 * java.util.Set, int, int, int)
	 */
	@Override
	public void addGroupChat(String chatId, ContactId contact, String subject,
			Set<ParticipantInfo> participants, int status, int reasonCode, int direction) {
		groupChatLog.addGroupChat(chatId, contact, subject, participants, status, reasonCode,
				direction);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#acceptGroupChatNextInvitation(java.lang.String)
	 */
	@Override
	public void acceptGroupChatNextInvitation(String chatId) {
		groupChatLog.acceptGroupChatNextInvitation(chatId);
	}

	/*
	 * (non-Javadoc)
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#
	 * updateGroupChatStatusAndReasonCode (java.lang.String, int, int)
	 */
	@Override
	public void updateGroupChatStateAndReasonCode(String chatId, int state, int reasonCode) {
		groupChatLog.updateGroupChatStateAndReasonCode(chatId, state, reasonCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#updateGroupChatParticipant(java.lang.String, java.util.Set)
	 */
	@Override
	public void updateGroupChatParticipant(String chatId, Set<ParticipantInfo> participants) {
		groupChatLog.updateGroupChatParticipant(chatId, participants);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#updateGroupChatRejoinIdOnSessionStart(java.lang.String, java.lang.String)
	 */
	@Override
	public void updateGroupChatRejoinIdOnSessionStart(String chatId, String rejoinId) {
		groupChatLog.updateGroupChatRejoinIdOnSessionStart(chatId, rejoinId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#getGroupChatInfo(java.lang.String)
	 */
	@Override
	public GroupChatInfo getGroupChatInfo(String chatId) {
		return groupChatLog.getGroupChatInfo(chatId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#getGroupChatConnectedParticipants(java.lang.String)
	 */
	@Override
	public Set<ParticipantInfo> getGroupChatConnectedParticipants(String chatId) {
		return groupChatLog.getGroupChatConnectedParticipants(chatId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.orangelabs.rcs.provider.messaging.IMessageLog#addSpamMessage(com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage)
	 */
	@Override
	public void addSpamMessage(InstantMessage msg) {
		messageLog.addSpamMessage(msg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.orangelabs.rcs.provider.messaging.IMessageLog#addIncomingOneToOneChatMessage(com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage))
	 */
	@Override
	public void addIncomingOneToOneChatMessage(InstantMessage msg) {
		messageLog.addIncomingOneToOneChatMessage(msg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.orangelabs.rcs.provider.messaging.IMessageLog#addOutgoingOneToOneChatMessage(com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage,
	 * int,int)
	 */
	@Override
	public void addOutgoingOneToOneChatMessage(InstantMessage msg, int status, int reasonCode) {
		messageLog.addOutgoingOneToOneChatMessage(msg, status, reasonCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#addGroupChatMessage(java.lang.String,
	 * com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage, int, int, int)
	 */
	@Override
	public void addGroupChatMessage(String chatId, InstantMessage msg, int direction, int status, int reasonCode) {
		messageLog.addGroupChatMessage(chatId, msg, direction, status, reasonCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#addGroupChatSystemMessage(java.lang.String, java.lang.String, int)
	 */
	@Override
	public void addGroupChatSystemMessage(String chatId, ContactId contact, int status) {
		messageLog.addGroupChatSystemMessage(chatId, contact, status);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#markMessageAsRead(java.lang.String)
	 */
	@Override
	public void markMessageAsRead(String msgId) {
		messageLog.markMessageAsRead(msgId);
	}

	/*
	 * (non-Javadoc)
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#
	 * updateChatMessageStatusAndReasonCode (java.lang.String, int, int)
	 */
	@Override
	public void updateChatMessageStatusAndReasonCode(String msgId, int status, int reasonCode) {
		messageLog.updateChatMessageStatusAndReasonCode(msgId, status, reasonCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#markIncomingChatMessageAsReceived(java.lang.String)
	 */
	@Override
	public void markIncomingChatMessageAsReceived(String msgId) {
		messageLog.markIncomingChatMessageAsReceived(msgId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#isMessagePersisted(java.lang.String)
	 */
	@Override
	public boolean isMessagePersisted(String msgId) {
		return messageLog.isMessagePersisted(msgId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#addFileTransfer(java.lang.String, java.lang.String, int,
	 * com.orangelabs.rcs.core.content.MmContent, com.orangelabs.rcs.core.content.MmContent, int, int)
	 */
	@Override
	public void addFileTransfer(ContactId contact, String fileTransferId, int direction,
			MmContent content, MmContent fileIcon, int status, int reasonCode) {
		fileTransferLog.addFileTransfer(contact, fileTransferId, direction, content, fileIcon,
				status, reasonCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#addOutgoingGroupFileTransfer(java.lang.String, java.lang.String,
	 * com.orangelabs.rcs.core.content.MmContent, com.orangelabs.rcs.core.content.MmContent)
	 */
	@Override
	public void addOutgoingGroupFileTransfer(String chatId, String fileTransferId, MmContent content, MmContent thumbnail) {
		fileTransferLog.addOutgoingGroupFileTransfer(chatId, fileTransferId, content, thumbnail);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#addIncomingGroupFileTransfer(java.lang.String, java.lang.String,
	 * java.lang.String, com.orangelabs.rcs.core.content.MmContent, com.orangelabs.rcs.core.content.MmContent, int, int)
	 */
	@Override
	public void addIncomingGroupFileTransfer(String chatId, ContactId contact, String fileTransferId, MmContent content,
			MmContent fileIcon, int state, int reasonCode) {
		fileTransferLog.addIncomingGroupFileTransfer(chatId, contact, fileTransferId, content,
				fileIcon, state, reasonCode);
	}

	/*
	 * (non-Javadoc)
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#
	 * updateFileTransferStateAndReasonCode(java.lang.String, int, int
	 */
	@Override
	public void updateFileTransferStateAndReasonCode(String fileTransferId, int state,
			int reasonCode) {
		fileTransferLog.updateFileTransferStateAndReasonCode(fileTransferId, state, reasonCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#markFileTransferAsRead(java.lang.String)
	 */
	@Override
	public void markFileTransferAsRead(String fileTransferId) {
		fileTransferLog.markFileTransferAsRead(fileTransferId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#updateFileTransferProgress(java.lang.String, long)
	 */
	@Override
	public void updateFileTransferProgress(String fileTransferId, long currentSize) {
		fileTransferLog.updateFileTransferProgress(fileTransferId, currentSize);
	}

	/* (non-Javadoc)
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#updateFileTransferred(java.lang.String, com.orangelabs.rcs.core.content.MmContent)
	 */
	@Override
	public void updateFileTransferred(String fileTransferId, MmContent content) {
		fileTransferLog.updateFileTransferred(fileTransferId, content);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#isFileTransfer(java.lang.String)
	 */
	@Override
	public boolean isFileTransfer(String fileTransferId) {
		return fileTransferLog.isFileTransfer(fileTransferId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#updateFileTransferChatId(java.lang.String, java.lang.String)
	 */
	@Override
	public void updateFileTransferChatId(String fileTransferId, String chatId) {
		fileTransferLog.updateFileTransferChatId(fileTransferId, chatId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#isGroupChatNextInviteRejected(java.lang.String)
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
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatDeliveryInfoLog#addGroupChatDeliveryInfoEntry(java.lang.String,
	 * com.gsma.services.rcs.contacts.ContactId, java.lang.String)
	 */
	@Override
	public Uri addGroupChatDeliveryInfoEntry(String chatId, ContactId contact, String msgId) {
		return groupChatDeliveryInfoLog.addGroupChatDeliveryInfoEntry(chatId, contact, msgId);
	}

	/*
	 * (non-Javadoc)
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatDeliveryInfoLog#
	 * updateGroupChatDeliveryInfoStatusAndReasonCode(java.lang.String, com.gsma.services.rcs.contacts.ContactId),
	 * int, int)
	 */
	@Override
	public void updateGroupChatDeliveryInfoStatusAndReasonCode(String msgId, ContactId contact,
			int status, int reasonCode) {
		groupChatDeliveryInfoLog.updateGroupChatDeliveryInfoStatusAndReasonCode(msgId, contact,
				status, reasonCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatDeliveryInfoLog#isDeliveredToAllRecipients(java.lang.String)
	 */
	@Override
	public boolean isDeliveredToAllRecipients(String msgId) {
		return groupChatDeliveryInfoLog.isDeliveredToAllRecipients(msgId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatDeliveryInfoLog#isDisplayedByAllRecipients(java.lang.String)
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
}
