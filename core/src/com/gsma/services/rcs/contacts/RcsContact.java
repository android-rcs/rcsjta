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

import com.gsma.services.rcs.capability.Capabilities;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Rcs contact
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 */
public class RcsContact implements Parcelable {
	/**
	 * Capabilities
	 */
	private Capabilities capabilities;

	/**
	 * Contact ID
	 */
	private ContactId mContact;

	/**
	 * Display name
	 */
	private String displayName;

	/**
	 * Registration state
	 */
	private boolean registered;

	/**
	 * Constructor
	 * 
	 * @param contact Contact ID
	 * @param registered Registration state
	 * @param capabilities Capabilities
	 * @param displayName display name
	 * @hide
	 */
	public RcsContact(ContactId contact, boolean registered, Capabilities capabilities,
			String displayName) {
		mContact = contact;
		this.registered = registered;
		this.capabilities = capabilities;
		this.displayName = displayName;
	}

	/**
	 * Constructor
	 * 
	 * @param source Parcelable source
	 * @hide
	 */
	public RcsContact(Parcel source) {
		boolean containsContact = source.readInt() != 0;
		if (containsContact) {
			mContact = ContactId.CREATOR.createFromParcel(source);
		} else {
			mContact = null;
		}
		registered = source.readInt() != 0;
		boolean containsCapabilities = source.readInt() != 0;
		if (containsCapabilities) {
			this.capabilities = Capabilities.CREATOR.createFromParcel(source);
		} else {
			this.capabilities = null;
		}
		displayName = source.readString();
	}

	/**
	 * Describe the kinds of special objects contained in this Parcelable's
	 * marshalled representation
	 * 
	 * @return Integer
	 * @hide
	 */
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
	public void writeToParcel(Parcel dest, int flags) {
		if (mContact != null) {
			dest.writeInt(1);
			mContact.writeToParcel(dest, flags);
		} else {
			dest.writeInt(0);
		}
		dest.writeInt(registered ? 1 : 0);
		if (capabilities != null) {
			dest.writeInt(1);
			capabilities.writeToParcel(dest, flags);
		} else {
			dest.writeInt(0);
		}
		dest.writeString(displayName);
	}

	/**
	 * Parcelable creator
	 * 
	 * @hide
	 */
	public static final Parcelable.Creator<RcsContact> CREATOR = new Parcelable.Creator<RcsContact>() {
		public RcsContact createFromParcel(Parcel source) {
			return new RcsContact(source);
		}

		public RcsContact[] newArray(int size) {
			return new RcsContact[size];
		}
	};

	/**
	 * Returns the canonical contact ID (i.e. MSISDN)
	 * 
	 * @return Contact ID
	 */
	public ContactId getContactId() {
		return mContact;
	}

	/**
	 * Is contact online (i.e. registered to the service platform)
	 * 
	 * @return Returns true if registered else returns false
	 */
	public boolean isRegistered() {
		return registered;
	}

	/**
	 * Returns the capabilities associated to the contact
	 * 
	 * @return Capabilities
	 */
	public Capabilities getCapabilities() {
		return capabilities;
	}

	/**
	 * Returns the display name associated to the contact
	 * 
	 * @return displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

}
