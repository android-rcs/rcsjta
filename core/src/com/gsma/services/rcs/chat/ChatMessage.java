/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/
package com.gsma.services.rcs.chat;

import android.os.Parcel;
import android.os.Parcelable;

import com.gsma.services.rcs.contacts.ContactId;

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
	private final String mId;

	/**
	 * Contact who has sent the message
	 */
	private final ContactId mRemoteContact;
	
	/**
	 * Message content
	 */
	private final String mContent;
	
	/**
	 * Time-stamp
	 */
	private final long mTimestamp;

	/**
	 * Time-stamp sent
	 */
	private final long mTimestampSent;

    /**
     * Constructor for outgoing message
     * 
     * @param id Message Id
     * @param remoteContact Contact Id
     * @param content Message content
     * @param timestamp Time-stamp
     * @param timestampSent Time-stamp sent
     * @hide
	 */
	public ChatMessage(String id, ContactId remoteContact, String content, long timestamp, long timestampSent) {
		mId = id;
		mRemoteContact = remoteContact;
		mContent = content;
		mTimestamp = timestamp;
		mTimestampSent = timestampSent;
	}
	
	/**
	 * Constructor
	 * 
	 * @param source Parcelable source
     * @hide
	 */
	public ChatMessage(Parcel source) {
		mId = source.readString();
		boolean containsContactId = source.readInt() != 0;
		if (containsContactId) {
			mRemoteContact = ContactId.CREATOR.createFromParcel(source);
		} else {
			mRemoteContact = null;
		}
		mContent = source.readString();
		mTimestamp = source.readLong();
		mTimestampSent = source.readLong();
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
    	dest.writeString(mId);
    	if (mRemoteContact != null) {
    		dest.writeInt(1);
    		mRemoteContact.writeToParcel(dest, flags);
    	} else {
    		dest.writeInt(0);
    	}
    	dest.writeString(mContent);
    	dest.writeLong(mTimestamp);
    	dest.writeLong(mTimestampSent);
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
    	return mId;
    }

	/**
	 * Returns the contact
	 * 
	 * @return ContactId
	 */
	public ContactId getRemoteContact() {
		return mRemoteContact;
	}

	/**
	 * Returns the message content
	 * 
	 * @return String
	 */
	public String getContent() {
		return mContent;
	}
	
	/**
	 * Returns the local time-stamp of when the chat message was sent and/or
	 * queued for outgoing messages or the local time-stamp of when the chat
	 * message was received for incoming messages.
	 * 
	 * @return long
	 */
	public long getTimestamp() {
		/* TODO: This method will be implemented in CR018. */
		throw new UnsupportedOperationException("Method not supported yet!");
	}

	/**
	 * Returns the local time-stamp of when the chat message was sent and/or
	 * queued for outgoing messages or the remote time-stamp of when the chat
	 * message was sent for incoming messages.
	 * 
	 * @return long
	 */
	public long getTimestampSent() {
		/* TODO: This method will be implemented in CR018. */
		throw new UnsupportedOperationException("Method not supported yet!");
	}
}
