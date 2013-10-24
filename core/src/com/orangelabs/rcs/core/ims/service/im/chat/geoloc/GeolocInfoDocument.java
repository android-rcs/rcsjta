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
package com.orangelabs.rcs.core.ims.service.im.chat.geoloc;

/**
 * Geolocation info document
 *
 * @author vfml3370
 */
public class GeolocInfoDocument {
	/**
	 * MIME type
	 */
	public static final String MIME_TYPE = "application/vnd.gsma.rcspushlocation+xml";
	
	/**
	 * Entity
	 */
	private String entity = null;

	/**
	 * Label
	 */
	private String label = null;
	
	/**
	 * Latitude
	 */
	private double latitude = 0;
	
	/**
	 * Longitude
	 */
	private double longitude = 0;
	
	/**
	 * Altitude
	 */
	private double altitude = 0;

	/**
	 * Expiration date
	 */
	private long expiration = 0;

	/**
	 * Radius in meters
	 */
	private float radius = 0;

	/**
	 * Constructor
	 * 
	 * @param entity Entity
	 */
	public GeolocInfoDocument(String entity) {
		this.entity = entity;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getEntity() {
		return entity;
	}

	public void setEntity(String entity) {
		this.entity = entity;
	}
	
	public long getExpiration() {
		return expiration;
	}

	public void setExpiration(long expiration) {
		this.expiration = expiration;
	}
	
	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double l) {
		latitude = l;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double l) {
		longitude = l;
	}

	public double getAltitude() {
		return altitude;
	}	

	public void setAltitude(double a) {
		altitude = a;
	}

	public float getRadius() {
		return radius;
	}	

	public void setRadius(float r) {
		radius = r;
	}
}
