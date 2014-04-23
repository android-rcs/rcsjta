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

package com.gsma.services.rcs.chat;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Participant information
 * 
 * @author YPLO6403
 * 
 */
public class ParticipantInfo implements Parcelable, Serializable {

	private static final long serialVersionUID = 0L;

	/**
	 * The status
	 */
	private int status;

	/**
	 * The contact number
	 */
	private String contact;

	/**
	 * Participant status
	 */
	public static class Status {
		/**
		 * Unknown status
		 */
		public final static int UNKNOWN = 0;

		/**
		 * Connected
		 */
		public final static int CONNECTED = 1;
		
		/**
		 * Disconnected
		 */
		public final static int DISCONNECTED = 2;

		/**
		 * Departed
		 */
		public final static int DEPARTED = 3;

		/**
		 * Booted
		 */
		public final static int BOOTED = 4;

		/**
		 * Failed
		 */
		public final static int FAILED = 5;

		/**
		 * Busy
		 */
		public final static int BUSY = 6;

		/**
		 * Declined
		 */
		public final static int DECLINED = 7;

		/**
		 * Pending
		 */
		public final static int PENDING = 8;

		private Status() {
		}
	}

	/**
	 * Constructor
	 * 
	 * @param status
	 *            the status
	 * @param contact
	 *            the contact number
	 * @hide
	 */
	public ParticipantInfo(int status, String contact) {
		super();
		this.status = status;
		this.contact = contact;
	}

	/**
	 * Constructor
	 * 
	 * @param in
	 *            Parcelable source
	 */
	public ParticipantInfo(Parcel in) {
		this.contact = in.readString();
		this.status = in.readInt();
	}

	/**
	 * Parcelable creator
	 * 
	 * @hide
	 */
	public static final Parcelable.Creator<ParticipantInfo> CREATOR = new Parcelable.Creator<ParticipantInfo>() {
		public ParticipantInfo createFromParcel(Parcel in) {
			return new ParticipantInfo(in);
		}

		public ParticipantInfo[] newArray(int size) {
			return new ParticipantInfo[size];
		}
	};

	/**
	 * Returns the status
	 * 
	 * @return Status
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Set the participant status
	 * 
	 * @param status
	 *            the participant status
	 * @hide
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * Returns the contact number
	 * 
	 * @return
	 */
	public String getContact() {
		return contact;
	}

	/**
	 * Set the participant contact number
	 * 
	 * @param contact
	 *            the participant contact number
	 * @hide
	 */
	public void setContact(String contact) {
		this.contact = contact;
	}

	/**
	 * Describe the kinds of special objects contained in this Parcelable's marshalled representation
	 * 
	 * @return Integer
	 * @hide
	 */
	@Override
	public int describeContents() {
		return 0;
	}

	/**
	 * Write parcelable object
	 * 
	 * @param dest
	 *            The Parcel in which the object should be written
	 * @param flags
	 *            Additional flags about how the object should be written
	 * @hide
	 */
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(contact);
		dest.writeInt(status);
	}

	/**
	 * Read parcelable object
	 * 
	 * @param in
	 *            Parcelable source
	 * @hide
	 */
	public void readFromParcel(Parcel in) {
		this.contact = in.readString();
		this.status = in.readInt();
	}

}
