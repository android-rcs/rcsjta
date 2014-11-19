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
package com.orangelabs.rcs.ri.sharing.image;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.gsma.services.rcs.ish.ImageSharingLog;

/**
 * Image Sharing Data Object
 * 
 * @author YPLO6403
 * 
 */
public class ImageSharingDAO implements Parcelable {

	private String sharingId;
	private ContactId contact;
	private Uri file;
	private String filename;
	private String mimeType;
	private int state;
	private int direction;
	private long timestamp;
	private long sizeTransferred;
	private long size;
	private int reasonCode;

	private static final String WHERE_CLAUSE = new StringBuilder(ImageSharingLog.SHARING_ID).append("=?").toString();

	/**
	 * Constructor
	 * 
	 * @param source
	 *            Parcelable source
	 */
	public ImageSharingDAO(Parcel source) {
		sharingId = source.readString();
		boolean containsContactId = source.readInt() != 0;
		if (containsContactId) {
			contact = ContactId.CREATOR.createFromParcel(source);
		} else {
			contact = null;
		}
		boolean containsFile = source.readInt() != 0;
		if (containsFile) {
			file = Uri.parse(source.readString());
		} else {
			file = null;
		}
		filename = source.readString();
		mimeType = source.readString();
		state = source.readInt();
		direction = source.readInt();
		timestamp = source.readLong();
		sizeTransferred = source.readLong();
		size = source.readLong();
		reasonCode = source.readInt();
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
		if (file != null) {
			dest.writeInt(1);
			dest.writeString(file.toString());
		} else {
			dest.writeInt(0);
		}
		dest.writeString(filename);
		dest.writeString(mimeType);
		dest.writeInt(state);
		dest.writeInt(direction);
		dest.writeLong(timestamp);
		dest.writeLong(sizeTransferred);
		dest.writeLong(size);
		dest.writeInt(reasonCode);
	};

	public int getState() {
		return state;
	}

	public long getSizeTransferred() {
		return sizeTransferred;
	}

	public String getSharingId() {
		return sharingId;
	}

	public ContactId getContact() {
		return contact;
	}

	public Uri getFile() {
		return file;
	}

	public String getFilename() {
		return filename;
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

	public long getSize() {
		return size;
	}

	public int getReasonCode() {
		return reasonCode;
	}

	/**
	 * Construct the Image Sharing data object from the provider
	 * <p>
	 * Note: to change with CR025 (enums)
	 * 
	 * @param context
	 * @param sharingId
	 *            the unique key field
	 * @throws Exception
	 */
	public ImageSharingDAO(final Context context, final String sharingId) throws Exception {
		Uri uri = ImageSharingLog.CONTENT_URI;
		String[] whereArgs = new String[] { sharingId };
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(uri, null, WHERE_CLAUSE, whereArgs, null);
			if (cursor.moveToFirst()) {
				this.sharingId = sharingId;
				String _contact = cursor.getString(cursor.getColumnIndexOrThrow(ImageSharingLog.CONTACT));
				if (_contact != null) {
					ContactUtils contactUtils = ContactUtils.getInstance(context);
					contact = contactUtils.formatContact(_contact);
				}
				file = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(ImageSharingLog.FILE)));
				filename = cursor.getString(cursor.getColumnIndexOrThrow(ImageSharingLog.FILENAME));
				mimeType = cursor.getString(cursor.getColumnIndexOrThrow(ImageSharingLog.MIME_TYPE));
				state = cursor.getInt(cursor.getColumnIndexOrThrow(ImageSharingLog.STATE));
				direction = cursor.getInt(cursor.getColumnIndexOrThrow(ImageSharingLog.DIRECTION));
				timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(ImageSharingLog.TIMESTAMP));
				sizeTransferred = cursor.getLong(cursor.getColumnIndexOrThrow(ImageSharingLog.TRANSFERRED));
				size = cursor.getLong(cursor.getColumnIndexOrThrow(ImageSharingLog.FILESIZE));
				reasonCode = cursor.getInt(cursor.getColumnIndexOrThrow(ImageSharingLog.REASON_CODE));
			} else {
				throw new IllegalArgumentException("Sharing ID not found");
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
		return "ImageSharingDAO [sharingId=" + sharingId + ", contact=" + contact + ", file=" + file + ", filename=" + filename
				+ ", mimeType=" + mimeType + ", state=" + state + ", size=" + size + "]";
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<ImageSharingDAO> CREATOR = new Parcelable.Creator<ImageSharingDAO>() {
		@Override
		public ImageSharingDAO createFromParcel(Parcel in) {
			return new ImageSharingDAO(in);
		}

		@Override
		public ImageSharingDAO[] newArray(int size) {
			return new ImageSharingDAO[size];
		}
	};

}
