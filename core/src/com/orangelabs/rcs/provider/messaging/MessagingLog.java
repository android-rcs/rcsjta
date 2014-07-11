/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.provider.messaging;

import java.util.Set;

import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

/**
 * Class to interface the Instant Messaging tables
 * 
 * @author LEMORDANT Philippe
 * 
 */
public class MessagingLog implements IGroupChatLog, IMessageLog, IFileTransferLog, IGroupChatDeliveryInfoLog {
	/**
	 * Current instance
	 */
	private static MessagingLog instance = null;

	/**
	 * Content resolver
	 */
	private ContentResolver contentResolver;

	private GroupChatLog groupChatLog;

	private MessageLog messageLog;

	private FileTransferLog fileTransferLog;

	private GroupChatDeliveryInfoLog groupChatDeliveryInfoLog;

	/**
	 * Create instance
	 * 
	 * @param ctx
	 *            Context
	 */
	public static synchronized void createInstance(Context ctx) {
		if (instance == null) {
			instance = new MessagingLog(ctx);
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
	 * @param ctx
	 *            Application context
	 */
	private MessagingLog(Context ctx) {
		contentResolver = ctx.getContentResolver();
		groupChatLog = new GroupChatLog(ctx);
		groupChatDeliveryInfoLog = new GroupChatDeliveryInfoLog(contentResolver);
		messageLog = new MessageLog(contentResolver, groupChatLog, groupChatDeliveryInfoLog);
		fileTransferLog = new FileTransferLog(contentResolver, groupChatLog, groupChatDeliveryInfoLog);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#addGroupChat(java.lang.String, java.lang.String, java.util.Set, int,
	 * int)
	 */
	@Override
	public void addGroupChat(String chatId, String subject, Set<ParticipantInfo> participants, int status, int direction) {
		groupChatLog.addGroupChat(chatId, subject, participants, status, direction);
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
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#updateGroupChatStatus(java.lang.String, int)
	 */
	@Override
	public void updateGroupChatStatus(String chatId, int status) {
		groupChatLog.updateGroupChatStatus(chatId, status);
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
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#updateGroupChatRejoinId(java.lang.String, java.lang.String)
	 */
	@Override
	public void updateGroupChatRejoinId(String chatId, String rejoingId) {
		groupChatLog.updateGroupChatRejoinId(chatId, rejoingId);
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
	 * com.orangelabs.rcs.provider.messaging.IMessageLog#addChatMessage(com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage,
	 * int)
	 */
	@Override
	public void addChatMessage(InstantMessage msg, int direction) {
		messageLog.addChatMessage(msg, direction);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#addGroupChatMessage(java.lang.String,
	 * com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage, int)
	 */
	@Override
	public void addGroupChatMessage(String chatId, InstantMessage msg, int direction) {
		messageLog.addGroupChatMessage(chatId, msg, direction);
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
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#updateChatMessageStatus(java.lang.String, int)
	 */
	@Override
	public void updateChatMessageStatus(String msgId, int status) {
		messageLog.updateChatMessageStatus(msgId, status);
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
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#updateOutgoingChatMessageDeliveryStatus(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void updateOutgoingChatMessageDeliveryStatus(String msgId, String status) {
		messageLog.updateOutgoingChatMessageDeliveryStatus(msgId, status);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#updateChatMessageDeliveryStatus(java.lang.String, java.lang.String)
	 */
	@Override
	public void updateChatMessageDeliveryStatus(String msgId, String status) {
		messageLog.updateChatMessageDeliveryStatus(msgId, status);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#isNewMessage(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean isNewMessage(String chatId, String msgId) {
		return messageLog.isNewMessage(chatId, msgId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#addFileTransfer(java.lang.String, java.lang.String, int,
	 * com.orangelabs.rcs.core.content.MmContent, com.orangelabs.rcs.core.content.MmContent)
	 */
	@Override
	public void addFileTransfer(ContactId contact, String fileTransferId, int direction, MmContent content, MmContent thumbnail) {
		fileTransferLog.addFileTransfer(contact, fileTransferId, direction, content, thumbnail);
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
	 * java.lang.String, com.orangelabs.rcs.core.content.MmContent, com.orangelabs.rcs.core.content.MmContent)
	 */
	@Override
	public void addIncomingGroupFileTransfer(String chatId, ContactId contact, String fileTransferId, MmContent content,
			MmContent thumbnail) {
		fileTransferLog.addIncomingGroupFileTransfer(chatId, contact, fileTransferId, content, thumbnail);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#updateFileTransferStatus(java.lang.String, int)
	 */
	@Override
	public void updateFileTransferStatus(String fileTransferId, int status) {
		fileTransferLog.updateFileTransferStatus(fileTransferId, status);
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
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#updateFileTransferProgress(java.lang.String, long, long)
	 */
	@Override
	public void updateFileTransferProgress(String fileTransferId, long size, long totalSize) {
		fileTransferLog.updateFileTransferProgress(fileTransferId, size, totalSize);
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
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#setChatMessageDeliveryRequested(java.lang.String)
	 */
	@Override
	public void setChatMessageDeliveryRequested(String msgId) {
		messageLog.setChatMessageDeliveryRequested(msgId);
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
		contentResolver.delete(ChatData.CONTENT_URI, null, null);
		contentResolver.delete(MessageData.CONTENT_URI, null, null);
		contentResolver.delete(FileTransferData.CONTENT_URI, null, null);
		contentResolver.delete(GroupChatDeliveryInfoData.CONTENT_URI, null, null);
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatDeliveryInfoLog#addGroupChatDeliveryInfoEntry(java.lang.String,
	 * java.lang.String, com.gsma.services.rcs.contacts.ContactId)
	 */
	@Override
	public Uri addGroupChatDeliveryInfoEntry(String chatId, String msgId, ContactId contact) {
		return groupChatDeliveryInfoLog.addGroupChatDeliveryInfoEntry(chatId, msgId, contact);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatDeliveryInfoLog#updateGroupChatDeliveryInfoStatus(java.lang.String,
	 * java.lang.String, com.gsma.services.rcs.contacts.ContactId)
	 */
	@Override
	public void updateGroupChatDeliveryInfoStatus(String msgId, String status, ContactId contact) {
		groupChatDeliveryInfoLog.updateGroupChatDeliveryInfoStatus(msgId, status, contact);
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
	public boolean isNewMessage(ContactId contact, String msgId) {
		return messageLog.isNewMessage(contact, msgId);
	}
}
