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

package com.orangelabs.rcs.core.ims.service.presence.pidf.geoloc;

public class Geopriv {
    private String method = null;
    private double latitude = 0;
    private double longitude = 0;
    private double altitude = 0;

    public Geopriv() {
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String met) {
        this.method = met;
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
}
