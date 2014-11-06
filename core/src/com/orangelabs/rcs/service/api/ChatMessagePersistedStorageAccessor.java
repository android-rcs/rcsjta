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

package com.orangelabs.rcs.service.api;

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.provider.messaging.MessagingLog;

/**
 * ChatMessagePersistedStorageAccessor helps in retrieving persisted data
 * related to a chat message from the persisted storage. It can utilize caching
 * for such data that will not be changed after creation of the group chat to
 * speed up consecutive access.
 */
public class ChatMessagePersistedStorageAccessor {

	private final MessagingLog mMessagingLog;

	private final String mId;

	private ContactId mRemoteContact;

	private String mContent;

	private String mMimeType;

	private String mChatId;

	private Long mTimestamp;

	private Integer mDirection;

	/**
	 * Constructor for outgoing message
	 * 
	 * @param messagingLog MessagingLog
	 * @param id Message Id
	 */
	public ChatMessagePersistedStorageAccessor(MessagingLog messagingLog, String id) {
		mMessagingLog = messagingLog;
		mId = id;
	}

	/**
	 * Constructor for outgoing message
	 * 
	 * @param messagingLog MessagingLog
	 * @param id Message Id
	 * @param remoteContact Contact Id
	 * @param content Message content
	 * @param mimeType Mime type
	 * @param chatId Chat ID
	 * @param timestamp Time-stamp
	 * @param direction Direction
	 */
	public ChatMessagePersistedStorageAccessor(MessagingLog messagingLog, String id,
			ContactId remoteContact, String content, String mimeType, String chatId,
			long timestamp, int direction) {
		mMessagingLog = messagingLog;
		mId = id;
		mRemoteContact = remoteContact;
		mContent = content;
		mChatId = chatId;
		mMimeType = mimeType;
		mTimestamp = timestamp;
		mDirection = direction;
	}

	public String getId() {
		return mId;
	}

	public ContactId getRemoteContact() {
		if (mRemoteContact == null) {
			mRemoteContact = mMessagingLog.getFileTransferRemoteContact(mId);
		}
		return mRemoteContact;
	}

	public String getContent() {
		if (mContent == null) {
			mContent = mMessagingLog.getMessageContent(mId);
		}
		return mContent;
	}

	public String getMimeType() {
		if (mMimeType == null) {
			mMimeType = mMessagingLog.getMessageMimeType(mId);
		}
		return mMimeType;
	}

	public int getDirection() {
		if (mDirection == null) {
			mDirection = mMessagingLog.getMessageDirection(mId);
		}
		return mDirection;
	}

	public long getTimestamp() {
		if (mDirection == null) {
			mTimestamp = mMessagingLog.getMessageTimestamp(mId);
		}
		return mTimestamp;
	}

	public long getTimestampSent() {
		return mMessagingLog.getMessageSentTimestamp(mId);
	}

	public long getTimestampDelivered() {
		return mMessagingLog.getMessageDeliveredTimestamp(mId);
	}

	public long getTimestampDisplayed() {
		return mMessagingLog.getMessageDisplayedTimestamp(mId);
	}

	public int getStatus() {
		return mMessagingLog.getMessageStatus(mId);
	}

	public int getReasonCode() {
		return mMessagingLog.getMessageReasonCode(mId);
	}

	public String getChatId() {
		if (mChatId == null) {
			mChatId = mMessagingLog.getMessageChatId(mId);
		}
		return mChatId;
	}

	public boolean isRead() {
		return mMessagingLog.isMessageRead(mId);
	}
}
