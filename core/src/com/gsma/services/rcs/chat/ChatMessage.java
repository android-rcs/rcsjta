/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/
package com.gsma.services.rcs.chat;

import java.util.Date;

import com.gsma.services.rcs.contacts.ContactId;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Chat message
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 *
 */
public class ChatMessage implements Parcelable {

	/**
	 * Unique message Id
	 */
	private String id;

	/**
	 * Contact who has sent the message
	 */
	private ContactId contact;
	
	/**
	 * Message content
	 */
	private String message;
	
	/**
	 * Receipt date of the message
	 */
	private Date receiptAt;

    /**
     * Constructor for outgoing message
     * 
     * @param messageId Message Id
     * @param remote Contact
     * @param message Message content
     * @param receiptAt Receipt date
     * @hide
	 */
	public ChatMessage(String messageId, ContactId remote, String message, Date receiptAt) {
		this.id = messageId;
		this.contact = remote;
		this.message = message;
		this.receiptAt = receiptAt;
	}
	
	/**
	 * Constructor
	 * 
	 * @param source Parcelable source
     * @hide
	 */
	public ChatMessage(Parcel source) {
		this.id = source.readString();
		boolean containsContactId = source.readInt() != 0;
		if (containsContactId) {
			this.contact = ContactId.CREATOR.createFromParcel(source);
		} else {
			this.contact = null;
		}
		this.message = source.readString();
		this.receiptAt = new Date(source.readLong());
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
    	dest.writeString(id);
    	if (contact != null) {
    		dest.writeInt(1);
    		contact.writeToParcel(dest, flags);
    	} else {
    		dest.writeInt(0);
    	}
    	dest.writeString(message);
    	dest.writeLong(receiptAt.getTime());
    }

    /**
     * Parcelable creator
     * 
     * @hide
     */
    public static final Parcelable.Creator<ChatMessage> CREATOR
            = new Parcelable.Creator<ChatMessage>() {
        public ChatMessage createFromParcel(Parcel source) {
            return new ChatMessage(source);
        }

        public ChatMessage[] newArray(int size) {
            return new ChatMessage[size];
        }
    };	
	
	/**
	 * Returns the message ID
	 * 
	 * @return ID
	 */
    public String getId(){
    	return id;
    }

	/**
	 * Returns the contact
	 * 
	 * @return ContactId
	 */
	public ContactId getContact() {
		return contact;
	}

	/**
	 * Returns the message content
	 * 
	 * @return String
	 */
	public String getMessage() {
		return message;
	}
	
	/**
	 * Returns the receipt date
	 * 
	 * @return Date
	 */
	public Date getReceiptDate() {
		return receiptAt;
	}

}
