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

package com.gsma.services.rcs.contacts;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * ContactId class is defined to handle phone numbers instead of string.
 * 
 * @author YPLO6403
 * 
 */
public class ContactId implements Parcelable, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The contactId formatted in the international representation of the phone number “<CC><NDCCS><SN>” <br>
	 * with:
	 * <ul>
	 * <li>- CC : the country code with a leading ‘+’ character
	 * <li>- NCSDC : the national destination code
	 * <li>- SN: the subscriber number
	 * </ul>
	 * All these codes CC, NDCS, SN are digits.
	 * <p>
	 * If this number needs to be displayed in the UI with some particular UI formatting, it is in the scope of UI code to format
	 * that. This class will never hold specific UI formatted numbers since they need to be unique.
	 */
	private String contactId;

	/**
	 * Constructor
	 * 
	 * @param contact
	 *            the contact number (valid characters are digits, space, ‘-‘, leading ‘+’)
	 */
	/* package private */ContactId(String contact) {
		this.contactId = contact;
	}

	/**
	 * Constructor
	 * 
	 * @param source
	 *            Parcelable source
	 * @hide
	 */
	public ContactId(Parcel source) {
		contactId = source.readString();
	}

	@Override
	public String toString() {
		return contactId;
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
		dest.writeString(contactId);
	}

	/**
	 * Parcelable creator
	 * 
	 * @hide
	 */
	public static final Parcelable.Creator<ContactId> CREATOR = new Parcelable.Creator<ContactId>() {
		public ContactId createFromParcel(Parcel source) {
			return new ContactId(source);
		}

		public ContactId[] newArray(int size) {
			return new ContactId[size];
		}
	};

	/**
	 * hashCode
	 * 
	 * @hide
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((contactId == null) ? 0 : contactId.hashCode());
		return result;
	}

	/**
	 * equals
	 * 
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
		ContactId other = (ContactId) obj;
		if (contactId == null) {
			if (other.contactId != null)
				return false;
		} else if (!contactId.equals(other.contactId))
			return false;
		return true;
	}
}
