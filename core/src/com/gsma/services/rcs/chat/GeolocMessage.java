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

import java.util.Date;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

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
     * @param contact Contact
     * @param geoloc Geolocation info
     * @param receiptAt Receipt date
     * @param displayedReportRequested Flag indicating if a displayed report is requested
     * @hide
	 */
	public GeolocMessage(String messageId, String remote, Geoloc geoloc, Date receiptAt, boolean imdnDisplayedRequested) {
		super(messageId, remote, GeolocMessage.geolocToString(geoloc), receiptAt, imdnDisplayedRequested);
		
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
		
		this.geoloc = new Geoloc(source);
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
    	
    	geoloc.writeToParcel(dest, flags);
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
	
	/** 
     * Format a geoloc object to a string
     * 
     * @param geoloc Geoloc object
     * @return String
	 * @see Geoloc
     */
    public static String geolocToString(Geoloc geoloc) {
    	String label = geoloc.getLabel();
    	if (label == null) {
    		label = "";
    	}
    	return label + "," +
    		geoloc.getLatitude() + "," +
    		geoloc.getLongitude() + "," +
    		geoloc.getAltitude() + "," +
    		geoloc.getExpiration() + "," +
    		geoloc.getAccuracy();
    }

    /** 
     * Format a string to a geoloc object
     * 
     * @param str String
     * @return Geoloc object
     */
    public static Geoloc stringToGeoloc(String str) {
    	try {
	    	StringTokenizer items = new StringTokenizer(str, ",");
	    	String label = null;
	    	if (items.countTokens() > 5) {
	    		label = items.nextToken();
	    	}
			double latitude = Double.valueOf(items.nextToken());					
			double longitude = Double.valueOf(items.nextToken());
			double altitude = Double.valueOf(items.nextToken());
			long expiration = Long.valueOf(items.nextToken());
			float accuracy =  Float.valueOf(items.nextToken());
			return new Geoloc(label, latitude, longitude, altitude, expiration, accuracy);
    	} catch(NoSuchElementException e) {
    		return null;
    	} catch(NumberFormatException e) {
    		return null;
    	}
    }		
}
