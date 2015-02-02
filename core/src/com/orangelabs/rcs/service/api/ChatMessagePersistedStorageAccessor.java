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

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.utils.ContactUtils;

import android.database.Cursor;

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

	private boolean mRead;

	private Direction mDirection;

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
			long timestamp, Direction direction) {
		mMessagingLog = messagingLog;
		mId = id;
		mRemoteContact = remoteContact;
		mContent = content;
		mChatId = chatId;
		mMimeType = mimeType;
		mTimestamp = timestamp;
		mDirection = direction;
	}

	private void cacheData() {
		Cursor cursor = null;
		try {
			cursor = mMessagingLog.getCacheableChatMessageData(mId);
			String contact = cursor.getString(cursor
					.getColumnIndexOrThrow(Message.CONTACT));
			if (contact != null) {
				mRemoteContact = ContactUtils.createContactId(contact);
			}
			mDirection = Direction.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(Message.DIRECTION)));
			mContent = cursor.getString(cursor.getColumnIndexOrThrow(Message.CONTENT));
			mChatId = cursor.getString(cursor.getColumnIndexOrThrow(Message.CHAT_ID));
			mMimeType = cursor.getString(cursor.getColumnIndexOrThrow(Message.MIME_TYPE));
			mTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Message.TIMESTAMP));
			if (!mRead) {
			    mRead = ReadStatus.READ.toInt() == cursor.getInt(cursor
			            .getColumnIndexOrThrow(Message.READ_STATUS));
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public String getId() {
		return mId;
	}

	public ContactId getRemoteContact() {
		if (mRemoteContact == null) {
			cacheData();
		}
		return mRemoteContact;
	}

	public String getContent() {
		if (mContent == null) {
			cacheData();
		}
		return mContent;
	}

	public String getMimeType() {
		if (mMimeType == null) {
			cacheData();
		}
		return mMimeType;
	}

	public Direction getDirection() {
		if (mDirection == null) {
			cacheData();
		}
		return mDirection;
	}

	public long getTimestamp() {
		if (mDirection == null) {
			cacheData();
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
			cacheData();
		}
		return mChatId;
	}

	public boolean isRead() {
		/*
		 * No need to read from provider unless incoming and not already marked
		 * as read.
		 */
		if (Direction.INCOMING == mDirection && !mRead) {
			cacheData();
		}
		return mRead;
	}
}
