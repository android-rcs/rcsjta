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
package com.orangelabs.rcs.ri.messaging.chat.group;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.gsma.services.rcs.chat.ChatLog;

/**
 * Group CHAT Data Object
 * 
 * @author YPLO6403
 * 
 */
public class GroupChatDAO implements Parcelable {

	private String chatId;
	
	private int direction;
	
	private String participants;
	
	private int state;
	
	private String subject;
	
	private long timestamp;
	
	private int reasonCode;

	private static final String WHERE_CLAUSE = new StringBuilder(ChatLog.GroupChat.CHAT_ID).append("=?").toString();

	public int getState() {
		return state;
	}

	public String getChatId() {
		return chatId;
	}

	public String getParticipants() {
		return participants;
	}

	public String getSubject() {
		return subject;
	}

	public int getDirection() {
		return direction;
	}

	public long getTimestamp() {
		return timestamp;
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
	public GroupChatDAO(Parcel source) {
		chatId = source.readString();
		state = source.readInt();
		direction = source.readInt();
		timestamp = source.readLong();
		subject = source.readString();
		participants = source.readString();
		reasonCode = source.readInt();
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(chatId);
		dest.writeInt(state);
		dest.writeInt(direction);
		dest.writeLong(timestamp);
		dest.writeString(subject);
		dest.writeString(participants);
		dest.writeInt(reasonCode);
	};

	/**
	 * Construct the Group CHAT data object from the provider
	 * <p>
	 * Note: to change with CR025 (enums)
	 * 
	 * @param context
	 * @param chatId
	 * @throws Exception
	 */
	public GroupChatDAO(final Context context, final String chatId) throws Exception {
		Uri uri = ChatLog.GroupChat.CONTENT_URI;
		String[] whereArgs = new String[] { chatId };
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(uri, null, WHERE_CLAUSE, whereArgs, null);
			if (cursor.moveToFirst()) {
				this.chatId = chatId;
				subject = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.GroupChat.SUBJECT));
				state = cursor.getInt(cursor.getColumnIndexOrThrow(ChatLog.GroupChat.STATE));
				direction = cursor.getInt(cursor.getColumnIndexOrThrow(ChatLog.GroupChat.DIRECTION));
				timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(ChatLog.GroupChat.TIMESTAMP));
				participants = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.GroupChat.PARTICIPANTS));
				reasonCode = cursor.getInt(cursor.getColumnIndexOrThrow(ChatLog.GroupChat.REASON_CODE));
			} else {
				throw new IllegalArgumentException("ChatId not found");
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

	public static final Parcelable.Creator<GroupChatDAO> CREATOR = new Parcelable.Creator<GroupChatDAO>() {
		@Override
		public GroupChatDAO createFromParcel(Parcel in) {
			return new GroupChatDAO(in);
		}

		@Override
		public GroupChatDAO[] newArray(int size) {
			return new GroupChatDAO[size];
		}
	};

	@Override
	public String toString() {
		return "GroupChatDAO [chatId=" + chatId + ", direction=" + direction + ", state=" + state + ", subject=" + subject + "]";
	}
	
	
}
