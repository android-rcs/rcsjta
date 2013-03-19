/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.joyn.presence;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.String;

/**
 * Class Geoloc.
 * 
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public class Geoloc implements Parcelable {

  public static final Parcelable.Creator<Geoloc> CREATOR = null;

  public Geoloc(double latitude, double longitude, double altitude){
  }

  public Geoloc(Parcel source){
  }

  public String toString(){
    return (String) null;
  }
  public int describeContents(){
    return 0;
  }
  public void writeToParcel(Parcel dest, int flags){
  }
  public double getLongitude(){
    return 0.0d;
  }
  public double getLatitude(){
    return 0.0d;
  }
  public double getAltitude(){
    return 0.0d;
  }
  public void setLatitude(double latitude){
  }
  public void setLongitude(double longitude){
  }
  public void setAltitude(double altitude){
  }
}
