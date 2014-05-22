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

import com.orangelabs.rcs.utils.PhoneUtils;

/**
 * Participant information
 * 
 * @author YPLO6403
 */
public class ParticipantInfo implements Parcelable, Serializable, Cloneable {

	private static final long serialVersionUID = 0L;

	/**
	 * Status
	 */
	private int status = Status.UNKNOWN;

	/**
	 * Contact
	 */
	private String contact = null;

	/**
	 * Participant status The status may have the following values:
	 * <ul>
	 * <li>UNKNOWN,
	 * <li>CONNECTED,
	 * <li>DEPARTED,
	 * <li>BOOTED,
	 * <li>FAILED,
	 * <li>BUSY,
	 * <li>DECLINED,
	 * <li>PENDING </u>
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
	 * @param contact
	 *            contact
	 * @hide
	 */
	public ParticipantInfo(String contact) {
		this(contact, Status.UNKNOWN);
	}

	/**
	 * Constructor
	 * 
	 * @param contact
	 *            Contact
	 * @param status
	 *            Status
	 * @hide
	 */
	public ParticipantInfo(String contact,int status) {
		super();
		this.status = status;
		String number = PhoneUtils.extractNumberFromUri(contact);
		if (PhoneUtils.isGlobalPhoneNumber(number)) {
			this.contact = number;
		} else {
			throw new IllegalArgumentException("Invalid contact "+contact);
		}
	}

	/**
	 * Constructor
	 * 
	 * @param in Parcelable source
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
	 * @see Status
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Sets the status
	 * 
	 * @param status
	 *            the new status
	 * @see Status
	 * @hide
	 */
	public void setStatus(int status) {
		this.status = status;
	}
	
	/**
	 * Returns the contact number
	 * 
	 * @return Contact
	 */
	public String getContact() {
		return contact;
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
	 * @param dest The Parcel in which the object should be written
	 * @param flags Additional flags about how the object should be written
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
	 * @param in Parcelable source
	 * @hide
	 */
	public void readFromParcel(Parcel in) {
		this.contact = in.readString();
		this.status = in.readInt();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 * @hide
	 */
	@Override public ParticipantInfo clone() {	
		try {
			return (ParticipantInfo)super.clone();
		} catch(CloneNotSupportedException cnse) {
			// Never reach this point since Cloneable
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 * @hide
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((contact == null) ? 0 : contact.hashCode());
		result = prime * result + status;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 * @hide
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ParticipantInfo other = (ParticipantInfo) obj;
		if (contact == null) {
			if (other.contact != null)
				return false;
		} else if (!contact.equals(other.contact))
			return false;
		if (status != other.status)
			return false;
		return true;
	}

	/**
	 * Test is status is connected
	 * 
	 * @param status
	 *            the status
	 * @return true if connected
	 * @hide
	 */
	public static boolean isConnected(int status) {
		return ((status == Status.CONNECTED) || (status == Status.PENDING) || (status == Status.BOOTED));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 * @hide
	 */
	@Override
	public String toString() {
		return "ParticipantInfo [contact=" + contact+ ", status=" + status+ "]";
	}
	
}
