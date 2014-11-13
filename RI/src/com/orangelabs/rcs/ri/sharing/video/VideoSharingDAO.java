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
package com.orangelabs.rcs.ri.sharing.video;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.gsma.services.rcs.vsh.VideoSharingLog;

/**
 * Video Sharing Data Object
 * 
 * @author YPLO6403
 * 
 */
public class VideoSharingDAO implements Parcelable {

	private String sharingId;
	private ContactId contact;
	private int state;
	private int direction;
	private long timestamp;
	private long duration;

	private static final String WHERE_CLAUSE = new StringBuilder(VideoSharingLog.SHARING_ID).append("=?").toString();

	/**
	 * Constructor
	 * 
	 * @param source
	 *            Parcelable source
	 */
	public VideoSharingDAO(Parcel source) {
		sharingId = source.readString();
		boolean containsContactId = source.readInt() != 0;
		if (containsContactId) {
			contact = ContactId.CREATOR.createFromParcel(source);
		} else {
			contact = null;
		}
		state = source.readInt();
		direction = source.readInt();
		timestamp = source.readLong();
		duration = source.readLong();
	}
	
	public int getState() {
		return state;
	}

	public String getSharingId() {
		return sharingId;
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

	public long getDuration() {
		return duration;
	}

	/**
	 * Construct the Video Sharing data object from the provider
	 * <p>
	 * Note: to change with CR025 (enums)
	 * 
	 * @param contentResolver
	 * @param sharingId
	 *            the unique key field
	 * @throws Exception
	 */
	public VideoSharingDAO(final Context context, final String sharingId) throws Exception {
		Uri uri = VideoSharingLog.CONTENT_URI;
		String[] whereArgs = new String[] { sharingId };
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(uri, null, WHERE_CLAUSE, whereArgs, null);
			if (cursor.moveToFirst()) {
				this.sharingId = sharingId;
				String _contact = cursor.getString(cursor.getColumnIndexOrThrow(VideoSharingLog.CONTACT));
				if (_contact != null) {
					ContactUtils contactUtils = ContactUtils.getInstance(context);
					contact = contactUtils.formatContact(_contact);
				}
				state = cursor.getInt(cursor.getColumnIndexOrThrow(VideoSharingLog.STATE));
				direction = cursor.getInt(cursor.getColumnIndexOrThrow(VideoSharingLog.DIRECTION));
				timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(VideoSharingLog.TIMESTAMP));
				duration = cursor.getLong(cursor.getColumnIndexOrThrow(VideoSharingLog.DURATION));
			} else {
				throw new Exception("Sharing ID not found" );
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
	public String toString() {
		return "VideoSharingDAO [sharingId=" + sharingId + ", contact=" + contact + ", state=" + state + ", duration=" + duration
				+ "]";
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(sharingId);
		if (contact != null) {
			dest.writeInt(1);
			contact.writeToParcel(dest, flags);
		} else {
			dest.writeInt(0);
		}
		dest.writeInt(state);
		dest.writeInt(direction);
		dest.writeLong(timestamp);
		dest.writeLong(duration);
	};

	public static final Parcelable.Creator<VideoSharingDAO> CREATOR = new Parcelable.Creator<VideoSharingDAO>() {
		@Override
		public VideoSharingDAO createFromParcel(Parcel in) {
			return new VideoSharingDAO(in);
		}

		@Override
		public VideoSharingDAO[] newArray(int size) {
			return new VideoSharingDAO[size];
		}
	};
}
