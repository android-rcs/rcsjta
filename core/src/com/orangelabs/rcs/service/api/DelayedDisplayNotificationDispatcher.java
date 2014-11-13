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

import android.content.ContentResolver;
import android.database.Cursor;

import com.gsma.services.rcs.RcsCommon.ReadStatus;
import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.orangelabs.rcs.utils.ContactUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Delayed Display Notification Dispatcher retrieves those text messages for
 * which requested display reports have not yet been successfully sent.
 */
public class DelayedDisplayNotificationDispatcher implements Runnable {

	private final static String SELECTION_READ_CHAT_MESSAGES_WITH_DISPLAY_REPORT_REQUESTED = new StringBuilder(
			ChatLog.Message.CHAT_ID).append("=").append(ChatLog.Message.CONTACT).append(" AND ")
			.append(ChatLog.Message.MIME_TYPE).append(" IN('").append(MimeType.TEXT_MESSAGE)
			.append("','").append(MimeType.GEOLOC_MESSAGE).append("') AND ")
			.append(ChatLog.Message.READ_STATUS).append("=").append(ReadStatus.READ)
			.append(" AND ").append(ChatLog.Message.MESSAGE_STATUS).append("=")
			.append(ChatLog.Message.Status.Content.DISPLAY_REPORT_REQUESTED).toString();

	private static final String ORDER_BY_TIMESTAMP_ASC = ChatLog.Message.TIMESTAMP.concat(" ASC");

	private static Logger logger = Logger.getLogger(DelayedDisplayNotificationDispatcher.class.getName());

	private ContentResolver mContentResolver;

	private ChatServiceImpl mChatApi;

	/* package private */DelayedDisplayNotificationDispatcher(ContentResolver contentResolver,
			ChatServiceImpl chatApi) {
		mContentResolver = contentResolver;
		mChatApi = chatApi;
	}

	@Override
	public void run() {
		Cursor cursor = null;
		try {
			String[] projection = new String[] { ChatLog.Message.MESSAGE_ID, ChatLog.Message.CONTACT };
			cursor = mContentResolver.query(ChatLog.Message.CONTENT_URI, projection,
					SELECTION_READ_CHAT_MESSAGES_WITH_DISPLAY_REPORT_REQUESTED, null, ORDER_BY_TIMESTAMP_ASC);
			while (cursor.moveToNext()) {
				String msgId = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.Message.MESSAGE_ID));
				String contactNumber = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.Message.CONTACT));
				try {
					mChatApi.tryToSendOne2OneDisplayedDeliveryReport(msgId, ContactUtils.createContactId(contactNumber));
				} catch (RcsContactFormatException e) {
					if (logger.isActivated())  {
						logger.error( "Cannot parse contact "+contactNumber);
					}
				}
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Could not retrieve messages for which delayed display notification should be send!", e);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
}
