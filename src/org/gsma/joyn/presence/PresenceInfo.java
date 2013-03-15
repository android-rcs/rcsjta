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

package org.gsma.joyn.presence;


public class PresenceInfo
  implements android.os.Parcelable
{
  // Fields

  public static final java.lang.String UNKNOWN = "unknown";

  public static final java.lang.String ONLINE = "open";

  public static final java.lang.String OFFLINE = "closed";

  public static final java.lang.String RCS_ACTIVE = "active";

  public static final java.lang.String RCS_REVOKED = "revoked";

  public static final java.lang.String RCS_BLOCKED = "blocked";

  public static final java.lang.String RCS_PENDING_OUT = "pending_out";

  public static final java.lang.String RCS_PENDING = "pending";

  public static final java.lang.String RCS_CANCELLED = "cancelled";

  public static final android.os.Parcelable.Creator<PresenceInfo> CREATOR = null;

  // Constructors

  public PresenceInfo(){
  }
  public PresenceInfo(android.os.Parcel arg1){
  }
  // Methods

  public java.lang.String toString(){
    return (java.lang.String) null;
  }
  public void setTimestamp(long arg1){
  }
  public long getTimestamp(){
    return 0l;
  }
  public int describeContents(){
    return 0;
  }
  public void writeToParcel(android.os.Parcel arg1, int arg2){
  }
  public void resetTimestamp(){
  }
  public static long getNewTimestamp(){
    return 0l;
  }
  public java.lang.String getPresenceStatus(){
    return (java.lang.String) null;
  }
  public void setPresenceStatus(java.lang.String arg1){
  }
  public boolean isOnline(){
    return false;
  }
  public boolean isOffline(){
    return false;
  }
  public java.lang.String getFreetext(){
    return (java.lang.String) null;
  }
  public void setFreetext(java.lang.String arg1){
  }
  public FavoriteLink getFavoriteLink(){
    return (FavoriteLink) null;
  }
  public java.lang.String getFavoriteLinkUrl(){
    return (java.lang.String) null;
  }
  public void setFavoriteLink(FavoriteLink arg1){
  }
  public void setFavoriteLinkUrl(java.lang.String arg1){
  }
  public PhotoIcon getPhotoIcon(){
    return (PhotoIcon) null;
  }
  public void setPhotoIcon(PhotoIcon arg1){
  }
  public Geoloc getGeoloc(){
    return (Geoloc) null;
  }
  public void setGeoloc(Geoloc arg1){
  }
}
