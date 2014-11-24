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
package com.orangelabs.rcs.ri.ipcall;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.gsma.services.rcs.ipcall.IPCallLog;

/**
 * IP Call Data Object
 * 
 * @author YPLO6403
 * 
 */
public class IPCallDAO implements Parcelable {

	private String callId;
	private ContactId contact;
	private int state;
	private int direction;
	private long timestamp;
	private int height;
	private int width;
	private String videoEncoding;
	private String audioEncoding;

	private static final String WHERE_CLAUSE = new StringBuilder(IPCallLog.CALL_ID).append("=?").toString();

	public int getState() {
		return state;
	}

	public ContactId getContact() {
		return contact;
	}

	public int getDirection() {
		return direction;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getAudioEncoding() {
		return audioEncoding;
	}

	public String getCallId() {
		return callId;
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	public String getVideoEncoding() {
		return videoEncoding;
	}

	/**
	 * Constructor
	 * 
	 * @param source
	 *            Parcelable source
	 */
	public IPCallDAO(Parcel source) {
		callId = source.readString();
		boolean containsContactId = source.readInt() != 0;
		if (containsContactId) {
			contact = ContactId.CREATOR.createFromParcel(source);
		} else {
			contact = null;
		}
		state = source.readInt();
		direction = source.readInt();
		timestamp = source.readLong();
		height = source.readInt();
		width = source.readInt();
		videoEncoding = source.readString();
		audioEncoding = source.readString();
	}
	
	/**
	 * Construct the IP Call data object from the provider
	 * <p>
	 * Note: to change with CR025 (enums)
	 * 
	 * @param context
	 * @param callId
	 *            the unique key field
	 * @throws Exception
	 */
	public IPCallDAO(final Context context, final String callId) throws Exception {
		Uri uri = IPCallLog.CONTENT_URI;
		String[] whereArgs = new String[] { callId };
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(uri, null, WHERE_CLAUSE, whereArgs, null);
			if (cursor.moveToFirst()) {
				this.callId = callId;
				String _contact = cursor.getString(cursor.getColumnIndexOrThrow(IPCallLog.CONTACT));
				if (_contact != null) {
					ContactUtils contactUtils = ContactUtils.getInstance(context);
					contact = contactUtils.formatContact(_contact);
				}
				state = cursor.getInt(cursor.getColumnIndexOrThrow(IPCallLog.STATE));
				direction = cursor.getInt(cursor.getColumnIndexOrThrow(IPCallLog.DIRECTION));
				timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(IPCallLog.TIMESTAMP));
				height = cursor.getInt(cursor.getColumnIndexOrThrow(IPCallLog.HEIGHT));
				width = cursor.getInt(cursor.getColumnIndexOrThrow(IPCallLog.WIDTH));
				videoEncoding = cursor.getString(cursor.getColumnIndexOrThrow(IPCallLog.VIDEO_ENCODING));
				audioEncoding = cursor.getString(cursor.getColumnIndexOrThrow(IPCallLog.AUDIO_ENCODING));
			} else {
				throw new IllegalArgumentException("Call ID not found");
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
		dest.writeString(callId);
		if (contact != null) {
			dest.writeInt(1);
			contact.writeToParcel(dest, flags);
		} else {
			dest.writeInt(0);
		}
		dest.writeInt(state);
		dest.writeInt(direction);
		dest.writeLong(timestamp);
		dest.writeInt(height);
		dest.writeInt(width);
		dest.writeString(videoEncoding);
		dest.writeString(audioEncoding);
	};
	
	public static final Parcelable.Creator<IPCallDAO> CREATOR = new Parcelable.Creator<IPCallDAO>() {
		@Override
		public IPCallDAO createFromParcel(Parcel in) {
			return new IPCallDAO(in);
		}

		@Override
		public IPCallDAO[] newArray(int size) {
			return new IPCallDAO[size];
		}
	};

	@Override
	public String toString() {
		return "IPCallDAO [callId=" + callId + ", contact=" + contact + ", state=" + state + ", direction=" + direction + "]";
	}
	
}
