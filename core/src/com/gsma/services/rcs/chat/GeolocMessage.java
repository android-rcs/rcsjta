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
 * Geoloc push message
 *  
 * @author Jean-Marc AUFFRET
 */
public class GeolocMessage extends ChatMessage implements Parcelable {

	/**
	 * Geoloc info
	 */
	private Geoloc geoloc = null;
		
    /**
     * Constructor for outgoing message
     * 
     * Constructor for outgoing message
     * 
     * @param messageId Message Id
     * @param contact Contact Id
     * @param geoloc Geolocation info
     * @param receiptAt Receipt date
     * @hide
	 */
	public GeolocMessage(String messageId, ContactId contact, Geoloc geoloc, Date receiptAt) {
		super(messageId, contact, null, receiptAt);
		
		this.geoloc = geoloc;
	}
	
	/**
	 * Constructor
	 * 
	 * @param source Parcelable source
     * @hide
	 */
	public GeolocMessage(Parcel source) {
		super(source);
		boolean flag = source.readInt() != 0;
		if (flag) {
			this.geoloc = new Geoloc(source);
		} else {
			this.geoloc = null;
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
    	super.writeToParcel(dest, flags);
    	if (geoloc != null) {
    		dest.writeInt(1);
    		geoloc.writeToParcel(dest, flags);
    	} else {
    		dest.writeInt(0);
    	}
    }

    /**
     * Parcelable creator
     * 
     * @hide
     */
    public static final Parcelable.Creator<GeolocMessage> CREATOR
            = new Parcelable.Creator<GeolocMessage>() {
        public GeolocMessage createFromParcel(Parcel source) {
            return new GeolocMessage(source);
        }

        public GeolocMessage[] newArray(int size) {
            return new GeolocMessage[size];
        }
    };	

    /**
	 * Get geolocation info
	 * 
	 * @return Geoloc object
	 * @see Geoloc
	 */
	public Geoloc getGeoloc() {
		return geoloc;
	}

}
