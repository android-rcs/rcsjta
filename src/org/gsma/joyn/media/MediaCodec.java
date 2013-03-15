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

package org.gsma.joyn.media;

import java.lang.String;

public class MediaCodec
  implements android.os.Parcelable
{
  // Fields

  public static final android.os.Parcelable.Creator<MediaCodec> CREATOR = null;

  // Constructors

  public MediaCodec(String arg1){
  }
  public MediaCodec(android.os.Parcel arg1){
  }
  // Methods

  public int describeContents(){
    return 0;
  }
  public void writeToParcel(android.os.Parcel arg1, int arg2){
  }
  public String getCodecName(){
    return (String) null;
  }
  public void setIntParam(String arg1, int arg2){
  }
  public void setStringParam(String arg1, String arg2){
  }
  public int getIntParam(String arg1, int arg2){
    return 0;
  }
  public String getStringParam(String arg1){
    return (String) null;
  }
  public void setCodecName(String arg1){
  }
}
