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

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.String;

/**
 * Class MediaCodec.
 *
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public class MediaCodec implements Parcelable {

  public static final Parcelable.Creator<MediaCodec> CREATOR = null;

  public MediaCodec(String arg1){
  }
  public MediaCodec(Parcel arg1){
  }

  public int describeContents(){
    return 0;
  }
  public void writeToParcel(Parcel arg1, int arg2){
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
