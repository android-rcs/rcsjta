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

package org.gsma.joyn.capability;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.String;
import java.util.ArrayList;

/**
 * Class Capabilities.
 *
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public class Capabilities implements Parcelable {

  public static final Parcelable.Creator<Capabilities> CREATOR = null;

  public Capabilities(){
  }
  public Capabilities(Parcel soruce){
  }

  public String toString(){
    return (String) null;
  }
  public void setTimestamp(long timestamp){
  }
  public long getTimestamp(){
    return 0l;
  }
  public int describeContents(){
    return 0;
  }
  public void writeToParcel(Parcel dest, int flags){
  }
  public boolean isImageSharingSupported(){
    return false;
  }
  public void setImageSharingSupport(boolean supported){
  }
  public boolean isVideoSharingSupported(){
    return false;
  }
  public void setVideoSharingSupport(boolean supported){
  }
  public boolean isImSessionSupported(){
    return false;
  }
  public void setImSessionSupport(boolean supported){
  }
  public boolean isFileTransferSupported(){
    return false;
  }
  public void setFileTransferSupport(boolean supported){
  }
  public boolean isCsVideoSupported(){
    return false;
  }
  public void setCsVideoSupport(boolean supported){
  }
  public boolean isPresenceDiscoverySupported(){
    return false;
  }
  public void setPresenceDiscoverySupport(boolean supported){
  }
  public boolean isSocialPresenceSupported(){
    return false;
  }
  public void setSocialPresenceSupport(boolean supported){
  }
  public boolean isFileTransferHttpSupported(){
    return false;
  }
  public void setFileTransferHttpSupport(boolean supported){
  }
  public boolean isGeolocationPushSupported(){
    return false;
  }
  public void setGeolocationPushSupport(boolean supported){
  }
  public void addSupportedExtension(String tag){
  }
  public ArrayList<String> getSupportedExtensions(){
    return (ArrayList<String>) null;
  }
}
