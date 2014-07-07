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

import android.os.Parcel;
import android.os.Parcelable;

import com.gsma.services.rcs.capability.Capabilities;

/**
 * Joyn contact
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 *
 */
public class JoynContact implements Parcelable {
	/**
	 * Capabilities
	 */
	private Capabilities capabilities = null;
	
	/**
	 * Contact ID
	 */
	private ContactId contactId = null;
	
	/**
	 * Registration state
	 */
	private boolean registered = false;
	
    /**
	 * Constructor
	 * 
	 * @param contactId Contact ID
	 * @param registered Registration state
	 * @param capabilities Capabilities 
     * @hide
	 */
	public JoynContact(ContactId contactId, boolean registered, Capabilities capabilities) {
		this.contactId = contactId;
		this.registered = registered;
		this.capabilities = capabilities;
	}
	
	/**
	 * Constructor
	 * 
	 * @param source Parcelable source
     * @hide
	 */
	public JoynContact(Parcel source) {
		boolean flag = source.readInt() != 0;
		if (flag) {
			contactId = ContactId.CREATOR.createFromParcel(source);
		} else {
			contactId = null;
		}
		registered = source.readInt() != 0;
		flag = source.readInt() != 0;
		if (flag) {
			this.capabilities = Capabilities.CREATOR.createFromParcel(source);
		} else {
			this.capabilities = null;
		}
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
    	if (contactId != null) {
    		dest.writeInt(1);
    		contactId.writeToParcel(dest, flags);
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
    }

    /**
     * Parcelable creator
     * 
     * @hide
     */
    public static final Parcelable.Creator<JoynContact> CREATOR
            = new Parcelable.Creator<JoynContact>() {
        public JoynContact createFromParcel(Parcel source) {
            return new JoynContact(source);
        }

        public JoynContact[] newArray(int size) {
            return new JoynContact[size];
        }
    };	
		
	/**
	 * Returns the canonical contact ID (i.e. MSISDN)
	 * 
	 * @return Contact ID
	 */
	public ContactId getContactId() {
		return contactId;
	}
	
	/**
	 * Is contact online (i.e. registered to the service platform)
	 * 
	 * @return Returns true if registered else returns false
	 */
	public boolean isRegistered(){
		return registered;
	}
	
	/**
	 * Returns the capabilities associated to the contact
	 * 
	 * @return Capabilities
	 */
	public Capabilities getCapabilities(){
		return capabilities;
	}

	@Override
	public String toString() {
		return "JoynContact [capabilities=" + capabilities + ", contactId=" + contactId + ", registered=" + registered + "]";
	}	
}
