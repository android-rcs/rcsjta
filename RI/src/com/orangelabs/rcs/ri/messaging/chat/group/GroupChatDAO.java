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

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog;

/**
 * Group CHAT Data Object
 * 
 * @author YPLO6403
 * 
 */
public class GroupChatDAO implements Parcelable {

	private String mChatId;
	
	private Direction mDirection;
	
	private String mParticipants;
	
	private int mState;
	
	private String mSubject;
	
	private long mTimestamp;
	
	private int mReasonCode;

	private static final String WHERE_CLAUSE = ChatLog.GroupChat.CHAT_ID.concat("=?");

	public int getState() {
		return mState;
	}

	public String getChatId() {
		return mChatId;
	}

	public String getParticipants() {
		return mParticipants;
	}

	public String getSubject() {
		return mSubject;
	}

	public Direction getDirection() {
		return mDirection;
	}

	public long getTimestamp() {
		return mTimestamp;
	}

	public int getReasonCode() {
		return mReasonCode;
	}

	/**
	 * Constructor
	 * 
	 * @param source
	 *            Parcelable source
	 */
	public GroupChatDAO(Parcel source) {
		mChatId = source.readString();
		mState = source.readInt();
		mDirection = Direction.valueOf(source.readInt());
		mTimestamp = source.readLong();
		mSubject = source.readString();
		mParticipants = source.readString();
		mReasonCode = source.readInt();
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mChatId);
		dest.writeInt(mState);
		dest.writeInt(mDirection.toInt());
		dest.writeLong(mTimestamp);
		dest.writeString(mSubject);
		dest.writeString(mParticipants);
		dest.writeInt(mReasonCode);
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
			if (!cursor.moveToFirst()) {
				throw new IllegalArgumentException("ChatId not found");
			}
			this.mChatId = chatId;
			mSubject = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.GroupChat.SUBJECT));
			mState = cursor.getInt(cursor.getColumnIndexOrThrow(ChatLog.GroupChat.STATE));
			mDirection = Direction.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(ChatLog.GroupChat.DIRECTION)));
			mTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(ChatLog.GroupChat.TIMESTAMP));
			mParticipants = cursor.getString(cursor
					.getColumnIndexOrThrow(ChatLog.GroupChat.PARTICIPANTS));
			mReasonCode = cursor.getInt(cursor.getColumnIndexOrThrow(ChatLog.GroupChat.REASON_CODE));
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
		return "GroupChatDAO [chatId=" + mChatId + ", direction=" + mDirection + ", state=" + mState + ", subject=" + mSubject + "]";
	}
	
	
}
