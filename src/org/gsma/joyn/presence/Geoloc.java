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

import java.lang.String;

public class Geoloc
  implements android.os.Parcelable
{
  // Fields

  public static final android.os.Parcelable.Creator<Geoloc> CREATOR = null;

  // Constructors

  public Geoloc(double arg1, double arg2, double arg3){
  }
  public Geoloc(android.os.Parcel arg1){
  }
  // Methods

  public String toString(){
    return (String) null;
  }
  public int describeContents(){
    return 0;
  }
  public void writeToParcel(android.os.Parcel arg1, int arg2){
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
  public void setLatitude(double arg1){
  }
  public void setLongitude(double arg1){
  }
  public void setAltitude(double arg1){
  }
}
