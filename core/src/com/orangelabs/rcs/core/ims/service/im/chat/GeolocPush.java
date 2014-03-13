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

package com.orangelabs.rcs.core.ims.service.im.chat;


/**
 * Geolocation push info
 * 
 * @author Orange
 */
public class GeolocPush {
    
    /**
     * Label associated to the location
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
     * Expiration date
     */
    private long expiration;
    
    /**
     * Accuracy (in meters)
     */
    private float accuracy = 0;

    /**
     * Constructor
     *
     * @param label Label
     * @param latitude Latitude
     * @param longitude Longitude
     * @param expiration Expiration date
     */
    public GeolocPush(String label, double latitude, double longitude, long expiration) {
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
     */
    public GeolocPush(String label, double latitude, double longitude, long expiration, float accuracy) {
    	this(label, latitude, longitude, expiration);
    	
        this.accuracy = accuracy;
    }

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
     * Returns the expiration date
     *
     * @return Expiration date
     */
    public long getExpiration() {
        return expiration;
    }
 
    /**
     * Set the expiration
     *
     * @param expiration Expiration
     */
    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }
 
    /**
     * Returns the accuracy
     *
     * @return Accuracy in meters
     */
    public float getAccuracy() {
        return accuracy;
    }
 
    /**
     * Set the accuracy
     *
     * @param accuracy Accuracy
     */
    public void setAcuracy(float accuracy) {
        this.accuracy = accuracy;
    }
    
    /**
     * Returns a string representation of the object
     *
     * @return String
     */
    public String toString() {
        return "Label=" + label +
                ", Latitude=" + latitude +
                ", Longitude=" + longitude +
        		", Expiration=" + expiration +
        		", Accuracy=" + accuracy;
    }
}
