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
package com.orangelabs.rcs.service.api.client.messaging;

import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Geoloc message
 * 
 * @author jexa7410
 */
public class GeolocMessage extends InstantMessage implements Parcelable {
	/**
	 * MIME type
	 */
	public static final String MIME_TYPE = "text/geoloc";

	/**
	 * Geoloc info
	 */
	private GeolocPush geoloc = null;
		
    /**
     * Constructor for outgoing message
     * 
     * @param messageId Message Id
     * @param remote Remote user
     * @param geoloc Geoloc info
     * @param imdnDisplayedRequested Flag indicating that an IMDN "displayed" is requested
	 */
	public GeolocMessage(String messageId, String remote, GeolocPush geoloc, boolean imdnDisplayedRequested) {
		super(messageId, remote, GeolocPush.formatGeolocToStr(geoloc), imdnDisplayedRequested);
		
		this.geoloc = geoloc;
	}
	
	/**
     * Constructor for incoming message
     * 
     * @param messageId Message Id
     * @param remote Remote user
     * @param geoloc Geoloc info
     * @param imdnDisplayedRequested Flag indicating that an IMDN "displayed" is requested
	 * @param serverReceiptAt Receipt date of the message on the server
	 */
	public GeolocMessage(String messageId, String remote, GeolocPush geoloc, boolean imdnDisplayedRequested, Date serverReceiptAt) {
		super(messageId, remote, GeolocPush.formatGeolocToStr(geoloc), imdnDisplayedRequested, serverReceiptAt);
		
		this.geoloc = geoloc;
	}
	
	/**
	 * Constructor
	 * 
	 * @param source Parcelable source
	 */
	public GeolocMessage(Parcel source) {
		super(source);
		
		this.geoloc = new GeolocPush(source);
    }
	
	/**
	 * Describe the kinds of special objects contained in this Parcelable's
	 * marshalled representation
	 * 
	 * @return Integer
	 */
	public int describeContents() {
        return 0;
    }

	/**
	 * Write parcelable object
	 * 
	 * @param dest The Parcel in which the object should be written
	 * @param flags Additional flags about how the object should be written
	 */
    public void writeToParcel(Parcel dest, int flags) {
    	super.writeToParcel(dest, flags);
    	
    	geoloc.writeToParcel(dest, flags);
    }

    /**
     * Parcelable creator
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
	 * Get geoloc info
	 * 
	 * @return Geoloc info
	 */
	public GeolocPush getGeoloc() {
		return geoloc;
	}
}
