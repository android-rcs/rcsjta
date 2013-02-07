/*
 * Copyright 2013, France Telecom
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *    http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.contacts;


public class ContactInfo
  implements android.os.Parcelable
{
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

  public java.lang.String toString(){
    return (java.lang.String) null;
  }
  public int describeContents(){
    return 0;
  }
  public void writeToParcel(android.os.Parcel arg1, int arg2){
  }
  public java.lang.String getContact(){
    return (java.lang.String) null;
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
  public org.gsma.rcs.presence.PresenceInfo getPresenceInfo(){
    return (org.gsma.rcs.presence.PresenceInfo) null;
  }
  public void setCapabilities(org.gsma.rcs.capability.Capabilities arg1){
  }
  public org.gsma.rcs.capability.Capabilities getCapabilities(){
    return (org.gsma.rcs.capability.Capabilities) null;
  }
  public void setPresenceInfo(org.gsma.rcs.presence.PresenceInfo arg1){
  }
  public void setContact(java.lang.String arg1){
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
