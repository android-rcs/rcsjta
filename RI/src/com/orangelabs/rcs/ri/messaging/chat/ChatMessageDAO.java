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
 ******************************************************************************/
package com.orangelabs.rcs.ri.messaging.chat;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;

/**
 * CHAT Message Data Object
 * 
 * @author YPLO6403
 * 
 */
public class ChatMessageDAO implements Parcelable {

	private String msgId;
	private ContactId contact;
	private String chatId;
	private int status;
	private int reasonCode;
	private int readStatus;
	private int direction;
	private String mimeType;
	private String content;
	private long timestamp;
	private long timestampSent;
	private long timestampDelivered;
	private long timestampDisplayed;

	private static final String WHERE_CLAUSE = new StringBuilder(ChatLog.Message.MESSAGE_ID).append("=?").toString();

	public int getStatus() {
		return status;
	}

	public String getMsgId() {
		return msgId;
	}

	public int getReadStatus() {
		return readStatus;
	}

	public long getTimestampSent() {
		return timestampSent;
	}

	public long getTimestampDelivered() {
		return timestampDelivered;
	}

	public long getTimestampDisplayed() {
		return timestampDisplayed;
	}

	public ContactId getContact() {
		return contact;
	}

	public String getChatId() {
		return chatId;
	}

	public String getMimeType() {
		return mimeType;
	}

	public int getDirection() {
		return direction;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getContent() {
		return content;
	}

	public int getReasonCode() {
		return reasonCode;
	}

	/**
	 * Constructor
	 * 
	 * @param source
	 *            Parcelable source
	 */
	public ChatMessageDAO(Parcel source) {
		msgId = source.readString();
		boolean containsContactId = source.readInt() != 0;
		if (containsContactId) {
			contact = ContactId.CREATOR.createFromParcel(source);
		} else {
			contact = null;
		}
		chatId = source.readString();
		mimeType = source.readString();
		content = source.readString();
		status = source.readInt();
		readStatus = source.readInt();
		direction = source.readInt();
		timestamp = source.readLong();
		timestampSent = source.readLong();
		timestampDelivered = source.readLong();
		timestampDisplayed = source.readLong();
		reasonCode = source.readInt();
	}

	/**
	 * Construct the CHAT Message data object from the provider
	 * <p>
	 * Note: to change with CR025 (enums)
	 * 
	 * @param context
	 * @param messageId
	 *            the unique key field
	 * @throws Exception
	 */
	public ChatMessageDAO(final Context context, final String messageId) throws Exception {
		Uri uri = ChatLog.Message.CONTENT_URI;
		String[] whereArgs = new String[] { messageId };
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(uri, null, WHERE_CLAUSE, whereArgs, null);
			if (cursor.moveToFirst()) {
				msgId = messageId;
				chatId = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.Message.CHAT_ID));
				String _contact = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.Message.CONTACT));
				if (_contact != null) {
					ContactUtils contactUtils = ContactUtils.getInstance(context);
					contact = contactUtils.formatContact(_contact);
				}
				mimeType = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.Message.MIME_TYPE));
				content = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.Message.CONTENT));
				status = cursor.getInt(cursor.getColumnIndexOrThrow(ChatLog.Message.STATUS));
				readStatus = cursor.getInt(cursor.getColumnIndexOrThrow(ChatLog.Message.READ_STATUS));
				direction = cursor.getInt(cursor.getColumnIndexOrThrow(ChatLog.Message.DIRECTION));
				timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(ChatLog.Message.TIMESTAMP));
				timestampSent = cursor.getLong(cursor.getColumnIndexOrThrow(ChatLog.Message.TIMESTAMP_SENT));
				timestampDelivered = cursor.getLong(cursor.getColumnIndexOrThrow(ChatLog.Message.TIMESTAMP_DELIVERED));
				timestampDisplayed = cursor.getLong(cursor.getColumnIndexOrThrow(ChatLog.Message.TIMESTAMP_DISPLAYED));
				reasonCode = cursor.getInt(cursor.getColumnIndexOrThrow(ChatLog.Message.REASON_CODE));
			} else {
				throw new IllegalArgumentException("messageId no found");
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(msgId);
		if (contact != null) {
			dest.writeInt(1);
			contact.writeToParcel(dest, flags);
		} else {
			dest.writeInt(0);
		}
		dest.writeString(chatId);
		dest.writeString(mimeType);
		dest.writeString(content);
		dest.writeInt(status);
		dest.writeInt(readStatus);
		dest.writeInt(direction);
		dest.writeLong(timestamp);
		dest.writeLong(timestampSent);
		dest.writeLong(timestampDelivered);
		dest.writeLong(timestampDisplayed);
		dest.writeInt(reasonCode);
	};

	public static final Parcelable.Creator<ChatMessageDAO> CREATOR = new Parcelable.Creator<ChatMessageDAO>() {
		@Override
		public ChatMessageDAO createFromParcel(Parcel in) {
			return new ChatMessageDAO(in);
		}

		@Override
		public ChatMessageDAO[] newArray(int size) {
			return new ChatMessageDAO[size];
		}
	};

	@Override
	public String toString() {
		return "ChatMessageDAO [msgId=" + msgId + ", contact=" + contact + ", chatId=" + chatId + ", direction=" + direction
				+ ", mimeType=" + mimeType + ", body='" + content + "']";
	}

}
