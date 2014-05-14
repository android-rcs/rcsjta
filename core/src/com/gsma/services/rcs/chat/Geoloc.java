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
 * Geolocation push info
 * 
 * @author Jean-Marc AUFFRET 
 */
public class Geoloc implements Parcelable, Serializable {
	private static final long serialVersionUID = 0L;
    
    /**
     * Label associated to the geolocation
     */
    private String label;

    /**
     * Latitude
     */
    private double latitude;

    /**
     * Longitude
     */
    private double longitude;

    /**
     * Expiration date of the geolocation
     */
    private long expiration = 0L;
    
    /**
     * Accuracy (in meters)
     */
    private float accuracy = 0.0f;
    
    /**
     * Constructor
     *
     * @param label Label
     * @param latitude Latitude
     * @param longitude Longitude
     * @param expiration Expiration time
     * @hide
     */
    public Geoloc(String label, double latitude, double longitude, long expiration) {
        this.label = label;
        this.latitude = latitude;
        this.longitude = longitude;
        this.expiration = expiration;
    }

    /**
     * Constructor
     *
     * @param label Label
     * @param latitude Latitude
     * @param longitude Longitude
     * @param expiration Expiration date
     * @param accuracy Accuracy 
     * @hide
     */
    public Geoloc(String label, double latitude, double longitude, long expiration, float accuracy) {
    	this(label, latitude, longitude, expiration);
    	
        this.accuracy = accuracy;
    }
    
    /**
     * Constructor
     *
     * @param source Parcelable source
     * @hide
     */
    public Geoloc(Parcel source) {
    	this.label = source.readString();
    	this.latitude = source.readDouble(); 
    	this.longitude = source.readDouble();     	    	                                              
    	this.expiration = source.readLong(); 
    	this.accuracy = source.readFloat(); 
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
    	dest.writeString(label);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
    	dest.writeLong(expiration);
    	dest.writeFloat(accuracy);
    }

    /**
     * Parcelable creator
     * 
     * @hide
     */
    public static final Parcelable.Creator<Geoloc> CREATOR = new Parcelable.Creator<Geoloc>() {
        public Geoloc createFromParcel(Parcel source) {
            return new Geoloc(source);
        }

        public Geoloc[] newArray(int size) {
            return new Geoloc[size];
        }
    };

    /**
     * Returns the label
     *
     * @return Label
     */
    public String getLabel() {
		return label;
    }

    /**
     * Set the label
     *
     * @param label Label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the latitude
     *
     * @return Latitude
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Set the latitude
     *
     * @param latitude Latitude
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Returns the longitude
     *
     * @return Longitude
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Set the longitude
     *
     * @param longitude Longitude
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * Returns the expiration date of the geolocation
     *
     * @return Expiration date. 0 means no expiration date has been defined.
     */
    public long getExpiration() {
        return expiration;
    }
 
    /**
     * Set the expiration date of the geolocation
     *
     * @param expiration Expiration
     */
    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }
 
    /**
     * Returns the accuracy
     *
     * @return Accuracy in meters. 0 means no accuracy has been defined.
     */
    public float getAccuracy() {
        return accuracy;
    }
 
    /**
     * Set the accuracy
     *
     * @param accuracy Accuracy in meters
     */
    public void setAcuracy(float accuracy) {
        this.accuracy = accuracy;
    }

}
