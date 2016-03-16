/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.services.rcs;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.StringTokenizer;

/**
 * Geolocation class
 */
public class Geoloc implements Parcelable, Serializable {

    private static final long serialVersionUID = 0L;

    private final String mLabel;

    private final double mLatitude;

    private final double mLongitude;

    private final long mExpiration;

    /**
     * Accuracy (in meters)
     */
    private final float mAccuracy;

    /**
     * Constructor
     * 
     * @param label Label
     * @param latitude Latitude
     * @param longitude Longitude
     * @param expiration Expiration date
     * @param accuracy Accuracy
     */
    public Geoloc(String label, double latitude, double longitude, long expiration, float accuracy) {
        mLabel = label;
        mLatitude = latitude;
        mLongitude = longitude;
        mExpiration = expiration;
        mAccuracy = accuracy;
    }

    /**
     * Constructor: returns a Geoloc instance as parsed from the GEOLOC field in the
     * GeolocSharingLog provider or the CONTENT field of a GelocMessage in the ChatLog.Message
     * provider.
     * 
     * @param geoloc Provider geoloc format
     */
    public Geoloc(String geoloc) {
        StringTokenizer items = new StringTokenizer(geoloc, ",");
        if (items.countTokens() > 4) {
            mLabel = items.nextToken();
        } else {
            mLabel = null;
        }
        mLatitude = Double.valueOf(items.nextToken());
        mLongitude = Double.valueOf(items.nextToken());
        mExpiration = Long.valueOf(items.nextToken());
        mAccuracy = Float.valueOf(items.nextToken());
    }

    /**
     * Constructor
     * 
     * @param source Parcelable source
     * @hide
     */
    public Geoloc(Parcel source) {
        mLabel = source.readString();
        mLatitude = source.readDouble();
        mLongitude = source.readDouble();
        mExpiration = source.readLong();
        mAccuracy = source.readFloat();
    }

    /**
     * Describe the kinds of special objects contained in this Parcelable's marshalled
     * representation.
     * 
     * @return Integer
     * @hide
     */
    public int describeContents() {
        return 0;
    }

    /**
     * Write parcelable object.
     * 
     * @param dest The Parcel in which the object should be written
     * @param flags Additional flags about how the object should be written
     * @hide
     */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mLabel);
        dest.writeDouble(mLatitude);
        dest.writeDouble(mLongitude);
        dest.writeLong(mExpiration);
        dest.writeFloat(mAccuracy);
    }

    /**
     * Parcelable creator.
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
     * Returns the label.
     * 
     * @return Label
     */
    public String getLabel() {
        return mLabel;
    }

    /**
     * Returns the latitude.
     * 
     * @return Latitude
     */
    public double getLatitude() {
        return mLatitude;
    }

    /**
     * Returns the longitude.
     * 
     * @return Longitude
     */
    public double getLongitude() {
        return mLongitude;
    }

    /**
     * Returns the expiration date of the geoloc.
     * 
     * @return Expiration date. 0 means no expiration date has been defined.
     */
    public long getExpiration() {
        return mExpiration;
    }

    /**
     * Returns the accuracy
     * 
     * @return Accuracy in meters. 0 means no accuracy has been defined.
     */
    public float getAccuracy() {
        return mAccuracy;
    }

    /**
     * Returns the geoloc in provider format.
     * 
     * @return String
     */
    @Override
    public String toString() {
        StringBuilder providerContent;
        if (mLabel == null) {
            providerContent = new StringBuilder();
        } else {
            providerContent = new StringBuilder(mLabel);
        }
        return providerContent.append(",").append(mLatitude).append(",").append(mLongitude)
                .append(",").append(mExpiration).append(",").append(mAccuracy).toString();
    }
}
