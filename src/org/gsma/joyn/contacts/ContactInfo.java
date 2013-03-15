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

package org.gsma.joyn.contacts;

import java.lang.String;

public class ContactInfo implements android.os.Parcelable {
  // Fields

  public static final int RCS_CAPABLE = 0;

  public static final int NOT_RCS = 1;

  public static final int RCS_ACTIVE = 2;

  public static final int RCS_REVOKED = 3;

  public static final int RCS_BLOCKED = 4;

  public static final int RCS_PENDING_OUT = 5;

  public static final int RCS_PENDING = 6;

  public static final int RCS_CANCELLED = 7;

  public static final int NO_INFO = 8;

  public static final int REGISTRATION_STATUS_UNKNOWN = 0;

  public static final int REGISTRATION_STATUS_ONLINE = 1;

  public static final int REGISTRATION_STATUS_OFFLINE = 2;

  public static final android.os.Parcelable.Creator<ContactInfo> CREATOR = null;

  // Constructors

  public ContactInfo(){
  }
  public ContactInfo(ContactInfo arg1){
  }
  public ContactInfo(android.os.Parcel arg1){
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
  public String getContact(){
    return (String) null;
  }
  public int getRegistrationState(){
    return 0;
  }
  public int getRcsStatus(){
    return 0;
  }
  public long getRcsStatusTimestamp(){
    return 0l;
  }
  public org.gsma.joyn.presence.PresenceInfo getPresenceInfo(){
    return (org.gsma.joyn.presence.PresenceInfo) null;
  }
  public void setCapabilities(org.gsma.joyn.capability.Capabilities arg1){
  }
  public org.gsma.joyn.capability.Capabilities getCapabilities(){
    return (org.gsma.joyn.capability.Capabilities) null;
  }
  public void setPresenceInfo(org.gsma.joyn.presence.PresenceInfo arg1){
  }
  public void setContact(String arg1){
  }
  public void setRcsStatus(int arg1){
  }
  public void setRegistrationState(int arg1){
  }
  public void setRcsStatusTimestamp(long arg1){
  }
  public boolean isRcsContact(){
    return false;
  }
}
