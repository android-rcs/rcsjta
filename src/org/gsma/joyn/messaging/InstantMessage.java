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

package org.gsma.joyn.messaging;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.String;

/**
 * Class InstantMessage
 * 
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public class InstantMessage implements Parcelable {
  // Fields

  public static final String MIME_TYPE = "text/plain";

  public static final Parcelable.Creator<InstantMessage> CREATOR = null;

  // Constructors

  public InstantMessage(String arg1, String arg2, String arg3, boolean arg4){
  }
  public InstantMessage(String arg1, String arg2, String arg3, boolean arg4, java.util.Date arg5){
  }
  public InstantMessage(Parcel arg1){
  }
  // Methods

  public java.util.Date getDate(){
    return (java.util.Date) null;
  }
  public int describeContents(){
    return 0;
  }
  public void writeToParcel(Parcel arg1, int arg2){
  }
  public String getTextMessage(){
    return (String) null;
  }
  public String getMessageId(){
    return (String) null;
  }
  public String getRemote(){
    return (String) null;
  }
  public boolean isImdnDisplayedRequested(){
    return false;
  }
  public java.util.Date getServerDate(){
    return (java.util.Date) null;
  }
}
