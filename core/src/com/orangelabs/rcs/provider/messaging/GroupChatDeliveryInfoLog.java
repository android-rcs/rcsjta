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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.gsma.services.rcs.DeliveryInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.provider.messaging.DeliveryInfoStatusAndReasonCode;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Class to interface the deliveryinfo table
 * 
 */
public class GroupChatDeliveryInfoLog implements IGroupChatDeliveryInfoLog {

	/**
	 * Delivery info database URI
	 */
	private Uri GroupChatDeliveryInfoDatabaseUri = GroupChatDeliveryInfoData.CONTENT_URI;

	private static final String SELECTION_DELIVERY_INFO_BY_MSG_ID_AND_CONTACT = new StringBuilder(
			GroupChatDeliveryInfoData.KEY_MSG_ID).append("=? AND ").append(GroupChatDeliveryInfoData.KEY_CONTACT).append("=?")
			.toString();

	private static final String SELECTION_CONTACTS_NOT_RECEIVED_MESSAGE = new StringBuilder(
			GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS).append("=")
			.append(DeliveryInfo.Status.NOT_DELIVERED).append(" OR (")
			.append(GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS).append("=")
			.append(DeliveryInfo.Status.FAILED).append(" AND ")
			.append(GroupChatDeliveryInfoData.KEY_REASON_CODE).append(" IN (")
			.append(DeliveryInfo.ReasonCode.FAILED_DELIVERY).append(",")
			.append(DeliveryInfo.ReasonCode.FAILED_DISPLAY).append("))").toString();

	/**
	 * Content resolver
	 */
	private ContentResolver cr;

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(GroupChatDeliveryInfoLog.class.getSimpleName());

	/**
	 * Constructor
	 * 
	 * @param cr
	 *            Content resolver
	 */
	/* package private */GroupChatDeliveryInfoLog(ContentResolver cr) {
		this.cr = cr;
	}

	@Override
	public Uri addGroupChatDeliveryInfoEntry(String chatId, String msgId, ContactId contact) {
		if (logger.isActivated()) {
			logger.debug("Add new entry: chatID=" + chatId + ", messageID=" + msgId + ", contact=" + contact);
		}
		ContentValues values = new ContentValues();
		values.put(GroupChatDeliveryInfoData.KEY_CHAT_ID, chatId);
		values.put(GroupChatDeliveryInfoData.KEY_MSG_ID, msgId);
		if (contact != null) {
			values.put(GroupChatDeliveryInfoData.KEY_CONTACT, contact.toString());
		}
		values.put(GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS, DeliveryInfo.Status.NOT_DELIVERED);
		values.put(GroupChatDeliveryInfoData.KEY_REASON_CODE, DeliveryInfo.ReasonCode.UNSPECIFIED);
		values.put(GroupChatDeliveryInfoData.KEY_TIMESTAMP_DELIVERED, 0);
		values.put(GroupChatDeliveryInfoData.KEY_TIMESTAMP_DISPLAYED, 0);
		return cr.insert(GroupChatDeliveryInfoDatabaseUri, values);
	}

	/**
	 * Set delivery status for outgoing group chat messages and files
	 * 
	 * @param msgID
	 *            Message ID
	 * @param status
	 *            Status
	 * @param reasonCode
	 *            Reason code
	 * @param contact
	 *            The contact ID for which the entry is to be updated
	 */
	public void updateGroupChatDeliveryInfoStatusAndReasonCode(String msgId, int status,
			int reasonCode, ContactId contact) {
		ContentValues values = new ContentValues();
		values.put(GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS, status);
		values.put(GroupChatDeliveryInfoData.KEY_TIMESTAMP_DELIVERED, System.currentTimeMillis());
		values.put(GroupChatDeliveryInfoData.KEY_REASON_CODE, reasonCode);
		String[] selectionArgs = new String[] {
				msgId, contact.toString()
		};
		if (cr.update(GroupChatDeliveryInfoDatabaseUri, values,
				SELECTION_DELIVERY_INFO_BY_MSG_ID_AND_CONTACT, selectionArgs) < 1) {
			/* TODO: Throw exception */
			if (logger.isActivated()) {
				logger.warn("There was not group chat delivery into for msgId '" + msgId
						+ "' and contact '" + contact + "' to update!");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatDeliveryInfoLog#isDeliveredToAllRecipients(java.lang.String)
	 */
	@Override
	public boolean isDeliveredToAllRecipients(String msgId) {
		Cursor cursor = null;
		try {
			cursor = cr.query(Uri.withAppendedPath(GroupChatDeliveryInfoData.CONTENT_MSG_URI, msgId), null,
					SELECTION_CONTACTS_NOT_RECEIVED_MESSAGE, null, null);
			return !cursor.moveToFirst();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatDeliveryInfoLog#isDisplayedByAllRecipients(java.lang.String)
	 */
	@Override
	public boolean isDisplayedByAllRecipients(String msgId) {
		Cursor cursor = null;
		try {
			cursor = cr.query(Uri.withAppendedPath(GroupChatDeliveryInfoData.CONTENT_MSG_URI, msgId), null,
					GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS + "!= " + DeliveryInfo.Status.DISPLAYED,
					null, null);
			return !cursor.moveToFirst();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
}
