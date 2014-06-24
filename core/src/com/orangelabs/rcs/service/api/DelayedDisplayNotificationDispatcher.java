/*
 * Copyright (C) 2014 Sony Mobile Communications AB.
 * All rights, including trade secret rights, reserved.
 */
package com.orangelabs.rcs.service.api;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.utils.logger.Logger;

import android.content.ContentResolver;
import android.database.Cursor;

/**
 * Delayed Display Notification Dispatcher retrieves those text messages for
 * which requested display reports have not yet been successfully sent.
 */
public class DelayedDisplayNotificationDispatcher implements Runnable {

	private final static String SELECTION_READ_ONE2ONE_TEXT_AND_GEOLOC_MESSAGES_WITH_DISPLAY_REPORT_REQUESTED = new StringBuilder(
			ChatLog.Message.CHAT_ID).append("=").append(ChatLog.Message.CONTACT_NUMBER)
			.append(" AND ").append(ChatLog.Message.MESSAGE_TYPE).append("=")
			.append(ChatLog.Message.Type.CONTENT).append(" AND ").append(ChatLog.Message.MIME_TYPE)
			.append(" IN('").append(ChatMessage.MIME_TYPE).append("','")
			.append(GeolocMessage.MIME_TYPE).append("') AND ")
			.append(ChatLog.Message.READ_STATUS).append("=")
			.append(ChatLog.Message.ReadStatus.READ).append(" AND ")
			.append(ChatLog.Message.MESSAGE_STATUS).append("=")
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
			cursor = mContentResolver.query(ChatLog.Message.CONTENT_URI, new String[] {
					ChatLog.Message.MESSAGE_ID, ChatLog.Message.CONTACT_NUMBER
			}, SELECTION_READ_ONE2ONE_TEXT_AND_GEOLOC_MESSAGES_WITH_DISPLAY_REPORT_REQUESTED, null,
					ORDER_BY_TIMESTAMP_ASC);
			while (cursor.moveToNext()) {
				String msgId = cursor.getString(cursor
						.getColumnIndexOrThrow(ChatLog.Message.MESSAGE_ID));
				String contactNumber = cursor.getString(cursor
						.getColumnIndexOrThrow(ChatLog.Message.CONTACT_NUMBER));
				mChatApi.tryToSendOne2OneDisplayedDeliveryReport(msgId, contactNumber);
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error(
						"Could not retrieve messages for which delayed display notification should be send!",
						e);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
}
